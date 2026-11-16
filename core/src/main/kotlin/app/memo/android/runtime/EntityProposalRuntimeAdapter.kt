package app.memo.android.runtime

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.Locale
import java.util.concurrent.CancellationException

/**
 * The first deterministic Memo app adapter.
 *
 * It accepts only source-linked entity-proposal input and returns a canonical, bounded candidate
 * batch. It does not write the workspace or approve entities; the orchestration transaction owns
 * persistence, and a separate read-only observer verifies the resulting records.
 */
data class EntityProposalRuntimeInput(
    val sourceConversationId: String,
    val sourceJobId: String,
    val proposals: List<EntityProposalRuntimeInputRow>
)

data class EntityProposalRuntimeInputRow(
    val type: String,
    val title: String,
    val description: String,
    val properties: Map<String, String>,
    val sourceMessageId: String,
    val sourceExcerpt: String
)

data class EntityProposalRuntimeOutputRow(
    val entityId: String,
    val type: String,
    val title: String,
    val description: String,
    val properties: Map<String, String>,
    val sourceMessageId: String,
    val sourceExcerpt: String
)

data class EntityProposalRuntimeBatch(
    val idempotencyKey: String,
    val sourceConversationId: String,
    val sourceJobId: String,
    val proposals: List<EntityProposalRuntimeOutputRow>
)

object EntityProposalRuntimeCodec {
    const val CONTENT_TYPE = "application/vnd.memo.entity-proposals+json"
    const val MAX_PROPOSALS = 12
    private val allowedTypes = setOf(
        "PROJECT", "TASK", "DECISION", "KNOWLEDGE", "PERSON", "ORGANIZATION", "ARTIFACT"
    )

