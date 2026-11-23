package app.memo.android.data

/**
 * A continuation is a new execution, never a restart of the previous run.
 *
 * This policy deliberately permits only interrupted runs whose recorded capability
 * lifecycle has no unresolved invocation. It does not authorize a capability, reuse
 * an approval, or establish that a destination is still in the same state.
 */
data class AgentRunContinuationAssessment(
    val allowed: Boolean,
    val reason: String
)

object AgentRunContinuationPolicy {
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
        if (run.lastEventType == "TOOL_STARTED" || run.toolCalls > run.successfulToolCalls) {
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
