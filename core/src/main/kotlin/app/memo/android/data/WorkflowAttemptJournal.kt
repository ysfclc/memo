package app.memo.android.data

import app.memo.android.orchestration.WorkflowGraph
import java.security.MessageDigest

/**
 * Durable journal entry for one execution attempt of one workflow checkpoint.
 *
 * Attempt status is intentionally separate from checkpoint status: OUTPUT_SAVED means a
 * runtime returned and its artifact was persisted, not that the workflow postcondition passed.
 */
data class WorkflowAttemptRecord(
    val id: String = newId(),
    val jobId: String,
    val stepId: String,
    val attemptNumber: Int,
    val idempotencyKey: String,
    val runtimeId: String,
    val capabilityIds: List<String> = emptyList(),
    val status: String = "RUNNING",
    val startedAt: Long,
    val finishedAt: Long = 0L,
    val evidenceId: String = "",
    val detail: String = "",
    val userOutcome: String = "",
    val userOutcomeAt: Long = 0L
)

/**
 * Pure state transitions and integrity checks for the per-node execution journal.
 * This journal records attempts; it never dispatches a runtime or authorizes side effects.
 */
object WorkflowAttemptJournal {
    const val MAX_ATTEMPTS = 2_000
    const val MAX_ATTEMPTS_PER_STEP = 4
    private val statuses = setOf("RUNNING", "OUTPUT_SAVED", "FAILED", "INTERRUPTED", "USER_REPORTED")

    fun create(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        runtimeId: String,
        capabilityIds: List<String>,
        idempotencyKey: String,
        now: Long
    ): WorkflowAttemptRecord {
        require(snapshot.workflowAttempts.size < MAX_ATTEMPTS) {
            "Workflow attempt history is full. Export a workspace backup before starting more work."
        }
        require(now > 0L) { "Attempt start time is invalid." }
        require(runtimeId.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}"))) { "Attempt runtime ID is invalid." }
        require(capabilityIds.size <= 20 && capabilityIds.distinct().size == capabilityIds.size &&
            capabilityIds.all { it.matches(Regex("[a-z0-9._-]{1,120}")) }) {
            "Attempt capability manifest is invalid."
        }
        require(idempotencyKey.matches(Regex("[0-9a-f]{64}"))) { "Attempt idempotency key is invalid." }
        val job = snapshot.jobs.firstOrNull { it.id == jobId } ?: error("This work plan no longer exists.")
        require(job.steps.any { it.id == stepId }) { "This checkpoint no longer exists." }
        val previous = snapshot.workflowAttempts.filter { it.jobId == jobId && it.stepId == stepId }
        require(previous.none { it.status == "RUNNING" }) { "This checkpoint already has a running attempt." }
        val previousTerminalAt = previous.maxOfOrNull { maxOf(it.startedAt, it.finishedAt, it.userOutcomeAt) } ?: 0L
        require(now >= previousTerminalAt) { "Attempt start time precedes the previous attempt's recorded outcome." }
        require(previous.size < MAX_ATTEMPTS_PER_STEP) {
            "This checkpoint has reached its four-attempt safety limit. Create a reviewed new plan instead of replaying it."
        }
        require(previous.all { it.idempotencyKey == idempotencyKey }) {
            "This checkpoint's idempotency identity changed between attempts. Review or rebuild the plan."
        }
        return WorkflowAttemptRecord(
            jobId = jobId,
            stepId = stepId,
            attemptNumber = (previous.maxOfOrNull { it.attemptNumber } ?: 0) + 1,
            idempotencyKey = idempotencyKey,
            runtimeId = runtimeId,
            capabilityIds = capabilityIds,
            startedAt = now
        )
    }

    fun outputSaved(
        attempts: List<WorkflowAttemptRecord>,
        jobId: String,
        stepId: String,
        evidenceId: String,
        now: Long
    ): List<WorkflowAttemptRecord> = finish(
        attempts, jobId, stepId, "OUTPUT_SAVED", evidenceId, "Runtime output saved for human review.", now
    )

    fun failed(
        attempts: List<WorkflowAttemptRecord>,
        jobId: String,
        stepId: String,
        detail: String,
        now: Long
    ): List<WorkflowAttemptRecord> = finish(
        attempts, jobId, stepId, "FAILED", "", detail, now
    )

