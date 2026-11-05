package app.memo.android.ai

import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.util.Locale

/** Minimal provider-neutral adapter for OpenAI-compatible HTTPS chat-completions endpoints. */
class OpenAiCompatibleClient {
    @Volatile private var activeStreamingConnection: HttpURLConnection? = null

    /** Disconnects the currently active SSE response. It cannot guarantee that the remote server stopped computing. */
    fun cancelStreaming() {
        activeStreamingConnection?.disconnect()
    }

    fun complete(config: ProviderConfig, system: String, user: String): String {
        val base = URI(config.baseUrl.trim().trimEnd('/'))
        require(base.scheme.equals("https", true) && !base.host.isNullOrBlank() && base.rawUserInfo == null) {
            "Cloud endpoints must use HTTPS and must not include credentials in the URL."
        }
        require(config.model.isNotBlank()) { "Choose a remote model name first." }
        val endpoint = URL(base.toASCIIString() + "/chat/completions")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "application/json")
            if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        try {
            val payload = JSONObject().apply {
                put("model", config.model)
                put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user)))
                put("temperature", 0.3)
            }.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            val input = if (status in 200..299) connection.inputStream else connection.errorStream
            val response = input?.use { stream ->
                val output = java.io.ByteArrayOutputStream()
                val buffer = ByteArray(8_192)
                var total = 0
                while (true) {
                    val read = stream.read(buffer)
                    if (read < 0) break
                    total += read
                    require(total <= 4 * 1024 * 1024) { "Provider response exceeded 4 MB." }
                    output.write(buffer, 0, read)
                }
                output.toString(Charsets.UTF_8.name())
            }.orEmpty()
            if (status !in 200..299) {
                val message = runCatching { JSONObject(response).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                error("Remote provider returned HTTP $status${message.takeIf { it.isNotBlank() }?.let { ": ${it.take(180)}" }.orEmpty()}.")
            }
            val root = JSONObject(response)
            val content = root.optJSONArray("choices")?.optJSONObject(0)?.optJSONObject("message")?.opt("content")
            val text = when (content) {
                is String -> content
                is JSONArray -> (0 until content.length()).mapNotNull { idx -> content.optJSONObject(idx)?.optString("text") }.joinToString("\n")
                else -> ""
            }.trim()
            require(text.isNotBlank()) { "The remote provider returned an empty answer." }
            return text.take(24_000)
        } finally {
            connection.disconnect()
        }
    }
    /**
     * Streaming variant of Chat Completions. Providers that do not support SSE fail explicitly; Memo never
     * fabricates progress. Each emitted delta is the actual `choices[].delta.content` from the endpoint.
     */
    fun completeStreaming(config: ProviderConfig, system: String, user: String, onChunk: (String) -> Unit): String {
        val base = URI(config.baseUrl.trim().trimEnd('/'))
        require(base.scheme.equals("https", true) && !base.host.isNullOrBlank() && base.rawUserInfo == null) {
            "Cloud endpoints must use HTTPS and must not include credentials in the URL."
        }
        require(config.model.isNotBlank()) { "Choose a remote model name first." }
        val endpoint = URL(base.toASCIIString() + "/chat/completions")
        val connection = (endpoint.openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("Accept", "text/event-stream")
            if (config.apiKey.isNotBlank()) setRequestProperty("Authorization", "Bearer ${config.apiKey}")
        }
        activeStreamingConnection = connection
        try {
            val payload = JSONObject().apply {
                put("model", config.model)
                put("messages", JSONArray().put(JSONObject().put("role", "system").put("content", system)).put(JSONObject().put("role", "user").put("content", user)))
                put("temperature", 0.3)
                put("stream", true)
            }.toString().toByteArray(Charsets.UTF_8)
            connection.outputStream.use { it.write(payload) }
            val status = connection.responseCode
            if (status !in 200..299) {
                val errorText = connection.errorStream?.use { stream ->
                    val bytes = stream.readNBytes(16_384)
                    bytes.toString(Charsets.UTF_8)
                }.orEmpty()
                val message = runCatching { JSONObject(errorText).optJSONObject("error")?.optString("message") }.getOrNull().orEmpty()
                error("Remote provider returned HTTP $status${message.takeIf { it.isNotBlank() }?.let { ": ${it.take(180)}" }.orEmpty()}. Streaming was not started.")
            }
            val contentType = connection.contentType.orEmpty().lowercase(Locale.ROOT)
            val answer = StringBuilder()
            connection.inputStream.bufferedReader(Charsets.UTF_8).use { reader ->
                while (answer.length < 24_000) {
                    val line = reader.readLine() ?: break
                    if (Thread.currentThread().isInterrupted) throw java.io.InterruptedIOException("Remote stream was interrupted.")
                    val data = when {
                        line.startsWith("data:") -> line.removePrefix("data:").trim()
                        line.startsWith("{") -> line.trim() // Some compatible endpoints return one JSON object instead of SSE.
                        else -> continue
                    }
                    if (data.isBlank() || data == "[DONE]") continue
                    val delta = runCatching {
                        val root = JSONObject(data)
                        val choice = root.optJSONArray("choices")?.optJSONObject(0)
                        val piece = choice?.optJSONObject("delta")?.opt("content")
                            ?: choice?.optJSONObject("message")?.opt("content")
                        when (piece) {
                            is String -> piece
                            is JSONArray -> (0 until piece.length()).mapNotNull { i -> piece.optJSONObject(i)?.optString("text") }.joinToString("")
                            else -> ""
                        }
                    }.getOrElse { parseError ->
                        if (!contentType.contains("text/event-stream")) throw IllegalStateException("Remote streaming response could not be parsed.", parseError)
                        ""
                    }
                    if (delta.isNotEmpty()) {
                        val bounded = delta.take(24_000 - answer.length)
                        answer.append(bounded)
                        if (bounded.isNotEmpty()) onChunk(bounded)
                    }
                }
            }
            require(answer.isNotBlank()) { "Remote provider returned no streamed content. Confirm that the selected endpoint supports OpenAI-compatible streaming." }
            return answer.toString().trim()
        } finally {
            if (activeStreamingConnection === connection) activeStreamingConnection = null
            connection.disconnect()
        }
    }

}
