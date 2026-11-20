package app.memo.android.runtime

import java.net.URI
import java.util.Locale

/** Conservative URL policy for model-selected visible browser navigation. */
object BrowserUrlPolicy {
    private val explicitHttpsUrl = Regex("""https://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val ipv4Address = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
    private val validHostname = Regex("""[a-z0-9.-]+""")

    /** Accept absolute HTTPS URLs on public-looking DNS hosts; reject credentials, IPs, local names and custom ports. */
    fun normalizeHttpsUrl(rawUrl: String): String? {
        val raw = rawUrl.trim()
        if (raw.isBlank() || raw.length > 2_048) return null
        val uri = runCatching { URI(raw) }.getOrNull() ?: return null
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.rawUserInfo != null) return null
        if (uri.port !in setOf(-1, 443)) return null
        val host = uri.host?.lowercase(Locale.ROOT)?.trimEnd('.') ?: return null
        if (!validHostname.matches(host) || host.length > 253 || !host.contains('.')) return null
        if (ipv4Address.matches(host) || host.contains(':')) return null
        val labels = host.split('.')
        if (labels.any { label ->
                label.isBlank() || label.length > 63 || label.startsWith('-') || label.endsWith('-') ||
                    !label.all { it.isLetterOrDigit() || it == '-' }
            }) return null
        if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") ||
            host.endsWith(".internal") || host.endsWith(".intranet") || host.endsWith(".lan") ||
            host.endsWith(".test") || host.endsWith(".example") || host.endsWith(".invalid") ||
            host.endsWith(".onion") || host.endsWith(".home.arpa")) return null

        val path = uri.rawPath?.takeIf { it.isNotBlank() } ?: "/"
        return buildString {
            append("https://").append(host).append(path)
            uri.rawQuery?.let { append('?').append(it) }
            uri.rawFragment?.let { append('#').append(it) }
        }
    }

    /** Return only absolute HTTPS URLs literally included in the user's request. */
    fun explicitlySuppliedUrls(userText: String): Set<String> =
        explicitHttpsUrl.findAll(userText)
            .mapNotNull { match -> normalizeHttpsUrl(trimTrailingPunctuation(match.value)) }
            .toSet()

    fun wasExplicitlySupplied(rawUrl: String, userText: String): Boolean {
        val normalized = normalizeHttpsUrl(rawUrl) ?: return false
        return normalized in explicitlySuppliedUrls(userText)
    }

    private fun trimTrailingPunctuation(value: String): String =
        value.trim().trimEnd('.', ',', ';', '!', '?', ')', ']', '}')
}
