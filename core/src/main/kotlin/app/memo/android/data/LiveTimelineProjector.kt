package app.memo.android.data

/**
 * A read-only projection of durable workspace records into one chronological Live feed.
 *
 * Entries describe recorded state, not proof that an external side effect succeeded.
 * Only persisted activity, job/step, task, action-audit and evidence records are included.
 */
enum class LiveTimelineKind(val label: String) {
    ACTIVITY("Workspace event"),
    JOB("Work plan"),
    JOB_STEP("Checkpoint"),
    ATTEMPT("Execution attempt"),
    TASK("Task"),
    ACTION("External action"),
    EVIDENCE("Evidence")
}

data class LiveTimelineEntry(
    val id: String,
    val occurredAt: Long,
    val kind: LiveTimelineKind,
    val title: String,
    val detail: String,
    val status: String,
    /** ID in the owning record collection; never an inferred deep link. */
    val relatedId: String = "",
    val severity: String = "INFO"
)

object LiveTimelineProjector {
    const val DEFAULT_LIMIT = 120
    const val MAX_LIMIT = 500

    fun build(snapshot: AppSnapshot, limit: Int = DEFAULT_LIMIT): List<LiveTimelineEntry> {
        require(limit in 1..MAX_LIMIT) { "Live timeline limit must be between 1 and $MAX_LIMIT." }

        val entries = mutableListOf<LiveTimelineEntry>()

        snapshot.activity.forEach { event ->
            entries += LiveTimelineEntry(
                id = "activity:${event.id}",
                occurredAt = event.at,
                kind = LiveTimelineKind.ACTIVITY,
                title = event.message,
                detail = if (event.taskId.isBlank()) "Workspace activity" else "Task reference: ${event.taskId}",
                status = event.level,
                relatedId = event.taskId,
                severity = event.level
            )
        }

        snapshot.jobs.forEach { job ->
            val activeStep = job.steps.firstOrNull { it.status in setOf("RUNNING", "NEEDS_REVIEW") }
            val summary = job.resultSummary.ifBlank { job.goal.substringBefore("\n\nMemo route:").take(220) }
            val detail = buildList {
                if (summary.isNotBlank()) add(summary)
                if (activeStep != null) add("Current checkpoint: ${activeStep.title}")
                add("${job.steps.count { it.status == "COMPLETED" }}/${job.steps.size} checkpoints completed")
            }.joinToString(" · ")
            entries += LiveTimelineEntry(
                id = "job:${job.id}",
                occurredAt = job.updatedAt,
                kind = LiveTimelineKind.JOB,
                title = job.title,
                detail = detail,
                status = job.status,
                relatedId = job.id
            )
            job.steps.forEach { step ->
                entries += LiveTimelineEntry(
                    id = "step:${job.id}:${step.id}",
                    occurredAt = step.updatedAt,
                    kind = LiveTimelineKind.JOB_STEP,
                    title = step.title,
                    detail = listOf(job.title, step.detail).filter { it.isNotBlank() }.joinToString(" · "),
                    status = step.status,
                    relatedId = job.id,
                    severity = if (step.status == "NEEDS_REVIEW") "WARN" else "INFO"
                )
            }
        }

        val jobsById = snapshot.jobs.associateBy { it.id }
        snapshot.workflowAttempts.forEach { attempt ->
            val job = jobsById[attempt.jobId]
            val step = job?.steps?.firstOrNull { it.id == attempt.stepId }
            entries += LiveTimelineEntry(
                id = "attempt:${attempt.id}",
                occurredAt = attempt.finishedAt.takeIf { it > 0L } ?: attempt.startedAt,
                kind = LiveTimelineKind.ATTEMPT,
                title = "${step?.title ?: attempt.stepId} · Attempt ${attempt.attemptNumber}",
                detail = listOf(
                    job?.title,
                    attempt.runtimeId,
                    attempt.capabilityIds.joinToString(", "),
                    attempt.detail.take(500),
                    attempt.evidenceId.takeIf { it.isNotBlank() }?.let { "Evidence: $it" },
                    attempt.userOutcome.takeIf { it.isNotBlank() }?.let { "User-reported: $it" }
                ).filter { !it.isNullOrBlank() }.joinToString(" · "),
                status = attempt.status,
                relatedId = attempt.jobId,
                severity = if (attempt.status in setOf("FAILED", "INTERRUPTED")) "WARN" else "INFO"
            )
        }

        snapshot.tasks.forEach { task ->
            entries += LiveTimelineEntry(
                id = "task:${task.id}",
                occurredAt = task.updatedAt,
                kind = LiveTimelineKind.TASK,
                title = task.title,
                detail = task.lastMessage,
                status = task.status.name,
                relatedId = task.id,
                severity = when (task.status) {
                    TaskStatus.FAILED, TaskStatus.BLOCKED -> "ERROR"
                    TaskStatus.NEEDS_REVIEW -> "WARN"
                    else -> "INFO"
                }
            )
        }

        val actionsById = snapshot.actionApprovals.associateBy { it.id }
        val auditedActionIds = snapshot.actionAuditEvents.mapTo(mutableSetOf()) { it.actionId }
        snapshot.actionAuditEvents.forEach { event ->
            val action = actionsById[event.actionId]
            entries += LiveTimelineEntry(
                id = "action-event:${event.id}",
                occurredAt = event.at,
                kind = LiveTimelineKind.ACTION,
                title = action?.title?.takeIf { it.isNotBlank() } ?: event.eventType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() },
                detail = listOf(event.eventType.replace('_', ' '), event.destination, "${event.runtimeId} · ${event.status}")
                    .filter { it.isNotBlank() }.joinToString(" · "),
                status = event.status,
                relatedId = event.actionId,
                severity = if (event.status in setOf("NEEDS_REVIEW", "FAILED", "BLOCKED")) "WARN" else "INFO"
            )
        }
        // Older/imported snapshots may contain an action record without its audit events.
        snapshot.actionApprovals.filterNot { it.id in auditedActionIds }.forEach { action ->
            entries += LiveTimelineEntry(
                id = "action:${action.id}",
                occurredAt = maxOf(action.createdAt, action.approvedAt, action.startedAt, action.completedAt),
                kind = LiveTimelineKind.ACTION,
                title = action.title,
                detail = listOf(action.destination, "${action.runtimeId} · ${action.capabilityId}").filter { it.isNotBlank() }.joinToString(" · "),
                status = action.status,
                relatedId = action.id,
                severity = if (action.status in setOf("NEEDS_REVIEW", "FAILED", "BLOCKED")) "WARN" else "INFO"
            )
        }

        snapshot.evidence.forEach { evidence ->
            entries += LiveTimelineEntry(
                id = "evidence:${evidence.id}",
                occurredAt = evidence.capturedAt,
                kind = LiveTimelineKind.EVIDENCE,
                title = evidence.title,
                detail = listOf(evidence.kind, evidence.sourceUrl).filter { it.isNotBlank() }.joinToString(" · "),
                status = "CAPTURED",
                relatedId = evidence.id
            )
        }

        return entries.sortedWith(compareByDescending<LiveTimelineEntry> { it.occurredAt }.thenBy { it.id })
            .take(limit)
    }
}
