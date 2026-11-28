package app.memo.android.orchestration

import app.memo.android.data.WorkGraphExecutionRecord
import app.memo.android.data.WorkNodeInvocationRecord
import app.memo.android.data.WorkJobRecord

/**
 * Pure DAG readiness selector. It never dispatches tools or mutates a journal.
 * Callers must persist each returned transition before any adapter is invoked.
 */
object WorkGraphScheduler {
    sealed interface Decision {
        data class Eligible(
            val node: TaskSpec,
            val invocationId: String
        ) : Decision

        data class Ready(
            val node: TaskSpec,
            val invocationId: String
        ) : Decision

        data class AwaitingApproval(val nodeId: String) : Decision
        data class AwaitingRecovery(val nodeId: String, val state: String) : Decision
        data class Blocked(val nodeIds: List<String>) : Decision
        data object Completed : Decision
        data class Stopped(val reason: String) : Decision
    }

    /**
     * Selects by graph topology and persisted node states only—never by goal/title keywords
     * or the order of the human-facing JobStepRecord list.
     */
    fun next(job: WorkJobRecord, journal: WorkGraphExecutionRecord): Decision {
        journal.validateFor(job)
        when (journal.status) {
            WorkGraphExecutionRecord.STATUS_COMPLETED -> return Decision.Completed
            WorkGraphExecutionRecord.STATUS_FAILED,
            WorkGraphExecutionRecord.STATUS_NEEDS_REVIEW,
            WorkGraphExecutionRecord.STATUS_CANCELLED -> return Decision.Stopped(journal.status)
        }

        val invocations = journal.invocations.associateBy { it.nodeId }
        val awaitingApproval = journal.invocations.firstOrNull {
            it.status == WorkNodeInvocationRecord.STATUS_WAITING_FOR_APPROVAL
        }
        if (awaitingApproval != null || journal.status == WorkGraphExecutionRecord.STATUS_WAITING_FOR_APPROVAL) {
            return Decision.AwaitingApproval(awaitingApproval?.nodeId.orEmpty())
        }

        val interrupted = journal.invocations.firstOrNull {
            it.status == WorkNodeInvocationRecord.STATUS_PREPARED ||
                it.status == WorkNodeInvocationRecord.STATUS_RUNNING
        }
        if (interrupted != null) {
            return Decision.AwaitingRecovery(interrupted.nodeId, interrupted.status)
        }

        val graph = WorkflowGraph.fromJson(job.workflowGraphJson)
        val ordered = WorkflowGraphValidator.topologicalOrder(graph)

        ordered.firstOrNull { node ->
            invocations[node.id]?.status == WorkNodeInvocationRecord.STATUS_READY
        }?.let { node ->
            return Decision.Ready(node, invocations.getValue(node.id).invocationId)
        }

        ordered.firstOrNull { node ->
            val invocation = invocations.getValue(node.id)
            invocation.status == WorkNodeInvocationRecord.STATUS_BLOCKED &&
                invocation.dependencies.all { dependency ->
                    invocations[dependency]?.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED
                }
        }?.let { node ->
            return Decision.Eligible(node, invocations.getValue(node.id).invocationId)
        }

        val unfinished = ordered.filter { node ->
            invocations.getValue(node.id).status == WorkNodeInvocationRecord.STATUS_BLOCKED
        }.map { it.id }
        if (unfinished.isNotEmpty()) return Decision.Blocked(unfinished)

        val terminal = journal.invocations.all {
            it.status in WorkNodeInvocationRecord.TERMINAL_STATUSES
        }
        return if (terminal && journal.invocations.all {
                it.status == WorkNodeInvocationRecord.STATUS_SUCCEEDED ||
                    it.status == WorkNodeInvocationRecord.STATUS_SKIPPED
            }
        ) Decision.Completed else Decision.Stopped("No node is safely runnable from the persisted graph state.")
    }
}
