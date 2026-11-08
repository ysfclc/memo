package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeRegistry
import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowTaskProposalParserTest {
    private val runtimes = RuntimeRegistry().listRuntimes()

    private fun task(
        id: String,
        participant: String,
        runtimeId: String,
        capabilities: List<String> = emptyList(),
        dependsOn: List<String> = emptyList(),
        kind: String = "DOMAIN_WORK",
        externalWrite: Boolean = false,
        requiresApproval: Boolean = false
    ) = JSONObject().apply {
        put("id", id)
        put("title", "Task $id")
        put("kind", kind)
        put("participant", participant)
        put("runtimeId", runtimeId)
        put("requiredCapabilities", JSONArray(capabilities))
        put("dependsOn", JSONArray(dependsOn))
        put("inputContract", "Input for $id")
        put("outputContract", "Output for $id")
        put("postcondition", "Observable result for $id")
        put("requiresApproval", requiresApproval)
        put("externalWrite", externalWrite)
        put("timeoutMs", 90_000)
        put("maxRetries", 0)
    }

    private fun proposal(tasks: List<JSONObject>): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("goal", "Research and prepare a sourced report")
        put("tasks", JSONArray(tasks))
    }.toString()

    @Test fun parsesDynamicTaskSpecsAndCreatesFreshAppOwnedIdempotencyKeys() {
        val raw = proposal(listOf(
            task("scope", "DETERMINISTIC", "app.jobs", listOf("job.create"), kind = "SCOPE"),
            task("research", "BROWSER", "runtime.browser", listOf("browser.navigate_https", "browser.capture_evidence"), listOf("scope")),
            task("draft", "LOCAL_MODEL", "app.models", listOf("model.generate"), listOf("research"), kind = "ARTIFACT"),
            task("review", "USER", "app.jobs", dependsOn = listOf("draft"), kind = "REVIEW")
        ))
        val first = WorkflowTaskProposalParser.parse(raw, "Research and prepare a sourced report", runtimes)
        val second = WorkflowTaskProposalParser.parse(raw, "Research and prepare a sourced report", runtimes)

        assertEquals(listOf("scope", "research", "draft", "review"), WorkflowGraphValidator.topologicalOrder(first).map { it.id })
        assertTrue(first.nodes.all { it.idempotencyKey.matches(Regex("[0-9a-f]{64}")) })
        assertTrue(first.nodes.zip(second.nodes).all { (a, b) -> a.idempotencyKey != b.idempotencyKey })
        assertEquals(first, WorkflowGraph.fromJson(first.toJson()))
    }

    @Test fun rejectsCapabilitiesThatExistOnlyOnAnotherRuntime() {
        val raw = proposal(listOf(
            task("inspect", "BROWSER", "runtime.browser", listOf("entity.propose"))
        ))
        val failure = runCatching {
            WorkflowTaskProposalParser.parse(raw, "Inspect a web page", runtimes)
        }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("runtime.browser:entity.propose"))
    }

    @Test fun rejectsCyclesAndUnapprovedExternalWrites() {
        val cycle = proposal(listOf(
            task("first", "DETERMINISTIC", "app.jobs", dependsOn = listOf("second")),
            task("second", "DETERMINISTIC", "app.jobs", dependsOn = listOf("first"))
        ))
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(cycle, "Do two dependent tasks", runtimes)
        }.isFailure)

        val unsafeWrite = proposal(listOf(
            task(
                "publish", "USER", "app.approvals", kind = "APPROVAL",
                externalWrite = true, requiresApproval = false
            )
        ))
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(unsafeWrite, "Publish a post", runtimes)
        }.isFailure)
    }

    @Test fun rejectsMalformedAndOversizedTaskFields() {
        val missingContract = task("inspect", "BROWSER", "runtime.browser", listOf("browser.inspect_page"))
            .put("postcondition", "")
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(proposal(listOf(missingContract)), "Inspect page", runtimes)
        }.isFailure)
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(" ".repeat(64_001), "Inspect page", runtimes)
        }.isFailure)
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(proposal(listOf(task("bad id", "BROWSER", "runtime.browser"))), "Inspect page", runtimes)
        }.isFailure)
    }
}
