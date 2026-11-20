package app.memo.android.ai

import app.memo.android.context.ContextCandidate
import app.memo.android.context.WorkspaceContextRetriever
import app.memo.android.data.EvidenceRecord
import app.memo.android.data.MemoWorkspaceViewModel
import app.memo.android.orchestration.AgentCapabilityAdapter
import app.memo.android.orchestration.AgentExecutionEvent
import app.memo.android.orchestration.AgentToolDefinition
import app.memo.android.orchestration.AgentToolExecutionClass
import app.memo.android.orchestration.AgentToolInvocation
import app.memo.android.orchestration.AgentToolResult
import app.memo.android.orchestration.AgentToolStatus
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Adapters over real Memo-owned operations.
 *
 * This catalogue intentionally contains only operations with callable implementations.
 * Browser, Android, media generation and other descriptors must not be advertised here
 * until their real adapters are connected to this same dispatcher.
 */
object MemoWorkspaceAgentTools {
    fun build(
        viewModel: MemoWorkspaceViewModel,
        contextCandidates: List<ContextCandidate>,
        includeWorkspaceSearch: Boolean,
        sourceConversationId: String,
        sourceMessageId: String,
        sourceExcerpt: String
    ): List<AgentCapabilityAdapter> = buildList {
        if (includeWorkspaceSearch) add(WorkspaceSearchAdapter(viewModel, contextCandidates))
        add(SaveNoteAdapter(viewModel))
        add(ProposeRecordAdapter(viewModel, sourceConversationId, sourceMessageId, sourceExcerpt))
    }

