package app.memo.android.data

import app.memo.android.orchestration.WorkflowTaskProposalParser
import app.memo.android.runtime.RuntimeRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentWorkPlanPolicyTest {
    private fun task(
        id: String,
        participant: String,
        runtimeId: String,
        capabilities: List<String> = emptyList(),
        dependsOn: List<String> = emptyList(),
        kind: String = "DOMAIN_WORK",
        requiresApproval: Boolean = false,
        externalWrite: Boolean = false
    ) = JSONObject().apply {
        put("id", id)
        put("title", "Step $id")
        put("kind", kind)
        put("participant", participant)
        put("runtimeId", runtimeId)
        put("requiredCapabilities", JSONArray(capabilities))
        put("dependsOn", JSONArray(dependsOn))
        put("inputContract", "Input for $id")
        put("outputContract", "Output for $id")
        put("postcondition", "Observable completion of $id")
        put("requiresApproval", requiresApproval)
        put("externalWrite", externalWrite)
        put("timeoutMs", 90_000)
        put("maxRetries", 0)
    }

    private fun graph() = WorkflowTaskProposalParser.parse(
        JSONObject().apply {
            put("schemaVersion", 1)
            put("goal", "Prepare a useful, source-backed deliverable")
            put("tasks", JSONArray(listOf(
                task("draft", "LOCAL_MODEL", "app.models", listOf("model.generate"), kind = "ARTIFACT"),
                task("approval", "USER", "app.approvals", dependsOn = listOf("draft"), kind = "APPROVAL", requiresApproval = true, externalWrite = true),
                task("submit", "BROWSER", "runtime.browser", listOf("browser.submit_prompt"), dependsOn = listOf("approval"), requiresApproval = true)
            )))
        }.toString(),
        "Prepare and submit a source-backed deliverable",
        RuntimeRegistry().listRuntimes()
    )

    @Test
    fun createsReviewableWorkStepsInDependencyOrderAndPreservesApprovalBoundary() {
        val graph = graph()
        val job = AgentWorkPlanPolicy.prepare(
            title = "Prepare deliverable",
            graph = graph,
            sourceConversationId = "conversation-1",
            sourceMessageId = "message-1"
        )

        assertEquals("DRAFT", job.status)
        assertEquals("app.jobs", job.runtimeId)
        assertEquals("conversation-1", job.sourceConversationId)
        assertEquals("message-1", job.sourceMessageId)
        assertEquals(graph.toJson(), job.workflowGraphJson)
        assertEquals(listOf("Step draft", "Step approval", "Step submit"), job.steps.map { it.title })
        assertEquals(listOf("draft", "approval", "submit"), job.steps.map { it.workflowNodeId })
        assertTrue(job.steps.all { it.status == "PENDING" })
        assertEquals("LOCAL_MODEL", job.steps[0].checkpointKind)
        assertTrue(job.steps[1].requiresApproval)
        assertEquals("HUMAN_REVIEW", job.steps[1].checkpointKind)
        assertTrue(job.steps[2].requiresApproval)
        assertEquals("EXTERNAL_ACTION", job.steps[2].checkpointKind)
        assertTrue(job.resultSummary.contains("No step has run"))
    }

    @Test
    fun remoteModelGenerationIsNotClassifiedAsAnExecutableLocalModelStep() {
        val raw = JSONObject().apply {
            put("schemaVersion", 1)
            put("goal", "Prepare a remote-model deliverable")
            put("tasks", JSONArray(listOf(
                task("consent", "USER", "app.approvals", kind = "APPROVAL", requiresApproval = true),
                task("generate", "LOCAL_MODEL", "app.models", listOf("model.generate_remote"), dependsOn = listOf("consent"), requiresApproval = true)
            )))
        }.toString()
        val graph = WorkflowTaskProposalParser.parse(
            raw,
            "Prepare a remote-model deliverable",
            RuntimeRegistry().listRuntimes()
        )
        val job = AgentWorkPlanPolicy.prepare("Remote plan", graph, "conversation-1", "message-1")

        assertEquals("HUMAN_REVIEW", job.steps[0].checkpointKind)
        assertEquals("HUMAN_REVIEW", job.steps[1].checkpointKind)
    }

    @Test
    fun rejectsMissingSourceAndInvalidTitle() {
        assertTrue(runCatching {
            AgentWorkPlanPolicy.prepare("Plan", graph(), "", "message-1")
        }.isFailure)
        assertTrue(runCatching {
            AgentWorkPlanPolicy.prepare(" ", graph(), "conversation-1", "message-1")
        }.isFailure)
    }

    @Test
    fun doesNotMarkAnyProposedStepAsCompleted() {
        val job = AgentWorkPlanPolicy.prepare("Plan", graph(), "conversation-1", "message-1")
        assertFalse(job.steps.any { it.status == "COMPLETED" })
        assertEquals("DRAFT", job.status)
    }
}
