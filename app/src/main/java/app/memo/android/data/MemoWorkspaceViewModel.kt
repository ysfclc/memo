package app.memo.android.data

import android.app.Application
import android.content.ContentResolver
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.memo.android.automation.TaskRules
import app.memo.android.runtime.RuntimeRegistry
import app.memo.android.orchestration.IntentPlanner
import app.memo.android.orchestration.CapabilityCompiler
import app.memo.android.orchestration.WorkflowGraphCompiler
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.withContext
import java.util.Locale

class MemoWorkspaceViewModel(application: Application) : AndroidViewModel(application) {
    private val vault = EncryptedVault(application)
    private val persistenceQueue = Channel<AppSnapshot>(Channel.CONFLATED)
    private val preferences = application.getSharedPreferences("synthos_preferences", Application.MODE_PRIVATE)
    private val runtimeRegistry = RuntimeRegistry()

    private val _snapshot = MutableStateFlow(AppSnapshot())
    val snapshot: StateFlow<AppSnapshot> = _snapshot.asStateFlow()

    private val _vaultReady = MutableStateFlow(false)
    val vaultReady: StateFlow<Boolean> = _vaultReady.asStateFlow()

    private val _vaultError = MutableStateFlow<String?>(null)
    val vaultError: StateFlow<String?> = _vaultError.asStateFlow()

    private val _themeMode = MutableStateFlow(preferences.getString("theme_mode", "SYSTEM") ?: "SYSTEM")
    val themeMode: StateFlow<String> = _themeMode.asStateFlow()

    private val _uiLanguage = MutableStateFlow(
        preferences.getString("ui_language", "EN")?.uppercase(Locale.ROOT)?.takeIf { it in setOf("EN", "PL", "TR") } ?: "EN"
    )
    val uiLanguage: StateFlow<String> = _uiLanguage.asStateFlow()


    init {
        viewModelScope.launch {
            for (candidate in persistenceQueue) {
                try {
                    withContext(Dispatchers.IO) { vault.write(candidate) }
                    _vaultError.value = null
                } catch (error: Throwable) {
                    _vaultError.value = safeError(error, "Could not save the encrypted workspace. Export your data before continuing.")
                }
            }
        }
    }
    fun unlockAndLoad() {
        if (_vaultReady.value) return
        viewModelScope.launch {
            try {
                val stored = withContext(Dispatchers.IO) { vault.read() }
                val restored = stored?.recoverInterruptedTasks() ?: AppSnapshot()
                _snapshot.value = restored
                _vaultError.value = null
                _vaultReady.value = true
                if (stored == null) {
                    log("Workspace created on this device.")
                } else if (restored != stored) {
                    persist(restored)
                }
            } catch (error: Throwable) {
                _vaultError.value = safeError(error, "The encrypted workspace could not be opened.")
                _vaultReady.value = true
            }
        }
    }

    /** Clears decrypted data from UI state when the user manually locks the workspace. */
    fun clearSensitiveMemory() {
        _snapshot.value = AppSnapshot()
        _vaultReady.value = false
        _vaultError.value = null
    }

    fun setThemeMode(mode: String) {
        val normalized = mode.uppercase(Locale.ROOT).takeIf { it in setOf("SYSTEM", "LIGHT", "DARK") } ?: "SYSTEM"
        _themeMode.value = normalized
        preferences.edit().putString("theme_mode", normalized).apply()
    }

    /** Interface language is a local device preference; it never changes user-authored content. */
    fun setUiLanguage(language: String) {
        val normalized = language.uppercase(Locale.ROOT).takeIf { it in setOf("EN", "PL", "TR") } ?: "EN"
        _uiLanguage.value = normalized
        preferences.edit().putString("ui_language", normalized).apply()
    }

    fun createConversation(title: String = "New conversation", sourceType: String = "NATIVE", sourceReference: String = ""): String {
        if (_snapshot.value.conversations.size >= 160 || _snapshot.value.entities.size >= 1_200) {
            log("Conversation or entity limit reached. Export a backup before removing older records.", "WARN")
            return ""
        }
        val conversationId = newId()
        val now = nowMillis()
        val conversation = ConversationRecord(
            id = conversationId,
            title = title.trim().take(240).ifBlank { "New conversation" },
            sourceType = sourceType.trim().uppercase(Locale.ROOT).take(40).ifBlank { "NATIVE" },
            sourceReference = sourceReference.trim().take(2_048),
            createdAt = now,
            updatedAt = now
        )
        val conversationEntity = EntityRecord(
            id = conversationId,
            type = "CONVERSATION",
            title = conversation.title,
            description = when (sourceType.uppercase(Locale.ROOT)) {
                "IMPORTED" -> "Imported conversation. Source access was user-mediated."
                "BROWSER_WORKFLOW" -> "Frontier browser workflow conversation. Provider exchanges are retained with source URLs and evidence."
                else -> "Native Memo conversation."
            },
            properties = mapOf("sourceType" to conversation.sourceType, "sourceReference" to conversation.sourceReference),
            sourceConversationId = conversationId,
            createdAt = now,
            updatedAt = now
        )
        updateSnapshot { current ->
            current.copy(
                conversations = listOf(conversation) + current.conversations,
                entities = (listOf(conversationEntity) + current.entities.filterNot { it.id == conversationId }).take(1_200)
            ).withActivity(if (sourceType.equals("IMPORTED", ignoreCase = true)) "Conversation imported: ${conversation.title}" else if (sourceType.equals("BROWSER_WORKFLOW", ignoreCase = true)) "Browser workflow conversation created: ${conversation.title}" else "Conversation created: ${conversation.title}")
        }
        return conversationId
    }

    fun importConversation(title: String, sourceReference: String, transcript: String): Result<String> {
        val cleanTitle = title.trim().take(240)
        val cleanTranscript = transcript.trim().take(24_000)
        if (_snapshot.value.conversations.size >= 160 || _snapshot.value.entities.size >= 1_200) return Result.failure(IllegalStateException("Conversation or entity limit reached. Export a backup before removing older records."))
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Add a conversation title."))
        if (cleanTranscript.isBlank()) return Result.failure(IllegalArgumentException("Paste the conversation text to import."))
        if (cleanTranscript.length < 8) return Result.failure(IllegalArgumentException("The imported transcript is too short to be useful."))
        val id = newId()
        val now = nowMillis()
        val conversation = ConversationRecord(
            id = id,
            title = cleanTitle,
            sourceType = "IMPORTED",
            sourceReference = sourceReference.trim().take(2_048),
            messages = listOf(ChatMessageRecord(role = "SOURCE", content = cleanTranscript, sourceType = "IMPORTED", createdAt = now)),
            createdAt = now,
            updatedAt = now
        )
        val sourceEntity = EntityRecord(
            id = id, type = "CONVERSATION", title = cleanTitle,
            description = "User-imported transcript. Memo does not claim direct access to the external service.",
            properties = mapOf("sourceType" to "IMPORTED", "sourceReference" to sourceReference.trim().take(2_048)),
            sourceConversationId = id, sourceExcerpt = cleanTranscript.take(1_000), createdAt = now, updatedAt = now
        )
        updateSnapshot { current ->
            current.copy(
                conversations = listOf(conversation) + current.conversations,
                entities = (listOf(sourceEntity) + current.entities.filterNot { it.id == id }).take(1_200)
            ).withActivity("Conversation imported from a user-provided transcript: $cleanTitle")
        }
        return Result.success(id)
    }

