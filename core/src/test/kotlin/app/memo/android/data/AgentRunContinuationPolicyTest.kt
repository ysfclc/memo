package app.memo.android.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRunContinuationPolicyTest {
    private fun run(
        status: String = "NEEDS_REVIEW",
        lastEventType: String = "RUN_NEEDS_REVIEW",
        pendingApprovalCallId: String = "",
        toolCalls: Int = 0,
        successfulToolCalls: Int = 0,
        conversationId: String = "conversation-1",
        goal: String = "Research the requested subject"
    ) = AgentRunRecord(
        id = "run-12345678",
        conversationId = conversationId,
        goal = goal,
        status = status,
        lastEventType = lastEventType,
        pendingApprovalCallId = pendingApprovalCallId,
        toolCalls = toolCalls,
        successfulToolCalls = successfulToolCalls
    )

    @Test fun allowsInterruptedRunBeforeAnyCapabilityStarted() {
        assertTrue(AgentRunContinuationPolicy.assess(run()).allowed)
    }

    @Test fun allowsContinuationAfterEveryRecordedCapabilityCompleted() {
        assertTrue(
            AgentRunContinuationPolicy.assess(
                run(lastEventType = "RUN_CANCELLED", toolCalls = 2, successfulToolCalls = 2)
            ).allowed
        )
    }

    @Test fun rejectsActiveAndTerminalRuns() {
        listOf("RUNNING", "WAITING_FOR_APPROVAL", "TOOL_RUNNING", "FINALIZING", "COMPLETED", "FAILED", "CANCELLED")
            .forEach { status ->
                assertFalse("$status must not be continued", AgentRunContinuationPolicy.assess(run(status = status)).allowed)
            }
    }

    @Test fun rejectsAnInvocationWhoseOutcomeMayBeUncertain() {
        assertFalse(
            AgentRunContinuationPolicy.assess(
                run(lastEventType = "TOOL_STARTED", toolCalls = 1, successfulToolCalls = 0)
            ).allowed
        )
        assertFalse(
            AgentRunContinuationPolicy.assess(
                run(lastEventType = "TOOL_FAILED", toolCalls = 1, successfulToolCalls = 0)
            ).allowed
        )
    }

    @Test fun rejectsAnyPersistedApprovalReference() {
        assertFalse(
            AgentRunContinuationPolicy.assess(
                run(pendingApprovalCallId = "old-call-id")
            ).allowed
        )
    }

    @Test fun rejectsMissingConversationOrGoal() {
        assertFalse(AgentRunContinuationPolicy.assess(run(conversationId = "")).allowed)
        assertFalse(AgentRunContinuationPolicy.assess(run(goal = " ")).allowed)
    }
}
