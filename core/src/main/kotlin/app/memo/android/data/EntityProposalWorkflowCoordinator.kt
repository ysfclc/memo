package app.memo.android.data

import app.memo.android.orchestration.CapabilityCompiler
import app.memo.android.orchestration.TaskSpec
import app.memo.android.orchestration.WorkflowGraph
import app.memo.android.orchestration.WorkflowGraphCompiler
import app.memo.android.orchestration.WorkflowNodeKind
import app.memo.android.orchestration.WorkflowParticipant
import app.memo.android.runtime.EntityProposalRuntimeCodec
import app.memo.android.runtime.EntityProposalRuntimeInput
import app.memo.android.runtime.EntityProposalRuntimeInputRow
import app.memo.android.runtime.RuntimeDescriptor
import app.memo.android.runtime.RuntimeRegistry
import app.memo.android.runtime.WorkflowCancellationSignal
import app.memo.android.runtime.WorkflowPostconditionEvidence
import app.memo.android.runtime.WorkflowPostconditionMethod
import app.memo.android.runtime.WorkflowPostconditionObserver
import app.memo.android.runtime.WorkflowPostconditionObserverDescriptor
import app.memo.android.runtime.WorkflowPostconditionOutcome
import app.memo.android.runtime.WorkflowPostconditionRequest
import app.memo.android.runtime.WorkflowRuntimeOutput
import app.memo.android.runtime.WorkflowRuntimeRequest
import app.memo.android.runtime.workflowTaskSpecSha256
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale

data class PreparedEntityProposalWorkflow(
    val snapshot: AppSnapshot,
    val jobId: String,
    val taskId: String,
    val inputEvidenceId: String
)

data class SavedEntityProposalOutput(
    val snapshot: AppSnapshot,
    val savedCount: Int,
    val duplicateCount: Int,
    val outputEvidenceId: String
)

data class EntityProposalWorkflowSummary(
    val jobId: String,
    val savedCount: Int,
    val duplicateCount: Int,
    val outputEvidenceId: String,
    val verificationEvidenceId: String
)

/**
 * Coordinates the first real deterministic app-runtime workflow:
 * source-checked proposal input -> exact app capability -> durable output receipt ->
 * independent read-after-write verification -> completed Work checkpoint.
 *
 * It never approves proposals. Every created EntityRecord stays PROPOSED for human review.
 */
object EntityProposalWorkflowCoordinator {
    const val TASK_ID = "propose_entities"
    const val MAX_SOURCE_EXCERPT_CHARS = 2_000
    private const val MAX_ENTITIES = 1_200
    private const val MAX_RELATIONSHIPS = 4_000
    private const val MAX_EVIDENCE = 150

