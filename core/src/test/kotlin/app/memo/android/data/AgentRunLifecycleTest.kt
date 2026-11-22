package app.memo.android.data

import app.memo.android.orchestration.AgentExecutionEvent
import app.memo.android.orchestration.AgentExecutionEventType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRunLifecycleTest {
    private fun run() = AgentRunRecord(
        id = "run-12345678",
        conversationId = "conversation-1",
        goal = "Find and summarize three useful records",
        createdAt = 1L,
        updatedAt = 1L
    )

    @Test
    fun eventsRecordApprovalToolAndTerminalLifecycle() {
        val started = run().applyEvent(AgentExecutionEvent(
            runId = "run-12345678",
            type = AgentExecutionEventType.RUN_STARTED,
            summary = "Memo started an agent execution.",
            at = 10L
        ))
        assertEquals("RUNNING", started.status)
        assertEquals("RUN_STARTED", started.events.single().eventType)
        assertEquals("app.memo", started.events.single().runtimeId)

        val waiting = started.applyEvent(AgentExecutionEvent(
            runId = started.id,
            type = AgentExecutionEventType.TOOL_APPROVAL_REQUESTED,
            summary = "Waiting for approval",
            toolId = "browser.submit_prompt",
            callId = "call-1",
            at = 20L
        ))
        assertEquals("WAITING_FOR_APPROVAL", waiting.status)
        assertEquals("call-1", waiting.pendingApprovalCallId)
        assertEquals("TOOL_APPROVAL_REQUESTED", waiting.events.last().eventType)
        assertEquals("browser.submit_prompt", waiting.events.last().toolId)

        val granted = waiting.applyEvent(AgentExecutionEvent(
            runId = waiting.id,
            type = AgentExecutionEventType.TOOL_APPROVAL_GRANTED,
            summary = "Exact action approved",
            toolId = "browser.submit_prompt",
            callId = "call-1",
            at = 30L
        ))
        assertEquals("RUNNING", granted.status)
        assertEquals("", granted.pendingApprovalCallId)

        val toolStarted = granted.applyEvent(AgentExecutionEvent(
            runId = granted.id,
            type = AgentExecutionEventType.TOOL_STARTED,
            summary = "Starting browser.submit_prompt",
            toolId = "browser.submit_prompt",
            callId = "call-1",
            at = 40L
        ))
        assertEquals("TOOL_RUNNING", toolStarted.status)
        assertEquals(1, toolStarted.toolCalls)

        val toolFinished = toolStarted.applyEvent(AgentExecutionEvent(
            runId = toolStarted.id,
            type = AgentExecutionEventType.TOOL_FINISHED,
            summary = "Provider response observed",
            toolId = "browser.submit_prompt",
            callId = "call-1",
            evidenceReference = "evidence:response-1",
            at = 50L
        ))
        assertEquals("RUNNING", toolFinished.status)
        assertEquals(1, toolFinished.successfulToolCalls)
        assertEquals("evidence:response-1", toolFinished.lastEvidenceReference)
        assertEquals("TOOL_FINISHED", toolFinished.events.last().eventType)
        assertEquals("evidence:response-1", toolFinished.events.last().evidenceReference)
        assertEquals("Provider response observed", toolFinished.events.last().summary)

        val completed = toolFinished.applyEvent(AgentExecutionEvent(
            runId = toolFinished.id,
            type = AgentExecutionEventType.RUN_FINISHED,
            summary = "Memo finished this execution turn.",
            at = 60L
        ))
        assertEquals("FINALIZING", completed.status)
        assertTrue(completed.isActive)
        assertEquals(0L, completed.finishedAt)
        assertEquals("RUN_FINISHED", completed.events.last().eventType)
        assertEquals(6, completed.events.last().sequence)
        val finalResultSaved = completed.copy(status = "COMPLETED", finishedAt = 60L)
        assertFalse(finalResultSaved.isActive)
    }

    @Test
    fun rejectedOrUncertainToolOutcomesRequireReview() {
        val waiting = run().copy(status = "WAITING_FOR_APPROVAL", pendingApprovalCallId = "call-2")
        val rejected = waiting.applyEvent(AgentExecutionEvent(
            runId = waiting.id,
            type = AgentExecutionEventType.TOOL_APPROVAL_REJECTED,
            summary = "The user rejected the action.",
            callId = "call-2",
            status = "REJECTED",
            at = 70L
        ))
        assertEquals("RUNNING", rejected.status)
        assertEquals("", rejected.pendingApprovalCallId)
        val rejectedFinalizing = rejected.applyEvent(AgentExecutionEvent(
            runId = rejected.id,
            type = AgentExecutionEventType.RUN_NEEDS_REVIEW,
            summary = "The rejected action was not executed; run needs review.",
            at = 75L
        ))
        assertEquals("FINALIZING", rejectedFinalizing.status)

        val started = run().copy(status = "TOOL_RUNNING", toolCalls = 1)
        val uncertain = started.applyEvent(AgentExecutionEvent(
            runId = started.id,
            type = AgentExecutionEventType.TOOL_FAILED,
            summary = "Write outcome is uncertain.",
            status = "NEEDS_REVIEW",
            at = 80L
        ))
        assertEquals("RUNNING", uncertain.status)
        val uncertainFinalizing = uncertain.applyEvent(AgentExecutionEvent(
            runId = uncertain.id,
            type = AgentExecutionEventType.RUN_NEEDS_REVIEW,
            summary = "A capability outcome is uncertain.",
            at = 85L
        ))
        assertEquals("FINALIZING", uncertainFinalizing.status)
        assertTrue(uncertainFinalizing.isActive)
    }

    @Test
    fun eventJournalIsBoundedAndKeepsMonotonicSequence() {
        val recorded = (1..(MAX_AGENT_RUN_EVENTS_PER_RUN + 3)).fold(run()) { current, index ->
            current.appendTimelineEvent(
                AgentRunEventRecord(
                    eventType = "TOOL_FINISHED",
                    summary = "Event $index",
                    status = "SUCCEEDED",
                    at = index.toLong()
                )
            )
        }
        assertEquals(MAX_AGENT_RUN_EVENTS_PER_RUN, recorded.events.size)
        assertEquals(4, recorded.events.first().sequence)
        assertEquals(MAX_AGENT_RUN_EVENTS_PER_RUN + 3, recorded.events.last().sequence)
        assertEquals("Event ${MAX_AGENT_RUN_EVENTS_PER_RUN + 3}", recorded.events.last().summary)
    }

    @Test
    fun eventsFromAnotherRunAreRejected() {
        assertTrue(runCatching {
            run().applyEvent(AgentExecutionEvent(
                runId = "different-run",
                type = AgentExecutionEventType.RUN_STARTED,
                summary = "Incorrect run identity"
            ))
        }.isFailure)
    }
}
