package app.memo.android.data

import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID

private const val MAX_TASKS = 200
private const val MAX_EVIDENCE = 150
private const val MAX_ACTIVITY = 500
private const val MAX_COUNCIL_RUNS = 60
private const val MAX_COUNCIL_TURNS = 12
private const val MAX_CONVERSATIONS = 160
private const val MAX_MESSAGES_PER_CONVERSATION = 250
private const val MAX_ENTITIES = 1_200
private const val MAX_RELATIONSHIPS = 4_000
private const val MAX_JOBS = 240
private const val MAX_JOB_STEPS = 100
private const val MAX_ACTION_APPROVALS = ActionApprovalLedger.MAX_ACTIONS
private const val MAX_ACTION_AUDIT_EVENTS = ActionApprovalLedger.MAX_EVENTS
private const val MAX_CONTENT_CHARS = 15_000
const val MAX_WORKSPACE_BACKUP_BYTES = 40 * 1024 * 1024
private const val MAX_SERIALIZED_BYTES = MAX_WORKSPACE_BACKUP_BYTES
private const val MAX_URL_CHARS = 2_048

fun newId(): String = UUID.randomUUID().toString()

fun nowMillis(): Long = System.currentTimeMillis()

enum class TaskKind(val label: String, val description: String) {
    CAPTURE_PAGE("Capture page", "Save readable page content and source metadata."),
    EXTRACT_LINKS("Extract links", "Collect visible links and their labels."),
    FIND_TEXT("Find text", "Find a phrase and capture its surrounding context."),
    SCROLL_CAPTURE("Review feed", "Scroll through a feed and collect visible article cards."),
    PREPARE_PROMPT("Prepare prompt", "Fill the current AI chat composer. Sending remains a separate action.");

    companion object {
        fun from(value: String?): TaskKind = entries.firstOrNull { it.name == value } ?: CAPTURE_PAGE
    }
}

enum class TaskStatus(val label: String) {
    QUEUED("Queued"),
    RUNNING("Running"),
    NEEDS_REVIEW("Needs review"),
    COMPLETED("Completed"),
    BLOCKED("Blocked"),
    FAILED("Failed");

    companion object {
        fun from(value: String?): TaskStatus = entries.firstOrNull { it.name == value } ?: QUEUED
    }
}

