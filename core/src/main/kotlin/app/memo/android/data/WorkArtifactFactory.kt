package app.memo.android.data

import org.json.JSONObject

/**
 * Builds a private, user-authored Studio draft using the same encrypted evidence store
 * as generated artifacts. This factory never marks content as verified or publishable.
 */
object WorkArtifactFactory {
    const val MAX_TITLE_CHARS = 240
    const val MAX_CONTENT_CHARS = 24_000
    private const val MAX_TYPE_CHARS = 80

    fun create(title: String, type: String, content: String): EvidenceRecord {
        val cleanTitle = title.trim().take(MAX_TITLE_CHARS)
        val cleanType = type.trim().take(MAX_TYPE_CHARS)
        val cleanContent = content.trim().take(MAX_CONTENT_CHARS)
        require(cleanTitle.isNotBlank()) { "A draft needs a title." }
        require(cleanType.isNotBlank()) { "A draft needs a type." }
        require(cleanContent.length >= 2) { "A draft needs at least two characters." }

        val metadata = JSONObject()
            .put("provenance", "user-authored")
            .put("artifactType", cleanType)
            .put("version", 1)
            .put("requiresHumanReview", true)

        return EvidenceRecord(
            title = cleanTitle,
            sourceUrl = "",
            kind = "WORK_ARTIFACT",
            content = cleanContent,
            summary = cleanContent.lineSequence().firstOrNull().orEmpty().take(1_000),
            metadata = metadata.toString()
        )
    }
}
