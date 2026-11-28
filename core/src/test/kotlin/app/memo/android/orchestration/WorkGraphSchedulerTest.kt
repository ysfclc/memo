package app.memo.android.orchestration

import app.memo.android.data.AppSnapshot
import app.memo.android.data.JobStepRecord
import app.memo.android.data.VerifiedWorkOutput
import app.memo.android.data.WorkGraphExecutionRecord
import app.memo.android.data.WorkNodeInvocationRecord
import app.memo.android.data.WorkJobRecord
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkGraphSchedulerTest {
    private val hash = "b".repeat(64)

    private fun fixture(): Pair<WorkJobRecord, WorkGraphExecutionRecord> {
        val graph = WorkflowGraph(
            goal = "An arbitrary multi-step request",
            budget = WorkflowBudget(maxNodes = 3, maxDurationMs = 360_000, maxModelCalls = 0, maxExternalWrites = 0),
            // Deliberately put the join before its inputs. The scheduler must respect dependencies.
            nodes = listOf(
                node("synthesize", listOf("gather_a", "gather_b")),
                node("gather_b", emptyList()),
                node("gather_a", emptyList())
            )
        )
        val job = WorkJobRecord(
            id = "job-scheduler",
            title = "Arbitrary work",
            goal = graph.goal,
            sourceConversationId = "conversation-scheduler",
            sourceMessageId = "message-scheduler",
            workflowGraphJson = graph.toJson(),
            // The UI list is intentionally in a different order and is not the scheduler's authority.
            steps = listOf(JobStepRecord(title = "Synthesize"), JobStepRecord(title = "Gather A"), JobStepRecord(title = "Gather B"))
        )
        val journal = WorkGraphExecutionRecord.createFor(
            job = job,
            executionId = "execution-scheduler",
            invocationIdFactory = { nodeId -> "invoke-$nodeId" },
            at = 100L
        )
        return job to journal
    }

    private fun node(id: String, dependencies: List<String>) = TaskSpec(
        id = id,
        title = "Arbitrary node $id",
        kind = WorkflowNodeKind.SCOPE,
        participant = WorkflowParticipant.DETERMINISTIC,
        runtimeId = "app.jobs",
        requiredCapabilities = emptyList(),
        dependsOn = dependencies,
        inputContract = "Bounded input",
        outputContract = "Observable output",
        postcondition = "Output is verified",
        idempotencyKey = "a".repeat(64)
    )

    private fun completeNode(journal: WorkGraphExecutionRecord, nodeId: String): WorkGraphExecutionRecord =
        journal.markReady(nodeId)
            .prepare(nodeId, hash)
            .start(nodeId)
            .finish(
                nodeId,
                WorkNodeInvocationRecord.STATUS_SUCCEEDED,
                verifiedOutput = VerifiedWorkOutput.verifyObserved("evidence:$nodeId", hash, hash)
            )

    @Test fun createsInvocationRecordsFromExactGraphWithStableNodeAndDependencyBindings() {
        val (job, journal) = fixture()
        journal.validateFor(job)
        assertEquals(listOf("gather_b", "gather_a", "synthesize"), journal.invocations.map { it.nodeId })
        assertEquals(listOf("gather_a", "gather_b"), journal.invocations.single { it.nodeId == "synthesize" }.dependencies)
        assertEquals("invoke-gather_a", journal.invocations.single { it.nodeId == "gather_a" }.invocationId)
    }

    @Test fun selectsEligibleNodesByTopologicalDependenciesNotHumanCheckpointOrder() {
        val (job, journal) = fixture()
        val decision = WorkGraphScheduler.next(job, journal)
        assertTrue(decision is WorkGraphScheduler.Decision.Eligible)
        assertEquals("gather_b", (decision as WorkGraphScheduler.Decision.Eligible).node.id)

        val readyJournal = journal.markReady("gather_b")
        val ready = WorkGraphScheduler.next(job, readyJournal)
        assertTrue(ready is WorkGraphScheduler.Decision.Ready)
        assertEquals("gather_b", (ready as WorkGraphScheduler.Decision.Ready).node.id)
    }

    @Test fun fanOutNodesCanCompleteIndependentlyButJoinWaitsForEveryDependency() {
        val (job, initial) = fixture()
        val afterB = completeNode(initial, "gather_b")
        val next = WorkGraphScheduler.next(job, afterB)
        assertTrue(next is WorkGraphScheduler.Decision.Eligible)
        assertEquals("gather_a", (next as WorkGraphScheduler.Decision.Eligible).node.id)

        val afterA = completeNode(afterB, "gather_a")
        val join = WorkGraphScheduler.next(job, afterA)
        assertTrue(join is WorkGraphScheduler.Decision.Eligible)
        assertEquals("synthesize", (join as WorkGraphScheduler.Decision.Eligible).node.id)
    }

    @Test fun preparedOrRunningNodesRequireExplicitRecoveryAndAreNeverSelectedAgain() {
        val (job, initial) = fixture()
        val prepared = initial.markReady("gather_b").prepare("gather_b", hash)
        val decision = WorkGraphScheduler.next(job, prepared)
        assertTrue(decision is WorkGraphScheduler.Decision.AwaitingRecovery)
        assertEquals("gather_b", (decision as WorkGraphScheduler.Decision.AwaitingRecovery).nodeId)
        assertEquals(WorkNodeInvocationRecord.STATUS_PREPARED, decision.state)
    }

    @Test fun approvalAndReviewStatesStopAutomaticScheduling() {
        val (job, initial) = fixture()
        val awaitingApproval = initial.markReady("gather_b").prepare("gather_b", hash).start("gather_b")
            .finish("gather_b", WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL)
        val approvalDecision = WorkGraphScheduler.next(job, awaitingApproval)
        assertTrue(approvalDecision is WorkGraphScheduler.Decision.AwaitingApproval)
        assertEquals("gather_b", (approvalDecision as WorkGraphScheduler.Decision.AwaitingApproval).nodeId)

        val needsReview = initial.markReady("gather_b").prepare("gather_b", hash).start("gather_b")
            .finish("gather_b", WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW)
        assertTrue(WorkGraphScheduler.next(job, needsReview) is WorkGraphScheduler.Decision.Stopped)
    }

    @Test fun completedGraphHasNoFurtherRunnableNodes() {
        val (job, initial) = fixture()
        val allDone = completeNode(completeNode(completeNode(initial, "gather_b"), "gather_a"), "synthesize")
        assertTrue(WorkGraphScheduler.next(job, allDone) is WorkGraphScheduler.Decision.Completed)
    }
    @Test fun inconsistentTerminalRunStateIsRejectedBeforeScheduling() {
        val (job, journal) = fixture()
        val forgedComplete = journal.copy(status = WorkGraphExecutionRecord.STATUS_COMPLETED)
        assertTrue(runCatching { WorkGraphScheduler.next(job, forgedComplete) }.isFailure)
    }

    @Test fun legacyGraphStepsMigrateToStableTopologicalNodeIdsOnImport() {
        val (job, _) = fixture()
        val legacyJob = job.copy(steps = listOf(
            JobStepRecord(title = "Synthesize"),
            JobStepRecord(title = "Gather B"),
            JobStepRecord(title = "Gather A")
        ))
        val restored = AppSnapshot.fromJson(AppSnapshot(jobs = listOf(legacyJob)).toJson())
        assertEquals(
            listOf("gather_b", "gather_a", "synthesize"),
            restored.jobs.single().steps.map { it.workflowNodeId }
        )
    }

}
