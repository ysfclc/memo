package app.memo.android.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RuntimeRegistryTest {
    @Test fun browserCapabilitiesAreSeparateFromNativeAppCapabilities() {
        val registry = RuntimeRegistry()
        val browser = registry.resolve("runtime.browser")
        val chats = registry.resolve("app.chats")
        assertNotNull(browser)
        assertNotNull(chats)
        assertEquals(RuntimeKind.BROWSER, browser!!.kind)
        assertEquals(RuntimeKind.APP, chats!!.kind)
        assertTrue(registry.can("runtime.browser", "browser.capture_evidence"))
        assertTrue(registry.can("runtime.browser", "browser.submit_prompt"))
        assertTrue(registry.can("runtime.browser", "browser.capture_response"))
        assertTrue(registry.can("runtime.browser", "browser.run_multi_chat_workflow"))
        assertFalse(registry.can("runtime.browser", "entity.propose"))
        assertTrue(registry.can("app.entities", "entity.propose"))
        assertTrue(registry.can("app.entities", "entity.create"))
        assertTrue(registry.can("app.jobs", "job.verify"))
        assertFalse(registry.can("app.entities", "browser.navigate_https"))
    }
    @Test fun localInferenceAndRemoteProviderConsentAreDifferentCapabilities() {
        val models = RuntimeRegistry().resolve("app.models")!!
        val local = models.capabilities.first { it.id == "model.generate" }
        val remote = models.capabilities.first { it.id == "model.generate_remote" }
        assertFalse(local.requiresUserApproval)
        assertTrue(remote.requiresUserApproval)
        assertFalse(remote.externalSideEffect)
        assertTrue(RuntimeRegistry().browser().capabilities.first { it.id == "browser.submit_prompt" }.externalSideEffect)
        assertFalse(RuntimeRegistry().browser().capabilities.first { it.id == "browser.prepare_prompt" }.externalSideEffect)
    }

    @Test fun humanApprovalInboxIsARegisteredRuntimeDistinctFromTheJobWorkbench() {
        val registry = RuntimeRegistry()
        val approvals = registry.resolve("app.approvals")
        val jobs = registry.resolve("app.jobs")
        assertNotNull(approvals)
        assertNotNull(jobs)
        assertEquals(RuntimeKind.APP, approvals!!.kind)
        assertTrue(registry.can("app.approvals", "approval.inspect"))
        assertTrue(registry.can("app.approvals", "approval.approve_exact"))
        assertTrue(registry.can("app.approvals", "approval.reject"))
        assertFalse(registry.can("app.jobs", "approval.approve_exact"))
    }

    @Test fun externalAndroidControlIsSeparateAndSensitiveActionsRequireApproval() {
        val registry = RuntimeRegistry()
        val runtime = registry.resolve("runtime.android.accessibility")
        assertNotNull(runtime)
        assertEquals(RuntimeKind.EXTERNAL_ANDROID, runtime!!.kind)
        assertTrue(registry.can("runtime.android.accessibility", "android.launch_app"))
        assertTrue(registry.can("runtime.android.accessibility", "android.verify_visible_text"))
        assertFalse(registry.can("runtime.android.accessibility", "browser.submit_prompt"))
        val tap = runtime.capabilities.first { it.id == "android.tap_exact_text" }
        assertTrue(tap.requiresUserApproval)
    }

}