    fun prepare(
        snapshot: AppSnapshot,
        sourceJobId: String,
        sourceConversationId: String,
        proposals: List<OutcomeProposal>,
        registeredRuntimes: List<RuntimeDescriptor> = RuntimeRegistry().listRuntimes(),
        now: Long = nowMillis()
    ): Result<PreparedEntityProposalWorkflow> = runCatching {
        require(now > 0L) { "Workflow creation time is invalid." }
        val maxProposals = app.memo.android.runtime.EntityProposalRuntimeCodec.MAX_PROPOSALS
        require(proposals.isNotEmpty() && proposals.size <= maxProposals) {
            "Select between 1 and $maxProposals source-grounded proposals."
        }
        require(snapshot.jobs.size < 240) { "The Work plan limit has been reached. Export a backup before removing older plans." }
        require(snapshot.evidence.size <= MAX_EVIDENCE - 3) {
            "Evidence storage is too full to persist workflow input, output and independent verification safely. Export a workspace backup first."
        }
        val sourceJob = snapshot.jobs.singleOrNull { it.id == sourceJobId }
            ?: error("The source extraction job no longer exists.")
        require(sourceJob.sourceConversationId == sourceConversationId &&
            sourceJob.status in setOf("RUNNING", "WAITING_FOR_USER")) {
            "The source extraction job no longer belongs to this conversation or is not active."
        }
        val conversation = snapshot.conversations.singleOrNull { it.id == sourceConversationId }
            ?: error("The source conversation no longer exists.")
        val rows = proposals.map { proposal ->
            val type = proposal.type.trim().uppercase(Locale.ROOT)
            require(type in setOf("PROJECT", "TASK", "DECISION", "KNOWLEDGE", "PERSON", "ORGANIZATION", "ARTIFACT", "SELF_MODEL_FACT")) {
                "Unsupported proposal type '$type'."
            }
            val title = proposal.title.trim().take(240)
            val excerpt = proposal.evidence.trim().take(MAX_SOURCE_EXCERPT_CHARS)
            require(title.length >= 2 && excerpt.length >= 8) { "A proposal is missing its title or exact source excerpt." }
            val normalizedExcerpt = normalize(excerpt)
            val sourceMessage = conversation.messages.firstOrNull { message ->
                normalize(message.content).contains(normalizedExcerpt)
            } ?: error("The excerpt for '$title' does not appear inside one individual source message. Nothing was saved.")
            require(normalize(sourceMessage.content).contains(normalizedExcerpt)) {
                "The source excerpt for '$title' could not be verified against its original message."
            }
            EntityProposalRuntimeInputRow(
                type = type,
                title = title,
                description = proposal.description.trim().take(12_000),
                properties = proposal.properties.entries.take(40).associate { it.key.trim().take(80) to it.value.trim().take(2_000) },
                sourceMessageId = sourceMessage.id,
                sourceExcerpt = excerpt
            )
        }
        val inputJson = EntityProposalRuntimeCodec.encodeInput(
            EntityProposalRuntimeInput(sourceConversationId, sourceJobId, rows)
        )
        require(inputJson.length <= WorkflowRuntimeRequest.MAX_INPUT_CHARS) {
            "The validated proposal batch is too large for one bounded workflow. Reduce the number or size of proposals."
        }
        val idempotencyKey = sha256("memo.entity-proposal.batch.v1|$sourceJobId|$sourceConversationId|$inputJson")
        val goal = "Create source-linked Work entity proposals from extraction job $sourceJobId. Keep every proposal in PROPOSED review state."
        val task = TaskSpec(
            id = TASK_ID,
            title = "Save source-grounded entity proposals",
            kind = WorkflowNodeKind.DOMAIN_WORK,
            participant = WorkflowParticipant.DETERMINISTIC,
            runtimeId = "app.entities",
            requiredCapabilities = listOf("entity.propose"),
            dependsOn = emptyList(),
            inputContract = "Validated proposal fields, exact excerpts and source message identities from one Memo conversation.",
            outputContract = "Canonical entity proposal candidates with stable IDs and retained provenance.",
            postcondition = "Every accepted proposal exists in Work as a PROPOSED entity linked to its exact source message.",
            requiresApproval = false,
            externalWrite = false,
            externalSideEffect = false,
            timeoutMs = 15_000,
            maxRetries = 0,
            idempotencyKey = idempotencyKey
        )
        val manifest = CapabilityCompiler.compileForTasks(goal, listOf(task), registeredRuntimes)
        val graph = WorkflowGraphCompiler.compile(goal, listOf(task), manifest)
        val jobId = newId()
        val jobTitle = "Save proposals · " + conversation.title.take(90)
        val inputEvidenceId = sha256("memo.entity-proposal-input.v1|" + jobId + "|" + idempotencyKey).take(32)
        require(snapshot.evidence.none { it.id == inputEvidenceId }) {
            "The deterministic proposal input identity already exists. Reopen Work before retrying."
        }
        val inputSha256 = sha256(inputJson)
        val inputEvidence = EvidenceRecord(
            id = inputEvidenceId,
            taskId = jobId,
            title = "Entity proposal workflow input",
            sourceUrl = "",
            kind = "WORKFLOW_INPUT",
            content = inputJson,
            summary = "Encrypted, validated source-linked inputs for the deterministic entity-proposal checkpoint.",
            metadata = JSONObject()
                .put("jobId", jobId)
                .put("stepId", task.id)
                .put("sourceJobId", sourceJobId)
                .put("sourceConversationId", sourceConversationId)
                .put("adapterId", "memo.entity-proposal.v1")
                .put("contentType", EntityProposalRuntimeCodec.CONTENT_TYPE)
                .put("inputSha256", inputSha256)
                .toString(),
            capturedAt = now
        )
        val job = WorkJobRecord(
            id = jobId,
            title = jobTitle,
            goal = graph.goal,
            status = "DRAFT",
            runtimeId = "app.entities",
            sourceConversationId = sourceConversationId,
            steps = graph.nodes.map { node ->
                JobStepRecord(id = node.id, title = node.title, detail = node.inputContract)
            },
            resultSummary = "A deterministic proposal workflow is ready. It cannot approve or publish records.",
            createdAt = now,
            updatedAt = now,
            workflowGraphJson = graph.toJson()
        )
        PreparedEntityProposalWorkflow(
            snapshot = snapshot.copy(
                evidence = listOf(inputEvidence) + snapshot.evidence,
                jobs = listOf(job) + snapshot.jobs,
                activity = (listOf(ActivityRecord(
                    at = now,
                    message = "Prepared deterministic entity-proposal workflow for " + rows.size + " source-grounded candidate(s).",
                    taskId = jobId
                )) + snapshot.activity).take(500)
            ),
            jobId = jobId,
            taskId = task.id,
            inputEvidenceId = inputEvidenceId
        )
    }

