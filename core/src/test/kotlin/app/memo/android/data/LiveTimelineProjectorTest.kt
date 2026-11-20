package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveTimelineProjectorTest {
    @Test
    fun combinesWorkspaceRecordsIntoOneNewestFirstTimeline() {
        val action = ActionApprovalRecord(
            id = "action-1",
            jobId = "job-1",
            stepId = "step-1",
            runtimeId = "runtime.browser",
            capabilityId = "browser.submit",
            destination = "https://example.com/submit",
            title = "Submit reviewed form",
            payload = "private payload",
            contentHash = "a".repeat(64),
            createdAt = 55L,
            status = "IN_PROGRESS"
        )
        val snapshot = AppSnapshot(
            activity = listOf(ActivityRecord(id = "activity-1", at = 100L, level = "INFO", message = "Workspace opened")),
            jobs = listOf(
                WorkJobRecord(
                    id = "job-1",
                    title = "Prepare report",
                    goal = "Research and prepare a report",
                    status = "RUNNING",
                    steps = listOf(
                        JobStepRecord(id = "step-1", title = "Verify sources", detail = "Check source evidence", status = "RUNNING", updatedAt = 80L)
                    ),
                    updatedAt = 90L
                )
            ),
            tasks = listOf(TaskRecord(id = "task-1", title = "Capture page", status = TaskStatus.NEEDS_REVIEW, lastMessage = "Review the captured page", updatedAt = 70L)),
            actionApprovals = listOf(action),
            actionAuditEvents = listOf(
                ActionAuditEvent(
                    id = "audit-1",
                    sequence = 1L,
                    at = 60L,
                    actor = "USER",
                    eventType = "ACTION_STARTED",
                    actionId = action.id,
                    runtimeId = action.runtimeId,
                    destination = action.destination,
                    contentHash = action.contentHash,
                    status = "IN_PROGRESS",
                    previousHash = "0".repeat(64),
                    eventHash = "1".repeat(64),
                    details = "private audit payload must not be rendered"
                )
            ),
            evidence = listOf(
                EvidenceRecord(id = "evidence-1", title = "Source capture", sourceUrl = "https://example.com/source", kind = "WEB_PAGE", content = "body", summary = "summary", capturedAt = 50L)
            )
        )

        val timeline = LiveTimelineProjector.build(snapshot)

        assertEquals(
            listOf(
                "activity:activity-1",
                "job:job-1",
                "step:job-1:step-1",
                "task:task-1",
                "action-event:audit-1",
                "evidence:evidence-1"
            ),
            timeline.map { it.id }
        )
        assertEquals(LiveTimelineKind.ACTION, timeline[4].kind)
        assertEquals("Submit reviewed form", timeline[4].title)
        assertEquals("action-1", timeline[4].relatedId)
        assertFalse(timeline[4].detail.contains("private audit payload"))
    }

    @Test
    fun legacyActionWithoutAuditEventsAppearsOnceUsingItsLatestKnownTimestamp() {
        val action = ActionApprovalRecord(
            id = "legacy-action",
            runtimeId = "runtime.browser",
            capabilityId = "browser.submit",
            destination = "https://example.com",
            title = "Legacy action",
            payload = "payload",
            contentHash = "b".repeat(64),
            createdAt = 10L,
            approvedAt = 20L,
            startedAt = 30L,
            status = "IN_PROGRESS"
        )

        val timeline = LiveTimelineProjector.build(AppSnapshot(actionApprovals = listOf(action)))

        assertEquals(1, timeline.size)
        assertEquals("action:legacy-action", timeline.single().id)
        assertEquals(30L, timeline.single().occurredAt)
    }

    @Test
    fun durableAttemptsAppearInLiveWithoutExposingModelPayloads() {
        val job = WorkJobRecord(
            id = "job-attempt", title = "Prepare a report", goal = "Prepare a report",
            runtimeId = "app.jobs", status = "WAITING_FOR_USER",
            steps = listOf(JobStepRecord(
                id = "step-attempt", title = "Draft report", status = "NEEDS_REVIEW", updatedAt = 20L
            )),
            updatedAt = 20L
        )
        val attempt = WorkflowAttemptRecord(
            id = "attempt-1", jobId = job.id, stepId = "step-attempt", attemptNumber = 1,
            idempotencyKey = WorkflowAttemptJournal.legacyIdempotencyKey(job.id, "step-attempt"),
            runtimeId = "app.jobs", status = "OUTPUT_SAVED", startedAt = 10L, finishedAt = 30L,
            evidenceId = "evidence-1", detail = "Draft persisted for review"
        )
        val timeline = LiveTimelineProjector.build(AppSnapshot(
            jobs = listOf(job),
            evidence = listOf(EvidenceRecord(
                id = "evidence-1", taskId = job.id, title = "Private model output", sourceUrl = "",
                kind = "WORK_ARTIFACT", content = "secret model payload", summary = "Draft",
                metadata = org.json.JSONObject().put("jobId", job.id).put("stepId", "step-attempt")
                    .put("requiresHumanReview", true).toString()
            )),
            workflowAttempts = listOf(attempt)
        ))
        val entry = timeline.first { it.id == "attempt:attempt-1" }
        assertEquals(LiveTimelineKind.ATTEMPT, entry.kind)
        assertEquals("OUTPUT_SAVED", entry.status)
        assertEquals(30L, entry.occurredAt)
        assertTrue(entry.detail.contains("app.jobs"))
        assertTrue(entry.detail.contains("evidence-1"))
        assertFalse(entry.detail.contains("secret model payload"))
    }

    @Test
    fun liveShowsReceiptFingerprintAndUnverifiedPostconditionWithoutModelPayload() {
        val job = WorkJobRecord(
            id = "job-receipt", title = "Prepare a report", goal = "Prepare a report",
            status = "WAITING_FOR_USER",
            steps = listOf(JobStepRecord(
                id = "step-receipt", title = "Draft report", status = "NEEDS_REVIEW", updatedAt = 20L
            )),
            updatedAt = 20L
        )
        val running = WorkflowAttemptRecord(
            id = "attempt-receipt", jobId = job.id, stepId = "step-receipt", attemptNumber = 1,
            idempotencyKey = "c".repeat(64), runtimeId = "app.models",
            capabilityIds = listOf("model.generate"), status = "RUNNING", startedAt = 10L
        )
        val draft = EvidenceRecord(
            id = "evidence-receipt", taskId = job.id, title = "Private model output", sourceUrl = "",
            kind = "WORK_ARTIFACT", content = "Sensitive draft content must stay out of Live.",
            summary = "Draft",
            metadata = org.json.JSONObject().put("jobId", job.id).put("stepId", "step-receipt")
                .put("requiresHumanReview", true).toString()
        )
        val receipt = WorkflowRuntimeReceipt.create(running, draft, 30L)
        val receiptedDraft = WorkflowRuntimeReceipt.attach(draft, receipt)
        val finished = running.copy(
            status = "OUTPUT_SAVED", finishedAt = 30L, evidenceId = draft.id,
            detail = "Runtime output saved for human review. ${WorkflowRuntimeReceipt.DETAIL_MARKER}"
        )

        val entry = LiveTimelineProjector.build(AppSnapshot(
            jobs = listOf(job),
            evidence = listOf(receiptedDraft),
            workflowAttempts = listOf(finished)
        )).single { it.id == "attempt:attempt-receipt" }

        assertTrue(entry.detail.contains("Output receipt SHA-256: ${receipt.outputSha256.take(12)}"))
        assertTrue(entry.detail.contains("postcondition NOT_VERIFIED"))
        assertFalse(entry.detail.contains("Sensitive draft content"))
    }

    @Test
    fun timelineLimitIsAppliedAfterNewestFirstOrdering() {
        val snapshot = AppSnapshot(
            activity = listOf(
                ActivityRecord(id = "older", at = 1L, message = "Older"),
                ActivityRecord(id = "newer", at = 2L, message = "Newer")
            )
        )

        val timeline = LiveTimelineProjector.build(snapshot, limit = 1)

        assertEquals(1, timeline.size)
        assertEquals("activity:newer", timeline.single().id)
    }

    @Test
    fun timelineRejectsUnboundedOrEmptyLimits() {
        assertTrue(runCatching { LiveTimelineProjector.build(AppSnapshot(), limit = 0) }.isFailure)
        assertTrue(runCatching { LiveTimelineProjector.build(AppSnapshot(), limit = 501) }.isFailure)
    }
}
