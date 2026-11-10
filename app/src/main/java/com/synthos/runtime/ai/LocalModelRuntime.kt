package com.synthos.runtime.ai

import android.content.Context
import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.SamplerConfig
import java.io.File
import java.util.concurrent.Executors

/**
 * On-device model runtime. The engine is loaded only from an app-private .litertlm file.
 * Inference runs on a single worker and never has direct access to Android actions. Network
 * access for model downloads is handled separately by the UI, never by this runtime.
 */
class LocalModelRuntime private constructor(context: Context) {
    interface Callback {
        fun onComplete(text: String?, error: String?)
    }

    private val appContext = context.applicationContext
    private val worker = Executors.newSingleThreadExecutor { r -> Thread(r, "Memo-Local-Model") }
    @Volatile private var loadedPath: String? = null
    @Volatile private var loadingOrLoaded = false
    @Volatile private var engine: Engine? = null

    fun isLoaded(): Boolean = loadingOrLoaded && engine != null
    fun loadedModelPath(): String? = loadedPath

    fun ask(modelPath: String, systemInstruction: String, prompt: String, callback: Callback) {
        worker.execute {
            try {
                val file = File(modelPath)
                require(file.isFile && file.canRead()) { "The selected model file is missing or unreadable. Import it again in Settings → Local AI." }
                val activeEngine = ensureEngine(file)
                val config = ConversationConfig(
                    systemInstruction = Contents.of(systemInstruction),
                    samplerConfig = SamplerConfig(topK = 20, topP = 0.9, temperature = 0.2)
                )
                val conversation: Conversation = activeEngine.createConversation(config)
                val response = try {
                    conversation.sendMessage(prompt)
                } finally {
                    conversation.close()
                }
                val answer = response.toString().trim()
                if (answer.isBlank()) callback.onComplete(null, "The local model returned an empty response. No action was executed.")
                else callback.onComplete(answer, null)
            } catch (t: Throwable) {
                loadingOrLoaded = engine != null
                callback.onComplete(null, readableError(t))
            }
        }
    }

    fun unload() {
        worker.execute {
            try { engine?.close() } catch (_: Throwable) { }
            engine = null
            loadedPath = null
            loadingOrLoaded = false
        }
    }

    private fun ensureEngine(file: File): Engine {
        val path = file.absolutePath
        if (engine != null && loadedPath == path) return engine!!
        try { engine?.close() } catch (_: Throwable) { }
        engine = null
        loadingOrLoaded = true

        // LiteRT-LM expects its cache directory to exist before Engine initialization.
        // The nested runtime directory is not guaranteed to exist on a fresh install or
        // after Android has cleared the application's cache.
        val appCache = appContext.cacheDir
        if (!appCache.exists() && !appCache.mkdirs()) {
            loadingOrLoaded = false
            throw IllegalStateException("Memo could not create the app cache directory.")
        }
        val modelCache = File(appCache, "litert-lm-cache")
        if (!modelCache.exists() && !modelCache.mkdirs()) {
            loadingOrLoaded = false
            throw IllegalStateException("Memo could not create the local model cache directory.")
        }
        if (!modelCache.isDirectory || !modelCache.canWrite()) {
            loadingOrLoaded = false
            throw IllegalStateException("The local model cache path is not a writable directory.")
        }

        val newEngine = Engine(
            EngineConfig(
                modelPath = path,
                backend = Backend.CPU(),
                cacheDir = modelCache.absolutePath
            )
        )
        try {
            newEngine.initialize()
        } catch (t: Throwable) {
            // LiteRT-LM documents that an Engine whose initialize() failed must not be closed.
            // It is never published to engine, so the failed instance cannot be reused.
            loadingOrLoaded = false
            throw t
        }
        engine = newEngine
        loadedPath = path
        loadingOrLoaded = true
        return newEngine
    }

    private fun readableError(error: Throwable): String {
        val raw = error.message?.trim().orEmpty()
        val name = error.javaClass.simpleName
        return when {
            raw.contains("memory", true) || raw.contains("alloc", true) -> "The model could not fit in available memory. Try a smaller quantized model and close other apps. Details: $raw"
            raw.contains("model", true) || raw.contains("litertlm", true) -> "The model file could not be loaded. Confirm it is a compatible .litertlm model, not a GGUF file. Details: $raw"
            raw.isNotEmpty() -> "$name: $raw"
            else -> "Local inference failed ($name). No Android action was executed."
        }
    }

    companion object {
        @Volatile private var instance: LocalModelRuntime? = null

        @JvmStatic fun shared(context: Context): LocalModelRuntime {
            return instance ?: synchronized(this) {
                instance ?: LocalModelRuntime(context).also { instance = it }
            }
        }
    }
}
