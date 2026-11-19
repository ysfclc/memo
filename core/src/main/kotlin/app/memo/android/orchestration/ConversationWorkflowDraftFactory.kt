package app.memo.android.orchestration

import app.memo.android.data.AppSnapshot
import app.memo.android.data.JobStepRecord
import app.memo.android.data.WorkJobRecord
import app.memo.android.runtime.RuntimeDescriptor

/**
 * Creates a Talk-linked Work draft from a validated dynamic graph.
 * This boundary persists inspectable steps only: it never starts a checkpoint or performs I/O.
 */
object ConversationWorkflowDraftFactory {
    fun create(
        snapshot: AppSnapshot,
        sourceConversationId: String,
        sourceMessageId: String,
        proposed: WorkflowGraph,
        registeredRuntimes: List<RuntimeDescriptor>,
        now: Long
    ): WorkJobRecord {
        val conversation = snapshot.conversations.firstOrNull { it.id == sourceConversationId }
            ?: throw IllegalArgumentException("The source Talk conversation no longer exists.")
        val sourceMessage = conversation.messages.firstOrNull { it.id == sourceMessageId }
            ?: throw IllegalArgumentException("The source user message no longer exists.")
        require(sourceMessage.role == "USER") {
            "A workflow draft must be linked to the exact original user message."
        }
        val originalIntent = sourceMessage.content.trim()
        require(originalIntent.isNotBlank() && originalIntent.length <= 8_000) {
            "The original user request is empty or too long for a workflow draft."
        }
        require(proposed.goal.trim() == originalIntent) {
            "The workflow proposal changed the original user request."
        }
        require(snapshot.jobs.size < 240) {
            "The 240-job limit has been reached. Export a backup before removing older jobs."
        }
        require(snapshot.jobs.none {
            it.sourceConversationId == sourceConversationId && it.sourceMessageId == sourceMessageId
        }) {
            "A workflow draft already exists for this user message."
        }
        require(proposed.nodes.none {
            it.externalWrite || it.externalSideEffect || it.requiresApproval ||
                it.kind == WorkflowNodeKind.APPROVAL || it.runtimeId == "app.approvals"
        }) {
            "Talk can only create a read-only planning draft. External actions require their dedicated approval flow."
        }

        val registeredById = registeredRuntimes.associateBy { it.id }
        require(proposed.nodes.all { registeredById[it.runtimeId]?.status == "AVAILABLE" }) {
            "The workflow proposal targets a runtime that is not currently available."
        }
        val manifest = CapabilityCompiler.compileForTasks(originalIntent, proposed.nodes, registeredRuntimes)
        require(manifest.unresolvedRequirements.isEmpty()) {
            "The workflow proposal requests unavailable capabilities: ${manifest.unresolvedRequirements.joinToString(", ")}."
        }
        require(manifest.capabilities.all { it.runtimeStatus == "AVAILABLE" }) {
            "The workflow proposal requires a runtime that is not currently available."
        }
        val validated = WorkflowGraphCompiler.compile(originalIntent, proposed.nodes, manifest, proposed.budget)
        require(validated.nodes.none {
            it.externalWrite || it.externalSideEffect || it.requiresApproval ||
                it.kind == WorkflowNodeKind.APPROVAL || it.runtimeId == "app.approvals"
        }) {
            "The compiled workflow is not eligible for a read-only Talk draft."
        }

        val ordered = WorkflowGraphValidator.topologicalOrder(validated)
        val steps = ordered.map { node ->
            val detail = buildList {
                add("Participant: ${node.participant.name}")
                add("Runtime: ${node.runtimeId}")
                if (node.dependsOn.isNotEmpty()) add("Depends on: ${node.dependsOn.joinToString(", ")}")
                if (node.requiredCapabilities.isNotEmpty()) add("Capabilities: ${node.requiredCapabilities.joinToString(", ")}")
                add("Input: ${node.inputContract}")
                add("Output: ${node.outputContract}")
                add("Verify: ${node.postcondition}")
            }.joinToString("\n").take(4_000)
            JobStepRecord(
                id = node.id,
                title = node.title,
                detail = detail,
                status = "PENDING",
                updatedAt = now,
                checkpointKind = node.kind.name,
                requiresApproval = false
            )
        }
        return WorkJobRecord(
            title = originalIntent.lineSequence().firstOrNull().orEmpty().take(120).ifBlank { "Work from Talk" },
            goal = originalIntent,
            status = "DRAFT",
            runtimeId = "app.jobs",
            sourceConversationId = sourceConversationId,
            sourceMessageId = sourceMessageId,
            steps = steps,
            resultSummary = "Memo prepared a dynamically validated workflow draft. No nodes were executed.",
            createdAt = now,
            updatedAt = now,
            workflowGraphJson = validated.toJson()
        )
    }
}
