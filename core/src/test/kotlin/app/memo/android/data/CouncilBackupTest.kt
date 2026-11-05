package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CouncilBackupTest {
    @Test fun councilTraceAndDecisionSurviveWorkspaceJsonRoundTrip() {
        val turn = CouncilTurnRecord(
            modelId = "local:qwen3-0.6b",
            modelName = "Qwen3 0.6B · on device",
            role = "Skeptic",
            requestSummary = "Review the proposal",
            response = "The claim needs a source.",
            durationMs = 1250
        )
        val run = CouncilRunRecord(
            prompt = "Evaluate this proposal",
            pattern = "CRITIC",
            turns = listOf(turn),
            synthesis = "There is not enough evidence.",
            finalDecision = "Verify the claim first.",
            sources = listOf(CouncilSourceRef("evidence-1", "Research note", "https://example.com/source")),
            cloudPermissionGranted = false,
            durationMs = 2600
        )
        val original = AppSnapshot(councilRuns = listOf(run))
        val restored = AppSnapshot.fromJson(original.toJson())
        assertEquals(6, org.json.JSONObject(original.toJson()).optInt("schemaVersion"))
        assertEquals("Evaluate this proposal", restored.councilRuns.single().prompt)
        assertEquals("Verify the claim first.", restored.councilRuns.single().finalDecision)
        assertEquals("The claim needs a source.", restored.councilRuns.single().turns.single().response)
        assertEquals("Research note", restored.councilRuns.single().sources.single().title)
        assertFalse(restored.councilRuns.single().cloudPermissionGranted)
    }

    @Test fun schemaVersionTwoBackupStillImportsWithoutWorkspaceRecords() {
        val v2 = """{"schemaVersion":2,"themeMode":"DARK","tasks":[],"evidence":[],"activity":[],"councilRuns":[]}"""
        val restored = AppSnapshot.fromJson(v2)
        assertTrue(restored.conversations.isEmpty())
        assertTrue(restored.entities.isEmpty())
        assertEquals("DARK", restored.themeMode)
    }

    @Test fun schemaVersionOneBackupStillImports() {
        val v1 = """{"schemaVersion":1,"themeMode":"SYSTEM","tasks":[],"evidence":[],"activity":[]}"""
        val restored = AppSnapshot.fromJson(v1)
        assertTrue(restored.councilRuns.isEmpty())
        assertEquals("SYSTEM", restored.themeMode)
    }
}
