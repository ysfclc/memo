package app.memo.android.browser

/** Versioned, text-only handoff from Memo's authenticated browser to another app. */
object BrowserCaptureSharePayload {
    const val HEADER = "MEMO_BROWSER_CAPTURE_V1"
    const val MAX_PAGE_TEXT = 15_000
    const val MAX_PAYLOAD = 18_500

    fun format(title: String, sourceUrl: String, summary: String, content: String): String? {
        val cleanTitle = title.trim().replace(Regex("\\s+"), " ").take(240)
        val candidateUrl = sourceUrl.trim().take(2_048)
        val uri = runCatching { java.net.URI(candidateUrl) }.getOrNull() ?: return null
        val scheme = uri.scheme?.lowercase()
        if (scheme !in setOf("http", "https") || uri.host.isNullOrBlank() || uri.rawUserInfo != null) return null
        // Query strings and fragments can carry OAuth codes or temporary session credentials.
        val cleanUrl = uri.toASCIIString().substringBefore('?').substringBefore('#')
        if (cleanUrl.isBlank() || cleanUrl.length > 2_048) return null

        val cleanSummary = summary.trim().take(600)
        val cleanContent = content.trim().take(MAX_PAGE_TEXT)
        if (cleanContent.length < 30) return null

        return buildString {
            append(HEADER).append('\n')
            append("Title: ").append(cleanTitle.ifBlank { uri.host }).append('\n')
            append("Source URL: ").append(cleanUrl).append('\n')
            append("Summary: ").append(cleanSummary).append("\n\n")
            append("Captured page text:\n")
            append(cleanContent)
        }.take(MAX_PAYLOAD)
    }
}
