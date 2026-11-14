package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FrontierResponseCaptureCoordinatorTest {
    private fun baseSnapshot(requiresApproval: Boolean = false): AppSnapshot {
        val conversation = ConversationRecord(
            id = "conversation-1",
            title = "Frontier consultation",
            sourceType = "BROWSER_WORKFLOW"
        )
        val step = JobStepRecord(
            id = "step-1",
            title = "Consult ChatGPT",
            status = "RUNNING",
            requiresApproval = requiresApproval
        )
        val job = WorkJobRecord(
            id = "job-1",
            title = "Research",
            goal = "Research the requested topic",
            status = "RUNNING",
            sourceConversationId = conversation.id,
            steps = listOf(step)
        )
        return AppSnapshot(conversations = listOf(conversation), jobs = listOf(job))
    }

    private fun request(actionId: String = "") = FrontierResponseCaptureRequest(
        jobId = "job-1",
        stepId = "step-1",
        conversationId = "conversation-1",
        actionId = actionId,
        consultationTitle = "Research brief",
        providerId = "chatgpt",
        providerName = "ChatGPT",
        prompt = "Research the requested topic and cite sources.",
        response = "A sufficiently detailed provider response with useful findings.",
        sourceUrl = "https://chatgpt.com/c/123",
        startedAt = 1_000L,
        completedAt = 2_000L,
        captureMethod = "AUTOMATIC_DOM",
        workflowMode = "CONSULT_AND_COMPARE"
    )

    private fun inProgressAction(): Pair<AppSnapshot, String> {
        val base = baseSnapshot(requiresApproval = true)
        val proposal = ActionApprovalLedger.propose(
            snapshot = base,
            jobId = "job-1",
            stepId = "step-1",
            runtimeId = "runtime.browser",
            capabilityId = "browser.submit_prompt",
            destination = "https://chatgpt.com/",
            title = "Submit consultation prompt",
            payload = "Research the requested topic and cite sources.",
            now = 1_100L
        )
        assertTrue(proposal.accepted)
        val action = requireNotNull(proposal.action)
        val approved = ActionApprovalLedger.approve(proposal.snapshot, action.id, action.contentHash, now = 1_200L)
        assertTrue(approved.accepted)
        val started = ActionApprovalLedger.beginExecution(
            approved.snapshot,
            action.id,
            "runtime.browser",
            "browser.submit_prompt",
            "https://chatgpt.com/",
            "Research the requested topic and cite sources.",
            now = 1_300L
        )
        assertTrue(started.accepted)
        return started.snapshot to action.id
    }

    @Test
    fun successfulCaptureAtomicallySavesEvidenceTranscriptEntityAndStep() {
        val transition = FrontierResponseCaptureCoordinator.commit(baseSnapshot(), request(), now = 3_000L)

        assertEquals(FrontierResponseCaptureStatus.PERSISTED, transition.status)
        assertEquals(1, transition.snapshot.evidence.size)
        assertEquals(1, transition.snapshot.conversations.single().messages.size)
        assertEquals("BROWSER", transition.snapshot.conversations.single().messages.single().sourceType)
        assertEquals("COMPLETED", transition.snapshot.jobs.single().steps.single().status)
        assertEquals(1, transition.snapshot.entities.size)
        assertEquals("FRONTIER_RESPONSE", transition.snapshot.entities.single().type)
        assertEquals(1, transition.snapshot.relationships.size)
        assertEquals(request().response.take(1_800), transition.snapshot.jobs.single().steps.single().evidence)
        assertTrue(transition.snapshot.activity.any { it.message.startsWith("Evidence captured:") })
    }

    @Test
    fun approvedInProgressActionCompletesOnlyAgainstTheExactSavedStepEvidence() {
        val (snapshot, actionId) = inProgressAction()
        val transition = FrontierResponseCaptureCoordinator.commit(snapshot, request(actionId), now = 3_000L)

        assertEquals(FrontierResponseCaptureStatus.PERSISTED, transition.status)
        val action = transition.snapshot.actionApprovals.single { it.id == actionId }
        assertEquals("COMPLETED", action.status)
        assertEquals("evidence:" + transition.evidenceId, action.evidenceRef)
        assertEquals("COMPLETED", transition.snapshot.jobs.single().steps.single().status)
    }

    @Test
    fun missingApprovalRecordSavesResponseButLeavesStepForReview() {
        val transition = FrontierResponseCaptureCoordinator.commit(
            baseSnapshot(requiresApproval = true),
            request("missing-action"),
            now = 3_000L
        )

        assertEquals(FrontierResponseCaptureStatus.NEEDS_REVIEW, transition.status)
        assertEquals(1, transition.snapshot.evidence.size)
        assertEquals(1, transition.snapshot.conversations.single().messages.size)
        assertEquals("NEEDS_REVIEW", transition.snapshot.jobs.single().steps.single().status)
        assertTrue(transition.message.contains("outbound action"))
    }

    @Test
    fun repeatedIdenticalCaptureIsIdempotent() {
        val first = FrontierResponseCaptureCoordinator.commit(baseSnapshot(), request(), now = 3_000L)
        val second = FrontierResponseCaptureCoordinator.commit(first.snapshot, request(), now = 4_000L)

        assertEquals(FrontierResponseCaptureStatus.PERSISTED, second.status)
        assertEquals(1, second.snapshot.evidence.size)
        assertEquals(1, second.snapshot.conversations.single().messages.size)
        assertEquals(1, second.snapshot.entities.size)
    }

    @Test
    fun conflictingResponseForSameProviderStepIsRejectedWithoutMutation() {
        val first = FrontierResponseCaptureCoordinator.commit(baseSnapshot(), request(), now = 3_000L)
        val conflicting = request().copy(response = "A different response from the same provider step.")
        val second = FrontierResponseCaptureCoordinator.commit(first.snapshot, conflicting, now = 4_000L)

        assertEquals(FrontierResponseCaptureStatus.FAILED, second.status)
        assertEquals(first.snapshot, second.snapshot)
        assertTrue(second.message.contains("different response"))
    }

    @Test
    fun evidenceLimitPreventsFalseCompletion() {
        val base = baseSnapshot()
        val full = base.copy(evidence = (1..150).map {
            EvidenceRecord(id = "evidence-$it", title = "Existing", sourceUrl = "https://example.com", kind = "TEST", content = "Existing", summary = "Existing")
        })
        val transition = FrontierResponseCaptureCoordinator.commit(full, request(), now = 3_000L)

        assertEquals(FrontierResponseCaptureStatus.FAILED, transition.status)
        assertEquals(full, transition.snapshot)
        assertEquals("RUNNING", transition.snapshot.jobs.single().steps.single().status)
    }

    @Test
    fun entityLimitSavesEvidenceButDoesNotClaimFullCompletion() {
        val base = baseSnapshot()
        val full = base.copy(entities = (1..1_200).map {
            EntityRecord(type = "NOTE", title = "Existing " + it)
        })
        val transition = FrontierResponseCaptureCoordinator.commit(full, request(), now = 3_000L)

        assertEquals(FrontierResponseCaptureStatus.NEEDS_REVIEW, transition.status)
        assertEquals(1, transition.snapshot.evidence.size)
        assertEquals(1, transition.snapshot.conversations.single().messages.size)
        assertEquals("NEEDS_REVIEW", transition.snapshot.jobs.single().steps.single().status)
        assertTrue(transition.entityId.isBlank())
    }
}
