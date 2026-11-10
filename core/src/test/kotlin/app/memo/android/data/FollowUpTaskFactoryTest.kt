package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FollowUpTaskFactoryTest {
    @Test fun createsLinkedTaskAndVerifiedJobWithoutUsingBrowserRuntime() {
        val conversationId = newId()
        val conversation = ConversationRecord(id = conversationId, title = "Store launch")
        val conversationEntity = EntityRecord(id = conversationId, type = "CONVERSATION", title = "Store launch", sourceConversationId = conversationId)
        val project = EntityRecord(type = "PROJECT", title = "Polish store", reviewStatus = "APPROVED", sourceConversationId = conversationId)
        val snapshot = AppSnapshot(conversations = listOf(conversation), entities = listOf(conversationEntity, project))

        val result = FollowUpTaskFactory.create(snapshot, project.id, "Compare suppliers", "Compare three supplier offers.")
        assertTrue(result.isSuccess)
        val created = result.getOrThrow()
        val task = created.snapshot.entities.single { it.id == created.taskId }
        val job = created.snapshot.jobs.single { it.id == created.jobId }
        assertEquals("TASK", task.type)
        assertEquals("APPROVED", task.reviewStatus)
        val sourceLink = created.snapshot.relationships.single { it.relationType == "HAS_TASK" }
        val conversationLink = created.snapshot.relationships.single { it.relationType == "PRODUCED" }
        assertEquals(project.id, sourceLink.fromEntityId)
        assertEquals(task.id, sourceLink.toEntityId)
        assertEquals(conversationId, conversationLink.fromEntityId)
        assertEquals(task.id, conversationLink.toEntityId)
        val entityIds = created.snapshot.entities.map { it.id }.toSet()
        assertTrue(created.snapshot.relationships.all { it.fromEntityId in entityIds && it.toEntityId in entityIds })
        assertEquals(job.id, task.sourceJobId)
        assertEquals("SUCCEEDED", job.status)
        assertEquals("app.entities", job.runtimeId)
        assertTrue(job.steps.all { it.status == "COMPLETED" })
        assertTrue(job.steps.last().evidence.contains(created.taskId))
    }

    @Test fun refusesDuplicateTaskAndUnapprovedSource() {
        val project = EntityRecord(type = "PROJECT", title = "Polish store", reviewStatus = "APPROVED")
        val existingTask = EntityRecord(type = "TASK", title = "Compare suppliers", reviewStatus = "APPROVED")
        val duplicate = FollowUpTaskFactory.create(AppSnapshot(entities = listOf(project, existingTask)), project.id, "Compare suppliers", "Compare offers.")
        assertTrue(duplicate.isFailure)

        val proposedProject = project.copy(reviewStatus = "PROPOSED")
        val unapproved = FollowUpTaskFactory.create(AppSnapshot(entities = listOf(proposedProject)), proposedProject.id, "Compare suppliers", "Compare offers.")
        assertTrue(unapproved.isFailure)
    }
}
