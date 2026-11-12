package app.memo.android.data

/**
 * Pure, bounded state transitions for local-model Work execution.
 *
 * A model response is an artifact draft, never proof that a workflow postcondition passed.
 * The artifact and review transition are returned as one immutable snapshot so callers can
 * persist them together.
 */
object LocalWorkArtifactCoordinator {
    private const val MAX_EVIDENCE = 150

    fun saveForReview(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        evidence: EvidenceRecord,
        now: Long
    ): Result<AppSnapshot> = runCatching {
        require(evidence.kind == "WORK_ARTIFACT" && evidence.taskId == jobId) {
            "The draft artifact is not linked to this work plan."
        }
        val job = snapshot.jobs.firstOrNull { it.id == jobId }
            ?: error("This work checkpoint no longer exists.")
        val step = job.steps.firstOrNull { it.id == stepId }
            ?: error("This work checkpoint no longer exists.")
        require(step.status == "RUNNING") {
            "This checkpoint is no longer active. Reopen Work before saving the draft."
        }
        require(snapshot.evidence.size < MAX_EVIDENCE) {
            "Evidence storage is full. Export a workspace backup and remove older evidence before creating another artifact."
        }
        require(snapshot.evidence.none { it.id == evidence.id }) {
            "An artifact with this identity already exists."
        }

        val modelId = runCatching { org.json.JSONObject(evidence.metadata).optString("modelId") }.getOrDefault("")
        val updatedStep = step.copy(
            status = "NEEDS_REVIEW",
            detail = "Local model produced a draft artifact. Review it before recording the checkpoint outcome.",
            evidence = "evidence:${evidence.id}; model:$modelId",
            updatedAt = now
        )
        val updatedJob = job.copy(
            status = "WAITING_FOR_USER",
            steps = job.steps.map { if (it.id == stepId) updatedStep else it },
            resultSummary = "A local draft was saved to Evidence. Review the actual output before continuing; it is not an independently verified result.",
            updatedAt = now
        )
        val attempts = WorkflowAttemptJournal.outputSaved(
            snapshot.workflowAttempts, jobId, stepId, evidence.id, now
        )
        snapshot.copy(
            evidence = listOf(evidence) + snapshot.evidence,
            workflowAttempts = attempts,
            jobs = snapshot.jobs.map { if (it.id == jobId) updatedJob else it },
            activity = (listOf(ActivityRecord(
                at = now,
                message = "Local Work draft saved for review: ${evidence.title}",
                taskId = jobId
            )) + snapshot.activity).take(500)
        )
    }

    fun markNeedsReview(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        detail: String,
        resultSummary: String,
        now: Long
    ): Result<AppSnapshot> = runCatching {
        val job = snapshot.jobs.firstOrNull { it.id == jobId }
            ?: error("This work checkpoint no longer exists.")
        require(job.steps.any { it.id == stepId && it.status == "RUNNING" }) {
            "This checkpoint is no longer active."
        }
        val attempts = WorkflowAttemptJournal.failed(snapshot.workflowAttempts, jobId, stepId, detail, now)
        val updatedJob = job.copy(
            status = "WAITING_FOR_USER",
            steps = job.steps.map { step ->
                if (step.id == stepId) step.copy(
                    status = "NEEDS_REVIEW",
                    detail = detail.trim().take(2_000),
                    updatedAt = now
                ) else step
            },
            resultSummary = resultSummary.trim().take(4_000),
            updatedAt = now
        )
        snapshot.copy(
            workflowAttempts = attempts,
            jobs = snapshot.jobs.map { if (it.id == jobId) updatedJob else it },
            activity = (listOf(ActivityRecord(
                at = now,
                level = "WARN",
                message = "Local Work checkpoint needs review: ${stepId}",
                taskId = jobId
            )) + snapshot.activity).take(500)
        )
    }
}
