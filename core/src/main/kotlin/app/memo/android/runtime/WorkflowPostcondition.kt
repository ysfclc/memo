package app.memo.android.runtime

import app.memo.android.data.EvidenceRecord
import app.memo.android.data.WorkflowAttemptRecord
import app.memo.android.data.WorkflowRuntimeReceipt
import app.memo.android.orchestration.TaskSpec
import java.security.MessageDigest

/**
 * Only registered, deterministic observers can produce postcondition evidence. Model output and
 * a runtime's own success message are deliberately not valid verification methods.
 */
enum class WorkflowPostconditionMethod {
    MEMO_RECORD_LOOKUP,
    BROWSER_VISIBLE_STATE,
    ANDROID_ACCESSIBILITY_STATE,
    FILE_ARTIFACT_INSPECTION
}

enum class WorkflowPostconditionOutcome {
    VERIFIED,
    NOT_VERIFIED,
    CONTRADICTED,
    NEEDS_REVIEW
}

data class WorkflowPostconditionObserverDescriptor(
    val id: String,
    val runtimeId: String,
    val capabilityId: String,
    val method: WorkflowPostconditionMethod,
    val schemaVersion: Int = 1
) {
    fun validate() {
        require(schemaVersion == 1) { "Unsupported postcondition observer schema." }
        require(id.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}"))) {
            "Postcondition observer ID is invalid."
        }
        require(runtimeId.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}")) &&
            capabilityId.matches(Regex("[a-z0-9._-]{1,120}"))) {
            "Postcondition observer runtime or capability is invalid."
        }
    }
}

/**
 * Created only after the runtime output receipt has been checked against its saved EvidenceRecord.
 * The request carries hashes/IDs, not the model output body, into the independent observer.
 */
data class WorkflowPostconditionRequest(
    val task: TaskSpec,
    val attempt: WorkflowAttemptRecord,
    val runtimeReceipt: WorkflowRuntimeReceipt,
    val outputEvidenceId: String,
    val outputSha256: String,
    val observer: WorkflowPostconditionObserverDescriptor,
    val taskSpecSha256: String = workflowTaskSpecSha256(task),
    val postconditionSha256: String = sha256(task.postcondition.trim())
) {
    companion object {
        fun create(
            task: TaskSpec,
            attempt: WorkflowAttemptRecord,
            runtimeReceipt: WorkflowRuntimeReceipt,
            outputEvidence: EvidenceRecord,
            observer: WorkflowPostconditionObserverDescriptor,
            registeredRuntimes: List<RuntimeDescriptor>,
            registeredObservers: List<WorkflowPostconditionObserverDescriptor>
        ): WorkflowPostconditionRequest {
            require(attempt.status == WorkflowRuntimeReceipt.OUTPUT_SAVED) {
                "Postcondition observation requires a durably saved runtime output."
            }
            runtimeReceipt.validateAgainst(attempt, outputEvidence)
            require(attempt.stepId == task.id && attempt.runtimeId == task.runtimeId &&
                attempt.capabilityIds == task.requiredCapabilities &&
                attempt.idempotencyKey == task.idempotencyKey) {
                "The saved runtime receipt does not match the exact TaskSpec."
            }
            require(registeredObservers.map { it.id }.distinct().size == registeredObservers.size) {
                "The postcondition observer registry contains duplicate IDs."
            }
            observer.validate()
            require(registeredObservers.singleOrNull { it.id == observer.id } == observer) {
                "The requested postcondition observer is not registered with this exact descriptor."
            }
            require(registeredRuntimes.map { it.id }.distinct().size == registeredRuntimes.size) {
                "The runtime registry contains duplicate runtime IDs."
            }
            val observerRuntime = registeredRuntimes.singleOrNull { it.id == observer.runtimeId }
                ?: throw IllegalArgumentException("Postcondition observer runtime '${observer.runtimeId}' is not registered.")
            require(observerRuntime.status == "AVAILABLE") {
                "Postcondition observer runtime '${observer.runtimeId}' is not available."
            }
            val capability = observerRuntime.capabilities.singleOrNull { it.id == observer.capabilityId }
                ?: throw IllegalArgumentException("Postcondition observer capability is not registered on its exact runtime.")
            require(!capability.requiresUserApproval && !capability.externalSideEffect) {
                "A postcondition observer must be read-only and must not require an action approval."
            }
            when (observer.method) {
                WorkflowPostconditionMethod.MEMO_RECORD_LOOKUP,
                WorkflowPostconditionMethod.FILE_ARTIFACT_INSPECTION ->
                    require(observerRuntime.kind == RuntimeKind.APP) {
                        "This postcondition method requires a Memo app observer runtime."
                    }
                WorkflowPostconditionMethod.BROWSER_VISIBLE_STATE ->
                    require(observerRuntime.kind == RuntimeKind.BROWSER) {
                        "Browser-visible postconditions require the registered browser runtime."
                    }
                WorkflowPostconditionMethod.ANDROID_ACCESSIBILITY_STATE ->
                    require(observerRuntime.kind == RuntimeKind.EXTERNAL_ANDROID) {
                        "Android-visible postconditions require the registered Accessibility runtime."
                    }
            }
            require(observer.runtimeId != task.runtimeId || observer.capabilityId !in task.requiredCapabilities) {
                "Postcondition evidence must come from a capability distinct from the one that produced the output."
            }
            return WorkflowPostconditionRequest(
                task = task,
                attempt = attempt,
                runtimeReceipt = runtimeReceipt,
                outputEvidenceId = outputEvidence.id,
                outputSha256 = runtimeReceipt.outputSha256,
                observer = observer
            )
        }
    }
}

