package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceSchemaV3Test {
    @Test fun conversationsEntitiesRelationshipsAndJobsRoundTrip() {
        val conversationId = newId()
        val conversationEntity = EntityRecord(id = conversationId, type = "CONVERSATION", title = "Store launch", sourceConversationId = conversationId)
        val job = WorkJobRecord(title = "Extract outcomes", goal = "Create source-grounded proposals", status = "WAITING_FOR_USER", sourceConversationId = conversationId, steps = listOf(JobStepRecord(title = "Validate", status = "COMPLETED", evidence = "2 proposals")))
        val project = EntityRecord(type = "PROJECT", title = "Polish store", reviewStatus = "PROPOSED", sourceConversationId = conversationId, sourceJobId = job.id, sourceExcerpt = "launch the Polish store")
        val snapshot = AppSnapshot(
            conversations = listOf(ConversationRecord(
                id = conversationId,
                title = "Store launch",
                providerId = "local:qwen",
                messages = listOf(ChatMessageRecord(role = "USER", content = "Let's launch the Polish store."))
            )),
            entities = listOf(conversationEntity, project),
            relationships = listOf(EntityRelationship(fromEntityId = conversationId, toEntityId = project.id, relationType = "PROPOSES", sourceConversationId = conversationId)),
            jobs = listOf(job)
        )
        val restored = AppSnapshot.fromJson(snapshot.toJson())
        assertEquals(1, restored.conversations.size)
        assertEquals("Let's launch the Polish store.", restored.conversations.single().messages.single().content)
        assertEquals(2, restored.entities.size)
        assertEquals(job.id, restored.entities.first { it.type == "PROJECT" }.sourceJobId)
        assertEquals("PROPOSES", restored.relationships.single().relationType)
        assertEquals("WAITING_FOR_USER", restored.jobs.single().status)
        assertEquals("COMPLETED", restored.jobs.single().steps.single().status)
        assertEquals("USER_CHECKPOINT", restored.jobs.single().steps.single().checkpointKind)
        assertEquals(6, org.json.JSONObject(snapshot.toJson()).optInt("schemaVersion"))
        assertEquals("6.0.0", org.json.JSONObject(snapshot.toJson()).optString("schemaSemver"))
    }

    @Test fun interruptedJobsAreRecoveredIntoReviewState() {
        val running = WorkJobRecord(title = "Extract outcomes", goal = "Extract outcomes", status = "RUNNING", steps = listOf(JobStepRecord(title = "Run model", status = "RUNNING")))
        val recovered = AppSnapshot(jobs = listOf(running)).recoverInterruptedTasks()
        assertEquals("WAITING_FOR_USER", recovered.jobs.single().status)
        assertEquals("NEEDS_REVIEW", recovered.jobs.single().steps.single().status)
        assertTrue(recovered.activity.first().message.contains("interrupted job"))
    }

    @Test fun v4BackupMigratesAdditiveCheckpointFieldsAndSemver() {
        val original = AppSnapshot(jobs = listOf(WorkJobRecord(title = "Plan", goal = "Goal", steps = listOf(JobStepRecord(title = "Review", checkpointKind = "HUMAN_REVIEW", requiresApproval = false)))))
        val root = org.json.JSONObject(original.toJson())
        root.put("schemaVersion", 4)
        root.remove("schemaSemver")
        root.remove("minimumReaderVersion")
        val rows = root.getJSONArray("jobs").getJSONObject(0).getJSONArray("steps")
        rows.getJSONObject(0).remove("checkpointKind")
        rows.getJSONObject(0).remove("requiresApproval")
        val restored = AppSnapshot.fromJson(root.toString())
        assertEquals("USER_CHECKPOINT", restored.jobs.single().steps.single().checkpointKind)
        assertEquals(false, restored.jobs.single().steps.single().requiresApproval)
        assertEquals(6, org.json.JSONObject(restored.toJson()).optInt("schemaVersion"))
    }

    @Test fun inconsistentSemverAndUnknownFutureSchemaAreRejected() {
        val root = org.json.JSONObject(AppSnapshot().toJson())
        root.put("schemaSemver", "4.2.0")
        assertTrue(runCatching { AppSnapshot.fromJson(root.toString()) }.isFailure)
        root.put("schemaVersion", 99)
        root.put("schemaSemver", "99.0.0")
        root.put("minimumReaderVersion", 99)
        assertTrue(runCatching { AppSnapshot.fromJson(root.toString()) }.isFailure)
    }

}
