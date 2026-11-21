package app.memo.android.ai

import app.memo.android.orchestration.AgentExecutionEvent
import app.memo.android.orchestration.AgentExecutionEventType
import org.junit.Assert.assertTrue
import org.junit.Test

class MemoWorkspaceAgentToolsTest {
    @Test
    fun transcriptExplainsAllApprovalLifecycleEvents() {
        val cases = listOf(
            AgentExecutionEventType.TOOL_APPROVAL_REQUESTED to "Waiting for approval",
            AgentExecutionEventType.TOOL_APPROVAL_GRANTED to "Approval recorded",
            AgentExecutionEventType.TOOL_APPROVAL_REJECTED to "Action not approved"
        )
        cases.forEach { (type, expected) ->
            val transcript = agentEventTranscriptMessage(
                AgentExecutionEvent(
                    runId = "run-1",
                    type = type,
                    summary = "Approval lifecycle",
                    toolId = "browser.submit"
                ),
                "en"
            )
            assertTrue("Missing transcript text for $type: $transcript", transcript.contains(expected))
            assertTrue(transcript.contains("Run ID: run-1"))
            assertTrue(transcript.contains("browser.submit"))
        }
    }
}
