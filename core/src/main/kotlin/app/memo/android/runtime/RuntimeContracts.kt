package app.memo.android.runtime

/** Interaction domains are intentionally distinct: capabilities are never inferred across runtimes. */
enum class RuntimeKind { APP, BROWSER, EXTERNAL_ANDROID }

data class RuntimeCapability(
    val id: String,
    val label: String,
    val description: String,
    val requiresUserApproval: Boolean = false
)

data class RuntimeDescriptor(
    val id: String,
    val name: String,
    val kind: RuntimeKind,
    val description: String,
    val capabilities: List<RuntimeCapability>,
    val status: String = "AVAILABLE"
)

/** Native Memo workspaces and the embedded browser are registered independently. */
class RuntimeRegistry {
    private val appRuntime = listOf(
        RuntimeDescriptor("app.chats", "AI Chats", RuntimeKind.APP, "Persistent conversations, source capture and model handoffs.", listOf(
            RuntimeCapability("chat.create", "Create conversation", "Create a durable local conversation."),
            RuntimeCapability("chat.append_message", "Append message", "Persist a user, model or imported message."),
            RuntimeCapability("chat.link_entities", "Link entities", "Connect a conversation to structured outcomes.")
        )),
        RuntimeDescriptor("app.entities", "Work & entities", RuntimeKind.APP, "Typed entities, relationships, source evidence and review.", listOf(
            RuntimeCapability("entity.propose", "Propose entity", "Create a source-linked proposal for user review."),
            RuntimeCapability("entity.create", "Create entity", "Create a user-authorized entity in the workspace."),
            RuntimeCapability("entity.approve", "Approve entity", "Commit a reviewed proposal."),
            RuntimeCapability("entity.link", "Create relationship", "Link two existing entities with a typed relation.")
        )),
        RuntimeDescriptor("app.jobs", "Job workbench", RuntimeKind.APP, "Durable job plans, step state, checkpoints and results.", listOf(
            RuntimeCapability("job.create", "Create job", "Persist an execution plan."),
            RuntimeCapability("job.update_step", "Update step", "Record an observed step transition."),
            RuntimeCapability("job.verify", "Record verification", "Save the basis for a verified result.")
        )),
        RuntimeDescriptor("app.models", "Models", RuntimeKind.APP, "Manage installed local models and explicitly configured providers.", listOf(
            RuntimeCapability("model.list", "List models", "Read currently configured model availability."),
            RuntimeCapability("model.generate", "Generate response", "Generate through a selected model and its configured permissions.", true)
        )),
        RuntimeDescriptor("app.council", "Council", RuntimeKind.APP, "Traceable multi-model analysis and retained disagreement.", listOf(
            RuntimeCapability("council.run", "Run Council", "Execute a selected model workflow with per-run remote consent.", true)
        )),
        RuntimeDescriptor("app.settings", "Settings", RuntimeKind.APP, "Workspace security, backups and appearance.", listOf(
            RuntimeCapability("workspace.export", "Export workspace", "Write a user-selected readable backup."),
            RuntimeCapability("workspace.lock", "Lock workspace", "Clear sensitive state from application memory.")
        ))
    )

    private val externalAndroidRuntime = RuntimeDescriptor(
        id = "runtime.android.accessibility",
        name = "External Android apps",
        kind = RuntimeKind.EXTERNAL_ANDROID,
        description = "User-enabled accessibility control over the real foreground Android app. It is not app virtualization and never operates while the device is locked.",
        status = "OPTIONAL_PERMISSION",
        capabilities = listOf(
            RuntimeCapability("android.launch_app", "Launch installed app", "Open a launchable app selected by the user."),
            RuntimeCapability("android.observe_visible_labels", "Observe visible labels", "Read exposed non-editable labels only after Accessibility is enabled."),
            RuntimeCapability("android.tap_exact_text", "Tap exact text", "Tap a unique visible label in the selected target app.", true),
            RuntimeCapability("android.verify_visible_text", "Verify visible outcome", "Require a visible-text postcondition before a run is marked verified."),
            RuntimeCapability("android.capture_screen_context", "Capture accessible screen labels", "Capture only after a user taps Capture; editable values and passwords are excluded.", true)
        )
    )

    private val browserRuntime = RuntimeDescriptor(
        id = "runtime.browser",
        name = "Browser runtime",
        kind = RuntimeKind.BROWSER,
        description = "Visible website sessions, frontier-chat interaction, page inspection, prompt submission and bounded browser workflows. This remains separate from the native Memo app runtime.",
        capabilities = listOf(
            RuntimeCapability("browser.navigate_https", "Navigate HTTPS", "Open a user-selected HTTPS page."),
            RuntimeCapability("browser.inspect_page", "Inspect visible page", "Read supported page content through browser-side scripts."),
            RuntimeCapability("browser.capture_evidence", "Capture evidence", "Save inspected content with source URL and timestamp."),
            RuntimeCapability("browser.prepare_prompt", "Prepare prompt", "Fill a supported frontier-chat composer for review.", true),
            RuntimeCapability("browser.submit_prompt", "Submit prompt", "Send a user-approved prompt through the visible provider website; provider UI compatibility is checked first.", true),
            RuntimeCapability("browser.capture_response", "Capture response", "Capture a new, stable assistant answer from the current provider page."),
            RuntimeCapability("browser.run_multi_chat_workflow", "Run multi-chat workflow", "Coordinate multiple frontier chat websites in a visible, sequential run with input/output linking and user takeover.", true)
        )
    )

    fun listRuntimes(): List<RuntimeDescriptor> = appRuntime + browserRuntime + externalAndroidRuntime
    fun listNativeApps(): List<RuntimeDescriptor> = appRuntime
    fun browser(): RuntimeDescriptor = browserRuntime
    fun resolve(runtimeId: String): RuntimeDescriptor? = listRuntimes().firstOrNull { it.id == runtimeId }
    fun can(runtimeId: String, capabilityId: String): Boolean = resolve(runtimeId)?.capabilities?.any { it.id == capabilityId } == true
}
