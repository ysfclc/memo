package app.memo.android.orchestration

import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

enum class WorkflowNodeKind { SCOPE, CONTEXT, DOMAIN_WORK, ARTIFACT, VERIFICATION, APPROVAL, REVIEW }
enum class WorkflowParticipant { DETERMINISTIC, LOCAL_MODEL, FRONTIER_WEB, BROWSER, ANDROID_APP, USER }

data class WorkflowBudget(
    val maxNodes: Int = 40,
    val maxDurationMs: Long = 900_000,
    val maxModelCalls: Int = 12,
    val maxExternalWrites: Int = 1
)

data class WorkflowNodeSpec(
    val id: String,
    val title: String,
    val kind: WorkflowNodeKind,
    val participant: WorkflowParticipant,
    val runtimeId: String,
    val requiredCapabilities: List<String>,
    val dependsOn: List<String>,
    val inputContract: String,
    val outputContract: String,
    val postcondition: String,
    val requiresApproval: Boolean = false,
    val externalWrite: Boolean = false,
    val timeoutMs: Long = 120_000,
    val maxRetries: Int = 0,
    val idempotencyKey: String
)

data class WorkflowGraph(
    val schemaVersion: Int = 1,
    val goal: String,
    val nodes: List<WorkflowNodeSpec>,
    val budget: WorkflowBudget = WorkflowBudget()
) {
    fun toJson(): String {
        WorkflowGraphValidator.validate(this)
        return JSONObject().apply {
            put("schemaVersion", schemaVersion)
            put("goal", goal)
            put("budget", JSONObject().apply {
                put("maxNodes", budget.maxNodes)
                put("maxDurationMs", budget.maxDurationMs)
                put("maxModelCalls", budget.maxModelCalls)
                put("maxExternalWrites", budget.maxExternalWrites)
            })
            put("nodes", JSONArray().apply {
                nodes.forEach { node ->
                    put(JSONObject().apply {
                        put("id", node.id)
                        put("title", node.title)
                        put("kind", node.kind.name)
                        put("participant", node.participant.name)
                        put("runtimeId", node.runtimeId)
                        put("requiredCapabilities", JSONArray(node.requiredCapabilities))
                        put("dependsOn", JSONArray(node.dependsOn))
                        put("inputContract", node.inputContract)
                        put("outputContract", node.outputContract)
                        put("postcondition", node.postcondition)
                        put("requiresApproval", node.requiresApproval)
                        put("externalWrite", node.externalWrite)
                        put("timeoutMs", node.timeoutMs)
                        put("maxRetries", node.maxRetries)
                        put("idempotencyKey", node.idempotencyKey)
                    })
                }
            })
        }.toString()
    }

    companion object {
        fun fromJson(raw: String): WorkflowGraph {
            require(raw.length <= 64_000) { "Workflow graph exceeds its storage limit." }
            val root = JSONObject(raw)
            val budgetRow = root.optJSONObject("budget") ?: JSONObject()
            val nodeRows = root.optJSONArray("nodes") ?: JSONArray()
            require(nodeRows.length() in 1..40) { "Workflow graph must contain between 1 and 40 nodes." }
            val nodes = (0 until nodeRows.length()).map { index ->
                val row = nodeRows.getJSONObject(index)
                fun strings(key: String): List<String> {
                    val values = row.optJSONArray(key) ?: JSONArray()
                    require(values.length() <= 40) { "Workflow node list is too large." }
                    return (0 until values.length()).map { values.getString(it).take(128) }
                }
                WorkflowNodeSpec(
                    id = row.optString("id", "").take(64),
                    title = row.optString("title", "").take(240),
                    kind = WorkflowNodeKind.valueOf(row.optString("kind")),
                    participant = WorkflowParticipant.valueOf(row.optString("participant")),
                    runtimeId = row.optString("runtimeId", "").take(120),
                    requiredCapabilities = strings("requiredCapabilities"),
                    dependsOn = strings("dependsOn"),
                    inputContract = row.optString("inputContract", "").take(1_000),
                    outputContract = row.optString("outputContract", "").take(1_000),
                    postcondition = row.optString("postcondition", "").take(1_000),
                    requiresApproval = row.optBoolean("requiresApproval", false),
                    externalWrite = row.optBoolean("externalWrite", false),
                    timeoutMs = row.optLong("timeoutMs", 120_000),
                    maxRetries = row.optInt("maxRetries", 0),
                    idempotencyKey = row.optString("idempotencyKey", "").take(64)
                )
            }
            val graph = WorkflowGraph(
                schemaVersion = root.optInt("schemaVersion", 0),
                goal = root.optString("goal", "").take(8_000),
                nodes = nodes,
                budget = WorkflowBudget(
                    maxNodes = budgetRow.optInt("maxNodes", 40),
                    maxDurationMs = budgetRow.optLong("maxDurationMs", 900_000),
                    maxModelCalls = budgetRow.optInt("maxModelCalls", 12),
                    maxExternalWrites = budgetRow.optInt("maxExternalWrites", 1)
                )
            )
            WorkflowGraphValidator.validate(graph)
            return graph
        }
    }
}

