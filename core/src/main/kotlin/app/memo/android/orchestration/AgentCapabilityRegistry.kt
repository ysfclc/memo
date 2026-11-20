package app.memo.android.orchestration

import app.memo.android.runtime.RuntimeDescriptor

/**
 * Validates executable adapters against runtime ownership and safety policy.
 * Readiness is intentionally recalculated before every model decision.
 */
class AgentCapabilityRegistry(
    runtimes: List<RuntimeDescriptor>,
    adapters: List<AgentCapabilityAdapter>
) {
    private val adaptersById = adapters.associateBy { it.definition.id }

    init {
        val runtimesById = runtimes.associateBy { it.id }
        require(runtimesById.size == runtimes.size) { "Runtime IDs must be unique." }
        require(adaptersById.size == adapters.size) { "Capability IDs must be unique within a run." }

        adapters.forEach { adapter ->
            val definition = adapter.definition
            val runtime = runtimesById[definition.runtimeId]
                ?: throw IllegalArgumentException("Capability ${definition.id} refers to unknown runtime ${definition.runtimeId}.")
            val policy = runtime.capabilities.singleOrNull { it.id == definition.id }
                ?: throw IllegalArgumentException("Capability ${definition.id} has no matching policy entry in runtime ${runtime.id}.")
            require(policy.requiresUserApproval == definition.requiresUserApproval) {
                "Capability ${definition.id} approval policy differs from runtime ${runtime.id}."
            }
            val hasExternalSideEffect =
                definition.executionClass == AgentToolExecutionClass.EXTERNAL_WRITE ||
                    definition.executionClass == AgentToolExecutionClass.DESTRUCTIVE
            require(policy.externalSideEffect == hasExternalSideEffect) {
                "Capability ${definition.id} side-effect classification differs from runtime ${runtime.id}."
            }
        }
    }

    fun resolve(capabilityId: String): AgentCapabilityAdapter? = adaptersById[capabilityId]

    fun availableAdapters(): List<AgentCapabilityAdapter> =
        adaptersById.values.filter { adapter ->
            runCatching { adapter.isAvailable() }.getOrDefault(false)
        }
}