    /**
     * Rebuild a prepared invocation from the encrypted workspace after process recreation.
     * The saved graph and exact input artifact remain the source of truth; no model call is repeated.
     */
    fun restorePrepared(
        snapshot: AppSnapshot,
        jobId: String
    ): Result<PreparedEntityProposalWorkflow> = runCatching {
        val job = snapshot.jobs.singleOrNull { it.id == jobId }
            ?: error("The deterministic proposal workflow no longer exists.")
        val graph = WorkflowGraph.fromJson(job.workflowGraphJson)
        require(graph.nodes.size == 1 && graph.nodes.single().id == TASK_ID) {
            "The saved proposal workflow is not a supported deterministic entity-proposal plan."
        }
        val task = graph.nodes.single()
        val matches = snapshot.evidence.filter { evidence ->
            if (evidence.kind != "WORKFLOW_INPUT" || evidence.taskId != jobId) return@filter false
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrNull() ?: return@filter false
            metadata.optString("stepId") == task.id &&
                metadata.optString("adapterId") == "memo.entity-proposal.v1"
        }
        require(matches.size == 1) {
            "The exact encrypted proposal input is missing or ambiguous. Keep this workflow in review; do not regenerate its payload."
        }
        val inputEvidence = matches.single()
        val metadata = JSONObject(inputEvidence.metadata)
        val input = EntityProposalRuntimeCodec.parseInput(inputEvidence.content)
        require(metadata.optString("jobId") == job.id &&
            metadata.optString("sourceJobId") == input.sourceJobId &&
            metadata.optString("sourceConversationId") == input.sourceConversationId &&
            metadata.optString("contentType") == EntityProposalRuntimeCodec.CONTENT_TYPE &&
            metadata.optString("inputSha256") == sha256(inputEvidence.content) &&
            job.sourceConversationId == input.sourceConversationId &&
            snapshot.jobs.any { it.id == input.sourceJobId } &&
            snapshot.conversations.any { it.id == input.sourceConversationId }) {
            "The encrypted proposal input no longer matches the saved plan and source identities."
        }
        PreparedEntityProposalWorkflow(
            snapshot = snapshot,
            jobId = job.id,
            taskId = task.id,
            inputEvidenceId = inputEvidence.id
        )
    }