    private class WorkspaceSearchAdapter(
        private val viewModel: MemoWorkspaceViewModel,
        private val candidates: List<ContextCandidate>
    ) : AgentCapabilityAdapter {
        override val definition = AgentToolDefinition(
            id = "workspace.search",
            description = "Search the user's permitted local Memo conversations, entities, jobs and saved evidence. Returns matching excerpts with source IDs; it does not access the internet.",
            inputSchemaJson = """{"type":"object","properties":{"query":{"type":"string","minLength":2,"maxLength":240}},"required":["query"],"additionalProperties":false}""",
            executionClass = AgentToolExecutionClass.READ_ONLY
        )

        override fun isAvailable(): Boolean =
            viewModel.vaultReady.value && viewModel.vaultError.value == null

        override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
            check(isAvailable()) { "The encrypted Memo workspace is unavailable." }
            val query = arguments.getString("query").trim()
            val selection = WorkspaceContextRetriever.select(
                query = query,
                candidates = candidates,
                maxHits = 8,
                maxCharacters = 6_000,
                minScore = 2
            )
            if (selection.isEmpty) {
                return AgentToolResult(
                    status = AgentToolStatus.SUCCEEDED,
                    output = "No matching records were found in the permitted local Memo workspace. This search did not access the internet.",
                    summary = "No matching local records."
                )
            }
            val output = selection.asPromptContext(6_000)
            return AgentToolResult(
                status = AgentToolStatus.SUCCEEDED,
                output = output,
                summary = "Found ${selection.hits.size} local record(s): " + selection.hits.joinToString { "[${it.type}:${it.id}] ${it.title}" },
                evidenceReference = selection.hits.joinToString(",") { "${it.type}:${it.id}" }.take(500)
            )
        }
    }

    private class SaveNoteAdapter(
        private val viewModel: MemoWorkspaceViewModel
    ) : AgentCapabilityAdapter {
        override val definition = AgentToolDefinition(
            id = "workspace.save_note",
            description = "Save a private, editable draft note in Memo's encrypted workspace. This creates a local workspace artifact, not a PDF, exported file or external publication.",
            inputSchemaJson = """{"type":"object","properties":{"title":{"type":"string","minLength":1,"maxLength":120},"content":{"type":"string","minLength":1,"maxLength":12000}},"required":["title","content"],"additionalProperties":false}""",
            executionClass = AgentToolExecutionClass.LOCAL_WRITE
        )

        override fun isAvailable(): Boolean =
            viewModel.vaultReady.value && viewModel.vaultError.value == null

        override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
            check(isAvailable()) { "The encrypted Memo workspace is unavailable." }
            val title = arguments.getString("title").trim().take(120)
            val content = arguments.getString("content").trim().take(12_000)
            require(title.isNotBlank() && content.isNotBlank()) { "A note needs a title and content." }

            val digest = sha256("$title\n$content")
            val existing = viewModel.snapshot.value.evidence.firstOrNull { record ->
                if (record.kind != "AGENT_NOTE") return@firstOrNull false
                runCatching { JSONObject(record.metadata).optString("contentDigest") == digest }.getOrDefault(false)
            }
            if (existing != null) {
                return AgentToolResult(
                    status = AgentToolStatus.SUCCEEDED,
                    output = "The same note already exists in the encrypted Memo workspace. Existing record ID: ${existing.id}. It was not duplicated.",
                    summary = "Existing note reused.",
                    evidenceReference = existing.id
                )
            }

            val metadata = JSONObject()
                .put("origin", "MEMO_AGENT")
                .put("contentDigest", digest)
                .put("idempotencyKey", invocation.idempotencyKey)
                .put("runId", invocation.runId)
                .put("callId", invocation.callId)
                .put("reviewStatus", "DRAFT")
                .toString()
            val note = EvidenceRecord(
                title = title,
                sourceUrl = "",
                kind = "AGENT_NOTE",
                content = content,
                summary = content.lineSequence().firstOrNull().orEmpty().take(2_000),
                metadata = metadata
            )
            val saved = viewModel.saveWorkArtifactDurably(note)
            if (saved.isFailure) {
                val reason = saved.exceptionOrNull()?.message ?: "The note could not be saved."
                val uncertain = viewModel.vaultError.value != null ||
                    reason.contains("vault write", ignoreCase = true) ||
                    reason.contains("read back", ignoreCase = true)
                return AgentToolResult(
                    status = if (uncertain) AgentToolStatus.NEEDS_REVIEW else AgentToolStatus.FAILED,
                    output = reason,
                    summary = if (uncertain) "Note persistence needs review." else "Note save failed."
                )
            }
            val persisted = viewModel.snapshot.value.evidence.firstOrNull { it.id == note.id }
                ?: return AgentToolResult(
                    status = AgentToolStatus.NEEDS_REVIEW,
                    output = "The vault write returned, but the note could not be found in the workspace snapshot. Inspect Memo before retrying.",
                    summary = "Note persistence could not be verified."
                )
            return AgentToolResult(
                status = AgentToolStatus.SUCCEEDED,
                output = "Saved a private draft note in Memo's encrypted workspace. Record ID: ${persisted.id}. The note is available in Work/Evidence for review.",
                summary = "Saved draft note '${persisted.title}'.",
                evidenceReference = persisted.id
            )
        }
    }

    private class ProposeRecordAdapter(
        private val viewModel: MemoWorkspaceViewModel,
        private val sourceConversationId: String,
        private val sourceMessageId: String,
        private val sourceExcerpt: String
    ) : AgentCapabilityAdapter {
        override val definition = AgentToolDefinition(
            id = "workspace.propose_record",
            description = "Create a new typed Memo workspace record as a source-linked proposal for human review. This does not approve, merge, publish or overwrite an existing record. Use this for people, organizations, projects, decisions or other records that the user's goal genuinely requires.",
            inputSchemaJson = """{"type":"object","properties":{"type":{"type":"string","minLength":2,"maxLength":64},"title":{"type":"string","minLength":1,"maxLength":240},"description":{"type":"string","maxLength":12000},"properties":{"type":"object"}},"required":["type","title","description"],"additionalProperties":false}""",
            executionClass = AgentToolExecutionClass.LOCAL_WRITE
        )

        override fun isAvailable(): Boolean =
            sourceConversationId.isNotBlank() &&
                sourceMessageId.isNotBlank() &&
                viewModel.vaultReady.value &&
                viewModel.vaultError.value == null

        override suspend fun invoke(arguments: JSONObject, invocation: AgentToolInvocation): AgentToolResult {
            check(isAvailable()) { "The source conversation or encrypted workspace is unavailable." }
            val type = arguments.getString("type").trim().uppercase().replace(Regex("[^A-Z0-9_]+"), "_").trim('_')
            val title = arguments.getString("title").trim().take(240)
            val description = arguments.getString("description").trim().take(12_000)
            require(type.matches(Regex("[A-Z][A-Z0-9_]{1,63}"))) { "Choose a valid record type." }
            require(title.isNotBlank()) { "A record needs a title." }

            val inputProperties = arguments.optJSONObject("properties") ?: JSONObject()
            require(inputProperties.length() <= 36) { "A record may contain at most 36 user-defined properties." }
            val propertyMap = linkedMapOf<String, String>()
            val keys = inputProperties.keys()
            while (keys.hasNext()) {
                val key = keys.next().trim().take(80)
                require(key.isNotBlank()) { "Record property names cannot be empty." }
                require(key !in setOf("origin", "agentContentDigest", "agentRunId", "agentCallId")) {
                    "The property '$key' is reserved by Memo."
                }
                val value = inputProperties.opt(key)
                require(value is String && value.length <= 2_000) {
                    "Record property '$key' must be text no longer than 2,000 characters."
                }
                propertyMap[key] = value
            }
            val stableProperties = propertyMap.toSortedMap().entries.joinToString("\n") { "${it.key}=${it.value}" }
            val digest = sha256("$type\n$title\n$description\n$stableProperties")
            val existing = viewModel.snapshot.value.entities.firstOrNull { entity ->
                entity.type == type &&
                    entity.title.equals(title, ignoreCase = true) &&
                    entity.reviewStatus !in setOf("REJECTED", "MERGED") &&
                    entity.properties["agentContentDigest"] == digest
            }
            if (existing != null) {
                return AgentToolResult(
                    status = AgentToolStatus.SUCCEEDED,
                    output = "The identical proposal already exists. Existing record ID: ${existing.id}. No duplicate was created.",
                    summary = "Existing record proposal reused.",
                    evidenceReference = existing.id
                )
            }

            val persistedProperties = propertyMap + mapOf(
                "origin" to "MEMO_AGENT",
                "agentContentDigest" to digest,
                "agentRunId" to invocation.runId,
                "agentCallId" to invocation.callId
            )
            val outcome = viewModel.createEntityDurably(
                type = type,
                title = title,
                description = description,
                properties = persistedProperties,
                sourceConversationId = sourceConversationId,
                sourceMessageId = sourceMessageId,
                sourceExcerpt = sourceExcerpt.take(2_000)
            )
            val entity = outcome.getOrNull()
            if (entity == null) {
                val reason = outcome.exceptionOrNull()?.message ?: "Memo could not create the record proposal."
                val uncertain = viewModel.vaultError.value != null ||
                    reason.contains("vault write", ignoreCase = true) ||
                    reason.contains("read back", ignoreCase = true)
                return AgentToolResult(
                    status = if (uncertain) AgentToolStatus.NEEDS_REVIEW else AgentToolStatus.FAILED,
                    output = reason,
                    summary = if (uncertain) "Record persistence needs review." else "Record proposal was not created."
                )
            }
            return AgentToolResult(
                status = AgentToolStatus.SUCCEEDED,
                output = "Created a source-linked record proposal in Memo. Type: ${entity.type}. Title: ${entity.title}. Review status: ${entity.reviewStatus}. Record ID: ${entity.id}. It remains a proposal and was not approved.",
                summary = "Created record proposal '${entity.title}'.",
                evidenceReference = entity.id
            )
        }
    }

    private fun sha256(value: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(value.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { byte -> "%02x".format(byte.toInt() and 0xff) }
    }
}

