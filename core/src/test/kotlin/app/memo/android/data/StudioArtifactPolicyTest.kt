package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class StudioArtifactPolicyTest {
    @Test
    fun normalizesDescriptiveTypeAndProducesStableDigest() {
        val first = StudioArtifactPolicy.prepare(
            title = "  Research brief  ",
            artifactType = "Research report",
            content = "  # Findings\n\nA source-linked draft.  "
        )
        val second = StudioArtifactPolicy.prepare(
            title = "Research brief",
            artifactType = "RESEARCH_REPORT",
            content = "# Findings\n\nA source-linked draft."
        )

        assertEquals("Research brief", first.title)
        assertEquals("RESEARCH_REPORT", first.artifactType)
        assertEquals(second.contentDigest, first.contentDigest)
        assertEquals("# Findings\n\nA source-linked draft.", first.content)
    }

    @Test
    fun digestBindsTitleTypeAndContent() {
        val base = StudioArtifactPolicy.prepare("Brief", "REPORT", "Same body")
        assertNotEquals(base.contentDigest, StudioArtifactPolicy.prepare("Other", "REPORT", "Same body").contentDigest)
        assertNotEquals(base.contentDigest, StudioArtifactPolicy.prepare("Brief", "PLAN", "Same body").contentDigest)
        assertNotEquals(base.contentDigest, StudioArtifactPolicy.prepare("Brief", "REPORT", "Different body").contentDigest)
    }

    @Test
    fun rejectsBlankAndOversizedArtifactFields() {
        assertTrue(runCatching { StudioArtifactPolicy.prepare("", "REPORT", "body") }.isFailure)
        assertTrue(runCatching { StudioArtifactPolicy.prepare("Title", "R", "body") }.isFailure)
        assertTrue(runCatching { StudioArtifactPolicy.prepare("Title", "REPORT", "  ") }.isFailure)
        assertTrue(runCatching { StudioArtifactPolicy.prepare("Title", "REPORT", "x".repeat(24_001)) }.isFailure)
    }

    @Test
    fun acceptsNewArtifactTypesWithoutAddingSpecializedWorkflows() {
        val draft = StudioArtifactPolicy.prepare("Plan", "Personal action plan", "1. Next step")
        assertEquals("PERSONAL_ACTION_PLAN", draft.artifactType)
    }
}
