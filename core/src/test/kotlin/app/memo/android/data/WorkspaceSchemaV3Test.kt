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
        assertEquals(7, org.json.JSONObject(restored.toJson()).optInt("schemaVersion"))
    }

    @Test fun agentRunRecordsRoundTripWithExactRunAndConversationIdentity() {
        val run = AgentRunRecord(
            id = "run-12345678",
            conversationId = "conversation-1",
            goal = "Find three relevant records",
            modelId = "local:model",
            status = "WAITING_FOR_APPROVAL",
            stage = "Waiting for exact-content approval",
            lastEventType = "TOOL_APPROVAL_REQUESTED",
            lastToolId = "browser.submit_prompt",
            pendingApprovalCallId = "call-1",
            modelCalls = 2,
            toolCalls = 0,
            createdAt = 10L,
            updatedAt = 20L
        )
        val source = AppSnapshot(
            conversations = listOf(ConversationRecord(id = "conversation-1", title = "Run journal")),
            agentRuns = listOf(run)
        )
        val restored = AppSnapshot.fromJson(source.toJson())
        assertEquals(run, restored.agentRuns.single())
        assertEquals(7, org.json.JSONObject(source.toJson()).optInt("schemaVersion"))
    }

    @Test fun schemaSixBackupMigratesWithoutAgentRunRecords() {
        val root = org.json.JSONObject(AppSnapshot().toJson())
        root.put("schemaVersion", 6)
        root.put("schemaSemver", "6.0.0")
        root.put("minimumReaderVersion", 6)
        root.remove("agentRuns")
        val restored = AppSnapshot.fromJson(root.toString())
        assertTrue(restored.agentRuns.isEmpty())
        assertEquals(7, org.json.JSONObject(restored.toJson()).optInt("schemaVersion"))
    }

    @Test fun interruptedAgentRunsRecoverToReviewAndTerminalRunsStayTerminal() {
        val running = AgentRunRecord(id = "run-1", conversationId = "conversation-1", goal = "Inspect records", status = "RUNNING", toolCalls = 1)
        val waiting = AgentRunRecord(id = "run-2", conversationId = "conversation-1", goal = "Submit exact prompt", status = "WAITING_FOR_APPROVAL", pendingApprovalCallId = "call-2")
        val completed = AgentRunRecord(id = "run-3", conversationId = "conversation-1", goal = "Summarize records", status = "COMPLETED", finishedAt = 30L)
        val recovered = AppSnapshot(agentRuns = listOf(running, waiting, completed)).recoverInterruptedTasks()
        assertEquals("NEEDS_REVIEW", recovered.agentRuns[0].status)
        assertEquals("NEEDS_REVIEW", recovered.agentRuns[1].status)
        assertEquals("", recovered.agentRuns[1].pendingApprovalCallId)
        assertTrue(recovered.agentRuns[0].recoveryReason.contains("No step was replayed"))
        assertTrue(recovered.agentRuns[0].finishedAt > 0L)
        assertEquals("COMPLETED", recovered.agentRuns[2].status)
        assertEquals(3, recovered.agentRuns.size)
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
