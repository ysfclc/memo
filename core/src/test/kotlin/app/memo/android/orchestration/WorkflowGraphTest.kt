package app.memo.android.orchestration

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
}
