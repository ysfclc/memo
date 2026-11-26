package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AgentRunStartTransactionTest {
    private fun snapshot(
        messages: List<ChatMessageRecord> = emptyList(),
        runs: List<AgentRunRecord> = emptyList()
    ) = AppSnapshot(
        conversations = listOf(
            ConversationRecord(
                id = "conversation-1",
                title = "New conversation",
                messages = messages
            )
        ),
        agentRuns = runs
    )

    private fun prepare(
        current: AppSnapshot,
        runId: String = "child-run",
        goal: String = "Research the requested subject",
        userMessage: String = "Research the requested subject",
        continuationOfRunId: String = ""
    ) = AgentRunStartTransaction.prepare(
        current = current,
        runId = runId,
        conversationId = "conversation-1",
        goal = goal,
        modelId = "local-model",
        userMessage = userMessage,
        continuationOfRunId = continuationOfRunId
    )

    @Test
    fun storesUserMessageAndNewRunInOneSnapshot() {
        val existing = ChatMessageRecord(role = "USER", content = "Earlier request")
        val current = snapshot(messages = listOf(existing))

        val prepared = prepare(current, userMessage = "Build the requested report")

        assertTrue(prepared.reason, prepared.accepted)
        val next = requireNotNull(prepared.snapshot)
        val conversation = next.conversations.single()
        val message = conversation.messages.last()
        val run = next.agentRuns.single()

        assertEquals(2, conversation.messages.size)
        assertEquals(prepared.userMessageId, message.id)
        assertEquals("USER", message.role)
        assertEquals("Build the requested report", message.content)
        assertEquals("Build the requested report", conversation.title)
        assertEquals("RUNNING", run.status)
        assertEquals("Research the requested subject", run.goal)
        assertEquals("", run.parentRunId)

        assertEquals(listOf(existing), current.conversations.single().messages)
        assertTrue(current.agentRuns.isEmpty())
    }

    @Test
    fun linksContinuationParentInSameSnapshotAsUserMessage() {
        val parent = AgentRunRecord(
            id = "parent-run",
            conversationId = "conversation-1",
            goal = "Original goal",
            status = "NEEDS_REVIEW",
            lastEventType = "RUN_NEEDS_REVIEW"
        )
        val current = snapshot(runs = listOf(parent))

        val prepared = prepare(
            current = current,
            continuationOfRunId = parent.id,
            userMessage = "Continue after reviewing the saved evidence"
        )

        assertTrue(prepared.reason, prepared.accepted)
        val next = requireNotNull(prepared.snapshot)
        assertEquals(parent.id, next.agentRuns.first().parentRunId)
        assertEquals(prepared.userMessageId, next.conversations.single().messages.last().id)
        assertEquals(2, next.agentRuns.size)
        assertEquals(parent, next.agentRuns.last())
    }

    @Test
    fun rejectsUnsafeParentWithoutPartialAppend() {
        val unsafeParent = AgentRunRecord(
            id = "parent-run",
            conversationId = "conversation-1",
            goal = "Original goal",
            status = "NEEDS_REVIEW",
            lastEventType = "TOOL_STARTED",
            toolCalls = 1,
            successfulToolCalls = 0
        )
        val current = snapshot(runs = listOf(unsafeParent))

        val prepared = prepare(current, continuationOfRunId = unsafeParent.id)

        assertFalse(prepared.accepted)
        assertNull(prepared.snapshot)
        assertTrue(prepared.reason.contains("may have started"))
        assertTrue(current.conversations.single().messages.isEmpty())
        assertEquals(listOf(unsafeParent), current.agentRuns)
    }

    @Test
    fun rejectsFullConversationWithoutPartialAppend() {
        val current = snapshot(
            messages = List(MAX_USER_MESSAGES_PER_CONVERSATION) {
                ChatMessageRecord(role = "USER", content = "Saved message $it")
            }
        )

        val prepared = prepare(current)

        assertFalse(prepared.accepted)
        assertNull(prepared.snapshot)
        assertTrue(prepared.reason.contains("message limit"))
        assertEquals(MAX_USER_MESSAGES_PER_CONVERSATION, current.conversations.single().messages.size)
        assertTrue(current.agentRuns.isEmpty())
    }

    @Test
    fun rejectsBlankMessageGoalAndDuplicateRunId() {
        val current = snapshot()
        assertFalse(prepare(current, userMessage = "  ").accepted)
        assertFalse(prepare(current, goal = "  ").accepted)

        val duplicate = AgentRunRecord(
            id = "child-run",
            conversationId = "conversation-1",
            goal = "Existing run",
            status = "NEEDS_REVIEW",
            lastEventType = "RUN_NEEDS_REVIEW"
        )
        assertFalse(prepare(snapshot(runs = listOf(duplicate))).accepted)
    }
}
