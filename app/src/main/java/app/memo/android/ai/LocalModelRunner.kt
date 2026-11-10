package app.memo.android.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/** One single-thread owner serializes all LiteRT engine operations and prevents overlapping use of model memory. */
class LocalModelRunner(private val cacheDir: File) {
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "synthos-litert") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var usingGpu: Boolean = false

    suspend fun generate(modelFile: File, system: String, prompt: String, onChunk: (String) -> Unit = {}): String = mutex.withLock {
        withContext(dispatcher) {
            val path = modelFile.absolutePath
            if (loadedPath != path || engine == null) loadEngine(path)

            suspend fun runConversation(): String {
                var conversation: Conversation? = null
                return try {
                    conversation = checkNotNull(engine).createConversation(
                        ConversationConfig(systemInstruction = Contents.of(system))
                    )
                    val response = StringBuilder()
                    conversation.sendMessageAsync(prompt).collect { chunk ->
                        if (response.length < 24_000) {
                            val bounded = chunk.toString().take(24_000 - response.length)
                            if (bounded.isNotEmpty()) {
                                response.append(bounded)
                                onChunk(bounded)
                            }
                        }
                    }
                    response.toString().trim().ifBlank { error("The local model returned an empty answer.") }
                } catch (cancelled: CancellationException) {
                    conversation?.cancelProcess()
                    throw cancelled
                } finally {
                    conversation?.close()
                }
            }

            try {
                runConversation()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (gpuError: Exception) {
                // Some phones let the GPU engine initialize but cannot execute its first graph. Retry once on CPU.
                if (!usingGpu) throw gpuError
                releaseEngine()
                val cpu = Engine(EngineConfig(modelPath = path, backend = Backend.CPU(), cacheDir = cacheDir.absolutePath))
                cpu.initialize()
                engine = cpu
                loadedPath = path
                usingGpu = false
                runConversation()
            }
        }
    }

    private fun loadEngine(path: String) {
        releaseEngine()
        try {
            val gpu = Engine(EngineConfig(modelPath = path, backend = Backend.GPU(), cacheDir = cacheDir.absolutePath))
            gpu.initialize()
            engine = gpu
            loadedPath = path
            usingGpu = true
        } catch (gpuInitializationFailure: Exception) {
            // An engine whose initialize() threw must not be closed. Fall back to the portable CPU backend.
            val cpu = Engine(EngineConfig(modelPath = path, backend = Backend.CPU(), cacheDir = cacheDir.absolutePath))
            cpu.initialize()
            engine = cpu
            loadedPath = path
            usingGpu = false
        }
    }

    suspend fun release() = mutex.withLock { withContext(dispatcher) { releaseEngine() } }

    private fun releaseEngine() {
        engine?.let { runCatching { it.close() } }
        engine = null
        loadedPath = null
        usingGpu = false
    }

    fun shutdown() {
        runCatching {
            executor.execute {
                releaseEngine()
                executor.shutdown()
            }
        }.onFailure { executor.shutdown() }
    }
}