/** Stable transcript representation: no tool arguments or private payloads are written to the chat. */
fun agentEventTranscriptMessage(
    event: AgentExecutionEvent,
    language: String
): String {
    val key = when (event.type) {
        app.memo.android.orchestration.AgentExecutionEventType.RUN_STARTED -> "Memo started an execution."
        app.memo.android.orchestration.AgentExecutionEventType.TOOL_STARTED -> "Using capability: ${event.toolId}"
        app.memo.android.orchestration.AgentExecutionEventType.TOOL_FINISHED -> "Capability finished: ${event.toolId}"
        app.memo.android.orchestration.AgentExecutionEventType.TOOL_FAILED -> "Capability did not succeed: ${event.toolId}"
        app.memo.android.orchestration.AgentExecutionEventType.RUN_FINISHED -> "Memo returned a final response."
        app.memo.android.orchestration.AgentExecutionEventType.RUN_LIMIT_REACHED -> "Execution limit reached."
        app.memo.android.orchestration.AgentExecutionEventType.RUN_NEEDS_REVIEW -> "Execution paused for review."
        app.memo.android.orchestration.AgentExecutionEventType.RUN_FAILED -> "Execution failed."
        app.memo.android.orchestration.AgentExecutionEventType.RUN_CANCELLED -> "Execution cancelled. Completed results were retained."
    }
    val metadata = buildList {
        if (event.runId.isNotBlank()) add("Run ID: ${event.runId}")
        if (event.callId.isNotBlank()) add("Call ID: ${event.callId}")
        if (event.status.isNotBlank()) add("Status: ${event.status}")
        if (event.evidenceReference.isNotBlank()) add("Evidence: ${event.evidenceReference}")
    }
    val lines = listOf(
        app.memo.android.ui.MemoLocalization.translate(key, language),
        event.summary.takeIf { it.isNotBlank() },
        metadata.takeIf { it.isNotEmpty() }?.joinToString(" · ")
    ).filterNotNull()
    return lines.joinToString("\n").take(1_600)
}