    fun encodeInput(input: EntityProposalRuntimeInput): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("sourceConversationId", input.sourceConversationId)
        put("sourceJobId", input.sourceJobId)
        put("proposals", JSONArray().apply {
            input.proposals.forEach { row ->
                put(JSONObject().apply {
                    put("type", row.type)
                    put("title", row.title)
                    put("description", row.description)
                    put("properties", JSONObject(row.properties))
                    put("sourceMessageId", row.sourceMessageId)
                    put("sourceExcerpt", row.sourceExcerpt)
                })
            }
        })
    }.toString()

    fun parseInput(raw: String): EntityProposalRuntimeInput {
        require(raw.isNotBlank() && raw.length <= WorkflowRuntimeRequest.MAX_INPUT_CHARS) {
            "Entity proposal input is empty or exceeds the runtime input limit."
        }
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", 0) == 1) { "Unsupported entity proposal input schema." }
        val conversationId = root.optString("sourceConversationId").trim()
        val sourceJobId = root.optString("sourceJobId").trim()
        require(validId(conversationId) && validId(sourceJobId)) {
            "Entity proposals must identify their source conversation and source job."
        }
        val rows = root.optJSONArray("proposals")
            ?: throw IllegalArgumentException("Entity proposal input is missing its proposals array.")
        require(rows.length() in 1..MAX_PROPOSALS) {
            "Entity proposal batches must contain between 1 and $MAX_PROPOSALS records."
        }
        val seen = mutableSetOf<String>()
        val proposals = (0 until rows.length()).map { index ->
            val row = rows.optJSONObject(index)
                ?: throw IllegalArgumentException("Entity proposal row ${index + 1} is malformed.")
            val type = row.optString("type").trim().uppercase(Locale.ROOT)
            val title = row.optString("title").trim()
            val description = row.optString("description").trim()
            val sourceMessageId = row.optString("sourceMessageId").trim()
            val sourceExcerpt = row.optString("sourceExcerpt").trim()
            require(type in allowedTypes) { "Entity proposal row ${index + 1} has an unsupported type." }
            require(title.length in 2..240) { "Entity proposal row ${index + 1} needs a valid title." }
            require(description.length <= 12_000) { "Entity proposal row ${index + 1} has an oversized description." }
            require(validId(sourceMessageId) && sourceExcerpt.length in 8..2_000) {
                "Entity proposal row ${index + 1} must retain a source message and exact excerpt."
            }
            val duplicateKey = "$type|${title.lowercase(Locale.ROOT)}"
            require(seen.add(duplicateKey)) { "The batch contains duplicate $type proposal '$title'." }
            val rawProperties = row.optJSONObject("properties") ?: JSONObject()
            require(rawProperties.length() <= 40) { "Entity proposal row ${index + 1} has too many properties." }
            val properties = linkedMapOf<String, String>()
            val keys = rawProperties.keys()
            while (keys.hasNext()) {
                val key = keys.next().trim()
                val value = rawProperties.optString(key, "").trim()
                require(key.isNotBlank() && key.length <= 80 && value.length <= 2_000) {
                    "Entity proposal row ${index + 1} has an invalid property."
                }
                if (value.isNotBlank()) properties[key] = value
            }
            EntityProposalRuntimeInputRow(
                type = type,
                title = title,
                description = description,
                properties = properties,
                sourceMessageId = sourceMessageId,
                sourceExcerpt = sourceExcerpt
            )
        }
        return EntityProposalRuntimeInput(conversationId, sourceJobId, proposals)
    }

    fun encodeOutput(batch: EntityProposalRuntimeBatch): String = JSONObject().apply {
        put("schemaVersion", 1)
        put("idempotencyKey", batch.idempotencyKey)
        put("sourceConversationId", batch.sourceConversationId)
        put("sourceJobId", batch.sourceJobId)
        put("proposals", JSONArray().apply {
            batch.proposals.forEach { row ->
                put(JSONObject().apply {
                    put("entityId", row.entityId)
                    put("type", row.type)
                    put("title", row.title)
                    put("description", row.description)
                    put("properties", JSONObject(row.properties))
                    put("sourceMessageId", row.sourceMessageId)
                    put("sourceExcerpt", row.sourceExcerpt)
                })
            }
        })
    }.toString()

    fun parseOutput(raw: String): EntityProposalRuntimeBatch {
        require(raw.isNotBlank() && raw.length <= WorkflowRuntimeRequest.MAX_OUTPUT_CHARS) {
            "Entity proposal runtime output is empty or too large."
        }
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", 0) == 1) { "Unsupported entity proposal output schema." }
        val key = root.optString("idempotencyKey")
        val conversationId = root.optString("sourceConversationId")
        val sourceJobId = root.optString("sourceJobId")
        require(key.matches(Regex("[0-9a-f]{64}")) && validId(conversationId) && validId(sourceJobId)) {
            "Entity proposal runtime output has an invalid identity."
        }
        val rows = root.optJSONArray("proposals")
            ?: throw IllegalArgumentException("Entity proposal output is missing its proposals array.")
        require(rows.length() in 1..MAX_PROPOSALS) { "Entity proposal output has an invalid record count." }
        val proposals = (0 until rows.length()).map { index ->
            val row = rows.optJSONObject(index)
                ?: throw IllegalArgumentException("Entity proposal output row ${index + 1} is malformed.")
            val propertiesJson = row.optJSONObject("properties") ?: JSONObject()
            val properties = linkedMapOf<String, String>()
            val keys = propertiesJson.keys()
            while (keys.hasNext()) {
                val name = keys.next()
                properties[name] = propertiesJson.optString(name, "")
            }
            EntityProposalRuntimeOutputRow(
                entityId = row.optString("entityId"),
                type = row.optString("type"),
                title = row.optString("title"),
                description = row.optString("description"),
                properties = properties,
                sourceMessageId = row.optString("sourceMessageId"),
                sourceExcerpt = row.optString("sourceExcerpt")
            ).also { proposal ->
                require(proposal.entityId.matches(Regex("[0-9a-f]{32}")) &&
                    proposal.type in allowedTypes &&
                    proposal.title.length in 2..240 &&
                    validId(proposal.sourceMessageId) &&
                    proposal.sourceExcerpt.length in 8..2_000) {
                    "Entity proposal output row ${index + 1} failed identity or provenance validation."
                }
            }
        }
        require(proposals.map { it.entityId }.distinct().size == proposals.size) {
            "Entity proposal output contains duplicate record identities."
        }
        return EntityProposalRuntimeBatch(key, conversationId, sourceJobId, proposals)
    }

    internal fun entityId(idempotencyKey: String, index: Int, row: EntityProposalRuntimeInputRow): String =
        sha256("memo.entity-proposal.v1|$idempotencyKey|$index|${row.type}|${row.title.lowercase(Locale.ROOT)}")
            .take(32)

    private fun validId(value: String): Boolean =
        value.isNotBlank() && value.length <= 128 && value.matches(Regex("[A-Za-z0-9._:-]+"))

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}

