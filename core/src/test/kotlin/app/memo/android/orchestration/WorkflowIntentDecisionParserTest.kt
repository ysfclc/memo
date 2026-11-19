package app.memo.android.orchestration

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowIntentDecisionParserTest {
    @Test fun acceptsNoWorkflowForOrdinaryConversation() {
        assertTrue(WorkflowIntentDecisionParser.parse("""{"decision":"NO_WORKFLOW"}""") is WorkflowIntentDecision.NoWorkflow)
    }

    @Test fun unwrapsTypedWorkflowProposal() {
        val workflow = JSONObject()
            .put("schemaVersion", 1)
            .put("goal", "Prepare a sourced market report")
            .put("tasks", JSONArray())
        val parsed = WorkflowIntentDecisionParser.parse(JSONObject()
            .put("decision", "PROPOSE")
            .put("workflow", workflow)
            .toString()) as WorkflowIntentDecision.Proposal

        assertEquals(workflow.toString(), JSONObject(parsed.workflowJson).toString())
    }

    @Test fun acceptsOneJsonFenceButRejectsSurroundingProse() {
        val parsed = WorkflowIntentDecisionParser.parse("""```json
{"decision":"NO_WORKFLOW"}
```""")
        assertTrue(parsed is WorkflowIntentDecision.NoWorkflow)
        assertTrue(runCatching {
            WorkflowIntentDecisionParser.parse("Here is the result: {\"decision\":\"NO_WORKFLOW\"}")
        }.isFailure)
    }

    @Test fun rejectsUnknownDecisionAndMissingWorkflow() {
        assertTrue(runCatching { WorkflowIntentDecisionParser.parse("""{"decision":"MAYBE"}""") }.isFailure)
        assertTrue(runCatching { WorkflowIntentDecisionParser.parse("""{"decision":"PROPOSE"}""") }.isFailure)
    }

    @Test fun rejectsMalformedAndOversizedOutput() {
        assertTrue(runCatching { WorkflowIntentDecisionParser.parse("not json") }.isFailure)
        assertTrue(runCatching { WorkflowIntentDecisionParser.parse("x".repeat(72_001)) }.isFailure)
    }
}
