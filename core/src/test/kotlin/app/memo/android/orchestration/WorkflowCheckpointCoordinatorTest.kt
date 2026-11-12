package app.memo.android.orchestration

import app.memo.android.data.AppSnapshot
import app.memo.android.data.JobStepRecord
import app.memo.android.data.WorkJobRecord
import app.memo.android.data.WorkflowAttemptJournal
import app.memo.android.runtime.RuntimeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowCheckpointCoordinatorTest {
    private val runtimes = RuntimeRegistry().listRuntimes()

    private fun node(
        id: String,
        runtimeId: String,
        capabilities: List<String> = emptyList(),
        dependencies: List<String> = emptyList(),
        participant: WorkflowParticipant = WorkflowParticipant.DETERMINISTIC,
        kind: WorkflowNodeKind = WorkflowNodeKind.DOMAIN_WORK
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
        postcondition = "Observable outcome for $id",
        timeoutMs = 60_000,
        maxRetries = 0,
        idempotencyKey = "a".repeat(64)
    )

    private fun fixture(): AppSnapshot {
        val nodes = listOf(
            node("scope", "app.jobs", listOf("job.create")),
            node("context", "app.entities", listOf("entity.propose"), listOf("scope")),
            node("research", "runtime.browser", listOf("browser.inspect_page"), listOf("scope"), WorkflowParticipant.BROWSER),
            node("review", "app.jobs", listOf("job.verify"), listOf("context", "research"), WorkflowParticipant.USER, WorkflowNodeKind.REVIEW)
        )
        val manifest = CapabilityCompiler.compileForTasks("Prepare a sourced report", nodes, runtimes)
        val graph = WorkflowGraphCompiler.compile("Prepare a sourced report", nodes, manifest)
        val job = WorkJobRecord(
            id = "job-1",
            title = "Sourced report",
            goal = graph.goal,
            status = "DRAFT",
            workflowGraphJson = graph.toJson(),
            steps = WorkflowGraphValidator.topologicalOrder(graph).map {
                JobStepRecord(id = it.id, title = it.title, detail = it.inputContract)
            }
        )
        return AppSnapshot(jobs = listOf(job))
    }

    @Test fun refusesToStartAChildBeforeItsDependenciesAreComplete() {
        val result = WorkflowCheckpointCoordinator.start(fixture(), "job-1", "research", runtimes, 10L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("waiting for dependencies"))
    }

    @Test fun startsAndRecordsAUserReportedCheckpointWithoutClaimingRuntimeVerification() {
        val initial = fixture()
        val started = WorkflowCheckpointCoordinator.start(initial, "job-1", "scope", runtimes, 10L).getOrThrow()
        assertEquals("RUNNING", started.jobs.single().status)
        assertEquals("RUNNING", started.jobs.single().steps.first { it.id == "scope" }.status)
        assertEquals(1, started.workflowAttempts.size)
        assertEquals("RUNNING", started.workflowAttempts.single().status)
        assertEquals("app.jobs", started.workflowAttempts.single().runtimeId)
        assertEquals("a".repeat(64), started.workflowAttempts.single().idempotencyKey)

        val completed = WorkflowCheckpointCoordinator.recordUserOutcome(
            started, "job-1", "scope", "Reviewed the project scope and constraints.", 20L
        ).getOrThrow()
        val scope = completed.jobs.single().steps.first { it.id == "scope" }
        assertEquals("COMPLETED", scope.status)
        assertTrue(scope.evidence.startsWith("USER_REPORTED:"))
        assertTrue(scope.detail.contains("not independently verified"))
        assertEquals("READY", completed.jobs.single().status)
        assertEquals("USER_REPORTED", completed.workflowAttempts.single().status)
        assertEquals("Reviewed the project scope and constraints.", completed.workflowAttempts.single().userOutcome)
    }

    @Test fun allowsDependencyReadyParallelBranchesInDeterministicTopologicalOrder() {
        val started = WorkflowCheckpointCoordinator.start(fixture(), "job-1", "scope", runtimes, 10L).getOrThrow()
        val completed = WorkflowCheckpointCoordinator.recordUserOutcome(
            started, "job-1", "scope", "Scope reviewed and confirmed.", 20L
        ).getOrThrow()
        val research = WorkflowCheckpointCoordinator.start(completed, "job-1", "research", runtimes, 30L).getOrThrow()
        assertEquals("RUNNING", research.jobs.single().steps.first { it.id == "research" }.status)
        assertEquals("PENDING", research.jobs.single().steps.first { it.id == "context" }.status)
    }

    @Test fun refusesToStartWhenAnyCheckpointIsAlreadyRunning() {
        val initial = fixture()
        val started = WorkflowCheckpointCoordinator.start(initial, "job-1", "scope", runtimes, 10L).getOrThrow()
        val result = WorkflowCheckpointCoordinator.start(started, "job-1", "context", runtimes, 20L)
        assertTrue(result.isFailure)
    }

    @Test fun refusesAWorkflowWithExternalEffectsEvenWhenItsGraphIsOtherwiseValid() {
        val nodes = listOf(
            node("approval", "app.approvals", emptyList(), emptyList(), WorkflowParticipant.USER, WorkflowNodeKind.APPROVAL)
                .copy(requiresApproval = true, externalWrite = true),
            node("send", "runtime.browser", listOf("browser.submit_prompt"), listOf("approval"), WorkflowParticipant.BROWSER)
                .copy(requiresApproval = true, externalSideEffect = true)
        )
        val manifest = CapabilityCompiler.compileForTasks("Send an approved prompt", nodes, runtimes)
        val graph = WorkflowGraphCompiler.compile("Send an approved prompt", nodes, manifest)
        val job = WorkJobRecord(
            id = "job-2", title = "Send prompt", goal = graph.goal,
            workflowGraphJson = graph.toJson(),
            steps = nodes.map { JobStepRecord(id = it.id, title = it.title) }
        )
        val result = WorkflowCheckpointCoordinator.start(AppSnapshot(jobs = listOf(job)), "job-2", "approval", runtimes, 10L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("approval-gated or external action"))
    }

    @Test fun rechecksRuntimeAvailabilityBeforeStartingAnyCheckpoint() {
        val unavailable = runtimes.map { runtime ->
            if (runtime.id == "runtime.browser") runtime.copy(status = "DISCONNECTED") else runtime
        }
        val result = WorkflowCheckpointCoordinator.start(fixture(), "job-1", "scope", unavailable, 10L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("not currently available"))
    }

    @Test fun doesNotAllowAUserNoteToCompleteAnExternalActionNode() {
        val nodes = listOf(
            node("approval", "app.approvals", emptyList(), emptyList(), WorkflowParticipant.USER, WorkflowNodeKind.APPROVAL)
                .copy(requiresApproval = true, externalWrite = true),
            node("send", "runtime.browser", listOf("browser.submit_prompt"), listOf("approval"), WorkflowParticipant.BROWSER)
                .copy(requiresApproval = true, externalSideEffect = true)
        )
        val manifest = CapabilityCompiler.compileForTasks("Send an approved prompt", nodes, runtimes)
        val graph = WorkflowGraphCompiler.compile("Send an approved prompt", nodes, manifest)
        val job = WorkJobRecord(
            id = "job-3", title = "Send prompt", goal = graph.goal, workflowGraphJson = graph.toJson(),
            steps = nodes.map { JobStepRecord(id = it.id, title = it.title, status = if (it.id == "send") "NEEDS_REVIEW" else "PENDING") }
        )
        val result = WorkflowCheckpointCoordinator.recordUserOutcome(
            AppSnapshot(jobs = listOf(job)), "job-3", "send", "The prompt was sent successfully.", 20L
        )
        assertTrue(result.isFailure)
        assertFalse(result.isSuccess)
    }
    @Test fun legacySequentialJobsRemainUsableWithoutAWorkflowGraph() {
        val job = WorkJobRecord(
            id = "legacy", title = "Legacy job", goal = "Older manual work plan",
            steps = listOf(
                JobStepRecord(id = "first", title = "First checkpoint"),
                JobStepRecord(id = "second", title = "Second checkpoint")
            )
        )
        val initial = AppSnapshot(jobs = listOf(job))
        val started = WorkflowCheckpointCoordinator.start(initial, "legacy", "first", runtimes, 10L).getOrThrow()
        val completed = WorkflowCheckpointCoordinator.recordUserOutcome(
            started, "legacy", "first", "Finished the first checkpoint.", 20L
        ).getOrThrow()
        val next = WorkflowCheckpointCoordinator.start(completed, "legacy", "second", runtimes, 30L).getOrThrow()
        assertEquals("RUNNING", next.jobs.single().steps.last().status)
    }

    @Test fun manualRetriesKeepOneIdempotencyIdentityAndStopAtFourAttempts() {
        var snapshot = fixture()
        repeat(4) { index ->
            snapshot = WorkflowCheckpointCoordinator.start(snapshot, "job-1", "scope", runtimes, 10L + index * 2).getOrThrow()
            snapshot = app.memo.android.data.LocalWorkArtifactCoordinator.markNeedsReview(
                snapshot, "job-1", "scope", "Local inference failed.", "Review before retrying.", 11L + index * 2
            ).getOrThrow()
        }
        assertEquals(listOf(1, 2, 3, 4), snapshot.workflowAttempts.map { it.attemptNumber })
        assertEquals(1, snapshot.workflowAttempts.map { it.idempotencyKey }.distinct().size)
        val refused = WorkflowCheckpointCoordinator.start(snapshot, "job-1", "scope", runtimes, 20L)
        assertTrue(refused.isFailure)
        assertTrue(refused.exceptionOrNull()?.message.orEmpty().contains("four-attempt safety limit"))
    }

    @Test fun legacyManualJobsCannotStartAnExternalActionShortcut() {
        val job = WorkJobRecord(
            id = "legacy-external", title = "Legacy action", goal = "Older action plan",
            steps = listOf(JobStepRecord(
                id = "send", title = "Send message", checkpointKind = "EXTERNAL_ACTION"
            ))
        )
        val result = WorkflowCheckpointCoordinator.start(AppSnapshot(jobs = listOf(job)), "legacy-external", "send", runtimes, 10L)
        assertTrue(result.isFailure)
        assertTrue(result.exceptionOrNull()?.message.orEmpty().contains("dedicated exact-content action approval"))
    }

}