    fun buildRequest(
        snapshot: AppSnapshot,
        prepared: PreparedEntityProposalWorkflow,
        registeredRuntimes: List<RuntimeDescriptor> = RuntimeRegistry().listRuntimes(),
        cancellation: WorkflowCancellationSignal = WorkflowCancellationSignal { false }
    ): WorkflowRuntimeRequest {
        val job = snapshot.jobs.singleOrNull { it.id == prepared.jobId }
            ?: error("The deterministic proposal workflow no longer exists.")
        val graph = WorkflowGraph.fromJson(job.workflowGraphJson)
        require(graph.nodes.size == 1 && graph.nodes.single().id == prepared.taskId) {
            "The saved proposal workflow changed. Reopen Work before running it."
        }
        val task = graph.nodes.single()
        val inputEvidence = snapshot.evidence.singleOrNull { it.id == prepared.inputEvidenceId }
            ?: error("The exact encrypted proposal input is missing. Review the workflow instead of rebuilding its payload.")
        require(inputEvidence.kind == "WORKFLOW_INPUT" && inputEvidence.taskId == job.id) {
            "The proposal input is not linked to this exact Work checkpoint."
        }
        val inputMetadata = JSONObject(inputEvidence.metadata)
        val input = EntityProposalRuntimeCodec.parseInput(inputEvidence.content)
        require(inputMetadata.optString("jobId") == job.id &&
            inputMetadata.optString("stepId") == task.id &&
            inputMetadata.optString("sourceJobId") == input.sourceJobId &&
            inputMetadata.optString("sourceConversationId") == input.sourceConversationId &&
            inputMetadata.optString("contentType") == EntityProposalRuntimeCodec.CONTENT_TYPE &&
            inputMetadata.optString("inputSha256") == sha256(inputEvidence.content) &&
            job.sourceConversationId == input.sourceConversationId) {
            "The exact proposal input no longer matches the saved TaskSpec or source."
        }
        val manifest = CapabilityCompiler.compileForTasks(graph.goal, graph.nodes, registeredRuntimes)
        val attempt = snapshot.workflowAttempts.singleOrNull {
            it.jobId == prepared.jobId && it.stepId == prepared.taskId && it.status == "RUNNING"
        } ?: error("The deterministic proposal checkpoint has no single active journal attempt.")
        return WorkflowRuntimeRequest(
            graph = graph,
            taskId = task.id,
            capabilityManifest = manifest,
            attempt = attempt,
            inputJson = inputEvidence.content,
            deadlineAt = attempt.startedAt + task.timeoutMs,
            cancellation = cancellation
        ).also { it.validate(registeredRuntimes) }
    }

