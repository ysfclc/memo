package app.memo.android.runtime

import app.memo.android.data.WorkflowAttemptRecord
import app.memo.android.orchestration.CapabilityManifest
import app.memo.android.orchestration.TaskSpec
import app.memo.android.orchestration.WorkflowGraph
import app.memo.android.orchestration.WorkflowGraphValidator
import app.memo.android.orchestration.WorkflowNodeKind
import app.memo.android.orchestration.WorkflowParticipant
import java.security.MessageDigest

/**
 * Immutable invocation boundary shared by deterministic, local-model, browser and Android
 * runtimes. A request is created only from a persisted TaskSpec and its exact active attempt.
 *
 * Adapters return output; they do not persist workspace state, mark a checkpoint complete, grant
 * approvals, or decide that a postcondition has passed.
 */
data class WorkflowRuntimeRequest(
    val graph: WorkflowGraph,
    val taskId: String,
    val capabilityManifest: CapabilityManifest,
    val attempt: WorkflowAttemptRecord,
    val inputJson: String,
    val deadlineAt: Long,
    val cancellation: WorkflowCancellationSignal = WorkflowCancellationSignal.NONE
) {
    val task: TaskSpec
        get() = graph.nodes.single { it.id == taskId }

    val taskSpecSha256: String
        get() = workflowTaskSpecSha256(task)

    fun validate(registeredRuntimes: List<RuntimeDescriptor>) {
        WorkflowGraphValidator.validate(graph)
        require(capabilityManifest.schemaVersion == 1) { "Unsupported capability manifest schema." }
        require(capabilityManifest.manifestSha256.matches(SHA256_PATTERN) &&
            capabilityManifest.sourceIntentSha256.matches(SHA256_PATTERN) &&
            capabilityManifest.manifestSha256 == capabilityManifestFingerprint(capabilityManifest)) {
            "The compiled capability manifest identity is invalid or its contents changed."
        }
        require(capabilityManifest.registeredRuntimeIds == registeredRuntimes.map { it.id }.toSet()) {
            "The registered runtime set changed after the capability manifest was compiled."
        }
        require(capabilityManifest.capabilities.map { it.runtimeId to it.capabilityId }.distinct().size ==
            capabilityManifest.capabilities.size) {
            "The compiled capability manifest contains duplicate runtime/capability pairs."
        }
        require(capabilityManifest.unresolvedRequirements.isEmpty()) {
            "The compiled capability manifest contains unresolved requirements."
        }
        require(registeredRuntimes.map { it.id }.distinct().size == registeredRuntimes.size) {
            "The runtime registry contains duplicate runtime IDs."
        }
        registeredRuntimes.forEach { runtime ->
            require(runtime.capabilities.map { it.id }.distinct().size == runtime.capabilities.size) {
                "Runtime '${runtime.id}' contains duplicate capability IDs."
            }
        }
        val runtimeIds = registeredRuntimes.map { it.id }.toSet()
        require(graph.nodes.all { it.runtimeId in capabilityManifest.registeredRuntimeIds && it.runtimeId in runtimeIds }) {
            "The workflow graph no longer matches the registered runtime manifest."
        }
        val selectedTask = graph.nodes.singleOrNull { it.id == taskId }
            ?: throw IllegalArgumentException("The requested task is not in the saved workflow graph.")
        val runtime = registeredRuntimes.singleOrNull { it.id == selectedTask.runtimeId }
            ?: throw IllegalArgumentException("Workflow runtime '${selectedTask.runtimeId}' is not registered.")
        require(runtime.status == "AVAILABLE") {
            "Workflow runtime '${runtime.id}' is not currently available (${runtime.status})."
        }
        require(selectedTask.participant != WorkflowParticipant.USER &&
            selectedTask.kind != WorkflowNodeKind.APPROVAL &&
            selectedTask.runtimeId != "app.approvals") {
            "Human checkpoints and approval tasks cannot be dispatched to a runtime adapter."
        }
        require(!selectedTask.requiresApproval && !selectedTask.externalWrite && !selectedTask.externalSideEffect) {
            "Approval-gated or external side-effect tasks must use the dedicated approval flow."
        }
        require(selectedTask.externalSideEffect == selectedTask.requiredCapabilities.any { capabilityId ->
            runtime.capabilities.singleOrNull { it.id == capabilityId }?.externalSideEffect == true
        }) {
            "The task's side-effect classification does not match the current runtime registry."
        }
        when (selectedTask.participant) {
            WorkflowParticipant.LOCAL_MODEL -> require(selectedTask.runtimeId == "app.models") {
                "Local-model tasks must target app.models."
            }
            WorkflowParticipant.FRONTIER_WEB, WorkflowParticipant.BROWSER -> require(selectedTask.runtimeId == "runtime.browser") {
                "Browser tasks must target runtime.browser."
            }
            WorkflowParticipant.ANDROID_APP -> require(selectedTask.runtimeId == "runtime.android.accessibility") {
                "Android-app tasks must target the registered Accessibility runtime."
            }
            WorkflowParticipant.DETERMINISTIC -> require(selectedTask.runtimeId.startsWith("app.")) {
                "Deterministic tasks must target a Memo app runtime."
            }
            WorkflowParticipant.USER -> error("User checkpoints cannot be runtime-dispatched.")
        }

        val manifestRows = capabilityManifest.capabilities.filter {
            it.runtimeId == selectedTask.runtimeId && it.capabilityId in selectedTask.requiredCapabilities
        }
        require(manifestRows.size == selectedTask.requiredCapabilities.size &&
            manifestRows.map { it.capabilityId }.toSet() == selectedTask.requiredCapabilities.toSet()) {
            "The exact TaskSpec capabilities are not present in the compiled capability manifest."
        }
        selectedTask.requiredCapabilities.forEach { capabilityId ->
            val current = runtime.capabilities.singleOrNull { it.id == capabilityId }
                ?: throw IllegalArgumentException(
                    "Capability '${runtime.id}:$capabilityId' is no longer registered."
                )
            val compiled = manifestRows.single { it.capabilityId == capabilityId }
            require(compiled.runtimeStatus == runtime.status &&
                compiled.requiresUserApproval == current.requiresUserApproval &&
                compiled.externalSideEffect == current.externalSideEffect) {
                "Capability '${runtime.id}:$capabilityId' changed after the workflow was compiled."
            }
            require(!current.requiresUserApproval && !current.externalSideEffect) {
                "Approval-gated capabilities cannot be invoked through this runtime contract."
            }
        }

        require(attempt.status == "RUNNING" && attempt.id.isNotBlank()) {
            "A runtime request requires one persisted active attempt."
        }
        require(attempt.jobId.isNotBlank() && attempt.stepId == selectedTask.id &&
            attempt.runtimeId == selectedTask.runtimeId &&
            attempt.capabilityIds == selectedTask.requiredCapabilities &&
            attempt.idempotencyKey == selectedTask.idempotencyKey) {
            "The active attempt does not match the exact saved TaskSpec runtime and capabilities."
        }
        require(inputJson.isNotBlank() && inputJson.length <= MAX_INPUT_CHARS) {
            "Runtime input is empty or exceeds its safe size limit."
        }
        require(attempt.startedAt > 0L && deadlineAt > attempt.startedAt &&
            deadlineAt - attempt.startedAt <= selectedTask.timeoutMs) {
            "Runtime deadline is invalid or exceeds the TaskSpec timeout."
        }
    }

    /** Call immediately before and during long-running adapter work. */
    fun checkActive(now: Long) {
        if (cancellation.isCancellationRequested()) {
            throw WorkflowRuntimeCancelledException("Workflow runtime execution was cancelled.")
        }
        if (now >= deadlineAt) {
            throw WorkflowRuntimeDeadlineExceededException("Workflow runtime execution exceeded its deadline.")
        }
    }

    companion object {
        const val MAX_INPUT_CHARS = 64_000
        const val MAX_OUTPUT_CHARS = 256_000
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

fun interface WorkflowCancellationSignal {
    fun isCancellationRequested(): Boolean

    companion object {
        val NONE = WorkflowCancellationSignal { false }
    }
}

class WorkflowRuntimeCancelledException(message: String) : IllegalStateException(message)
class WorkflowRuntimeDeadlineExceededException(message: String) : IllegalStateException(message)

/** Runtime output is a candidate artifact, never proof that the task postcondition passed. */
data class WorkflowRuntimeOutput(
    val content: String,
    val contentType: String,
    val summary: String,
    val sourceEvidenceIds: List<String> = emptyList()
) {
    fun validate() {
        require(content.isNotBlank() && content.length <= WorkflowRuntimeRequest.MAX_OUTPUT_CHARS) {
            "Runtime output is empty or exceeds its safe size limit."
        }
        require(contentType.matches(Regex("[a-z0-9][a-z0-9.+/-]{0,63}"))) {
            "Runtime output content type is invalid."
        }
        require(summary.length <= 2_000) { "Runtime output summary is too long." }
        require(sourceEvidenceIds.size <= 40 && sourceEvidenceIds.distinct().size == sourceEvidenceIds.size &&
            sourceEvidenceIds.all { it.isNotBlank() && it.length <= 128 }) {
            "Runtime output source references are invalid."
        }
    }
}

sealed interface WorkflowRuntimeResult {
    val completedAt: Long

    data class Output(
        val value: WorkflowRuntimeOutput,
        override val completedAt: Long
    ) : WorkflowRuntimeResult

    data class Failed(
        val code: String,
        val detail: String,
        override val completedAt: Long
    ) : WorkflowRuntimeResult

    data class NeedsReview(
        val reason: String,
        override val completedAt: Long
    ) : WorkflowRuntimeResult

    data class Cancelled(
        override val completedAt: Long
    ) : WorkflowRuntimeResult

    fun validateFor(request: WorkflowRuntimeRequest) {
        require(completedAt in request.attempt.startedAt..request.deadlineAt) {
            "Runtime result timestamp is outside the active attempt deadline."
        }
        when (this) {
            is Output -> value.validate()
            is Failed -> require(code.matches(Regex("[A-Z0-9_]{1,64}")) &&
                detail.isNotBlank() && detail.length <= 2_000) {
                "Runtime failure result is invalid."
            }
            is NeedsReview -> require(reason.isNotBlank() && reason.length <= 2_000) {
                "Runtime review reason is invalid."
            }
            is Cancelled -> Unit
        }
    }
}

/**
 * One adapter per registered runtime. Implementations must execute only the capabilities named
 * by request.task.requiredCapabilities and must return a result without mutating Memo's workspace.
 */
interface WorkflowRuntimeAdapter {
    val runtimeId: String
    val capabilityIds: Set<String>
    suspend fun execute(request: WorkflowRuntimeRequest): WorkflowRuntimeResult
}

object WorkflowRuntimeAdapterResolver {
    fun resolve(
        request: WorkflowRuntimeRequest,
        registeredRuntimes: List<RuntimeDescriptor>,
        adapters: List<WorkflowRuntimeAdapter>
    ): WorkflowRuntimeAdapter {
        request.validate(registeredRuntimes)
        require(adapters.map { it.runtimeId }.distinct().size == adapters.size) {
            "More than one adapter is registered for the same runtime."
        }
        val adapter = adapters.singleOrNull { it.runtimeId == request.task.runtimeId }
            ?: throw IllegalArgumentException("No adapter is registered for runtime '${request.task.runtimeId}'.")
        val runtime = registeredRuntimes.single { it.id == request.task.runtimeId }
        require(adapter.capabilityIds.all { capabilityId ->
            runtime.capabilities.any { it.id == capabilityId }
        }) {
            "The adapter advertises a capability that is not registered on its exact runtime."
        }
        require(adapter.capabilityIds.containsAll(request.task.requiredCapabilities)) {
            "The selected runtime adapter does not implement every required TaskSpec capability."
        }
        return adapter
    }
}

/** Stable fingerprint of every field that can change a task's meaning or execution boundary. */
internal fun workflowTaskSpecSha256(task: TaskSpec): String {
    val fields = listOf(
        task.id, task.title, task.kind.name, task.participant.name, task.runtimeId,
        task.requiredCapabilities.joinToString("\u001f"), task.dependsOn.joinToString("\u001f"),
        task.inputContract, task.outputContract, task.postcondition,
        task.requiresApproval.toString(), task.externalWrite.toString(), task.externalSideEffect.toString(),
        task.timeoutMs.toString(), task.maxRetries.toString(), task.idempotencyKey
    )
    val canonical = fields.joinToString("") { "${it.length}:$it" }
    return MessageDigest.getInstance("SHA-256")
        .digest(canonical.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

private fun capabilityManifestFingerprint(manifest: CapabilityManifest): String {
    val payload = buildString {
        if (manifest.domainId == "task-spec") {
            append("schema=1\nmode=task-spec\nsource=").append(manifest.sourceIntentSha256).append('\n')
        } else {
            append("schema=1\ndomain=").append(manifest.domainId)
                .append("\nsource=").append(manifest.sourceIntentSha256).append('\n')
        }
        manifest.capabilities.forEach { capability ->
            append(capability.runtimeId).append('|').append(capability.capabilityId).append('|')
                .append(capability.requiresUserApproval).append('|').append(capability.externalSideEffect)
                .append('|').append(capability.runtimeStatus).append('\n')
        }
        manifest.unresolvedRequirements.forEach { append("missing|").append(it).append('\n') }
        append("approval=").append(manifest.requiresUserApproval)
    }
    return MessageDigest.getInstance("SHA-256")
        .digest(payload.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
