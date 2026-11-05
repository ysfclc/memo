package app.memo.android.orchestration

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IntentPlannerTest {
    @Test fun buildRequestsPrioritizeInspectionAndImplementation() {
        val plan = IntentPlanner.plan("Build a mobile app for my projects")
        assertEquals(IntentDomain.BUILD, plan.domain)
        assertTrue(plan.steps.any { it.title.contains("Inspect the current implementation") })
        assertTrue(plan.steps.any { it.title.contains("Implement a coherent vertical slice") })
        assertFalse(plan.requiresExternalApproval)
    }

    @Test fun researchRequestsRequireEvidenceAndVerification() {
        val plan = IntentPlanner.plan("Research the best local models and compare evidence")
        assertEquals(IntentDomain.RESEARCH, plan.domain)
        assertTrue(plan.steps.any { it.title.contains("source evidence", true) })
        assertTrue(plan.steps.any { it.title.contains("Verify", true) })
    }

    @Test fun externalWriteIntentCreatesNonExecutableApprovalCheckpoint() {
        val plan = IntentPlanner.plan("Publish this post to LinkedIn")
        assertTrue(plan.requiresExternalApproval)
        val gate = plan.steps.single { it.requiresApproval }
        assertEquals("EXTERNAL_ACTION", gate.checkpointKind)
        assertTrue(gate.instruction.contains("cannot itself send"))
    }

    @Test fun turkishAndPolishIntentsAreRecognized() {
        assertEquals(IntentDomain.CAREER, IntentPlanner.plan("CV ve iş başvurusu hazırla").domain)
        assertEquals(IntentDomain.CRM, IntentPlanner.plan("Dodaj kontakt i firmę do CRM").domain)
        assertEquals(IntentDomain.CONTENT, IntentPlanner.plan("Prepare a LinkedIn post").domain)
    }

    @Test fun shortOrBlankIntentsAreRejected() {
        assertTrue(runCatching { IntentPlanner.plan(" ") }.isFailure)
        assertTrue(runCatching { IntentPlanner.plan("abc") }.isFailure)
    }
}
