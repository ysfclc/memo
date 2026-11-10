package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeRegistry
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

class CapabilityCompilerTest {
    private val registry = RuntimeRegistry()

    @Test fun researchManifestSelectsOnlyRegisteredCapabilitiesAndIsDeterministic() {
        val plan = IntentPlanner.plan("Research local models and compare evidence")
        val first = CapabilityCompiler.compile(plan, registry.listRuntimes(), plan.goal)
        val second = CapabilityCompiler.compile(plan, registry.listRuntimes(), plan.goal)
        assertTrue(first.capabilities.any { it.capabilityId == "browser.capture_evidence" })
        assertTrue(first.capabilities.any { it.capabilityId == "model.generate" })
        assertTrue(first.unresolvedRequirements.isEmpty())
        assertEquals(first.manifestSha256, second.manifestSha256)
        assertTrue(first.manifestSha256.matches(Regex("[a-f0-9]{64}")))
    }

    @Test fun jobSearchSelectsBrowserResearchButDoesNotSelectExternalWrite() {
        val plan = IntentPlanner.plan("Find current job openings and compare salary ranges")
        val manifest = CapabilityCompiler.compile(plan, registry.listRuntimes())
        assertEquals(IntentDomain.RESEARCH, plan.domain)
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.navigate_https" })
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.capture_evidence" })
        assertFalse(manifest.requiresUserApproval)
        assertFalse(manifest.capabilities.any { it.capabilityId == "browser.prepare_prompt" })
    }

    @Test fun externalWriteRequiresApprovalAndApprovalCapability() {
        val plan = IntentPlanner.plan("Publish this post to LinkedIn")
        val manifest = CapabilityCompiler.compile(plan, registry.listRuntimes())
        assertTrue(manifest.requiresUserApproval)
        assertTrue(manifest.capabilities.any { it.capabilityId == "browser.prepare_prompt" && it.requiresUserApproval })
    }

    @Test fun sourceIntentHashChangesWhenIntentChanges() {
        val plan = IntentPlanner.plan("Prepare my career summary")
        val a = CapabilityCompiler.compile(plan, registry.listRuntimes(), "career summary A")
        val b = CapabilityCompiler.compile(plan, registry.listRuntimes(), "career summary B")
        assertNotEquals(a.sourceIntentSha256, b.sourceIntentSha256)
        assertNotEquals(a.manifestSha256, b.manifestSha256)
    }

    @Test fun optionalRuntimeIsSelectedButNeverPretendedToBeEnabled() {
        val plan = IntentPlanner.plan("Automate a sequence in my Android app")
        assertEquals(IntentDomain.AUTOMATION, plan.domain, "Explicit automation intent must outrank the generic app/build keyword")

        val manifest = CapabilityCompiler.compile(plan, registry.listRuntimes())
        val android = manifest.capabilities.filter { it.runtimeId == "runtime.android.accessibility" }
        assertTrue(android.isNotEmpty())
        assertTrue(android.any { it.capabilityId == "android.launch_app" })
        assertTrue(android.all { it.runtimeStatus == "OPTIONAL_PERMISSION" })
        assertFalse(manifest.unresolvedRequirements.contains("android.launch_app"))
    }

    @Test fun buildingAWorkflowEditorIsNotMisclassifiedAsAutomation() {
        val plan = IntentPlanner.plan("Build the Android app's workflow editor")
        assertEquals(IntentDomain.BUILD, plan.domain)
    }
}
