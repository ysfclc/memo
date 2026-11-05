package app.memo.android.automation

import app.memo.android.data.TaskKind
import java.net.URI
import java.util.Locale

object TaskRules {
    fun normalizeHttpsUrl(raw: String): String {
        val input = raw.trim()
        if (input.isEmpty()) return ""
        require(input.length <= 2_048) { "URL is too long." }
        require(!input.any { it.isWhitespace() }) { "URL cannot contain spaces." }
        val candidate = if (input.contains("://")) input else "https://$input"
        val uri = runCatching { URI(candidate) }.getOrElse { throw IllegalArgumentException("Enter a valid HTTPS address.") }
        require(uri.scheme?.lowercase(Locale.ROOT) == "https") { "Only HTTPS websites are supported to protect your session." }
        require(uri.rawUserInfo == null) { "URLs must not contain embedded usernames or passwords." }
        require(!uri.host.isNullOrBlank()) { "Enter a website address such as https://example.com." }
        require(uri.host!!.length <= 253) { "Website host name is invalid." }
        return uri.toASCIIString()
    }

    fun validate(title: String, targetUrl: String, kind: TaskKind, query: String): String? {
        if (title.trim().isEmpty()) return "Give this task a name."
        if (title.trim().length > 160) return "Task names must be 160 characters or fewer."
        if (query.length > 8_000) return "Task instructions must be 8,000 characters or fewer."
        if (kind == TaskKind.FIND_TEXT && query.trim().isEmpty()) return "Enter the phrase you want to find."
        if (kind == TaskKind.PREPARE_PROMPT && query.trim().isEmpty()) return "Enter the prompt to prepare in the page."
        return runCatching { normalizeHttpsUrl(targetUrl) }.exceptionOrNull()?.message
    }

    fun guessTitle(kind: TaskKind, query: String): String = when (kind) {
        TaskKind.CAPTURE_PAGE -> "Capture page"
        TaskKind.EXTRACT_LINKS -> "Extract page links"
        TaskKind.FIND_TEXT -> "Find: ${query.trim().take(60)}"
        TaskKind.SCROLL_CAPTURE -> "Review feed"
        TaskKind.PREPARE_PROMPT -> "Prepare AI prompt"
    }
}
