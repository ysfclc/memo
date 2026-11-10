package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeDescriptor
import java.security.MessageDigest

/**
 * Compiles an intent plan into a deterministic, inspectable capability manifest.
 * It only selects registered capabilities; it never grants permissions or executes actions.
 */
data class CompiledCapability(
    val runtimeId: String,
    val runtimeName: String,
    val capabilityId: String,
    val label: String,
    val requiresUserApproval: Boolean,
    val externalSideEffect: Boolean,
    val runtimeStatus: String
)

data class CapabilityManifest(
    val schemaVersion: Int,
    val domainId: String,
    val sourceIntentSha256: String,
    val capabilities: List<CompiledCapability>,
    val unresolvedRequirements: List<String>,
    val requiresUserApproval: Boolean,
    val manifestSha256: String,
    val registeredRuntimeIds: Set<String> = emptySet()
) {
    val isRunnableForLocalPlanning: Boolean get() = unresolvedRequirements.isEmpty()
}

object CapabilityCompiler {
    private val common = listOf(
        "app.jobs" to "job.create",
        "app.jobs" to "job.update_step",
        "app.jobs" to "job.verify",
        "app.entities" to "entity.propose"
    )
    private val routes = mapOf(
        IntentDomain.GENERAL to common + listOf(
            "app.chats" to "chat.create", "app.chats" to "chat.append_message",
            "app.models" to "model.list", "app.models" to "model.generate"
        ),
        IntentDomain.RESEARCH to common + listOf(
            "runtime.browser" to "browser.navigate_https", "runtime.browser" to "browser.inspect_page",
            "runtime.browser" to "browser.capture_evidence", "app.models" to "model.generate"
        ),
        IntentDomain.BUILD to common + listOf(
            "app.models" to "model.list", "app.models" to "model.generate",
            "runtime.browser" to "browser.navigate_https", "runtime.browser" to "browser.inspect_page"
        ),
        IntentDomain.CAREER to common + listOf(
            "app.chats" to "chat.create", "app.chats" to "chat.append_message", "app.models" to "model.generate"
        ),
        IntentDomain.CONTENT to common + listOf(
            "app.chats" to "chat.create", "app.chats" to "chat.append_message", "app.models" to "model.generate"
        ),
        IntentDomain.CRM to common + listOf(
            "app.entities" to "entity.create", "app.entities" to "entity.link", "app.chats" to "chat.append_message"
        ),
        IntentDomain.CAPTURE to common + listOf(
            "app.chats" to "chat.create", "app.chats" to "chat.append_message"
        ),
        IntentDomain.AUTOMATION to common + listOf(
            "runtime.android.accessibility" to "android.launch_app",
            "runtime.android.accessibility" to "android.observe_visible_labels",
            "runtime.android.accessibility" to "android.verify_visible_text"
        )
    )

    /**
     * Assemble only the capabilities explicitly required by a proposed TaskSpec graph.
     * Requirements are resolved against the exact runtime ID; capability names are not global.
     */
    fun compileForTasks(
        sourceIntent: String,
        tasks: List<TaskSpec>,
        registeredRuntimes: List<RuntimeDescriptor>
    ): CapabilityManifest {
        require(sourceIntent.isNotBlank() && sourceIntent.length <= 8_000) { "Workflow intent is missing or too long." }
        require(tasks.isNotEmpty() && tasks.size <= 40) { "Task capability compilation requires 1 to 40 tasks." }
        require(registeredRuntimes.map { it.id }.distinct().size == registeredRuntimes.size) {
            "The runtime registry contains duplicate runtime IDs."
        }
        val runtimes = registeredRuntimes.associateBy { it.id }
        registeredRuntimes.forEach { runtime ->
            require(runtime.capabilities.map { it.id }.distinct().size == runtime.capabilities.size) {
                "Runtime '${runtime.id}' contains duplicate capability IDs."
            }
        }
        val requirements = tasks.flatMap { task ->
            require(task.runtimeId in runtimes) { "Task '${task.id}' targets an unregistered runtime '${task.runtimeId}'." }
            task.requiredCapabilities.map { task.runtimeId to it }
        }.distinct().sortedWith(compareBy<Pair<String, String>> { it.first }.thenBy { it.second })
        val chosen = mutableListOf<CompiledCapability>()
        val missing = mutableListOf<String>()
        requirements.forEach { (runtimeId, capabilityId) ->
            val runtime = runtimes[runtimeId]
            val capability = runtime?.capabilities?.firstOrNull { it.id == capabilityId }
            if (runtime == null || capability == null) {
                missing += "$runtimeId:$capabilityId"
            } else {
                chosen += CompiledCapability(
                    runtimeId = runtime.id,
                    runtimeName = runtime.name,
                    capabilityId = capability.id,
                    label = capability.label,
                    requiresUserApproval = capability.requiresUserApproval,
                    externalSideEffect = capability.externalSideEffect,
                    runtimeStatus = runtime.status
                )
            }
        }
        val approval = chosen.any { it.requiresUserApproval } || tasks.any { it.requiresApproval }
        val sourceHash = sha256(sourceIntent.trim())
        val manifestPayload = buildString {
            append("schema=1\nmode=task-spec\nsource=").append(sourceHash).append('\n')
            chosen.forEach { capability ->
                append(capability.runtimeId).append('|').append(capability.capabilityId).append('|')
                    .append(capability.requiresUserApproval).append('|').append(capability.externalSideEffect)
                    .append('|').append(capability.runtimeStatus).append('\n')
            }
            missing.forEach { append("missing|").append(it).append('\n') }
            append("approval=").append(approval)
        }
        return CapabilityManifest(
            schemaVersion = 1,
            domainId = "task-spec",
            sourceIntentSha256 = sourceHash,
            capabilities = chosen,
            unresolvedRequirements = missing,
            requiresUserApproval = approval,
            manifestSha256 = sha256(manifestPayload),
            registeredRuntimeIds = registeredRuntimes.map { it.id }.toSet()
        )
    }