object WorkflowGraphValidator {
    fun topologicalOrder(graph: WorkflowGraph): List<WorkflowNodeSpec> {
        validateShape(graph)
        val byId = graph.nodes.associateBy { it.id }
        val indegree = graph.nodes.associate { it.id to it.dependsOn.size }.toMutableMap()
        val dependants = mutableMapOf<String, MutableList<String>>()
        graph.nodes.forEach { node -> node.dependsOn.forEach { dependency ->
            dependants.getOrPut(dependency) { mutableListOf() }.add(node.id)
        } }
        val ready = ArrayDeque(graph.nodes.filter { indegree.getValue(it.id) == 0 }.map { it.id })
        val ordered = mutableListOf<WorkflowNodeSpec>()
        while (ready.isNotEmpty()) {
            val id = ready.removeFirst()
            ordered += byId.getValue(id)
            dependants[id].orEmpty().forEach { next ->
                val remaining = indegree.getValue(next) - 1
                indegree[next] = remaining
                if (remaining == 0) ready.addLast(next)
            }
        }
        require(ordered.size == graph.nodes.size) { "Workflow graph contains a dependency cycle." }
        return ordered
    }

    fun validate(graph: WorkflowGraph) { topologicalOrder(graph) }

    private fun validateShape(graph: WorkflowGraph) {
        require(graph.schemaVersion == 1) { "Unsupported workflow graph schema." }
        require(graph.goal.isNotBlank() && graph.goal.length <= 8_000) { "Workflow goal is missing or too long." }
        require(graph.nodes.size in 1..graph.budget.maxNodes && graph.budget.maxNodes in 1..40) { "Workflow node budget exceeded." }
        require(graph.budget.maxDurationMs in 1..3_600_000) { "Workflow time budget is outside safe bounds." }
        require(graph.budget.maxModelCalls in 0..40 && graph.budget.maxExternalWrites in 0..5) { "Workflow call budget is outside safe bounds." }
        val ids = graph.nodes.map { it.id }
        require(ids.distinct().size == ids.size) { "Workflow node IDs must be unique." }
        val known = ids.toSet()
        graph.nodes.forEach { node ->
            require(node.id.matches(Regex("[a-z0-9][a-z0-9._-]{0,63}"))) { "Invalid workflow node ID." }
            require(node.title.isNotBlank() && node.title.length <= 240) { "Workflow node title is missing or too long." }
            require(node.inputContract.length <= 1_000 && node.outputContract.length <= 1_000 && node.postcondition.isNotBlank() && node.postcondition.length <= 1_000) { "Workflow contracts or postcondition are missing or too long." }
            require(node.dependsOn.distinct().size == node.dependsOn.size && node.id !in node.dependsOn && node.dependsOn.all { it in known }) { "Workflow dependencies contain an unknown, duplicate or self reference." }
            require(node.requiredCapabilities.size <= 20 && node.requiredCapabilities.all { it.matches(Regex("[a-z0-9._-]{1,120}")) }) { "Invalid workflow capability requirement." }
            require(node.timeoutMs in 1..600_000 && node.maxRetries in 0..3) { "Workflow node execution limits are outside safe bounds." }
            require(node.idempotencyKey.matches(Regex("[0-9a-f]{64}"))) { "Workflow node idempotency key is missing or invalid." }
            if (node.externalWrite) require(
                node.requiresApproval && node.kind == WorkflowNodeKind.APPROVAL &&
                    node.participant == WorkflowParticipant.USER && node.runtimeId == "app.approvals" && node.maxRetries == 0
            ) {
                "External writes must be isolated behind a non-retrying user approval checkpoint."
            }
        }
        require(graph.nodes.count { it.externalWrite } <= graph.budget.maxExternalWrites) { "Workflow external-write budget exceeded." }
    }
}

