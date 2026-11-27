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

    @Test fun rejectsAnUnregisteredRuntimeEvenWhenTheTaskNeedsNoCapabilities() {
        val raw = proposal(listOf(
            task("unknown", "DETERMINISTIC", "app.ghost-runtime")
        ))
        val failure = runCatching {
            WorkflowTaskProposalParser.parse(raw, "Run an unknown task", runtimes)
        }.exceptionOrNull()
        assertTrue(failure?.message.orEmpty().contains("unregistered runtime"))
    }


    @Test fun approvalGatedCapabilitiesRequireAnExplicitApprovalTask() {
        val missingApproval = proposal(listOf(
            task("submit", "BROWSER", "runtime.browser", listOf("browser.submit_prompt"))
        ))
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(missingApproval, "Submit a provider prompt", runtimes)
        }.isFailure)

        val approvalFlagWithoutGate = proposal(listOf(
            task("submit", "BROWSER", "runtime.browser", listOf("browser.submit_prompt"), requiresApproval = true)
        ))
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(approvalFlagWithoutGate, "Submit a provider prompt", runtimes)
        }.isFailure)
        val retryableSensitiveAction = proposal(listOf(
            task("approval", "USER", "app.approvals", kind = "APPROVAL", externalWrite = true, requiresApproval = true),
            task("submit", "BROWSER", "runtime.browser", listOf("browser.submit_prompt"), listOf("approval"), requiresApproval = true)
                .put("maxRetries", 1)
        ))
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(retryableSensitiveAction, "Submit a provider prompt", runtimes)
        }.isFailure)

        val approvedFlow = proposal(listOf(
            task("draft", "DETERMINISTIC", "app.jobs", kind = "ARTIFACT"),
            task("approval", "USER", "app.approvals", dependsOn = listOf("draft"), kind = "APPROVAL", externalWrite = true, requiresApproval = true),
            task("submit", "BROWSER", "runtime.browser", listOf("browser.submit_prompt"), dependsOn = listOf("approval"), requiresApproval = true),
            task("review", "USER", "app.jobs", dependsOn = listOf("submit"), kind = "REVIEW")
        ))
        val graph = WorkflowTaskProposalParser.parse(approvedFlow, "Submit a provider prompt", runtimes)
        assertTrue(graph.nodes.single { it.id == "submit" }.requiresApproval)
        assertTrue(graph.nodes.single { it.id == "submit" }.externalSideEffect)
        assertEquals(listOf("approval"), graph.nodes.single { it.id == "submit" }.dependsOn)
        assertTrue(graph.nodes.single { it.id == "approval" }.externalWrite)
        val tampered = graph.copy(nodes = graph.nodes.map { node ->
            if (node.id == "submit") node.copy(dependsOn = emptyList()) else node
        })
        assertTrue(runCatching { WorkflowGraphValidator.validate(tampered) }.isFailure)
    }

    @Test fun remoteModelConsentUsesApprovalWithoutBeingClassifiedAsExternalWrite() {
        val raw = proposal(listOf(
            task("draft", "DETERMINISTIC", "app.jobs", kind = "ARTIFACT"),
            task("consent", "USER", "app.approvals", dependsOn = listOf("draft"), kind = "APPROVAL", requiresApproval = true),
            task("generate", "LOCAL_MODEL", "app.models", listOf("model.generate_remote"), listOf("consent"), requiresApproval = true),
            task("review", "USER", "app.jobs", dependsOn = listOf("generate"), kind = "REVIEW")
        ))
        val graph = WorkflowTaskProposalParser.parse(raw, "Generate using a remote provider", runtimes)
        val generate = graph.nodes.single { it.id == "generate" }
        val consent = graph.nodes.single { it.id == "consent" }
        assertTrue(generate.requiresApproval)
        assertTrue(!generate.externalSideEffect)
        assertTrue(!consent.externalWrite)
        assertTrue(runCatching { WorkflowGraph.fromJson(graph.toJson()) }.isSuccess)
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
        val fractionalLimit = task("fractional", "BROWSER", "runtime.browser")
            .put("timeoutMs", 90_000.5)
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(proposal(listOf(fractionalLimit)), "Inspect page", runtimes)
        }.isFailure)
    }
    @Test fun rejectsCoercedProposalEnvelopeFields() {
        val valid = proposal(listOf(task("inspect", "BROWSER", "runtime.browser", listOf("browser.inspect_page"))))

        val stringSchemaVersion = JSONObject(valid).put("schemaVersion", "1").toString()
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(stringSchemaVersion, "Inspect a page", runtimes)
        }.isFailure)

        val fractionalSchemaVersion = JSONObject(valid).put("schemaVersion", 1.5).toString()
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(fractionalSchemaVersion, "Inspect a page", runtimes)
        }.isFailure)

        val booleanSchemaVersion = JSONObject(valid).put("schemaVersion", true).toString()
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(booleanSchemaVersion, "Inspect a page", runtimes)
        }.isFailure)

        val missingSchemaVersion = JSONObject(valid).apply { remove("schemaVersion") }.toString()
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(missingSchemaVersion, "Inspect a page", runtimes)
        }.isFailure)

        val nonTextGoal = JSONObject(valid).put("goal", 123).toString()
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(nonTextGoal, "Inspect a page", runtimes)
        }.isFailure)
    }


    @Test fun rejectsOutOfRangeIntegerFields() {
        val valid = proposal(listOf(task("inspect", "BROWSER", "runtime.browser", listOf("browser.inspect_page"))))
        val overflow = valid.replace(
            "\"timeoutMs\":120000",
            "\"timeoutMs\":9223372036854775808"
        )
        assertNotEquals("The overflow fixture must change the serialized proposal.", valid, overflow)
        assertTrue(runCatching {
            WorkflowTaskProposalParser.parse(overflow, "Inspect a page", runtimes)
        }.isFailure)
    }

}
