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
    @Test
    fun transcriptCarriesStructuredActionContextWithoutToolArguments() {
        val transcript = agentEventTranscriptMessage(
            AgentExecutionEvent(
                runId = "run-2",
                type = AgentExecutionEventType.TOOL_FINISHED,
                summary = "Capability finished.",
                toolId = "browser.submit_prompt",
                callId = "call-2",
                status = "SUCCEEDED",
                toolVersion = 3,
                evidenceReference = "evidence:response-2",
                runtimeId = "runtime.browser",
                target = "Send reviewed prompt",
                rationale = "Send the exact prompt after explicit approval.",
                result = "A visible response was captured."
            ),
            "en"
        )
        assertTrue(transcript.contains("Runtime: runtime.browser"))
        assertTrue(transcript.contains("Target: Send reviewed prompt"))
        assertTrue(transcript.contains("Rationale: Send the exact prompt after explicit approval."))
        assertTrue(transcript.contains("Result: A visible response was captured."))
        assertTrue(transcript.contains("Evidence: evidence:response-2"))
        assertTrue(!transcript.contains("payload"))
    }

    @Test
    fun transcriptLocalizesStructuredMetadataLabels() {
        val transcript = agentEventTranscriptMessage(
            AgentExecutionEvent(
                runId = "run-pl",
                type = AgentExecutionEventType.TOOL_FINISHED,
                summary = "Capability finished.",
                toolId = "browser.submit_prompt",
                status = "SUCCEEDED",
                evidenceReference = "evidence:pl",
                runtimeId = "runtime.browser",
                target = "Send reviewed prompt",
                rationale = "Send the exact prompt after explicit approval.",
                result = "A visible response was captured."
            ),
            "PL"
        )
        assertTrue(transcript.contains("Środowisko uruchomieniowe: runtime.browser"))
        assertTrue(transcript.contains("Cel: Send reviewed prompt"))
        assertTrue(transcript.contains("Uzasadnienie: Send the exact prompt after explicit approval."))
        assertTrue(transcript.contains("Wynik: A visible response was captured."))
        assertTrue(transcript.contains("Dowód: evidence:pl"))
    }

}