    fun saveOutput(
        snapshot: AppSnapshot,
        request: WorkflowRuntimeRequest,
        output: WorkflowRuntimeOutput,
        now: Long = nowMillis()
    ): Result<SavedEntityProposalOutput> = runCatching {
        require(now >= request.attempt.startedAt) { "Runtime output time precedes the active attempt." }
        output.validate()
        require(output.contentType == EntityProposalRuntimeCodec.CONTENT_TYPE) {
            "The app runtime returned an unsupported proposal content type."
        }
        val inputSha256 = sha256(request.inputJson)
        require(snapshot.evidence.any { evidence ->
            if (evidence.kind != "WORKFLOW_INPUT" || evidence.taskId != request.attempt.jobId) return@any false
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrNull() ?: return@any false
            metadata.optString("stepId") == request.task.id &&
                metadata.optString("inputSha256") == inputSha256 &&
                evidence.content == request.inputJson
        }) {
            "The exact encrypted workflow input is no longer present. The output cannot be committed."
        }
        val input = EntityProposalRuntimeCodec.parseInput(request.inputJson)
        val batch = EntityProposalRuntimeCodec.parseOutput(output.content)
        require(batch.idempotencyKey == request.attempt.idempotencyKey &&
            batch.sourceConversationId == input.sourceConversationId &&
            batch.sourceJobId == input.sourceJobId &&
            batch.proposals.size == input.proposals.size) {
            "The runtime result does not match the exact source input and idempotency identity."
        }
        batch.proposals.forEachIndexed { index, actual ->
            val expected = input.proposals[index]
            val rowNumber = index + 1
            require(actual.entityId == EntityProposalRuntimeCodec.entityId(batch.idempotencyKey, index, expected) &&
                actual.type == expected.type && actual.title == expected.title &&
                actual.description == expected.description && actual.properties == expected.properties &&
                actual.sourceMessageId == expected.sourceMessageId && actual.sourceExcerpt == expected.sourceExcerpt) {
                "The deterministic runtime altered proposal $rowNumber or its provenance."
            }
        }
        val parentJob = snapshot.jobs.singleOrNull { it.id == batch.sourceJobId }
            ?: error("The source extraction job no longer exists.")
        require(parentJob.sourceConversationId == batch.sourceConversationId &&
            snapshot.conversations.any { it.id == batch.sourceConversationId }) {
            "The proposal result is not bound to the current source conversation."
        }
        require(snapshot.evidence.size < MAX_EVIDENCE) {
            "Evidence storage is full. Export a workspace backup and remove older evidence before creating another artifact."
        }

        val entitiesToAdd = mutableListOf<EntityRecord>()
        val acceptedIds = mutableListOf<String>()
        var duplicateCount = 0
        batch.proposals.forEach { proposal ->
            val existingById = snapshot.entities.firstOrNull { it.id == proposal.entityId }
            if (existingById != null) {
                require(existingById.type == proposal.type &&
                    existingById.title == proposal.title &&
                    existingById.sourceConversationId == batch.sourceConversationId &&
                    existingById.sourceJobId == batch.sourceJobId &&
                    existingById.sourceMessageId == proposal.sourceMessageId &&
                    existingById.sourceExcerpt == proposal.sourceExcerpt &&
                    existingById.reviewStatus == "PROPOSED") {
                    "A deterministic proposal identity is already used by a different or reviewed entity."
                }
                acceptedIds += proposal.entityId
                return@forEach
            }
            val duplicate = snapshot.entities.firstOrNull {
                it.type.equals(proposal.type, true) &&
                    it.title.trim().equals(proposal.title, true) &&
                    it.reviewStatus !in setOf("REJECTED", "MERGED")
            }
            if (duplicate != null) {
                duplicateCount++
                return@forEach
            }
            val sourceConversation = snapshot.conversations.single { it.id == batch.sourceConversationId }
            val sourceMessage = sourceConversation.messages.singleOrNull { it.id == proposal.sourceMessageId }
                ?: error("The source message for '" + proposal.title + "' no longer exists.")
            require(normalize(sourceMessage.content).contains(normalize(proposal.sourceExcerpt))) {
                "The stored source excerpt for '" + proposal.title + "' no longer matches the original message."
            }
            entitiesToAdd += EntityRecord(
                id = proposal.entityId,
                type = proposal.type,
                title = proposal.title,
                description = proposal.description,
                properties = proposal.properties,
                reviewStatus = "PROPOSED",
                sourceConversationId = batch.sourceConversationId,
                sourceMessageId = proposal.sourceMessageId,
                sourceJobId = batch.sourceJobId,
                sourceExcerpt = proposal.sourceExcerpt,
                createdAt = now,
                updatedAt = now
            )
            acceptedIds += proposal.entityId
        }
        require(acceptedIds.isNotEmpty()) {
            "Every candidate already matches a different existing entity. Nothing new was created; review the existing Work records."
        }
        require(snapshot.entities.size + entitiesToAdd.size <= MAX_ENTITIES) {
            "Entity storage is full. Export a workspace backup and remove older records before creating more proposals."
        }
        val newRelationships = entitiesToAdd.map { entity ->
            val relationSeed = "memo.entity-proposal-link.v1|" + entity.id
            val relationId = sha256(relationSeed).take(32)
            val existing = snapshot.relationships.singleOrNull { it.id == relationId }
            if (existing != null) {
                require(existing.fromEntityId == batch.sourceConversationId &&
                    existing.toEntityId == entity.id && existing.relationType == "PROPOSES") {
                    "A proposal relationship identity is already used by another link."
                }
                null
            } else {
                EntityRelationship(
                    id = relationId,
                    fromEntityId = batch.sourceConversationId,
                    toEntityId = entity.id,
                    relationType = "PROPOSES",
                    sourceConversationId = batch.sourceConversationId,
                    createdAt = now
                )
            }
        }.filterNotNull()
        require(snapshot.relationships.size + newRelationships.size <= MAX_RELATIONSHIPS) {
            "Relationship storage is full. Export a workspace backup before creating more links."
        }

        val artifactSeed = "memo.entity-proposal-output.v1|" + request.attempt.id
        val artifactId = sha256(artifactSeed).take(32)
        val acceptedCount = acceptedIds.distinct().size
        val metadata = JSONObject()
            .put("jobId", request.attempt.jobId)
            .put("stepId", request.task.id)
            .put("provenance", "deterministic-app-runtime")
            .put("adapterId", "memo.entity-proposal.v1")
            .put("requiresHumanReview", true)
            .put("sourceConversationId", batch.sourceConversationId)
            .put("sourceJobId", batch.sourceJobId)
            .put("proposedEntityIds", JSONArray(acceptedIds.distinct()))
            .put("duplicateCount", duplicateCount)
        val artifact = EvidenceRecord(
            id = artifactId,
            taskId = request.attempt.jobId,
            title = "Entity proposal runtime output",
            sourceUrl = "",
            kind = "WORK_ARTIFACT",
            content = output.content,
            summary = "Prepared $acceptedCount source-linked proposal(s); $duplicateCount existing duplicate(s) were skipped.",
            metadata = metadata.toString(),
            capturedAt = now
        )
        val withProposals = snapshot.copy(
            entities = entitiesToAdd + snapshot.entities,
            relationships = newRelationships + snapshot.relationships,
            activity = (listOf(ActivityRecord(
                at = now,
                message = "Deterministic app runtime proposed " + entitiesToAdd.size + " entity record(s) for human review.",
                taskId = request.attempt.jobId
            )) + snapshot.activity).take(500)
        )
        val saved = LocalWorkArtifactCoordinator.saveForReview(
            withProposals, request.attempt.jobId, request.task.id, artifact, now
        ).getOrThrow()
        SavedEntityProposalOutput(
            snapshot = saved,
            savedCount = acceptedCount,
            duplicateCount = duplicateCount,
            outputEvidenceId = artifactId
        )
    }

