package app.memo.android.data

import org.json.JSONObject

enum class FrontierResponseCaptureStatus { PERSISTED, NEEDS_REVIEW, FAILED }

data class FrontierResponseCaptureRequest(
    val jobId: String,
    val stepId: String,
    val conversationId: String,
    val actionId: String = "",
    val consultationTitle: String,
    val providerId: String,
    val providerName: String,
    val prompt: String,
    val response: String,
    val sourceUrl: String,
    val startedAt: Long,
    val completedAt: Long,
    val captureMethod: String,
    val workflowMode: String
)

data class FrontierResponseCaptureTransition(
    val snapshot: AppSnapshot,
    val status: FrontierResponseCaptureStatus,
    val message: String,
    val evidenceId: String = "",
    val entityId: String = ""
)

data class FrontierResponseCaptureResult(
    val status: FrontierResponseCaptureStatus,
    val message: String,
    val evidenceId: String = "",
    val entityId: String = ""
)

/**
 * Builds the complete browser-response record set as one immutable workspace snapshot.
 * No individual record is considered complete until the caller durably writes this snapshot.
 */
object FrontierResponseCaptureCoordinator {
    fun commit(
        snapshot: AppSnapshot,
        request: FrontierResponseCaptureRequest,
        now: Long = nowMillis()
    ): FrontierResponseCaptureTransition {
        val jobId = request.jobId.trim()
        val stepId = request.stepId.trim()
        val conversationId = request.conversationId.trim()
        val actionId = request.actionId.trim()
        val providerId = request.providerId.trim()
        val providerName = request.providerName.trim().take(100)
        val response = request.response.trim().take(14_000)
        val sourceUrl = request.sourceUrl.trim().take(2_048)
        val title = request.consultationTitle.trim().take(200).ifBlank { "Frontier consultation" }

        fun failed(message: String) = FrontierResponseCaptureTransition(
            snapshot = snapshot,
            status = FrontierResponseCaptureStatus.FAILED,
            message = message
        )

        if (jobId.isBlank() || jobId.length > 128 || stepId.isBlank() || stepId.length > 128) {
            return failed("The browser response is not bound to a valid saved job and step.")
        }
        if (conversationId.isBlank() || providerId.isBlank() || providerName.isBlank()) {
            return failed("The browser response is missing its conversation or provider identity.")
        }
        if (response.length < 8 || sourceUrl.isBlank() || !sourceUrl.startsWith("https://", ignoreCase = true)) {
            return failed("The visible provider response or its HTTPS source URL is incomplete.")
        }
        if (request.completedAt < request.startedAt) {
            return failed("The provider response timestamps are inconsistent.")
        }

        val job = snapshot.jobs.firstOrNull { it.id == jobId }
            ?: return failed("The linked Work job no longer exists.")
        val step = job.steps.firstOrNull { it.id == stepId }
            ?: return failed("The linked Work step no longer exists.")
        if (job.sourceConversationId != conversationId) {
            return failed("The Work job belongs to a different conversation.")
        }
        val conversation = snapshot.conversations.firstOrNull { it.id == conversationId }
            ?: return failed("The linked consultation conversation no longer exists.")

        val existingEvidence = snapshot.evidence.firstOrNull { evidence ->
            if (evidence.taskId != jobId) return@firstOrNull false
            val metadata = runCatching { JSONObject(evidence.metadata) }.getOrDefault(JSONObject())
            metadata.optString("stepId") == stepId && metadata.optString("providerId") == providerId
        }
        if (existingEvidence != null && existingEvidence.content != response) {
            return failed("A different response is already saved for this provider step. Review it instead of overwriting evidence.")
        }
        if (existingEvidence == null && snapshot.evidence.size >= 150) {
            return failed("The evidence limit is reached. Export a backup and make room before capturing another provider response.")
        }
        val evidence = existingEvidence ?: EvidenceRecord(
            taskId = jobId,
            title = "$providerName · $title",
            sourceUrl = sourceUrl,
            kind = "FRONTIER_BROWSER_RESPONSE",
            content = response,
            summary = if (request.captureMethod == "USER_PASTED") {
                "User manually captured the visible provider response after automatic detection did not complete. The model's claims have not been independently verified."
            } else {
                "Captured from a visible frontier chat webpage after submission and a new stable response were observed. The model's claims have not been independently verified."
            },
            capturedAt = request.completedAt,
            metadata = JSONObject().apply {
                put("providerId", providerId)
                put("captureMethod", request.captureMethod.take(40))
                put("stepId", stepId)
                put("startedAt", request.startedAt)
                put("completedAt", request.completedAt)
                put("workflowMode", request.workflowMode.take(80))
            }.toString()
        )

        val existingMessage = conversation.messages.firstOrNull {
            it.role == "ASSISTANT" && it.modelId == providerId &&
                it.sourceType == "BROWSER" && it.content == response
        }
        if (existingMessage == null && conversation.messages.size >= 250) {
            return failed("The linked conversation has reached its message limit. Export its history before capturing another response.")
        }
        val message = existingMessage ?: ChatMessageRecord(
            role = "ASSISTANT",
            content = response,
            modelId = providerId,
            sourceType = "BROWSER",
            createdAt = request.completedAt
        )

        val existingEntity = snapshot.entities.firstOrNull {
            it.type == "FRONTIER_RESPONSE" && it.sourceJobId == jobId &&
                it.properties["providerId"] == providerId
        }
        val entityTitle = "$title · $providerName · " + jobId.take(8)
        val conflictingEntity = if (existingEntity == null) snapshot.entities.firstOrNull {
            it.type == "FRONTIER_RESPONSE" &&
                it.title.trim().equals(entityTitle, ignoreCase = true) &&
                it.reviewStatus !in setOf("REJECTED", "MERGED")
        } else null

        var entity = existingEntity
        var entityCreated = false
        var entityIssue = ""
        if (entity == null) {
            entityIssue = when {
                conflictingEntity != null ->
                    "A response entity with this name already exists but is not linked to this job/provider."
                snapshot.entities.size >= 1_200 ->
                    "The entity limit is reached. The response and evidence are saved, but its response entity needs review."
                snapshot.relationships.size >= 4_000 ->
                    "The relationship limit is reached. The response and evidence are saved, but its response entity could not be linked."
                else -> ""
            }
            if (entityIssue.isBlank()) {
                entity = EntityRecord(
                    type = "FRONTIER_RESPONSE",
                    title = entityTitle,
                    description = response.take(12_000),
                    properties = mapOf(
                        "providerId" to providerId,
                        "sourceUrl" to sourceUrl,
                        "prompt" to request.prompt.trim().take(1_800),
                        "workflowMode" to request.workflowMode.take(80),
                        "capturedAt" to request.completedAt.toString(),
                        "captureMethod" to request.captureMethod.take(40),
                        "jobId" to jobId
                    ),
                    reviewStatus = "APPROVED",
                    sourceConversationId = conversationId,
                    sourceMessageId = message.id,
                    sourceJobId = jobId,
                    sourceExcerpt = response.take(2_000),
                    createdAt = now,
                    updatedAt = now
                )
                entityCreated = true
            }
        }

        val evidenceList = if (existingEvidence == null) {
            (listOf(evidence) + snapshot.evidence).take(150)
        } else snapshot.evidence

        val conversations = snapshot.conversations.map { current ->
            if (current.id != conversationId) current
            else current.copy(
                messages = if (existingMessage == null) current.messages + message else current.messages,
                updatedAt = now
            )
        }

        val entities = if (entityCreated && entity != null) {
            (listOf(entity) + snapshot.entities).take(1_200)
        } else snapshot.entities

        var relationships = snapshot.relationships
        if (entity != null && relationships.none {
                it.fromEntityId == conversationId && it.toEntityId == entity.id &&
                    it.relationType == "PROPOSES"
            }
        ) {
            if (relationships.size < 4_000) {
                relationships = listOf(
                    EntityRelationship(
                        fromEntityId = conversationId,
                        toEntityId = entity.id,
                        relationType = "PROPOSES",
                        sourceConversationId = conversationId,
                        createdAt = now
                    )
                ) + relationships
            } else {
                entityIssue = "The relationship limit is reached. The response entity could not be linked to its consultation."
            }
        }

        var next = snapshot.copy(
            evidence = evidenceList,
            conversations = conversations,
            entities = entities,
            relationships = relationships
        )

        var actionIssue = ""
        if (actionId.isBlank()) {
            if (step.requiresApproval) {
                actionIssue = "This step requires an approval-ledger record, but no action identity was supplied."
            }
        } else {
            val action = next.actionApprovals.firstOrNull { it.id == actionId }
            when {
                action == null -> {
                    actionIssue = "The linked outbound action no longer exists in the action ledger."
                }
                action.status == "COMPLETED" && action.evidenceRef == "evidence:" + evidence.id -> Unit
                action.status == "IN_PROGRESS" -> {
                    val completion = ActionApprovalLedger.completeVerified(next, actionId, "evidence:" + evidence.id, now)
                    if (completion.accepted && completion.action?.status == "COMPLETED") {
                        next = completion.snapshot
                    } else {
                        next = completion.snapshot
                        actionIssue = completion.message
                        val review = ActionApprovalLedger.markNeedsReview(next, actionId, completion.message, now)
                        if (review.accepted) next = review.snapshot
                    }
                }
                action.status == "NEEDS_REVIEW" -> {
                    actionIssue = "The linked outbound action already requires human review."
                }
                else -> {
                    actionIssue = "The linked outbound action is " + action.status.lowercase().replace('_', ' ') + " and cannot be verified as completed."
                    val review = ActionApprovalLedger.markNeedsReview(next, actionId, actionIssue, now)
                    if (review.accepted) next = review.snapshot
                }
            }
        }

        val needsReview = actionIssue.isNotBlank() || entityIssue.isNotBlank()
        val stepStatus = if (needsReview) "NEEDS_REVIEW" else "COMPLETED"
        val stepDetail = if (needsReview) {
            listOf("Response, transcript and evidence were saved.", entityIssue, actionIssue)
                .filter { it.isNotBlank() }.joinToString(" ")
        } else if (request.captureMethod == "USER_PASTED") {
            "User manually captured the visible response from $providerName."
        } else {
            "Captured a new, stable response from $providerName."
        }

        val updatedJobs = next.jobs.map { currentJob ->
            if (currentJob.id != jobId) currentJob
            else currentJob.copy(
                steps = currentJob.steps.map { currentStep ->
                    if (currentStep.id != stepId) currentStep
                    else currentStep.copy(
                        status = stepStatus,
                        detail = stepDetail.take(2_000),
                        evidence = response.take(1_800),
                        updatedAt = now
                    )
                },
                updatedAt = now
            )
        }

        val activity = buildList {
            if (existingEvidence == null) add(ActivityRecord(
                at = now, level = "INFO", message = "Evidence captured: " + evidence.title, taskId = jobId
            ))
            if (entityCreated && entity != null) add(ActivityRecord(
                at = now, level = "INFO", message = "Entity created: FRONTIER_RESPONSE · " + entity.title, taskId = jobId
            ))
            if (needsReview) add(ActivityRecord(
                at = now,
                level = "WARN",
                message = "Browser response saved but requires review: " +
                    listOf(entityIssue, actionIssue).filter { it.isNotBlank() }.joinToString(" "),
                taskId = jobId
            ))
        }
        next = next.copy(jobs = updatedJobs, activity = (activity + next.activity).take(500))

        val status = if (needsReview) FrontierResponseCaptureStatus.NEEDS_REVIEW
        else FrontierResponseCaptureStatus.PERSISTED
        val resultMessage = if (needsReview) {
            listOf("The provider response, transcript and evidence were saved.", entityIssue, actionIssue)
                .filter { it.isNotBlank() }.joinToString(" ")
        } else {
            "The provider response, evidence, conversation message and response entity were saved together."
        }

        return FrontierResponseCaptureTransition(
            snapshot = next,
            status = status,
            message = resultMessage,
            evidenceId = evidence.id,
            entityId = entity?.id.orEmpty()
        )
    }

    fun markNeedsReview(
        snapshot: AppSnapshot,
        request: FrontierResponseCaptureRequest,
        reason: String,
        now: Long = nowMillis()
    ): AppSnapshot {
        val jobId = request.jobId.trim()
        val stepId = request.stepId.trim()
        val actionId = request.actionId.trim()
        val safeReason = reason.trim().take(1_600).ifBlank { "The provider response could not be durably committed." }
        var next = snapshot.copy(
            jobs = snapshot.jobs.map { job ->
                if (job.id != jobId) job
                else job.copy(
                    steps = job.steps.map { step ->
                        if (step.id != stepId) step
                        else step.copy(status = "NEEDS_REVIEW", detail = safeReason, updatedAt = now)
                    },
                    updatedAt = now
                )
            }
        )
        if (actionId.isNotBlank()) {
            val review = ActionApprovalLedger.markNeedsReview(next, actionId, safeReason, now)
            if (review.accepted) next = review.snapshot
        }
        return next.copy(
            activity = (listOf(ActivityRecord(
                at = now,
                level = "WARN",
                message = "Browser response capture requires review: " + safeReason,
                taskId = jobId
            )) + next.activity).take(500)
        )
    }
}
