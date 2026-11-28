package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InterruptedTaskRecoveryTest {
    @Test
    fun runningTaskBecomesReviewableAfterProcessRestart() {
        val running = TaskRecord(title = "Capture a page", status = TaskStatus.RUNNING)
        val queued = TaskRecord(title = "Open another site", status = TaskStatus.QUEUED)
        val restored = AppSnapshot(tasks = listOf(running, queued)).recoverInterruptedTasks()

        assertEquals(TaskStatus.NEEDS_REVIEW, restored.tasks.first { it.id == running.id }.status)
        assertTrue(restored.tasks.first { it.id == running.id }.lastMessage.contains("closed"))
        assertEquals(TaskStatus.QUEUED, restored.tasks.first { it.id == queued.id }.status)
        assertTrue(restored.activity.first().message.contains("1 interrupted task"))
    }

    @Test
    fun snapshotWithoutRunningTasksIsUnchanged() {
        val snapshot = AppSnapshot(tasks = listOf(TaskRecord(title = "Waiting", status = TaskStatus.QUEUED)))
        assertEquals(snapshot, snapshot.recoverInterruptedTasks())
    }

    @Test
    fun runningJobsBecomeReviewableAndRunningStepsRequireReview() {
        val runningStep = JobStepRecord(title = "Consult frontier model", status = "RUNNING")
        val waitingStep = JobStepRecord(title = "Persist output", status = "PENDING")
        val runningJob = WorkJobRecord(title = "Frontier consultation", goal = "Compare responses", status = "RUNNING", steps = listOf(runningStep, waitingStep))
        val restored = AppSnapshot(jobs = listOf(runningJob)).recoverInterruptedTasks()

        val recovered = restored.jobs.single()
        assertEquals("WAITING_FOR_USER", recovered.status)
        assertEquals("NEEDS_REVIEW", recovered.steps.first { it.id == runningStep.id }.status)
        assertEquals("PENDING", recovered.steps.first { it.id == waitingStep.id }.status)
        assertTrue(recovered.resultSummary.contains("restarted"))
        assertTrue(restored.activity.first().message.contains("0 interrupted tasks"))
        assertTrue(restored.activity.first().message.contains("1 interrupted job"))
    }

    @Test
    fun interruptedWorkStepLinksOnlyAnArtifactFromItsExactRunAndSource() {
        val runId = "work-run-123"
        val sourceConversationId = "conversation-source"
        val sourceMessageId = "message-source"
        val runningStep = JobStepRecord(
            title = "Prepare the requested draft",
            status = "RUNNING",
            evidence = "run:$runId",
            checkpointKind = "LOCAL_MODEL"
        )
        val job = WorkJobRecord(
            title = "User's requested work",
            goal = "Prepare a useful deliverable",
            status = "RUNNING",
            sourceConversationId = sourceConversationId,
            sourceMessageId = sourceMessageId,
            steps = listOf(runningStep)
        )
        val matchingArtifact = EvidenceRecord(
            id = "artifact-matching",
            title = "Private draft",
            sourceUrl = "",
            kind = "STUDIO_ARTIFACT",
            content = "# Draft",
            summary = "A private draft",
            metadata = """{"origin":"MEMO_AGENT","runId":"$runId","sourceConversationId":"$sourceConversationId","sourceMessageId":"$sourceMessageId"}"""
        )
        val wrongSourceArtifact = EvidenceRecord(
            id = "artifact-wrong-source",
            title = "Unrelated draft",
            sourceUrl = "",
            kind = "STUDIO_ARTIFACT",
            content = "# Other",
            summary = "An unrelated draft",
            metadata = """{"origin":"MEMO_AGENT","runId":"$runId","sourceConversationId":"another-conversation","sourceMessageId":"another-message"}"""
        )

        val restored = AppSnapshot(
            jobs = listOf(job),
            evidence = listOf(wrongSourceArtifact, matchingArtifact)
        ).recoverInterruptedTasks()

        val recovered = restored.jobs.single()
        val recoveredStep = recovered.steps.single()
        assertEquals("WAITING_FOR_USER", recovered.status)
        assertEquals("NEEDS_REVIEW", recoveredStep.status)
        assertEquals("evidence:artifact-matching; run:$runId", recoveredStep.evidence)
        assertTrue(recoveredStep.detail.contains("private draft"))
    }

    @Test
    fun interruptedWorkStepWithoutMatchingArtifactKeepsRunReferenceAndIsNotReplayed() {
        val runId = "work-run-456"
        val step = JobStepRecord(
            title = "Prepare the requested draft",
            status = "RUNNING",
            evidence = "run:$runId",
            checkpointKind = "LOCAL_MODEL"
        )
        val job = WorkJobRecord(
            title = "User's requested work",
            goal = "Prepare a useful deliverable",
            status = "RUNNING",
            sourceConversationId = "conversation-source",
            sourceMessageId = "message-source",
            steps = listOf(step)
        )

        val restored = AppSnapshot(jobs = listOf(job)).recoverInterruptedTasks()
        val recoveredStep = restored.jobs.single().steps.single()

        assertEquals("NEEDS_REVIEW", recoveredStep.status)
        assertEquals("run:$runId", recoveredStep.evidence)
        assertTrue(recoveredStep.detail.contains("Inspect the source run"))
    }

    @Test
    fun activeAgentRunsBecomeReviewableAfterProcessRestartWithoutReplay() {
        val activeStatuses = listOf("RUNNING", "WAITING_FOR_APPROVAL", "TOOL_RUNNING", "FINALIZING")
        val activeRuns = activeStatuses.map { status ->
            AgentRunRecord(
                conversationId = "conversation-$status",
                goal = "Complete a useful task",
                status = status,
                stage = "Last durable checkpoint: $status",
                lastEventType = if (status == "TOOL_RUNNING") "TOOL_STARTED" else "RUN_STARTED",
                lastToolId = if (status == "TOOL_RUNNING") "workspace.save_note" else "",
                pendingApprovalCallId = if (status == "WAITING_FOR_APPROVAL") "call-pending" else ""
            )
        }
        val completed = AgentRunRecord(
            conversationId = "conversation-completed",
            goal = "Already finished",
            status = "COMPLETED",
            finalMessage = "Saved result"
        )

        val restored = AppSnapshot(agentRuns = activeRuns + completed).recoverInterruptedTasks()

        activeRuns.forEach { previous ->
            val recovered = restored.agentRuns.single { it.id == previous.id }
            assertEquals("NEEDS_REVIEW", recovered.status)
            assertEquals("Interrupted by application restart", recovered.stage)
            assertTrue(recovered.recoveryReason.contains("No step was replayed"))
            assertEquals("", recovered.pendingApprovalCallId)
            assertTrue(recovered.finishedAt > 0L)
            assertEquals(previous.lastToolId, recovered.lastToolId)
        }
        assertEquals(completed, restored.agentRuns.single { it.id == completed.id })
        assertTrue(restored.activity.first().message.contains("4 interrupted Memo runs"))
    }

    @Test
    fun terminalAgentRunsAndSnapshotsWithoutActiveRunsRemainUnchanged() {
        val completed = AgentRunRecord(
            conversationId = "conversation-1",
            goal = "Finished task",
            status = "COMPLETED",
            finalMessage = "Done",
            finishedAt = 123L
        )
        val failed = AgentRunRecord(
            conversationId = "conversation-2",
            goal = "Failed task",
            status = "FAILED",
            finalMessage = "Failed",
            finishedAt = 456L
        )
        val snapshot = AppSnapshot(agentRuns = listOf(completed, failed))

        assertEquals(snapshot, snapshot.recoverInterruptedTasks())
    }

}
