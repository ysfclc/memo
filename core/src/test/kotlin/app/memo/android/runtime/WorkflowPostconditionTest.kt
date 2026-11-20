package app.memo.android.runtime

import app.memo.android.data.EvidenceRecord
import app.memo.android.data.WorkflowAttemptRecord
import app.memo.android.data.WorkflowRuntimeReceipt
import app.memo.android.orchestration.TaskSpec
import app.memo.android.orchestration.WorkflowNodeKind
import app.memo.android.orchestration.WorkflowParticipant
import org.json.JSONObject
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowPostconditionTest {
    private val runtimes = RuntimeRegistry().listRuntimes()
    private val task = TaskSpec(
        id = "draft",
        title = "Prepare a draft",
        kind = WorkflowNodeKind.ARTIFACT,
        participant = WorkflowParticipant.LOCAL_MODEL,
        runtimeId = "app.models",
        requiredCapabilities = listOf("model.generate"),
        dependsOn = emptyList(),
        inputContract = "Approved source context",
        outputContract = "A reviewable draft",
        postcondition = "A draft artifact is saved for review.",
        timeoutMs = 500,
        maxRetries = 0,
        idempotencyKey = "a".repeat(64)
    )
    private val observer = WorkflowPostconditionObserverDescriptor(
        id = "memo.workspace-record-observer.v1",
        runtimeId = "app.jobs",
        capabilityId = "job.verify",
        method = WorkflowPostconditionMethod.MEMO_RECORD_LOOKUP
    )

    private data class Fixture(
        val request: WorkflowPostconditionRequest,
        val attempt: WorkflowAttemptRecord
    )

    private fun outputEvidence() = EvidenceRecord(
        id = "output-evidence",
        taskId = "job-1",
        title = "Draft output",
        sourceUrl = "",
        kind = "WORK_ARTIFACT",
        content = "Draft content",
        summary = "Review this draft",
        metadata = JSONObject()
            .put("jobId", "job-1")
            .put("stepId", task.id)
            .put("requiresHumanReview", true)
            .toString()
    )

    private fun fixture(): Fixture {
        val runningAttempt = WorkflowAttemptRecord(
            id = "attempt-1",
            jobId = "job-1",
            stepId = task.id,
            attemptNumber = 1,
            idempotencyKey = task.idempotencyKey,
            runtimeId = task.runtimeId,
            capabilityIds = task.requiredCapabilities,
            status = "RUNNING",
            startedAt = 100L
        )
        val outputEvidence = outputEvidence()
        val receipt = WorkflowRuntimeReceipt.create(runningAttempt, outputEvidence, 200L)
        val savedAttempt = runningAttempt.copy(
            status = "OUTPUT_SAVED",
            finishedAt = 200L,
            evidenceId = outputEvidence.id,
            detail = "Runtime output saved for human review. ${WorkflowRuntimeReceipt.DETAIL_MARKER}"
        )
        val request = WorkflowPostconditionRequest.create(
            task = task,
            attempt = savedAttempt,
            runtimeReceipt = receipt,
            outputEvidence = outputEvidence,
            observer = observer,
            registeredRuntimes = runtimes,
            registeredObservers = listOf(observer)
        )
        return Fixture(request, savedAttempt)
    }

    private fun evidence(
        request: WorkflowPostconditionRequest,
        outcome: WorkflowPostconditionOutcome = WorkflowPostconditionOutcome.VERIFIED,
        evidenceId: String = "verification-evidence",
        evidenceSha256: String = "c".repeat(64)
    ) = WorkflowPostconditionEvidence(
        attemptId = request.attempt.id,
        jobId = request.attempt.jobId,
        stepId = request.task.id,
        taskRuntimeId = request.task.runtimeId,
        taskCapabilityIds = request.task.requiredCapabilities,
        idempotencyKey = request.task.idempotencyKey,
        taskSpecSha256 = request.taskSpecSha256,
        postconditionSha256 = request.postconditionSha256,
        observerId = request.observer.id,
        observerRuntimeId = request.observer.runtimeId,
        observerCapabilityId = request.observer.capabilityId,
        method = request.observer.method,
        outcome = outcome,
        evidenceId = evidenceId,
        evidenceSha256 = evidenceSha256,
        observedAt = 250L,
        detail = "The saved workspace record was independently inspected."
    )

    @Test fun postconditionRequestRequiresAValidatedSavedOutputAndRegisteredObserver() {
        val fixture = fixture()
        assertTrue(runCatching {
            WorkflowPostconditionRequest.create(
                task, fixture.attempt, fixture.request.runtimeReceipt,
                outputEvidence().copy(content = "Mutated content"),
                observer, runtimes, listOf(observer)
            )
        }.isFailure)
        assertTrue(runCatching {
            WorkflowPostconditionRequest.create(
                task, fixture.attempt, fixture.request.runtimeReceipt,
                outputEvidence(), observer, runtimes, emptyList()
            )
        }.isFailure)
    }

    @Test fun postconditionEvidenceIsBoundToTheExactTaskAttemptAndObserver() {
        val request = fixture().request
        val evidence = evidence(request)
        evidence.validateAgainst(request)
        assertTrue(evidence.permitsCheckpointCompletion)
        assertTrue(runCatching {
            evidence.copy(attemptId = "different-attempt").validateAgainst(request)
        }.isFailure)
        assertTrue(runCatching {
            evidence.copy(postconditionSha256 = "d".repeat(64)).validateAgainst(request)
        }.isFailure)
        assertTrue(runCatching {
            evidence.copy(observerCapabilityId = "model.generate").validateAgainst(request)
        }.isFailure)
    }

    @Test fun postconditionObserverMustBeIndependentAndReadOnly() {
        val fixture = fixture()
        val sameCapability = observer.copy(
            id = "memo.same-capability-observer",
            runtimeId = "app.models",
            capabilityId = "model.generate"
        )
        assertTrue(runCatching {
            WorkflowPostconditionRequest.create(
                task, fixture.attempt, fixture.request.runtimeReceipt,
                outputEvidence(), sameCapability, runtimes, listOf(sameCapability)
            )
        }.isFailure)

        val unknown = observer.copy(
            id = "memo.unknown-observer",
            runtimeId = "runtime.unknown",
            capabilityId = "unknown.verify"
        )
        assertTrue(runCatching {
            WorkflowPostconditionRequest.create(
                task, fixture.attempt, fixture.request.runtimeReceipt,
                outputEvidence(), unknown, runtimes, listOf(unknown)
            )
        }.isFailure)
    }

    @Test fun nonVerifiedOutcomesNeverPermitCheckpointCompletion() {
        val request = fixture().request
        val notVerified = evidence(
            request,
            outcome = WorkflowPostconditionOutcome.NOT_VERIFIED,
            evidenceId = "",
            evidenceSha256 = ""
        )
        notVerified.validateAgainst(request)
        assertFalse(notVerified.permitsCheckpointCompletion)

        val contradicted = evidence(request, outcome = WorkflowPostconditionOutcome.CONTRADICTED)
        contradicted.validateAgainst(request)
        assertFalse(contradicted.permitsCheckpointCompletion)

        val needsReview = evidence(request, outcome = WorkflowPostconditionOutcome.NEEDS_REVIEW)
        needsReview.validateAgainst(request)
        assertFalse(needsReview.permitsCheckpointCompletion)
    }
}