/**
 * Source evidence is checked again at the app-runtime boundary, not trusted merely because a model
 * returned a JSON proposal. The adapter remains pure: its result is a candidate batch only.
 */
class EntityProposalRuntimeAdapter(
    private val registeredRuntimes: List<RuntimeDescriptor> = RuntimeRegistry().listRuntimes()
) : WorkflowRuntimeAdapter {
    override val runtimeId: String = "app.entities"
    override val capabilityIds: Set<String> = setOf("entity.propose")

    override suspend fun execute(request: WorkflowRuntimeRequest): WorkflowRuntimeResult {
        val started = System.currentTimeMillis()
        return try {
            request.validate(registeredRuntimes)
            require(request.task.participant == app.memo.android.orchestration.WorkflowParticipant.DETERMINISTIC &&
                request.task.runtimeId == runtimeId &&
                request.task.requiredCapabilities == listOf("entity.propose")) {
                "This adapter only handles the registered app.entities/entity.propose capability."
            }
            request.checkActive(started)
            val input = EntityProposalRuntimeCodec.parseInput(request.inputJson)
            val rows = input.proposals.mapIndexed { index, proposal ->
                EntityProposalRuntimeOutputRow(
                    entityId = EntityProposalRuntimeCodec.entityId(request.attempt.idempotencyKey, index, proposal),
                    type = proposal.type,
                    title = proposal.title,
                    description = proposal.description,
                    properties = proposal.properties,
                    sourceMessageId = proposal.sourceMessageId,
                    sourceExcerpt = proposal.sourceExcerpt
                )
            }
            val batch = EntityProposalRuntimeBatch(
                idempotencyKey = request.attempt.idempotencyKey,
                sourceConversationId = input.sourceConversationId,
                sourceJobId = input.sourceJobId,
                proposals = rows
            )
            val content = EntityProposalRuntimeCodec.encodeOutput(batch)
            request.checkActive(System.currentTimeMillis())
            WorkflowRuntimeResult.Output(
                value = WorkflowRuntimeOutput(
                    content = content,
                    contentType = EntityProposalRuntimeCodec.CONTENT_TYPE,
                    summary = "Prepared ${rows.size} source-linked entity proposal(s) for review.",
                    sourceEvidenceIds = rows.map { it.sourceMessageId }.distinct()
                ),
                completedAt = System.currentTimeMillis()
            )
        } catch (error: CancellationException) {
            throw error
        } catch (error: WorkflowRuntimeCancelledException) {
            WorkflowRuntimeResult.Cancelled(minOf(System.currentTimeMillis(), request.deadlineAt))
        } catch (error: WorkflowRuntimeDeadlineExceededException) {
            WorkflowRuntimeResult.Failed(
                "DEADLINE_EXCEEDED",
                error.message ?: "The deterministic runtime exceeded its deadline.",
                request.deadlineAt
            )
        } catch (error: Throwable) {
            WorkflowRuntimeResult.Failed(
                "INVALID_PROPOSAL_INPUT",
                (error.message ?: "The entity proposal batch could not be validated.").take(2_000),
                minOf(System.currentTimeMillis(), request.deadlineAt)
            )
        }
    }
}