object WorkflowGraphCompiler {
    fun compile(plan: IntentPlan, manifest: CapabilityManifest): WorkflowGraph {
        val available = manifest.capabilities.map { it.capabilityId }.toSet()
        val domainNode = when (plan.domain) {
            IntentDomain.RESEARCH -> node("domain", plan.steps.getOrNull(2)?.title ?: "Gather source evidence", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.BROWSER, "runtime.browser", listOf("browser.navigate_https", "browser.inspect_page", "browser.capture_evidence"), listOf("scope", "context"), "Intent and relevant context", "Sourced claims with URLs/excerpts", "Every retained claim has source evidence.")
            IntentDomain.BUILD -> node("domain", plan.steps.getOrNull(2)?.title ?: "Inspect and implement", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("model.list", "model.generate", "browser.navigate_https", "browser.inspect_page"), listOf("scope", "context"), "Goal, constraints, repository and existing tests", "Bounded implementation diff and regression tests", "The requested behavior exists in source and tests pass.")
            IntentDomain.CAREER -> node("domain", plan.steps.getOrNull(2)?.title ?: "Prepare career artifact", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("chat.create", "chat.append_message", "model.generate"), listOf("scope", "context"), "Target role and approved Self Model facts", "Factual, tailored career artifact", "Every personal claim is supported or marked for confirmation.")
            IntentDomain.CONTENT -> node("domain", plan.steps.getOrNull(2)?.title ?: "Create content draft", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.LOCAL_MODEL, "app.models", listOf("chat.create", "chat.append_message", "model.generate"), listOf("scope", "context"), "Idea, source material and approved positioning", "Editable private content draft", "Factual claims are sourced and no content is published.")
            IntentDomain.CRM -> node("domain", plan.steps.getOrNull(2)?.title ?: "Resolve identity and propose record", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.DETERMINISTIC, "app.entities", listOf("entity.create", "entity.link", "entity.propose"), listOf("scope", "context"), "Captured person/company and source", "Deduplicated, sourced record proposal", "Identity matches and field provenance are reviewable.")
            IntentDomain.CAPTURE -> node("domain", plan.steps.getOrNull(2)?.title ?: "Capture and structure material", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.DETERMINISTIC, "app.entities", listOf("chat.create", "chat.append_message", "entity.propose"), listOf("scope", "context"), "Original supplied material", "Structured notes, evidence and proposed actions", "Original source and capture time are retained.")
            IntentDomain.AUTOMATION -> node("domain", plan.steps.getOrNull(2)?.title ?: "Map runtime and safe actions", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.ANDROID_APP, "runtime.android.accessibility", listOf("android.launch_app", "android.observe_visible_labels", "android.verify_visible_text"), listOf("scope", "context"), "Target app, requested action and permission state", "Bounded action proposal and observable postcondition", "Target app and required permission are verified before action.")
            IntentDomain.GENERAL -> node("domain", plan.steps.getOrNull(2)?.title ?: "Break goal into deliverables", WorkflowNodeKind.DOMAIN_WORK, WorkflowParticipant.DETERMINISTIC, "app.jobs", listOf("job.create", "job.update_step"), listOf("scope", "context"), "Goal and constraints", "Ordered deliverables and dependencies", "Every deliverable has a done condition.")
        }
        val nodes = mutableListOf<WorkflowNodeSpec>()
        nodes += node("scope", "Frame the outcome and constraints", WorkflowNodeKind.SCOPE, WorkflowParticipant.DETERMINISTIC, "app.jobs", emptyList(), emptyList(), "Original user intent", "Outcome, constraints, deadline and definition of done", "Goal and material unknowns are explicit.")
        nodes += node("context", "Retrieve relevant Memo context", WorkflowNodeKind.CONTEXT, WorkflowParticipant.DETERMINISTIC, "app.entities", listOf("entity.propose"), emptyList(), "Goal and eligible workspace scope", "Relevant records with stable IDs and provenance", "Only permission-eligible records are included.")
        val routedDomainNode = when (domainNode.participant) {
            WorkflowParticipant.BROWSER -> if ("browser.inspect_page" in available) domainNode else domainNode.copy(participant = WorkflowParticipant.USER, runtimeId = "app.jobs")
            WorkflowParticipant.LOCAL_MODEL -> if ("model.generate" in available) domainNode else domainNode.copy(participant = WorkflowParticipant.USER, runtimeId = "app.jobs")
            WorkflowParticipant.ANDROID_APP -> if ("android.observe_visible_labels" in available) domainNode else domainNode.copy(participant = WorkflowParticipant.USER, runtimeId = "app.jobs")
            else -> domainNode
        }
        nodes += routedDomainNode
        nodes += node("artifact", "Produce the working artifact or result", WorkflowNodeKind.ARTIFACT, if ("model.generate" in available) WorkflowParticipant.LOCAL_MODEL else WorkflowParticipant.USER, if ("model.generate" in available) "app.models" else "app.jobs", if ("model.generate" in available) listOf("model.generate", "job.update_step").filter { it in available } else emptyList(), listOf("domain"), "Validated domain outputs", "Editable deliverable with provenance", "The actual artifact is saved; unavailable capabilities are reported.")
        nodes += node("verify", "Verify the result against the goal", WorkflowNodeKind.VERIFICATION, WorkflowParticipant.DETERMINISTIC, "app.jobs", listOf("job.verify").filter { it in available }, listOf("artifact"), "Deliverable and acceptance conditions", "Verification report with observed evidence", "Each acceptance condition is checked against an observable result.")
        if (plan.requiresExternalApproval) {
            val approvalCapability = if (plan.domain == IntentDomain.AUTOMATION) "android.tap_exact_text" else "browser.prepare_prompt"
            nodes += node("approval", "Review exact external action", WorkflowNodeKind.APPROVAL, WorkflowParticipant.USER, "app.approvals", listOf(approvalCapability), listOf("verify"), "Exact content, destination, capability and risk", "One-use approval bound to content hash and destination", "No external write occurs before explicit current approval.", approval = true, externalWrite = true)
        }
        val reviewDependency = if (plan.requiresExternalApproval) listOf("approval") else listOf("verify")
        nodes += node("review", "Review, accept or revise", WorkflowNodeKind.REVIEW, WorkflowParticipant.USER, "app.jobs", emptyList(), reviewDependency, "Artifact, evidence and unresolved limitations", "User decision and follow-up work", "The user explicitly accepts, revises or defers the outcome.")
        val graph = WorkflowGraph(goal = plan.goal, nodes = nodes)
        WorkflowGraphValidator.validate(graph)
        return graph
    }

    private fun node(
        id: String, title: String, kind: WorkflowNodeKind, participant: WorkflowParticipant, runtimeId: String,
        required: List<String>, dependencies: List<String>, input: String, output: String, postcondition: String,
        approval: Boolean = false, externalWrite: Boolean = false
    ) = WorkflowNodeSpec(
        id = id, title = title.take(240), kind = kind, participant = participant, runtimeId = runtimeId,
        requiredCapabilities = required.distinct(), dependsOn = dependencies,
        inputContract = input.take(1_000), outputContract = output.take(1_000), postcondition = postcondition.take(1_000),
        requiresApproval = approval, externalWrite = externalWrite, timeoutMs = 120_000, maxRetries = 0,
        idempotencyKey = sha256("$id|$title|${UUID.randomUUID()}")
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8)).joinToString("") { "%02x".format(it) }
}
