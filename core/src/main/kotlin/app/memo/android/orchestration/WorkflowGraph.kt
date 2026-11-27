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

/**
 * One bounded unit of work in a Memo workflow.
 *
 * This is a proposal contract, not an executable command. Runtime dispatch, permission checks,
 * approvals and postcondition verification remain separate deterministic boundaries.
 */
data class TaskSpec(
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
    /** Derived by the compiler from registered runtime policy; never trusted from model JSON. */
    val externalSideEffect: Boolean = false,
    val timeoutMs: Long = 120_000,
    val maxRetries: Int = 0,
    val idempotencyKey: String
)

/** Source compatibility for existing v1 graph readers and callers. */
typealias WorkflowNodeSpec = TaskSpec

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
                        put("externalSideEffect", node.externalSideEffect)
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
            require(raw.isNotBlank() && raw.length <= 64_000) {
                "Workflow graph is empty or exceeds its storage limit."
            }
            val root = JSONObject(raw)
            val schemaVersion = requiredInt(root, "schemaVersion")
            val goal = requiredString(root, "goal", 8_000)
            val budgetRow = root.optJSONObject("budget")
                ?: throw IllegalArgumentException("Workflow graph budget is missing.")
            val nodeRows = root.optJSONArray("nodes")
                ?: throw IllegalArgumentException("Workflow graph nodes are missing.")
            require(nodeRows.length() in 1..40) {
                "Workflow graph must contain between 1 and 40 nodes."
            }
            // externalSideEffect was added after schema-v1 graphs had already been persisted.
            // Accept the original representation only when every node omits the field; mixed
            // representations are ambiguous and must be rejected.
            val sideEffectFieldPresence = (0 until nodeRows.length()).map { index ->
                nodeRows.getJSONObject(index).has("externalSideEffect")
            }
            val legacySideEffectFormat = sideEffectFieldPresence.none { it }
            require(legacySideEffectFormat || sideEffectFieldPresence.all { it }) {
                "Workflow graph mixes legacy and current side-effect classification fields."
            }
            val decodedNodes = (0 until nodeRows.length()).map { index ->
                val row = nodeRows.optJSONObject(index)
                    ?: throw IllegalArgumentException("Workflow node ${index + 1} must be an object.")
                WorkflowNodeSpec(
                    id = requiredString(row, "id", 64),
                    title = requiredString(row, "title", 240),
                    kind = requiredEnum<WorkflowNodeKind>(row, "kind"),
                    participant = requiredEnum<WorkflowParticipant>(row, "participant"),
                    runtimeId = requiredString(row, "runtimeId", 120),
                    requiredCapabilities = requiredStringArray(row, "requiredCapabilities", 20, 120),
                    dependsOn = requiredStringArray(row, "dependsOn", 40, 64),
                    inputContract = requiredString(row, "inputContract", 1_000),
                    outputContract = requiredString(row, "outputContract", 1_000),
                    postcondition = requiredString(row, "postcondition", 1_000),
                    requiresApproval = requiredBoolean(row, "requiresApproval"),
                    externalWrite = requiredBoolean(row, "externalWrite"),
                    externalSideEffect = if (legacySideEffectFormat) false else requiredBoolean(row, "externalSideEffect"),
                    timeoutMs = requiredLong(row, "timeoutMs"),
                    maxRetries = requiredInt(row, "maxRetries"),
                    idempotencyKey = requiredString(row, "idempotencyKey", 64)
                )
            }
            // Before externalSideEffect was persisted, schema-v1 approval semantics matched a
            // required action to its explicit externalWrite checkpoint. Reconstruct that legacy
            // classification from the dependency shape; never infer it for current-format graphs,
            // where the compiler-derived field is mandatory.
            val nodes = if (legacySideEffectFormat) {
                val nodesById = decodedNodes.associateBy { it.id }
                decodedNodes.map { node ->
                    val hasLegacyExternalWriteGate = node.kind != WorkflowNodeKind.APPROVAL &&
                        node.requiresApproval && node.dependsOn.any { dependency ->
                            val gate = nodesById[dependency]
                            gate?.kind == WorkflowNodeKind.APPROVAL && gate.requiresApproval &&
                                gate.externalWrite && gate.participant == WorkflowParticipant.USER &&
                                gate.runtimeId == "app.approvals" && gate.maxRetries == 0
                        }
                    node.copy(externalSideEffect = hasLegacyExternalWriteGate)
                }
            } else {
                decodedNodes
            }
            val graph = WorkflowGraph(
                schemaVersion = schemaVersion,
                goal = goal,
                nodes = nodes,
                budget = WorkflowBudget(
                    maxNodes = requiredInt(budgetRow, "maxNodes"),
                    maxDurationMs = requiredLong(budgetRow, "maxDurationMs"),
                    maxModelCalls = requiredInt(budgetRow, "maxModelCalls"),
                    maxExternalWrites = requiredInt(budgetRow, "maxExternalWrites")
                )
            )
            WorkflowGraphValidator.validate(graph)
            return graph
        }

        private fun requiredString(row: JSONObject, key: String, maxLength: Int): String {
            val value = row.opt(key)
            require(value is String && value.isNotBlank() && value.length <= maxLength) {
                "Workflow field '$key' must be non-empty text of at most $maxLength characters."
            }
            return value
        }

        private fun requiredStringArray(
            row: JSONObject,
            key: String,
            maxItems: Int,
            maxLength: Int
        ): List<String> {
            val values = row.optJSONArray(key)
                ?: throw IllegalArgumentException("Workflow field '$key' must be an array.")
            require(values.length() <= maxItems) {
                "Workflow field '$key' contains too many entries."
            }
            return (0 until values.length()).map { index ->
                val value = values.opt(index)
                require(value is String && value.isNotBlank() && value.length <= maxLength) {
                    "Workflow field '$key' contains an invalid text entry."
                }
                value
            }
        }

        private fun requiredBoolean(row: JSONObject, key: String): Boolean {
            val value = row.opt(key)
            require(value is Boolean) { "Workflow field '$key' must be boolean." }
            return value
        }

        private fun requiredLong(row: JSONObject, key: String): Long {
            val value = row.opt(key)
            require(value is Number) { "Workflow field '$key' must be an integer." }
            val numeric = value.toDouble()
            require(
                numeric.isFinite() && numeric % 1.0 == 0.0 &&
                    numeric >= Long.MIN_VALUE.toDouble() && numeric <= Long.MAX_VALUE.toDouble()
            ) { "Workflow field '$key' must be a whole number." }
            val result = value.toLong()
            require(result.toDouble() == numeric) { "Workflow field '$key' is outside the supported integer range." }
            return result
        }

        private fun requiredInt(row: JSONObject, key: String): Int {
            val value = requiredLong(row, key)
            require(value in Int.MIN_VALUE.toLong()..Int.MAX_VALUE.toLong()) {
                "Workflow field '$key' is outside the supported integer range."
            }
            return value.toInt()
        }

        private inline fun <reified T : Enum<T>> requiredEnum(row: JSONObject, key: String): T {
            val value = requiredString(row, key, 40)
            return runCatching { enumValueOf<T>(value) }
                .getOrElse { throw IllegalArgumentException("Workflow field '$key' contains an unsupported value.") }
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
        val modelCalls = graph.nodes.count { it.participant == WorkflowParticipant.LOCAL_MODEL || it.participant == WorkflowParticipant.FRONTIER_WEB }
        require(modelCalls <= graph.budget.maxModelCalls) { "Workflow model-call budget exceeded." }
        require(graph.nodes.sumOf { it.timeoutMs } <= graph.budget.maxDurationMs) { "Workflow total timeout exceeds its duration budget." }
        val ids = graph.nodes.map { it.id }
        require(ids.distinct().size == ids.size) { "Workflow node IDs must be unique." }
        val known = ids.toSet()
        val byId = graph.nodes.associateBy { it.id }
        graph.nodes.forEach { node ->
            require(node.id.matches(Regex("[a-z0-9][a-z0-9._-]{0,63}"))) { "Invalid workflow node ID." }
            require(node.title.isNotBlank() && node.title.length <= 240) { "Workflow node title is missing or too long." }
            require(node.runtimeId.matches(Regex("[a-z0-9][a-z0-9._-]{0,119}"))) { "Workflow task runtime is missing or invalid." }
            require(node.inputContract.isNotBlank() && node.outputContract.isNotBlank() && node.inputContract.length <= 1_000 && node.outputContract.length <= 1_000 && node.postcondition.isNotBlank() && node.postcondition.length <= 1_000) { "Workflow task input, output and postcondition contracts are required and bounded." }
            require(node.requiredCapabilities.distinct().size == node.requiredCapabilities.size) { "Workflow task capability requirements must be unique." }
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
            if (node.externalSideEffect) require(
                node.requiresApproval && node.kind != WorkflowNodeKind.APPROVAL &&
                    node.requiredCapabilities.isNotEmpty() && node.maxRetries == 0
            ) { "External side-effect tasks must be approval-gated and non-retrying." }
            if (node.requiresApproval && node.kind != WorkflowNodeKind.APPROVAL) {
                require(node.maxRetries == 0) { "Approval-gated task '${node.id}' cannot be automatically retried." }
                require(node.dependsOn.any { dependency ->
                    val gate = byId[dependency]
                    gate?.kind == WorkflowNodeKind.APPROVAL && gate.requiresApproval &&
                        gate.externalWrite == node.externalSideEffect &&
                        gate.participant == WorkflowParticipant.USER && gate.runtimeId == "app.approvals" && gate.maxRetries == 0
                }) { "Approval-gated task '${node.id}' must depend on a matching dedicated approval checkpoint." }
            }

        }
        require(graph.nodes.count { it.externalWrite } <= graph.budget.maxExternalWrites) { "Workflow external-write budget exceeded." }
    }
}

