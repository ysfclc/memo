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
    fun approvedExternalCapabilityRunsOnlyAfterExactApprovalGrant() = runBlocking {
        val envelope = AgentApprovalEnvelope(
            runtimeId = "runtime.browser",
            capabilityId = "browser.submit_prompt",
            destination = "https://chat.example.com/",
            title = "Send reviewed prompt",
            payload = "Summarize the current report.",
            risk = "EXTERNAL_WRITE"
        )
        var approvedExecutionCount = 0
        var ordinaryExecutionCount = 0
        var approvalRequest: AgentApprovalRequest? = null
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.submit_prompt",
                description = "Send an exact reviewed prompt to the current page.",
                inputSchemaJson = schema,
                executionClass = AgentToolExecutionClass.EXTERNAL_WRITE,
                requiresUserApproval = true,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = true
            override fun approvalEnvelope(arguments: JSONObject) = envelope
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                ordinaryExecutionCount++
                return AgentToolResult(AgentToolStatus.FAILED, "The unapproved invoke path must not run.")
            }
            override suspend fun invokeApproved(
                arguments: JSONObject,
                invocation: AgentToolInvocation,
                grant: AgentApprovalGrant
            ): AgentToolResult {
                assertEquals("action-1", grant.actionId)
                assertEquals("sha256-exact-envelope", grant.contentHash)
                assertEquals("send now", arguments.getString("query"))
                approvedExecutionCount++
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "The visible conversation confirms the prompt.", evidenceReference = "evidence:prompt-1")
            }
        }
        val events = mutableListOf<AgentExecutionEvent>()
        val responses = ArrayDeque(
            listOf(
                """{"type":"tool_call","tool":"browser.submit_prompt","arguments":{"query":"send now"}}""",
                """{"type":"final","content":"The prompt was submitted and verified."}"""
            )
        )
        val engine = AgentExecutionEngine(AgentModelProvider { _, _ -> responses.removeFirst() }, listOf(adapter))
        val result = engine.execute(
            "Submit the approved prompt",
            onApprovalRequired = { request ->
                approvalRequest = request
                AgentApprovalGrant("action-1", "sha256-exact-envelope")
            },
            onEvent = { events += it }
        )

        assertEquals(AgentRunStatus.FINISHED, result.status)
        assertEquals(envelope, approvalRequest?.envelope)
        assertEquals(1, approvedExecutionCount)
        assertEquals(0, ordinaryExecutionCount)
        assertEquals(1, result.toolCalls)
        assertEquals(1, result.successfulToolCalls)
        assertTrue(events.map { it.type }.containsAll(listOf(
            AgentExecutionEventType.TOOL_APPROVAL_REQUESTED,
            AgentExecutionEventType.TOOL_APPROVAL_GRANTED,
            AgentExecutionEventType.TOOL_STARTED,
            AgentExecutionEventType.TOOL_FINISHED
        )))
    }

    @Test
    fun rejectedExternalApprovalNeverInvokesCapability() = runBlocking {
        var invoked = false
        var asked = false
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.submit_prompt",
                description = "Send an exact reviewed prompt.",
                inputSchemaJson = schema,
                executionClass = AgentToolExecutionClass.EXTERNAL_WRITE,
                requiresUserApproval = true,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = true
            override fun approvalEnvelope(arguments: JSONObject) = AgentApprovalEnvelope(
                "runtime.browser", "browser.submit_prompt", "https://chat.example.com/",
                "Send reviewed prompt", arguments.getString("query")
            )
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                invoked = true
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "must not run")
            }
            override suspend fun invokeApproved(arguments: JSONObject, invocation: AgentToolInvocation, grant: AgentApprovalGrant): AgentToolResult {
                invoked = true
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "must not run")
            }
        }
        val model = AgentModelProvider { _, _ ->
            """{"type":"tool_call","tool":"browser.submit_prompt","arguments":{"query":"send now"}}"""
        }
        val events = mutableListOf<AgentExecutionEvent>()
        val result = AgentExecutionEngine(model, listOf(adapter)).execute(
            "Submit the prompt",
            onApprovalRequired = { asked = true; null },
            onEvent = { events += it }
        )

        assertTrue(asked)
        assertFalse(invoked)
        assertEquals(AgentRunStatus.NEEDS_REVIEW, result.status)
        assertEquals(0, result.toolCalls)
        assertTrue(events.any { it.type == AgentExecutionEventType.TOOL_APPROVAL_REJECTED })
    }

    @Test
    fun mismatchedApprovalEnvelopeCannotReachHumanApprovalHandler() = runBlocking {
        var approvalUiOpened = false
        var invoked = false
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition(
                id = "browser.submit_prompt",
                description = "Send an exact reviewed prompt.",
                inputSchemaJson = schema,
                executionClass = AgentToolExecutionClass.EXTERNAL_WRITE,
                requiresUserApproval = true,
                runtimeId = "runtime.browser"
            )
            override fun isAvailable() = true
            override fun approvalEnvelope(arguments: JSONObject) = AgentApprovalEnvelope(
                "runtime.other", "browser.submit_prompt", "https://chat.example.com/",
                "Send reviewed prompt", arguments.getString("query")
            )
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                invoked = true
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "must not run")
            }
        }
        val model = AgentModelProvider { _, _ ->
            """{"type":"tool_call","tool":"browser.submit_prompt","arguments":{"query":"send now"}}"""
        }
        val result = AgentExecutionEngine(model, listOf(adapter)).execute(
            "Submit the prompt",
            onApprovalRequired = { approvalUiOpened = true; AgentApprovalGrant("action-1", "hash") }
        )

        assertEquals(AgentRunStatus.NEEDS_REVIEW, result.status)
        assertFalse(approvalUiOpened)
        assertFalse(invoked)
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

    @Test
    fun eventPersistenceFailureAfterToolExecutionStopsRunForReview() = runBlocking {
        var modelCalls = 0
        var capabilityCalls = 0
        val model = AgentModelProvider { _, _ ->
            modelCalls++
            if (modelCalls == 1) {
                """{"type":"tool_call","tool":"workspace.search","arguments":{"query":"records"}}"""
            } else {
                """{"type":"final","content":"Finished"}"""
            }
        }
        val adapter = object : AgentCapabilityAdapter {
            override val definition = AgentToolDefinition("workspace.search", "Search saved records.", schema)
            override fun isAvailable() = true
            override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
                capabilityCalls++
                return AgentToolResult(AgentToolStatus.SUCCEEDED, "Search result returned.")
            }
        }
        val result = AgentExecutionEngine(model, listOf(adapter)).execute("Search records") { event ->
            if (event.type == AgentExecutionEventType.TOOL_FINISHED) error("event store unavailable")
        }

        assertEquals(AgentRunStatus.NEEDS_REVIEW, result.status)
        assertEquals(1, modelCalls)
        assertEquals(1, capabilityCalls)
        assertEquals(1, result.toolCalls)
        assertTrue(result.finalMessage.contains("review the action outcome"))
    }


}