data class TaskRecord(
    val id: String = newId(),
    val title: String,
    val targetUrl: String = "",
    val kind: TaskKind = TaskKind.CAPTURE_PAGE,
    val query: String = "",
    val status: TaskStatus = TaskStatus.QUEUED,
    val lastMessage: String = "Ready to run",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

data class EvidenceRecord(
    val id: String = newId(),
    val taskId: String = "",
    val title: String,
    val sourceUrl: String,
    val kind: String,
    val content: String,
    val summary: String,
    val capturedAt: Long = nowMillis(),
    val metadata: String = "{}"
)

data class ActivityRecord(
    val id: String = newId(),
    val at: Long = nowMillis(),
    val level: String = "INFO",
    val message: String,
    val taskId: String = ""
)

data class CouncilSourceRef(
    val evidenceId: String,
    val title: String,
    val sourceUrl: String = ""
)

data class CouncilTurnRecord(
    val id: String = newId(),
    val modelId: String,
    val modelName: String,
    val role: String,
    val requestSummary: String,
    val response: String = "",
    val status: String = "COMPLETED",
    val durationMs: Long = 0,
    val error: String = ""
)

data class CouncilRunRecord(
    val id: String = newId(),
    val prompt: String,
    val pattern: String,
    val turns: List<CouncilTurnRecord>,
    val synthesis: String,
    val finalDecision: String = "",
    val sources: List<CouncilSourceRef> = emptyList(),
    val cloudPermissionGranted: Boolean = false,
    val createdAt: Long = nowMillis(),
    val durationMs: Long = 0
)

data class ChatMessageRecord(
    val id: String = newId(),
    val role: String,
    val content: String,
    val modelId: String = "",
    val sourceType: String = "NATIVE",
    val createdAt: Long = nowMillis()
)

data class ConversationRecord(
    val id: String = newId(),
    val title: String,
    val providerId: String = "",
    val sourceType: String = "NATIVE",
    val sourceReference: String = "",
    val messages: List<ChatMessageRecord> = emptyList(),
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

data class EntityRecord(
    val id: String = newId(),
    val type: String,
    val title: String,
    val description: String = "",
    val properties: Map<String, String> = emptyMap(),
    val reviewStatus: String = "APPROVED",
    val sourceConversationId: String = "",
    val sourceMessageId: String = "",
    val sourceJobId: String = "",
    val sourceExcerpt: String = "",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis()
)

data class EntityRelationship(
    val id: String = newId(),
    val fromEntityId: String,
    val toEntityId: String,
    val relationType: String,
    val sourceConversationId: String = "",
    val createdAt: Long = nowMillis()
)

data class JobStepRecord(
    val id: String = newId(),
    val title: String,
    val detail: String = "",
    val status: String = "PENDING",
    val evidence: String = "",
    val updatedAt: Long = nowMillis(),
    val checkpointKind: String = "USER_CHECKPOINT",
    val requiresApproval: Boolean = false
)

data class WorkJobRecord(
    val id: String = newId(),
    val title: String,
    val goal: String,
    val status: String = "DRAFT",
    val runtimeId: String = "",
    val sourceConversationId: String = "",
    val steps: List<JobStepRecord> = emptyList(),
    val resultSummary: String = "",
    val createdAt: Long = nowMillis(),
    val updatedAt: Long = nowMillis(),
    val workflowGraphJson: String = ""
)

data class AppSnapshot(
    val tasks: List<TaskRecord> = emptyList(),
    val evidence: List<EvidenceRecord> = emptyList(),
    val activity: List<ActivityRecord> = emptyList(),
    val themeMode: String = "SYSTEM",
    val councilRuns: List<CouncilRunRecord> = emptyList(),
    val conversations: List<ConversationRecord> = emptyList(),
    val entities: List<EntityRecord> = emptyList(),
    val relationships: List<EntityRelationship> = emptyList(),
    val jobs: List<WorkJobRecord> = emptyList(),
    val actionApprovals: List<ActionApprovalRecord> = emptyList(),
    val actionAuditEvents: List<ActionAuditEvent> = emptyList()
) {
    /** A persisted RUNNING task has lost its WebView execution context after process death. */
    fun recoverInterruptedTasks(): AppSnapshot {
        val interrupted = tasks.count { it.status == TaskStatus.RUNNING }
        val interruptedJobs = jobs.count { it.status == "RUNNING" }
        val actionCandidates = actionApprovals.count { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS") }
        if (interrupted == 0 && interruptedJobs == 0 && actionCandidates == 0) return this
        val recovered = copy(
            tasks = tasks.map { task ->
                if (task.status == TaskStatus.RUNNING) task.copy(
                    status = TaskStatus.NEEDS_REVIEW,
                    lastMessage = "Memo was closed while this task was active. Review the page and run it again.",
                    updatedAt = nowMillis()
                ) else task
            },
            jobs = jobs.map { job ->
                if (job.status == "RUNNING") job.copy(
                    status = "WAITING_FOR_USER",
                    resultSummary = "Memo restarted during this job. Review the last step before resuming.",
                    steps = job.steps.map { step -> if (step.status == "RUNNING") step.copy(status = "NEEDS_REVIEW", detail = "Interrupted by application restart.", updatedAt = nowMillis()) else step },
                    updatedAt = nowMillis()
                ) else job
            },
            activity = (listOf(ActivityRecord(
                message = "Recovered ${interrupted} interrupted ${if (interrupted == 1) "task" else "tasks"}, ${interruptedJobs} interrupted ${if (interruptedJobs == 1) "job" else "jobs"}, and invalidated or flagged ${actionCandidates} pending external ${if (actionCandidates == 1) "action" else "actions"}. Review is required before resuming.",
                level = "WARN"
            )) + activity).take(MAX_ACTIVITY)
        )
        return ActionApprovalLedger.recoverAfterRestart(recovered)
    }

    /** Serialize the complete bounded workspace; refuse oversized snapshots instead of silently dropping user data. */
    fun toJson(): String {
        require(conversations.size <= MAX_CONVERSATIONS) { "Workspace contains too many conversations for a safe backup." }
        require(conversations.all { it.messages.size <= MAX_MESSAGES_PER_CONVERSATION }) { "A conversation exceeds the 250-message backup limit." }
        require(entities.size <= MAX_ENTITIES) { "Workspace contains too many entities for a safe backup." }
        require(relationships.size <= MAX_RELATIONSHIPS) { "Workspace contains too many relationships for a safe backup." }
        require(jobs.size <= MAX_JOBS && jobs.all { it.steps.size <= MAX_JOB_STEPS && it.workflowGraphJson.length <= 64_000 }) { "Workspace contains too many jobs, job steps or workflow graph data for a safe backup." }
        jobs.forEach { job -> if (job.workflowGraphJson.isNotBlank()) app.memo.android.orchestration.WorkflowGraph.fromJson(job.workflowGraphJson) }
        require(actionApprovals.size <= MAX_ACTION_APPROVALS) { "Workspace contains too many action approval records for a safe backup." }
        require(actionAuditEvents.size <= MAX_ACTION_AUDIT_EVENTS) { "Workspace contains too many audit events for a safe backup." }
        require(ActionApprovalLedger.verifyLedger(this)) { "The action ledger or audit chain failed verification; export is blocked to preserve evidence of tampering." }
        // Estimate escaped JSON size before constructing the full object tree. Every UTF-8 code point costs
        // at most four bytes; control characters may expand to six ASCII bytes when JSON-escaped.
        val estimatedBytes = estimateSerializedBytes()
        require(estimatedBytes <= MAX_SERIALIZED_BYTES) {
            "Workspace content is too large to serialize safely. Export and remove older messages or evidence before continuing."
        }
        val serialized = buildJson(
            contentLimit = MAX_CONTENT_CHARS,
            summaryLimit = 2_000,
            metadataLimit = 4_000,
            queryLimit = 8_000,
            activityMessageLimit = 1_000,
            taskLimit = MAX_TASKS,
            evidenceLimit = MAX_EVIDENCE,
            activityLimit = MAX_ACTIVITY,
            councilRunLimit = MAX_COUNCIL_RUNS
        )
        require(serialized.toByteArray(Charsets.UTF_8).size <= MAX_SERIALIZED_BYTES) {
            "Workspace backup exceeds 40 MB. Export or delete older evidence, then try again."
        }
        return serialized
    }

    private fun estimateSerializedBytes(): Long {
        var total = 4_096L // Root object, keys and record delimiters.
        fun add(value: String, fieldOverhead: Int = 8) {
            total += fieldOverhead.toLong()
            var index = 0
            while (index < value.length) {
                val char = value[index]
                total += when {
                    char == '"' || char == '\\' || char.code < 0x20 -> 6L
                    char.code <= 0x7F -> 1L
                    char.code <= 0x7FF -> 2L
                    Character.isHighSurrogate(char) && index + 1 < value.length && Character.isLowSurrogate(value[index + 1]) -> {
                        index++
                        4L
                    }
                    Character.isSurrogate(char) -> 3L
                    else -> 3L
                }
                index++
            }
        }
        tasks.forEach { add(it.id); add(it.title); add(it.targetUrl); add(it.query); add(it.lastMessage) }
        evidence.forEach { add(it.id); add(it.taskId); add(it.title); add(it.sourceUrl); add(it.kind); add(it.content); add(it.summary); add(it.metadata) }
        activity.forEach { add(it.id); add(it.level); add(it.message); add(it.taskId) }
        councilRuns.forEach { run ->
            add(run.id); add(run.prompt); add(run.pattern); add(run.synthesis); add(run.finalDecision)
            run.turns.forEach { add(it.id); add(it.modelId); add(it.modelName); add(it.role); add(it.requestSummary); add(it.response); add(it.status); add(it.error) }
            run.sources.forEach { add(it.evidenceId); add(it.title); add(it.sourceUrl) }
        }
        conversations.forEach { conversation ->
            add(conversation.id); add(conversation.title); add(conversation.providerId); add(conversation.sourceType); add(conversation.sourceReference)
            conversation.messages.forEach { add(it.id); add(it.role); add(it.content); add(it.modelId); add(it.sourceType) }
        }
        entities.forEach { entity ->
            add(entity.id); add(entity.type); add(entity.title); add(entity.description); add(entity.reviewStatus)
            add(entity.sourceConversationId); add(entity.sourceMessageId); add(entity.sourceJobId); add(entity.sourceExcerpt)
            entity.properties.forEach { (key, value) -> add(key); add(value) }
        }
        relationships.forEach { add(it.id); add(it.fromEntityId); add(it.toEntityId); add(it.relationType); add(it.sourceConversationId) }
        jobs.forEach { job ->
            add(job.id); add(job.title); add(job.goal); add(job.status); add(job.runtimeId); add(job.sourceConversationId); add(job.resultSummary); add(job.workflowGraphJson)
            job.steps.forEach { add(it.id); add(it.title); add(it.detail); add(it.status); add(it.evidence) }
        }
        actionApprovals.forEach { action ->
            add(action.id); add(action.jobId); add(action.stepId); add(action.runtimeId); add(action.capabilityId); add(action.destination)
            add(action.title); add(action.payload); add(action.contentHash); add(action.risk); add(action.status); add(action.approvalHash); add(action.evidenceRef); add(action.resolution)
        }
        actionAuditEvents.forEach { event ->
            add(event.id); add(event.sequence.toString()); add(event.at.toString()); add(event.actor); add(event.eventType)
            add(event.actionId); add(event.runtimeId); add(event.destination); add(event.contentHash); add(event.status)
            add(event.previousHash); add(event.eventHash); add(event.details)
        }
        return total
    }

    private fun buildJson(
        contentLimit: Int,
        summaryLimit: Int,
        metadataLimit: Int,
        queryLimit: Int,
        activityMessageLimit: Int,
        taskLimit: Int,
        evidenceLimit: Int,
        activityLimit: Int,
        councilRunLimit: Int
    ): String {
        val root = JSONObject()
        root.put("schemaVersion", 6)
        root.put("schemaSemver", "6.0.0")
        root.put("minimumReaderVersion", 6)
        root.put("exportedAt", nowMillis())
        root.put("themeMode", themeMode)
        root.put("tasks", JSONArray().apply {
            tasks.take(taskLimit).forEach { task ->
                put(JSONObject().apply {
                    put("id", task.id.take(128))
                    put("title", task.title.take(160))
                    put("targetUrl", task.targetUrl.take(MAX_URL_CHARS))
                    put("kind", task.kind.name)
                    put("query", task.query.take(queryLimit))
                    put("status", task.status.name)
                    put("lastMessage", task.lastMessage.take(activityMessageLimit))
                    put("createdAt", task.createdAt)
                    put("updatedAt", task.updatedAt)
                })
            }
        })
        root.put("evidence", JSONArray().apply {
            evidence.take(evidenceLimit).forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id.take(128))
                    put("taskId", item.taskId.take(128))
                    put("title", item.title.take(240))
                    put("sourceUrl", item.sourceUrl.take(MAX_URL_CHARS))
                    put("kind", item.kind.take(80))
                    put("content", item.content.take(contentLimit))
                    put("summary", item.summary.take(summaryLimit))
                    put("capturedAt", item.capturedAt)
                    put("metadata", item.metadata.take(metadataLimit))
                })
            }
        })
        root.put("activity", JSONArray().apply {
            activity.take(activityLimit).forEach { item ->
                put(JSONObject().apply {
                    put("id", item.id.take(128))
                    put("at", item.at)
                    put("level", item.level.take(16))
                    put("message", item.message.take(activityMessageLimit))
                    put("taskId", item.taskId.take(128))
                })
            }
        })
        root.put("councilRuns", JSONArray().apply {
            councilRuns.take(councilRunLimit).forEach { run ->
                put(JSONObject().apply {
                    put("id", run.id.take(128))
                    put("prompt", run.prompt.take(8_000))
                    put("pattern", run.pattern.take(40))
                    put("synthesis", run.synthesis.take(24_000))
                    put("finalDecision", run.finalDecision.take(4_000))
                    put("cloudPermissionGranted", run.cloudPermissionGranted)
                    put("createdAt", run.createdAt)
                    put("durationMs", run.durationMs.coerceAtLeast(0))
                    put("turns", JSONArray().apply {
                        run.turns.take(MAX_COUNCIL_TURNS).forEach { turn ->
                            put(JSONObject().apply {
                                put("id", turn.id.take(128))
                                put("modelId", turn.modelId.take(160))
                                put("modelName", turn.modelName.take(200))
                                put("role", turn.role.take(80))
                                put("requestSummary", turn.requestSummary.take(16_000))
                                put("response", turn.response.take(24_000))
                                put("status", turn.status.take(24))
                                put("durationMs", turn.durationMs.coerceAtLeast(0))
                                put("error", turn.error.take(400))
                            })
                        }
                    })
                    put("sources", JSONArray().apply {
                        run.sources.take(20).forEach { source ->
                            put(JSONObject().apply {
                                put("evidenceId", source.evidenceId.take(128))
                                put("title", source.title.take(240))
                                put("sourceUrl", source.sourceUrl.take(MAX_URL_CHARS))
                            })
                        }
                    })
                })
            }
        })
        root.put("conversations", JSONArray().apply {
            conversations.forEach { conversation ->
                put(JSONObject().apply {
                    put("id", conversation.id.take(128))
                    put("title", conversation.title.take(240))
                    put("providerId", conversation.providerId.take(160))
                    put("sourceType", conversation.sourceType.take(40))
                    put("sourceReference", conversation.sourceReference.take(2_048))
                    put("createdAt", conversation.createdAt)
                    put("updatedAt", conversation.updatedAt)
                    put("messages", JSONArray().apply {
                        conversation.messages.forEach { message ->
                            put(JSONObject().apply {
                                put("id", message.id.take(128))
                                put("role", message.role.take(24))
                                put("content", message.content.take(24_000))
                                put("modelId", message.modelId.take(160))
                                put("sourceType", message.sourceType.take(40))
                                put("createdAt", message.createdAt)
                            })
                        }
                    })
                })
            }
        })
        root.put("entities", JSONArray().apply {
            entities.forEach { entity ->
                put(JSONObject().apply {
                    put("id", entity.id.take(128))
                    put("type", entity.type.take(64))
                    put("title", entity.title.take(240))
                    put("description", entity.description.take(12_000))
                    put("reviewStatus", entity.reviewStatus.take(24))
                    put("sourceConversationId", entity.sourceConversationId.take(128))
                    put("sourceMessageId", entity.sourceMessageId.take(128))
                    put("sourceJobId", entity.sourceJobId.take(128))
                    put("sourceExcerpt", entity.sourceExcerpt.take(2_000))
                    put("createdAt", entity.createdAt)
                    put("updatedAt", entity.updatedAt)
                    put("properties", JSONObject().apply {
                        entity.properties.entries.take(40).forEach { (key, value) -> put(key.take(80), value.take(2_000)) }
                    })
                })
            }
        })
        root.put("relationships", JSONArray().apply {
            relationships.forEach { relation ->
                put(JSONObject().apply {
                    put("id", relation.id.take(128))
                    put("fromEntityId", relation.fromEntityId.take(128))
                    put("toEntityId", relation.toEntityId.take(128))
                    put("relationType", relation.relationType.take(80))
                    put("sourceConversationId", relation.sourceConversationId.take(128))
                    put("createdAt", relation.createdAt)
                })
            }
        })
        root.put("jobs", JSONArray().apply {
            jobs.forEach { job ->
                put(JSONObject().apply {
                    put("id", job.id.take(128))
                    put("title", job.title.take(240))
                    put("goal", job.goal.take(8_000))
                    put("status", job.status.take(32))
                    put("runtimeId", job.runtimeId.take(80))
                    put("sourceConversationId", job.sourceConversationId.take(128))
                    put("resultSummary", job.resultSummary.take(4_000))
                    put("workflowGraphJson", job.workflowGraphJson.take(64_000))
                    put("createdAt", job.createdAt)
                    put("updatedAt", job.updatedAt)
                    put("steps", JSONArray().apply {
                        job.steps.take(MAX_JOB_STEPS).forEach { step ->
                            put(JSONObject().apply {
                                put("id", step.id.take(128))
                                put("title", step.title.take(240))
                                put("detail", step.detail.take(2_000))
                                put("status", step.status.take(32))
                                put("evidence", step.evidence.take(2_000))
                                put("updatedAt", step.updatedAt)
                                put("checkpointKind", step.checkpointKind.take(40))
                                put("requiresApproval", step.requiresApproval)
                            })
                        }
                    })
                })
            }
        })
        root.put("actionApprovals", JSONArray().apply {
            actionApprovals.forEach { action ->
                put(JSONObject().apply {
                    put("id", action.id.take(128))
                    put("jobId", action.jobId.take(128))
                    put("stepId", action.stepId.take(128))
                    put("runtimeId", action.runtimeId.take(120))
                    put("capabilityId", action.capabilityId.take(120))
                    put("destination", action.destination.take(MAX_URL_CHARS))
                    put("title", action.title.take(240))
                    put("payload", action.payload.take(ActionApprovalLedger.MAX_PAYLOAD_CHARS))
                    put("contentHash", action.contentHash.take(64))
                    put("risk", action.risk.take(40))
                    put("status", action.status.take(32))
                    put("createdAt", action.createdAt)
                    put("expiresAt", action.expiresAt)
                    put("approvedAt", action.approvedAt)
                    put("approvalHash", action.approvalHash.take(64))
                    put("startedAt", action.startedAt)
                    put("completedAt", action.completedAt)
                    put("evidenceRef", action.evidenceRef.take(2_048))
                    put("resolution", action.resolution.take(400))
                })
            }
        })
        root.put("actionAuditEvents", JSONArray().apply {
            actionAuditEvents.forEach { event ->
                put(JSONObject().apply {
                    put("id", event.id.take(128))
                    put("sequence", event.sequence)
                    put("at", event.at)
                    put("actor", event.actor.take(40))
                    put("eventType", event.eventType.take(64))
                    put("actionId", event.actionId.take(128))
                    put("runtimeId", event.runtimeId.take(120))
                    put("destination", event.destination.take(MAX_URL_CHARS))
                    put("contentHash", event.contentHash.take(64))
                    put("status", event.status.take(32))
                    put("previousHash", event.previousHash.take(64))
                    put("eventHash", event.eventHash.take(64))
                    put("details", event.details.take(1_000))
                })
            }
        })
        return root.toString(2)
    }

    companion object {
        fun fromJson(raw: String): AppSnapshot {
            require(raw.toByteArray(Charsets.UTF_8).size <= MAX_WORKSPACE_BACKUP_BYTES) { "Import file is larger than 40 MB." }
            val root = JSONObject(raw)
            val schema = root.optInt("schemaVersion", 0)
            require(schema in 1..6) { "This backup format is not supported." }
            val schemaSemver = root.safeString("schemaSemver")
            if (schemaSemver.isNotBlank()) {
                val major = schemaSemver.substringBefore('.').toIntOrNull()
                require(major == schema) { "Backup schema version metadata is inconsistent." }
            }
            require(root.optInt("minimumReaderVersion", schema).coerceAtLeast(1) <= 6) { "This backup requires a newer Memo reader." }

            val usedTaskIds = mutableSetOf<String>()
            val tasks = root.optJSONArray("tasks").toTypedRecords(MAX_TASKS) { row ->
                TaskRecord(
                    id = uniqueImportedId(row.safeString("id"), usedTaskIds),
                    title = row.safeString("title").take(160).ifBlank { "Untitled task" },
                    targetUrl = normalizeImportedHttpsUrl(row.safeString("targetUrl")),
                    kind = TaskKind.from(row.optString("kind")),
                    query = row.safeString("query").take(8_000),
                    status = TaskStatus.from(row.optString("status")),
                    lastMessage = row.safeString("lastMessage").take(1_000),
                    createdAt = row.optLong("createdAt", nowMillis()),
                    updatedAt = row.optLong("updatedAt", nowMillis())
                )
            }
            val usedEvidenceIds = mutableSetOf<String>()
            val evidence = root.optJSONArray("evidence").toTypedRecords(MAX_EVIDENCE) { row ->
                EvidenceRecord(
                    id = uniqueImportedId(row.safeString("id"), usedEvidenceIds),
                    taskId = row.safeString("taskId").take(128),
                    title = row.safeString("title").take(240).ifBlank { "Captured evidence" },
                    sourceUrl = normalizeImportedHttpsUrl(row.safeString("sourceUrl")),
                    kind = row.safeString("kind").take(80),
                    content = row.safeString("content").take(MAX_CONTENT_CHARS),
                    summary = row.safeString("summary").take(2_000),
                    capturedAt = row.optLong("capturedAt", nowMillis()),
                    metadata = row.safeString("metadata").take(4_000).ifBlank { "{}" }
                )
            }
            val usedActivityIds = mutableSetOf<String>()
            val activity = root.optJSONArray("activity").toTypedRecords(MAX_ACTIVITY) { row ->
                ActivityRecord(
                    id = uniqueImportedId(row.safeString("id"), usedActivityIds),
                    at = row.optLong("at", nowMillis()),
                    level = row.safeString("level").take(16).ifBlank { "INFO" },
                    message = row.safeString("message").take(1_000),
                    taskId = row.safeString("taskId")
                )
            }
            val councilRuns = if (schema < 2) emptyList() else root.optJSONArray("councilRuns").toTypedRecords(MAX_COUNCIL_RUNS) { row ->
                val usedTurnIds = mutableSetOf<String>()
                val turns = row.optJSONArray("turns").toTypedRecords(MAX_COUNCIL_TURNS) { turn ->
                    CouncilTurnRecord(
                        id = uniqueImportedId(turn.safeString("id"), usedTurnIds),
                        modelId = turn.safeString("modelId").take(160),
                        modelName = turn.safeString("modelName").take(200),
                        role = turn.safeString("role").take(80),
                        requestSummary = turn.safeString("requestSummary").take(16_000),
                        response = turn.safeString("response").take(24_000),
                        status = turn.safeString("status").take(24).ifBlank { "COMPLETED" },
                        durationMs = turn.optLong("durationMs", 0).coerceAtLeast(0),
                        error = turn.safeString("error").take(400)
                    )
                }
                val sources = row.optJSONArray("sources").toTypedRecords(20) { source ->
                    CouncilSourceRef(
                        evidenceId = source.safeString("evidenceId").take(128),
                        title = source.safeString("title").take(240),
                        sourceUrl = normalizeImportedHttpsUrl(source.safeString("sourceUrl"))
                    )
                }
                CouncilRunRecord(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    prompt = row.safeString("prompt").take(8_000),
                    pattern = row.safeString("pattern").take(40),
                    turns = turns,
                    synthesis = row.safeString("synthesis").take(24_000),
                    finalDecision = row.safeString("finalDecision").take(4_000),
                    sources = sources,
                    cloudPermissionGranted = row.optBoolean("cloudPermissionGranted", false),
                    createdAt = row.optLong("createdAt", nowMillis()),
                    durationMs = row.optLong("durationMs", 0).coerceAtLeast(0)
                )
            }
            val conversations = if (schema < 3) emptyList() else root.optJSONArray("conversations").toTypedRecords(MAX_CONVERSATIONS) { row ->
                val usedMessageIds = mutableSetOf<String>()
                val messages = row.optJSONArray("messages").toTypedRecords(MAX_MESSAGES_PER_CONVERSATION) { message ->
                    ChatMessageRecord(
                        id = uniqueImportedId(message.safeString("id"), usedMessageIds),
                        role = message.safeString("role").uppercase().takeIf { it in setOf("USER", "ASSISTANT", "SYSTEM", "SOURCE", "TOOL") } ?: "USER",
                        content = message.safeString("content").take(24_000),
                        modelId = message.safeString("modelId").take(160),
                        sourceType = message.safeString("sourceType").take(40).ifBlank { "NATIVE" },
                        createdAt = message.optLong("createdAt", nowMillis())
                    )
                }
                ConversationRecord(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    title = row.safeString("title").take(240).ifBlank { "Imported conversation" },
                    providerId = row.safeString("providerId").take(160),
                    sourceType = row.safeString("sourceType").take(40).ifBlank { "IMPORTED" },
                    sourceReference = row.safeString("sourceReference").take(2_048),
                    messages = messages,
                    createdAt = row.optLong("createdAt", nowMillis()),
                    updatedAt = row.optLong("updatedAt", nowMillis())
                )
            }
            val entities = if (schema < 3) emptyList() else root.optJSONArray("entities").toTypedRecords(MAX_ENTITIES) { row ->
                val props = linkedMapOf<String, String>()
                val rawProps = row.optJSONObject("properties")
                if (rawProps != null) {
                    val keys = rawProps.keys()
                    var count = 0
                    while (keys.hasNext() && count < 40) {
                        val key = keys.next().take(80)
                        props[key] = rawProps.optString(key, "").take(2_000)
                        count++
                    }
                }
                EntityRecord(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    type = row.safeString("type").take(64).ifBlank { "KNOWLEDGE" },
                    title = row.safeString("title").take(240).ifBlank { "Untitled entity" },
                    description = row.safeString("description").take(12_000),
                    properties = props,
                    reviewStatus = row.safeString("reviewStatus").takeIf { it in setOf("PROPOSED", "APPROVED", "REJECTED", "MERGED") } ?: "APPROVED",
                    sourceConversationId = row.safeString("sourceConversationId").take(128),
                    sourceMessageId = row.safeString("sourceMessageId").take(128),
                    sourceJobId = row.safeString("sourceJobId").take(128),
                    sourceExcerpt = row.safeString("sourceExcerpt").take(2_000),
                    createdAt = row.optLong("createdAt", nowMillis()),
                    updatedAt = row.optLong("updatedAt", nowMillis())
                )
            }
            val entityIds = entities.map { it.id }.toSet()
            val relationships = if (schema < 3) emptyList() else root.optJSONArray("relationships").toTypedRecords(MAX_RELATIONSHIPS) { row ->
                EntityRelationship(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    fromEntityId = row.safeString("fromEntityId").take(128),
                    toEntityId = row.safeString("toEntityId").take(128),
                    relationType = row.safeString("relationType").take(80).ifBlank { "RELATED_TO" },
                    sourceConversationId = row.safeString("sourceConversationId").take(128),
                    createdAt = row.optLong("createdAt", nowMillis())
                )
            }.filter { it.fromEntityId in entityIds && it.toEntityId in entityIds && it.fromEntityId != it.toEntityId }
            if (schema >= 6) {
                val jobRows = root.optJSONArray("jobs") ?: JSONArray()
                for (index in 0 until jobRows.length()) {
                    val graphJson = jobRows.optJSONObject(index)?.safeString("workflowGraphJson").orEmpty()
                    if (graphJson.isNotBlank()) app.memo.android.orchestration.WorkflowGraph.fromJson(graphJson)
                }
            }
            val jobs = if (schema < 3) emptyList() else root.optJSONArray("jobs").toTypedRecords(MAX_JOBS) { row ->
                val steps = row.optJSONArray("steps").toTypedRecords(MAX_JOB_STEPS) { step ->
                    JobStepRecord(
                        id = step.safeString("id").ifBlank { newId() }.take(128),
                        title = step.safeString("title").take(240).ifBlank { "Step" },
                        detail = step.safeString("detail").take(2_000),
                        status = step.safeString("status").take(32).ifBlank { "PENDING" },
                        evidence = step.safeString("evidence").take(2_000),
                        updatedAt = step.optLong("updatedAt", nowMillis()),
                        checkpointKind = step.safeString("checkpointKind").take(40).ifBlank { "USER_CHECKPOINT" },
                        requiresApproval = step.optBoolean("requiresApproval", false)
                    )
                }
                WorkJobRecord(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    title = row.safeString("title").take(240).ifBlank { "Untitled job" },
                    goal = row.safeString("goal").take(8_000),
                    status = row.safeString("status").take(32).ifBlank { "DRAFT" },
                    runtimeId = row.safeString("runtimeId").take(80),
                    sourceConversationId = row.safeString("sourceConversationId").take(128),
                    steps = steps,
                    resultSummary = row.safeString("resultSummary").take(4_000),
                    createdAt = row.optLong("createdAt", nowMillis()),
                    updatedAt = row.optLong("updatedAt", nowMillis()),
                    workflowGraphJson = row.safeString("workflowGraphJson").take(64_000)
                )
            }
            val actionApprovals = if (schema < 4) emptyList() else root.optJSONArray("actionApprovals").toTypedRecords(MAX_ACTION_APPROVALS) { row ->
                val payload = row.safeString("payload").take(ActionApprovalLedger.MAX_PAYLOAD_CHARS)
                val runtimeId = row.safeString("runtimeId").take(120)
                val capabilityId = row.safeString("capabilityId").take(120)
                val destination = row.safeString("destination").take(MAX_URL_CHARS)
                val storedHash = row.safeString("contentHash").take(64)
                val computedHash = ActionApprovalLedger.contentHash(runtimeId, capabilityId, destination, payload)
                require(storedHash == computedHash) { "An imported action payload failed its content hash check." }
                val status = row.safeString("status").take(32).takeIf { it in setOf("PENDING", "APPROVED", "REJECTED", "IN_PROGRESS", "COMPLETED", "NEEDS_REVIEW", "FAILED", "CANCELLED", "EXPIRED", "INVALIDATED") } ?: "NEEDS_REVIEW"
                val approvalHash = row.safeString("approvalHash").take(64)
                ActionApprovalRecord(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    jobId = row.safeString("jobId").take(128), stepId = row.safeString("stepId").take(128),
                    runtimeId = runtimeId, capabilityId = capabilityId, destination = destination,
                    title = row.safeString("title").take(240).ifBlank { "External action" }, payload = payload,
                    contentHash = storedHash, risk = row.safeString("risk").take(40).ifBlank { "EXTERNAL_WRITE" },
                    status = status, createdAt = row.optLong("createdAt", nowMillis()), expiresAt = row.optLong("expiresAt", 0L),
                    approvedAt = row.optLong("approvedAt", 0L), approvalHash = approvalHash,
                    startedAt = row.optLong("startedAt", 0L), completedAt = row.optLong("completedAt", 0L),
                    evidenceRef = row.safeString("evidenceRef").take(2_048), resolution = row.safeString("resolution").take(400)
                )
            }
            val actionAuditEvents = if (schema < 4) emptyList() else root.optJSONArray("actionAuditEvents").toTypedRecords(MAX_ACTION_AUDIT_EVENTS) { row ->
                ActionAuditEvent(
                    id = row.safeString("id").ifBlank { newId() }.take(128),
                    sequence = row.optLong("sequence", 0L), at = row.optLong("at", nowMillis()),
                    actor = row.safeString("actor").take(40), eventType = row.safeString("eventType").take(64),
                    actionId = row.safeString("actionId").take(128), runtimeId = row.safeString("runtimeId").take(120),
                    destination = row.safeString("destination").take(MAX_URL_CHARS), contentHash = row.safeString("contentHash").take(64),
                    status = row.safeString("status").take(32),
                    previousHash = row.safeString("previousHash").take(64), eventHash = row.safeString("eventHash").take(64),
                    details = row.safeString("details").take(1_000)
                )
            }
            val theme = root.safeString("themeMode").uppercase().takeIf { it in setOf("SYSTEM", "LIGHT", "DARK") } ?: "SYSTEM"
            val snapshot = AppSnapshot(tasks, evidence, activity, theme, councilRuns, conversations, entities, relationships, jobs, actionApprovals, actionAuditEvents)
            require(ActionApprovalLedger.verifyLedger(snapshot)) { "The imported action ledger or audit chain failed verification." }
            return snapshot
        }

        private inline fun <reified T> JSONArray?.toTypedRecords(limit: Int, crossinline map: (JSONObject) -> T): List<T> {
            if (this == null) return emptyList()
            return (0 until minOf(length(), limit)).mapNotNull { index ->
                runCatching { optJSONObject(index)?.let(map) }.getOrNull()
            }
        }
    }
}

private fun JSONObject.safeString(key: String): String = if (isNull(key)) "" else optString(key, "").takeUnless { it == "null" } ?: ""

private fun uniqueImportedId(candidate: String, used: MutableSet<String>): String {
    val normalized = candidate.trim().take(128)
    if (normalized.isNotBlank() && used.add(normalized)) return normalized
    var replacement: String
    do { replacement = newId() } while (!used.add(replacement))
    return replacement
}

private fun normalizeImportedHttpsUrl(raw: String): String {
    val input = raw.trim().take(MAX_URL_CHARS)
    if (input.isBlank()) return ""
    return runCatching {
        val candidate = if (input.contains("://")) input else "https://$input"
        val uri = java.net.URI(candidate)
        require(uri.scheme.equals("https", ignoreCase = true))
        require(!uri.host.isNullOrBlank() && uri.rawUserInfo == null)
        uri.toASCIIString().takeIf { it.length <= MAX_URL_CHARS } ?: ""
    }.getOrDefault("")
}
