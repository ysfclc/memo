package app.memo.android.orchestration

import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentExecutionEngineTest {
    private val schema = """{"type":"object","properties":{"query":{"type":"string","minLength":1,"maxLength":120}},"required":["query"],"additionalProperties":false}"""

    @Test
    fun modelChoosesToolThenUsesObservedResultBeforeFinalAnswer() = runBlocking {
        val prompts = mutableListOf<String>()
        val modelResponses = ArrayDeque(
            listOf(
                """{"type":"tool_call","tool":"workspace.search","arguments":{"query":"vendor list"}}""",
                """{"type":"final","content":"I found three matching records."}"""
            )
        )
        val model = AgentModelProvider { _, prompt ->
            prompts += prompt
            modelResponses.removeFirst()
        }
        var invocations = 0
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition("workspace.search", "Search saved records.", schema)
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                invocations++
                assertEquals("vendor list", arguments.getString("query"))
                assertTrue(invocation.idempotencyKey.isNotBlank())
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "Three records with source IDs.", "Found three records.", "evidence:123")
            }
        }
        val events = mutableListOf<AgentExecutionEvent>()
        val result = AgentExecutionEngine(model, listOf(adapter)).execute("Find vendor records") { events += it }

        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals("I found three matching records.", result.finalMessage)
        assertEquals(2, result.modelCalls)
        assertEquals(1, result.toolCalls)
        assertEquals(1, result.successfulToolCalls)
        assertEquals(1, invocations)
        assertTrue(prompts.last().contains("Three records with source IDs."))
        assertEquals("evidence:123", events.first { it.type == AgentExecutionEventType.TOOL_FINISHED }.evidenceReference)
        assertEquals(1, events.first { it.type == AgentExecutionEventType.TOOL_FINISHED }.toolVersion)
        assertEquals(
            listOf(AgentExecutionEventType.RUN_STARTED, AgentExecutionEventType.TOOL_STARTED, AgentExecutionEventType.TOOL_FINISHED, AgentExecutionEventType.RUN_FINISHED),
            events.map { it.type }
        )
    }

    @Test
    fun unknownCapabilityIsNotExecutedAndModelCanRecover() = runBlocking {
        val responses = ArrayDeque(
            listOf(
                """{"type":"tool_call","tool":"device.shell","arguments":{"query":"anything"}}""",
                """{"type":"final","content":"That capability is not available."}"""
            )
        )
        var calls = 0
        val model = AgentModelProvider { _, _ -> calls++; responses.removeFirst() }
        val result = AgentExecutionEngine(model, emptyList()).execute("Do something")

        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals(2, calls)
        assertEquals(0, result.toolCalls)
    }

    @Test
    fun schemaRejectsUnknownFieldsAndOversizedStrings() {
        val extraField = AgentToolSchemaValidator.validate(schema, JSONObject("""{"query":"ok","shell":"rm -rf"}"""))
        val longField = AgentToolSchemaValidator.validate(schema, JSONObject("""{"query":"xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx"}"""))

        assertTrue(extraField?.contains("Unexpected field") == true)
        assertTrue(longField?.contains("maximum length") == true)
        assertFalse(AgentToolSchemaValidator.validate(schema, JSONObject("""{"query":"ok"}""")) != null)
    }

    @Test
    fun externalOrDestructiveCapabilityPausesBeforeInvocation() = runBlocking {
        var executed = false
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                "browser.submit_prompt",
                "Submit an external form.",
                schema,
                executionClass = AgentToolExecutionClass.EXTERNAL_WRITE,
                requiresUserApproval = true,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                executed = true
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "sent")
            }
        }
        val model = AgentModelProvider { _, _ ->
            """{"type":"tool_call","tool":"browser.submit_prompt","arguments":{"query":"send now"}}"""
        }
        val result = AgentExecutionEngine(model, listOf(adapter)).execute("Submit the form")

        assertEquals(AgentRunStatus.NEEDS_REVIEW, result.status)
        assertFalse(executed)
        assertEquals(0, result.toolCalls)
    }

    @Test
    fun localWriteExceptionPausesForReviewAndNeverBlindlyRetries() = runBlocking {
        var modelCalls = 0
        var toolCalls = 0
        val model = AgentModelProvider { _, _ ->
            modelCalls++
            """{"type":"tool_call","tool":"workspace.save_note","arguments":{"query":"save"}}"""
        }
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                "workspace.save_note",
                "Save a private workspace item.",
                schema,
                executionClass = AgentToolExecutionClass.LOCAL_WRITE
            )
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                toolCalls++
                error("Vault write may have completed before the storage layer failed.")
            }
        }

        val result = AgentExecutionEngine(model, listOf(adapter)).execute("Save this")
        assertEquals(AgentRunStatus.NEEDS_REVIEW, result.status)
        assertEquals(1, modelCalls)
        assertEquals(1, toolCalls)
    }

    @Test
    fun jsonWithoutProtocolTypeIsNotMistakenForAnAction() = runBlocking {
        val model = AgentModelProvider { _, _ -> """{"answer":"ordinary text"}""" }
        val result = AgentExecutionEngine(model, emptyList()).execute("Explain JSON")
        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals("""{"answer":"ordinary text"}""", result.finalMessage)
        assertEquals(0, result.toolCalls)
    }

    @Test
    fun plainTextModelResponseStillWorksAsDirectAnswer() = runBlocking {
        val model = AgentModelProvider { _, _ -> "Hello. What would you like to accomplish?" }
        val result = AgentExecutionEngine(model, emptyList()).execute("Hello")

        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals("Hello. What would you like to accomplish?", result.finalMessage)
    }

    @Test
    fun capabilitiesAreRediscoveredAfterEachSuccessfulStep() = runBlocking {
        var pageReady = false
        val systemPrompts = mutableListOf<String>()
        val responses = ArrayDeque(
            listOf(
                """{"type":"tool_call","tool":"browser.navigate_https","arguments":{"url":"https://example.com/"}}""",
                """{"type":"tool_call","tool":"browser.capture_evidence","arguments":{}}""",
                """{"type":"final","content":"I opened the supplied page and saved its evidence."}"""
            )
        )
        val navigate = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.navigate_https",
                description = "Open the HTTPS URL supplied by the user.",
                inputSchemaJson = """{"type":"object","properties":{"url":{"type":"string","minLength":8,"maxLength":2048}},"required":["url"],"additionalProperties":false}""",
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = !pageReady
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                assertEquals("https://example.com/", arguments.getString("url"))
                pageReady = true
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "Visible page loaded.", "Opened example.com.")
            }
        }
        val capture = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.capture_evidence",
                description = "Capture the currently visible page into encrypted Memo evidence.",
                inputSchemaJson = """{"type":"object","properties":{},"required":[],"additionalProperties":false}""",
                executionClass = AgentToolExecutionClass.LOCAL_WRITE,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = pageReady
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult =
                AgentToolResult(AgentToolStatus.SUCCEEDED, "Page text and source URL saved.", "Evidence saved.", "evidence:web-1")
        }
        val model = AgentModelProvider { systemPrompt, _ ->
            systemPrompts += systemPrompt
            responses.removeFirst()
        }

        val result = AgentExecutionEngine(model, listOf(navigate, capture))
            .execute("Open and inspect https://example.com/")

        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals(2, result.toolCalls)
        assertFalse(systemPrompts.first().contains("browser.capture_evidence"))
        assertTrue(systemPrompts[1].contains("browser.capture_evidence"))
        assertTrue(systemPrompts[2].contains("browser.capture_evidence"))
    }

}
