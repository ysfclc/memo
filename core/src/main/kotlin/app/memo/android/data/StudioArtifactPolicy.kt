package app.memo.android.data

import java.security.MessageDigest
import java.util.Locale

/**
 * Validates and canonicalizes a private text artifact before it crosses the Talk capability boundary.
 * Artifact types are descriptive metadata only; they never select a domain-specific execution path.
 */
data class StudioArtifactDraft(
    val title: String,
    val artifactType: String,
    val content: String,
    val contentDigest: String
)

object StudioArtifactPolicy {
    fun prepare(title: String, artifactType: String, content: String): StudioArtifactDraft {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank() && cleanTitle.length <= 240) {
            "An artifact title must contain 1–240 characters."
        }

        val cleanType = artifactType.trim()
            .uppercase(Locale.ROOT)
            .replace(Regex("[^A-Z0-9_]+"), "_")
            .trim('_')
        require(cleanType.matches(Regex("[A-Z][A-Z0-9_]{1,63}"))) {
            "Artifact type must contain 2–64 letters, numbers or underscores."
        }

        val cleanContent = content.trim()
        require(cleanContent.isNotBlank() && cleanContent.length <= 24_000) {
            "Artifact content must contain 1–24,000 characters."
        }

        val digestInput = "$cleanTitle\u0000$cleanType\u0000$cleanContent"
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(digestInput.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }

        return StudioArtifactDraft(
            title = cleanTitle,
            artifactType = cleanType,
            content = cleanContent,
            contentDigest = digest
        )
    }
}
