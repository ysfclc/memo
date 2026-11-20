package app.memo.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Durable, content-bound receipt for one runtime output.
 *
 * A receipt proves that a specific runtime attempt returned these exact bytes and Memo saved
 * them against one checkpoint. It deliberately does not prove the workflow postcondition:
 * that remains NOT_VERIFIED until a separate observer supplies evidence.
 */
data class WorkflowRuntimeReceipt(
    val attemptId: String,
    val jobId: String,
    val stepId: String,
    val attemptNumber: Int,
    val idempotencyKey: String,
    val runtimeId: String,
    val capabilityIds: List<String>,
    val startedAt: Long,
    val completedAt: Long,
    val evidenceId: String,
    val outputSha256: String,
    val outcome: String = OUTPUT_SAVED,
    val postconditionStatus: String = NOT_VERIFIED
) {
    fun toJson(): JSONObject = JSONObject()
        .put("schemaVersion", SCHEMA_VERSION)
        .put("attemptId", attemptId)
        .put("jobId", jobId)
        .put("stepId", stepId)
        .put("attemptNumber", attemptNumber)
        .put("idempotencyKey", idempotencyKey)
        .put("runtimeId", runtimeId)
        .put("capabilityIds", JSONArray(capabilityIds))
        .put("startedAt", startedAt)
        .put("completedAt", completedAt)
        .put("evidenceId", evidenceId)
        .put("outputSha256", outputSha256)
        .put("outcome", outcome)
        .put("postconditionStatus", postconditionStatus)

    fun validateAgainst(attempt: WorkflowAttemptRecord, evidence: EvidenceRecord) {
        require(attempt.status == "OUTPUT_SAVED") { "A runtime receipt must reference a finished output attempt." }
        require(attempt.id == attemptId && attempt.jobId == jobId && attempt.stepId == stepId &&
            attempt.attemptNumber == attemptNumber && attempt.idempotencyKey == idempotencyKey &&
            attempt.runtimeId == runtimeId && attempt.capabilityIds == capabilityIds &&
            attempt.startedAt == startedAt && attempt.finishedAt == completedAt &&
            attempt.evidenceId == evidenceId) {
            "The runtime receipt does not match its exact workflow attempt."
        }
        require(evidence.id == evidenceId && evidence.taskId == jobId && evidence.kind == "WORK_ARTIFACT") {
            "The runtime receipt is not bound to its exact saved artifact."
        }
        require(outputSha256.matches(SHA256_PATTERN) && sha256(evidence.content) == outputSha256) {
            "The saved artifact content no longer matches its runtime receipt."
        }
        require(outcome == OUTPUT_SAVED && postconditionStatus == NOT_VERIFIED) {
            "A saved runtime receipt cannot claim an unobserved workflow postcondition."
        }
        validateFields()
    }

    private fun validateFields() {
        require(attemptId.isNotBlank() && attemptId.length <= 128)
        require(jobId.isNotBlank() && jobId.length <= 128)
        require(stepId.isNotBlank() && stepId.length <= 128)
        require(attemptNumber in 1..WorkflowAttemptJournal.MAX_ATTEMPTS_PER_STEP)
        require(idempotencyKey.matches(SHA256_PATTERN))
        require(runtimeId.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}")))
        require(capabilityIds.size <= 20 && capabilityIds.distinct().size == capabilityIds.size &&
            capabilityIds.all { it.matches(Regex("[a-z0-9._-]{1,120}")) })
        require(startedAt > 0L && completedAt >= startedAt)
        require(evidenceId.isNotBlank() && evidenceId.length <= 128)
        require(outputSha256.matches(SHA256_PATTERN))
        require(outcome == OUTPUT_SAVED && postconditionStatus == NOT_VERIFIED)
    }

    companion object {
        const val SCHEMA_VERSION = 1
        const val OUTPUT_SAVED = "OUTPUT_SAVED"
        const val NOT_VERIFIED = "NOT_VERIFIED"
        const val METADATA_KEY = "runtimeReceipt"
        const val DETAIL_MARKER = "[runtime-receipt:v1]"
        private val SHA256_PATTERN = Regex("[0-9a-f]{64}")

        fun create(
            attempt: WorkflowAttemptRecord,
            evidence: EvidenceRecord,
            completedAt: Long
        ): WorkflowRuntimeReceipt {
            require(attempt.status == "RUNNING") { "A receipt can only be created for the active runtime attempt." }
            require(evidence.taskId == attempt.jobId && evidence.kind == "WORK_ARTIFACT") {
                "The runtime output is not linked to the active workflow attempt."
            }
            require(completedAt >= attempt.startedAt) { "Runtime completion time precedes the attempt start." }
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrElse {
                throw IllegalArgumentException("The runtime output metadata is invalid.")
            }
            require(metadata.optString("jobId") == attempt.jobId && metadata.optString("stepId") == attempt.stepId) {
                "The runtime output metadata does not identify the active checkpoint."
            }
            require(!metadata.has(METADATA_KEY)) { "The runtime output already contains a receipt." }
            return WorkflowRuntimeReceipt(
                attemptId = attempt.id,
                jobId = attempt.jobId,
                stepId = attempt.stepId,
                attemptNumber = attempt.attemptNumber,
                idempotencyKey = attempt.idempotencyKey,
                runtimeId = attempt.runtimeId,
                capabilityIds = attempt.capabilityIds,
                startedAt = attempt.startedAt,
                completedAt = completedAt,
                evidenceId = evidence.id,
                outputSha256 = sha256(evidence.content)
            ).also { it.validateFields() }
        }

        fun attach(evidence: EvidenceRecord, receipt: WorkflowRuntimeReceipt): EvidenceRecord {
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrElse {
                throw IllegalArgumentException("The runtime output metadata is invalid.")
            }
            require(!metadata.has(METADATA_KEY)) { "The runtime output already contains a receipt." }
            metadata.put(METADATA_KEY, receipt.toJson())
            require(metadata.toString().length <= MAX_METADATA_CHARS) {
                "The runtime receipt exceeds the safe artifact metadata limit."
            }
            return evidence.copy(metadata = metadata.toString())
        }

        fun fromEvidence(evidence: EvidenceRecord): WorkflowRuntimeReceipt? {
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrNull() ?: return null
            val value = metadata.optJSONObject(METADATA_KEY) ?: return null
            return runCatching {
                require(value.optInt("schemaVersion", 0) == SCHEMA_VERSION) {
                    "Unsupported runtime receipt schema."
                }
                val capabilities = value.optJSONArray("capabilityIds")
                    ?: throw IllegalArgumentException("Runtime receipt capabilities are missing.")
                val receipt = WorkflowRuntimeReceipt(
                    attemptId = value.optString("attemptId"),
                    jobId = value.optString("jobId"),
                    stepId = value.optString("stepId"),
                    attemptNumber = value.optInt("attemptNumber", 0),
                    idempotencyKey = value.optString("idempotencyKey"),
                    runtimeId = value.optString("runtimeId"),
                    capabilityIds = (0 until capabilities.length()).map { index ->
                        capabilities.getString(index)
                    },
                    startedAt = value.optLong("startedAt", 0L),
                    completedAt = value.optLong("completedAt", 0L),
                    evidenceId = value.optString("evidenceId"),
                    outputSha256 = value.optString("outputSha256"),
                    outcome = value.optString("outcome"),
                    postconditionStatus = value.optString("postconditionStatus")
                )
                receipt.validateFields()
                receipt
            }.getOrNull()
        }

        private const val MAX_METADATA_CHARS = 4_000

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
