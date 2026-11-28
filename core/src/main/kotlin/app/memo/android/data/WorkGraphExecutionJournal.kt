package app.memo.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Durable, job-scoped execution journal. It records intent before dispatch and never treats
 * an interrupted invocation as safe to replay.
 */
data class WorkNodeInvocationRecord(
    val nodeId: String,
    val invocationId: String,
    val dependencies: List<String> = emptyList(),
    val status: String = STATUS_BLOCKED,
    val attempt: Int = 0,
    val inputHash: String = "",
    val outputReference: String = "",
    val outputHash: String = "",
    val approvalBindingHash: String = "",
    val error: String = "",
    val startedAt: Long = 0L,
    val updatedAt: Long = nowMillis(),
    val finishedAt: Long = 0L
) {
    companion object {
        const val STATUS_BLOCKED = "BLOCKED"
        const val STATUS_READY = "READY"
        const val STATUS_PREPARED = "PREPARED"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_SUCCEEDED = "SUCCEEDED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
        const val STATUS_NEEDS_REVIEW = "NEEDS_REVIEW"
        const val STATUS_CANCELLED = "CANCELLED"
        const val STATUS_SKIPPED = "SKIPPED"
        val ALL_STATUSES = setOf(
            STATUS_BLOCKED, STATUS_READY, STATUS_PREPARED, STATUS_RUNNING, STATUS_SUCCEEDED,
            STATUS_FAILED, STATUS_WAITING_FOR_APPROVAL, STATUS_NEEDS_REVIEW, STATUS_CANCELLED, STATUS_SKIPPED
        )
        val TERMINAL_STATUSES = setOf(STATUS_SUCCEEDED, STATUS_FAILED, STATUS_NEEDS_REVIEW, STATUS_CANCELLED, STATUS_SKIPPED)
    }
}

data class VerifiedWorkOutput private constructor(val reference: String, val sha256: String) {
    companion object {
        /** The adapter must supply the digest read back from the persisted output, not merely its proposed bytes. */
        fun verifyObserved(reference: String, expectedSha256: String, observedSha256: String): VerifiedWorkOutput {
            require(reference.isNotBlank() && reference.length <= 2_048) { "Verified output reference is invalid." }
            require(expectedSha256.matches(Regex("[a-fA-F0-9]{64}")) && observedSha256.matches(Regex("[a-fA-F0-9]{64}"))) {
                "Output verification requires SHA-256 digests."
            }
            require(expectedSha256.equals(observedSha256, ignoreCase = true)) { "Persisted output digest does not match the expected output." }
            return VerifiedWorkOutput(reference, observedSha256.lowercase())
        }
    }
}

