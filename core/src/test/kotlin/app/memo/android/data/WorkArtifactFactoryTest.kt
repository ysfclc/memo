package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.json.JSONObject

class WorkArtifactFactoryTest {
    @Test
    fun createsPrivateUserAuthoredDraftWithReviewRequiredMetadata() {
        val artifact = WorkArtifactFactory.create(
            title = "  Event proposal  ",
            type = "  Proposal ",
            content = "  First paragraph\nSecond paragraph  "
        )
        val metadata = JSONObject(artifact.metadata)

        assertEquals("Event proposal", artifact.title)
        assertEquals("WORK_ARTIFACT", artifact.kind)
        assertEquals("First paragraph\nSecond paragraph", artifact.content)
        assertEquals("First paragraph", artifact.summary)
        assertEquals("user-authored", metadata.getString("provenance"))
        assertEquals("Proposal", metadata.getString("artifactType"))
        assertEquals(1, metadata.getInt("version"))
        assertTrue(metadata.getBoolean("requiresHumanReview"))
        assertFalse(metadata.has("approved"))
        assertFalse(metadata.has("published"))
    }

    @Test
    fun rejectsMissingTitleTypeAndContent() {
        assertTrue(runCatching { WorkArtifactFactory.create(" ", "Note", "Body") }.isFailure)
        assertTrue(runCatching { WorkArtifactFactory.create("Title", " ", "Body") }.isFailure)
        assertTrue(runCatching { WorkArtifactFactory.create("Title", "Note", " ") }.isFailure)
    }

    @Test
    fun enforcesThePersistedArtifactBounds() {
        val artifact = WorkArtifactFactory.create(
            title = "T".repeat(WorkArtifactFactory.MAX_TITLE_CHARS + 10),
            type = "K".repeat(WorkArtifactFactory.MAX_TITLE_CHARS),
            content = "x".repeat(WorkArtifactFactory.MAX_CONTENT_CHARS + 10)
        )

        assertEquals(WorkArtifactFactory.MAX_TITLE_CHARS, artifact.title.length)
        assertEquals(WorkArtifactFactory.MAX_CONTENT_CHARS, artifact.content.length)
        assertEquals(80, JSONObject(artifact.metadata).getString("artifactType").length)
    }
}