    private fun finish(
        attempts: List<WorkflowAttemptRecord>,
        jobId: String,
        stepId: String,
        status: String,
        evidenceId: String,
        detail: String,
        now: Long
    ): List<WorkflowAttemptRecord> {
        require(now > 0L) { "Attempt finish time is invalid." }
        val active = attempts.filter { it.jobId == jobId && it.stepId == stepId && it.status == "RUNNING" }
        require(active.size == 1) { "There is no single active attempt for this checkpoint." }
        val attempt = active.single()
        require(now >= attempt.startedAt) { "Attempt finish time precedes its start." }
        return attempts.map { candidate ->
            if (candidate.id == attempt.id) candidate.copy(
                status = status,
                finishedAt = now,
                evidenceId = evidenceId,
                detail = detail.trim().take(2_000)
            ) else candidate
        }
    }

    fun recordUserOutcome(
        attempts: List<WorkflowAttemptRecord>,
        jobId: String,
        stepId: String,
        outcome: String,
        now: Long
    ): List<WorkflowAttemptRecord> {
        val clean = outcome.trim().take(2_000)
        if (clean.isBlank()) return attempts
        val latest = attempts.filter { it.jobId == jobId && it.stepId == stepId }
            .maxByOrNull { it.attemptNumber } ?: return attempts
        require(latest.status != "RUNNING" || latest.runtimeId != "app.models") {
            "A local-model inference attempt must finish before its output can be reviewed."
        }
        require(now >= latest.startedAt && (latest.status == "RUNNING" || now >= latest.finishedAt)) {
            "User outcome time is invalid."
        }
        return attempts.map { candidate ->
            if (candidate.id == latest.id) candidate.copy(
                status = if (latest.status == "RUNNING") "USER_REPORTED" else latest.status,
                finishedAt = if (latest.status == "RUNNING") now else latest.finishedAt,
                detail = if (latest.status == "RUNNING") "User-reported outcome; runtime postcondition was not independently verified." else latest.detail,
                userOutcome = clean,
                userOutcomeAt = now
            ) else candidate
        }
    }

    fun recover(attempts: List<WorkflowAttemptRecord>, now: Long): List<WorkflowAttemptRecord> {
        if (attempts.none { it.status == "RUNNING" }) return attempts
        return attempts.map { attempt ->
            if (attempt.status == "RUNNING") attempt.copy(
                status = "INTERRUPTED",
                finishedAt = maxOf(now, attempt.startedAt),
                detail = "Memo stopped before this attempt recorded a result. Review the checkpoint before retrying."
            ) else attempt
        }
    }

