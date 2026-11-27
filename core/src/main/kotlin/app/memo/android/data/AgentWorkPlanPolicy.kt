package app.memo.android.data

import app.memo.android.orchestration.WorkflowGraph
import app.memo.android.orchestration.WorkflowGraphValidator

/**
 * Converts a validated dynamic workflow proposal into a durable, reviewable Work item.
 * This is a persistence boundary only: it never executes graph nodes or external actions.
 */
object AgentWorkPlanPolicy {
    fun prepare(
        title: String,
        graph: WorkflowGraph,
        sourceConversationId: String,
        sourceMessageId: String
    ): WorkJobRecord {
        val cleanTitle = title.trim()
        require(cleanTitle.isNotBlank() && cleanTitle.length <= 240) {
            "A work plan title must contain 1–240 characters."
        }
        require(sourceConversationId.isNotBlank() && sourceConversationId.length <= 128) {
            "A source conversation is required for a Talk-created work plan."
        }
        require(sourceMessageId.isNotBlank() && sourceMessageId.length <= 128) {
            "A source message is required for a Talk-created work plan."
        }
        WorkflowGraphValidator.validate(graph)
        val now = nowMillis()
        val steps = WorkflowGraphValidator.topologicalOrder(graph).map { node ->
            val detail = buildString {
                append("Participant: ").append(node.participant.name)
                append("\nRuntime: ").append(node.runtimeId)
                append("\nCapabilities: ").append(node.requiredCapabilities.ifEmpty { listOf("none") }.joinToString(", "))
                append("\nDepends on: ").append(node.dependsOn.ifEmpty { listOf("none") }.joinToString(", "))
                append("\nInput: ").append(node.inputContract)
                append("\nExpected output: ").append(node.outputContract)
                append("\nDone when: ").append(node.postcondition)
                if (node.requiresApproval) append("\nHuman approval required before this step.")
                if (node.externalSideEffect) append("\nExternal side effect: do not execute from this draft.")
            }.take(2_000)
            JobStepRecord(
                title = node.title,
                detail = detail,
                status = "PENDING",
                updatedAt = now,
                checkpointKind = when {
                    node.externalSideEffect -> "EXTERNAL_ACTION"
                    node.requiresApproval -> "HUMAN_REVIEW"
                    else -> "USER_CHECKPOINT"
                },
                requiresApproval = node.requiresApproval || node.externalSideEffect
            )
        }
        return WorkJobRecord(
            title = cleanTitle,
            goal = graph.goal,
            status = "DRAFT",
            runtimeId = "app.jobs",
            sourceConversationId = sourceConversationId,
            sourceMessageId = sourceMessageId,
            steps = steps,
            resultSummary = "Dynamic workflow proposal saved for review. No step has run and no external action has been executed.",
            createdAt = now,
            updatedAt = now,
            workflowGraphJson = graph.toJson()
        )
    }
}