    fun createPostconditionRequest(
        snapshot: AppSnapshot,
        saved: SavedEntityProposalOutput,
        request: WorkflowRuntimeRequest,
        registeredRuntimes: List<RuntimeDescriptor> = RuntimeRegistry().listRuntimes()
    ): WorkflowPostconditionRequest {
        val outputEvidence = snapshot.evidence.singleOrNull { it.id == saved.outputEvidenceId }
            ?: error("The saved runtime output evidence no longer exists.")
        val attempt = snapshot.workflowAttempts.singleOrNull { it.id == request.attempt.id }
            ?: error("The saved runtime attempt no longer exists.")
        val receipt = WorkflowRuntimeReceipt.fromEvidence(outputEvidence)
            ?: error("The saved runtime output has no content-bound receipt.")
        val observer = EntityProposalPostconditionObserver.descriptor
        return WorkflowPostconditionRequest.create(
            task = request.task,
            attempt = attempt,
            runtimeReceipt = receipt,
            outputEvidence = outputEvidence,
            observer = observer,
            registeredRuntimes = registeredRuntimes,
            registeredObservers = listOf(observer)
        )
    }

    fun complete(
        snapshot: AppSnapshot,
        request: WorkflowPostconditionRequest,
        postcondition: WorkflowPostconditionEvidence,
        now: Long = nowMillis()
    ): Result<AppSnapshot> = runCatching {
        postcondition.validateAgainst(request)
        require(postcondition.permitsCheckpointCompletion &&
            postcondition.outcome == WorkflowPostconditionOutcome.VERIFIED) {
            "The proposal records were not independently verified. Keep this checkpoint in review."
        }
        require(sha256(postcondition.detail) == postcondition.evidenceSha256) {
            "The postcondition evidence content does not match its fingerprint."
        }
        require(snapshot.evidence.size < MAX_EVIDENCE) {
            "Evidence storage is full. Export a workspace backup before recording verification."
        }
        require(snapshot.evidence.none { it.id == postcondition.evidenceId }) {
            "This postcondition evidence has already been committed."
        }
        val job = snapshot.jobs.singleOrNull { it.id == request.attempt.jobId }
            ?: error("The verified Work plan no longer exists.")
        val graph = WorkflowGraph.fromJson(job.workflowGraphJson)
        val savedTask = graph.nodes.singleOrNull { it.id == request.task.id }
            ?: error("The verified TaskSpec no longer exists in the saved workflow.")
        require(workflowTaskSpecSha256(savedTask) == request.taskSpecSha256) {
            "The saved TaskSpec changed before verification was committed."
        }
        val step = job.steps.singleOrNull { it.id == request.task.id }
            ?: error("The verified Work checkpoint no longer exists.")
        require(step.status == "NEEDS_REVIEW") {
            "The Work checkpoint is no longer awaiting this runtime result."
        }
        val attempt = snapshot.workflowAttempts.singleOrNull { it.id == request.attempt.id }
            ?: error("The exact workflow attempt no longer exists.")
        require(attempt.status == "OUTPUT_SAVED" &&
            attempt.evidenceId == request.outputEvidenceId) {
            "The saved output no longer matches the active postcondition request."
        }
        val outputEvidence = snapshot.evidence.singleOrNull { it.id == request.outputEvidenceId }
            ?: error("The runtime output evidence no longer exists.")
        request.runtimeReceipt.validateAgainst(attempt, outputEvidence)

        val verificationRecord = EvidenceRecord(
            id = postcondition.evidenceId,
            taskId = request.attempt.jobId,
            title = "Verified entity-proposal postcondition",
            sourceUrl = "",
            kind = "WORKFLOW_VERIFICATION",
            content = postcondition.detail,
            summary = postcondition.detail.take(1_000),
            metadata = JSONObject()
                .put("jobId", request.attempt.jobId)
                .put("stepId", request.task.id)
                .put("attemptId", request.attempt.id)
                .put("taskSpecSha256", request.taskSpecSha256)
                .put("postconditionSha256", request.postconditionSha256)
                .put("observerId", postcondition.observerId)
                .put("observerRuntimeId", postcondition.observerRuntimeId)
                .put("observerCapabilityId", postcondition.observerCapabilityId)
                .put("method", postcondition.method.name)
                .put("outcome", postcondition.outcome.name)
                .put("outputEvidenceId", request.outputEvidenceId)
                .put("outputSha256", request.outputSha256)
                .put("evidenceSha256", postcondition.evidenceSha256)
                .put("observedAt", postcondition.observedAt)
                .toString(),
            capturedAt = postcondition.observedAt
        )
        val committedAt = maxOf(now, postcondition.observedAt)
        val updatedStep = step.copy(
            status = "COMPLETED",
            detail = "Independent read-after-write verification passed. The proposed entities remain PROPOSED until you approve them.",
            evidence = "output:" + request.outputEvidenceId + ";verification:" + verificationRecord.id,
            updatedAt = committedAt
        )
        val updatedSteps = job.steps.map { if (it.id == step.id) updatedStep else it }
        val allComplete = updatedSteps.isNotEmpty() && updatedSteps.all { it.status == "COMPLETED" }
        val resultSummary = if (allComplete) {
            "The deterministic app runtime saved and independently verified: " + postcondition.detail +
                " Entity records remain in PROPOSED review state; no entity was automatically approved."
        } else {
            "This app-runtime checkpoint was independently verified. Dependency-ready work can continue."
        }
        val updatedJob = job.copy(
            status = if (allComplete) "SUCCEEDED" else "READY",
            steps = updatedSteps,
            resultSummary = resultSummary,
            updatedAt = committedAt
        )
        snapshot.copy(
            evidence = listOf(verificationRecord) + snapshot.evidence,
            jobs = snapshot.jobs.map { if (it.id == job.id) updatedJob else it },
            activity = (listOf(ActivityRecord(
                at = committedAt,
                message = "Independent app-runtime postcondition verified: " + request.task.title,
                taskId = job.id
            )) + snapshot.activity).take(500)
        ).also { WorkflowAttemptJournal.validate(it) }
    }

