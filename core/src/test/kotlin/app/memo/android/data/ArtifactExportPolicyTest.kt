package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream

class ArtifactExportPolicyTest {
    @Test
    fun preparesUtf8MarkdownWithSafeFilenameAndExactDigest() {
        val payload = ArtifactExportPolicy.prepare(
            fileName = "../../Résumé: Q4?.txt",
            mimeType = "text/markdown",
            content = "# Résumé\n\nZażółć gęślą jaźń.\n"
        )

        assertEquals("Résumé-Q4.md", payload.fileName)
        assertEquals("text/markdown", payload.mimeType)
        assertEquals("# Résumé\n\nZażółć gęślą jaźń.\n", payload.bytes.toString(Charsets.UTF_8))
        assertTrue(Regex("^[0-9a-f]{64}$").matches(payload.sha256))
        assertEquals("Résumé-Q4.md", ArtifactExportPolicy.suggestedFileName("../../Résumé: Q4?.txt", "text/markdown"))
    }

    @Test
    fun usesSafeFallbackWhenTitleHasNoFilenameCharacters() {
        assertEquals("memo-artifact.md", ArtifactExportPolicy.suggestedFileName("***", "text/markdown"))
        assertEquals("memo-artifact.md", ArtifactExportPolicy.prepare("***", "text/markdown", "body").fileName)
    }

    @Test
    fun verifiesExactReadBackAndRejectsChangedOrTruncatedBytes() {
        val payload = ArtifactExportPolicy.prepare("report", "application/json", "{\"ok\":true}")
        assertEquals("report.json", payload.fileName)
        assertTrue(ArtifactExportPolicy.verify(payload.sha256, ByteArrayInputStream(payload.bytes)))
        assertFalse(ArtifactExportPolicy.verify(payload.sha256, ByteArrayInputStream("{\"ok\":false}".toByteArray())))
        assertFalse(ArtifactExportPolicy.verify(payload.sha256, ByteArrayInputStream("{\"ok\"".toByteArray())))
    }

    @Test
    fun rejectsUnsupportedMimeEmptyAndOversizedExports() {
        assertTrue(runCatching { ArtifactExportPolicy.prepare("file", "application/pdf", "body") }.isFailure)
        assertTrue(runCatching { ArtifactExportPolicy.prepare("file", "text/plain", "") }.isFailure)
        assertTrue(runCatching {
            ArtifactExportPolicy.prepare("file", "text/plain", "x".repeat(ArtifactExportPolicy.MAX_EXPORT_BYTES + 1))
        }.isFailure)
    }

    @Test
    fun verificationFailsClosedForInvalidDigestAndOversizedReadBack() {
        val bytes = "small".toByteArray()
        val digest = ArtifactExportPolicy.prepare("small", "text/plain", "small").sha256
        assertFalse(ArtifactExportPolicy.verify("not-a-digest", ByteArrayInputStream(bytes)))
        assertFalse(ArtifactExportPolicy.verify(digest, ByteArrayInputStream(bytes), maxBytes = 2))
    }
}
