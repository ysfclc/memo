package app.memo.android.ui

import app.memo.android.data.AgentRunRecord
import app.memo.android.data.AppSnapshot
import app.memo.android.data.ChatMessageRecord
import app.memo.android.data.ConversationRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveAgentRunTimelineTest {
    private fun run(id: String = "run-123") = AgentRunRecord(
        id = id,
        conversationId = "conversation-1",
        goal = "Inspect the saved execution"
    )

    private fun event(
        id: String,
        at: Long,
        runMarker: String,
        sourceType: String = "AGENT_EVENT"
    ) = ChatMessageRecord(
        id = id,
        role = "TOOL",
        content = "Recorded execution event\n$runMarker",
        sourceType = sourceType,
        createdAt = at
    )

    @Test
    fun timelineIncludesOnlyExactRunEventsAndSortsChronologically() {
        val conversation = ConversationRecord(
            id = "conversation-1",
            title = "Execution",
            messages = listOf(
                event("later", 30L, "Run ID: run-123 · Call ID: call-later"),
                event("other-run", 20L, "Run ID: run-456 · Call ID: call-other"),
                event("earlier", 10L, "Run ID: run-123 · Status: COMPLETED"),
                event("ordinary-message", 5L, "Run ID: run-123", sourceType = "NATIVE")
            )
        )

        val actual = agentRunTimelineMessages(
            AppSnapshot(conversations = listOf(conversation)),
            run()
        )

        assertEquals(listOf("earlier", "later"), actual.map { it.id })
    }

    @Test
    fun runIdMarkerMustBeAnExactFinalMetadataField() {
        val conversation = ConversationRecord(
            id = "conversation-1",
            title = "Execution",
            messages = listOf(
                event("partial", 10L, "Run ID: run-123-extra · Call ID: call-1"),
                event("embedded", 20L, "Prefix Run ID: run-123"),
                ChatMessageRecord(
                    id = "not-final-metadata",
                    role = "TOOL",
                    content = "Run ID: run-123\nSummary continues after the marker",
                    sourceType = "AGENT_EVENT",
                    createdAt = 30L
                )
            )
        )

        assertTrue(agentRunTimelineMessages(AppSnapshot(conversations = listOf(conversation)), run()).isEmpty())
    }

    @Test
    fun missingOriginatingConversationProducesNoTimelineEvents() {
        val snapshot = AppSnapshot(conversations = emptyList())

        assertTrue(agentRunTimelineMessages(snapshot, run()).isEmpty())
    }

    @Test
    fun executionTimelineLabelsAreTranslated() {
        assertEquals("Oś czasu wykonania", MemoLocalization.translate("Execution timeline", "PL"))
        assertEquals("Yürütme zaman çizelgesi", MemoLocalization.translate("Execution timeline", "TR"))
    }
}
