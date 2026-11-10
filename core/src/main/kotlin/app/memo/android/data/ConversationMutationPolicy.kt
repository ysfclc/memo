package app.memo.android.data

/** Guards user mutations so source evidence and linked workspace records are never silently orphaned. */
object ConversationMutationPolicy {
    fun messageMutationBlockReason(message: ChatMessageRecord, entities: List<EntityRecord>): String? {
        if (message.role !in setOf("USER", "ASSISTANT")) {
            return "Imported source and system messages are read-only."
        }
        if (entities.any { it.sourceMessageId == message.id }) {
            return "This message is cited by a workspace record. Keep its original wording so the record's provenance remains valid."
        }
        return null
    }

    fun conversationDependencyCount(
        conversationId: String,
        entities: List<EntityRecord>,
        jobs: List<WorkJobRecord>,
        relationships: List<EntityRelationship>
    ): Int {
        val linkedEntities = entities.count { it.id != conversationId && it.sourceConversationId == conversationId }
        val linkedJobs = jobs.count { it.sourceConversationId == conversationId }
        val linkedRelationships = relationships.count {
            it.sourceConversationId == conversationId || it.fromEntityId == conversationId || it.toEntityId == conversationId
        }
        return linkedEntities + linkedJobs + linkedRelationships
    }
}
