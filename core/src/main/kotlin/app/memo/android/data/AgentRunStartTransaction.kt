package app.memo.android.data

/**
 * Pure preparation for starting a Talk execution.
 *
 * The user's message and the new durable run record are added to one immutable
 * workspace snapshot. The caller must persist and read back that complete
 * snapshot before invoking the model or any capability.
 */
data class AgentRunStartPreparation(
    val snapshot: AppSnapshot?,
    val userMessageId: String = "",
    val reason: String = ""
) {
    val accepted: Boolean
        get() = snapshot != null && userMessageId.isNotBlank()
}

object AgentRunStartTransaction {
    fun prepare(
        current: AppSnapshot,
        runId: String,
        conversationId: String,
        goal: String,
        modelId: String,
        userMessage: String,
        continuationOfRunId: String = ""
    ): AgentRunStartPreparation {
        val cleanRunId = runId.trim()
        val cleanConversationId = conversationId.trim()
        val cleanGoal = goal.trim().take(6_000)
        val cleanMessage = userMessage.trim().take(24_000)
        if (cleanRunId.isBlank() || cleanRunId.length > 128) {
            return rejected("Memo could not create a valid execution ID.")
        }
        if (cleanConversationId.isBlank() || cleanConversationId.length > 128) {
            return rejected("The originating conversation no longer exists.")
        }
        if (cleanGoal.isBlank() || cleanMessage.isBlank()) {
            return rejected("A non-empty user message and execution goal are required.")
        }
        if (current.agentRuns.size >= MAX_AGENT_RUNS) {
            return rejected("Memo execution history is full. Export your workspace before starting another run.")
        }
        if (current.agentRuns.any { it.id == cleanRunId }) {
            return rejected("Memo execution ID already exists.")
        }
        val conversation = current.conversations.firstOrNull { it.id == cleanConversationId }
            ?: return rejected("The originating conversation no longer exists.")
        if (conversation.messages.size >= MAX_USER_MESSAGES_PER_CONVERSATION) {
            return rejected("This conversation is at its message limit. Export the full history before continuing in a linked conversation.")
        }
        val parentRun = if (continuationOfRunId.isBlank()) {
            null
        } else {
            val resolution = AgentRunContinuationPolicy.resolveParent(
                parentRunId = continuationOfRunId,
                conversationId = cleanConversationId,
                runs = current.agentRuns
            )
            if (!resolution.assessment.allowed || resolution.parentRun == null) {
                return rejected(resolution.assessment.reason)
            }
            resolution.parentRun
        }

        val now = nowMillis()
        val message = ChatMessageRecord(
            role = "USER",
            content = cleanMessage,
            createdAt = now
        )
        val title = if (conversation.title == "New conversation") {
            cleanMessage.lineSequence().first().trim().take(72).ifBlank { "New conversation" }
        } else {
            conversation.title
        }
        val updatedConversation = conversation.copy(
            title = title,
            messages = conversation.messages + message,
            updatedAt = now
        )
        val run = AgentRunRecord(
            id = cleanRunId,
            conversationId = cleanConversationId,
            goal = cleanGoal,
            parentRunId = parentRun?.id.orEmpty(),
            modelId = modelId.trim().take(160)
        )
        val next = current.copy(
            conversations = current.conversations.map {
                if (it.id == cleanConversationId) updatedConversation else it
            },
            entities = current.entities.map {
                if (it.id == cleanConversationId) it.copy(title = title, updatedAt = now) else it
            },
            agentRuns = listOf(run) + current.agentRuns
        )
        return AgentRunStartPreparation(
            snapshot = next,
            userMessageId = message.id
        )
    }

    private fun rejected(reason: String) = AgentRunStartPreparation(
        snapshot = null,
        reason = reason
    )
}
