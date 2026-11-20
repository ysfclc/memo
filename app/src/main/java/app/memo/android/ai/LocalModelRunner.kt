package app.memo.android.ai

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.concurrent.Executors

/** Raised only after the bounded inference scope has cancelled its LiteRT conversation. */
class LocalInferenceTimeoutException(timeoutMs: Long) : Exception("Local model inference timed out after ${if (timeoutMs < 1_000L) "$timeoutMs ms" else "${timeoutMs / 1_000} seconds"}. The partial response was not saved as a completed result.")

/** Shared timeout boundary so the cancellation-to-timeout mapping is independently testable. */
internal suspend fun <T> withLocalInferenceTimeout(timeoutMs: Long, block: suspend () -> T): T = try {
    withTimeout(timeoutMs) { block() }
} catch (timeout: TimeoutCancellationException) {
    throw LocalInferenceTimeoutException(timeoutMs)
}

/** A failed GPU request may be replayed only before any text has reached the caller. */
internal fun shouldRetryOnCpuAfterGpuFailure(usingGpu: Boolean, emittedAnyText: Boolean): Boolean =
    usingGpu && !emittedAnyText

/** One single-thread owner serializes all LiteRT engine operations and prevents overlapping use of model memory. */
class LocalModelRunner(private val cacheDir: File) {
    companion object {
        /** A single local inference, including model initialization, must not occupy the engine indefinitely. */
        const val INFERENCE_TIMEOUT_MS = 180_000L
    }
    private val executor = Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "synthos-litert") }
    private val dispatcher = executor.asCoroutineDispatcher()
    private val mutex = Mutex()
    private var engine: Engine? = null
    private var loadedPath: String? = null
    private var usingGpu: Boolean = false

    suspend fun generate(modelFile: File, system: String, prompt: String, onChunk: (String) -> Unit = {}): String =
        withLocalInferenceTimeout(INFERENCE_TIMEOUT_MS) {
            mutex.withLock {
                withContext(dispatcher) {
                    var emittedAnyText = false
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
                                        if (bounded.isNotBlank()) emittedAnyText = true
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
                        if (!shouldRetryOnCpuAfterGpuFailure(usingGpu, emittedAnyText)) {
                            // Do not replay a request after its partial output has already reached the UI.
                            if (usingGpu && emittedAnyText) releaseEngine()
                            throw gpuError
                        }
                        // Some phones let the GPU engine initialize but cannot execute its first graph.
                        // Retry once on CPU only while the request has not emitted any visible text.
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
