package app.memo.android.data

import app.memo.android.orchestration.WorkflowCheckpointCoordinator
import app.memo.android.runtime.EntityProposalRuntimeAdapter
import app.memo.android.runtime.WorkflowPostconditionOutcome
import app.memo.android.runtime.WorkflowRuntimeAdapterResolver
import app.memo.android.runtime.WorkflowRuntimeResult
import app.memo.android.runtime.RuntimeRegistry
import kotlin.coroutines.Continuation
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.coroutines.startCoroutine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityProposalWorkflowCoordinatorTest {
    private val runtimes = RuntimeRegistry().listRuntimes()

    private fun fixture(): AppSnapshot {
        val conversation = ConversationRecord(
            id = "conversation-1",
            title = "Industrial account discussion",
            messages = listOf(
                ChatMessageRecord(
                    id = "message-1",
                    role = "USER",
                    content = "Acme Robotics currently uses a legacy ERP. Their team has 40 employees and wants to modernise reporting."
                )
            )
        )
        val sourceJob = WorkJobRecord(
            id = "source-job",
            title = "Extract discussion outcomes",
            goal = "Extract source-grounded entities and propose them for review.",
            status = "RUNNING",
            runtimeId = "app.models",
            sourceConversationId = conversation.id,
            steps = listOf(JobStepRecord(id = "source-step", title = "Validate and save proposals", status = "RUNNING"))
        )
        return AppSnapshot(
            conversations = listOf(conversation),
            entities = listOf(EntityRecord(
                id = conversation.id,
                type = "CONVERSATION",
                title = conversation.title,
                sourceConversationId = conversation.id
            )),
            jobs = listOf(sourceJob)
        )
    }

    private fun proposal(title: String, excerpt: String, type: String = "ORGANIZATION") = OutcomeProposal(
        type = type,
        title = title,
        description = "A source-grounded account candidate.",
        properties = mapOf("teamSize" to "40", "system" to "legacy ERP"),
        evidence = excerpt
    )

    private fun runSuspend(block: suspend () -> WorkflowRuntimeResult): WorkflowRuntimeResult {
        var completed: Result<WorkflowRuntimeResult>? = null
        block.startCoroutine(object : Continuation<WorkflowRuntimeResult> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<WorkflowRuntimeResult>) {
                completed = result
            }
        })
        return completed?.getOrThrow() ?: error("The deterministic adapter unexpectedly suspended.")
    }

    private fun runSuspendObserver(
        block: suspend () -> app.memo.android.runtime.WorkflowPostconditionEvidence
    ): app.memo.android.runtime.WorkflowPostconditionEvidence {
        var completed: Result<app.memo.android.runtime.WorkflowPostconditionEvidence>? = null
        block.startCoroutine(object : Continuation<app.memo.android.runtime.WorkflowPostconditionEvidence> {
            override val context = EmptyCoroutineContext
            override fun resumeWith(result: Result<app.memo.android.runtime.WorkflowPostconditionEvidence>) {
                completed = result
            }
        })
        return completed?.getOrThrow() ?: error("The read-only observer unexpectedly suspended.")
    }

    @Test fun executesEntityProposalThroughExactRuntimeAndIndependentReadAfterWriteVerification() {
        val initial = fixture()
        val prepared = EntityProposalWorkflowCoordinator.prepare(
            initial,
            "source-job",
            "conversation-1",
            listOf(proposal("Acme Robotics", "Acme Robotics currently uses a legacy ERP.")),
            runtimes
        ).getOrThrow()

        assertEquals(1, prepared.snapshot.jobs.size - initial.jobs.size)
        val started = WorkflowCheckpointCoordinator.start(
            prepared.snapshot, prepared.jobId, prepared.taskId, runtimes, System.currentTimeMillis()
        ).getOrThrow()
        val request = EntityProposalWorkflowCoordinator.buildRequest(started, prepared, runtimes)
        val adapter = EntityProposalRuntimeAdapter(runtimes)
        val resolved = WorkflowRuntimeAdapterResolver.resolve(request, runtimes, listOf(adapter))
        assertEquals("app.entities", resolved.runtimeId)
        assertEquals(setOf("entity.propose"), resolved.capabilityIds)

        val runtimeResult = runSuspend { resolved.execute(request) }
        assertTrue(runtimeResult is WorkflowRuntimeResult.Output)
        runtimeResult.validateFor(request)
        val output = (runtimeResult as WorkflowRuntimeResult.Output).value
        val saved = EntityProposalWorkflowCoordinator.saveOutput(
            started, request, output, runtimeResult.completedAt
        ).getOrThrow()

        val proposed = saved.snapshot.entities.single { it.title == "Acme Robotics" }
        assertEquals("PROPOSED", proposed.reviewStatus)
        assertEquals("conversation-1", proposed.sourceConversationId)
        assertEquals("message-1", proposed.sourceMessageId)
        assertEquals("source-job", proposed.sourceJobId)
        assertTrue(proposed.sourceExcerpt.isNotBlank())
        assertEquals("NEEDS_REVIEW", saved.snapshot.jobs.single { it.id == prepared.jobId }.steps.single().status)
        assertEquals("OUTPUT_SAVED", saved.snapshot.workflowAttempts.single { it.jobId == prepared.jobId }.status)

        val postconditionRequest = EntityProposalWorkflowCoordinator.createPostconditionRequest(
            saved.snapshot, saved, request, runtimes
        )
        val observer = EntityProposalPostconditionObserver(saved.snapshot)
        val postcondition = runSuspendObserver { observer.observe(postconditionRequest) }
        postcondition.validateAgainst(postconditionRequest)
        assertEquals(WorkflowPostconditionOutcome.VERIFIED, postcondition.outcome)

        val completed = EntityProposalWorkflowCoordinator.complete(
            saved.snapshot, postconditionRequest, postcondition
        ).getOrThrow()
        val completedJob = completed.jobs.single { it.id == prepared.jobId }
        assertEquals("SUCCEEDED", completedJob.status)
        assertEquals("COMPLETED", completedJob.steps.single().status)
        assertTrue(completedJob.steps.single().evidence.contains("verification:"))
        assertTrue(completed.evidence.any { it.id == postcondition.evidenceId && it.kind == "WORKFLOW_VERIFICATION" })
        assertEquals("OUTPUT_SAVED", completed.workflowAttempts.single { it.jobId == prepared.jobId }.status)
        assertEquals("PROPOSED", completed.entities.single { it.id == proposed.id }.reviewStatus)
        assertTrue(completed.relationships.any {
            it.fromEntityId == "conversation-1" && it.toEntityId == proposed.id && it.relationType == "PROPOSES"
        })
        WorkflowAttemptJournal.validate(completed)
    }

    @Test fun restoresTheExactEncryptedInputAfterWorkspaceRecreation() {
        val initial = fixture()
        val prepared = EntityProposalWorkflowCoordinator.prepare(
            initial,
            "source-job",
            "conversation-1",
            listOf(proposal("Acme Robotics", "Acme Robotics currently uses a legacy ERP.")),
            runtimes
        ).getOrThrow()

        // Simulate reopening the encrypted workspace: only the saved snapshot and job ID survive.
        val restored = EntityProposalWorkflowCoordinator.restorePrepared(
            prepared.snapshot.copy(),
            prepared.jobId
        ).getOrThrow()
        assertEquals(prepared.inputEvidenceId, restored.inputEvidenceId)

        val started = WorkflowCheckpointCoordinator.start(
            restored.snapshot, restored.jobId, restored.taskId, runtimes, System.currentTimeMillis()
        ).getOrThrow()
        val request = EntityProposalWorkflowCoordinator.buildRequest(started, restored, runtimes)
        assertTrue(request.inputJson.contains("Acme Robotics currently uses a legacy ERP."))
        assertEquals("source-job", EntityProposalRuntimeCodec.parseInput(request.inputJson).sourceJobId)
    }

    @Test fun refusesToRestoreWhenTheExactEncryptedInputEvidenceIsMissing() {
        val prepared = EntityProposalWorkflowCoordinator.prepare(
            fixture(),
            "source-job",
            "conversation-1",
            listOf(proposal("Acme Robotics", "Acme Robotics currently uses a legacy ERP.")),
            runtimes
        ).getOrThrow()
        val withoutInput = prepared.snapshot.copy(
            evidence = prepared.snapshot.evidence.filterNot { it.id == prepared.inputEvidenceId }
        )
        val restored = EntityProposalWorkflowCoordinator.restorePrepared(withoutInput, prepared.jobId)
        assertTrue(restored.isFailure)
        assertTrue(restored.exceptionOrNull()?.message.orEmpty().contains("missing or ambiguous"))
    }

    @Test fun repeatedIdenticalProposalBatchesGetDistinctDurableInputRecords() {
        val initial = fixture()
        val proposals = listOf(proposal("Acme Robotics", "Acme Robotics currently uses a legacy ERP."))
        val first = EntityProposalWorkflowCoordinator.prepare(
            initial, "source-job", "conversation-1", proposals, runtimes
        ).getOrThrow()
        val second = EntityProposalWorkflowCoordinator.prepare(
            first.snapshot, "source-job", "conversation-1", proposals, runtimes
        ).getOrThrow()
        assertTrue(first.jobId != second.jobId)
        assertTrue(first.inputEvidenceId != second.inputEvidenceId)
        assertEquals(2, second.snapshot.evidence.count { it.kind == "WORKFLOW_INPUT" })
    }

    @Test fun rejectsEvidenceThatCannotBeLinkedToOneOriginalConversationMessage() {
        val result = EntityProposalWorkflowCoordinator.prepare(
            fixture(),
            "source-job",
            "conversation-1",
            listOf(proposal("Acme Robotics", "This text was not in the source conversation.")),
            runtimes
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("one individual source message"))
    }

    @Test fun skipsExistingDuplicateAndPersistsOnlyTheNewProposals() {
        val initial = fixture().copy(entities = fixture().entities + EntityRecord(
            id = "existing-organization",
            type = "ORGANIZATION",
            title = "Duplicate Systems",
            reviewStatus = "APPROVED"
        ))
        val proposals = listOf(
            proposal("Duplicate Systems", "Acme Robotics currently uses a legacy ERP."),
            proposal("Acme Robotics", "Their team has 40 employees")
        )
        val prepared = EntityProposalWorkflowCoordinator.prepare(
            initial, "source-job", "conversation-1", proposals, runtimes
        ).getOrThrow()
        val started = WorkflowCheckpointCoordinator.start(
            prepared.snapshot, prepared.jobId, prepared.taskId, runtimes, System.currentTimeMillis()
        ).getOrThrow()
        val request = EntityProposalWorkflowCoordinator.buildRequest(started, prepared, runtimes)
        val adapter = EntityProposalRuntimeAdapter(runtimes)
        val output = runSuspend {
            WorkflowRuntimeAdapterResolver.resolve(request, runtimes, listOf(adapter)).execute(request)
        } as WorkflowRuntimeResult.Output
        val saved = EntityProposalWorkflowCoordinator.saveOutput(
            started, request, output.value, output.completedAt
        ).getOrThrow()
        assertEquals(1, saved.savedCount)
        assertEquals(1, saved.duplicateCount)
        assertTrue(saved.snapshot.entities.any { it.title == "Acme Robotics" && it.reviewStatus == "PROPOSED" })
        assertEquals(1, saved.snapshot.entities.count { it.title == "Duplicate Systems" })
    }

    @Test fun missingPersistedProposalCannotPassTheIndependentPostcondition() {
        val prepared = EntityProposalWorkflowCoordinator.prepare(
            fixture(),
            "source-job",
            "conversation-1",
            listOf(proposal("Acme Robotics", "Acme Robotics currently uses a legacy ERP.")),
            runtimes
        ).getOrThrow()
        val started = WorkflowCheckpointCoordinator.start(
            prepared.snapshot, prepared.jobId, prepared.taskId, runtimes, System.currentTimeMillis()
        ).getOrThrow()
        val request = EntityProposalWorkflowCoordinator.buildRequest(started, prepared, runtimes)
        val adapter = EntityProposalRuntimeAdapter(runtimes)
        val output = runSuspend {
            WorkflowRuntimeAdapterResolver.resolve(request, runtimes, listOf(adapter)).execute(request)
        } as WorkflowRuntimeResult.Output
        val saved = EntityProposalWorkflowCoordinator.saveOutput(
            started, request, output.value, output.completedAt
        ).getOrThrow()
        val tampered = saved.snapshot.copy(entities = saved.snapshot.entities.filterNot { it.type == "ORGANIZATION" })
        val postconditionRequest = EntityProposalWorkflowCoordinator.createPostconditionRequest(
            tampered, saved.copy(snapshot = tampered), request, runtimes
        )
        val postcondition = runSuspendObserver {
            EntityProposalPostconditionObserver(tampered).observe(postconditionRequest)
        }
        assertFalse(postcondition.permitsCheckpointCompletion)
        val completion = EntityProposalWorkflowCoordinator.complete(tampered, postconditionRequest, postcondition)
        assertTrue(completion.isFailure)
    }
}