interface WorkflowPostconditionObserver {
    val descriptor: WorkflowPostconditionObserverDescriptor
    suspend fun observe(request: WorkflowPostconditionRequest): WorkflowPostconditionEvidence
}

/**
 * Evidence is bound to the saved attempt, exact TaskSpec and declared postcondition. Only VERIFIED
 * with a separate persisted evidence item can authorize the orchestrator to complete the checkpoint.
 */
data class WorkflowPostconditionEvidence(
    val attemptId: String,
    val jobId: String,
    val stepId: String,
    val taskRuntimeId: String,
    val taskCapabilityIds: List<String>,
    val idempotencyKey: String,
    val taskSpecSha256: String,
    val postconditionSha256: String,
    val observerId: String,
    val observerRuntimeId: String,
    val observerCapabilityId: String,
    val method: WorkflowPostconditionMethod,
    val outcome: WorkflowPostconditionOutcome,
    val evidenceId: String,
    val evidenceSha256: String,
    val observedAt: Long,
    val detail: String
) {
    val permitsCheckpointCompletion: Boolean
        get() = outcome == WorkflowPostconditionOutcome.VERIFIED

    fun validateAgainst(request: WorkflowPostconditionRequest) {
        require(attemptId == request.attempt.id && jobId == request.attempt.jobId &&
            stepId == request.task.id && taskRuntimeId == request.task.runtimeId &&
            taskCapabilityIds == request.task.requiredCapabilities &&
            idempotencyKey == request.task.idempotencyKey &&
            taskSpecSha256 == request.taskSpecSha256 &&
            postconditionSha256 == request.postconditionSha256) {
            "Postcondition evidence is not bound to the exact workflow attempt and TaskSpec."
        }
        require(observerId == request.observer.id &&
            observerRuntimeId == request.observer.runtimeId &&
            observerCapabilityId == request.observer.capabilityId &&
            method == request.observer.method) {
            "Postcondition evidence was not produced by the registered observer."
        }
        require(observedAt >= request.runtimeReceipt.completedAt && observedAt > 0L) {
            "Postcondition evidence predates the saved runtime output."
        }
        if (outcome == WorkflowPostconditionOutcome.VERIFIED) {
            require(evidenceId.isNotBlank() && evidenceId.length <= 128 &&
                evidenceId != request.outputEvidenceId &&
                evidenceSha256.matches(SHA256_PATTERN)) {
                "Verified postconditions require a separate, content-hashed evidence record."
            }
        } else {
            require(
                (evidenceId.isBlank() && evidenceSha256.isBlank()) ||
                    (evidenceId.length <= 128 && evidenceId != request.outputEvidenceId &&
                        evidenceSha256.matches(SHA256_PATTERN))
            ) {
                "Optional postcondition evidence must be a separate, content-hashed record."
            }
        }
        require(detail.isNotBlank() && detail.length <= 2_000) {
            "Postcondition evidence detail is missing or too long."
        }
        require(taskSpecSha256.matches(SHA256_PATTERN) && postconditionSha256.matches(SHA256_PATTERN) &&
            idempotencyKey.matches(SHA256_PATTERN)) {
            "Postcondition evidence fingerprints are invalid."
        }
    }

    companion object {
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")
    }
}

private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
    .digest(value.toByteArray(Charsets.UTF_8))
    .joinToString("") { "%02x".format(it) }
