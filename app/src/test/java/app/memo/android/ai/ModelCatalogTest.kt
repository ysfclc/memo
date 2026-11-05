package app.memo.android.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ModelCatalogTest {
    @Test
    fun curatedEntriesHaveUniqueIdsAndFileNames() {
        val entries = ModelCatalog.entries
        assertEquals(3, entries.size)
        assertEquals(entries.size, entries.map { it.id }.distinct().size)
        assertEquals(entries.size, entries.map { it.fileName }.distinct().size)
    }

    @Test
    fun everyDownloadIsPinnedHasAValidChecksumAndUsesLiteRtFormat() {
        ModelCatalog.entries.forEach { entry ->
            assertTrue(entry.downloadUrl.startsWith("https://"))
            assertTrue("Model revision must be immutable: ${entry.id}", Regex("/resolve/[0-9a-f]{7,40}/").containsMatchIn(entry.downloadUrl))
            assertTrue(entry.fileName.endsWith(".litertlm"))
            assertTrue(Regex("^[0-9a-f]{64}$").matches(entry.expectedSha256))
            assertTrue(entry.minimumBytes > 0L)
            assertTrue(entry.expectedBytes >= entry.minimumBytes)
            assertEquals("Apache-2.0", entry.license)
        }
    }

    @Test
    fun memoryTiersAreClearlyRepresented() {
        assertTrue(ModelCatalog.entries.any { it.id == "qwen3-0.6b" })
        assertTrue(ModelCatalog.entries.any { it.id == "qwen3-1.7b-int4" })
        assertTrue(ModelCatalog.entries.any { it.id == "gemma4-e2b" })
        assertEquals("qwen3-0.6b", ModelCatalog.entries.minBy { it.expectedBytes }.id)
    }
}
