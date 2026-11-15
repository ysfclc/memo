package app.memo.android.runtime

import app.memo.android.data.WorkflowAttemptRecord
import app.memo.android.orchestration.CapabilityCompiler
import app.memo.android.orchestration.TaskSpec
import app.memo.android.orchestration.WorkflowGraphCompiler
import app.memo.android.orchestration.WorkflowNodeKind
import app.memo.android.orchestration.WorkflowParticipant
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkflowRuntimeAdapterTest {
    private val runtimes = RuntimeRegistry().listRuntimes()

    private fun task(
        capability: String = "model.generate",
        requiresApproval: Boolean = false,
        dependsOn: List<String> = emptyList(),
        kind: WorkflowNodeKind = WorkflowNodeKind.ARTIFACT,
        key: String = "a".repeat(64)
    ) = TaskSpec(
        id = "draft",
        title = "Prepare a draft",
        kind = kind,
        participant = WorkflowParticipant.LOCAL_MODEL,
        runtimeId = "app.models",
        requiredCapabilities = listOf(capability),
        dependsOn = dependsOn,
        inputContract = "Approved source context",
        outputContract = "A reviewable draft",
        postcondition = "A draft artifact is saved for review.",
        requiresApproval = requiresApproval,
        timeoutMs = 500,
        maxRetries = 0,
        idempotencyKey = key
    )

    private fun request(task: TaskSpec = task()): WorkflowRuntimeRequest {
        val manifest = CapabilityCompiler.compileForTasks("Prepare a draft", listOf(task), runtimes)
        val graph = WorkflowGraphCompiler.compile("Prepare a draft", listOf(task), manifest)
        val attempt = WorkflowAttemptRecord(
            id = "attempt-1",
            jobId = "job-1",
            stepId = task.id,
            attemptNumber = 1,
            idempotencyKey = task.idempotencyKey,
            runtimeId = task.runtimeId,
            capabilityIds = task.requiredCapabilities,
            status = "RUNNING",
            startedAt = 100L
        )
        return WorkflowRuntimeRequest(
            graph = graph,
            taskId = task.id,
            capabilityManifest = manifest,
            attempt = attempt,
            inputJson = """{"sourceEvidenceIds":["evidence-1"]}""",
            deadlineAt = 600L
        )
    }

    private class FakeAdapter(
        override val runtimeId: String,
        override val capabilityIds: Set<String>
    ) : WorkflowRuntimeAdapter {
        override suspend fun execute(request: WorkflowRuntimeRequest): WorkflowRuntimeResult =
            WorkflowRuntimeResult.Output(
                WorkflowRuntimeOutput("Draft output", "text/plain", "Saved draft"),
                completedAt = 200L
            )
    }

    @Test fun requestBindsThePersistedAttemptToExactRuntimeAndCompiledCapabilities() {
        val request = request()
        request.validate(runtimes)
        assertTrue(request.task.runtimeId == "app.models")
        assertTrue(request.attempt.capabilityIds == listOf("model.generate"))
        assertTrue(request.taskSpecSha256.length == 64)
    }

    @Test fun resolverSelectsOnlyTheAdapterForTheExactRuntime() {
        val request = request()
        val adapter = FakeAdapter("app.models", setOf("model.generate", "model.list"))
        val resolved = WorkflowRuntimeAdapterResolver.resolve(request, runtimes, listOf(
            FakeAdapter("runtime.browser", setOf("browser.navigate_https")),
            adapter
        ))
        assertTrue(resolved === adapter)
    }

    @Test fun resolverRejectsDuplicateRuntimeAdaptersAndUnregisteredAdapterCapabilities() {
        val request = request()
        assertTrue(runCatching {
            WorkflowRuntimeAdapterResolver.resolve(
                request, runtimes,
                listOf(
                    FakeAdapter("app.models", setOf("model.generate")),
                    FakeAdapter("app.models", setOf("model.generate"))
                )
            )
        }.isFailure)
        assertTrue(runCatching {
            WorkflowRuntimeAdapterResolver.resolve(
                request, runtimes,
                listOf(FakeAdapter("app.models", setOf("model.generate", "model.not_registered")))
            )
        }.isFailure)
    }

    @Test fun requestRejectsCapabilitiesMissingFromTheCompiledManifest() {
        val original = request()
        val tampered = original.copy(
            capabilityManifest = original.capabilityManifest.copy(capabilities = emptyList())
        )
        assertTrue(runCatching { tampered.validate(runtimes) }.isFailure)
        assertTrue(runCatching {
            original.copy(
                capabilityManifest = original.capabilityManifest.copy(requiresUserApproval = true)
            ).validate(runtimes)
        }.isFailure)
    }

    @Test fun requestRejectsRuntimeAvailabilityOrCapabilityPolicyDrift() {
        val original = request()
        val unavailable = runtimes.map {
            if (it.id == "app.models") it.copy(status = "DISABLED") else it
        }
        assertTrue(runCatching { original.validate(unavailable) }.isFailure)

        val approvalRequired = runtimes.map { runtime ->
            if (runtime.id == "app.models") runtime.copy(
                capabilities = runtime.capabilities.map { capability ->
                    if (capability.id == "model.generate") capability.copy(requiresUserApproval = true)
                    else capability
                }
            ) else runtime
        }
        assertTrue(runCatching { original.validate(approvalRequired) }.isFailure)
    }

    @Test fun requestRejectsAnAttemptThatDoesNotMatchItsTaskSpec() {
        val original = request()
        assertTrue(runCatching {
            original.copy(attempt = original.attempt.copy(capabilityIds = listOf("model.list")))
                .validate(runtimes)
        }.isFailure)
        assertTrue(runCatching {
            original.copy(attempt = original.attempt.copy(idempotencyKey = "b".repeat(64)))
                .validate(runtimes)
        }.isFailure)
    }

    @Test fun approvalGatedTasksCannotBypassTheDedicatedApprovalFlow() {
        val approval = TaskSpec(
            id = "approval",
            title = "Review remote model use",
            kind = WorkflowNodeKind.APPROVAL,
            participant = WorkflowParticipant.USER,
            runtimeId = "app.approvals",
            requiredCapabilities = emptyList(),
            dependsOn = emptyList(),
            inputContract = "Exact provider and context scope",
            outputContract = "One-use approval decision",
            postcondition = "The user approved this exact remote request.",
            requiresApproval = true,
            timeoutMs = 30_000,
            maxRetries = 0,
            idempotencyKey = "b".repeat(64)
        )
        val remote = task(
            capability = "model.generate_remote",
            requiresApproval = true,
            dependsOn = listOf("approval"),
            key = "c".repeat(64)
        )
        val manifest = CapabilityCompiler.compileForTasks("Use remote model", listOf(approval, remote), runtimes)
        val graph = WorkflowGraphCompiler.compile("Use remote model", listOf(approval, remote), manifest)
        val attempt = WorkflowAttemptRecord(
            id = "attempt-remote",
            jobId = "job-remote",
            stepId = remote.id,
            attemptNumber = 1,
            idempotencyKey = remote.idempotencyKey,
            runtimeId = remote.runtimeId,
            capabilityIds = remote.requiredCapabilities,
            status = "RUNNING",
            startedAt = 100L
        )
        val request = WorkflowRuntimeRequest(
            graph, remote.id, manifest, attempt, """{"prompt":"reviewed"}""", 600L
        )
        assertTrue(runCatching { request.validate(runtimes) }.isFailure)
    }

    @Test fun runtimeDeadlineAndCancellationAreEnforcedByTheSharedRequest() {
        val original = request()
        assertTrue(runCatching {
            original.copy(deadlineAt = 700L).validate(runtimes)
        }.isFailure)
        assertTrue(runCatching {
            original.copy(cancellation = WorkflowCancellationSignal { true }).checkActive(150L)
        }.isFailure)
        assertTrue(runCatching { original.checkActive(600L) }.isFailure)
        original.checkActive(599L)
    }

    @Test fun runtimeOutputIsBoundedAndDoesNotClaimPostconditionVerification() {
        val request = request()
        val result = WorkflowRuntimeResult.Output(
            WorkflowRuntimeOutput("Draft output", "text/plain", "Saved for review"),
            completedAt = 200L
        )
        result.validateFor(request)
        assertTrue(runCatching {
            WorkflowRuntimeResult.Output(
                WorkflowRuntimeOutput(
                    "x".repeat(WorkflowRuntimeRequest.MAX_OUTPUT_CHARS + 1),
                    "text/plain",
                    "Too large"
                ),
                completedAt = 200L
            ).validateFor(request)
        }.isFailure)
        assertTrue(runCatching {
            WorkflowRuntimeResult.Failed("bad code", "Failed", 200L).validateFor(request)
        }.isFailure)
    }
}
