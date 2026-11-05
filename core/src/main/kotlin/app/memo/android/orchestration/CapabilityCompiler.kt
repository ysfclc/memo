package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeDescriptor
import app.memo.android.runtime.RuntimeKind
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
    val runtimeStatus: String
)

data class CapabilityManifest(
    val schemaVersion: Int,
    val domainId: String,
    val sourceIntentSha256: String,
    val capabilities: List<CompiledCapability>,
    val unresolvedRequirements: List<String>,
    val requiresUserApproval: Boolean,
    val manifestSha256: String
) {
    val isRunnableForLocalPlanning: Boolean get() = unresolvedRequirements.isEmpty()
}

object CapabilityCompiler {
    private val common = listOf("job.create", "job.update_step", "job.verify", "entity.propose")
    private val routes = mapOf(
        IntentDomain.GENERAL to common + listOf("chat.create", "chat.append_message", "model.list", "model.generate"),
        IntentDomain.RESEARCH to common + listOf("browser.navigate_https", "browser.inspect_page", "browser.capture_evidence", "model.generate"),
        IntentDomain.BUILD to common + listOf("model.list", "model.generate", "browser.navigate_https", "browser.inspect_page"),
        IntentDomain.CAREER to common + listOf("chat.create", "chat.append_message", "model.generate"),
        IntentDomain.CONTENT to common + listOf("chat.create", "chat.append_message", "model.generate"),
        IntentDomain.CRM to common + listOf("entity.create", "entity.link", "entity.propose", "chat.append_message"),
        IntentDomain.CAPTURE to common + listOf("chat.create", "chat.append_message", "entity.propose"),
        IntentDomain.AUTOMATION to common + listOf("android.launch_app", "android.observe_visible_labels", "android.verify_visible_text")
    )

    fun compile(
        plan: IntentPlan,
        registeredRuntimes: List<RuntimeDescriptor>,
        sourceIntent: String = plan.goal
    ): CapabilityManifest {
        val byId = registeredRuntimes.flatMap { runtime ->
            runtime.capabilities.map { capability -> Triple(runtime, capability, capability.id) }
        }.associateBy { it.third }
        val desired = routes[plan.domain].orEmpty().distinct().toMutableList()
        if (plan.requiresExternalApproval) {
            // This is deliberately only a proposal/approval capability. It does not execute the write.
            desired += if (plan.domain == IntentDomain.AUTOMATION) "android.tap_exact_text" else "browser.prepare_prompt"
        }
        val chosen = desired.distinct().mapNotNull { id ->
            val (runtime, capability) = byId[id]?.let { it.first to it.second } ?: return@mapNotNull null
            CompiledCapability(
                runtimeId = runtime.id,
                runtimeName = runtime.name,
                capabilityId = capability.id,
                label = capability.label,
                requiresUserApproval = capability.requiresUserApproval || id in setOf("browser.prepare_prompt", "android.tap_exact_text"),
                runtimeStatus = runtime.status
            )
        }.sortedWith(compareBy<CompiledCapability> { it.runtimeId }.thenBy { it.capabilityId })
        val requiredMissing = desired.distinct().filterNot(byId::containsKey).sorted()
        val approval = plan.requiresExternalApproval || chosen.any { it.requiresUserApproval }
        val sourceHash = sha256(sourceIntent.trim())
        val manifestPayload = buildString {
            append("schema=1\ndomain=").append(plan.domain.id).append("\nsource=").append(sourceHash).append('\n')
            chosen.forEach { c ->
                append(c.runtimeId).append('|').append(c.capabilityId).append('|')
                    .append(c.requiresUserApproval).append('|').append(c.runtimeStatus).append('\n')
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
            manifestSha256 = sha256(manifestPayload)
        )
    }

    private fun sha256(text: String): String = MessageDigest.getInstance("SHA-256")
        .digest(text.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