    fun appendChatMessage(conversationId: String, role: String, content: String, modelId: String = "", sourceType: String = "NATIVE"): String? {
        val cleanContent = content.trim().take(24_000)
        val normalizedRole = role.trim().uppercase(Locale.ROOT)
        if (cleanContent.isBlank()) return "Message cannot be empty."
        if (normalizedRole !in setOf("USER", "ASSISTANT", "SYSTEM", "SOURCE", "TOOL")) return "Unsupported message role."
        val current = _snapshot.value
        val currentConversation = current.conversations.firstOrNull { it.id == conversationId } ?: return "Conversation no longer exists."
        val messageLimit = if (normalizedRole == "USER") 249 else 250
        if (currentConversation.messages.size >= messageLimit) return "This conversation is at its message limit. Export the full history before continuing in a linked conversation."
        val message = ChatMessageRecord(role = normalizedRole, content = cleanContent, modelId = modelId.take(160), sourceType = sourceType.take(40))
        updateSnapshot { snapshot ->
            val conversations = snapshot.conversations.map { conversation ->
                if (conversation.id != conversationId) conversation else {
                    val title = if (normalizedRole == "USER" && conversation.title == "New conversation") cleanContent.lineSequence().first().trim().take(72).ifBlank { "New conversation" } else conversation.title
                    conversation.copy(
                        title = title,
                        messages = conversation.messages + message,
                        updatedAt = nowMillis()
                    )
                }
            }
            val updatedConversation = conversations.firstOrNull { it.id == conversationId }
            snapshot.copy(
                conversations = conversations,
                entities = snapshot.entities.map { entity ->
                    if (entity.id == conversationId && updatedConversation != null) entity.copy(title = updatedConversation.title, updatedAt = nowMillis()) else entity
                }
            ).let { next ->
                if (normalizedRole == "USER") next.withActivity("Message added to ${updatedConversation?.title ?: "conversation"}") else next
            }
        }
        return null
    }

    fun updateChatMessage(conversationId: String, messageId: String, content: String): String? {
        val cleanContent = content.trim().take(24_000)
        if (cleanContent.isBlank()) return "A message cannot be empty."
        val current = _snapshot.value
        val conversation = current.conversations.firstOrNull { it.id == conversationId } ?: return "Conversation no longer exists."
        val message = conversation.messages.firstOrNull { it.id == messageId } ?: return "Message no longer exists."
        ConversationMutationPolicy.messageMutationBlockReason(message, current.entities)?.let { return it }
        if (message.content == cleanContent) return null
        updateSnapshot { snapshot ->
            snapshot.copy(conversations = snapshot.conversations.map { item ->
                if (item.id != conversationId) item else item.copy(
                    messages = item.messages.map { if (it.id == messageId) it.copy(content = cleanContent) else it },
                    updatedAt = nowMillis()
                )
            }).withActivity("Chat message edited in ${conversation.title}.")
        }
        return null
    }

    fun deleteChatMessage(conversationId: String, messageId: String): String? {
        val current = _snapshot.value
        val conversation = current.conversations.firstOrNull { it.id == conversationId } ?: return "Conversation no longer exists."
        val message = conversation.messages.firstOrNull { it.id == messageId } ?: return "Message no longer exists."
        ConversationMutationPolicy.messageMutationBlockReason(message, current.entities)?.let { reason ->
            return if (reason.contains("cited by")) "This message is cited by a workspace record. Remove or update that record's source link before deleting the message." else reason
        }
        updateSnapshot { snapshot ->
            snapshot.copy(conversations = snapshot.conversations.map { item ->
                if (item.id != conversationId) item else item.copy(
                    messages = item.messages.filterNot { it.id == messageId },
                    updatedAt = nowMillis()
                )
            }).withActivity("Chat message deleted from ${conversation.title}.", level = "WARN")
        }
        return null
    }

    fun deleteConversation(conversationId: String): String? {
        val current = _snapshot.value
        val conversation = current.conversations.firstOrNull { it.id == conversationId } ?: return "Conversation no longer exists."
        val dependencies = ConversationMutationPolicy.conversationDependencyCount(
            conversationId, current.entities, current.jobs, current.relationships
        )
        if (dependencies > 0) {
            return "This conversation has $dependencies linked workspace record(s). Remove or reassign those links first; Memo will not orphan their provenance."
        }
        updateSnapshot { snapshot ->
            snapshot.copy(
                conversations = snapshot.conversations.filterNot { it.id == conversationId },
                entities = snapshot.entities.filterNot { it.id == conversationId && it.type == "CONVERSATION" },
                relationships = snapshot.relationships.filterNot { it.sourceConversationId == conversationId }
            ).withActivity("Conversation deleted: ${conversation.title}", level = "WARN")
        }
        return null
    }

    fun renameConversation(conversationId: String, newTitle: String): String? {
        val title = newTitle.trim().take(240)
        if (title.isBlank()) return "Conversation title cannot be empty."
        if (_snapshot.value.conversations.none { it.id == conversationId }) return "Conversation no longer exists."
        updateSnapshot { current ->
            current.copy(
                conversations = current.conversations.map { if (it.id == conversationId) it.copy(title = title, updatedAt = nowMillis()) else it },
                entities = current.entities.map { if (it.id == conversationId && it.type == "CONVERSATION") it.copy(title = title, updatedAt = nowMillis()) else it }
            ).withActivity("Conversation renamed: $title")
        }
        return null
    }