    internal fun normalizedContains(source: String, excerpt: String): Boolean =
        normalize(source).contains(normalize(excerpt))

    private fun normalize(value: String): String =
        value.trim().replace(Regex("\\s+"), " ").lowercase(Locale.ROOT)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/**
 * Independent read-only verifier. It reads the persisted workspace snapshot, not the adapter's
 * return value, and can only authorize completion when every referenced proposal is still present
 * with exact source provenance and PROPOSED status.
 */
class EntityProposalPostconditionObserver(
    private val snapshot: AppSnapshot
) : WorkflowPostconditionObserver {
    override val descriptor: WorkflowPostconditionObserverDescriptor = Companion.descriptor

    override suspend fun observe(request: WorkflowPostconditionRequest): WorkflowPostconditionEvidence {
        val output = snapshot.evidence.singleOrNull { it.id == request.outputEvidenceId }
        val metadata = output?.let { runCatching { JSONObject(it.metadata) }.getOrNull() }
        val ids = metadata?.optJSONArray("proposedEntityIds") ?: JSONArray()
        val sourceConversationId = metadata?.optString("sourceConversationId").orEmpty()
        val sourceJobId = metadata?.optString("sourceJobId").orEmpty()
        val conversation = snapshot.conversations.singleOrNull { it.id == sourceConversationId }
        val problems = mutableListOf<String>()
        if (output == null || output.kind != "WORK_ARTIFACT") problems += "The output artifact is missing."
        if (ids.length() == 0) problems += "The output does not identify any proposed records."
        if (conversation == null) problems += "The source conversation is missing."
        if (sourceJobId.isBlank()) problems += "The source extraction job identity is missing."
        val verifiedTitles = mutableListOf<String>()
        for (index in 0 until ids.length()) {
            val id = ids.optString(index)
            val entity = snapshot.entities.singleOrNull { it.id == id }
            if (entity == null) {
                problems += "A proposed entity record is missing."
                continue
            }
            val sourceMessage = conversation?.messages?.singleOrNull { it.id == entity.sourceMessageId }
            val provenanceValid = entity.reviewStatus == "PROPOSED" &&
                entity.sourceConversationId == sourceConversationId &&
                entity.sourceJobId == sourceJobId &&
                entity.sourceExcerpt.isNotBlank() &&
                sourceMessage != null &&
                EntityProposalWorkflowCoordinator.normalizedContains(sourceMessage.content, entity.sourceExcerpt)
            if (!provenanceValid) {
                problems += "A proposed entity failed its source, review-state or message-link check."
            } else {
                verifiedTitles += entity.type + ": " + entity.title
            }
        }
        val verified = problems.isEmpty() && verifiedTitles.isNotEmpty()
        val detail = if (verified) {
            "Read-after-write verified " + verifiedTitles.size + " Work proposal(s): " +
                verifiedTitles.joinToString("; ") + ". All remain PROPOSED for human approval."
        } else {
            "Postcondition needs review: " + problems.distinct().joinToString(" ")
        }.take(2_000)
        val observedAt = maxOf(System.currentTimeMillis(), request.runtimeReceipt.completedAt)
        val evidenceSeed = "memo.entity-proposal-verification.v1|" + request.attempt.id + "|" + descriptor.id
        return WorkflowPostconditionEvidence(
            attemptId = request.attempt.id,
            jobId = request.attempt.jobId,
            stepId = request.task.id,
            taskRuntimeId = request.task.runtimeId,
            taskCapabilityIds = request.task.requiredCapabilities,
            idempotencyKey = request.task.idempotencyKey,
            taskSpecSha256 = request.taskSpecSha256,
            postconditionSha256 = request.postconditionSha256,
            observerId = descriptor.id,
            observerRuntimeId = descriptor.runtimeId,
            observerCapabilityId = descriptor.capabilityId,
            method = descriptor.method,
            outcome = if (verified) WorkflowPostconditionOutcome.VERIFIED else WorkflowPostconditionOutcome.NEEDS_REVIEW,
            evidenceId = if (verified) sha256(evidenceSeed).take(32) else "",
            evidenceSha256 = if (verified) sha256(detail) else "",
            observedAt = observedAt,
            detail = detail
        )
    }

    companion object {
        val descriptor = WorkflowPostconditionObserverDescriptor(
            id = "memo.entity-proposal-observer.v1",
            runtimeId = "app.jobs",
            capabilityId = "job.verify",
            method = WorkflowPostconditionMethod.MEMO_RECORD_LOOKUP
        )

        private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
            .digest(value.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(it) }
    }
}
