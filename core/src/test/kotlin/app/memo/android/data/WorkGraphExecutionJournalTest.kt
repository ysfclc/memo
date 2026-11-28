package app.memo.android.data

import app.memo.android.orchestration.TaskSpec
import app.memo.android.orchestration.WorkflowBudget
import app.memo.android.orchestration.WorkflowGraph
import app.memo.android.orchestration.WorkflowNodeKind
import app.memo.android.orchestration.WorkflowParticipant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkGraphExecutionJournalTest {
    private val hash = "a".repeat(64)

    private fun fixture(): Pair<WorkJobRecord, WorkGraphExecutionRecord> {
        val graph = WorkflowGraph(
            goal = "Complete an arbitrary user request",
            budget = WorkflowBudget(maxNodes = 2, maxDurationMs = 300_000, maxModelCalls = 0, maxExternalWrites = 0),
            nodes = listOf(
                node("collect", emptyList(), WorkflowNodeKind.SCOPE, WorkflowParticipant.DETERMINISTIC, "app.jobs"),
                node("review", listOf("collect"), WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.jobs")
            )
        )
        val graphJson = graph.toJson()
        val job = WorkJobRecord(
            id = "job-1",
            title = "Work",
            goal = graph.goal,
            sourceConversationId = "conversation-1",
            sourceMessageId = "message-1",
            workflowGraphJson = graphJson
        )
        val journal = WorkGraphExecutionRecord(
            id = "execution-1",
            jobId = job.id,
            sourceConversationId = job.sourceConversationId,
            sourceMessageId = job.sourceMessageId,
            graphDigest = WorkGraphExecutionRecord.graphDigest(graphJson),
            invocations = listOf(
                WorkNodeInvocationRecord(nodeId = "collect", invocationId = "invoke-collect"),
                WorkNodeInvocationRecord(nodeId = "review", invocationId = "invoke-review", dependencies = listOf("collect"))
            )
        )
        journal.validateFor(job)
        return job to journal
    }

    private fun node(
        id: String,
        dependencies: List<String>,
        kind: WorkflowNodeKind,
        participant: WorkflowParticipant,
        runtime: String
    ) = TaskSpec(
        id = id,
        title = id,
        kind = kind,
        participant = participant,
        runtimeId = runtime,
        requiredCapabilities = emptyList(),
        dependsOn = dependencies,
        inputContract = "Bounded input",
        outputContract = "Observable output",
        postcondition = "Output is verified",
        idempotencyKey = "a".repeat(64)
    )

    @Test fun dependencyMustSucceedBeforeDependentNodeCanBecomeReady() {
        val (_, initial) = fixture()
        assertTrue(runCatching { initial.markReady("review") }.isFailure)

        val firstReady = initial.markReady("collect", at = 10L)
        val prepared = firstReady.prepare("collect", hash, at = 11L)
        assertTrue(runCatching { prepared.start("collect", at = 12L).markReady("review") }.isFailure)

        val started = prepared.start("collect", at = 12L)
        val succeeded = started.finish(
            "collect", WorkNodeInvocationRecord.STATUS_SUCCEEDED,
            verifiedOutput = VerifiedWorkOutput.verifyObserved("artifact:collect", hash, hash), at = 13L
        )
        val secondReady = succeeded.markReady("review", at = 14L)
        assertEquals(WorkNodeInvocationRecord.STATUS_READY, secondReady.invocations.single { it.nodeId == "review" }.status)
    }

    @Test fun dispatchRequiresDurablePreparedStateAndSuccessRequiresOutputReference() {
        val (_, initial) = fixture()
        val ready = initial.markReady("collect")
        assertTrue(runCatching { ready.start("collect") }.isFailure)
        val prepared = ready.prepare("collect", hash)
        val running = prepared.start("collect")
        assertTrue(runCatching {
            running.finish("collect", WorkNodeInvocationRecord.STATUS_SUCCEEDED)
        }.isFailure)
        assertTrue(runCatching { VerifiedWorkOutput.verifyObserved("artifact:collect", hash, "b".repeat(64)) }.isFailure)
        assertEquals(WorkNodeInvocationRecord.STATUS_RUNNING, running.invocations.first().status)
    }

    @Test fun secondNodeCannotStartWhileAnotherInvocationIsRunning() {
        val (_, initial) = fixture()
        val running = initial.markReady("collect").prepare("collect", hash).start("collect")
        val withSecondReady = running.copy(invocations = running.invocations.map { node ->
            if (node.nodeId == "review") node.copy(status = WorkNodeInvocationRecord.STATUS_READY) else node
        })
        assertTrue(runCatching { withSecondReady.prepare("review", hash).start("review") }.isFailure)
    }

    @Test fun approvalIsBoundToExactPreparedInput() {
        val (_, initial) = fixture()
        val waiting = initial.markReady("collect").prepare("collect", hash).start("collect")
            .finish("collect", WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL)
        assertTrue(runCatching { waiting.prepareAfterApproval("collect", "b".repeat(64)) }.isFailure)
        val approved = waiting.prepareAfterApproval("collect", hash)
        assertEquals(hash, approved.invocations.first().approvalBindingHash)
        assertEquals(WorkNodeInvocationRecord.STATUS_PREPARED, approved.invocations.first().status)
    }

    @Test fun interruptedPreparedOrRunningInvocationBecomesReviewOnlyAndIsNeverReplayed() {
        val (_, initial) = fixture()
        val prepared = initial.markReady("collect").prepare("collect", hash, at = 20L)
        val recovered = prepared.recoverAfterRestart(at = 30L)
        assertEquals(WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW, recovered.invocations.first().status)
        assertEquals(WorkNodeInvocationRecord.STATUS_BLOCKED, recovered.invocations.last().status)
        assertEquals(WorkGraphExecutionRecord.STATUS_NEEDS_REVIEW, recovered.status)
        assertTrue(recovered.invocations.first().error.contains("never") || recovered.invocations.first().error.contains("Inspect"))
    }

    @Test fun journalRoundTripsInsideExistingJobBackupWithSchemaVersionEight() {
        val (job, journal) = fixture()
        val source = AppSnapshot(jobs = listOf(job.copy(executionJournalJson = journal.toJson())))
        val restored = AppSnapshot.fromJson(source.toJson())
        val restoredJob = restored.jobs.single()
        val restoredJournal = WorkGraphExecutionRecord.fromJson(restoredJob.executionJournalJson)
        restoredJournal.validateFor(restoredJob)
        assertEquals(journal, restoredJournal)
        assertEquals(8, org.json.JSONObject(source.toJson()).optInt("schemaVersion"))
    }

    @Test fun journalRejectsChangedGraphOrSourceIdentity() {
        val (job, journal) = fixture()
        val changedGraph = job.copy(workflowGraphJson = job.workflowGraphJson.replace("Complete an arbitrary", "Different arbitrary"))
        assertTrue(runCatching { journal.validateFor(changedGraph) }.isFailure)
        val changedSource = job.copy(sourceMessageId = "different-message")
        assertTrue(runCatching { journal.validateFor(changedSource) }.isFailure)
    }

    @Test fun restartRecoveryIsIntegratedIntoWorkspaceSnapshot() {
        val (job, journal) = fixture()
        val running = journal.markReady("collect").prepare("collect", hash).start("collect")
        val snapshot = AppSnapshot(jobs = listOf(job.copy(status = "RUNNING", executionJournalJson = running.toJson())))
        val recovered = snapshot.recoverInterruptedTasks()
        val recoveredJob = recovered.jobs.single()
        assertEquals("WAITING_FOR_USER", recoveredJob.status)
        assertEquals(WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW,
            WorkGraphExecutionRecord.fromJson(recoveredJob.executionJournalJson).invocations.first().status)
        assertTrue(recovered.activity.first().message.contains("1 interrupted job"))
    }
    @Test fun schemaSevenWorkspaceImportsWithoutInventingAnExecutionJournal() {
        val (job, _) = fixture()
        val root = org.json.JSONObject(AppSnapshot(jobs = listOf(job)).toJson())
        root.put("schemaVersion", 7)
        root.put("schemaSemver", "7.0.0")
        root.put("minimumReaderVersion", 7)
        root.getJSONArray("jobs").getJSONObject(0).remove("executionJournalJson")

        val restored = AppSnapshot.fromJson(root.toString())
        assertEquals("", restored.jobs.single().executionJournalJson)
        assertEquals(8, org.json.JSONObject(restored.toJson()).optInt("schemaVersion"))
    }

}
