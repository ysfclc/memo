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
        assertEquals(7, org.json.JSONObject(snapshot.toJson()).optInt("schemaVersion"))
        assertEquals("7.0.0", org.json.JSONObject(snapshot.toJson()).optString("schemaSemver"))
    }

    @Test fun schemaSevenPersistsAttemptJournalAndSchemaSixMigratesAdditively() {
        val job = WorkJobRecord(
            id = "journal-job", title = "Prepare report", goal = "Create a sourced report",
            runtimeId = "app.jobs", status = "WAITING_FOR_USER",
            steps = listOf(JobStepRecord(id = "journal-step", title = "Draft", status = "NEEDS_REVIEW"))
        )
        val evidence = EvidenceRecord(
            id = "journal-evidence", taskId = job.id, title = "Draft", sourceUrl = "",
            kind = "WORK_ARTIFACT", content = "Private draft", summary = "Needs review",
            metadata = org.json.JSONObject().put("jobId", job.id).put("stepId", "journal-step")
                .put("requiresHumanReview", true).toString()
        )
        val attempt = WorkflowAttemptRecord(
            id = "attempt-1", jobId = job.id, stepId = "journal-step", attemptNumber = 1,
            idempotencyKey = WorkflowAttemptJournal.legacyIdempotencyKey(job.id, "journal-step"),
            runtimeId = "app.jobs", status = "OUTPUT_SAVED", startedAt = 10L, finishedAt = 20L,
            evidenceId = evidence.id, detail = "Saved for review"
        )
        val original = AppSnapshot(jobs = listOf(job), evidence = listOf(evidence), workflowAttempts = listOf(attempt))
        val json = org.json.JSONObject(original.toJson())
        assertEquals(7, json.optInt("schemaVersion"))
        assertEquals("7.0.0", json.optString("schemaSemver"))
        val restored = AppSnapshot.fromJson(json.toString())
        assertEquals(attempt, restored.workflowAttempts.single())

        val schemaSix = org.json.JSONObject(original.copy(workflowAttempts = emptyList()).toJson())
        schemaSix.put("schemaVersion", 6)
        schemaSix.put("schemaSemver", "6.0.0")
        schemaSix.put("minimumReaderVersion", 6)
        schemaSix.remove("workflowAttempts")
        val migrated = AppSnapshot.fromJson(schemaSix.toString())
        assertTrue(migrated.workflowAttempts.isEmpty())
        assertEquals(7, org.json.JSONObject(migrated.toJson()).optInt("schemaVersion"))
    }

    @Test fun tamperedAttemptReferencesAreRejected() {
        val job = WorkJobRecord(
            id = "journal-job", title = "Prepare report", goal = "Create a report",
            runtimeId = "app.jobs", status = "RUNNING",
            steps = listOf(JobStepRecord(id = "journal-step", title = "Draft", status = "RUNNING"))
        )
        val attempt = WorkflowAttemptRecord(
            id = "attempt-1", jobId = job.id, stepId = "journal-step", attemptNumber = 1,
            idempotencyKey = WorkflowAttemptJournal.legacyIdempotencyKey(job.id, "journal-step"),
            runtimeId = "app.jobs", startedAt = 10L
        )
        val root = org.json.JSONObject(AppSnapshot(jobs = listOf(job), workflowAttempts = listOf(attempt)).toJson())
        root.getJSONArray("workflowAttempts").getJSONObject(0).put("jobId", "missing-job")
        assertTrue(runCatching { AppSnapshot.fromJson(root.toString()) }.isFailure)
    }

    @Test fun interruptedJobsAreRecoveredIntoReviewState() {
        val running = WorkJobRecord(
            id = "interrupted-job", title = "Extract outcomes", goal = "Extract outcomes",
            runtimeId = "app.jobs", status = "RUNNING",
            steps = listOf(JobStepRecord(id = "interrupted-step", title = "Run model", status = "RUNNING"))
        )
        val attempt = WorkflowAttemptRecord(
            id = "interrupted-attempt", jobId = running.id, stepId = "interrupted-step", attemptNumber = 1,
            idempotencyKey = WorkflowAttemptJournal.legacyIdempotencyKey(running.id, "interrupted-step"),
            runtimeId = "app.jobs", startedAt = 10L
        )
        val recovered = AppSnapshot(jobs = listOf(running), workflowAttempts = listOf(attempt)).recoverInterruptedTasks()
        assertEquals("WAITING_FOR_USER", recovered.jobs.single().status)
        assertEquals("NEEDS_REVIEW", recovered.jobs.single().steps.single().status)
        assertEquals("INTERRUPTED", recovered.workflowAttempts.single().status)
        assertTrue(recovered.workflowAttempts.single().finishedAt >= 10L)
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
        assertEquals(7, org.json.JSONObject(restored.toJson()).optInt("schemaVersion"))
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
