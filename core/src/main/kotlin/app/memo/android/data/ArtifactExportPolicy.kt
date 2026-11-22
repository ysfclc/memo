package app.memo.android.data

import java.io.InputStream
import java.security.MessageDigest
import java.util.Locale

data class PreparedArtifactExport(
    val fileName: String,
    val mimeType: String,
    val bytes: ByteArray,
    val sha256: String
)

/**
 * Builds bounded, user-directed text exports and verifies the bytes read back from the selected
 * document URI. This policy does not choose a destination or perform filesystem side effects.
 */
object ArtifactExportPolicy {
    const val MAX_EXPORT_BYTES = 1_048_576

    private val extensions = mapOf(
        "text/markdown" to "md",
        "text/plain" to "txt",
        "text/csv" to "csv",
        "application/json" to "json"
    )
    private val knownExtensions = extensions.values.toSet()

    fun suggestedFileName(title: String, mimeType: String): String {
        val extension = extensionFor(mimeType)
        return "${safeStem(title)}.$extension"
    }

    fun prepare(fileName: String, mimeType: String, content: String): PreparedArtifactExport {
        val normalizedMime = mimeType.trim().lowercase(Locale.ROOT)
        val extension = extensionFor(normalizedMime)
        val safeName = "${safeStem(fileName)}.$extension"
        val bytes = content.toByteArray(Charsets.UTF_8)
        require(bytes.isNotEmpty()) { "An exported artifact cannot be empty." }
        require(bytes.size <= MAX_EXPORT_BYTES) { "The exported artifact exceeds the 1 MiB limit." }
        return PreparedArtifactExport(
            fileName = safeName,
            mimeType = normalizedMime,
            bytes = bytes,
            sha256 = sha256(bytes)
        )
    }

    /**
     * Reads at most [maxBytes] and compares the digest in constant time.
     * The caller owns and must close [input]; a failed or oversized read is never verified.
     */
    fun verify(expectedSha256: String, input: InputStream, maxBytes: Int = MAX_EXPORT_BYTES): Boolean {
        if (!Regex("^[0-9a-f]{64}$").matches(expectedSha256) || maxBytes !in 1..MAX_EXPORT_BYTES) return false
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(8 * 1024)
        var total = 0L
        return try {
            while (true) {
                val count = input.read(buffer)
                if (count < 0) break
                if (count == 0) continue
                total += count
                if (total > maxBytes) return false
                digest.update(buffer, 0, count)
            }
            MessageDigest.isEqual(
                expectedSha256.toByteArray(Charsets.US_ASCII),
                digest.digest().toHex().toByteArray(Charsets.US_ASCII)
            )
        } catch (_: Exception) {
            false
        }
    }

    private fun extensionFor(mimeType: String): String =
        extensions[mimeType.trim().lowercase(Locale.ROOT)]
            ?: throw IllegalArgumentException("Unsupported artifact export type.")

    private fun safeStem(fileName: String): String {
        val raw = fileName.trim().take(240)
        require(raw.isNotBlank()) { "An artifact filename is required." }
        val suffix = raw.substringAfterLast('.', "").lowercase(Locale.ROOT)
        val stem = if (suffix in knownExtensions) raw.substringBeforeLast('.') else raw
        val safe = buildString {
            stem.forEach { character ->
                if (character.isLetterOrDigit() || character == '-' || character == '_') {
                    append(character)
                } else if (isNotEmpty() && last() != '-') {
                    append('-')
                }
            }
        }.trim('-', '_').take(72).trim('-', '_')
        require(safe.isNotBlank()) { "The artifact filename has no usable characters." }
        return safe
    }

    private fun sha256(bytes: ByteArray): String =
        MessageDigest.getInstance("SHA-256").digest(bytes).toHex()

    private fun ByteArray.toHex(): String =
        joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
}
