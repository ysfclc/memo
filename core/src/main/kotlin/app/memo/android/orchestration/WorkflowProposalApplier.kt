package app.memo.android.orchestration

import app.memo.android.data.ActivityRecord
import app.memo.android.data.AppSnapshot
import app.memo.android.data.JobStepRecord
import app.memo.android.runtime.RuntimeDescriptor

/**
 * Applies a validated, non-external TaskSpec proposal to an untouched draft job.
 * This changes the plan only; it never starts a node or performs a runtime action.
 */
object WorkflowProposalApplier {
    fun apply(
        snapshot: AppSnapshot,
        jobId: String,
        proposed: WorkflowGraph,
        registeredRuntimes: List<RuntimeDescriptor>,
        now: Long
    ): AppSnapshot {
        val existing = snapshot.jobs.firstOrNull { it.id == jobId }
            ?: throw IllegalArgumentException("This work plan no longer exists.")
        require(existing.status == "DRAFT" && existing.steps.all { it.status == "PENDING" }) {
            "A model-expanded plan can only replace an untouched draft. Create a follow-up plan for work that has started."
        }

        val originalIntent = existing.goal.substringBefore("\n\nMemo route:").trim()
        require(proposed.goal.trim() == originalIntent) {
            "The expanded proposal changed the original user goal. Keep the original intent unchanged and try again."
        }

        val manifest = CapabilityCompiler.compileForTasks(proposed.goal, proposed.nodes, registeredRuntimes)
        require(manifest.unresolvedRequirements.isEmpty()) {
            "The proposal requests unavailable capabilities: ${manifest.unresolvedRequirements.joinToString(", ")}."
        }
        val unavailableRuntimes = manifest.capabilities.filter { it.runtimeStatus != "AVAILABLE" }
        require(unavailableRuntimes.isEmpty()) {
            "The proposal uses a runtime that is not currently available: ${unavailableRuntimes.joinToString { it.runtimeId + " (" + it.runtimeStatus + ")" }}."
        }

        // Recompile from trusted registry metadata. Never trust model-supplied side-effect flags.
        val validated = WorkflowGraphCompiler.compile(proposed.goal, proposed.nodes, manifest, proposed.budget)
        require(validated.nodes.none {
            it.externalWrite || it.externalSideEffect || it.requiresApproval ||
                it.kind == WorkflowNodeKind.APPROVAL || it.runtimeId == "app.approvals"
        }) {
            "This expansion accepts no external side-effect, approval-gated or approval-runtime tasks. Keep external actions in their separate review flow."
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

        val updated = existing.copy(
            steps = steps,
            workflowGraphJson = validated.toJson(),
            resultSummary = "A local model expanded this draft into a validated workflow proposal. No nodes were executed.",
            updatedAt = now
        )
        return snapshot.copy(
            jobs = snapshot.jobs.map { if (it.id == jobId) updated else it },
            activity = (listOf(ActivityRecord(
                message = "Read-only workflow proposal expanded and saved: ${updated.title}",
                taskId = jobId
            )) + snapshot.activity).take(500)
        )
    }
}
