package app.memo.android.orchestration

import app.memo.android.data.AppSnapshot
import app.memo.android.data.JobStepRecord
import app.memo.android.data.WorkJobRecord
import app.memo.android.runtime.RuntimeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowProposalApplierTest {
    private val runtimes = RuntimeRegistry().listRuntimes()
    private val goal = "Research and prepare a sourced report"

    private fun draftJob(
        status: String = "DRAFT",
        stepStatus: String = "PENDING",
        intent: String = goal
    ) = WorkJobRecord(
        id = "job-1",
        title = "Prepare report",
        goal = "$intent\n\nMemo route: Research. Source-backed research.",
        status = status,
        runtimeId = "app.jobs",
        sourceConversationId = "conversation-1",
        sourceMessageId = "message-1",
        steps = listOf(JobStepRecord(id = "old-step", title = "Old checkpoint", detail = "Old detail", status = stepStatus))
    )

    private fun node(
        id: String,
        kind: WorkflowNodeKind,
        participant: WorkflowParticipant,
        runtimeId: String,
        capabilities: List<String> = emptyList(),
        dependencies: List<String> = emptyList(),
        externalWrite: Boolean = false,
        requiresApproval: Boolean = false
    ) = TaskSpec(
        id = id,
        title = "Checkpoint $id",
        kind = kind,
        participant = participant,
        runtimeId = runtimeId,
        requiredCapabilities = capabilities,
        dependsOn = dependencies,
        inputContract = "Input for $id",
        outputContract = "Output for $id",
        postcondition = "Observable postcondition for $id",
        requiresApproval = requiresApproval,
        externalWrite = externalWrite,
        timeoutMs = 90_000,
        maxRetries = 0,
        idempotencyKey = id.toByteArray().joinToString("") { "%02x".format(it) }.padEnd(64, '0')
    )

    private fun readOnlyGraph(intent: String = goal) = WorkflowGraph(
        goal = intent,
        nodes = listOf(
            node("scope", WorkflowNodeKind.SCOPE, WorkflowParticipant.DETERMINISTIC, "app.jobs"),
            node("research", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.BROWSER, "runtime.browser", listOf("browser.inspect_page", "browser.capture_evidence"), listOf("scope")),
            node("review", WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.jobs", dependencies = listOf("research"))
        )
    )

    @Test
    fun appliesValidatedDraftGraphWithoutExecutingNodes() {
        val snapshot = AppSnapshot(jobs = listOf(draftJob()))
        val updated = WorkflowProposalApplier.apply(snapshot, "job-1", readOnlyGraph(), runtimes, now = 123L)
        val job = updated.jobs.single()

        assertEquals("DRAFT", job.status)
        assertEquals(listOf("scope", "research", "review"), job.steps.map { it.id })
        assertTrue(job.steps.all { it.status == "PENDING" && !it.requiresApproval })
        assertTrue(job.steps[1].detail.contains("Depends on: scope"))
        assertTrue(job.steps[1].detail.contains("Output: Output for research"))
        assertTrue(job.steps[1].detail.contains("Verify: Observable postcondition for research"))
        assertEquals(123L, job.updatedAt)
        assertEquals(goal, WorkflowGraph.fromJson(job.workflowGraphJson).goal)
        assertEquals("conversation-1", job.sourceConversationId)
        assertEquals("message-1", job.sourceMessageId)
        assertTrue(job.resultSummary.contains("No nodes were executed"))
        assertEquals("job-1", updated.activity.first().taskId)
        assertEquals(3, updated.jobs.single().steps.size)
    }

    @Test
    fun refusesToReplaceAPlanThatHasAlreadyStarted() {
        val snapshot = AppSnapshot(jobs = listOf(draftJob(status = "RUNNING", stepStatus = "RUNNING")))
        assertTrue(runCatching {
            WorkflowProposalApplier.apply(snapshot, "job-1", readOnlyGraph(), runtimes, now = 123L)
        }.isFailure)
    }

    @Test
    fun refusesAProposalThatChangesTheOriginalUserGoal() {
        val snapshot = AppSnapshot(jobs = listOf(draftJob()))
        assertTrue(runCatching {
            WorkflowProposalApplier.apply(snapshot, "job-1", readOnlyGraph("A different goal"), runtimes, now = 123L)
        }.exceptionOrNull()?.message.orEmpty().contains("changed the original user goal"))
    }

    @Test
    fun refusesCapabilitiesOnAnOptionalRuntimeThatIsNotAvailable() {
        val graph = WorkflowGraph(
            goal = goal,
            nodes = listOf(
                node("launch", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.ANDROID_APP, "runtime.android.accessibility", listOf("android.launch_app"))
            )
        )
        val snapshot = AppSnapshot(jobs = listOf(draftJob()))
        assertTrue(runCatching {
            WorkflowProposalApplier.apply(snapshot, "job-1", graph, runtimes, now = 123L)
        }.exceptionOrNull()?.message.orEmpty().contains("not currently available"))
    }

    @Test
    fun refusesApprovalRuntimeEvenWhenTheNodeIsNotMarkedAsAnExternalWrite() {
        val graph = WorkflowGraph(
            goal = goal,
            nodes = listOf(
                node("approval", WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.approvals")
            )
        )
        val snapshot = AppSnapshot(jobs = listOf(draftJob()))
        assertTrue(runCatching {
            WorkflowProposalApplier.apply(snapshot, "job-1", graph, runtimes, now = 123L)
        }.exceptionOrNull()?.message.orEmpty().contains("approval-runtime"))
    }

    @Test
    fun refusesApprovalGatedOrExternalWriteProposalsInTheDraftOnlyExpansionFlow() {
        val graph = WorkflowGraph(
            goal = goal,
            nodes = listOf(
                node("approval", WorkflowNodeKind.APPROVAL, WorkflowParticipant.USER, "app.approvals", externalWrite = true, requiresApproval = true)
            )
        )
        val snapshot = AppSnapshot(jobs = listOf(draftJob()))
        assertTrue(runCatching {
            WorkflowProposalApplier.apply(snapshot, "job-1", graph, runtimes, now = 123L)
        }.exceptionOrNull()?.message.orEmpty().contains("external side-effect"))
    }
}
