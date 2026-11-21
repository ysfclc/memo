package app.memo.android.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
    @Test
    fun downloadCheckpointRoundTripsAndBindsToPinnedCatalogSource() {
        val entry = ModelCatalog.entries.first()
        val checkpoint = ModelDownloadCheckpoint(
            modelId = entry.id,
            sourceUrl = entry.downloadUrl,
            expectedSha256 = entry.expectedSha256,
            expectedBytes = entry.expectedBytes,
            etag = "\"model-revision-etag\"",
            lastModified = "Sat, 21 Nov 2026 10:00:00 GMT"
        )

        val decoded = ModelDownloadCheckpointCodec.decode(ModelDownloadCheckpointCodec.encode(checkpoint))
        assertEquals(checkpoint, decoded)
        assertTrue(decoded!!.matches(entry))
        assertFalse(decoded.copy(expectedSha256 = "0".repeat(64)).matches(entry))
        assertFalse(decoded.copy(sourceUrl = "https://example.invalid/model").matches(entry))
    }

    @Test
    fun rangeResumeRequiresExactOffsetAndStableValidator() {
        assertTrue(
            ModelDownloadResumePolicy.canAppend(
                offset = 500L,
                responseCode = 206,
                contentRange = "bytes 500-999/1000",
                savedEtag = "etag-v1",
                responseEtag = "etag-v1",
                savedLastModified = "",
                responseLastModified = ""
            )
        )
        assertEquals(1_000L, ModelDownloadResumePolicy.contentRangeTotal("bytes 500-999/1000"))
        assertFalse(
            ModelDownloadResumePolicy.canAppend(
                500L, 200, null, "etag-v1", "etag-v1", "", ""
            )
        )
        assertFalse(
            ModelDownloadResumePolicy.canAppend(
                500L, 206, "bytes 400-999/1000", "etag-v1", "etag-v1", "", ""
            )
        )
        assertFalse(
            ModelDownloadResumePolicy.canAppend(
                500L, 206, "bytes 500-999/1000", "etag-old", "etag-new", "", ""
            )
        )
        assertFalse(
            ModelDownloadResumePolicy.canAppend(
                500L, 206, "bytes 500-999/1000", "", "", "last-old", "last-new"
            )
        )
    }

    @Test
    fun modelDiscoveryFindsCatalogAndCustomFilesWithoutListingPartialOrNestedFiles() {
        val root = File(System.getProperty("java.io.tmpdir"), "memo-model-discovery-${System.nanoTime()}")
        val nested = File(root, "nested")
        assertTrue(root.mkdirs())
        assertTrue(nested.mkdirs())
        try {
            val catalog = ModelCatalog.entries.first()
            File(root, catalog.fileName).writeBytes(byteArrayOf(1, 2, 3))
            File(root, "custom-model.litertlm").writeBytes(byteArrayOf(4, 5, 6))
            File(root, "broken.litertlm.partial").writeBytes(byteArrayOf(7, 8, 9))
            File(nested, "nested-model.litertlm").writeBytes(byteArrayOf(10, 11, 12))

            val found = ModelFileDiscovery.scan(listOf(root, root), minimumBytes = 1L)
            assertEquals(2, found.size)
            assertTrue(found.any { it.id == catalog.id && it.isCatalogModel })
            assertTrue(found.any { it.name == "custom-model" && !it.isCatalogModel })
            assertTrue(found.map { it.id }.distinct().size == found.size)
        } finally {
            root.deleteRecursively()
        }
    }

}