object WorkflowGraphCompiler {
    /**
     * Compile a planner-proposed task list into a validated dependency graph.
     *
     * Capability IDs are checked against the exact runtime that will execute each task.
     * A matching capability on a different runtime is not interchangeable.
     */
    fun compile(
        goal: String,
        tasks: List<TaskSpec>,
        manifest: CapabilityManifest,
        budget: WorkflowBudget = WorkflowBudget()
    ): WorkflowGraph {
        require(manifest.unresolvedRequirements.isEmpty()) {
            "The capability manifest contains unresolved requirements; the workflow cannot be compiled."
        }
        require(tasks.isNotEmpty()) { "A workflow needs at least one task." }
        val capabilitiesByRuntime = manifest.capabilities.associateBy { it.runtimeId to it.capabilityId }
        // Side-effect classification is derived from the trusted runtime registry, never from model JSON.
        val resolvedTasks = tasks.map { task ->
            val externalSideEffect = task.requiredCapabilities.any { capabilityId ->
                capabilitiesByRuntime[task.runtimeId to capabilityId]?.externalSideEffect == true
            }
            task.copy(externalSideEffect = externalSideEffect)
        }
        val tasksById = resolvedTasks.associateBy { it.id }
        resolvedTasks.forEach { task ->
            require(task.runtimeId in manifest.registeredRuntimeIds) {
                "Task '${task.id}' targets an unregistered runtime '${task.runtimeId}'."
            }
            task.requiredCapabilities.forEach { capabilityId ->
                val capability = capabilitiesByRuntime[task.runtimeId to capabilityId]
                    ?: throw IllegalArgumentException("Task '${task.id}' requires a capability that is not registered on runtime '${task.runtimeId}'.")
                if (capability.requiresUserApproval) {
                    require(task.requiresApproval) {
                        "Task '${task.id}' uses approval-gated capability '${capabilityId}' without an explicit approval checkpoint."
                    }
                }
                if (capability.externalSideEffect) {
                    require(task.requiresApproval) {
                        "External side-effect task '${task.id}' must require explicit approval."
                    }
                }
            }
            if (task.requiresApproval && task.kind != WorkflowNodeKind.APPROVAL) {
                require(task.maxRetries == 0) {
                    "Approval-gated task '${task.id}' cannot be automatically retried."
                }
                require(task.dependsOn.any { dependency ->
                    val gate = tasksById[dependency]
                    gate?.kind == WorkflowNodeKind.APPROVAL && gate.requiresApproval &&
                        gate.externalWrite == task.externalSideEffect &&
                        gate.participant == WorkflowParticipant.USER && gate.runtimeId == "app.approvals" && gate.maxRetries == 0
                }) {
                    "Task '${task.id}' must depend on a dedicated approval checkpoint before execution."
                }
            }
            when (task.participant) {
                WorkflowParticipant.LOCAL_MODEL -> require(task.runtimeId == "app.models") {
                    "Local-model tasks must target the registered app.models runtime."
                }
                WorkflowParticipant.FRONTIER_WEB, WorkflowParticipant.BROWSER -> require(task.runtimeId == "runtime.browser") {
                    "Browser and frontier-web tasks must target runtime.browser."
                }
                WorkflowParticipant.ANDROID_APP -> require(task.runtimeId == "runtime.android.accessibility") {
                    "Android-app tasks must target the accessibility runtime."
                }
                WorkflowParticipant.DETERMINISTIC -> require(task.runtimeId.startsWith("app.")) {
                    "Deterministic tasks must target a Memo app runtime."
                }
                WorkflowParticipant.USER -> require(task.runtimeId in setOf("app.jobs", "app.approvals")) {
                    "User checkpoints must target the Memo job or approval runtime."
                }
            }
        }
        val graph = WorkflowGraph(goal = goal, nodes = resolvedTasks, budget = budget)
        WorkflowGraphValidator.validate(graph)
        return graph
    }

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
            // The capability belongs to the external runtime in the manifest, not to the human approval inbox.
            nodes += node("approval", "Review exact external action", WorkflowNodeKind.APPROVAL, WorkflowParticipant.USER, "app.approvals", emptyList(), listOf("verify"), "Exact content, destination, capability and risk", "One-use approval bound to content hash and destination", "No external write occurs before explicit current approval.", approval = true, externalWrite = true)
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
