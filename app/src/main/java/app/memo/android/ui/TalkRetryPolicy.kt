package app.memo.android.ui

import app.memo.android.data.ChatMessageRecord

/**
 * A failed model request already has a USER turn in the transcript. Reuse that turn
 * when the same draft is retried after one or more recorded request errors, instead
 * of appending duplicate copies of the same message to conversation history.
 *
 * A cancelled/partial turn is deliberately not reused: it represents a distinct
 * interrupted attempt and remains visible in the transcript.
 */
internal fun shouldAppendTalkUserMessage(
    messages: List<ChatMessageRecord>,
    userText: String
): Boolean {
    val normalizedText = userText.trim()
    if (normalizedText.isBlank()) return false

    val lastUserIndex = messages.indexOfLast { it.role == "USER" }
    if (lastUserIndex < 0) return true

    val lastUser = messages[lastUserIndex]
    if (lastUser.content != normalizedText) return true

    val trailingMessages = messages.drop(lastUserIndex + 1)
    val retryableFailure = trailingMessages.isNotEmpty() && trailingMessages.all {
        it.role == "SYSTEM" && it.sourceType == "ERROR"
    }
    return !retryableFailure
}
