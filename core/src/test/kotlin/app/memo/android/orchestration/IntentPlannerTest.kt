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

    @Test fun jobSearchUsesBrowserResearchInsteadOfCareerArtifactGeneration() {
        val english = IntentPlanner.plan("Find me ten job openings in Krakow")
        assertEquals(IntentDomain.RESEARCH, english.domain)
        assertTrue(english.steps.any { it.title == "Find current job opportunities" })
        assertTrue(english.steps.any { it.instruction.contains("source URL") })
        assertFalse(english.requiresExternalApproval)

        assertEquals(IntentDomain.RESEARCH, IntentPlanner.plan("Znajdź oferty pracy zdalnej").domain)
        assertEquals(IntentDomain.RESEARCH, IntentPlanner.plan("İş ilanları bul").domain)
        assertEquals(IntentDomain.CAREER, IntentPlanner.plan("Prepare my CV for Customer Success").domain)
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

    @Test fun oneEntryRouterSeparatesActionPlansFromConversation() {
        assertTrue(IntentPlanner.shouldCreateWorkflow("Find me ten potential customers"))
        assertTrue(IntentPlanner.shouldCreateWorkflow("Prepare my CV for a Customer Success role"))
        assertTrue(IntentPlanner.shouldCreateWorkflow("Automate a sequence in my Android app"))
        assertFalse(IntentPlanner.shouldCreateWorkflow("Brainstorm an idea with me"))
        assertFalse(IntentPlanner.shouldCreateWorkflow("What does this error mean?"))
        assertFalse(IntentPlanner.shouldCreateWorkflow("How can I find better job opportunities?"))
    }

    @Test fun oneEntryRouterRecognizesPolishAndTurkishActions() {
        assertTrue(IntentPlanner.shouldCreateWorkflow("Przygotuj CV na stanowisko Customer Success"))
        assertTrue(IntentPlanner.shouldCreateWorkflow("Bana on yeni müşteri bul"))
        assertFalse(IntentPlanner.shouldCreateWorkflow("Jak działa ten proces?"))
        assertFalse(IntentPlanner.shouldCreateWorkflow("Bu ne anlama geliyor?"))
    }

    @Test fun shortOrBlankIntentsAreRejected() {
        assertTrue(runCatching { IntentPlanner.plan(" ") }.isFailure)
        assertTrue(runCatching { IntentPlanner.plan("abc") }.isFailure)
    }
}
