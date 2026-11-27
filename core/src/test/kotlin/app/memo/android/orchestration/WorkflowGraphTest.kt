package app.memo.android.orchestration

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowGraphTest {
    @Test fun graphHasParallelContextAndScopeBranchesThenJoinsBeforeDomainWork() {
        val plan = IntentPlanner.plan("Research local AI models and compare evidence")
        val manifest = CapabilityCompiler.compile(plan, app.memo.android.runtime.RuntimeRegistry().listRuntimes())
        val graph = WorkflowGraphCompiler.compile(plan, manifest)
        val ordered = WorkflowGraphValidator.topologicalOrder(graph)
        assertEquals(setOf("scope", "context"), graph.nodes.first { it.id == "domain" }.dependsOn.toSet())
        assertTrue(ordered.indexOfFirst { it.id == "scope" } < ordered.indexOfFirst { it.id == "domain" })
        assertTrue(ordered.indexOfFirst { it.id == "context" } < ordered.indexOfFirst { it.id == "domain" })
        assertTrue(graph.nodes.any { it.kind == WorkflowNodeKind.VERIFICATION && it.postcondition.isNotBlank() })
    }

    @Test fun jobSearchDomainUsesRegisteredBrowserRuntimeAndEvidenceCapabilities() {
        val plan = IntentPlanner.plan("Find current job offers in Krakow")
        val runtimes = app.memo.android.runtime.RuntimeRegistry().listRuntimes()
        val manifest = CapabilityCompiler.compile(plan, runtimes)
        val graph = WorkflowGraphCompiler.compile(plan, manifest)
        val domain = graph.nodes.single { it.id == "domain" }

        assertEquals(IntentDomain.RESEARCH, plan.domain)
        assertEquals(WorkflowParticipant.BROWSER, domain.participant)
        assertEquals("runtime.browser", domain.runtimeId)
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.navigate_https" })
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.inspect_page" })
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.capture_evidence" })
        assertFalse(plan.requiresExternalApproval)
    }

    @Test fun externalWriteIsAnIsolatedApprovalCheckpoint() {
        val plan = IntentPlanner.plan("Publish this post to LinkedIn")
        val manifest = CapabilityCompiler.compile(plan, app.memo.android.runtime.RuntimeRegistry().listRuntimes())
        val graph = WorkflowGraphCompiler.compile(plan, manifest)
        val approval = graph.nodes.single { it.externalWrite }
        assertEquals(WorkflowNodeKind.APPROVAL, approval.kind)
        assertTrue(approval.requiresApproval)
        assertEquals(0, approval.maxRetries)
        assertTrue(approval.requiredCapabilities.isEmpty())
        assertEquals(listOf("verify"), approval.dependsOn)
        assertEquals(listOf("approval"), graph.nodes.single { it.id == "review" }.dependsOn)
    }

    @Test fun graphRoundTripsWithItsContractsAndStableIds() {
        val plan = IntentPlanner.plan("Add a new CRM contact from this business card")
        val manifest = CapabilityCompiler.compile(plan, app.memo.android.runtime.RuntimeRegistry().listRuntimes())
        val graph = WorkflowGraphCompiler.compile(plan, manifest)
        val restored = WorkflowGraph.fromJson(graph.toJson())
        assertEquals(graph, restored)
    }

    private fun task(
        id: String,
        kind: WorkflowNodeKind,
        participant: WorkflowParticipant,
        runtimeId: String,
        capabilities: List<String> = emptyList(),
        dependsOn: List<String> = emptyList(),
        externalWrite: Boolean = false,
        requiresApproval: Boolean = false,
        timeoutMs: Long = 120_000
    ) = TaskSpec(
        id = id,
        title = "Task $id",
        kind = kind,
        participant = participant,
        runtimeId = runtimeId,
        requiredCapabilities = capabilities,
        dependsOn = dependsOn,
        inputContract = "Typed input for $id",
        outputContract = "Typed output for $id",
        postcondition = "Observable result for $id",
        requiresApproval = requiresApproval,
        externalWrite = externalWrite,
        timeoutMs = timeoutMs,
        maxRetries = 0,
        idempotencyKey = "a".repeat(64)
    )

    @Test fun dynamicTaskSpecsCompileIntoAValidatedFanOutAndJoinDag() {
        val registry = app.memo.android.runtime.RuntimeRegistry()
        val tasks = listOf(
            task("scope", WorkflowNodeKind.SCOPE, WorkflowParticipant.DETERMINISTIC, "app.jobs", listOf("job.create")),
            task("context", WorkflowNodeKind.CONTEXT, WorkflowParticipant.DETERMINISTIC, "app.entities", listOf("entity.propose"), listOf("scope")),
            task("research", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.BROWSER, "runtime.browser", listOf("browser.navigate_https", "browser.inspect_page", "browser.capture_evidence"), listOf("scope")),
            task("draft", WorkflowNodeKind.ARTIFACT, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("model.generate"), listOf("context", "research")),
            task("review", WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.jobs", dependsOn = listOf("draft"))
        )
        val firstManifest = CapabilityCompiler.compileForTasks("Research, compare and draft a sourced report", tasks, registry.listRuntimes())
        val secondManifest = CapabilityCompiler.compileForTasks("Research, compare and draft a sourced report", tasks, registry.listRuntimes())

        assertTrue(firstManifest.unresolvedRequirements.isEmpty())
        assertEquals(firstManifest.manifestSha256, secondManifest.manifestSha256)
        assertEquals(tasks.flatMap { task -> task.requiredCapabilities.map { task.runtimeId to it } }.toSet(),
            firstManifest.capabilities.map { it.runtimeId to it.capabilityId }.toSet())

        val graph = WorkflowGraphCompiler.compile("Research, compare and draft a sourced report", tasks, firstManifest)
        val ordered = WorkflowGraphValidator.topologicalOrder(graph)
        assertEquals(setOf("context", "research"), graph.nodes.single { it.id == "draft" }.dependsOn.toSet())
        assertTrue(ordered.indexOfFirst { it.id == "scope" } < ordered.indexOfFirst { it.id == "context" })
        assertTrue(ordered.indexOfFirst { it.id == "scope" } < ordered.indexOfFirst { it.id == "research" })
        assertTrue(ordered.indexOfFirst { it.id == "context" } < ordered.indexOfFirst { it.id == "draft" })
        assertTrue(ordered.indexOfFirst { it.id == "research" } < ordered.indexOfFirst { it.id == "draft" })
        assertEquals(graph, WorkflowGraph.fromJson(graph.toJson()))
    }

    @Test fun dynamicCapabilityAssemblyNeverBorrowsAnIdenticallyNamedCapabilityFromAnotherRuntime() {
        val registry = app.memo.android.runtime.RuntimeRegistry()
        val wrongRuntimeTask = task(
            "inspect", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.BROWSER,
            "runtime.browser", listOf("entity.propose")
        )
        val manifest = CapabilityCompiler.compileForTasks("Inspect the page", listOf(wrongRuntimeTask), registry.listRuntimes())
        assertTrue(manifest.capabilities.isEmpty())
        assertEquals(listOf("runtime.browser:entity.propose"), manifest.unresolvedRequirements)
        assertTrue(runCatching {
            WorkflowGraphCompiler.compile("Inspect the page", listOf(wrongRuntimeTask), manifest)
        }.isFailure)
    }

    @Test fun dynamicGraphRejectsModelAndDurationBudgetOverruns() {
        val registry = app.memo.android.runtime.RuntimeRegistry()
        val modelTask = task("draft", WorkflowNodeKind.ARTIFACT, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("model.generate"))
        val manifest = CapabilityCompiler.compileForTasks("Draft a report", listOf(modelTask), registry.listRuntimes())
        assertTrue(runCatching {
            WorkflowGraphCompiler.compile("Draft a report", listOf(modelTask), manifest, WorkflowBudget(maxModelCalls = 0))
        }.isFailure)
        assertTrue(runCatching {
            WorkflowGraphCompiler.compile("Draft a report", listOf(modelTask), manifest, WorkflowBudget(maxDurationMs = 60_000))
        }.isFailure)
    }

    @Test fun cyclesUnknownDependenciesAndUnapprovedWritesFailClosed() {
        val base = WorkflowGraphCompiler.compile(
            IntentPlanner.plan("Organize these notes"),
            CapabilityCompiler.compile(IntentPlanner.plan("Organize these notes"), app.memo.android.runtime.RuntimeRegistry().listRuntimes())
        )
        val cycle = base.copy(nodes = base.nodes.map { if (it.id == "scope") it.copy(dependsOn = listOf("review")) else it })
        assertTrue(runCatching { WorkflowGraphValidator.validate(cycle) }.isFailure)
        val missing = base.copy(nodes = base.nodes.map { if (it.id == "domain") it.copy(dependsOn = listOf("missing")) else it })
        assertTrue(runCatching { WorkflowGraphValidator.validate(missing) }.isFailure)
        val unapproved = base.copy(nodes = base.nodes + base.nodes.first().copy(id = "write", externalWrite = true))
        assertTrue(runCatching { WorkflowGraphValidator.validate(unapproved) }.isFailure)
    }

    private fun approvedExternalGraph(): WorkflowGraph {
        val proposal = JSONObject().apply {
            put("schemaVersion", 1)
            put("goal", "Prepare and submit a reviewed deliverable")
            put("tasks", JSONArray(listOf(
                task("draft", WorkflowNodeKind.ARTIFACT, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("model.generate")),
                task(
                    "approval", WorkflowNodeKind.APPROVAL, WorkflowParticipant.USER, "app.approvals",
                    dependsOn = listOf("draft"), externalWrite = true, requiresApproval = true
                ),
                task(
                    "submit", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.BROWSER, "runtime.browser",
                    listOf("browser.submit_prompt"), listOf("approval"), requiresApproval = true
                ),
                task("review", WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.jobs", dependsOn = listOf("submit"))
            )))
        }.toString()
        return WorkflowTaskProposalParser.parse(
            proposal, "Prepare and submit a reviewed deliverable",
            app.memo.android.runtime.RuntimeRegistry().listRuntimes()
        )
    }

    @Test fun persistedGraphDecoderRejectsOverlongFieldsInsteadOfSilentlyTruncating() {
        val json = JSONObject(approvedExternalGraph().toJson())
        json.getJSONArray("nodes").getJSONObject(0).put("title", "x".repeat(241))

        assertTrue(runCatching { WorkflowGraph.fromJson(json.toString()) }.isFailure)
    }

    @Test fun persistedGraphDecoderRequiresSideEffectClassificationAndStringCapabilities() {
        val graph = approvedExternalGraph()
        val missingClassification = JSONObject(graph.toJson())
        val rows = missingClassification.getJSONArray("nodes")
        val sideEffectNode = (0 until rows.length())
            .map { rows.getJSONObject(it) }
            .single { it.optBoolean("externalSideEffect") }
        sideEffectNode.remove("externalSideEffect")
        assertTrue(runCatching { WorkflowGraph.fromJson(missingClassification.toString()) }.isFailure)

        val wrongCapabilityType = JSONObject(graph.toJson())
        val capabilityRows = wrongCapabilityType.getJSONArray("nodes")
        val capabilityNode = (0 until capabilityRows.length())
            .map { capabilityRows.getJSONObject(it) }
            .first { it.getJSONArray("requiredCapabilities").length() > 0 }
        capabilityNode.getJSONArray("requiredCapabilities").put(0, 123)
        assertTrue(runCatching { WorkflowGraph.fromJson(wrongCapabilityType.toString()) }.isFailure)
    }

    @Test fun persistedGraphDecoderRejectsFractionalLimitsAndMissingBudget() {
        val fractionalTimeout = JSONObject(approvedExternalGraph().toJson())
        fractionalTimeout.getJSONArray("nodes").getJSONObject(0).put("timeoutMs", 90_000.5)
        assertTrue(runCatching { WorkflowGraph.fromJson(fractionalTimeout.toString()) }.isFailure)

        val missingBudget = JSONObject(approvedExternalGraph().toJson())
        missingBudget.remove("budget")
        assertTrue(runCatching { WorkflowGraph.fromJson(missingBudget.toString()) }.isFailure)
    }

}
