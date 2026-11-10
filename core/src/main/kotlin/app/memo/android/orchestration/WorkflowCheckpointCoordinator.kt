package app.memo.android.orchestration

import app.memo.android.data.ActivityRecord
import app.memo.android.data.AppSnapshot
import app.memo.android.runtime.RuntimeDescriptor

/**
 * Dependency-aware, persisted checkpoint transitions for a workflow graph.
 *
 * This is deliberately not a runtime dispatcher: it can start a safe checkpoint and record
 * a user-reported outcome, but it cannot invoke a browser, Android app, model or external write.
 * Legacy jobs without a graph retain their original sequential checkpoint behavior.
 */
object WorkflowCheckpointCoordinator {
    fun start(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        registeredRuntimes: List<RuntimeDescriptor>,
        now: Long
    ): Result<AppSnapshot> = runCatching {
        val job = snapshot.jobs.firstOrNull { it.id == jobId }
            ?: error("This work plan no longer exists.")
        require(job.status in setOf("DRAFT", "READY", "RUNNING", "WAITING_FOR_USER")) {
            "This plan cannot start a checkpoint from ${job.status}."
        }
        val step = job.steps.firstOrNull { it.id == stepId }
            ?: error("This checkpoint no longer exists.")
        require(job.steps.none { it.status == "RUNNING" }) {
            "Another checkpoint is already active. Finish or review it before starting another."
        }

        val graph = job.workflowGraphJson.takeIf { it.isNotBlank() }?.let { WorkflowGraph.fromJson(it) }
        if (graph == null) {
            require(!step.requiresApproval && step.checkpointKind != "EXTERNAL_ACTION") {
                "This checkpoint requires the dedicated exact-content action approval flow."
            }
            val index = job.steps.indexOfFirst { it.id == stepId }
            require(job.steps.take(index).all { it.status == "COMPLETED" }) {
                "Complete earlier checkpoints in order first."
            }
        } else {
            val node = graph.nodes.firstOrNull { it.id == stepId }
                ?: error("This checkpoint is not part of the saved workflow graph.")
            require(job.steps.map { it.id }.toSet() == graph.nodes.map { it.id }.toSet() &&
                job.steps.size == graph.nodes.size) {
                "The saved checkpoints do not match the workflow graph. Review or rebuild this plan."
            }
            require(graph.nodes.none {
                it.externalWrite || it.externalSideEffect || it.requiresApproval ||
                    it.kind == WorkflowNodeKind.APPROVAL || it.runtimeId == "app.approvals"
            }) {
                "This workflow contains an approval-gated or external action. Use its dedicated approval flow."
            }
            val completed = job.steps.filter { it.status == "COMPLETED" }.map { it.id }.toSet()
            require(node.dependsOn.all { it in completed }) {
                "This checkpoint is waiting for dependencies: ${node.dependsOn.filterNot { it in completed }.joinToString(", ")}."
            }
            val runtimesById = registeredRuntimes.associateBy { it.id }
            require(runtimesById.size == registeredRuntimes.size) { "The runtime registry contains duplicate runtime IDs." }
            graph.nodes.forEach { candidate ->
                val runtime = runtimesById[candidate.runtimeId]
                    ?: error("Workflow runtime '${candidate.runtimeId}' is not registered.")
                require(runtime.status == "AVAILABLE") {
                    "Workflow runtime '${candidate.runtimeId}' is not currently available (${runtime.status})."
                }
                candidate.requiredCapabilities.forEach { capabilityId ->
                    require(runtime.capabilities.any { it.id == capabilityId }) {
                        "Capability '${candidate.runtimeId}:$capabilityId' is no longer registered."
                    }
                }
            }
        }
        require(step.status in setOf("PENDING", "NEEDS_REVIEW")) {
            "This checkpoint is not startable from ${step.status}."
        }

        val updatedStep = step.copy(
            status = "RUNNING",
            detail = (step.detail.take(1_700) + "\n\nStarted from Work at $now. Record an observed outcome before continuing.").take(2_000),
            updatedAt = now
        )
        val updatedJob = job.copy(
            status = "RUNNING",
            steps = job.steps.map { if (it.id == stepId) updatedStep else it },
            resultSummary = "Checkpoint active. Memo has not invoked a runtime or performed an external action.",
            updatedAt = now
        )
        snapshot.copy(
            jobs = snapshot.jobs.map { if (it.id == jobId) updatedJob else it },
            activity = (listOf(ActivityRecord(at = now, message = "Started workflow checkpoint: ${step.title}", taskId = jobId)) + snapshot.activity).take(500)
        )
    }

    /**
     * Records a user-reported outcome only. This never upgrades a user note into runtime-verified
     * evidence and never completes a checkpoint that represents an external action.
     */
    fun recordUserOutcome(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        outcome: String,
        now: Long
    ): Result<AppSnapshot> = runCatching {
        val clean = outcome.trim().take(2_000)
        require(clean.length >= 6) { "Record a concrete outcome of at least six characters." }
        val job = snapshot.jobs.firstOrNull { it.id == jobId } ?: error("This work plan no longer exists.")
        val step = job.steps.firstOrNull { it.id == stepId } ?: error("This checkpoint no longer exists.")
        val graph = job.workflowGraphJson.takeIf { it.isNotBlank() }?.let { WorkflowGraph.fromJson(it) }
        val node = graph?.nodes?.firstOrNull { it.id == stepId }
        if (graph != null) {
            require(node != null) { "This checkpoint is not in the saved workflow graph." }
            require(!node.externalWrite && !node.externalSideEffect && !node.requiresApproval &&
                node.kind != WorkflowNodeKind.APPROVAL && node.runtimeId != "app.approvals") {
                "External actions cannot be completed by writing a note. Use the dedicated approval and runtime flow."
            }
        } else {
            require(!step.requiresApproval && step.checkpointKind != "EXTERNAL_ACTION") {
                "External actions cannot be completed by writing a note. Use the dedicated approval and runtime flow."
            }
            val index = job.steps.indexOfFirst { it.id == stepId }
            require(job.steps.take(index).all { it.status == "COMPLETED" }) {
                "Complete earlier checkpoints in order first."
            }
        }
        require(step.status in setOf("RUNNING", "NEEDS_REVIEW")) {
            "Start this checkpoint or open its review before recording an outcome."
        }
        val steps = job.steps.map { candidate ->
            if (candidate.id == stepId) candidate.copy(
                status = "COMPLETED",
                evidence = "USER_REPORTED: $clean".take(2_000),
                detail = (candidate.detail.take(1_200) + "\n\nOutcome recorded by the user; not independently verified.").take(2_000),
                updatedAt = now
            ) else candidate
        }
        val allComplete = steps.isNotEmpty() && steps.all { it.status == "COMPLETED" }
        val updatedJob = job.copy(
            status = if (allComplete) "SUCCEEDED" else "READY",
            steps = steps,
            resultSummary = if (allComplete)
                "All checkpoints have user-reported outcomes. Runtime postconditions and external effects were not independently verified."
            else "Checkpoint outcome recorded by the user. Dependency-ready work can continue.",
            updatedAt = now
        )
        snapshot.copy(
            jobs = snapshot.jobs.map { if (it.id == jobId) updatedJob else it },
            activity = (listOf(ActivityRecord(at = now, message = "User recorded checkpoint outcome: ${step.title}", taskId = jobId)) + snapshot.activity).take(500)
        )
    }
}
