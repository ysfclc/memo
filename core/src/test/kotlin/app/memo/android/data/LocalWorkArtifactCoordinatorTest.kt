package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalWorkArtifactCoordinatorTest {
    private fun snapshot(status: String = "RUNNING"): AppSnapshot {
        val step = JobStepRecord(id = "step-1", title = "Prepare deliverable", status = status)
        val initial = AppSnapshot(jobs = listOf(WorkJobRecord(
            id = "job-1",
            title = "Prepare a report",
            goal = "Create a sourced report",
            status = if (status == "RUNNING") "RUNNING" else "WAITING_FOR_USER",
            steps = listOf(step)
        )))
        if (status != "RUNNING") return initial
        val attempt = WorkflowAttemptJournal.create(
            initial, "job-1", "step-1", "app.jobs", emptyList(),
            WorkflowAttemptJournal.legacyIdempotencyKey("job-1", "step-1"), 10L
        )
        return initial.copy(workflowAttempts = listOf(attempt))
    }

    private fun artifact(id: String = "evidence-1", jobId: String = "job-1") = EvidenceRecord(
        id = id,
        taskId = jobId,
        title = "Draft report",
        sourceUrl = "",
        kind = "WORK_ARTIFACT",
        content = "A draft that requires review.",
        summary = "Draft report",
        metadata = org.json.JSONObject()
            .put("jobId", jobId)
            .put("stepId", "step-1")
            .put("modelId", "local:test")
            .put("requiresHumanReview", true)
            .toString()
    )

    @Test fun savesDraftAndMovesStepAndJobToReviewInOneSnapshot() {
        val result = LocalWorkArtifactCoordinator.saveForReview(snapshot(), "job-1", "step-1", artifact(), 42L).getOrThrow()
        assertEquals("evidence-1", result.evidence.single().id)
        assertEquals("NEEDS_REVIEW", result.jobs.single().steps.single().status)
        assertEquals("WAITING_FOR_USER", result.jobs.single().status)
        assertTrue(result.jobs.single().steps.single().evidence.contains("evidence:evidence-1"))
        assertEquals(42L, result.activity.first().at)
        assertEquals("OUTPUT_SAVED", result.workflowAttempts.single().status)
        assertEquals("evidence-1", result.workflowAttempts.single().evidenceId)
        assertEquals(42L, result.workflowAttempts.single().finishedAt)
        val receipt = WorkflowRuntimeReceipt.fromEvidence(result.evidence.single())
        assertTrue(receipt != null)
        assertEquals("job-1", receipt?.jobId)
        assertEquals("step-1", receipt?.stepId)
        assertEquals("app.jobs", receipt?.runtimeId)
        assertEquals(10L, receipt?.startedAt)
        assertEquals(42L, receipt?.completedAt)
        assertEquals("NOT_VERIFIED", receipt?.postconditionStatus)
        receipt?.validateAgainst(result.workflowAttempts.single(), result.evidence.single())
        WorkflowAttemptJournal.validate(result)
    }

    @Test fun legacySchemaSevenOutputAttemptsWithoutReceiptsRemainValid() {
        val initial = snapshot()
        val job = initial.jobs.single().copy(
            status = "WAITING_FOR_USER",
            steps = listOf(initial.jobs.single().steps.single().copy(status = "NEEDS_REVIEW"))
        )
        val attempt = initial.workflowAttempts.single().copy(
            status = "OUTPUT_SAVED",
            finishedAt = 42L,
            evidenceId = "evidence-legacy",
            detail = "Runtime output saved for human review."
        )
        val legacy = initial.copy(
            jobs = listOf(job),
            evidence = listOf(artifact(id = "evidence-legacy")),
            workflowAttempts = listOf(attempt)
        )
        WorkflowAttemptJournal.validate(legacy)
    }

    @Test fun receiptBindsTheExactSavedBytesAndRejectsLaterArtifactMutation() {
        val saved = LocalWorkArtifactCoordinator.saveForReview(
            snapshot(), "job-1", "step-1", artifact(), 42L
        ).getOrThrow()
        val mutated = saved.copy(evidence = listOf(saved.evidence.single().copy(content = "different output")))
        assertTrue(runCatching { WorkflowAttemptJournal.validate(mutated) }.isFailure)
    }

    @Test fun refusesToSaveOutputWhenThereIsNoUniqueActiveRuntimeAttempt() {
        val noAttempt = snapshot().copy(workflowAttempts = emptyList())
        val result = LocalWorkArtifactCoordinator.saveForReview(noAttempt, "job-1", "step-1", artifact(), 42L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("active runtime attempt"))
    }

    @Test fun refusesToAttachAnArtifactToAStaleCheckpoint() {
        val result = LocalWorkArtifactCoordinator.saveForReview(snapshot("NEEDS_REVIEW"), "job-1", "step-1", artifact(), 42L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("no longer active"))
        assertTrue(snapshot("NEEDS_REVIEW").evidence.isEmpty())
    }

    @Test fun refusesAnArtifactBoundToAnotherCheckpointInTheSameJob() {
        val wrongStep = artifact().copy(metadata = org.json.JSONObject()
            .put("jobId", "job-1")
            .put("stepId", "another-step")
            .put("requiresHumanReview", true)
            .toString())
        val result = LocalWorkArtifactCoordinator.saveForReview(
            snapshot(), "job-1", "step-1", wrongStep, 42L
        )
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("exact checkpoint"))
    }

    @Test fun refusesAnArtifactThatBelongsToAnotherJob() {
        val result = LocalWorkArtifactCoordinator.saveForReview(snapshot(), "job-1", "step-1", artifact(jobId = "job-2"), 42L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("not linked"))
    }

    @Test fun refusesDuplicateEvidenceIdentity() {
        val initial = snapshot().copy(evidence = listOf(artifact()))
        val result = LocalWorkArtifactCoordinator.saveForReview(initial, "job-1", "step-1", artifact(), 42L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("already exists"))
    }

    @Test fun failureTransitionMovesOnlyTheActiveStepToReview() {
        val initial = snapshot().copy(jobs = listOf(snapshot().jobs.single().copy(steps = listOf(
            JobStepRecord(id = "step-1", title = "Prepare deliverable", status = "RUNNING"),
            JobStepRecord(id = "step-2", title = "Next checkpoint", status = "PENDING")
        ))))
        val result = LocalWorkArtifactCoordinator.markNeedsReview(initial, "job-1", "step-1", "Model failed.", "No draft was saved.", 99L).getOrThrow()
        assertEquals("NEEDS_REVIEW", result.jobs.single().steps[0].status)
        assertEquals("PENDING", result.jobs.single().steps[1].status)
        assertEquals("WAITING_FOR_USER", result.jobs.single().status)
        assertEquals("FAILED", result.workflowAttempts.single().status)
        assertEquals(99L, result.workflowAttempts.single().finishedAt)
        assertFalse(result.evidence.isNotEmpty())
    }

    @Test fun refusesFailureTransitionForACheckpointThatIsNotRunning() {
        val result = LocalWorkArtifactCoordinator.markNeedsReview(snapshot("PENDING"), "job-1", "step-1", "Model failed.", "No draft.", 99L)
        assertTrue(result.isFailure)
    }
}