    fun validate(snapshot: AppSnapshot) {
        require(snapshot.workflowAttempts.size <= MAX_ATTEMPTS) { "Workflow attempt history exceeds its safe storage limit." }
        require(snapshot.workflowAttempts.map { it.id }.distinct().size == snapshot.workflowAttempts.size) {
            "Workflow attempt identities must be unique."
        }
        val jobs = snapshot.jobs.associateBy { it.id }
        val evidence = snapshot.evidence.associateBy { it.id }
        snapshot.workflowAttempts.groupBy { it.jobId to it.stepId }.forEach { (key, rows) ->
            val (jobId, stepId) = key
            val job = jobs[jobId] ?: error("A workflow attempt references a missing work plan.")
            val step = job.steps.firstOrNull { it.id == stepId }
                ?: error("A workflow attempt references a missing checkpoint.")
            val ordered = rows.sortedBy { it.attemptNumber }
            require(ordered.map { it.attemptNumber } == (1..ordered.size).toList() &&
                ordered.size <= MAX_ATTEMPTS_PER_STEP) {
                "Workflow attempt numbers are duplicated, missing or exceed the retry safety limit."
            }
            require(ordered.map { it.idempotencyKey }.distinct().size == 1) {
                "Workflow attempts for one checkpoint must keep the same idempotency identity."
            }
            var previousTerminalAt = 0L
            ordered.forEach { attempt ->
                require(attempt.startedAt >= previousTerminalAt) {
                    "Workflow attempts for one checkpoint overlap or are out of chronological order."
                }
                require(attempt.id.isNotBlank() && attempt.id.length <= 128 &&
                    attempt.jobId == jobId && attempt.stepId == stepId) { "Workflow attempt identity is invalid." }
                require(attempt.idempotencyKey.matches(Regex("[0-9a-f]{64}"))) { "Workflow attempt idempotency key is invalid." }
                require(attempt.runtimeId.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}"))) { "Workflow attempt runtime ID is invalid." }
                require(attempt.capabilityIds.size <= 20 && attempt.capabilityIds.distinct().size == attempt.capabilityIds.size &&
                    attempt.capabilityIds.all { it.matches(Regex("[a-z0-9._-]{1,120}")) }) {
                    "Workflow attempt capabilities are invalid."
                }
                require(attempt.status in statuses && attempt.startedAt > 0L && attempt.startedAt >= previousStart &&
                    attempt.finishedAt >= 0L && attempt.detail.length <= 2_000 &&
                    attempt.userOutcome.length <= 2_000 && attempt.evidenceId.length <= 128) {
                    "Workflow attempt fields are invalid."
                }
                if (attempt.status == "RUNNING") {
                    require(attempt.finishedAt == 0L && attempt.evidenceId.isBlank() &&
                        attempt.userOutcome.isBlank() && attempt.userOutcomeAt == 0L &&
                        step.status == "RUNNING" && job.status == "RUNNING") {
                        "A running attempt must match an active checkpoint and cannot contain a finished result."
                    }
                } else {
                    require(attempt.finishedAt >= attempt.startedAt) { "Workflow attempt finish time precedes its start." }
                    require(step.status in setOf("NEEDS_REVIEW", "COMPLETED", "RUNNING")) {
                        "A finished attempt does not match the checkpoint lifecycle."
                    }
                }
                if (attempt.status == "OUTPUT_SAVED") {
                    val item = evidence[attempt.evidenceId]
                        ?: error("A saved workflow attempt references missing evidence.")
                    val metadata = runCatching { org.json.JSONObject(item.metadata) }.getOrNull()
                    require(item.taskId == jobId && item.kind == "WORK_ARTIFACT" &&
                        metadata != null && metadata.optString("jobId") == jobId &&
                        metadata.optString("stepId") == stepId &&
                        metadata.optBoolean("requiresHumanReview", false)) {
                        "A saved workflow attempt must reference its exact checkpoint-bound review artifact."
                    }
                } else {
                    require(attempt.evidenceId.isBlank()) { "Only an output-saved attempt may reference an artifact." }
                }
                if (attempt.userOutcome.isBlank()) {
                    require(attempt.userOutcomeAt == 0L && attempt.status != "USER_REPORTED") {
                        "An empty user outcome cannot have a timestamp or user-reported status."
                    }
                } else {
                    require(attempt.userOutcomeAt >= attempt.finishedAt && attempt.userOutcomeAt > 0L) {
                        "A user-reported outcome timestamp is invalid."
                    }
                }
                previousTerminalAt = maxOf(attempt.startedAt, attempt.finishedAt, attempt.userOutcomeAt)
                if (attempt.status == "USER_REPORTED") {
                    require(step.status == "COMPLETED") { "A user-reported attempt must belong to a completed checkpoint." }
                }
                val graph = job.workflowGraphJson.takeIf { it.isNotBlank() }?.let { WorkflowGraph.fromJson(it) }
                val node = graph?.nodes?.firstOrNull { it.id == stepId }
                if (graph != null) {
                    require(node != null && !node.externalWrite && !node.externalSideEffect &&
                        !node.requiresApproval && node.kind != app.memo.android.orchestration.WorkflowNodeKind.APPROVAL &&
                        node.runtimeId != "app.approvals" && attempt.runtimeId == node.runtimeId &&
                        attempt.capabilityIds == node.requiredCapabilities &&
                        attempt.idempotencyKey == node.idempotencyKey) {
                        "A workflow attempt does not match its saved safe TaskSpec."
                    }
                } else {
                    require(attempt.runtimeId == job.runtimeId.ifBlank { "app.jobs" } &&
                        attempt.capabilityIds.isEmpty() &&
                        attempt.idempotencyKey == legacyIdempotencyKey(jobId, stepId)) {
                        "A legacy workflow attempt does not match its checkpoint identity."
                    }
                }
            }
        }
    }

    fun legacyIdempotencyKey(jobId: String, stepId: String): String =
        sha256("memo-legacy-workflow:$jobId:$stepId")

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
