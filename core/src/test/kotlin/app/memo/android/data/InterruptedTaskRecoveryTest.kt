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
}
