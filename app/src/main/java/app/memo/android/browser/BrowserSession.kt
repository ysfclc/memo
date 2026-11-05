package app.memo.android.browser

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.SafeBrowsingResponse
import android.webkit.SslErrorHandler
import android.webkit.WebChromeClient
import android.webkit.WebResourceError
import android.webkit.WebResourceRequest
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import app.memo.android.data.ActionApprovalLedger
import app.memo.android.data.EvidenceRecord
import app.memo.android.data.TaskKind
import app.memo.android.data.TaskRecord
import org.json.JSONObject
import org.json.JSONTokener
import java.util.Locale

class TaskCallbacks(
    val onStarted: (String) -> Unit,
    val onCompleted: (String, EvidenceRecord?, String) -> Unit,
    val onNeedsReview: (String, String) -> Unit,
    val onFailed: (String, String, Boolean) -> Unit,
    val onEvidence: (EvidenceRecord) -> Unit,
    val onLog: (String, String) -> Unit
)

@SuppressLint("SetJavaScriptEnabled")
class BrowserSession(
    context: Context,
    private val onVisited: (String, String) -> Unit,
    private val onBrowserError: (String) -> Unit
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    val webView: WebView = WebView(context)

    var currentUrl by mutableStateOf("")
        private set
    var pageTitle by mutableStateOf("New session")
        private set
    var isLoading by mutableStateOf(false)
        private set
    var progress by mutableStateOf(0)
        private set
    var browserMessage by mutableStateOf("Sign in to a site in this browser. Memo never asks for your website password.")
        private set
    var pendingTask by mutableStateOf<TaskRecord?>(null)
        private set
    var canContinueTask by mutableStateOf(false)
        private set
    var isExecutingTask by mutableStateOf(false)
        private set
    var preparedPrompt by mutableStateOf("")
        private set
    var preparedSite by mutableStateOf("")
        private set
    var isSendingPrompt by mutableStateOf(false)
        private set
    var sendMessage by mutableStateOf("")
        private set
    var workflowState by mutableStateOf(BrowserWorkflowUiState())
        private set

    private var workflowProviders: List<BrowserModelProvider> = emptyList()
    private var workflowGoal: String = ""
    private var workflowBrief: String = ""
    private var workflowMode: BrowserWorkflowMode = BrowserWorkflowMode.CONSULT_AND_COMPARE
    private var workflowIndex: Int = 0
    private var workflowPromptSent: Boolean = false
    private var workflowPromptPrepared: Boolean = false
    private var workflowApprovedHash: String = ""
    private var workflowApprovedDestination: String = ""
    private var workflowStepStartedAt: Long = 0L
    private var workflowGeneration: Long = 0L
    private var workflowNavigationCallback: (() -> Unit)? = null
    private var workflowCallbacks: BrowserWorkflowCallbacks? = null

    val canOpenNewConversation: Boolean
        get() = newConversationAddress() != null

    val canPreparePrompt: Boolean
        get() = !isLoading && isAssistantSite(currentUrl)

    private var callbacks: TaskCallbacks? = null
    private var preparedTaskId: String = ""
    private var preparedHost: String = ""
    private var taskGeneration: Long = 0L
    private var disposed = false

    init {
        CookieManager.getInstance().setAcceptCookie(true)
        webView.apply {
            layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
            setBackgroundColor(android.graphics.Color.TRANSPARENT)
            settings.apply {
                javaScriptEnabled = true
                domStorageEnabled = true
                databaseEnabled = true
                javaScriptCanOpenWindowsAutomatically = false
                setSupportMultipleWindows(false)
                allowFileAccess = false
                allowContentAccess = false
                allowFileAccessFromFileURLs = false
                allowUniversalAccessFromFileURLs = false
                mixedContentMode = WebSettings.MIXED_CONTENT_NEVER_ALLOW
                cacheMode = WebSettings.LOAD_DEFAULT
                safeBrowsingEnabled = true
                loadsImagesAutomatically = true
                mediaPlaybackRequiresUserGesture = true
            }
            webChromeClient = object : WebChromeClient() {
                override fun onProgressChanged(view: WebView?, newProgress: Int) {
                    this@BrowserSession.progress = newProgress.coerceIn(0, 100)
                    this@BrowserSession.isLoading = newProgress < 100
                }

                override fun onReceivedTitle(view: WebView?, title: String?) {
                    if (!title.isNullOrBlank()) this@BrowserSession.pageTitle = title.take(240)
                }
            }
            webViewClient = object : WebViewClient() {
                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                    this@BrowserSession.isLoading = true
                    this@BrowserSession.progress = 0
                    if (!url.isNullOrBlank()) {
                        this@BrowserSession.currentUrl = url
                        if (Uri.parse(url).host.orEmpty().equals("accounts.google.com", ignoreCase = true)) {
                            this@BrowserSession.browserMessage = "Google may block sign-in inside embedded browsers. Use Open in browser for Google's supported sign-in flow; its login session stays in that browser."
                        }
                    }
                    if (pendingTask != null && !isExecutingTask) this@BrowserSession.canContinueTask = false
                }

                override fun onPageFinished(view: WebView?, url: String?) {
                    this@BrowserSession.isLoading = false
                    this@BrowserSession.progress = 100
                    val finalUrl = url ?: view?.url.orEmpty()
                    if (finalUrl.isNotBlank()) {
                        this@BrowserSession.currentUrl = finalUrl
                        this@BrowserSession.pageTitle = view?.title?.take(240)?.ifBlank { Uri.parse(finalUrl).host ?: "Website" } ?: "Website"
                        onVisited(finalUrl, this@BrowserSession.pageTitle)
                    }
                    if (pendingTask != null && !isExecutingTask) {
                        this@BrowserSession.canContinueTask = true
                        this@BrowserSession.browserMessage = "Page ready. Sign in if required, then continue the task when you are satisfied the correct account is open."
                    }
                    val workflowContinuation = workflowNavigationCallback
                    if (workflowContinuation != null) {
                        workflowNavigationCallback = null
                        mainHandler.post { runCatching { workflowContinuation() } }
                    }
                }

                override fun shouldOverrideUrlLoading(view: WebView?, request: WebResourceRequest?): Boolean {
                    val uri = request?.url ?: return true
                    val scheme = uri.scheme?.lowercase(Locale.ROOT)
                    if (scheme == "https") return false
                    browserMessage = if (scheme == "http") {
                        "Blocked an insecure HTTP page. Use its HTTPS address instead."
                    } else {
                        "Blocked an external app or unsupported link scheme: ${scheme ?: "unknown"}."
                    }
                    onBrowserError(browserMessage)
                    return true
                }

                override fun onReceivedSslError(view: WebView?, handler: SslErrorHandler?, error: android.net.http.SslError?) {
                    handler?.cancel()
                    browserMessage = "Secure connection failed. Memo blocked this page rather than bypassing certificate validation."
                    onBrowserError(browserMessage)
                    if (workflowState.isActive) pauseWorkflowForUser("The provider's secure connection failed. Memo will not bypass certificate validation. Retry or choose another provider.")
                }

                override fun onReceivedError(view: WebView?, request: WebResourceRequest?, error: WebResourceError?) {
                    if (request?.isForMainFrame == true) {
                        browserMessage = "Page could not load: ${error?.description?.toString()?.take(160) ?: "network error"}"
                        onBrowserError(browserMessage)
                        if (workflowState.isActive) pauseWorkflowForUser("The provider page could not load. Check the connection or open the provider manually, then continue the workflow.")
                    }
                }
            }
        }
    }

    fun navigate(rawAddress: String) {
        if (pendingTask != null && isExecutingTask) cancelPendingTask()
        if (isSendingPrompt) {
            val taskId = preparedTaskId
            isSendingPrompt = false
            sendMessage = "Navigation interrupted submission verification. Check the original conversation before retrying."
            if (taskId.isNotBlank()) callbacks?.onNeedsReview(taskId, sendMessage)
        }
        val input = rawAddress.trim()
        if (input.isBlank()) {
            browserMessage = "Enter a website address first."
            return
        }
        if (input.length > 2_048) {
            browserMessage = "That address or search is too long. Keep it under 2,048 characters."
            return
        }
        val explicitScheme = Regex("^[A-Za-z][A-Za-z0-9+.-]*://").containsMatchIn(input)
        val firstPart = input.substringBefore('/').substringBefore('?').substringBefore('#')
        val looksLikeHost = firstPart.contains('.') || firstPart.startsWith("[") || firstPart.equals("localhost", ignoreCase = true)
        val candidate = when {
            explicitScheme -> input
            input.any { it.isWhitespace() } || !looksLikeHost -> Uri.parse("https://www.google.com/search")
                .buildUpon().appendQueryParameter("q", input).build().toString()
            else -> "https://$input"
        }
        val uri = runCatching { Uri.parse(candidate) }.getOrNull()
        if (uri?.scheme != "https" || uri.host.isNullOrBlank() || uri.userInfo != null) {
            browserMessage = "Only valid HTTPS website addresses are supported. Passwords must never be embedded in a URL."
            onBrowserError(browserMessage)
            return
        }
        preparedPrompt = ""
        preparedTaskId = ""
        preparedHost = ""
        sendMessage = ""
        browserMessage = "Opening ${uri.host}…"
        webView.loadUrl(uri.toString())
    }

    fun openQuickLink(url: String) = navigate(url)

    fun openNewConversation() {
        val target = newConversationAddress()
        if (target == null) {
            browserMessage = "This site does not have a configured new-conversation route. Use its own New chat control."
            return
        }
        navigate(target)
    }

    private fun newConversationAddress(): String? {
        val host = Uri.parse(currentUrl).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
        return when (host) {
            "chatgpt.com", "chat.openai.com" -> "https://chatgpt.com/"
            "claude.ai" -> "https://claude.ai/new"
            "grok.com" -> "https://grok.com/"
            "deepseek.com", "chat.deepseek.com" -> "https://chat.deepseek.com/"
            "gemini.google.com" -> "https://gemini.google.com/app"
            "z.ai", "chat.z.ai" -> "https://chat.z.ai/"
            "qwen.ai", "chat.qwen.ai" -> "https://chat.qwen.ai/"
            "mistral.ai", "chat.mistral.ai" -> "https://chat.mistral.ai/chat"
            else -> null
        }
    }

    /** Opens the current page in the user's regular browser for sites that reject embedded WebViews. */
    fun openCurrentPageExternally() {
        val target = webView.url?.takeIf { it.startsWith("https://", ignoreCase = true) }
            ?: currentUrl.takeIf { it.startsWith("https://", ignoreCase = true) }
        if (target.isNullOrBlank()) {
            browserMessage = "Open an HTTPS page first."
            return
        }
        runCatching {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(target)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            webView.context.startActivity(intent)
            browserMessage = "Opened in your regular browser. Sign-in cookies are managed by that browser and are not automatically shared with Memo's embedded session."
        }.onFailure {
            browserMessage = "Could not open the external browser: ${it.message?.take(120) ?: "no compatible browser found"}"
            onBrowserError(browserMessage)
        }
    }

    fun goBack() {
        if (webView.canGoBack()) webView.goBack() else browserMessage = "There is no earlier page in this browser session."
    }

    fun goForward() {
        if (webView.canGoForward()) webView.goForward() else browserMessage = "There is no later page in this browser session."
    }

    fun refresh() {
        if (currentUrl.isBlank()) browserMessage = "Open a website first." else webView.reload()
    }

    /**
     * Runs a connected, browser-backed consultation across selected frontier chat websites.
     * The user must consent in the workflow UI before calling this method. Each provider is opened
     * in the visible WebView, and its response must be detected in the page before the next step runs.
     */
    fun startBrowserWorkflow(
        title: String,
        goal: String,
        structuredBrief: String,
        providerIds: List<String>,
        mode: BrowserWorkflowMode,
        callbacks: BrowserWorkflowCallbacks
    ): String? {
        if (workflowState.isActive) return "A browser workflow is already active. Stop it before starting another."
        val cleanGoal = goal.trim().take(2_400)
        if (cleanGoal.isBlank()) return "Describe the goal before starting a frontier consultation."
        val providers = providerIds.distinct().mapNotNull(BrowserModelProviders::get).take(4)
        if (providers.size < 2) return "Select at least two supported frontier chat websites."
        if (providers.size != providerIds.distinct().size) return "One selected provider is not supported by the browser workflow. Refresh the provider list and try again."
        workflowGeneration++
        workflowProviders = providers
        workflowGoal = cleanGoal
        workflowBrief = structuredBrief.trim().take(1_800)
        workflowMode = mode
        workflowIndex = 0
        workflowPromptSent = false
        workflowPromptPrepared = false
        workflowApprovedHash = ""
        workflowApprovedDestination = ""
        workflowCallbacks = callbacks
        preparedPrompt = ""
        preparedTaskId = ""
        preparedHost = ""
        val initial = BrowserWorkflowUiState(
            status = BrowserWorkflowStatus.NAVIGATING,
            title = title.trim().take(160).ifBlank { "Frontier consultation" },
            goal = cleanGoal,
            currentProviderId = providers.first().id,
            currentProviderName = providers.first().label,
            currentStep = 1,
            totalSteps = providers.size,
            message = "Starting a visible browser workflow. Provider responses will be saved only after they are detected in the page.",
            startedAt = System.currentTimeMillis()
        )
        publishWorkflowState(initial)
        beginWorkflowStep(workflowGeneration)
        return null
    }

    /** Resume after a login, challenge, unsupported composer, or delayed response has been handled manually. */
    fun continueBrowserWorkflow() {
        if (workflowState.status != BrowserWorkflowStatus.WAITING_FOR_USER || workflowState.awaitingPromptApproval) return
        val generation = workflowGeneration
        if (workflowPromptSent) {
            publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_RESPONSE, message = "Checking the current conversation for the answer. Memo will not resubmit the prompt."))
            pollWorkflowResponse(generation, System.currentTimeMillis() + 120_000L)
        } else if (!workflowPromptPrepared) {
            beginWorkflowStep(generation, retryCurrentPage = true)
        } else {
            // A provider may have an unfamiliar Send button. If the user submitted the reviewed
            // composer manually, detect it before doing anything that could send a duplicate.
            val prompt = workflowState.currentPrompt
            webView.evaluateJavascript(BrowserScripts.verifyPromptSubmission(prompt, preserveState = true)) { raw ->
                if (disposed || generation != workflowGeneration || !workflowState.isActive) return@evaluateJavascript
                val checked = runCatching { JSONObject(decodeJsString(raw)) }.getOrNull()
                when {
                    checked?.optBoolean("verified") == true -> {
                        workflowPromptSent = true
                        publishWorkflowState(workflowState.copy(
                            status = BrowserWorkflowStatus.WAITING_FOR_RESPONSE,
                            sentForCurrentStep = true,
                            message = "The manually submitted prompt is visible in the conversation. Waiting for a new stable answer without resending it."
                        ))
                        pollWorkflowResponse(generation, System.currentTimeMillis() + 120_000L)
                    }
                    checked?.optBoolean("contextPresent") != true -> pauseWorkflowForUser("Memo cannot prove whether this prompt was submitted because the page context changed. It will not resend automatically. Inspect the conversation; stop this run and start a new one only if you confirm the prompt was not sent.")
                    checked?.optBoolean("composerCleared") == true -> pauseWorkflowForUser("The composer is empty, but Memo cannot yet verify the submitted message. It will not send a duplicate. Inspect the conversation, then continue checking when the message is visible.")
                    else -> beginWorkflowStep(generation, retryCurrentPage = true)
                }
            }
        }
    }


    /** Continue only when the currently shown provider and exact prompt still match the approval. */
    fun approveCurrentPrompt(expectedHash: String, expectedDestination: String): Boolean {
        if (disposed || workflowState.status != BrowserWorkflowStatus.WAITING_FOR_USER || !workflowState.awaitingPromptApproval) return false
        val provider = workflowProviders.getOrNull(workflowIndex) ?: return false
        val destination = approvalDestination(currentUrl) ?: return false
        val actualHash = ActionApprovalLedger.contentHash("runtime.browser", "browser.submit_prompt", destination, workflowState.currentPrompt)
        if (!providerMatches(provider, currentUrl) || destination != expectedDestination || actualHash != expectedHash ||
            workflowState.currentPromptHash != expectedHash || workflowState.approvalDestination != expectedDestination) return false
        workflowApprovedHash = expectedHash
        workflowApprovedDestination = expectedDestination
        publishWorkflowState(workflowState.copy(
            status = BrowserWorkflowStatus.PREPARING_PROMPT,
            awaitingPromptApproval = false,
            message = "Exact prompt approved for ${provider.label}. Memo is preparing that prompt for the visible website."
        ))
        attemptWorkflowPrompt(workflowGeneration)
        return true
    }

    /** Declining a prompt stops the sequence; no other provider receives the same authority. */
    fun rejectCurrentPrompt() {
        if (workflowState.awaitingPromptApproval) stopBrowserWorkflow()
    }

    private fun approvalDestination(url: String): String? {
        val uri = Uri.parse(url)
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() || !uri.userInfo.isNullOrBlank()) return null
        return uri.buildUpon().clearQuery().fragment(null).build().toString().take(2_048).takeIf { it.startsWith("https://") }
    }

    fun stopBrowserWorkflow() {
        if (!workflowState.isActive) return
        workflowGeneration++
        workflowNavigationCallback = null
        workflowApprovedHash = ""
        workflowApprovedDestination = ""
        val stopped = workflowState.copy(status = BrowserWorkflowStatus.CANCELLED, awaitingPromptApproval = false, message = "Workflow stopped by the user. Responses already captured remain saved; the current prompt will not be resent.")
        publishWorkflowState(stopped)
        workflowCallbacks?.onFinished(stopped)
        workflowCallbacks = null
    }

    private fun beginWorkflowStep(generation: Long, retryCurrentPage: Boolean = false) {
        if (disposed || generation != workflowGeneration || workflowIndex !in workflowProviders.indices) return
        val provider = workflowProviders[workflowIndex]
        val prompt = BrowserWorkflowPrompts.build(workflowGoal, workflowBrief, workflowMode, provider, workflowIndex, workflowState.responses)
        workflowStepStartedAt = System.currentTimeMillis()
        if (!retryCurrentPage) {
            workflowApprovedHash = ""
            workflowApprovedDestination = ""
        }
        val nextState = workflowState.copy(
            status = if (retryCurrentPage) BrowserWorkflowStatus.PREPARING_PROMPT else BrowserWorkflowStatus.NAVIGATING,
            currentProviderId = provider.id,
            currentProviderName = provider.label,
            currentStep = workflowIndex + 1,
            currentPrompt = prompt,
            currentPromptHash = "",
            approvalDestination = "",
            awaitingPromptApproval = false,
            sentForCurrentStep = workflowPromptSent,
            message = if (retryCurrentPage) "Retrying the current step without discarding previous model responses." else "Opening ${provider.label} in the visible browser. Sign in there if the provider requests it."
        )
        publishWorkflowState(nextState)
        if (!retryCurrentPage) {
            // Always return to the provider's configured entry/new-chat route before a new step.
            // Merely matching the host is not enough: the WebView may still be showing a previous
            // consultation, and appending this prompt would contaminate that conversation.
            workflowNavigationCallback = { if (generation == workflowGeneration) attemptWorkflowPrompt(generation) }
            navigate(provider.startUrl)
            mainHandler.postDelayed({
                if (generation == workflowGeneration && workflowState.status == BrowserWorkflowStatus.NAVIGATING && isLoading) {
                    pauseWorkflowForUser("${provider.label} is taking longer than expected to load. Inspect the browser page, then continue.")
                }
            }, 45_000L)
        } else if (isLoading) {
            publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_USER, message = "Wait for the provider page to finish loading, then continue the workflow."))
        } else {
            attemptWorkflowPrompt(generation)
        }
    }

    private fun attemptWorkflowPrompt(generation: Long) {
        if (disposed || generation != workflowGeneration || workflowIndex !in workflowProviders.indices) return
        val provider = workflowProviders[workflowIndex]
        if (isLoading) {
            pauseWorkflowForUser("Wait for ${provider.label} to finish loading, then continue.")
            return
        }
        if (!providerMatches(provider, currentUrl)) {
            pauseWorkflowForUser("Open ${provider.label} in the visible browser to continue. Memo will not send the prompt to a different site.")
            return
        }
        if (workflowPromptSent) {
            publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_RESPONSE, awaitingPromptApproval = false, message = "The prompt was already submitted. Checking for a new answer without resending it."))
            pollWorkflowResponse(generation, System.currentTimeMillis() + 120_000L)
            return
        }
        val prompt = workflowState.currentPrompt
        val destination = approvalDestination(currentUrl)
        if (destination == null) {
            pauseWorkflowForUser("Memo cannot create a safe HTTPS approval target for this page. Check the address before continuing.")
            return
        }
        val promptHash = ActionApprovalLedger.contentHash("runtime.browser", "browser.submit_prompt", destination, prompt)
        if (workflowApprovedHash != promptHash || workflowApprovedDestination != destination) {
            if (workflowState.awaitingPromptApproval && workflowState.currentPromptHash == promptHash && workflowState.approvalDestination == destination) return
            workflowApprovedHash = ""
            workflowApprovedDestination = ""
            publishWorkflowState(workflowState.copy(
                status = BrowserWorkflowStatus.WAITING_FOR_USER,
                currentPromptHash = promptHash,
                approvalDestination = destination,
                awaitingPromptApproval = true,
                message = "Review the exact prompt and approve this specific ${provider.label} submission before Memo sends it."
            ))
            workflowCallbacks?.onApprovalRequired(workflowIndex, provider, prompt, destination, promptHash)
            return
        }
        if (workflowState.currentPromptHash != promptHash || workflowState.approvalDestination != destination) {
            pauseWorkflowForUser("The approved prompt or destination changed. A fresh approval is required.")
            return
        }
        publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.PREPARING_PROMPT, awaitingPromptApproval = false, message = "Preparing the approved prompt for ${provider.label}…"))
        webView.evaluateJavascript(BrowserScripts.preparePrompt(prompt)) { raw ->
            if (disposed || generation != workflowGeneration) return@evaluateJavascript
            val result = runCatching { JSONObject(decodeJsString(raw)) }.getOrNull()
            if (result?.optBoolean("ok") != true) {
                workflowApprovedHash = ""
                workflowApprovedDestination = ""
                pauseWorkflowForUser("${provider.label} is not ready for the approved prompt. ${result?.optString("content")?.take(180).orEmpty()} Sign in or open a chat manually, then prepare a new approval.")
                return@evaluateJavascript
            }
            val currentDestination = approvalDestination(currentUrl)
            val currentHash = ActionApprovalLedger.contentHash("runtime.browser", "browser.submit_prompt", currentDestination.orEmpty(), prompt)
            if (currentDestination != destination || currentHash != workflowApprovedHash || currentHash != promptHash || !providerMatches(provider, currentUrl)) {
                workflowApprovedHash = ""
                workflowApprovedDestination = ""
                pauseWorkflowForUser("The browser destination or prompt changed after approval. Nothing further was submitted; prepare a fresh approval.")
                return@evaluateJavascript
            }
            workflowPromptPrepared = true
            // Consume the one-time approval immediately before issuing the external send operation.
            workflowApprovedHash = ""
            workflowApprovedDestination = ""
            workflowCallbacks?.onStepStarted(workflowIndex, provider, prompt)
            publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.SENDING_PROMPT, awaitingPromptApproval = false, message = "Submitting the approved prompt to ${provider.label} through its visible website…"))
            webView.evaluateJavascript(BrowserScripts.sendPreparedPrompt(prompt)) { sendRaw ->
                if (disposed || generation != workflowGeneration) return@evaluateJavascript
                val sent = runCatching { JSONObject(decodeJsString(sendRaw)) }.getOrNull()
                if (sent?.optBoolean("ok") != true || sent?.optBoolean("clicked") != true) {
                    pauseWorkflowForUser("Memo could not verify whether ${provider.label} received the prompt. Do not retry blindly; inspect the provider conversation first.")
                    return@evaluateJavascript
                }
                workflowPromptSent = true
                publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_RESPONSE, sentForCurrentStep = true, awaitingPromptApproval = false, message = "Prompt submitted. Waiting for a new, stable answer from ${provider.label}…"))
                pollWorkflowResponse(generation, System.currentTimeMillis() + 120_000L)
            }
        }
    }

    private fun pollWorkflowResponse(generation: Long, deadline: Long) {
        if (disposed || generation != workflowGeneration || !workflowState.isActive) return
        if (!providerMatches(workflowProviders.getOrNull(workflowIndex) ?: return, currentUrl)) {
            pauseWorkflowForUser("The browser moved away from the current provider. Return to the conversation and continue checking; Memo will not resubmit the prompt.")
            return
        }
        webView.evaluateJavascript(BrowserScripts.pollAssistantResponse) { raw ->
            if (disposed || generation != workflowGeneration || !workflowState.isActive) return@evaluateJavascript
            val result = runCatching { JSONObject(decodeJsString(raw)) }.getOrNull()
            val status = result?.optString("status").orEmpty()
            val response = result?.optString("response").orEmpty()
            val reason = result?.optString("reason").orEmpty()
            when {
                status == "COMPLETED" && response.isNotBlank() -> completeWorkflowStep(generation, response.take(14_000))
                status == "NEEDS_REVIEW" -> pauseWorkflowForUser(reason.ifBlank { "The answer needs manual review before the workflow can continue." }.take(240))
                System.currentTimeMillis() >= deadline -> pauseWorkflowForUser("No stable response was detected within two minutes. Inspect ${workflowState.currentProviderName}'s page. Continue to check again; the prompt will not be resent.")
                else -> {
                    publishWorkflowState(workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_RESPONSE, message = result?.optString("reason")?.take(220)?.ifBlank { null } ?: "Waiting for ${workflowState.currentProviderName} to finish responding…"))
                    mainHandler.postDelayed({ pollWorkflowResponse(generation, deadline) }, 1_000L)
                }
            }
        }
    }

    private fun completeWorkflowStep(generation: Long, responseText: String) {
        if (disposed || generation != workflowGeneration || workflowIndex !in workflowProviders.indices) return
        val provider = workflowProviders[workflowIndex]
        val prompt = workflowState.currentPrompt
        val result = BrowserModelResponse(
            providerId = provider.id,
            providerName = provider.label,
            prompt = prompt,
            response = responseText.trim().take(14_000),
            sourceUrl = currentUrl.take(2_048),
            startedAt = workflowStepStartedAt,
            completedAt = System.currentTimeMillis()
        )
        val results = workflowState.responses + result
        workflowCallbacks?.onStepCompleted(workflowIndex, result)
        workflowPromptSent = false
        workflowPromptPrepared = false
        workflowIndex++
        if (workflowIndex >= workflowProviders.size) {
            val finished = workflowState.copy(
                status = BrowserWorkflowStatus.COMPLETED,
                responses = results,
                currentStep = workflowProviders.size,
                sentForCurrentStep = false,
                awaitingPromptApproval = false,
                message = "All ${results.size} frontier responses were captured from their browser pages. Review agreement and disagreement in the linked consultation chat.",
                currentPrompt = ""
            )
            publishWorkflowState(finished)
            workflowCallbacks?.onFinished(finished)
        } else {
            publishWorkflowState(workflowState.copy(
                status = BrowserWorkflowStatus.NAVIGATING,
                responses = results,
                currentStep = workflowIndex + 1,
                currentProviderId = workflowProviders[workflowIndex].id,
                currentProviderName = workflowProviders[workflowIndex].label,
                sentForCurrentStep = false,
                awaitingPromptApproval = false,
                message = "${provider.label} response captured. Moving to ${workflowProviders[workflowIndex].label} with the earlier response included in its prompt.",
                currentPrompt = ""
            ))
            beginWorkflowStep(generation)
        }
    }

    private fun pauseWorkflowForUser(message: String) {
        if (!workflowState.isActive) return
        val paused = workflowState.copy(status = BrowserWorkflowStatus.WAITING_FOR_USER, message = message)
        publishWorkflowState(paused)
    }

    private fun publishWorkflowState(state: BrowserWorkflowUiState) {
        workflowState = state
        workflowCallbacks?.onStateChanged(state)
    }

    private fun providerMatches(provider: BrowserModelProvider, url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
        return provider.hosts.any { host == it || host.endsWith(".$it") }
    }

    fun runTask(task: TaskRecord, taskCallbacks: TaskCallbacks) {
        if (pendingTask != null || isExecutingTask || isSendingPrompt) {
            taskCallbacks.onFailed(task.id, "Another browser task is already active. Finish or cancel it first.", true)
            return
        }
        val activeUrl = currentUrl
        if (task.targetUrl.isBlank() && activeUrl.isBlank()) {
            taskCallbacks.onFailed(task.id, "Open a website first or add a target HTTPS URL to the task.", true)
            return
        }
        callbacks = taskCallbacks
        pendingTask = task
        taskCallbacks.onStarted(task.id)
        if (task.targetUrl.isNotBlank() && !samePageTarget(task.targetUrl, activeUrl)) {
            canContinueTask = false
            navigate(task.targetUrl)
            browserMessage = "Target page is loading. Sign in manually if asked, then tap Continue task."
        } else {
            canContinueTask = !isLoading
            browserMessage = if (isLoading) "Wait for the page to finish loading, then continue." else "Task is ready. Tap Continue task to inspect this page."
        }
    }

    fun continueTask() {
        val task = pendingTask ?: return
        val actionCallbacks = callbacks ?: return
        if (isLoading) {
            browserMessage = "Wait for the page to finish loading before continuing."
            return
        }
        if (currentUrl.isBlank()) {
            actionCallbacks.onFailed(task.id, "No website is open.", true)
            resetPending()
            return
        }
        if (task.targetUrl.isNotBlank() && !hostsMatch(task.targetUrl, currentUrl)) {
            val message = "The open page does not match the task's target site. Return to ${Uri.parse(task.targetUrl).host} before continuing."
            browserMessage = message
            actionCallbacks.onFailed(task.id, message, true)
            resetPending()
            return
        }
        if (task.kind == TaskKind.PREPARE_PROMPT && task.query.length > 8_000) {
            actionCallbacks.onFailed(task.id, "Prompt exceeds the 8,000 character safety limit.", true)
            resetPending()
            return
        }
        if (task.kind == TaskKind.PREPARE_PROMPT && !isAssistantSite(currentUrl)) {
            actionCallbacks.onFailed(task.id, "Prompt preparation is limited to recognized AI chat sites. Open your AI conversation and run the task again.", true)
            resetPending()
            return
        }
        isExecutingTask = true
        canContinueTask = false
        val generation = ++taskGeneration
        browserMessage = "Running ${task.kind.label.lowercase()}…"
        if (task.kind == TaskKind.SCROLL_CAPTURE) {
            webView.evaluateJavascript(BrowserScripts.startScrollCapture) {
                if (generation == taskGeneration) pollScrollResult(task, actionCallbacks, System.currentTimeMillis() + 18_000L, generation)
            }
            return
        }
        val script = when (task.kind) {
            TaskKind.CAPTURE_PAGE -> BrowserScripts.capturePage
            TaskKind.EXTRACT_LINKS -> BrowserScripts.extractLinks
            TaskKind.FIND_TEXT -> BrowserScripts.findText(task.query)
            TaskKind.PREPARE_PROMPT -> BrowserScripts.preparePrompt(task.query)
            TaskKind.SCROLL_CAPTURE -> BrowserScripts.capturePage
        }
        webView.evaluateJavascript(script) { raw ->
            if (generation == taskGeneration) processTaskResult(task, actionCallbacks, raw, generation = generation)
        }
    }

    private fun pollScrollResult(task: TaskRecord, taskCallbacks: TaskCallbacks, deadline: Long, generation: Long) {
        if (disposed || generation != taskGeneration) return
        webView.evaluateJavascript(BrowserScripts.pollScrollCapture) { raw ->
            if (generation != taskGeneration) return@evaluateJavascript
            val decoded = decodeJsString(raw)
            when {
                decoded == "PENDING" -> {
                    if (System.currentTimeMillis() >= deadline) {
                        isExecutingTask = false
                        taskCallbacks.onFailed(task.id, "Feed review timed out. The page may be busy or may block scrolling automation.", false)
                        resetPending()
                    } else {
                        mainHandler.postDelayed({ pollScrollResult(task, taskCallbacks, deadline, generation) }, 350L)
                    }
                }
                decoded.isBlank() || decoded == "null" -> {
                    isExecutingTask = false
                    taskCallbacks.onFailed(task.id, "The page did not return a feed result.", false)
                    resetPending()
                }
                else -> processTaskResult(task, taskCallbacks, decoded, alreadyDecoded = true, generation = generation)
            }
        }
    }

    private fun processTaskResult(task: TaskRecord, taskCallbacks: TaskCallbacks, raw: String?, alreadyDecoded: Boolean = false, generation: Long) {
        if (disposed || generation != taskGeneration) return
        val text = if (alreadyDecoded) raw.orEmpty() else decodeJsString(raw)
        val result = runCatching { JSONObject(text) }.getOrElse {
            isExecutingTask = false
            taskCallbacks.onFailed(task.id, "Could not read the browser result. Try reloading the page and running the task again.", false)
            resetPending()
            return
        }
        val ok = result.optBoolean("ok", false)
        if (!ok) {
            isExecutingTask = false
            taskCallbacks.onFailed(task.id, result.optString("content", "The page did not complete the requested action."), true)
            resetPending()
            return
        }
        val kind = if (task.kind == TaskKind.CAPTURE_PAGE && isAssistantSite(currentUrl)) {
            "AI conversation snapshot"
        } else result.optString("kind", task.kind.label)
        val title = result.optString("title", pageTitle).take(240).ifBlank { pageTitle }
        val url = result.optString("url", currentUrl).take(2_048).ifBlank { currentUrl }
        val content = result.optString("content", "").take(15_000)
        val summary = result.optString("summary", "Evidence captured from the active page.").take(2_000)
        val metadata = compactMetadata(result.optJSONObject("metadata"))

        if (task.kind == TaskKind.PREPARE_PROMPT) {
            preparedPrompt = task.query
            preparedTaskId = task.id
            preparedHost = Uri.parse(currentUrl).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
            preparedSite = Uri.parse(currentUrl).host.orEmpty()
            isExecutingTask = false
            taskCallbacks.onNeedsReview(task.id, "Prompt is filled into the page but has not been sent. Review it, then use Send prepared prompt.")
            taskCallbacks.onLog("Prompt prepared in $preparedSite; it was not sent.", "INFO")
            browserMessage = "Prompt prepared. Nothing has been sent. Review the composer before you decide to send it."
            resetPending(keepCallbacks = true)
            return
        }

        val evidence = EvidenceRecord(
            taskId = task.id,
            title = title,
            sourceUrl = url,
            kind = kind,
            content = content,
            summary = summary,
            metadata = metadata
        )
        isExecutingTask = false
        taskCallbacks.onCompleted(task.id, evidence, summary)
        browserMessage = "Saved evidence from ${Uri.parse(url).host ?: "the current page"}."
        resetPending(keepCallbacks = true)
    }

    fun sendPreparedPrompt() {
        val taskId = preparedTaskId
        val prompt = preparedPrompt
        val taskCallbacks = callbacks
        if (taskId.isBlank() || prompt.isBlank() || taskCallbacks == null) {
            sendMessage = "There is no prepared prompt to send."
            return
        }
        if (isLoading || isSendingPrompt) {
            sendMessage = "Wait for the page to finish loading."
            return
        }
        val currentHost = Uri.parse(currentUrl).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
        if (currentHost.isBlank() || currentHost != preparedHost || !isAssistantSite(currentUrl)) {
            sendMessage = "The current site is different from the supported AI chat where the prompt was prepared. Return to $preparedSite and prepare again."
            return
        }
        isSendingPrompt = true
        sendMessage = "Requesting submission and verifying the conversation…"
        webView.evaluateJavascript(BrowserScripts.sendPreparedPrompt(prompt)) { raw ->
            if (disposed || !isSendingPrompt) return@evaluateJavascript
            val result = runCatching { JSONObject(decodeJsString(raw)) }.getOrNull()
            if (result?.optBoolean("ok") != true || result.optBoolean("clicked") != true) {
                isSendingPrompt = false
                sendMessage = result?.optString("content")?.take(220) ?: "The page did not confirm a send action. Review the composer manually."
                taskCallbacks.onNeedsReview(taskId, sendMessage)
                return@evaluateJavascript
            }
            mainHandler.postDelayed({ verifyPromptSubmission(taskId, prompt, taskCallbacks) }, 1_500L)
        }
    }

    private fun verifyPromptSubmission(taskId: String, prompt: String, taskCallbacks: TaskCallbacks) {
        if (disposed || !isSendingPrompt) return
        webView.evaluateJavascript(BrowserScripts.verifyPromptSubmission(prompt)) { raw ->
            if (disposed || !isSendingPrompt) return@evaluateJavascript
            val verification = runCatching { JSONObject(decodeJsString(raw)) }.getOrNull()
            val verified = verification?.optBoolean("verified", false) == true
            webView.evaluateJavascript(BrowserScripts.capturePage) { captureRaw ->
                if (disposed || !isSendingPrompt) return@evaluateJavascript
                val result = runCatching { JSONObject(decodeJsString(captureRaw)) }.getOrNull()
                val content = result?.optString("content").orEmpty()
                val evidence = if (result?.optBoolean("ok") == true) EvidenceRecord(
                    taskId = taskId,
                    title = result.optString("title", "Conversation after submission").take(240),
                    sourceUrl = result.optString("url", currentUrl).take(2_048),
                    kind = "AI conversation",
                    content = content.take(15_000),
                    summary = if (verified) "Page captured after submission was observed in the conversation." else "Page snapshot captured after a send attempt; submission remains unverified.",
                    metadata = JSONObject().apply {
                        put("submissionVerification", verification ?: JSONObject())
                    }.toString()
                ) else null
                isSendingPrompt = false
                if (evidence != null) taskCallbacks.onEvidence(evidence)
                if (verified) {
                    sendMessage = "The page shows the new prompt and the composer no longer contains it. The conversation snapshot was saved."
                    taskCallbacks.onCompleted(taskId, null, "Prompt submission verified by a visible conversation change; page evidence saved.")
                    preparedPrompt = ""
                    preparedTaskId = ""
                    preparedHost = ""
                    preparedSite = ""
                } else {
                    sendMessage = verification?.optString("reason")?.take(220)
                        ?: "The page did not provide enough evidence to verify submission. Check the conversation before retrying."
                    taskCallbacks.onNeedsReview(taskId, sendMessage)
                }
            }
        }
    }

    fun cancelPendingTask() {
        taskGeneration++
        val task = pendingTask
        val taskCallbacks = callbacks
        if (task?.kind == TaskKind.SCROLL_CAPTURE && isExecutingTask) {
            runCatching { webView.evaluateJavascript(BrowserScripts.cancelScrollCapture, null) }
        }
        if (task != null && taskCallbacks != null) taskCallbacks.onFailed(task.id, "Task cancelled by the user.", true)
        resetPending()
        browserMessage = "Pending browser task cancelled."
    }

    fun clearPreparedPrompt() {
        preparedPrompt = ""
        preparedTaskId = ""
        preparedHost = ""
        preparedSite = ""
        sendMessage = "Prepared prompt cleared from Memo memory. The page composer was not cleared."
    }

    private fun resetPending(keepCallbacks: Boolean = false) {
        pendingTask = null
        canContinueTask = false
        isExecutingTask = false
        if (!keepCallbacks) callbacks = null
    }

    private fun compactMetadata(source: JSONObject?): String {
        if (source == null) return "{}"
        val raw = source.toString()
        if (raw.length <= 4_000) return raw
        val compact = JSONObject().put("truncated", true).put("originalLength", raw.length)
        val keys = source.keys().asSequence().take(30).toList()
        keys.forEach { key ->
            val value = source.opt(key)
            when (value) {
                is org.json.JSONArray -> {
                    compact.put("${key}Count", value.length())
                    if (key.equals("headings", ignoreCase = true)) {
                        compact.put(key, org.json.JSONArray().apply {
                            (0 until minOf(value.length(), 12)).forEach { index ->
                                put(value.optString(index).take(120))
                            }
                        })
                    }
                }
                is JSONObject -> compact.put("${key}Fields", value.keys().asSequence().take(20).toList().joinToString(","))
                is String -> compact.put(key, value.take(240))
                is Number, is Boolean -> compact.put(key, value)
                else -> if (value != null && value != JSONObject.NULL) compact.put(key, value.toString().take(160))
            }
        }
        if (compact.toString().length <= 4_000) return compact.toString()
        return JSONObject().put("truncated", true).put("originalLength", raw.length)
            .put("availableFields", keys.joinToString(",").take(1_000)).toString()
    }

    private fun decodeJsString(raw: String?): String {
        if (raw.isNullOrBlank() || raw == "null") return ""
        return runCatching {
            val token = JSONTokener(raw)
            val parsed = token.nextValue()
            if (parsed is String) parsed else parsed.toString()
        }.getOrDefault(raw)
    }

    private fun isAssistantSite(url: String): Boolean {
        val host = Uri.parse(url).host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
        val supported = setOf(
            "chatgpt.com", "chat.openai.com", "claude.ai", "grok.com", "deepseek.com",
            "gemini.google.com", "z.ai", "qwen.ai", "mistral.ai", "perplexity.ai",
            "poe.com", "pi.ai", "copilot.microsoft.com", "you.com", "huggingface.co"
        )
        return supported.any { host == it || host.endsWith(".$it") }
    }

    private fun samePageTarget(target: String, active: String): Boolean {
        if (active.isBlank()) return false
        val targetUri = Uri.parse(target)
        val activeUri = Uri.parse(active)
        return targetUri.host?.equals(activeUri.host, ignoreCase = true) == true &&
            targetUri.path.orEmpty().trimEnd('/') == activeUri.path.orEmpty().trimEnd('/') &&
            targetUri.query == activeUri.query
    }

    private fun hostsMatch(target: String, active: String): Boolean {
        fun canonical(host: String?): String = host.orEmpty().lowercase(Locale.ROOT).removePrefix("www.")
        val expected = canonical(Uri.parse(target).host)
        val actual = canonical(Uri.parse(active).host)
        return expected.isNotBlank() && actual.isNotBlank() && (actual == expected || actual.endsWith(".$expected"))
    }

    fun dispose() {
        disposed = true
        workflowGeneration++
        workflowNavigationCallback = null
        workflowCallbacks = null
        workflowPromptSent = false
        workflowPromptPrepared = false
        mainHandler.removeCallbacksAndMessages(null)
        runCatching {
            webView.stopLoading()
            CookieManager.getInstance().flush()
            webView.webChromeClient = null
            webView.webViewClient = WebViewClient()
            (webView.parent as? ViewGroup)?.removeView(webView)
            webView.destroy()
        }
    }
}
