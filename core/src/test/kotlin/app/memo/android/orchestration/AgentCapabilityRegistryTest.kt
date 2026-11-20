package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeRegistry
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentCapabilityRegistryTest {
    private val schema = """{"type":"object","properties":{},"required":[],"additionalProperties":false}"""

    @Test
    fun adaptersMustMatchARegisteredRuntimeCapability() {
        val invalid = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "workspace.unregistered",
                description = "An adapter without a matching runtime contract.",
                inputSchemaJson = schema,
                runtimeId = "app.workspace"
            )
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation) =
                AgentToolResult(AgentToolStatus.SUCCEEDED, "done")
        }
        val error = runCatching {
            AgentCapabilityRegistry(RuntimeRegistry().listRuntimes(), listOf(invalid))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("no matching policy entry"))
    }

    @Test
    fun readinessIsRecheckedInsteadOfFrozenAtConstruction() {
        var ready = false
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.capture_evidence",
                description = "Capture the current visible page.",
                inputSchemaJson = schema,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = ready
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation) =
                AgentToolResult(AgentToolStatus.SUCCEEDED, "saved")
        }
        val registry = AgentCapabilityRegistry(RuntimeRegistry().listRuntimes(), listOf(adapter))
        assertTrue(registry.availableAdapters().isEmpty())
        ready = true
        assertEquals(listOf(adapter), registry.availableAdapters())
        ready = false
        assertFalse(registry.availableAdapters().contains(adapter))
    }

    @Test
    fun adapterCannotWeakenRegisteredApprovalPolicy() {
        val invalid = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.submit_prompt",
                description = "Submit an external prompt.",
                inputSchemaJson = schema,
                executionClass = AgentToolExecutionClass.READ_ONLY,
                requiresUserApproval = false,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation) =
                AgentToolResult(AgentToolStatus.SUCCEEDED, "sent")
        }
        val error = runCatching {
            AgentCapabilityRegistry(RuntimeRegistry().listRuntimes(), listOf(invalid))
        }.exceptionOrNull()
        assertTrue(error?.message.orEmpty().contains("approval policy"))
    }
}