    fun createRelationship(fromEntityId: String, toEntityId: String, relationType: String, sourceConversationId: String = ""): String? {
        val type = relationType.trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_]+"), "_").trim('_').take(80)
        if (fromEntityId == toEntityId) return "An entity cannot be related to itself."
        if (type.length < 2 || !type.matches(Regex("[A-Z][A-Z0-9_]{1,79}"))) return "Enter a relationship type, such as SUPPORTS or BELONGS_TO."
        val current = _snapshot.value
        val fromEntity = current.entities.firstOrNull { it.id == fromEntityId } ?: return "The source entity no longer exists."
        val toEntity = current.entities.firstOrNull { it.id == toEntityId } ?: return "The target entity no longer exists."
        if (fromEntity.reviewStatus in setOf("REJECTED", "MERGED") || toEntity.reviewStatus in setOf("REJECTED", "MERGED")) return "Archived entities cannot receive new relationships. Open an active entity instead."
        if (sourceConversationId.isNotBlank() && current.conversations.none { it.id == sourceConversationId }) return "The source conversation no longer exists."
        if (current.relationships.any { it.fromEntityId == fromEntityId && it.toEntityId == toEntityId && it.relationType == type && it.sourceConversationId == sourceConversationId }) return "That relationship already exists for this source."
        if (current.relationships.size >= 4_000) return "Relationship limit reached. Export a backup before removing older links."
        val relation = EntityRelationship(fromEntityId = fromEntityId, toEntityId = toEntityId, relationType = type, sourceConversationId = sourceConversationId)
        updateSnapshot { snapshot -> snapshot.copy(relationships = (listOf(relation) + snapshot.relationships).take(4_000)).withActivity("Relationship created: $type") }
        return null
    }

    fun deleteRelationship(relationshipId: String) {
        updateSnapshot { current ->
            if (current.relationships.none { it.id == relationshipId }) current
            else current.copy(relationships = current.relationships.filterNot { it.id == relationshipId }).withActivity("Relationship deleted.")
        }
    }

    fun createEntity(
        type: String,
        title: String,
        description: String,
        properties: Map<String, String> = emptyMap(),
        sourceConversationId: String = "",
        sourceMessageId: String = "",
        sourceJobId: String = "",
        sourceExcerpt: String = "",
        reviewStatus: String = "APPROVED"
    ): String? {
        val normalizedType = type.trim().uppercase(Locale.ROOT).replace(Regex("[^A-Z0-9_]+"), "_").trim('_').take(64)
        val cleanTitle = title.trim().take(240)
        if (!normalizedType.matches(Regex("[A-Z][A-Z0-9_]{1,63}"))) return "Choose a valid entity type."
        if (cleanTitle.isBlank()) return "Give the entity a name."
        if (description.trim().length > 12_000) return "The description is too long."
        val current = _snapshot.value
        if (sourceConversationId.isNotBlank() && current.conversations.none { it.id == sourceConversationId }) return "The source conversation no longer exists."
        if (sourceJobId.isNotBlank()) {
            val sourceJob = current.jobs.firstOrNull { it.id == sourceJobId } ?: return "The source job no longer exists."
            if (sourceConversationId.isNotBlank() && sourceJob.sourceConversationId != sourceConversationId) return "The source job belongs to a different conversation."
        }
        val cleanReviewStatus = reviewStatus.uppercase(Locale.ROOT).takeIf { it in setOf("PROPOSED", "APPROVED") } ?: "APPROVED"
        if (current.entities.size >= 1_200) return "Entity limit reached. Export a backup before removing older entities."
        if (sourceConversationId.isNotBlank() && current.relationships.size >= 4_000) return "Relationship limit reached. Export a backup before removing older links."
        val existing = current.entities.firstOrNull { it.type.equals(normalizedType, true) && it.title.trim().equals(cleanTitle, true) && it.reviewStatus !in setOf("REJECTED", "MERGED") }
        if (existing != null) return "An entity with this type and name already exists: ${existing.title}. Open it to update or link it instead."
        val now = nowMillis()
        val entity = EntityRecord(
            type = normalizedType,
            title = cleanTitle,
            description = description.trim().take(12_000),
            properties = properties.entries.take(40).associate { it.key.trim().take(80) to it.value.trim().take(2_000) },
            reviewStatus = cleanReviewStatus,
            sourceConversationId = sourceConversationId,
            sourceMessageId = sourceMessageId,
            sourceJobId = sourceJobId,
            sourceExcerpt = sourceExcerpt.trim().take(2_000),
            createdAt = now,
            updatedAt = now
        )
        updateSnapshot { snapshot ->
            var relations = snapshot.relationships
            if (sourceConversationId.isNotBlank()) {
                relations = (listOf(EntityRelationship(fromEntityId = sourceConversationId, toEntityId = entity.id, relationType = "PROPOSES", sourceConversationId = sourceConversationId)) + relations).take(4_000)
            }
            snapshot.copy(entities = (listOf(entity) + snapshot.entities).take(1_200), relationships = relations)
                .withActivity("${if (cleanReviewStatus == "PROPOSED") "Entity proposed" else "Entity created"}: ${entity.type} · ${entity.title}")
        }
        return null
    }

    /** Save a reusable browser-backed multi-chat workflow as a proper workspace entity. */
    fun saveBrowserWorkflow(
        existingWorkflowId: String?,
        title: String,
        providerIds: List<String>,
        mode: String
    ): Result<String> {
        if (!_vaultReady.value) return Result.failure(IllegalStateException("Unlock the workspace before saving a workflow."))
        if (_vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable. Export or restore the workspace before continuing."))
        val cleanTitle = title.trim().take(160)
        val validProviders = setOf("chatgpt", "claude", "gemini", "grok", "deepseek", "perplexity", "mistral", "copilot")
        val cleanProviders = providerIds.distinct().filter { it in validProviders }.take(4)
        val cleanMode = mode.uppercase(Locale.ROOT).takeIf { it in setOf("CONSULT_AND_COMPARE", "CRITIQUE_CHAIN") }
            ?: return Result.failure(IllegalArgumentException("Choose a supported workflow mode."))
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("Name this workflow."))
        if (cleanProviders.size < 2 || cleanProviders.size != providerIds.distinct().size) {
            return Result.failure(IllegalArgumentException("Choose two to four supported frontier chat websites."))
        }
        val current = _snapshot.value
        val workflowDescription = if (cleanMode == "CONSULT_AND_COMPARE") {
            "Reusable browser-backed multi-chat workflow. Each selected frontier site receives the original goal independently through the visible browser. Responses are captured with source evidence, locally compared, and may produce source-grounded entity proposals for human review."
        } else {
            "Reusable browser-backed critique workflow. Each selected frontier site receives the original goal and earlier captured responses through the visible browser, then critiques and refines the work. Responses are captured with source evidence and may produce source-grounded entity proposals for human review."
        }
        val properties = mapOf(
            "providerIds" to org.json.JSONArray(cleanProviders).toString(),
            "mode" to cleanMode,
            "runtime" to "runtime.browser",
            "workflowVersion" to "2"
        )
        if (existingWorkflowId != null) {
            val existing = current.entities.firstOrNull { it.id == existingWorkflowId && it.type == "BROWSER_WORKFLOW" }
                ?: return Result.failure(IllegalStateException("The saved workflow no longer exists."))
            if (current.entities.any { it.id != existing.id && it.type == "BROWSER_WORKFLOW" && it.title.equals(cleanTitle, true) && it.reviewStatus !in setOf("REJECTED", "MERGED") }) {
                return Result.failure(IllegalArgumentException("Another saved workflow already uses this name."))
            }
            updateSnapshot { snapshot ->
                snapshot.copy(entities = snapshot.entities.map { entity ->
                    if (entity.id == existing.id) entity.copy(
                        title = cleanTitle,
                        description = workflowDescription,
                        properties = properties,
                        updatedAt = nowMillis()
                    ) else entity
                }).withActivity("Browser workflow updated: $cleanTitle")
            }
            return Result.success(existing.id)
        }
        if (current.entities.any { it.type == "BROWSER_WORKFLOW" && it.title.equals(cleanTitle, true) && it.reviewStatus !in setOf("REJECTED", "MERGED") }) {
            return Result.failure(IllegalArgumentException("A workflow with this name already exists. Load it to update its settings."))
        }
        val error = createEntity(
            type = "BROWSER_WORKFLOW",
            title = cleanTitle,
            description = workflowDescription,
            properties = properties
        )
        if (error != null) return Result.failure(IllegalStateException(error))
        val saved = _snapshot.value.entities.firstOrNull { it.type == "BROWSER_WORKFLOW" && it.title == cleanTitle }
            ?: return Result.failure(IllegalStateException("The workflow did not appear in the workspace after saving."))
        return Result.success(saved.id)
    }

    fun updateEntityDetails(entityId: String, title: String, description: String, properties: Map<String, String>): String? {
        val cleanTitle = title.trim().take(240)
        val cleanDescription = description.trim().take(12_000)
        if (cleanTitle.isBlank()) return "Give the entity a name."
        if (description.length > 12_000) return "The description is too long."
        EntityEditPolicy.propertyValidationError(properties)?.let { return it }
        val cleanProperties = properties.entries.associate { it.key.trim() to it.value.trim().take(2_000) }
        val current = _snapshot.value
        val entity = current.entities.firstOrNull { it.id == entityId } ?: return "Entity no longer exists."
        if (entity.type == "CONVERSATION") return "Rename this entity from AI Chats so the conversation and entity stay synchronized."
        if (entity.reviewStatus in setOf("REJECTED", "MERGED")) return "Archived entity records are read-only so their provenance remains intact."
        if (current.entities.any { it.id != entityId && it.type == entity.type && it.title.trim().equals(cleanTitle, true) && it.reviewStatus !in setOf("REJECTED", "MERGED") }) {
            return "Another ${entity.type.lowercase(Locale.ROOT)} already uses that name. Open it and add a relationship instead."
        }
        updateSnapshot { snapshot ->
            snapshot.copy(entities = snapshot.entities.map { candidate ->
                if (candidate.id == entityId) candidate.copy(title = cleanTitle, description = cleanDescription, properties = cleanProperties, updatedAt = nowMillis()) else candidate
            }).withActivity("Entity updated: ${entity.type} · $cleanTitle")
        }
        return null
    }

    /** Merge keeps the source entity as an archived provenance record and redirects its graph links. */
    fun mergeEntities(sourceEntityId: String, targetEntityId: String): String? {
        if (sourceEntityId == targetEntityId) return "Choose two different entities."
        val current = _snapshot.value
        val source = current.entities.firstOrNull { it.id == sourceEntityId } ?: return "The source entity no longer exists."
        val target = current.entities.firstOrNull { it.id == targetEntityId } ?: return "The target entity no longer exists."
        if (source.type == "CONVERSATION" || target.type == "CONVERSATION") return "Conversation records cannot be merged into other entities."
        if (source.type != target.type) return "Only entities of the same type can be merged safely."
        if (source.reviewStatus != "APPROVED" || target.reviewStatus != "APPROVED") return "Approve both entities before merging them."
        if (current.relationships.size >= 4_000) return "Relationship limit reached. Export a backup before removing older links."
        if (current.relationships.any { it.fromEntityId == sourceEntityId && it.toEntityId == targetEntityId && it.relationType == "MERGED_INTO" }) return "This entity is already merged into that target."

        val redirected = current.relationships.mapNotNull { relation ->
            if (relation.relationType == "MERGED_INTO" && relation.fromEntityId == sourceEntityId) return@mapNotNull null
            val from = if (relation.fromEntityId == sourceEntityId) targetEntityId else relation.fromEntityId
            val to = if (relation.toEntityId == sourceEntityId) targetEntityId else relation.toEntityId
            if (from == to) null else relation.copy(fromEntityId = from, toEntityId = to)
        }
        val deduplicated = redirected.distinctBy { "${it.fromEntityId}|${it.toEntityId}|${it.relationType}|${it.sourceConversationId}" }.toMutableList()
        deduplicated += EntityRelationship(
            fromEntityId = sourceEntityId,
            toEntityId = targetEntityId,
            relationType = "MERGED_INTO",
            sourceConversationId = source.sourceConversationId.ifBlank { target.sourceConversationId }
        )
        if (deduplicated.size > 4_000) return "The merge would exceed the workspace relationship limit. Remove old links and try again."

        val mergedProperties = target.properties.toMutableMap()
        source.properties.forEach { (key, value) ->
            if (mergedProperties[key].isNullOrBlank() && (key in mergedProperties || mergedProperties.size < 40)) mergedProperties[key] = value
        }
        val combinedDescription = listOf(target.description.trim(), source.description.trim())
            .filter { it.isNotBlank() }
            .distinct()
            .joinToString("\n\nMerged context: ")
            .take(12_000)
        updateSnapshot { snapshot ->
            snapshot.copy(
                entities = snapshot.entities.map { entity ->
                    when (entity.id) {
                        targetEntityId -> entity.copy(
                            description = combinedDescription,
                            properties = mergedProperties,
                            updatedAt = nowMillis()
                        )
                        sourceEntityId -> entity.copy(reviewStatus = "MERGED", updatedAt = nowMillis())
                        else -> entity
                    }
                },
                relationships = deduplicated
            ).withActivity("Merged ${source.type} · ${source.title} into ${target.title}; source retained in archive.")
        }
        return null
    }

    fun setEntityReviewStatus(entityId: String, status: String) {
        val normalized = status.uppercase(Locale.ROOT).takeIf { it in setOf("APPROVED", "REJECTED") } ?: return
        updateSnapshot { current ->
            val entity = current.entities.firstOrNull { it.id == entityId } ?: return@updateSnapshot current
            if (entity.reviewStatus != "PROPOSED") return@updateSnapshot current
            val updatedEntities = current.entities.map { if (it.id == entityId) it.copy(reviewStatus = normalized, updatedAt = nowMillis()) else it }
            val relations = if (normalized == "REJECTED") current.relationships.filterNot { it.toEntityId == entityId || it.fromEntityId == entityId } else current.relationships.map { relation ->
                if (relation.toEntityId == entityId && relation.relationType == "PROPOSES") relation.copy(relationType = "PRODUCED") else relation
            }
            var updatedJobs = current.jobs
            val sourceJobId = entity.sourceJobId
            if (sourceJobId.isNotBlank()) {
                val remainingProposals = updatedEntities.any { it.sourceJobId == sourceJobId && it.reviewStatus == "PROPOSED" }
                if (!remainingProposals) {
                    val accepted = updatedEntities.count { it.sourceJobId == sourceJobId && it.reviewStatus == "APPROVED" }
                    val rejected = updatedEntities.count { it.sourceJobId == sourceJobId && it.reviewStatus == "REJECTED" }
                    updatedJobs = updatedJobs.map { job ->
                        if (job.id != sourceJobId || job.status != "WAITING_FOR_USER") job else job.copy(
                            status = "SUCCEEDED",
                            resultSummary = "Human review complete: $accepted proposal(s) approved, $rejected rejected.",
                            steps = job.steps.map { step ->
                                if (step.title.equals("Review proposed entities", true) || step.status == "NEEDS_REVIEW") {
                                    step.copy(status = "COMPLETED", detail = "Human review completed: $accepted approved, $rejected rejected.", updatedAt = nowMillis())
                                } else step
                            },
                            updatedAt = nowMillis()
                        )
                    }
                }
            }
            current.copy(entities = updatedEntities, relationships = relations, jobs = updatedJobs)
                .withActivity("Entity ${normalized.lowercase(Locale.ROOT)}: ${entity.title}")
                .let { next ->
                    val completed = updatedJobs.firstOrNull { it.id == sourceJobId && it.status == "SUCCEEDED" }
                    if (completed != null) next.withActivity("Job completed after entity review: ${completed.title}") else next
                }
        }
    }

    fun deleteEntity(entityId: String): String? {
        val current = _snapshot.value
        val entity = current.entities.firstOrNull { it.id == entityId } ?: return "Entity no longer exists."
        if (entity.type == "CONVERSATION") return "Conversation entities are managed by AI Chats."
        if (entity.reviewStatus in setOf("REJECTED", "MERGED")) return "Archived entity records are retained to preserve provenance."
        if (entity.reviewStatus == "PROPOSED") return "Reject this proposal instead so the job review can be completed."
        if (current.relationships.any { it.fromEntityId == entityId || it.toEntityId == entityId }) {
            return "This entity has relationships. Remove the links or merge the entity before deleting it, so the graph remains consistent."
        }
        updateSnapshot { snapshot ->
            snapshot.copy(entities = snapshot.entities.filterNot { it.id == entityId })
                .withActivity("Entity deleted: ${entity.title}", level = "WARN")
        }
        return if (_snapshot.value.entities.none { it.id == entityId }) null else "Workspace changes could not be saved. Check workspace status before retrying."
    }

    /** Executes a bounded app-runtime workflow and persists its verified workspace result. */
    fun createFollowUpTask(sourceEntityId: String, title: String, description: String): Result<String> {
        if (!_vaultReady.value) return Result.failure(IllegalStateException("Unlock the workspace before creating a follow-up task."))
        if (_vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable. Export or restore the workspace before continuing."))
        val before = _snapshot.value
        val creation = FollowUpTaskFactory.create(before, sourceEntityId, title, description, runtimeRegistry)
        if (creation.isFailure) return Result.failure(creation.exceptionOrNull() ?: IllegalStateException("The follow-up task could not be prepared."))
        val outcome = creation.getOrThrow()
        updateSnapshot { current -> if (current == before) outcome.snapshot else current }
        val committed = _snapshot.value.entities.any { it.id == outcome.taskId } &&
            _snapshot.value.relationships.any { it.toEntityId == outcome.taskId } &&
            _snapshot.value.jobs.any { it.id == outcome.jobId && it.status == "SUCCEEDED" }
        return if (committed) Result.success(outcome.jobId) else Result.failure(IllegalStateException("The workspace changed before this task could be committed. Review Work and retry."))
    }

    /** Turn an unstructured request into a persistent, editable, checkpoint-driven Memo job. This creates a plan only; it never executes external effects. */
    fun createIntentPlan(rawIntent: String, language: String = _uiLanguage.value): Result<String> {
        if (!_vaultReady.value) return Result.failure(IllegalStateException("Unlock the workspace before creating work."))
        if (_vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable. Resolve the workspace issue before creating work."))
        val plan = runCatching { IntentPlanner.plan(rawIntent, language) }
            .getOrElse { return Result.failure(it) }
        val current = _snapshot.value
        if (current.jobs.size >= 240) return Result.failure(IllegalStateException("The 240-job limit has been reached. Export a backup before removing older jobs."))
        val compiledCapabilities = CapabilityCompiler.compile(plan, runtimeRegistry.listRuntimes(), plan.goal)
        val capabilitySummary = compiledCapabilities.capabilities.joinToString(", ") { capability ->
            "${capability.runtimeName}: ${capability.label}" + if (capability.requiresUserApproval) " [approval]" else ""
        }
        val capabilityRecord = "\nCapability manifest v${compiledCapabilities.schemaVersion}: ${compiledCapabilities.manifestSha256}. " +
            "Selected: ${capabilitySummary.ifBlank { "none" }}. " +
            "Unresolved: ${compiledCapabilities.unresolvedRequirements.ifEmpty { listOf("none") }.joinToString(", ")}. " +
            "This manifest describes available components; it does not grant permissions or execute any step."
        val now = nowMillis()
        val job = WorkJobRecord(
            title = plan.title,
            goal = plan.goal + "\n\nMemo route: ${plan.domain.label}. ${plan.rationale}\nExternal approval required: ${plan.requiresExternalApproval}." + capabilityRecord,
            status = "DRAFT",
            runtimeId = "app.jobs",
            steps = plan.steps.map { step ->
                JobStepRecord(
                    title = step.title,
                    detail = step.instruction,
                    status = "PENDING",
                    updatedAt = now,
                    checkpointKind = step.checkpointKind,
                    requiresApproval = step.requiresApproval
                )
            },
            resultSummary = "Plan created locally. No apps were controlled and no external action was executed.",
            createdAt = now,
            updatedAt = now,
            workflowGraphJson = WorkflowGraphCompiler.compile(plan, compiledCapabilities).toJson()
        )
        updateSnapshot { snapshot ->
            snapshot.copy(jobs = (listOf(job) + snapshot.jobs).take(240))
                .withActivity("Intent converted into a ${plan.domain.id} work plan: ${plan.title}")
        }
        val saved = _snapshot.value.jobs.any { it.id == job.id }
        return if (saved) Result.success(job.id) else Result.failure(IllegalStateException("The plan could not be persisted. Check workspace status."))
    }

    /** Revise an untouched draft plan and regenerate its deterministic checkpoints. This never performs side effects. */
    fun reviseIntentPlan(jobId: String, title: String, rawIntent: String): Result<Unit> {
        if (!_vaultReady.value || _vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable."))
        val current = _snapshot.value
        val existing = current.jobs.firstOrNull { it.id == jobId }
            ?: return Result.failure(IllegalArgumentException("This plan no longer exists."))
        if (existing.status != "DRAFT" || existing.steps.any { it.status != "PENDING" }) {
            return Result.failure(IllegalStateException("A plan can only be revised before any checkpoint has started. Create a follow-up plan to change work already in progress."))
        }
        val revised = runCatching { IntentPlanner.plan(rawIntent, _uiLanguage.value) }
            .getOrElse { return Result.failure(it) }
        val cleanTitle = title.trim().take(240).ifBlank { revised.title }
        val compiledCapabilities = CapabilityCompiler.compile(revised, runtimeRegistry.listRuntimes(), revised.goal)
        val capabilitySummary = compiledCapabilities.capabilities.joinToString(", ") { capability ->
            "${capability.runtimeName}: ${capability.label}" + if (capability.requiresUserApproval) " [approval]" else ""
        }
        val capabilityRecord = "\nCapability manifest v${compiledCapabilities.schemaVersion}: ${compiledCapabilities.manifestSha256}. " +
            "Selected: ${capabilitySummary.ifBlank { "none" }}. " +
            "Unresolved: ${compiledCapabilities.unresolvedRequirements.ifEmpty { listOf("none") }.joinToString(", ")}. " +
            "This manifest describes available components; it does not grant permissions or execute any step."
        val now = nowMillis()
        val updated = existing.copy(
            title = cleanTitle,
            goal = revised.goal + "\n\nMemo route: ${revised.domain.label}. ${revised.rationale}\nExternal approval required: ${revised.requiresExternalApproval}." + capabilityRecord,
            steps = revised.steps.map { planned ->
                JobStepRecord(
                    title = planned.title,
                    detail = planned.instruction,
                    status = "PENDING",
                    updatedAt = now,
                    checkpointKind = planned.checkpointKind,
                    requiresApproval = planned.requiresApproval
                )
            },
            resultSummary = "Draft plan revised locally at $now. No app was controlled and no external action was executed.",
            updatedAt = now,
            workflowGraphJson = WorkflowGraphCompiler.compile(revised, compiledCapabilities).toJson()
        )
        updateSnapshot { snapshot ->
            if (snapshot.jobs.none { it.id == jobId && it.status == "DRAFT" && it.steps.all { step -> step.status == "PENDING" } }) snapshot
            else snapshot.copy(jobs = snapshot.jobs.map { if (it.id == jobId) updated else it })
                .withActivity("Draft work plan revised: $cleanTitle", jobId)
        }
        val committed = _snapshot.value.jobs.firstOrNull { it.id == jobId }
        return if (committed?.updatedAt == updated.updatedAt && committed.title == cleanTitle && committed.steps.size == revised.steps.size) Result.success(Unit)
        else Result.failure(IllegalStateException("The plan changed while you were editing it. Reopen the plan and try again."))
    }

    /** Start only the next sequential checkpoint. An external-write checkpoint cannot be started through this generic workbench. */
    fun startJobStep(jobId: String, stepId: String): Result<Unit> {
        if (!_vaultReady.value || _vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable."))
        val current = _snapshot.value
        val job = current.jobs.firstOrNull { it.id == jobId } ?: return Result.failure(IllegalArgumentException("Job no longer exists."))
        val index = job.steps.indexOfFirst { it.id == stepId }
        if (index < 0) return Result.failure(IllegalArgumentException("Checkpoint no longer exists."))
        val step = job.steps[index]
        if (step.requiresApproval || step.checkpointKind == "EXTERNAL_ACTION") {
            return Result.failure(IllegalStateException("This checkpoint requires the dedicated exact-content action approval flow. The work planner cannot execute external actions."))
        }
        if (job.steps.take(index).any { it.status != "COMPLETED" }) return Result.failure(IllegalStateException("Complete earlier checkpoints in order first."))
        if (job.steps.any { it.status == "RUNNING" }) return Result.failure(IllegalStateException("Finish or review the active checkpoint before starting another."))
        if (step.status !in setOf("PENDING", "NEEDS_REVIEW")) return Result.failure(IllegalStateException("This checkpoint is not startable from ${step.status}."))
        updateSnapshot { snapshot ->
            snapshot.copy(jobs = snapshot.jobs.map { candidate ->
                if (candidate.id != jobId) candidate else candidate.copy(
                    status = "RUNNING",
                    steps = candidate.steps.map { candidateStep -> if (candidateStep.id == stepId) candidateStep.copy(status = "RUNNING", detail = candidateStep.detail.take(1_700) + "\n\nStarted by the user at ${nowMillis()}.", updatedAt = nowMillis()) else candidateStep },
                    resultSummary = "A checkpoint is active. Memo is not acting outside this screen; record a real outcome before moving on.",
                    updatedAt = nowMillis()
                )
            }).withActivity("Started checkpoint: ${step.title}", jobId)
        }
        return Result.success(Unit)
    }

    /** Save a user-reported result for a local checkpoint. External-write steps require the real approval/runtime flow and are never completed by this shortcut. */
    fun recordJobCheckpoint(jobId: String, stepId: String, outcome: String): Result<Unit> {
        if (!_vaultReady.value || _vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable."))
        val clean = outcome.trim().take(2_000)
        if (clean.length < 6) return Result.failure(IllegalArgumentException("Record a concrete outcome of at least six characters."))
        val current = _snapshot.value
        val job = current.jobs.firstOrNull { it.id == jobId } ?: return Result.failure(IllegalArgumentException("Job no longer exists."))
        val step = job.steps.firstOrNull { it.id == stepId } ?: return Result.failure(IllegalArgumentException("Checkpoint no longer exists."))
        if (step.requiresApproval || step.checkpointKind == "EXTERNAL_ACTION") {
            return Result.failure(IllegalStateException("External actions cannot be marked complete from a note. Use the destination runtime and its exact-content approval gate, then retain verifiable outcome evidence."))
        }
        if (step.status !in setOf("RUNNING", "NEEDS_REVIEW")) return Result.failure(IllegalStateException("Start this checkpoint or open its review before recording an outcome."))
        val updatedSteps = job.steps.map { candidate ->
            if (candidate.id == stepId) candidate.copy(
                status = "COMPLETED",
                evidence = "USER_REPORTED: $clean".take(2_000),
                detail = candidate.detail.take(1_200) + "\n\nOutcome recorded by user; not independently verified.",
                updatedAt = nowMillis()
            ) else candidate
        }
        val allComplete = updatedSteps.isNotEmpty() && updatedSteps.all { it.status == "COMPLETED" }
        updateSnapshot { snapshot ->
            snapshot.copy(jobs = snapshot.jobs.map { candidate ->
                if (candidate.id != jobId) candidate else candidate.copy(
                    status = if (allComplete) "SUCCEEDED" else "RUNNING",
                    steps = updatedSteps,
                    resultSummary = if (allComplete) "All local checkpoints were marked complete by the user. External side effects, if any, were not independently verified by this plan." else "Checkpoint outcome recorded by user. Continue to the next step in sequence.",
                    updatedAt = nowMillis()
                )
            }).withActivity("User recorded checkpoint outcome: ${step.title}", jobId, if (allComplete) "INFO" else "INFO")
        }
        return Result.success(Unit)
    }

    fun createJob(title: String, goal: String, runtimeId: String, sourceConversationId: String = "", stepTitles: List<String>): String? {
        val cleanTitle = title.trim().take(240)
        val cleanGoal = goal.trim().take(8_000)
        if (cleanTitle.isBlank() || cleanGoal.isBlank() || stepTitles.size > 100) return null
        if (runtimeRegistry.resolve(runtimeId) == null) return null
        if (sourceConversationId.isNotBlank() && _snapshot.value.conversations.none { it.id == sourceConversationId }) return null
        if (_snapshot.value.jobs.size >= 240) { log("The 240-job limit has been reached. Export a backup before removing older jobs.", "WARN"); return null }
        val job = WorkJobRecord(
            title = cleanTitle, goal = cleanGoal, status = "DRAFT", runtimeId = runtimeId.trim().take(80),
            sourceConversationId = sourceConversationId, steps = stepTitles.take(100).map { JobStepRecord(title = it.trim().take(240)) }
        )
        updateSnapshot { current -> current.copy(jobs = (listOf(job) + current.jobs).take(240)).withActivity("Job created: ${job.title}") }
        return job.id
    }

    fun updateJob(jobId: String, transform: (WorkJobRecord) -> WorkJobRecord) {
        updateSnapshot { current ->
            val existing = current.jobs.firstOrNull { it.id == jobId } ?: return@updateSnapshot current
            val updated = transform(existing).copy(updatedAt = nowMillis())
            current.copy(jobs = current.jobs.map { if (it.id == jobId) updated else it })
        }
    }

    fun updateJobStep(jobId: String, stepId: String, status: String, detail: String = "", evidence: String = "") {
        val valid = status.uppercase(Locale.ROOT).takeIf { it in setOf("PENDING", "RUNNING", "COMPLETED", "FAILED", "NEEDS_REVIEW", "SKIPPED") } ?: return
        updateJob(jobId) { job -> job.copy(steps = job.steps.map { step ->
            if (step.id == stepId) step.copy(status = valid, detail = detail.take(2_000), evidence = evidence.take(2_000), updatedAt = nowMillis()) else step
        }) }
    }

    fun updateJobStatus(jobId: String, status: String, resultSummary: String = "") {
        val valid = status.uppercase(Locale.ROOT).takeIf { it in setOf("DRAFT", "READY", "RUNNING", "WAITING_FOR_USER", "SUCCEEDED", "FAILED", "CANCELLED") } ?: return
        updateJob(jobId) { it.copy(status = valid, resultSummary = resultSummary.take(4_000)) }
        log("Job ${valid.lowercase(Locale.ROOT).replace('_', ' ')}: ${_snapshot.value.jobs.firstOrNull { it.id == jobId }?.title ?: "Job"}", if (valid == "FAILED") "ERROR" else "INFO")
    }

    /** Persist an exact external payload before it can be approved or sent. */
    fun proposeExternalAction(
        jobId: String,
        stepId: String,
        runtimeId: String,
        capabilityId: String,
        destination: String,
        title: String,
        payload: String,
        risk: String = "EXTERNAL_WRITE"
    ): String? {
        if (!_vaultReady.value || _vaultError.value != null) return null
        var createdId: String? = null
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.propose(
                current, jobId, stepId, runtimeId, capabilityId, destination, title, payload, risk
            )
            val proposedAction = transition.action
            if (!transition.accepted || proposedAction == null) {
                current.withActivity(transition.message, jobId, "WARN")
            } else {
                createdId = proposedAction.id
                transition.snapshot.withActivity("Action staged for exact-content approval: ${proposedAction.title}", jobId)
            }
        }
        return createdId
    }

    fun approveExternalAction(actionId: String, displayedHash: String): Boolean {
        var accepted = false
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.approve(current, actionId, displayedHash)
            accepted = transition.accepted
            if (transition.snapshot == current) {
                if (!transition.accepted) current.withActivity(transition.message, level = "WARN") else current
            } else transition.snapshot.withActivity(transition.message, transition.action?.jobId.orEmpty(), if (transition.accepted) "INFO" else "WARN")
        }
        return accepted
    }

    /** One-use gate immediately before an external side effect. */
    fun beginExternalAction(actionId: String, runtimeId: String, capabilityId: String, destination: String, payload: String): Boolean {
        var accepted = false
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.beginExecution(current, actionId, runtimeId, capabilityId, destination, payload)
            accepted = transition.accepted
            if (transition.snapshot == current) {
                if (!transition.accepted) current.withActivity(transition.message, level = "WARN") else current
            } else transition.snapshot.withActivity(transition.message, transition.action?.jobId.orEmpty(), if (transition.accepted) "INFO" else "WARN")
        }
        return accepted
    }

    fun rejectExternalAction(actionId: String, reason: String = "Rejected by user."): Boolean {
        var accepted = false
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.reject(current, actionId, reason)
            accepted = transition.accepted
            if (transition.snapshot == current) {
                if (!transition.accepted) current.withActivity(transition.message, level = "WARN") else current
            } else transition.snapshot.withActivity(transition.message, transition.action?.jobId.orEmpty(), if (transition.accepted) "INFO" else "WARN")
        }
        return accepted
    }

    fun completeExternalAction(actionId: String, evidenceRef: String): Boolean {
        var accepted = false
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.completeVerified(current, actionId, evidenceRef)
            accepted = transition.accepted
            if (transition.snapshot == current) {
                if (!transition.accepted) current.withActivity(transition.message, level = "WARN") else current
            } else transition.snapshot.withActivity(transition.message, transition.action?.jobId.orEmpty(), if (transition.accepted) "INFO" else "WARN")
        }
        return accepted
    }

    fun markExternalActionNeedsReview(actionId: String, reason: String): Boolean {
        var accepted = false
        updateSnapshot { current ->
            val transition = ActionApprovalLedger.markNeedsReview(current, actionId, reason)
            accepted = transition.accepted
            if (transition.snapshot == current) {
                if (!transition.accepted) current.withActivity(transition.message, level = "WARN") else current
            } else transition.snapshot.withActivity(transition.message, transition.action?.jobId.orEmpty(), if (transition.accepted) "WARN" else "WARN")
        }
        return accepted
    }

    fun createTask(title: String, rawUrl: String, kind: TaskKind, query: String): String? {
        val validation = TaskRules.validate(title, rawUrl, kind, query)
        if (validation != null) return validation
        val normalizedUrl = runCatching { TaskRules.normalizeHttpsUrl(rawUrl) }.getOrElse { return it.message ?: "Invalid URL." }
        val task = TaskRecord(
            title = title.trim(),
            targetUrl = normalizedUrl,
            kind = kind,
            query = query.trim(),
            status = TaskStatus.QUEUED,
            lastMessage = if (normalizedUrl.isBlank()) "Will use the current browser page." else "Ready to open the target page."
        )
        updateSnapshot { current ->
            current.copy(tasks = (listOf(task) + current.tasks).take(200))
                .withActivity("Task created: ${task.title}", task.id)
        }
        return null
    }

    fun markTaskRunning(id: String, message: String = "Waiting for the page. Sign in if needed, then continue.") {
        updateSnapshot { current ->
            current.updateTask(id) { it.copy(status = TaskStatus.RUNNING, lastMessage = message, updatedAt = nowMillis()) }
                .withActivity("Task started: ${current.tasks.firstOrNull { it.id == id }?.title ?: "Task"}", id)
        }
    }

    fun markTaskNeedsReview(id: String, message: String) {
        updateSnapshot { current ->
            current.updateTask(id) { it.copy(status = TaskStatus.NEEDS_REVIEW, lastMessage = message, updatedAt = nowMillis()) }
                .withActivity(message, id)
        }
    }

    fun completeTask(id: String, evidence: EvidenceRecord?, message: String = "Completed and verified.") {
        updateSnapshot { current ->
            var next = current.updateTask(id) { it.copy(status = TaskStatus.COMPLETED, lastMessage = message, updatedAt = nowMillis()) }
            if (evidence != null) next = next.copy(evidence = (listOf(evidence) + next.evidence).take(150))
            next.withActivity("Task completed: $message", id)
        }
    }

    fun failTask(id: String, message: String, blocked: Boolean = false) {
        updateSnapshot { current ->
            current.updateTask(id) {
                it.copy(status = if (blocked) TaskStatus.BLOCKED else TaskStatus.FAILED, lastMessage = message, updatedAt = nowMillis())
            }.withActivity("Task ${if (blocked) "blocked" else "failed"}: $message", id, if (blocked) "WARN" else "ERROR")
        }
    }

    fun addEvidence(evidence: EvidenceRecord) {
        updateSnapshot { current ->
            if (current.evidence.size >= 150) {
                current.withActivity("Evidence limit reached. Export a workspace backup and delete older evidence before capturing more.", evidence.taskId, "WARN")
            } else {
                current.copy(evidence = listOf(evidence) + current.evidence).withActivity("Evidence captured: ${evidence.title}", evidence.taskId)
            }
        }
    }

    /** Edit a generated work artifact while keeping its identity and a small, inspectable revision history. Source captures stay immutable. */
    fun updateWorkArtifact(id: String, title: String, content: String): Result<Unit> {
        if (!_vaultReady.value || _vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable."))
        val cleanTitle = title.trim().take(240)
        val cleanContent = content.trim().take(24_000)
        if (cleanTitle.isBlank()) return Result.failure(IllegalArgumentException("An artifact needs a title."))
        if (cleanContent.length < 2) return Result.failure(IllegalArgumentException("The artifact cannot be empty."))
        val current = _snapshot.value
        val existing = current.evidence.firstOrNull { it.id == id }
            ?: return Result.failure(IllegalArgumentException("This artifact no longer exists."))
        if (existing.kind !in setOf("WORK_ARTIFACT", "ARTIFACT_DRAFT", "SELF_MODEL_ARTIFACT")) {
            return Result.failure(IllegalStateException("Captured source evidence is immutable. Only Memo-created artifacts can be edited."))
        }
        if (existing.content == cleanContent && existing.title == cleanTitle) return Result.success(Unit)
        val metadata = runCatching { org.json.JSONObject(existing.metadata) }.getOrDefault(org.json.JSONObject())
        val history = metadata.optJSONArray("revisionHistory") ?: org.json.JSONArray()
        history.put(org.json.JSONObject().put("title", existing.title).put("content", existing.content.take(24_000)).put("savedAt", nowMillis()))
        while (history.length() > 5) history.remove(0)
        metadata.put("revisionHistory", history)
        metadata.put("version", metadata.optInt("version", 1) + 1)
        metadata.put("lastEditedAt", nowMillis())
        metadata.put("editedBy", "user")
        metadata.put("requiresHumanReview", true)
        val updated = existing.copy(
            title = cleanTitle,
            content = cleanContent,
            summary = cleanContent.lineSequence().firstOrNull().orEmpty().take(1_000),
            metadata = metadata.toString()
        )
        updateSnapshot { latest ->
            if (latest.evidence.none { it.id == id }) latest
            else latest.copy(evidence = latest.evidence.map { if (it.id == id) updated else it })
                .withActivity("Work artifact revised: ${cleanTitle}")
        }
        return if (_snapshot.value.evidence.any { it.id == id && it.content == cleanContent && it.title == cleanTitle }) Result.success(Unit)
        else Result.failure(IllegalStateException("The artifact revision could not be committed."))
    }

    /** Strict artifact write for job execution. Unlike legacy capture callbacks, it never silently evicts older evidence. */
    fun saveWorkArtifact(evidence: EvidenceRecord): Result<Unit> {
        if (!_vaultReady.value || _vaultError.value != null) return Result.failure(IllegalStateException("Workspace persistence is unavailable."))
        val current = _snapshot.value
        if (current.evidence.size >= 150) return Result.failure(IllegalStateException("Evidence storage is full. Export a workspace backup and remove older evidence before creating another artifact."))
        if (current.evidence.any { it.id == evidence.id }) return Result.failure(IllegalStateException("An artifact with this identity already exists."))
        updateSnapshot { snapshot ->
            if (snapshot.evidence.size >= 150 || snapshot.evidence.any { it.id == evidence.id }) snapshot
            else snapshot.copy(evidence = listOf(evidence) + snapshot.evidence).withActivity("Work artifact saved: ${evidence.title}", evidence.taskId)
        }
        return if (_snapshot.value.evidence.any { it.id == evidence.id }) Result.success(Unit)
        else Result.failure(IllegalStateException("The artifact could not be committed to the workspace."))
    }

    fun addCouncilRun(run: CouncilRunRecord) {
        updateSnapshot { current ->
            current.copy(councilRuns = (listOf(run) + current.councilRuns.filterNot { it.id == run.id }).take(60))
                .withActivity("Council completed: ${run.pattern.lowercase().replace('_', ' ')} · ${run.turns.count { it.status == "COMPLETED" }} completed turn(s)")
        }
    }

    fun updateCouncilDecision(id: String, decision: String) {
        updateSnapshot { current ->
            current.copy(councilRuns = current.councilRuns.map { run ->
                if (run.id == id) run.copy(finalDecision = decision.trim().take(4_000)) else run
            }).withActivity("Final decision updated for Council run ${id.take(8)}.")
        }
    }

    fun deleteCouncilRun(id: String) {
        updateSnapshot { current -> current.copy(councilRuns = current.councilRuns.filterNot { it.id == id }).withActivity("Council run removed from history.") }
    }

    fun deleteEvidence(id: String) {
        updateSnapshot { current -> current.copy(evidence = current.evidence.filterNot { it.id == id }).withActivity("Evidence item deleted.") }
    }

    fun deleteTask(id: String) {
        updateSnapshot { current -> current.copy(tasks = current.tasks.filterNot { it.id == id }).withActivity("Task deleted.", id) }
    }

    fun log(message: String, level: String = "INFO", taskId: String = "") {
        updateSnapshot { it.withActivity(message, taskId, level) }
    }

    fun exportTo(uri: Uri, resolver: ContentResolver, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val content = _snapshot.value.copy(themeMode = _themeMode.value).toJson()
                    require(content.toByteArray(Charsets.UTF_8).size <= MAX_WORKSPACE_BACKUP_BYTES) { "Export is larger than 40 MB." }
                    resolver.openOutputStream(uri, "wt")?.use { output -> output.write(content.toByteArray(Charsets.UTF_8)) }
                        ?: error("Could not open the selected destination.")
                }.exceptionOrNull()
            }
            onDone(outcome?.let { safeError(it, "Export failed.") })
            if (outcome == null) log("Workspace exported to a user-selected JSON file.")
        }
    }

    fun exportEncryptedTo(uri: Uri, resolver: ContentResolver, passphrase: CharArray, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val outcome = withContext(Dispatchers.IO) {
                runCatching {
                    val plain = _snapshot.value.copy(themeMode = _themeMode.value).toJson().toByteArray(Charsets.UTF_8)
                    require(plain.size <= MAX_WORKSPACE_BACKUP_BYTES) { "Workspace backup is larger than 40 MB." }
                    val encrypted = try { EncryptedBackupCodec.encrypt(plain, passphrase) } finally { plain.fill(0) }
                    try {
                        require(encrypted.size <= MAX_WORKSPACE_BACKUP_BYTES + 1_024) { "Encrypted backup exceeded the permitted size." }
                        resolver.openOutputStream(uri, "wt")?.use { output -> output.write(encrypted) }
                            ?: error("Could not open the selected destination.")
                    } finally {
                        encrypted.fill(0)
                    }
                }.exceptionOrNull()
            }
            passphrase.fill('\u0000')
            onDone(outcome?.let { safeError(it, "Encrypted export failed.") })
            if (outcome == null) log("Workspace exported as a passphrase-encrypted backup.")
        }
    }

    fun importEncryptedFrom(uri: Uri, resolver: ContentResolver, passphrase: CharArray, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val encrypted = resolver.openInputStream(uri)?.use { input ->
                        val output = java.io.ByteArrayOutputStream()
                        val chunk = ByteArray(8_192)
                        var total = 0
                        while (true) {
                            val count = input.read(chunk)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_WORKSPACE_BACKUP_BYTES + 1_024) { "Encrypted backup is larger than 40 MB." }
                            output.write(chunk, 0, count)
                        }
                        output.toByteArray()
                    } ?: error("Could not read the encrypted backup file.")
                    val plaintext = try { EncryptedBackupCodec.decrypt(encrypted, passphrase) } finally { encrypted.fill(0) }
                    try {
                        val text = plaintext.toString(Charsets.UTF_8)
                        require(text.trimStart().startsWith("{")) { "Decrypted content is not a Memo workspace backup." }
                        ActionApprovalLedger.invalidateImportedApprovals(AppSnapshot.fromJson(text)).recoverInterruptedTasks()
                    } finally {
                        plaintext.fill(0)
                    }
                }
            }
            passphrase.fill('\u0000')
            result.fold(
                onSuccess = { imported ->
                    _snapshot.value = imported
                    setThemeMode(imported.themeMode)
                    _vaultError.value = null
                    _vaultReady.value = true
                    persist(imported)
                    log("Workspace restored from an authenticated encrypted backup.")
                    onDone(null)
                },
                onFailure = { onDone(safeError(it, "Encrypted restore failed.")) }
            )
        }
    }

    fun importFrom(uri: Uri, resolver: ContentResolver, onDone: (String?) -> Unit) {
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) {
                runCatching {
                    val text = resolver.openInputStream(uri)?.use { stream ->
                        val buffer = java.io.ByteArrayOutputStream()
                        val chunk = ByteArray(8_192)
                        var total = 0
                        while (true) {
                            val count = stream.read(chunk)
                            if (count < 0) break
                            total += count
                            require(total <= MAX_WORKSPACE_BACKUP_BYTES) { "Import file is larger than 40 MB." }
                            buffer.write(chunk, 0, count)
                        }
                        buffer.toByteArray().toString(Charsets.UTF_8)
                    } ?: error("Could not read the selected file.")
                    ActionApprovalLedger.invalidateImportedApprovals(AppSnapshot.fromJson(text)).recoverInterruptedTasks()
                }
            }
            result.fold(
                onSuccess = { imported ->
                    _snapshot.value = imported
                    setThemeMode(imported.themeMode)
                    _vaultError.value = null
                    _vaultReady.value = true
                    persist(imported)
                    log("Workspace restored from a JSON export.")
                    onDone(null)
                },
                onFailure = { onDone(safeError(it, "Import failed.")) }
            )
        }
    }

    private fun updateSnapshot(transform: (AppSnapshot) -> AppSnapshot) {
        if (!_vaultReady.value || _vaultError.value != null) return
        val next = transform(_snapshot.value)
        _snapshot.value = next
        persist(next)
    }

    private fun persist(snapshot: AppSnapshot) {
        persistenceQueue.trySend(snapshot)
    }

    private fun AppSnapshot.withActivity(message: String, taskId: String = "", level: String = "INFO"): AppSnapshot =
        copy(activity = (listOf(ActivityRecord(message = message, taskId = taskId, level = level)) + activity).take(500))

    private fun AppSnapshot.updateTask(id: String, transform: (TaskRecord) -> TaskRecord): AppSnapshot =
        copy(tasks = tasks.map { task -> if (task.id == id) transform(task) else task })

    private fun safeError(error: Throwable, fallback: String): String {
        val message = error.message?.trim().orEmpty()
        return (message.takeIf { it.isNotBlank() } ?: fallback).take(220)
    }
}
