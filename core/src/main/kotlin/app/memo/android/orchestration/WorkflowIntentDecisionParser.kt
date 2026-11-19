package app.memo.android.orchestration

import org.json.JSONObject

/** A model may recommend a workflow, but it cannot execute one. */
sealed class WorkflowIntentDecision {
    object NoWorkflow : WorkflowIntentDecision()
    data class Proposal(val workflowJson: String) : WorkflowIntentDecision()
}

object WorkflowIntentDecisionParser {
    private const val MAX_RAW_CHARS = 72_000

    fun parse(raw: String): WorkflowIntentDecision {
        require(raw.isNotBlank() && raw.length <= MAX_RAW_CHARS) {
            "The intent decision is empty or exceeds its size limit."
        }
        val fence = "```"
        val clean = raw.trim()
            .removePrefix(fence + "json")
            .removePrefix(fence)
            .removeSuffix(fence)
            .trim()
        val root = JSONObject(clean)
        return when (root.optString("decision", "").trim().uppercase()) {
            "NO_WORKFLOW" -> WorkflowIntentDecision.NoWorkflow
            "PROPOSE" -> {
                val workflow = root.optJSONObject("workflow")
                    ?: throw IllegalArgumentException("A PROPOSE decision must contain a workflow object.")
                WorkflowIntentDecision.Proposal(workflow.toString())
            }
            else -> throw IllegalArgumentException("The intent decision must be NO_WORKFLOW or PROPOSE.")
        }
    }
}
