package app.memo.android.data

/**
 * A continuation is a new execution, never a restart of the previous run.
 *
 * Eligibility is deliberately conservative. It does not authorize a capability,
 * reuse an approval, or establish that a destination is still in the same state.
 */
data class AgentRunContinuationAssessment(
    val allowed: Boolean,
    val reason: String
)

data class AgentRunContinuationResolution(
    val parentRun: AgentRunRecord?,
    val assessment: AgentRunContinuationAssessment
)

object AgentRunContinuationPolicy {
    fun resolveParent(
        parentRunId: String,
        conversationId: String,
        runs: List<AgentRunRecord>
    ): AgentRunContinuationResolution {
        val parent = runs.firstOrNull {
            it.id == parentRunId && it.conversationId == conversationId
        }
        val assessment = parent?.let(::assess) ?: AgentRunContinuationAssessment(
            allowed = false,
            reason = "The original conversation or goal is missing."
        )
        return AgentRunContinuationResolution(parentRun = parent, assessment = assessment)
    }

    fun assess(run: AgentRunRecord): AgentRunContinuationAssessment {
        if (run.status != "NEEDS_REVIEW") {
            return AgentRunContinuationAssessment(
                allowed = false,
                reason = "Only an interrupted run awaiting review can be continued."
            )
        }
        if (run.conversationId.isBlank() || run.goal.isBlank()) {
            return AgentRunContinuationAssessment(
                allowed = false,
                reason = "The original conversation or goal is missing."
            )
        }
        if (run.pendingApprovalCallId.isNotBlank()) {
            return AgentRunContinuationAssessment(
                allowed = false,
                reason = "A previous approval reference is unresolved and cannot be reused."
            )
        }
        if (run.toolCalls < 0 || run.successfulToolCalls < 0 || run.successfulToolCalls > run.toolCalls) {
            return AgentRunContinuationAssessment(
                allowed = false,
                reason = "The saved capability counters are inconsistent; inspect this run before continuing."
            )
        }
        if (run.lastEventType == "TOOL_STARTED" ||
            run.lastEventType == "TOOL_FAILED" ||
            run.toolCalls > run.successfulToolCalls
        ) {
            return AgentRunContinuationAssessment(
                allowed = false,
                reason = "A capability may have started without a verified completion. Inspect its destination before continuing."
            )
        }
        return AgentRunContinuationAssessment(
            allowed = true,
            reason = "A new run may be started after reviewing the saved conversation and evidence. Previous actions and approvals will not be replayed."
        )
    }
}
