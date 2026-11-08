package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeDescriptor
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.UUID

/**
 * Converts an untrusted model proposal into a validated, inspectable workflow graph.
 *
 * The parser never dispatches a task. Runtime capabilities are resolved against their exact
 * registered runtime, and execution, permissions, approval and postcondition checks stay outside
 * this proposal boundary.
 */
object WorkflowTaskProposalParser {
    private const val MAX_RAW_CHARS = 64_000
    private const val MAX_TASKS = 40
    private const val MAX_LIST_ITEMS = 20

    fun parse(
        raw: String,
        sourceIntent: String,
        registeredRuntimes: List<RuntimeDescriptor>,
        budget: WorkflowBudget = WorkflowBudget()
    ): WorkflowGraph {
        require(raw.isNotBlank() && raw.length <= MAX_RAW_CHARS) {
            "The workflow proposal is empty or exceeds its size limit."
        }
        val root = JSONObject(raw)
        require(root.optInt("schemaVersion", 0) == 1) { "Unsupported task proposal schema." }
        val goal = root.optString("goal", "").trim()
        require(goal.isNotBlank() && goal.length <= 8_000) { "The proposed workflow goal is missing or too long." }
        val rows = root.optJSONArray("tasks")
            ?: throw IllegalArgumentException("The workflow proposal must contain a tasks array.")
        require(rows.length() in 1..MAX_TASKS) { "The workflow proposal must contain between 1 and 40 tasks." }

        // Model output never supplies idempotency keys. Each accepted proposal gets fresh,
        // app-generated keys so a regenerated plan cannot accidentally reuse prior execution IDs.
        val proposalNonce = UUID.randomUUID().toString()
        val tasks = (0 until rows.length()).map { index ->
            val row = rows.optJSONObject(index)
                ?: throw IllegalArgumentException("Task ${index + 1} must be an object.")
            val id = boundedString(row, "id", 64)
            val title = boundedString(row, "title", 240)
            val kind = enumValue<WorkflowNodeKind>(row, "kind")
            val participant = enumValue<WorkflowParticipant>(row, "participant")
            val runtimeId = boundedString(row, "runtimeId", 120)
            val capabilities = stringArray(row, "requiredCapabilities", MAX_LIST_ITEMS, 120)
            val dependencies = stringArray(row, "dependsOn", MAX_TASKS, 64)
            val inputContract = boundedString(row, "inputContract", 1_000)
            val outputContract = boundedString(row, "outputContract", 1_000)
            val postcondition = boundedString(row, "postcondition", 1_000)
            val timeoutMs = number(row, "timeoutMs").toLong()
            val maxRetries = number(row, "maxRetries").toInt()
            val requiresApproval = boolean(row, "requiresApproval")
            val externalWrite = boolean(row, "externalWrite")
            val keyMaterial = listOf(
                proposalNonce, id, title, kind.name, participant.name, runtimeId,
                capabilities.sorted().joinToString(","), dependencies.sorted().joinToString(","),
                inputContract, outputContract, postcondition
            ).joinToString("|")
            TaskSpec(
                id = id,
                title = title,
                kind = kind,
                participant = participant,
                runtimeId = runtimeId,
                requiredCapabilities = capabilities,
                dependsOn = dependencies,
                inputContract = inputContract,
                outputContract = outputContract,
                postcondition = postcondition,
                requiresApproval = requiresApproval,
                externalWrite = externalWrite,
                timeoutMs = timeoutMs,
                maxRetries = maxRetries,
                idempotencyKey = sha256(keyMaterial)
            )
        }

        val manifest = CapabilityCompiler.compileForTasks(sourceIntent, tasks, registeredRuntimes)
        require(manifest.unresolvedRequirements.isEmpty()) {
            "The workflow requests unavailable runtime capabilities: ${manifest.unresolvedRequirements.joinToString(", ")}."
        }
        return WorkflowGraphCompiler.compile(goal, tasks, manifest, budget)
    }

    private fun boundedString(row: JSONObject, key: String, maxLength: Int): String {
        val value = row.opt(key)
        require(value is String) { "Task field '$key' must be text." }
        val clean = value.trim()
        require(clean.isNotBlank() && clean.length <= maxLength) {
            "Task field '$key' is missing or exceeds its $maxLength-character limit."
        }
        return clean
    }

    private fun stringArray(row: JSONObject, key: String, maxItems: Int, maxLength: Int): List<String> {
        val values = row.optJSONArray(key)
            ?: throw IllegalArgumentException("Task field '$key' must be an array.")
        require(values.length() <= maxItems) { "Task field '$key' contains too many entries." }
        return (0 until values.length()).map { index ->
            val value = values.opt(index)
            require(value is String) { "Task field '$key' entries must be text." }
            value.trim().also { require(it.isNotBlank() && it.length <= maxLength) { "Task field '$key' contains an invalid entry." } }
        }
    }

    private fun number(row: JSONObject, key: String): Number {
        val value = row.opt(key)
        require(value is Number) { "Task field '$key' must be numeric." }
        return value
    }

    private fun boolean(row: JSONObject, key: String): Boolean {
        val value = row.opt(key)
        require(value is Boolean) { "Task field '$key' must be boolean." }
        return value
    }

    private inline fun <reified T : Enum<T>> enumValue(row: JSONObject, key: String): T {
        val value = boundedString(row, key, 40)
        return runCatching { enumValueOf<T>(value) }
            .getOrElse { throw IllegalArgumentException("Task field '$key' contains an unsupported value.") }
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
