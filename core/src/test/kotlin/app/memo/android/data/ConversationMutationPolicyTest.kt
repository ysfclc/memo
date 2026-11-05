package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationMutationPolicyTest {
    @Test
    fun importedAndSystemMessagesAreReadOnly() {
        val source = ChatMessageRecord(id = "source", role = "SOURCE", content = "Imported")
        val system = ChatMessageRecord(id = "system", role = "SYSTEM", content = "System")
        assertTrue(ConversationMutationPolicy.messageMutationBlockReason(source, emptyList())!!.contains("read-only"))
        assertTrue(ConversationMutationPolicy.messageMutationBlockReason(system, emptyList())!!.contains("read-only"))
    }

    @Test
    fun userAndAssistantMessagesCanBeChangedUntilCitedAsEvidence() {
        val user = ChatMessageRecord(id = "user", role = "USER", content = "Question")
        val assistant = ChatMessageRecord(id = "assistant", role = "ASSISTANT", content = "Answer")
        assertNull(ConversationMutationPolicy.messageMutationBlockReason(user, emptyList()))
        assertNull(ConversationMutationPolicy.messageMutationBlockReason(assistant, emptyList()))
        val citation = EntityRecord(type = "KNOWLEDGE", title = "Saved fact", sourceMessageId = "user")
        assertTrue(ConversationMutationPolicy.messageMutationBlockReason(user, listOf(citation))!!.contains("provenance"))
    }

    @Test
    fun conversationDeletionCountsOnlyExternalDependencies() {
        val conversation = EntityRecord(id = "c1", type = "CONVERSATION", title = "Chat", sourceConversationId = "c1")
        val project = EntityRecord(type = "PROJECT", title = "Project", sourceConversationId = "c1")
        val job = WorkJobRecord(title = "Job", goal = "Goal", sourceConversationId = "c1")
        val relationship = EntityRelationship(fromEntityId = "c1", toEntityId = "project", relationType = "ABOUT")
        val otherRelationship = EntityRelationship(fromEntityId = "other", toEntityId = "else", relationType = "RELATED")
        assertEquals(3, ConversationMutationPolicy.conversationDependencyCount(
            "c1", listOf(conversation, project), listOf(job), listOf(relationship, otherRelationship)
        ))
        assertEquals(0, ConversationMutationPolicy.conversationDependencyCount("c1", listOf(conversation), emptyList(), emptyList()))
    }
}
