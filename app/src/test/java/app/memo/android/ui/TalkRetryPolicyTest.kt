package app.memo.android.ui

import app.memo.android.data.ChatMessageRecord
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TalkRetryPolicyTest {
    @Test
    fun firstAttemptAppendsUserMessage() {
        assertTrue(shouldAppendTalkUserMessage(emptyList(), "Check this for me"))
    }

    @Test
    fun retryAfterCommittedUserTurnWithoutRuntimeResultReusesExistingMessage() {
        val messages = listOf(ChatMessageRecord(role = "USER", content = "Check this for me"))

        assertFalse(shouldAppendTalkUserMessage(messages, "Check this for me"))
    }

    @Test
    fun retryAfterRecordedRequestErrorReusesExistingUserMessage() {
        val messages = listOf(
            ChatMessageRecord(role = "USER", content = "Check this for me"),
            ChatMessageRecord(role = "SYSTEM", content = "Network unavailable", sourceType = "ERROR")
        )

        assertFalse(shouldAppendTalkUserMessage(messages, "Check this for me"))
    }

    @Test
    fun repeatedFailuresDoNotDuplicateTheOriginalUserMessage() {
        val messages = listOf(
            ChatMessageRecord(role = "USER", content = "Check this for me"),
            ChatMessageRecord(role = "SYSTEM", content = "Network unavailable", sourceType = "ERROR"),
            ChatMessageRecord(role = "SYSTEM", content = "Provider unavailable", sourceType = "ERROR")
        )

        assertFalse(shouldAppendTalkUserMessage(messages, "Check this for me"))
    }

    @Test
    fun changedDraftStartsANewUserTurn() {
        val messages = listOf(
            ChatMessageRecord(role = "USER", content = "Check this for me"),
            ChatMessageRecord(role = "SYSTEM", content = "Network unavailable", sourceType = "ERROR")
        )

        assertTrue(shouldAppendTalkUserMessage(messages, "Check this updated version"))
    }

    @Test
    fun completedOrPartialTurnIsNotMistakenForRetryableFailure() {
        val completed = listOf(
            ChatMessageRecord(role = "USER", content = "Check this for me"),
            ChatMessageRecord(role = "ASSISTANT", content = "Here is the result")
        )
        val partial = listOf(
            ChatMessageRecord(role = "USER", content = "Check this for me"),
            ChatMessageRecord(role = "ASSISTANT", content = "Partial output", sourceType = "PARTIAL")
        )

        assertTrue(shouldAppendTalkUserMessage(completed, "Check this for me"))
        assertTrue(shouldAppendTalkUserMessage(partial, "Check this for me"))
    }
}
