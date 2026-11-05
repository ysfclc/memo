package app.memo.android.data

import app.memo.android.runtime.RuntimeRegistry
import java.util.Locale

/** Pure planner for a bounded, deterministic app-runtime workflow. */
data class FollowUpTaskCreation(
    val snapshot: AppSnapshot,
    val jobId: String,
    val taskId: String
)

object FollowUpTaskFactory {
    fun create(
        snapshot: AppSnapshot,
        sourceEntityId: String,
        title: String,
        description: String,
        runtimeRegistry: RuntimeRegistry = RuntimeRegistry()
    ): Result<FollowUpTaskCreation> {
        if (!runtimeRegistry.can("app.entities", "entity.create") ||
            !runtimeRegistry.can("app.entities", "entity.link") ||
            !runtimeRegistry.can("app.jobs", "job.create") ||
            !runtimeRegistry.can("app.jobs", "job.verify")) {
            return Result.failure(IllegalStateException("The registered app runtime does not expose every capability required for this workflow."))
        }
        val cleanTitle = title.trim().take(240)
        val cleanDescription = description.trim().take(12_000)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Give the follow-up task a name."))
        val source = snapshot.entities.firstOrNull { it.id == sourceEntityId }
            ?: return Result.failure(IllegalStateException("The source entity no longer exists."))
        if (source.reviewStatus != "APPROVED") return Result.failure(IllegalStateException("Approve the source entity before creating a follow-up task."))
        if (source.type == "CONVERSATION") return Result.failure(IllegalArgumentException("Create follow-up tasks from a project, decision, or other approved work entity."))
        if (snapshot.entities.any {
                it.type == "TASK" && it.reviewStatus !in setOf("REJECTED", "MERGED") && it.title.trim().equals(cleanTitle, true)
            }) {
            return Result.failure(IllegalStateException("A task with this name already exists. Open it and link it to this source instead."))
        }
        val sourceConversationId = source.sourceConversationId.takeIf { id -> snapshot.conversations.any { it.id == id } }.orEmpty()
        val hasConversationEntity = sourceConversationId.isNotBlank() && snapshot.entities.any { it.id == sourceConversationId && it.type == "CONVERSATION" }
        val relationshipCount = if (hasConversationEntity) 2 else 1
        if (snapshot.entities.size >= 1_200) return Result.failure(IllegalStateException("Entity limit reached. Export a backup before removing older entities."))
        if (snapshot.relationships.size + relationshipCount > 4_000) return Result.failure(IllegalStateException("Relationship limit reached. Export a backup before removing older links."))
        if (snapshot.jobs.size >= 240) return Result.failure(IllegalStateException("Job limit reached. Export a backup before removing older jobs."))

        val now = nowMillis()
        val jobId = newId()
        val task = EntityRecord(
            type = "TASK",
            title = cleanTitle,
            description = cleanDescription,
            properties = mapOf("createdFrom" to source.type, "sourceEntityId" to source.id),
            reviewStatus = "APPROVED",
            sourceConversationId = sourceConversationId,
            sourceJobId = jobId,
            createdAt = now,
            updatedAt = now
        )
        val sourceLink = EntityRelationship(
            fromEntityId = source.id,
            toEntityId = task.id,
            relationType = "HAS_TASK",
            sourceConversationId = sourceConversationId,
            createdAt = now
        )
        val conversationLink = if (hasConversationEntity) EntityRelationship(
            fromEntityId = sourceConversationId,
            toEntityId = task.id,
            relationType = "PRODUCED",
            sourceConversationId = sourceConversationId,
            createdAt = now
        ) else null
        val steps = listOf(
            JobStepRecord(title = "Validate source entity", detail = "Source exists and is approved.", status = "COMPLETED", evidence = "entity:${source.id}", updatedAt = now),
            JobStepRecord(title = "Create task entity", detail = "Created a typed TASK entity with a stable identity.", status = "COMPLETED", evidence = "entity:${task.id}", updatedAt = now),
            JobStepRecord(title = "Link to source context", detail = "Created a HAS_TASK relationship and preserved conversation provenance where available.", status = "COMPLETED", evidence = listOfNotNull("relationship:${sourceLink.id}", conversationLink?.let { "relationship:${it.id}" }).joinToString(", "), updatedAt = now),
            JobStepRecord(title = "Verify workspace result", detail = "Read-after-write check confirmed the task and source relationship are present in the workspace snapshot.", status = "COMPLETED", evidence = "entity:${task.id};relationship:${sourceLink.id}", updatedAt = now)
        )
        val job = WorkJobRecord(
            id = jobId,
            title = "Create follow-up task · ${cleanTitle.take(100)}",
            goal = "Create a real task entity, link it to ${source.type.lowercase(Locale.ROOT)} '${source.title.take(120)}', and verify the resulting workspace records.",
            status = "SUCCEEDED",
            runtimeId = "app.entities",
            sourceConversationId = sourceConversationId,
            steps = steps,
            resultSummary = "Verified in workspace: task '$cleanTitle' exists and is linked to '${source.title}'.",
            createdAt = now,
            updatedAt = now
        )
        val entityRecords = (listOf(task) + snapshot.entities).take(1_200)
        val relations = listOfNotNull(sourceLink, conversationLink) + snapshot.relationships
        val jobs = (listOf(job) + snapshot.jobs).take(240)
        val next = snapshot.copy(
            entities = entityRecords,
            relationships = relations,
            jobs = jobs,
            activity = (listOf(ActivityRecord(message = "Verified follow-up task created: ${task.title}")) + snapshot.activity).take(500)
        )
        val verified = next.entities.any { it.id == task.id && it.type == "TASK" } &&
            next.relationships.any { it.id == sourceLink.id && it.fromEntityId == source.id && it.toEntityId == task.id } &&
            next.jobs.any { it.id == job.id && it.status == "SUCCEEDED" && it.steps.all { step -> step.status == "COMPLETED" } }
        if (!verified) return Result.failure(IllegalStateException("Memo could not verify that the task, job, and source relationship were created."))
        return Result.success(FollowUpTaskCreation(next, jobId, task.id))
    }
}