    fun compile(
        plan: IntentPlan,
        registeredRuntimes: List<RuntimeDescriptor>,
        sourceIntent: String = plan.goal
    ): CapabilityManifest {
        require(registeredRuntimes.map { it.id }.distinct().size == registeredRuntimes.size) {
            "The runtime registry contains duplicate runtime IDs."
        }
        registeredRuntimes.forEach { runtime ->
            require(runtime.capabilities.map { it.id }.distinct().size == runtime.capabilities.size) {
                "Runtime '${runtime.id}' contains duplicate capability IDs."
            }
        }
        val runtimes = registeredRuntimes.associateBy { it.id }
        val desired = routes[plan.domain].orEmpty().distinct().toMutableList()
        if (plan.requiresExternalApproval) {
            // This is deliberately only a proposal/approval capability. It does not execute the write.
            desired += if (plan.domain == IntentDomain.AUTOMATION) {
                "runtime.android.accessibility" to "android.tap_exact_text"
            } else {
                "runtime.browser" to "browser.prepare_prompt"
            }
        }
        val chosen = desired.mapNotNull { (runtimeId, id) ->
            val runtime = runtimes[runtimeId] ?: return@mapNotNull null
            val capability = runtime.capabilities.firstOrNull { it.id == id } ?: return@mapNotNull null
            CompiledCapability(
                runtimeId = runtime.id,
                runtimeName = runtime.name,
                capabilityId = capability.id,
                label = capability.label,
                requiresUserApproval = capability.requiresUserApproval ||
                    (runtimeId to id) in setOf("runtime.browser" to "browser.prepare_prompt", "runtime.android.accessibility" to "android.tap_exact_text"),
                externalSideEffect = capability.externalSideEffect,
                runtimeStatus = runtime.status
            )
        }.sortedWith(compareBy<CompiledCapability> { it.runtimeId }.thenBy { it.capabilityId })
        val requiredMissing = desired.filterNot { (runtimeId, id) ->
            runtimes[runtimeId]?.capabilities?.any { it.id == id } == true
        }.map { (runtimeId, id) -> "$runtimeId:$id" }.sorted()
        val approval = plan.requiresExternalApproval || chosen.any { it.requiresUserApproval }
        val sourceHash = sha256(sourceIntent.trim())
        val manifestPayload = buildString {
            append("schema=1\ndomain=").append(plan.domain.id).append("\nsource=").append(sourceHash).append('\n')
            chosen.forEach { c ->
                append(c.runtimeId).append('|').append(c.capabilityId).append('|')
                    .append(c.requiresUserApproval).append('|').append(c.externalSideEffect)
                    .append('|').append(c.runtimeStatus).append('\n')
            }
            requiredMissing.forEach { append("missing|").append(it).append('\n') }
            append("approval=").append(approval)
        }
        return CapabilityManifest(
            schemaVersion = 1,
            domainId = plan.domain.id,
            sourceIntentSha256 = sourceHash,
            capabilities = chosen,
            unresolvedRequirements = requiredMissing,
            requiresUserApproval = approval,
            manifestSha256 = sha256(manifestPayload),
            registeredRuntimeIds = registeredRuntimes.map { it.id }.toSet()
        )
    }

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