data class WorkGraphExecutionRecord(
    val id: String,
    val jobId: String,
    val sourceConversationId: String,
    val sourceMessageId: String,
    val graphDigest: String,
    val status: String = STATUS_READY,
    val invocations: List<WorkNodeInvocationRecord>,
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
) {
    fun validateFor(job: WorkJobRecord) {
        require(id.isNotBlank() && id.length <= 128 && jobId == job.id) { "Execution journal is not bound to this Work job." }
        require(sourceConversationId == job.sourceConversationId && sourceMessageId == job.sourceMessageId) {
            "Execution journal source identity does not match the originating user request."
        }
        require(graphDigest.matches(Regex("[a-fA-F0-9]{64}"))) { "Execution journal graph digest is invalid." }
        require(job.workflowGraphJson.isNotBlank()) { "Execution journal has no source workflow graph." }
        require(graphDigest.equals(WorkGraphExecutionRecord.graphDigest(job.workflowGraphJson), ignoreCase = true)) { "Execution journal does not match the stored workflow graph." }
        val graph = app.memo.android.orchestration.WorkflowGraph.fromJson(job.workflowGraphJson)
        val graphNodes = graph.nodes.associateBy { it.id }
        require(invocations.map { it.nodeId }.toSet() == graphNodes.keys) { "Execution journal nodes do not match the stored workflow graph." }
        require(invocations.all { invocation -> invocation.dependencies == graphNodes.getValue(invocation.nodeId).dependsOn }) {
            "Execution journal dependencies do not match the stored workflow graph."
        }
        val invocationsByNode = invocations.associateBy { it.nodeId }
        require(invocations.all { invocation ->
            invocation.status in setOf(WorkNodeInvocationRecord.STATUS_BLOCKED, WorkNodeInvocationRecord.STATUS_SKIPPED) ||
                invocation.dependencies.all { dependency -> invocationsByNode[dependency]?.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED }
        }) { "A non-blocked node has an unfinished dependency." }
        require(status in ALL_STATUSES && invocations.size in 1..MAX_NODES) { "Execution journal state or node count is invalid." }
        require(status != STATUS_COMPLETED || (
            invocations.all { it.status in WorkNodeInvocationRecord.TERMINAL_STATUSES } &&
                invocations.all { it.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED || it.status == WorkNodeInvocationRecord.STATUS_SKIPPED }
        )) { "A completed graph must have only successful or explicitly skipped nodes." }
        require(status != STATUS_FAILED || invocations.any { it.status == WorkNodeInvocationRecord.STATUS_FAILED }) {
            "A failed graph must contain a failed node."
        }
        require(status != STATUS_NEEDS_REVIEW || invocations.any { it.status == WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW }) {
            "A graph requiring review must contain a node requiring review."
        }
        require(status != STATUS_WAITING_FOR_APPROVAL || invocations.any { it.status == WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL }) {
            "A graph waiting for approval must contain a node waiting for approval."
        }
        require(invocations.map { it.nodeId }.toSet().size == invocations.size) { "Execution journal contains duplicate node IDs." }
        require(invocations.map { it.invocationId }.toSet().size == invocations.size) { "Execution journal contains duplicate invocation IDs." }
        val nodeIds = invocations.map { it.nodeId }.toSet()
        require(invocations.all { node -> node.nodeId !in node.dependencies && node.dependencies.toSet().size == node.dependencies.size && node.dependencies.all { it in nodeIds } }) {
            "Execution journal dependencies are invalid."
        }
        require(invocations.all {
            it.nodeId.isNotBlank() && it.nodeId.length <= 128 &&
                it.dependencies.size <= MAX_NODES && it.dependencies.all { dependency -> dependency.isNotBlank() && dependency.length <= 128 } &&
                it.invocationId.isNotBlank() && it.invocationId.length <= 128 &&
                it.status in WorkNodeInvocationRecord.ALL_STATUSES && it.attempt in 0..1 &&
                it.inputHash.length <= 64 && it.outputReference.length <= 2_048 && it.outputHash.length <= 64 &&
                (it.status != WorkNodeInvocationRecord.STATUS_SUCCEEDED || (it.outputReference.isNotBlank() && it.outputHash.matches(Regex("[a-fA-F0-9]{64}")))) &&
                (it.approvalBindingHash.isBlank() || (it.inputHash.matches(Regex("[a-fA-F0-9]{64}")) && it.approvalBindingHash.equals(it.inputHash, ignoreCase = true))) &&
                it.approvalBindingHash.length <= 64 && it.error.length <= 1_000 &&
                it.startedAt >= 0L && it.updatedAt >= 0L && it.finishedAt >= 0L
        }) { "Execution journal contains an invalid node invocation." }
        require(invocations.count { it.status == WorkNodeInvocationRecord.STATUS_RUNNING } <= 1) {
            "This execution journal currently supports one active node at a time."
        }
    }

    /** Makes a node runnable only after every declared dependency has succeeded. */
    fun markReady(nodeId: String, at: Long = nowMillis()): WorkGraphExecutionRecord {
        val target = invocations.firstOrNull { it.nodeId == nodeId } ?: throw IllegalArgumentException("Unknown workflow node '$nodeId'.")
        require(target.dependencies.all { dependency -> invocations.any { it.nodeId == dependency && it.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED } }) {
            "A node cannot become ready until all dependencies have succeeded."
        }
        val next = updateNode(nodeId, at) { node ->
            require(node.status == WorkNodeInvocationRecord.STATUS_BLOCKED) { "Only a blocked node can become ready." }
            node.copy(status = WorkNodeInvocationRecord.STATUS_READY, updatedAt = at)
        }
        return next.copy(
            status = if (next.invocations.any { it.status == WorkNodeInvocationRecord.STATUS_RUNNING }) STATUS_RUNNING else STATUS_READY,
            updatedAt = at
        )
    }

    /** Persist PREPARED before any adapter is called. */
    fun prepare(nodeId: String, inputHash: String, at: Long = nowMillis()): WorkGraphExecutionRecord {
        require(inputHash.matches(Regex("[a-fA-F0-9]{64}"))) { "A prepared invocation requires a SHA-256 input hash." }
        requireDependenciesSucceeded(nodeId)
        return updateNode(nodeId, at) { node ->
            require(node.status == WorkNodeInvocationRecord.STATUS_READY) { "Only a ready node can be prepared." }
            node.copy(status = WorkNodeInvocationRecord.STATUS_PREPARED, inputHash = inputHash, updatedAt = at)
        }
    }

    /** Dispatch is legal only after PREPARED has been durably written by the caller. */
    fun start(nodeId: String, at: Long = nowMillis()): WorkGraphExecutionRecord {
        requireDependenciesSucceeded(nodeId)
        require(invocations.none { it.status == WorkNodeInvocationRecord.STATUS_RUNNING }) {
            "Another workflow node is already running; parallel dispatch is not enabled."
        }
        return updateNode(nodeId, at) { node ->
            require(node.status == WorkNodeInvocationRecord.STATUS_PREPARED) { "An invocation must be prepared before dispatch." }
            node.copy(status = WorkNodeInvocationRecord.STATUS_RUNNING, startedAt = at, updatedAt = at)
        }.copy(status = STATUS_RUNNING, updatedAt = at)
    }

    fun finish(
        nodeId: String,
        outcome: String,
        verifiedOutput: VerifiedWorkOutput? = null,
        error: String = "",
        at: Long = nowMillis()
    ): WorkGraphExecutionRecord {
        require(outcome in setOf(
            WorkNodeInvocationRecord.STATUS_SUCCEEDED, WorkNodeInvocationRecord.STATUS_FAILED,
            WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL, WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW,
            WorkNodeInvocationRecord.STATUS_CANCELLED
        )) { "Unsupported invocation outcome." }
        if (outcome == WorkNodeInvocationRecord.STATUS_SUCCEEDED) require(verifiedOutput != null) {
            "A successful invocation requires a persisted output whose read-back digest has been verified."
        }
        val next = updateNode(nodeId, at) { node ->
            require(node.status == WorkNodeInvocationRecord.STATUS_RUNNING) { "Only a running invocation can finish." }
            node.copy(
                status = outcome,
                outputReference = verifiedOutput?.reference.orEmpty(),
                outputHash = verifiedOutput?.sha256.orEmpty(),
                error = error.take(1_000),
                updatedAt = at, finishedAt = if (outcome == WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL) 0L else at
            )
        }
        val statuses = next.invocations.map { it.status }
        val runStatus = when {
            statuses.any { it == WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW } -> STATUS_NEEDS_REVIEW
            statuses.any { it == WorkNodeInvocationRecord.STATUS_FAILED } -> STATUS_FAILED
            statuses.all { it in WorkNodeInvocationRecord.TERMINAL_STATUSES } -> STATUS_COMPLETED
            statuses.any { it == WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL } -> STATUS_WAITING_FOR_APPROVAL
            else -> STATUS_RUNNING
        }
        return next.copy(status = runStatus, updatedAt = at)
    }

    /** Approval is bound to the exact prepared invocation input; approval never authorizes a changed payload. */
    fun prepareAfterApproval(nodeId: String, bindingHash: String, at: Long = nowMillis()): WorkGraphExecutionRecord {
        require(bindingHash.matches(Regex("[a-fA-F0-9]{64}"))) { "Approval must bind to a SHA-256 payload hash." }
        requireDependenciesSucceeded(nodeId)
        val next = updateNode(nodeId, at) { node ->
            require(node.status == WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL) { "This node is not waiting for approval." }
            require(node.inputHash.equals(bindingHash, ignoreCase = true)) { "Approval does not match the exact prepared input." }
            node.copy(
                status = WorkNodeInvocationRecord.STATUS_PREPARED,
                approvalBindingHash = bindingHash.lowercase(),
                updatedAt = at
            )
        }
        return next.copy(
            status = if (next.invocations.any { it.status == WorkNodeInvocationRecord.STATUS_RUNNING }) STATUS_RUNNING else STATUS_READY,
            updatedAt = at
        )
    }

    val hasInterruptedInvocation: Boolean
        get() = invocations.any { it.status in setOf(WorkNodeInvocationRecord.STATUS_PREPARED, WorkNodeInvocationRecord.STATUS_RUNNING) }

    /** Process death is ambiguous: PREPARED and RUNNING are both review-only, never auto-replayed. */
    fun recoverAfterRestart(at: Long = nowMillis()): WorkGraphExecutionRecord {
        val recovered = invocations.map { node ->
            if (node.status in setOf(WorkNodeInvocationRecord.STATUS_PREPARED, WorkNodeInvocationRecord.STATUS_RUNNING)) {
                node.copy(
                    status = WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW,
                    error = "Interrupted by application restart. Inspect evidence before deciding whether to continue.",
                    updatedAt = at,
                    finishedAt = at
                )
            } else node
        }
        val runStatus = if (recovered.any { it.status == WorkNodeInvocationRecord.STATUS_NEEDS_REVIEW }) STATUS_NEEDS_REVIEW else status
        return copy(status = runStatus, invocations = recovered, updatedAt = at)
    }

    private fun requireDependenciesSucceeded(nodeId: String) {
        val target = invocations.firstOrNull { it.nodeId == nodeId }
            ?: throw IllegalArgumentException("Unknown workflow node '$nodeId'.")
        require(target.dependencies.all { dependency ->
            invocations.any { it.nodeId == dependency && it.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED }
        }) { "A node cannot be prepared or started before every dependency succeeds." }
    }

    private fun updateNode(
        nodeId: String,
        at: Long,
        transform: (WorkNodeInvocationRecord) -> WorkNodeInvocationRecord
    ): WorkGraphExecutionRecord {
        require(invocations.count { it.status == WorkNodeInvocationRecord.STATUS_RUNNING } <= 1) {
            "A node is already running; parallel dispatch is not enabled."
        }
        require(invocations.any { it.nodeId == nodeId }) { "Unknown workflow node '$nodeId'." }
        return copy(invocations = invocations.map { if (it.nodeId == nodeId) transform(it) else it }, updatedAt = at)
    }

    fun toJson(): String {
        require(invocations.size in 1..MAX_NODES) { "Execution journal node count is invalid." }
        return JSONObject().apply {
            put("version", VERSION)
            put("id", id)
            put("jobId", jobId)
            put("sourceConversationId", sourceConversationId)
            put("sourceMessageId", sourceMessageId)
            put("graphDigest", graphDigest)
            put("status", status)
            put("createdAt", createdAt)
            put("updatedAt", updatedAt)
            put("invocations", JSONArray().apply {
                invocations.forEach { node ->
                    put(JSONObject().apply {
                        put("nodeId", node.nodeId)
                        put("invocationId", node.invocationId)
                        put("dependencies", JSONArray(node.dependencies))
                        put("status", node.status)
                        put("attempt", node.attempt)
                        put("inputHash", node.inputHash)
                        put("outputReference", node.outputReference)
                        put("outputHash", node.outputHash)
                        put("approvalBindingHash", node.approvalBindingHash)
                        put("error", node.error)
                        put("startedAt", node.startedAt)
                        put("updatedAt", node.updatedAt)
                        put("finishedAt", node.finishedAt)
                    })
                }
            })
        }.toString().also { require(it.length <= MAX_JSON_CHARS) { "Execution journal exceeds its safe size limit." } }
    }

    companion object {
        const val STATUS_READY = "READY"
        const val STATUS_RUNNING = "RUNNING"
        const val STATUS_WAITING_FOR_APPROVAL = "WAITING_FOR_APPROVAL"
        const val STATUS_NEEDS_REVIEW = "NEEDS_REVIEW"
        const val STATUS_COMPLETED = "COMPLETED"
        const val STATUS_FAILED = "FAILED"
        const val STATUS_CANCELLED = "CANCELLED"
        private const val VERSION = 1
        private const val MAX_NODES = 40
        private const val MAX_JSON_CHARS = 256_000
        private val ALL_STATUSES = setOf(STATUS_READY, STATUS_RUNNING, STATUS_WAITING_FOR_APPROVAL, STATUS_NEEDS_REVIEW, STATUS_COMPLETED, STATUS_FAILED, STATUS_CANCELLED)

        fun graphDigest(graphJson: String): String =
            MessageDigest.getInstance("SHA-256").digest(graphJson.toByteArray(Charsets.UTF_8))
                .joinToString("") { "%02x".format(it.toInt() and 0xff) }

        /** Creates one stable invocation identity per node from the exact graph stored on the job. */
        fun createFor(
            job: WorkJobRecord,
            executionId: String = newId(),
            invocationIdFactory: (String) -> String = { newId() },
            at: Long = nowMillis()
        ): WorkGraphExecutionRecord {
            require(job.id.isNotBlank() && job.sourceConversationId.isNotBlank() && job.sourceMessageId.isNotBlank()) {
                "A source-linked Work job is required to create an execution journal."
            }
            require(job.workflowGraphJson.isNotBlank()) { "This Work job has no validated workflow graph." }
            val graph = app.memo.android.orchestration.WorkflowGraph.fromJson(job.workflowGraphJson)
            val orderedNodes = app.memo.android.orchestration.WorkflowGraphValidator.topologicalOrder(graph)
            val record = WorkGraphExecutionRecord(
                id = executionId,
                jobId = job.id,
                sourceConversationId = job.sourceConversationId,
                sourceMessageId = job.sourceMessageId,
                graphDigest = graphDigest(job.workflowGraphJson),
                status = STATUS_READY,
                invocations = orderedNodes.map { node ->
                    WorkNodeInvocationRecord(
                        nodeId = node.id,
                        invocationId = invocationIdFactory(node.id),
                        dependencies = node.dependsOn,
                        status = WorkNodeInvocationRecord.STATUS_BLOCKED,
                        updatedAt = at
                    )
                },
                createdAt = at,
                updatedAt = at
            )
            record.validateFor(job)
            return record
        }

        fun fromJson(raw: String): WorkGraphExecutionRecord {
            require(raw.length in 2..MAX_JSON_CHARS) { "Execution journal is empty or too large." }
            val root = JSONObject(raw)
            require(root.optInt("version", 0) == VERSION) { "Execution journal version is not supported." }
            val nodes = root.optJSONArray("invocations") ?: JSONArray()
            require(nodes.length() in 1..MAX_NODES) { "Execution journal node count is invalid." }
            val record = WorkGraphExecutionRecord(
                id = root.optString("id", "").take(128),
                jobId = root.optString("jobId", "").take(128),
                sourceConversationId = root.optString("sourceConversationId", "").take(128),
                sourceMessageId = root.optString("sourceMessageId", "").take(128),
                graphDigest = root.optString("graphDigest", "").take(64),
                status = root.optString("status", "").take(40),
                createdAt = root.optLong("createdAt", 0L),
                updatedAt = root.optLong("updatedAt", 0L),
                invocations = (0 until nodes.length()).map { index ->
                    val row = nodes.getJSONObject(index)
                    WorkNodeInvocationRecord(
                        nodeId = row.optString("nodeId", "").take(128),
                        invocationId = row.optString("invocationId", "").take(128),
                        dependencies = (0 until (row.optJSONArray("dependencies")?.length() ?: 0)).map { dependencyIndex -> row.getJSONArray("dependencies").getString(dependencyIndex).take(128) },
                        status = row.optString("status", "").take(40),
                        attempt = row.optInt("attempt", 0),
                        inputHash = row.optString("inputHash", "").take(64),
                        outputReference = row.optString("outputReference", "").take(2_048),
                        outputHash = row.optString("outputHash", "").take(64),
                        approvalBindingHash = row.optString("approvalBindingHash", "").take(64),
                        error = row.optString("error", "").take(1_000),
                        startedAt = row.optLong("startedAt", 0L),
                        updatedAt = row.optLong("updatedAt", 0L),
                        finishedAt = row.optLong("finishedAt", 0L)
                    )
                }
            )
            require(record.status in ALL_STATUSES) { "Execution journal status is invalid." }
            require(record.id.isNotBlank() && record.jobId.isNotBlank() && record.sourceConversationId.isNotBlank() && record.sourceMessageId.isNotBlank()) {
                "Execution journal identity is incomplete."
            }
            require(record.graphDigest.matches(Regex("[a-fA-F0-9]{64}"))) { "Execution journal graph digest is invalid." }
            require(record.createdAt >= 0L && record.updatedAt >= 0L) { "Execution journal timestamps are invalid." }
            require(record.invocations.map { it.nodeId }.toSet().size == record.invocations.size) { "Execution journal contains duplicate node IDs." }
            require(record.invocations.map { it.invocationId }.toSet().size == record.invocations.size) { "Execution journal contains duplicate invocation IDs." }
            val nodeIds = record.invocations.map { it.nodeId }.toSet()
            require(record.invocations.all { node -> node.nodeId !in node.dependencies && node.dependencies.toSet().size == node.dependencies.size && node.dependencies.all { it in nodeIds } }) {
                "Execution journal dependencies are invalid."
            }
            require(record.invocations.all { node ->
                node.nodeId.isNotBlank() && node.invocationId.isNotBlank() &&
                    node.status in WorkNodeInvocationRecord.ALL_STATUSES && node.attempt in 0..1 &&
                    node.inputHash.length <= 64 && node.approvalBindingHash.length <= 64 &&
                    node.outputReference.length <= 2_048 && node.outputHash.length <= 64 &&
                    (node.status != WorkNodeInvocationRecord.STATUS_SUCCEEDED || (node.outputReference.isNotBlank() && node.outputHash.matches(Regex("[a-fA-F0-9]{64}")))) &&
                    (node.approvalBindingHash.isBlank() || (node.inputHash.matches(Regex("[a-fA-F0-9]{64}")) && node.approvalBindingHash.equals(node.inputHash, ignoreCase = true))) &&
                    node.error.length <= 1_000 &&
                    node.startedAt >= 0L && node.updatedAt >= 0L && node.finishedAt >= 0L
            }) { "Execution journal contains an invalid node invocation." }
            require(record.invocations.count { it.status == WorkNodeInvocationRecord.STATUS_RUNNING } <= 1) {
                "Execution journal contains multiple active invocations."
            }
            return record
        }
    }
}
