package app.memo.android.ui

import android.Manifest
import android.app.Activity
import android.app.TimePickerDialog
import android.content.ContentResolver
import android.content.ClipboardManager
import android.content.ClipData
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import android.net.Uri
import android.speech.RecognizerIntent
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Article
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Bolt
import androidx.compose.material.icons.rounded.Build
import androidx.compose.material.icons.rounded.Bookmark
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.CloseFullscreen
import androidx.compose.material.icons.rounded.OpenInFull
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Explore
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.FileDownload
import androidx.compose.material.icons.rounded.FileUpload
import androidx.compose.material.icons.rounded.Fingerprint
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.ChatBubbleOutline
import androidx.compose.material.icons.rounded.Hub
import androidx.compose.material.icons.rounded.Link
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.Mic
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.TaskAlt
import androidx.compose.material.icons.rounded.Tune
import androidx.compose.material.icons.rounded.Web
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import app.memo.android.ui.MemoText as Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.viewmodel.compose.viewModel
import app.memo.android.automation.TaskRules
import app.memo.android.ai.AiRuntimeViewModel
import app.memo.android.ai.ModelRuntimeUiState
import app.memo.android.browser.BrowserSession
import app.memo.android.browser.TaskCallbacks
import app.memo.android.data.AppSnapshot
import app.memo.android.data.ActivityRecord
import app.memo.android.data.EvidenceRecord
import app.memo.android.data.MemoWorkspaceViewModel
import app.memo.android.notifications.MemoNotificationCenter
import app.memo.android.orchestration.IntentPlanner
import com.synthos.runtime.engine.AutomationAccessibilityService
import com.synthos.runtime.remote.RemoteRunnerClient
import app.memo.android.data.TaskKind
import app.memo.android.data.TaskRecord
import app.memo.android.data.TaskStatus
import app.memo.android.ui.components.DotLabel
import app.memo.android.ui.components.EmptyState
import app.memo.android.ui.components.SectionHeading
import app.memo.android.ui.components.StatusPill
import app.memo.android.ui.components.SurfaceCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class AppTab(val label: String) {
    HOME("Home"), CHATS("Talk"), LIVE("Live"), WORK("Work"), STUDIO("Studio"), BROWSER("Browser"), MORE("More"), APPS("Apps"), APPROVALS("Approvals"), COUNCIL("Council"), MODELS("Models"), SETTINGS("Settings")
}

private enum class ToolSection(val label: String) { BROWSER("Browser"), WORKFLOWS("Workflows"), TASKS("Tasks"), EVIDENCE("Evidence") }

private data class QuickSite(val label: String, val url: String)

private val quickSites = listOf(
    QuickSite("ChatGPT", "https://chatgpt.com/"),
    QuickSite("Claude", "https://claude.ai/new"),
    QuickSite("Grok", "https://grok.com/"),
    QuickSite("DeepSeek", "https://chat.deepseek.com/"),
    QuickSite("Gemini", "https://gemini.google.com/"),
    QuickSite("LinkedIn", "https://www.linkedin.com/feed/"),
    QuickSite("Google Maps", "https://www.google.com/maps"),
    QuickSite("Facebook", "https://www.facebook.com/"),
    QuickSite("Instagram", "https://www.instagram.com/"),
    QuickSite("X", "https://x.com/"),
    QuickSite("Google News", "https://news.google.com/")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MemoWorkspaceApp(
    viewModel: MemoWorkspaceViewModel,
    onLock: () -> Unit,
    sharedText: String? = null,
    sharedSubject: String? = null,
    initialWorkspace: String? = null,
    onSharedTextConsumed: () -> Unit = {},
    onInitialWorkspaceConsumed: () -> Unit = {}
) {
    val snapshot by viewModel.snapshot.collectAsState()
    val vaultReady by viewModel.vaultReady.collectAsState()
    val vaultError by viewModel.vaultError.collectAsState()
    val themeMode by viewModel.themeMode.collectAsState()
    val uiLanguage by viewModel.uiLanguage.collectAsState()
    val aiRuntime: AiRuntimeViewModel = androidx.lifecycle.viewmodel.compose.viewModel<AiRuntimeViewModel>()
    val modelState by aiRuntime.uiState.collectAsState()
    LaunchedEffect(aiRuntime) { aiRuntime.unlockWorkspace() }
    val lockWorkspace = { aiRuntime.lockWorkspace(); onLock() }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHost = remember { SnackbarHostState() }
    var tab by remember { mutableStateOf(AppTab.HOME) }
    var activeConversationId by remember { mutableStateOf<String?>(null) }
    var pendingChatPrompt by remember { mutableStateOf("") }
    var toolSection by remember { mutableStateOf(ToolSection.BROWSER) }
    var workSectionRequest by remember { mutableStateOf("ENTITIES") }
    LaunchedEffect(initialWorkspace) {
        val destination = initialWorkspace?.trim()?.uppercase().orEmpty()
        if (destination.isNotBlank()) {
            tab = when (destination) {
                "CHATS" -> AppTab.CHATS
                "TALK", "HOME" -> AppTab.HOME
                "LIVE" -> AppTab.LIVE
                "WORK", "FLOWS", "RUNS", "RECORDS" -> AppTab.WORK
                "STUDIO" -> AppTab.STUDIO
                "APPS" -> AppTab.APPS
                "APPROVALS", "REVIEW" -> AppTab.APPROVALS
                "BROWSER" -> AppTab.BROWSER
                "LIBRARY", "EVIDENCE" -> { toolSection = ToolSection.EVIDENCE; AppTab.BROWSER }
                "COUNCIL" -> AppTab.COUNCIL
                "MODELS" -> AppTab.MODELS
                "SETTINGS" -> AppTab.SETTINGS
                "MORE" -> AppTab.MORE
                else -> AppTab.CHATS
            }
            onInitialWorkspaceConsumed()
        }
    }
    var evidenceDetail by remember { mutableStateOf<EvidenceRecord?>(null) }
    var pendingArtifactExport by remember { mutableStateOf<EvidenceRecord?>(null) }
    var deleteEvidence by remember { mutableStateOf<EvidenceRecord?>(null) }
    var deleteTask by remember { mutableStateOf<TaskRecord?>(null) }
    var importUri by remember { mutableStateOf<Uri?>(null) }
    var showImportConfirm by remember { mutableStateOf(false) }
    var importError by remember { mutableStateOf<String?>(null) }
    var showEncryptedExportDialog by remember { mutableStateOf(false) }
    var encryptedExportPassphrase by remember { mutableStateOf("") }
    var encryptedExportPassphraseConfirm by remember { mutableStateOf("") }
    var pendingEncryptedExportPassphrase by remember { mutableStateOf<CharArray?>(null) }
    var encryptedImportUri by remember { mutableStateOf<Uri?>(null) }
    var encryptedImportPassphrase by remember { mutableStateOf("") }
    var showEncryptedImportDialog by remember { mutableStateOf(false) }
    var lastVisitedHost by remember { mutableStateOf("") }

    fun snack(message: String) { scope.launch { snackbarHost.showSnackbar(message) } }

    LaunchedEffect(sharedText, sharedSubject, vaultReady) {
        val shared = sharedText?.trim().orEmpty()
        if (vaultReady && shared.isNotBlank()) {
            val title = sharedSubject?.trim()?.takeIf { it.isNotBlank() } ?: shared.lineSequence().firstOrNull().orEmpty().take(72).ifBlank { "Shared conversation" }
            viewModel.importConversation(title, "Android share sheet", shared).fold(
                onSuccess = { id ->
                    activeConversationId = id
                    tab = AppTab.CHATS
                    snack("Shared text imported into AI Chats.")
                    onSharedTextConsumed()
                },
                onFailure = { error ->
                    snack(error.message ?: "Could not import shared text.")
                    onSharedTextConsumed()
                }
            )
        }
    }
    val browser = remember(viewModel, context) {
        BrowserSession(
            context = context,
            onVisited = { url, title ->
                val host = runCatching { Uri.parse(url).host.orEmpty().removePrefix("www.") }.getOrDefault("")
                if (host.isNotBlank() && host != lastVisitedHost) {
                    lastVisitedHost = host
                    viewModel.log("Opened $host · ${title.take(100)}")
                }
            },
            onBrowserError = { message -> snack(message) }
        )
    }
    /** Stop active runtimes immediately; remote cancellation is explicitly best-effort. */
    fun stopActiveWork() {
        browser.stopBrowserWorkflow()
        browser.cancelPendingTask()
        aiRuntime.cancelRun()
        AutomationAccessibilityService.requestCancel()

        snapshot.actionApprovals.forEach { action ->
            when (action.status) {
                "PENDING" -> viewModel.rejectExternalAction(action.id, "Emergency stop requested by user before execution.")
                "APPROVED", "IN_PROGRESS" -> viewModel.markExternalActionNeedsReview(
                    action.id,
                    "Emergency stop requested. The external side effect may have started; verify destination state before retrying."
                )
            }
        }

        val remoteReceipt = context.getSharedPreferences("memo.remote.receipt", Context.MODE_PRIVATE)
        val jobId = remoteReceipt.getString("jobId", "").orEmpty().trim()
        val priorStatus = remoteReceipt.getString("status", "").orEmpty().uppercase()
        val terminal = priorStatus in setOf("COMPLETED", "SUCCEEDED", "FAILED", "CANCELLED", "CANCELED", "STOPPED", "DONE")
        if (jobId.isNotBlank() && !terminal) {
            val client = RemoteRunnerClient(context.applicationContext)
            scope.launch {
                runCatching { withContext(Dispatchers.IO) { client.cancel(jobId) } }
                    .onSuccess { response ->
                        val remoteStatus = response.optString("status", "response received")
                        remoteReceipt.edit().putString("status", remoteStatus).apply()
                        snack("Local stop requested. Guest runner responded: $remoteStatus. Verify its final state in Apps & automation.")
                    }
                    .onFailure {
                        snack("Local stop requested. Guest-runner cancellation could not be confirmed; verify the remote job in Apps & automation.")
                    }
            }
        } else {
            snack("Stop requested for local AI, browser work and active phone automation. Any uncertain external action is marked for review.")
        }
    }

    DisposableEffect(browser) { onDispose { browser.dispose() } }

    val callbacks = remember(viewModel, browser) {
        TaskCallbacks(
            onStarted = { viewModel.markTaskRunning(it) },
            onCompleted = { id, evidence, message ->
                viewModel.completeTask(id, evidence, message)
                snack("Task completed. Results saved to Evidence.")
            },
            onNeedsReview = { id, message -> viewModel.markTaskNeedsReview(id, message); snack("Review required: ${message.take(110)}") },
            onFailed = { id, message, blocked -> viewModel.failTask(id, message, blocked); snack(message.take(180)) },
            onEvidence = viewModel::addEvidence,
            onLog = { message, level -> viewModel.log(message, level) }
        )
    }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null) viewModel.exportTo(uri, context.contentResolver) { error -> snack(error ?: "Workspace exported as a JSON backup.") }
    }
    val artifactExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/markdown")) { uri ->
        val artifact = pendingArtifactExport
        pendingArtifactExport = null
        if (uri != null && artifact != null) {
            val markdown = buildString {
                append("# ").append(artifact.title).append("\n\n")
                append("- Type: ").append(artifact.kind).append("\n")
                append("- Created/edited: ").append(formatDate(artifact.capturedAt)).append("\n")
                if (artifact.sourceUrl.isNotBlank()) append("- Source: ").append(artifact.sourceUrl).append("\n")
                if (artifact.summary.isNotBlank()) append("\n").append(artifact.summary).append("\n")
                append("\n---\n\n").append(artifact.content)
            }
            runCatching { context.contentResolver.openOutputStream(uri)?.use { it.write(markdown.toByteArray(Charsets.UTF_8)) } ?: error("The selected destination could not be opened.") }
                .onSuccess { snack("Artifact exported as Markdown.") }
                .onFailure { snack("Artifact export failed: ${(it.message ?: "Unknown error").take(160)}") }
        }
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            importUri = uri
            showImportConfirm = true
        }
    }
    val encryptedExportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/octet-stream")) { uri ->
        val passphrase = pendingEncryptedExportPassphrase
        pendingEncryptedExportPassphrase = null
        if (uri != null && passphrase != null) {
            viewModel.exportEncryptedTo(uri, context.contentResolver, passphrase) { error ->
                snack(error ?: "Encrypted backup saved. Store its passphrase separately; Memo cannot recover it.")
            }
        } else {
            passphrase?.fill('\u0000')
        }
    }
    val encryptedImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            encryptedImportUri = uri
            encryptedImportPassphrase = ""
            showEncryptedImportDialog = true
        }
    }

    BackHandler(enabled = tab != AppTab.HOME || evidenceDetail != null || (tab == AppTab.CHATS && activeConversationId != null)) {
        when {
            evidenceDetail != null -> evidenceDetail = null
            tab == AppTab.CHATS && activeConversationId != null -> activeConversationId = null
            tab in setOf(AppTab.BROWSER, AppTab.APPS, AppTab.APPROVALS) -> tab = AppTab.LIVE
            tab != AppTab.HOME -> { activeConversationId = null; tab = AppTab.HOME }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = { AppHeader(onLock = lockWorkspace, onStop = ::stopActiveWork, onMore = { tab = AppTab.MORE }) },
        snackbarHost = { SnackbarHost(snackbarHost) },
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp,
                windowInsets = WindowInsets.navigationBars
            ) {
                val items = listOf(
                    Triple(AppTab.HOME, Icons.Rounded.ChatBubbleOutline, "Talk"),
                    Triple(AppTab.LIVE, Icons.Rounded.Bolt, "Live"),
                    Triple(AppTab.WORK, Icons.Rounded.Hub, "Work"),
                    Triple(AppTab.STUDIO, Icons.Rounded.Article, "Studio")
                )
                items.forEach { (dest, icon, label) ->
                    NavigationBarItem(
                        selected = when (dest) {
                            AppTab.HOME -> tab in setOf(AppTab.HOME, AppTab.CHATS, AppTab.COUNCIL)
                            AppTab.LIVE -> tab in setOf(AppTab.LIVE, AppTab.BROWSER, AppTab.APPS, AppTab.APPROVALS)
                            AppTab.WORK -> tab == AppTab.WORK
                            AppTab.STUDIO -> tab == AppTab.STUDIO
                            else -> false
                        },
                        onClick = { if (dest == AppTab.HOME) activeConversationId = null; tab = dest },
                        icon = { Icon(icon, contentDescription = label, modifier = Modifier.size(22.dp)) },
                        label = { Text(label, maxLines = 1, style = MaterialTheme.typography.labelSmall) },
                        alwaysShowLabel = true
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(Modifier.fillMaxSize().padding(innerPadding)) {
            if (vaultError != null) {
                VaultWarning(
                    message = vaultError!!,
                    onExport = { exportLauncher.launch("memo-recovery-${System.currentTimeMillis()}.json") },
                    onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) }
                )
                EmptyState(
                    title = "Workspace changes are paused",
                    body = "Export any information still visible in memory, or restore a valid Memo JSON backup. Browser tasks are paused so results are not silently discarded."
                )
            } else if (!vaultReady) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            } else {
                when (tab) {
                    AppTab.HOME -> HomeScreen(
                        snapshot = snapshot,
                        activity = snapshot.activity.take(6),
                        localModelCount = modelState.installedModels.size,
                        remoteConfigured = modelState.providerConfigured,
                        onCouncil = { tab = AppTab.COUNCIL },
                        onModels = { tab = AppTab.MODELS },
                        onTools = { tab = AppTab.LIVE },
                        onChats = { activeConversationId = null; tab = AppTab.CHATS },
                        onWork = { workSectionRequest = "ENTITIES"; tab = AppTab.WORK },
                        onOpenStudio = { tab = AppTab.STUDIO },
                        onContinueWithMemo = { rawIntent ->
                            val clean = rawIntent.trim().take(6_000)
                            val title = clean.lineSequence().firstOrNull().orEmpty().take(64).ifBlank { "New conversation" }
                            if (IntentPlanner.shouldCreateWorkflow(clean)) {
                                val sourceId = viewModel.createConversation(title = title)
                                if (sourceId.isBlank()) {
                                    snack("Memo reached its conversation limit. Export a backup before removing older records.")
                                } else {
                                    val captureError = viewModel.appendChatMessage(sourceId, "USER", clean)
                                    if (captureError != null) {
                                        activeConversationId = sourceId
                                        tab = AppTab.CHATS
                                        snack(captureError)
                                    } else {
                                        viewModel.createIntentPlan(clean, uiLanguage, sourceConversationId = sourceId).fold(
                                            onSuccess = { jobId ->
                                                workSectionRequest = "JOBS"
                                                tab = AppTab.WORK
                                                val checkpoints = viewModel.snapshot.value.jobs.firstOrNull { it.id == jobId }?.steps?.size ?: 0
                                                snack("Draft plan created with $checkpoints checkpoints and linked to its source conversation. Review it before starting; no external action has run.")
                                            },
                                            onFailure = { error ->
                                                activeConversationId = sourceId
                                                tab = AppTab.CHATS
                                                snack("The request is saved in Talk, but its plan could not be created: ${error.message ?: "unknown error"}")
                                            }
                                        )
                                    }
                                }
                            } else {
                                val id = viewModel.createConversation(title = title)
                                if (id.isBlank()) {
                                    snack("Memo reached its conversation limit. Export a backup before removing older records.")
                                } else {
                                    pendingChatPrompt = clean
                                    activeConversationId = id
                                    tab = AppTab.CHATS
                                }
                            }
                        },
                        onShowMessage = ::snack,
                        onOpenConversation = { id -> activeConversationId = id; tab = AppTab.CHATS },
                        onOpenEvidence = { evidenceDetail = it },
                        onOpenJob = { workSectionRequest = "JOBS"; tab = AppTab.WORK },
                        onStartConversation = { prompt ->
                            val clean = prompt.trim().take(6_000)
                            if (clean.isNotBlank()) {
                                val title = clean.lineSequence().firstOrNull().orEmpty().take(64).ifBlank { "New conversation" }
                                val id = viewModel.createConversation(title = title)
                                if (id.isBlank()) snack("Memo reached its conversation limit. Export a backup before removing older records.")
                                else {
                                    pendingChatPrompt = clean
                                    activeConversationId = id
                                    tab = AppTab.CHATS
                                }
                            }
                        },
                        onResumeTask = { task -> runTask(task, browser, callbacks, onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER }) }
                    )
                    AppTab.CHATS -> ChatsWorkspaceScreen(
                        snapshot = snapshot,
                        runtime = aiRuntime,
                        runtimeState = modelState,
                        viewModel = viewModel,
                        selectedConversationId = activeConversationId,
                        onSelectConversation = { activeConversationId = it },
                        initialPrompt = pendingChatPrompt,
                        onInitialPromptConsumed = { pendingChatPrompt = "" },
                        onOpenModels = { tab = AppTab.MODELS },
                        onOpenWork = { tab = AppTab.WORK },
                        onOpenCouncil = { tab = AppTab.COUNCIL },
                        onStartConversation = { prompt ->
                            val clean = prompt.trim().take(6_000)
                            if (clean.isNotBlank()) {
                                val title = clean.lineSequence().firstOrNull().orEmpty().take(64).ifBlank { "New conversation" }
                                val id = viewModel.createConversation(title = title)
                                if (id.isBlank()) snack("Memo reached its conversation limit. Export a backup before removing older records.")
                                else { pendingChatPrompt = clean; activeConversationId = id }
                            }
                        },
                        onShowMessage = ::snack
                    )
                    AppTab.LIVE -> LiveWorkspaceScreen(
                        snapshot = snapshot,
                        browserState = browser.workflowState,
                        onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER },
                        onOpenApps = { tab = AppTab.APPS },
                        onOpenApprovals = { tab = AppTab.APPROVALS },
                        onOpenWork = { tab = AppTab.WORK },
                        onOpenTalk = { id -> activeConversationId = id; tab = AppTab.CHATS },
                        onResumeTask = { task -> runTask(task, browser, callbacks, onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER }) }
                    )
                    AppTab.STUDIO -> SelfModelStudioScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        runtime = aiRuntime,
                        runtimeState = modelState,
                        onOpenEvidence = { id -> evidenceDetail = viewModel.snapshot.value.evidence.firstOrNull { it.id == id } },
                        onOpenModels = { tab = AppTab.MODELS },
                        onShowMessage = ::snack
                    )
                    AppTab.WORK -> WorkWorkspaceScreen(
                        snapshot = snapshot,
                        viewModel = viewModel,
                        runtime = aiRuntime,
                        runtimeState = modelState,
                        initialSection = workSectionRequest,
                        onSectionChanged = { workSectionRequest = it },
                        onOpenConversation = { id -> activeConversationId = id; tab = AppTab.CHATS },
                        onOpenApprovals = { tab = AppTab.APPROVALS },
                        onOpenEvidence = { id -> evidenceDetail = viewModel.snapshot.value.evidence.firstOrNull { it.id == id } },
                        onOpenModels = { tab = AppTab.MODELS },
                        onShowMessage = ::snack
                    )
                    AppTab.COUNCIL -> CouncilScreen(
                        snapshot = snapshot,
                        runtime = aiRuntime,
                        onOpenModels = { tab = AppTab.MODELS },
                        onRunSaved = viewModel::addCouncilRun,
                        onDecisionSaved = viewModel::updateCouncilDecision,
                        onShowMessage = ::snack
                    )
                    AppTab.MODELS -> ModelsScreen(runtime = aiRuntime, onShowMessage = ::snack)
                    AppTab.BROWSER -> ToolsHubScreen(
                        section = toolSection,
                        onSectionChange = { toolSection = it },
                        snapshot = snapshot,
                        viewModel = viewModel,
                        runtime = aiRuntime,
                        runtimeState = modelState,
                        browser = browser,
                        callbacks = callbacks,
                        onOpenConversation = { id -> activeConversationId = id; tab = AppTab.CHATS },
                        onShowMessage = ::snack,
                        onQuickTask = { kind, query ->
                            val title = TaskRules.guessTitle(kind, query)
                            val error = viewModel.createTask(title, "", kind, query)
                            if (error != null) snack(error) else {
                                val task = viewModel.snapshot.value.tasks.first()
                                runTask(task, browser, callbacks, onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER })
                            }
                        },
                        onCancelTask = { browser.cancelPendingTask() },
                        onClearPrompt = { browser.clearPreparedPrompt() },
                        onCreateTask = { title, url, kind, query ->
                            val error = viewModel.createTask(title, url, kind, query)
                            if (error == null) snack("Task saved. Run it when ready.") else snack(error)
                            error == null
                        },
                        onRunTask = { task -> runTask(task, browser, callbacks, onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER }) },
                        onDeleteTask = { deleteTask = it },
                        onSelectEvidence = { evidenceDetail = it },
                        onDeleteEvidence = { deleteEvidence = it },
                        onExportEvidence = { exportLauncher.launch("memo-backup-${System.currentTimeMillis()}.json") }
                    )
                    AppTab.APPS -> AppsWorkspaceScreen(onShowMessage = ::snack)
                    AppTab.APPROVALS -> ActionApprovalInboxScreen(
                        snapshot = snapshot,
                        browserState = browser.workflowState,
                        onOpenBrowser = { tab = AppTab.BROWSER; toolSection = ToolSection.BROWSER },
                        onReject = { id ->
                            val pending = viewModel.snapshot.value.actionApprovals.firstOrNull { it.id == id }
                            val matchesLivePrompt = pending != null && browser.workflowState.awaitingPromptApproval &&
                                browser.workflowState.currentPromptHash == pending.contentHash &&
                                browser.workflowState.approvalDestination == pending.destination
                            val accepted = viewModel.rejectExternalAction(id, "User rejected the proposal from the action inbox.")
                            if (accepted && matchesLivePrompt) browser.rejectCurrentPrompt()
                            accepted
                        },
                        onMarkNeedsReview = { id, reason -> viewModel.markExternalActionNeedsReview(id, reason) },
                        onShowMessage = ::snack
                    )
                    AppTab.SETTINGS -> SettingsScreen(
                        themeMode = themeMode,
                        uiLanguage = uiLanguage,
                        onThemeMode = viewModel::setThemeMode,
                        onUiLanguage = viewModel::setUiLanguage,
                        onExport = { exportLauncher.launch("memo-backup-${System.currentTimeMillis()}.json") },
                        onImport = { importLauncher.launch(arrayOf("application/json", "text/*", "application/octet-stream")) },
                        onExportEncrypted = { showEncryptedExportDialog = true },
                        onImportEncrypted = { encryptedImportLauncher.launch(arrayOf("application/octet-stream", "application/x-memo-backup", "*/*")) },
                        onLock = lockWorkspace,
                        onShowMessage = ::snack
                    )
                    AppTab.MORE -> MoreWorkspaceScreen(
                        onOpenCouncil = { tab = AppTab.COUNCIL },
                        onOpenModels = { tab = AppTab.MODELS },
                        onOpenSettings = { tab = AppTab.SETTINGS },
                        onOpenApps = { tab = AppTab.APPS },
                        onOpenApprovals = { tab = AppTab.APPROVALS }
                    )
                }
            }
        }
    }

    if (evidenceDetail != null) EvidenceDialog(
        evidence = evidenceDetail!!,
        onDismiss = { evidenceDetail = null },
        onOpenSource = {
            val url = evidenceDetail?.sourceUrl.orEmpty()
            if (url.startsWith("https://")) { browser.navigate(url); toolSection = ToolSection.BROWSER; tab = AppTab.BROWSER; evidenceDetail = null }
            else snack("This evidence has no valid HTTPS source URL.")
        },
        onCopy = {
            val item = evidenceDetail
            if (item != null) {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText(item.title, item.content))
                snack("Artifact copied to the clipboard.")
            }
        },
        onExport = { item -> pendingArtifactExport = item; artifactExportLauncher.launch(item.title.take(64).ifBlank { "memo-artifact" } + ".md") },
        onSave = { title, content ->
            val item = evidenceDetail
            if (item == null) false else viewModel.updateWorkArtifact(item.id, title, content).fold(
                onSuccess = {
                    evidenceDetail = viewModel.snapshot.value.evidence.firstOrNull { it.id == item.id }
                    snack("Artifact revision saved in the encrypted workspace.")
                    true
                },
                onFailure = { error -> snack(error.message ?: "Artifact could not be saved."); false }
            )
        },
        onDelete = { deleteEvidence = evidenceDetail; evidenceDetail = null }
    )

    if (deleteEvidence != null) {
        ConfirmDialog(
            title = "Delete saved evidence?",
            body = "This removes the captured content from your encrypted workspace. It cannot be undone.",
            confirmLabel = "Delete evidence",
            onDismiss = { deleteEvidence = null },
            onConfirm = { viewModel.deleteEvidence(deleteEvidence!!.id); deleteEvidence = null; snack("Evidence deleted.") }
        )
    }
    if (deleteTask != null) {
        ConfirmDialog(
            title = "Delete task?",
            body = "The task will be removed. Evidence already captured from it remains saved.",
            confirmLabel = "Delete task",
            onDismiss = { deleteTask = null },
            onConfirm = { viewModel.deleteTask(deleteTask!!.id); deleteTask = null; snack("Task deleted.") }
        )
    }
    if (showImportConfirm && importUri != null) {
        ConfirmDialog(
            title = "Replace this workspace?",
            body = "Importing a Memo JSON backup replaces conversations, entities, relationships, jobs, tasks, evidence, Council history and activity currently stored on this device. Export a backup first if you want to preserve the current data.",
            confirmLabel = "Replace workspace",
            onDismiss = { showImportConfirm = false; importUri = null },
            onConfirm = {
                val uri = importUri!!
                showImportConfirm = false
                importUri = null
                viewModel.importFrom(uri, context.contentResolver) { error ->
                    importError = error
                    snack(error ?: "Backup restored.")
                }
            }
        )
    }
    if (showEncryptedExportDialog) {
        AlertDialog(
            onDismissRequest = {
                showEncryptedExportDialog = false
                encryptedExportPassphrase = ""
                encryptedExportPassphraseConfirm = ""
            },
            title = { Text("Create encrypted backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Memo encrypts the entire workspace with AES-256-GCM and a passphrase-derived key. Use at least 12 characters and store the passphrase separately. If you lose it, this backup cannot be recovered.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = encryptedExportPassphrase, onValueChange = { encryptedExportPassphrase = it.take(200) }, label = { Text("Backup passphrase") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    OutlinedTextField(value = encryptedExportPassphraseConfirm, onValueChange = { encryptedExportPassphraseConfirm = it.take(200) }, label = { Text("Confirm passphrase") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                    if (encryptedExportPassphraseConfirm.isNotEmpty() && encryptedExportPassphrase != encryptedExportPassphraseConfirm) {
                        Text("The passphrases do not match.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    pendingEncryptedExportPassphrase?.fill('\u0000')
                    pendingEncryptedExportPassphrase = encryptedExportPassphrase.toCharArray()
                    encryptedExportPassphrase = ""
                    encryptedExportPassphraseConfirm = ""
                    showEncryptedExportDialog = false
                    encryptedExportLauncher.launch("memo-encrypted-backup-${System.currentTimeMillis()}.membak")
                }, enabled = encryptedExportPassphrase.length >= 12 && encryptedExportPassphrase == encryptedExportPassphraseConfirm) { Text("Choose backup location") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptedExportDialog = false
                    encryptedExportPassphrase = ""
                    encryptedExportPassphraseConfirm = ""
                }) { Text("Cancel") }
            }
        )
    }
    if (showEncryptedImportDialog && encryptedImportUri != null) {
        AlertDialog(
            onDismissRequest = {
                showEncryptedImportDialog = false
                encryptedImportUri = null
                encryptedImportPassphrase = ""
            },
            title = { Text("Restore encrypted backup") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text("Restoring replaces current conversations, tasks, evidence, jobs and workspace graph. Memo verifies the encrypted authentication tag before parsing the backup. Export the current workspace first if you need to keep it.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    OutlinedTextField(value = encryptedImportPassphrase, onValueChange = { encryptedImportPassphrase = it.take(200) }, label = { Text("Backup passphrase") }, visualTransformation = PasswordVisualTransformation(), singleLine = true, modifier = Modifier.fillMaxWidth())
                }
            },
            confirmButton = {
                Button(onClick = {
                    val uri = encryptedImportUri ?: return@Button
                    val chars = encryptedImportPassphrase.toCharArray()
                    encryptedImportUri = null
                    encryptedImportPassphrase = ""
                    showEncryptedImportDialog = false
                    viewModel.importEncryptedFrom(uri, context.contentResolver, chars) { error ->
                        importError = error
                        snack(error ?: "Authenticated encrypted backup restored.")
                    }
                }, enabled = encryptedImportPassphrase.length >= 1) { Text("Replace workspace") }
            },
            dismissButton = {
                TextButton(onClick = {
                    showEncryptedImportDialog = false
                    encryptedImportUri = null
                    encryptedImportPassphrase = ""
                }) { Text("Cancel") }
            }
        )
    }
    if (importError != null) {
        AlertDialog(
            onDismissRequest = { importError = null },
            title = { Text("Import failed") },
            text = { Text(importError!!) },
            confirmButton = { TextButton(onClick = { importError = null }) { Text("OK") } }
        )
    }
}

private fun runTask(task: TaskRecord, browser: BrowserSession, callbacks: TaskCallbacks, onOpenBrowser: () -> Unit) {
    if (browser.pendingTask?.id == task.id) {
        onOpenBrowser()
        return
    }
    browser.runTask(task, callbacks)
    onOpenBrowser()
}

@Composable
private fun ToolsHubScreen(
    section: ToolSection,
    onSectionChange: (ToolSection) -> Unit,
    snapshot: AppSnapshot,
    viewModel: MemoWorkspaceViewModel,
    runtime: AiRuntimeViewModel,
    runtimeState: ModelRuntimeUiState,
    browser: BrowserSession,
    callbacks: TaskCallbacks,
    onOpenConversation: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onQuickTask: (TaskKind, String) -> Unit,
    onCancelTask: () -> Unit,
    onClearPrompt: () -> Unit,
    onCreateTask: (String, String, TaskKind, String) -> Boolean,
    onRunTask: (TaskRecord) -> Unit,
    onDeleteTask: (TaskRecord) -> Unit,
    onSelectEvidence: (EvidenceRecord) -> Unit,
    onDeleteEvidence: (EvidenceRecord) -> Unit,
    onExportEvidence: () -> Unit
) {
    Column(Modifier.fillMaxSize()) {
        Column(Modifier.padding(horizontal = MemoTokens.Spacing.md, vertical = MemoTokens.Spacing.xs), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Browser runtime", style = MaterialTheme.typography.titleLarge)
            Text("A separate website session with its own navigation, page state, bounded browser tasks and captured evidence. This is not the native app runtime.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ToolSection.entries.forEach { candidate ->
                    FilterChip(selected = section == candidate, onClick = { onSectionChange(candidate) }, label = { Text(candidate.label) })
                }
            }
        }
        Box(Modifier.weight(1f)) {
            when (section) {
                ToolSection.BROWSER -> BrowserScreen(
                    browser = browser,
                    onQuickTask = onQuickTask,
                    onCancelTask = onCancelTask,
                    onClearPrompt = onClearPrompt
                )
                ToolSection.WORKFLOWS -> BrowserWorkflowScreen(
                    snapshot = snapshot,
                    browser = browser,
                    viewModel = viewModel,
                    runtime = runtime,
                    runtimeState = runtimeState,
                    onOpenConversation = onOpenConversation,
                    onOpenBrowser = { onSectionChange(ToolSection.BROWSER) },
                    onShowMessage = onShowMessage
                )
                ToolSection.TASKS -> TasksScreen(
                    tasks = snapshot.tasks,
                    activeTaskId = browser.pendingTask?.id,
                    onCreateTask = onCreateTask,
                    onRun = onRunTask,
                    onDelete = onDeleteTask,
                    onOpenBrowser = { onSectionChange(ToolSection.BROWSER) }
                )
                ToolSection.EVIDENCE -> EvidenceScreen(
                    evidence = snapshot.evidence,
                    onSelect = onSelectEvidence,
                    onDelete = onDeleteEvidence,
                    onExport = onExportEvidence
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AppHeader(onLock: () -> Unit, onStop: () -> Unit, onMore: () -> Unit) {
    val language = LocalMemoLanguage.current
    Surface(color = MaterialTheme.colorScheme.background, tonalElevation = 0.dp) {
        Row(
            modifier = Modifier.fillMaxWidth().statusBarsPadding().padding(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.sm),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Rounded.Bolt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(23.dp))
            }
            Spacer(Modifier.width(MemoTokens.Spacing.sm))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text("MEMO", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground, letterSpacing = 1.3.sp)
                Text("INTENT / WORK / EVIDENCE", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onMore, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(Icons.Rounded.MoreHoriz, contentDescription = MemoLocalization.translate("More", language), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = onStop, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(Icons.Rounded.StopCircle, contentDescription = MemoLocalization.translate("Stop active work", language), tint = MaterialTheme.colorScheme.error)
            }
            IconButton(onClick = onLock, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(Icons.Rounded.Lock, contentDescription = MemoLocalization.translate("Lock workspace", language), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun VaultWarning(message: String, onExport: () -> Unit, onImport: () -> Unit) {
    SurfaceCard(modifier = Modifier.padding(horizontal = MemoTokens.Spacing.md, vertical = MemoTokens.Spacing.xs)) {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.error)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Workspace storage needs attention", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.error)
                Text(message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
                    TextButton(onClick = onExport) { Text("Export visible data") }
                    TextButton(onClick = onImport) { Text("Restore backup") }
                }
            }
        }
    }
}

@Composable
private fun HomeScreen(
    snapshot: AppSnapshot,
    activity: List<ActivityRecord>,
    localModelCount: Int,
    remoteConfigured: Boolean,
    onCouncil: () -> Unit,
    onModels: () -> Unit,
    onTools: () -> Unit,
    onChats: () -> Unit,
    onWork: () -> Unit,
    onOpenStudio: () -> Unit,
    onContinueWithMemo: (String) -> Unit,
    onShowMessage: (String) -> Unit,
    onOpenConversation: (String) -> Unit,
    onOpenEvidence: (EvidenceRecord) -> Unit,
    onOpenJob: (String) -> Unit,
    onResumeTask: (TaskRecord) -> Unit
) {
    var captureDraft by remember { mutableStateOf("") }
    var workspaceSearch by remember { mutableStateOf("") }
    val context = LocalContext.current
    val uiLanguage = LocalMemoLanguage.current
    val speechResultLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val spoken = result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull().orEmpty().trim()
            if (spoken.isNotBlank()) {
                captureDraft = listOf(captureDraft.trim(), spoken).filter { it.isNotBlank() }.joinToString(" ").take(6_000)
            } else {
                onShowMessage(MemoLocalization.translate("No speech was recognized. Try again or type your message.", uiLanguage))
            }
        }
    }
    val voiceIntent = {
        Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, when (uiLanguage) { "PL" -> "pl-PL"; "TR" -> "tr-TR"; else -> "en-US" })
            putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, false)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
            putExtra(RecognizerIntent.EXTRA_PROMPT, MemoLocalization.translate("Speak your message to Memo", uiLanguage))
        }
    }
    val speechPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) {
            runCatching { speechResultLauncher.launch(voiceIntent()) }
                .onFailure { onShowMessage(MemoLocalization.translate("Voice input is unavailable. Try typing or check your Android speech service.", uiLanguage)) }
        } else {
            onShowMessage(MemoLocalization.translate("Microphone permission is needed for voice input.", uiLanguage))
        }
    }
    val reviewEntities = snapshot.entities.count { it.reviewStatus == "PROPOSED" }
    val reviewJobs = snapshot.jobs.count { it.status in setOf("WAITING_FOR_USER", "NEEDS_REVIEW", "FAILED") }
    val reviewTasks = snapshot.tasks.count { it.status in setOf(TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED, TaskStatus.BLOCKED) }
    val reviewCount = reviewEntities + reviewJobs + reviewTasks
    val latestConversation = snapshot.conversations.maxByOrNull { it.updatedAt }
    val attentionTasks = snapshot.tasks
        .filter { it.status in setOf(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED) }
        .sortedByDescending { it.updatedAt }
        .take(3)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
                DotLabel("MEMO  /  YOUR PRIVATE OPERATING ENVIRONMENT")
                Text("Make sense of what matters.", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
                Text(
                    "Capture a thought, connect its context, and turn it into work you can inspect. People, projects, sources, decisions and outcomes stay linked.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = workspaceSearch,
                    onValueChange = { workspaceSearch = it.take(240) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    placeholder = { Text("Search everything in this workspace…") },
                    supportingText = { Text("Searches local conversations, connected records, jobs and saved evidence.") }
                )
                val query = workspaceSearch.trim()
                if (query.length >= 2) {
                    val conversations = snapshot.conversations.filter { item -> item.title.contains(query, true) || item.messages.any { it.content.contains(query, true) } }.take(3)
                    val entities = snapshot.entities.filter { item -> item.reviewStatus !in setOf("REJECTED", "MERGED") && (item.title.contains(query, true) || item.type.contains(query, true) || item.description.contains(query, true) || item.properties.values.any { it.contains(query, true) }) }.take(3)
                    val jobs = snapshot.jobs.filter { item -> item.title.contains(query, true) || item.goal.contains(query, true) || item.steps.any { it.title.contains(query, true) || it.detail.contains(query, true) } }.take(3)
                    val evidence = snapshot.evidence.filter { item -> item.title.contains(query, true) || item.summary.contains(query, true) || item.content.contains(query, true) || item.sourceUrl.contains(query, true) }.take(3)
                    SurfaceCard {
                        Text("Local search · ${conversations.size + entities.size + jobs.size + evidence.size} result(s)", style = MaterialTheme.typography.titleSmall)
                        if (conversations.isEmpty() && entities.isEmpty() && jobs.isEmpty() && evidence.isEmpty()) {
                            Text("No matching records. Memo searches only information already saved to this workspace; it does not search the web from this field.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        conversations.forEach { item ->
                            TextButton(onClick = { onOpenConversation(item.id) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("Conversation · ${item.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.messages.lastOrNull()?.content.orEmpty().take(100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        entities.forEach { item ->
                            TextButton(onClick = onWork, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${item.type} · ${item.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.description.take(100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                        jobs.forEach { item ->
                            TextButton(onClick = { onOpenJob(item.id) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("Job · ${item.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text("${item.status.lowercase().replace('_', ' ')} · ${item.steps.count { it.status == "COMPLETED" }}/${item.steps.size} checkpoints", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        evidence.forEach { item ->
                            TextButton(onClick = { onOpenEvidence(item) }, modifier = Modifier.fillMaxWidth()) {
                                Column(Modifier.fillMaxWidth()) {
                                    Text("${item.kind} · ${item.title}", maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(item.summary.take(100), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Box(
                        modifier = Modifier.size(42.dp).clip(RoundedCornerShape(14.dp)).background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer, modifier = Modifier.size(22.dp))
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Start with what is on your mind", style = MaterialTheme.typography.titleMedium)
                        Text("Your note becomes a draft in a conversation. Memo will not send it until you choose to.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                OutlinedTextField(
                    value = captureDraft,
                    onValueChange = { if (it.length <= 6_000) captureDraft = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Ask a question, capture an idea, or describe the outcome you want…") },
                    minLines = 2,
                    maxLines = 5,
                    shape = RoundedCornerShape(18.dp),
                    trailingIcon = {
                        IconButton(
                            onClick = {
                                if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                                    runCatching { speechResultLauncher.launch(voiceIntent()) }
                                        .onFailure { onShowMessage(MemoLocalization.translate("Voice input is unavailable. Try typing or check your Android speech service.", uiLanguage)) }
                                } else {
                                    speechPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
                                }
                            }
                        ) {
                            Icon(Icons.Rounded.Mic, contentDescription = MemoLocalization.translate("Voice input", uiLanguage), tint = MaterialTheme.colorScheme.primary)
                        }
                    }
                )
                Spacer(Modifier.height(MemoTokens.Spacing.xs))
                Button(
                    onClick = { val intent = captureDraft.trim(); if (intent.isNotBlank()) { captureDraft = ""; onContinueWithMemo(intent) } },
                    enabled = captureDraft.isNotBlank(),
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp)
                ) {
                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp))
                    Text("Continue with Memo", maxLines = 1)
                }
                Text(
                    "Memo routes action requests into an editable plan and questions or brainstorming into Talk. Nothing external is sent or changed automatically.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onChats, modifier = Modifier.weight(1f).height(44.dp)) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Conversations", maxLines = 1)
                    }
                    OutlinedButton(onClick = onWork, modifier = Modifier.weight(1f).height(44.dp)) {
                        Icon(Icons.Rounded.Hub, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp)); Text("Connected work", maxLines = 1)
                    }
                }
                OutlinedButton(onClick = onOpenStudio, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Icon(Icons.Rounded.Person, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(7.dp)); Text("Self Model & Artifact Studio", maxLines = 1)
                }
            }
        }
        item { SectionHeading("Your world, in one place", "Durable records and evidence—not just chat history") }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs), modifier = Modifier.fillMaxWidth()) {
                MetricCard("Conversations", snapshot.conversations.size.toString(), "ideas and decisions", Modifier.weight(1f))
                MetricCard("Connected records", snapshot.entities.size.toString(), "people, plans, facts", Modifier.weight(1f))
                MetricCard("Sources", snapshot.evidence.size.toString(), "saved evidence", Modifier.weight(1f))
            }
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                    Icon(
                        if (reviewCount > 0) Icons.Rounded.Tune else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint = if (reviewCount > 0) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Needs your decision", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (reviewCount > 0) "$reviewCount item(s) need review across proposed records, jobs and interrupted work. Suggestions remain separate from accepted truth."
                            else "Nothing is waiting for review. Proposed records and uncertain runs stay separate until you decide.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(MemoTokens.Spacing.xs))
                OutlinedButton(onClick = onWork, modifier = Modifier.fillMaxWidth().height(44.dp)) {
                    Text(if (reviewCount > 0) "Review connected work" else "Open work graph")
                    Spacer(Modifier.width(6.dp)); Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, modifier = Modifier.size(18.dp))
                }
            }
        }
        if (latestConversation != null) {
            item { SectionHeading("Pick up where you left off", "Your latest conversation keeps its history and source labels") }
            item {
                SurfaceCard(modifier = Modifier.clickable { onOpenConversation(latestConversation.id) }) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                        Icon(Icons.Rounded.ChatBubbleOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(latestConversation.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text(
                                latestConversation.messages.lastOrNull()?.content.orEmpty().ifBlank { "No messages yet. Open to continue." },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text("${latestConversation.messages.size} messages · ${formatDate(latestConversation.updatedAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                        }
                        Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Open conversation", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
        if (attentionTasks.isNotEmpty()) {
            item { SectionHeading("Work in motion", "Resume, inspect or review before continuing") }
            items(attentionTasks, key = { it.id }) { task -> TaskCompactCard(task = task, onRun = { onResumeTask(task) }) }
        }
        item { SectionHeading("Tools around your work", "Specialist capabilities support the workspace; they are not separate sources of truth") }
        item {
            SurfaceCard(modifier = Modifier.clickable(onClick = onTools)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                    Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Browser & source capture", style = MaterialTheme.typography.titleMedium)
                        Text("Browse visibly, capture evidence, and bring selected sources back into Memo.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                    Icon(Icons.Rounded.Fingerprint, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("Intelligence & execution", style = MaterialTheme.typography.titleMedium)
                        Text(
                            (if (localModelCount > 0) "$localModelCount local model(s) installed" else "No local model configured yet") +
                                (if (remoteConfigured) " · optional remote provider configured; consent is still required per request" else " · remote providers remain optional"),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = onModels, modifier = Modifier.weight(1f).height(44.dp)) { Icon(Icons.Rounded.Tune, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Models") }
                    OutlinedButton(onClick = onCouncil, modifier = Modifier.weight(1f).height(44.dp)) { Icon(Icons.Rounded.AutoAwesome, null, modifier = Modifier.size(18.dp)); Spacer(Modifier.width(5.dp)); Text("Compare models") }
                }
            }
        }
        item { SectionHeading("Recent activity", "A local trail of what happened") }
        if (activity.isEmpty()) item { EmptyState("Nothing recorded yet", "As Memo captures sources and completes work, the activity trail will show observable events—not invented progress.") }
        else items(activity, key = { it.id }) { event -> ActivityLine(event) }
        item { Spacer(Modifier.height(4.dp)) }
    }
}

@Composable
private fun ActivityLine(event: ActivityRecord) {
    val tint = when (event.level.uppercase(Locale.ROOT)) {
        "ERROR" -> MaterialTheme.colorScheme.error
        "WARN" -> MaterialTheme.colorScheme.secondary
        "SUCCESS" -> MaterialTheme.colorScheme.tertiary
        else -> MaterialTheme.colorScheme.primary
    }
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            Icon(
                imageVector = if (event.level.equals("ERROR", true)) Icons.Rounded.StopCircle else Icons.Rounded.CheckCircle,
                contentDescription = event.level.lowercase(Locale.ROOT),
                tint = tint,
                modifier = Modifier.size(20.dp)
            )
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(event.message, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
                Text("${event.level} · ${formatDate(event.at)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun MetricCard(title: String, value: String, hint: String, modifier: Modifier = Modifier) {
    SurfaceCard(modifier) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.height(2.dp))
        Text(title, style = MaterialTheme.typography.titleSmall)
        Text(hint, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun QuickSiteCard(site: QuickSite, modifier: Modifier = Modifier, onClick: () -> Unit) {
    SurfaceCard(modifier = modifier.clickable(onClick = onClick)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
            Icon(Icons.Rounded.Public, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
            Text(site.label, style = MaterialTheme.typography.titleSmall, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Spacer(Modifier.height(4.dp))
        Text(Uri.parse(site.url).host.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

@Composable
private fun TaskCompactCard(task: TaskRecord, onRun: () -> Unit) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(top = 2.dp))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(task.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                StatusPill(task.status)
            }
            IconButton(onClick = onRun, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = "Open task in browser", tint = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun BrowserScreen(
    browser: BrowserSession,
    onQuickTask: (TaskKind, String) -> Unit,
    onCancelTask: () -> Unit,
    onClearPrompt: () -> Unit
) {
    var address by remember { mutableStateOf("") }
    var prompt by remember { mutableStateOf("") }
    var showNewConversationConfirm by remember { mutableStateOf(false) }
    var expandedBrowser by remember { mutableStateOf(false) }
    LaunchedEffect(browser.currentUrl) { address = browser.currentUrl }
    Column(modifier = Modifier.fillMaxSize().imePadding().padding(horizontal = MemoTokens.Spacing.sm, vertical = MemoTokens.Spacing.xs), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            OutlinedTextField(
                value = address,
                onValueChange = { address = it },
                modifier = Modifier.weight(1f),
                singleLine = true,
                label = { Text("Website or search") },
                leadingIcon = { Icon(Icons.Rounded.Public, contentDescription = null) }
            )
            Button(onClick = { browser.navigate(address) }, modifier = Modifier.height(56.dp), contentPadding = PaddingValues(horizontal = 12.dp)) { Text("Go") }
            IconButton(
                onClick = { showNewConversationConfirm = true },
                enabled = browser.canOpenNewConversation && !expandedBrowser,
                modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Start a new AI conversation", tint = if (browser.canOpenNewConversation && !expandedBrowser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
            }
            IconButton(onClick = { browser.openCurrentPageExternally() }, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(Icons.Rounded.OpenInNew, contentDescription = "Open current page in your regular browser")
            }
            IconButton(onClick = { expandedBrowser = !expandedBrowser }, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                Icon(if (expandedBrowser) Icons.Rounded.CloseFullscreen else Icons.Rounded.OpenInFull, contentDescription = if (expandedBrowser) "Exit full-screen browser" else "Expand browser to full screen")
            }
        }
        if (!expandedBrowser) {
        Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WEB SHORTCUTS", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("Open in browser runtime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Row(modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                quickSites.forEach { site ->
                    val selected = browser.currentUrl.contains(Uri.parse(site.url).host.orEmpty(), ignoreCase = true)
                    Card(
                        modifier = Modifier.width(118.dp).height(66.dp).clickable { address = site.url; browser.openQuickLink(site.url) },
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(containerColor = if (selected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(Modifier.fillMaxSize().padding(horizontal = 10.dp, vertical = 8.dp), verticalArrangement = Arrangement.SpaceBetween) {
                            Icon(Icons.Rounded.Web, contentDescription = null, tint = if (selected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.primary, modifier = Modifier.size(18.dp))
                            Text(site.label, style = MaterialTheme.typography.labelMedium, maxLines = 1, overflow = TextOverflow.Ellipsis, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { browser.goBack() }, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = "Back") }
            OutlinedButton(onClick = { browser.goForward() }, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.AutoMirrored.Rounded.ArrowForward, contentDescription = "Forward") }
            OutlinedButton(onClick = { browser.refresh() }, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(0.dp)) { Icon(Icons.Rounded.Refresh, contentDescription = "Reload") }
            OutlinedButton(onClick = { onQuickTask(TaskKind.CAPTURE_PAGE, "") }, enabled = browser.currentUrl.isNotBlank() && !browser.isLoading, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Rounded.Bookmark, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Save") }
            OutlinedButton(onClick = { onQuickTask(TaskKind.EXTRACT_LINKS, "") }, enabled = browser.currentUrl.isNotBlank() && !browser.isLoading, modifier = Modifier.weight(1f).height(48.dp), contentPadding = PaddingValues(horizontal = 8.dp)) { Icon(Icons.Rounded.Link, contentDescription = null); Spacer(Modifier.width(4.dp)); Text("Links") }
        }
        if (browser.pendingTask != null) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Task ready", style = MaterialTheme.typography.titleSmall)
                        Text(browser.pendingTask?.title.orEmpty(), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        Text(if (browser.canContinueTask) "Review the open page, then continue." else "Waiting for page load…", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    Button(onClick = { browser.continueTask() }, enabled = browser.canContinueTask && !browser.isExecutingTask, modifier = Modifier.height(48.dp), contentPadding = PaddingValues(horizontal = 12.dp)) {
                        if (browser.isExecutingTask) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Text("Continue")
                    }
                    IconButton(onClick = onCancelTask, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) { Icon(Icons.Rounded.Close, contentDescription = "Cancel task") }
                }
            }
        }
        if (browser.preparedPrompt.isNotBlank()) {
            SurfaceCard {
                Text("Prompt prepared · not sent", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Text("The prompt is in the website composer. Review it before sending.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (browser.sendMessage.isNotBlank()) Text(browser.sendMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
                    Button(onClick = { browser.sendPreparedPrompt() }, enabled = !browser.isSendingPrompt && !browser.isLoading, modifier = Modifier.weight(1f).height(48.dp)) {
                        if (browser.isSendingPrompt) CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp) else Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = null)
                        Spacer(Modifier.width(6.dp)); Text("Send prepared prompt")
                    }
                    OutlinedButton(onClick = onClearPrompt, enabled = !browser.isSendingPrompt, modifier = Modifier.height(48.dp)) { Text("Clear") }
                }
            }
        }
        }
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            if (browser.isLoading) CircularProgressIndicator(modifier = Modifier.size(14.dp), strokeWidth = 2.dp)
            Text(browser.pageTitle.ifBlank { "No page open" }, style = MaterialTheme.typography.labelLarge, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Text("${browser.progress}%", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Box(
            modifier = Modifier.weight(1f).fillMaxWidth().clip(RoundedCornerShape(MemoTokens.Radius.medium)).background(MaterialTheme.colorScheme.surfaceVariant)
        ) {
            AndroidView(factory = { browser.webView }, modifier = Modifier.fillMaxSize(), update = {})
            if (browser.currentUrl.isBlank()) {
                Column(
                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)).padding(MemoTokens.Spacing.lg),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Rounded.Web, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Your app workspace", style = MaterialTheme.typography.titleMedium)
                    Text("Open an app from the shelf. You can tap, type, scroll, sign in, and use the page manually here. AI-assisted browser actions remain visible and bounded.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                }
            }
        }
        if (!expandedBrowser) OutlinedTextField(
            value = prompt,
            onValueChange = { prompt = it },
            modifier = Modifier.fillMaxWidth(),
            minLines = 1,
            maxLines = 2,
            label = { Text("Prompt for the current AI chat") },
            placeholder = { Text("Prepare a prompt. It will not be sent yet.") },
            trailingIcon = {
                IconButton(onClick = {
                    if (prompt.isBlank()) return@IconButton
                    onQuickTask(TaskKind.PREPARE_PROMPT, prompt.trim())
                    prompt = ""
                }, enabled = prompt.isNotBlank() && browser.canPreparePrompt) {
                    Icon(Icons.AutoMirrored.Rounded.Send, contentDescription = "Prepare prompt in page", tint = if (prompt.isNotBlank() && browser.canPreparePrompt) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        )
        Text(browser.browserMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
    }
    if (showNewConversationConfirm) {
        AlertDialog(
            onDismissRequest = { showNewConversationConfirm = false },
            title = { Text("Start a new conversation?") },
            text = { Text("Memo will open this provider's new-chat page. Existing conversations remain on the provider, but an unsent draft on the current page may be lost.") },
            confirmButton = {
                Button(onClick = { showNewConversationConfirm = false; browser.openNewConversation() }) { Text("Open new chat") }
            },
            dismissButton = { TextButton(onClick = { showNewConversationConfirm = false }) { Text("Stay here") } }
        )
    }
}

@Composable
private fun TasksScreen(
    tasks: List<TaskRecord>,
    activeTaskId: String?,
    onCreateTask: (String, String, TaskKind, String) -> Boolean,
    onRun: (TaskRecord) -> Unit,
    onDelete: (TaskRecord) -> Unit,
    onOpenBrowser: () -> Unit
) {
    var showCreate by remember { mutableStateOf(false) }
    var filter by remember { mutableStateOf("All") }
    val visible = when (filter) {
        "Active" -> tasks.filter { it.status in setOf(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.NEEDS_REVIEW) }
        "Done" -> tasks.filter { it.status == TaskStatus.COMPLETED }
        "Attention" -> tasks.filter { it.status in setOf(TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED, TaskStatus.BLOCKED) }
        else -> tasks
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.md), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            SectionHeading("Browser tasks", "Small, inspectable actions that you can resume")
            Button(onClick = { showCreate = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                Icon(Icons.Rounded.Add, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Create task")
            }
            Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf("All", "Active", "Attention", "Done").forEach { label ->
                    FilterChip(selected = filter == label, onClick = { filter = label }, label = { Text(label) })
                }
            }
        }
        if (visible.isEmpty()) {
            EmptyState(
                if (tasks.isEmpty()) "No tasks yet" else "Nothing in this view",
                "Create a task to capture a page, collect links, find a phrase, review a feed or prepare an AI prompt."
            )
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(start = MemoTokens.Spacing.pageMargin, end = MemoTokens.Spacing.pageMargin, bottom = MemoTokens.Spacing.lg),
                verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)
            ) {
                items(visible, key = { it.id }) { task ->
                    TaskCard(
                        task = task,
                        active = task.id == activeTaskId,
                        onRun = { if (task.id == activeTaskId) onOpenBrowser() else onRun(task) },
                        onDelete = { onDelete(task) }
                    )
                }
            }
        }
    }
    if (showCreate) CreateTaskDialog(onDismiss = { showCreate = false }, onCreate = { title, url, kind, query ->
        val created = onCreateTask(title, url, kind, query)
        if (created) showCreate = false
        created
    })
}

@Composable
private fun TaskCard(task: TaskRecord, active: Boolean, onRun: () -> Unit, onDelete: () -> Unit) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            Box(modifier = Modifier.size(40.dp).clip(RoundedCornerShape(12.dp)).background(MaterialTheme.colorScheme.primaryContainer), contentAlignment = Alignment.Center) {
                Icon(Icons.Rounded.TaskAlt, contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(task.title, style = MaterialTheme.typography.titleMedium)
                Text(task.kind.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (task.targetUrl.isNotBlank()) Text(task.targetUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (task.query.isNotBlank()) Text(task.query, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(task.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    StatusPill(task.status)
                    Text(formatDate(task.updatedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                IconButton(onClick = onRun, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) {
                    Icon(if (active) Icons.Rounded.OpenInNew else Icons.Rounded.PlayArrow, contentDescription = if (active) "Continue in browser" else "Run task", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) { Icon(Icons.Rounded.Delete, contentDescription = "Delete task", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun CreateTaskDialog(onDismiss: () -> Unit, onCreate: (String, String, TaskKind, String) -> Boolean) {
    var title by remember { mutableStateOf("") }
    var url by remember { mutableStateOf("") }
    var query by remember { mutableStateOf("") }
    var kind by remember { mutableStateOf(TaskKind.CAPTURE_PAGE) }
    var kindExpanded by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create browser task") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                Text("A task runs in the browser after you review the open page. Leave the URL blank to use the current page.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(value = title, onValueChange = { title = it; validation = "" }, label = { Text("Task name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                OutlinedTextField(value = url, onValueChange = { url = it; validation = "" }, label = { Text("Target HTTPS URL (optional)") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://example.com") })
                Box {
                    OutlinedButton(onClick = { kindExpanded = true }, modifier = Modifier.fillMaxWidth().height(48.dp)) {
                        Text(kind.label, modifier = Modifier.weight(1f)); Icon(Icons.Rounded.MoreHoriz, contentDescription = "Choose task type")
                    }
                    DropdownMenu(expanded = kindExpanded, onDismissRequest = { kindExpanded = false }) {
                        TaskKind.entries.forEach { option ->
                            DropdownMenuItem(text = { Text(option.label) }, onClick = { kind = option; kindExpanded = false; validation = "" })
                        }
                    }
                }
                Text(kind.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (kind == TaskKind.FIND_TEXT || kind == TaskKind.PREPARE_PROMPT) {
                    OutlinedTextField(
                        value = query, onValueChange = { query = it; validation = "" }, modifier = Modifier.fillMaxWidth(),
                        label = { Text(if (kind == TaskKind.FIND_TEXT) "Phrase to find" else "Prompt to prepare") },
                        minLines = if (kind == TaskKind.PREPARE_PROMPT) 3 else 1, maxLines = 5
                    )
                } else {
                    OutlinedTextField(value = query, onValueChange = { query = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Notes / query (optional)") }, minLines = 1, maxLines = 3)
                }
                if (validation.isNotBlank()) Text(validation, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
            }
        },
        confirmButton = {
            Button(onClick = {
                val safeTitle = title.trim().ifBlank { TaskRules.guessTitle(kind, query) }
                val error = TaskRules.validate(safeTitle, url, kind, query)
                if (error != null) validation = error else if (onCreate(safeTitle, url, kind, query)) onDismiss()
            }) { Text("Save task") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun EvidenceScreen(evidence: List<EvidenceRecord>, onSelect: (EvidenceRecord) -> Unit, onDelete: (EvidenceRecord) -> Unit, onExport: () -> Unit) {
    var search by remember { mutableStateOf("") }
    val filtered = evidence.filter { item ->
        search.isBlank() || item.title.contains(search, true) || item.summary.contains(search, true) || item.sourceUrl.contains(search, true) || item.content.contains(search, true)
    }
    Column(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.padding(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.md), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
            SectionHeading("Evidence ledger", "Captured content keeps its source and collection context")
            Row(horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs), verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = search, onValueChange = { search = it }, modifier = Modifier.weight(1f), singleLine = true,
                    label = { Text("Search saved evidence") }, leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) }
                )
                IconButton(onClick = onExport, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) { Icon(Icons.Rounded.FileDownload, contentDescription = "Export workspace JSON") }
            }
        }
        if (filtered.isEmpty()) {
            EmptyState(if (evidence.isEmpty()) "Nothing captured yet" else "No matching evidence", "Use Capture page, Extract links or Review feed in the browser. Each item stays on this device until you export it.")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.xs), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)) {
                items(filtered, key = { it.id }) { item -> EvidenceCard(item, onSelect = { onSelect(item) }, onDelete = { onDelete(item) }) }
            }
        }
    }
}

@Composable
private fun EvidenceCard(item: EvidenceRecord, onSelect: () -> Unit, onDelete: () -> Unit) {
    SurfaceCard {
        Column(modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(8.dp)) {
                    Text(item.kind, modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onTertiaryContainer)
                }
                Spacer(Modifier.weight(1f))
                Text(formatDate(item.capturedAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(item.title, style = MaterialTheme.typography.titleMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text(item.summary.ifBlank { item.content.take(220) }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 3, overflow = TextOverflow.Ellipsis)
            Text(item.sourceUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
            TextButton(onClick = onSelect) { Icon(Icons.Rounded.Description, contentDescription = null); Spacer(Modifier.width(5.dp)); Text("Inspect evidence") }
            IconButton(onClick = onDelete, modifier = Modifier.size(MemoTokens.Spacing.minimumTouch)) { Icon(Icons.Rounded.Delete, contentDescription = "Delete evidence", tint = MaterialTheme.colorScheme.error) }
        }
    }
}

@Composable
private fun EvidenceDialog(
    evidence: EvidenceRecord,
    onDismiss: () -> Unit,
    onOpenSource: () -> Unit,
    onCopy: () -> Unit,
    onExport: (EvidenceRecord) -> Unit,
    onSave: (String, String) -> Boolean,
    onDelete: () -> Unit
) {
    val editable = evidence.kind in setOf("WORK_ARTIFACT", "ARTIFACT_DRAFT", "SELF_MODEL_ARTIFACT")
    var editing by remember(evidence.id) { mutableStateOf(false) }
    var titleDraft by remember(evidence.id, evidence.title) { mutableStateOf(evidence.title) }
    var contentDraft by remember(evidence.id, evidence.content) { mutableStateOf(evidence.content) }
    val revision = runCatching { org.json.JSONObject(evidence.metadata).optInt("version", 1) }.getOrDefault(1)
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (editing) "Edit artifact" else evidence.title, style = MaterialTheme.typography.titleLarge) },
        text = {
            Column(modifier = Modifier.heightIn(max = 520.dp), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)) {
                Text("${evidence.kind} · ${formatDate(evidence.capturedAt)}${if (editable) " · v$revision" else ""}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                if (evidence.sourceUrl.isNotBlank()) Text(evidence.sourceUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (editing) {
                    OutlinedTextField(titleDraft, { titleDraft = it.take(240) }, modifier = Modifier.fillMaxWidth(), label = { Text("Artifact title") }, singleLine = true)
                    OutlinedTextField(contentDraft, { if (it.length <= 24_000) contentDraft = it }, modifier = Modifier.fillMaxWidth().weight(1f, fill = false).heightIn(min = 180.dp), label = { Text("Artifact content") }, minLines = 8, maxLines = 16)
                    Text("Editing creates a new local revision. Captured source evidence remains immutable; generated text remains a draft until you accept it.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    if (evidence.summary.isNotBlank()) Text(evidence.summary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    Divider(color = MaterialTheme.colorScheme.outlineVariant)
                    SelectionContainer {
                        LazyColumn(modifier = Modifier.weight(1f)) {
                            item { Text(evidence.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface) }
                        }
                    }
                    if (editable) Text("Memo artifact · review required · ${(runCatching { org.json.JSONObject(evidence.metadata).optString("provenance", "local workspace") }.getOrDefault("local workspace"))}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                }
            }
        },
        confirmButton = {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                if (editing) {
                    Button(onClick = { if (onSave(titleDraft, contentDraft)) editing = false }, enabled = titleDraft.isNotBlank() && contentDraft.trim().length >= 2, modifier = Modifier.fillMaxWidth()) { Text("Save revision") }
                    TextButton(onClick = { titleDraft = evidence.title; contentDraft = evidence.content; editing = false }, modifier = Modifier.fillMaxWidth()) { Text("Discard edits") }
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                        TextButton(onClick = onCopy) { Icon(Icons.Rounded.Bookmark, null); Spacer(Modifier.width(4.dp)); Text("Copy") }
                        TextButton(onClick = { onExport(evidence) }) { Icon(Icons.Rounded.FileDownload, null); Spacer(Modifier.width(4.dp)); Text("Export") }
                        TextButton(onClick = onOpenSource) { Icon(Icons.Rounded.OpenInNew, null); Spacer(Modifier.width(4.dp)); Text("Source") }
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.fillMaxWidth()) {
                        if (editable) TextButton(onClick = { titleDraft = evidence.title; contentDraft = evidence.content; editing = true }) { Icon(Icons.Rounded.Edit, null); Spacer(Modifier.width(4.dp)); Text("Edit artifact") }
                        TextButton(onClick = onDismiss) { Text("Close") }
                    }
                }
            }
        },
        dismissButton = {
            if (!editing) TextButton(onClick = onDelete) { Text("Delete", color = MaterialTheme.colorScheme.error) }
        }
    )
}

@Composable
private fun SettingsScreen(
    themeMode: String,
    uiLanguage: String,
    onThemeMode: (String) -> Unit,
    onUiLanguage: (String) -> Unit,
    onExport: () -> Unit,
    onImport: () -> Unit,
    onExportEncrypted: () -> Unit,
    onImportEncrypted: () -> Unit,
    onLock: () -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    var notificationsAllowed by remember { mutableStateOf(MemoNotificationCenter.areNotificationsEnabled(context)) }
    var dailyNudgeEnabled by remember { mutableStateOf(MemoNotificationCenter.isDailyNudgeEnabled(context)) }
    var workflowNotificationsEnabled by remember { mutableStateOf(MemoNotificationCenter.areWorkflowNotificationsEnabled(context)) }
    var dailyNudgeTime by remember { mutableStateOf(MemoNotificationCenter.dailyNudgeTime(context)) }
    var enableNudgeAfterPermission by remember { mutableStateOf(false) }
    var enableWorkflowNotificationsAfterPermission by remember { mutableStateOf(false) }
    var sendTestAfterPermission by remember { mutableStateOf(false) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        notificationsAllowed = MemoNotificationCenter.areNotificationsEnabled(context)
        if (granted && enableNudgeAfterPermission) {
            dailyNudgeEnabled = true
            MemoNotificationCenter.setDailyNudge(context, true, dailyNudgeTime.first, dailyNudgeTime.second)
            onShowMessage(MemoLocalization.translate("Daily reminder enabled.", uiLanguage))
        } else if (granted && enableWorkflowNotificationsAfterPermission) {
            workflowNotificationsEnabled = true
            MemoNotificationCenter.setWorkflowNotificationsEnabled(context, true)
            onShowMessage(MemoLocalization.translate("Workflow notifications enabled.", uiLanguage))
        } else if (granted && sendTestAfterPermission) {
            MemoNotificationCenter.showTestNotification(context)
            onShowMessage(MemoLocalization.translate("Notifications are working. Private work details stay inside Memo.", uiLanguage))
        } else if (!granted) {
            onShowMessage(MemoLocalization.translate("Notifications are disabled. Enable them in Android settings to receive reminders.", uiLanguage))
        }
        enableNudgeAfterPermission = false
        enableWorkflowNotificationsAfterPermission = false
        sendTestAfterPermission = false
    }
    fun openNotificationSettings() {
        runCatching {
            context.startActivity(Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName))
        }.onFailure { onShowMessage(MemoLocalization.translate("Could not open notification settings.", uiLanguage)) }
    }
    fun requestWorkflowNotifications() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            enableWorkflowNotificationsAfterPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!MemoNotificationCenter.areNotificationsEnabled(context)) {
            onShowMessage(MemoLocalization.translate("Notifications are disabled. Enable them in Android settings to receive reminders.", uiLanguage))
            openNotificationSettings()
        } else {
            workflowNotificationsEnabled = true
            MemoNotificationCenter.setWorkflowNotificationsEnabled(context, true)
            onShowMessage(MemoLocalization.translate("Workflow notifications enabled.", uiLanguage))
        }
    }

    fun requestNotificationsForNudge() {
        if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            enableNudgeAfterPermission = true
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else if (!MemoNotificationCenter.areNotificationsEnabled(context)) {
            onShowMessage(MemoLocalization.translate("Notifications are disabled. Enable them in Android settings to receive reminders.", uiLanguage))
            openNotificationSettings()
        } else {
            dailyNudgeEnabled = true
            MemoNotificationCenter.setDailyNudge(context, true, dailyNudgeTime.first, dailyNudgeTime.second)
            onShowMessage(MemoLocalization.translate("Daily reminder enabled.", uiLanguage))
        }
    }
    LazyColumn(modifier = Modifier.fillMaxSize(), contentPadding = PaddingValues(MemoTokens.Spacing.pageMargin), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)) {
        item { SectionHeading("Settings", "Privacy, appearance and your local workspace") }
        item {
            SurfaceCard {
                Text("Language", style = MaterialTheme.typography.titleLarge)
                Text("Choose the language for Memo's interface.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                MemoLocalization.languages.forEach { (code, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onUiLanguage(code) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = uiLanguage == code, onClick = { onUiLanguage(code) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            SurfaceCard {
                Text("Notifications & reminders", style = MaterialTheme.typography.titleLarge)
                Text("Optional local notifications. Reminder text stays generic on the lock screen; Memo does not need a push-notification server.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Workflow status updates", style = MaterialTheme.typography.titleSmall)
                        Text("Notify when a native app workflow reaches a final state.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Switch(checked = workflowNotificationsEnabled, onCheckedChange = { enabled ->
                        if (enabled) requestWorkflowNotifications() else {
                            workflowNotificationsEnabled = false
                            MemoNotificationCenter.setWorkflowNotificationsEnabled(context, false)
                        }
                    })
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("Daily Memo nudge", style = MaterialTheme.typography.titleSmall)
                        Text("One quiet, content-free reminder. Android may delay it in battery-saving modes.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(MemoLocalization.translate("Reminder time", uiLanguage) + " · ${"%02d:%02d".format(dailyNudgeTime.first, dailyNudgeTime.second)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    Switch(checked = dailyNudgeEnabled, onCheckedChange = { if (it) requestNotificationsForNudge() else {
                        dailyNudgeEnabled = false
                        MemoNotificationCenter.setDailyNudge(context, false, dailyNudgeTime.first, dailyNudgeTime.second)
                    } })
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    OutlinedButton(onClick = {
                        TimePickerDialog(context, { _, hour, minute ->
                            dailyNudgeTime = hour to minute
                            MemoNotificationCenter.setDailyNudge(context, dailyNudgeEnabled, hour, minute)
                        }, dailyNudgeTime.first, dailyNudgeTime.second, true).show()
                    }, modifier = Modifier.weight(1f)) { Text("Choose reminder time") }
                    OutlinedButton(onClick = {
                        notificationsAllowed = MemoNotificationCenter.areNotificationsEnabled(context)
                        if (notificationsAllowed) MemoNotificationCenter.showTestNotification(context)
                        else if (Build.VERSION.SDK_INT >= 33 && ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                            sendTestAfterPermission = true
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else openNotificationSettings()
                    }, modifier = Modifier.weight(1f)) { Text("Test notification") }
                }
                Text("Voice input starts only when you tap the microphone. Always-listening wake words are not enabled by this build.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SurfaceCard {
                Text("Appearance", style = MaterialTheme.typography.titleLarge)
                Text("Choose how Memo should look. System follows Android settings.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                listOf("SYSTEM" to "System", "LIGHT" to "Light", "DARK" to "Dark").forEach { (value, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).clickable { onThemeMode(value) }.padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = themeMode == value, onClick = { onThemeMode(value) })
                        Text(label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(26.dp))
                    Spacer(Modifier.width(MemoTokens.Spacing.sm))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text("On-device encrypted workspace", style = MaterialTheme.typography.titleMedium)
                        Text("AES-256-GCM workspace vault. The encryption key is held by Android Keystore. Remote-provider credentials are stored separately and excluded from workspace exports. Local model weights stay in private app storage.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                Text("No built-in analytics or product telemetry. Model downloads happen only when you request them. Cloud model prompts and selected sources stay blocked until you explicitly grant permission for that Council run. Android's system WebView keeps its own cookie store.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        item {
            SurfaceCard {
                Text("Backup and restore", style = MaterialTheme.typography.titleLarge)
                Text("Plain JSON is readable by anyone with the file. Prefer a passphrase-encrypted backup for moving personal workspace data between devices. Restore replaces this device's workspace after confirmation.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                Button(onClick = onExportEncrypted, modifier = Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Rounded.Lock, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Export encrypted backup") }
                Spacer(Modifier.height(MemoTokens.Spacing.xs))
                OutlinedButton(onClick = onImportEncrypted, modifier = Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Rounded.Lock, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Restore encrypted backup") }
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth().height(44.dp)) { Icon(Icons.Rounded.FileDownload, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Export plain JSON") }
                Spacer(Modifier.height(MemoTokens.Spacing.xs))
                OutlinedButton(onClick = onImport, modifier = Modifier.fillMaxWidth().height(44.dp)) { Icon(Icons.Rounded.FileUpload, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Restore plain JSON") }
            }
        }
        item {
            SurfaceCard {
                Text("Automation boundaries", style = MaterialTheme.typography.titleLarge)
                Text("Council roles organize model work; they do not grant independent authority. Outputs remain inspectable, disagreements are preserved, and you own the final decision. Browser actions remain bounded to user-visible HTTPS pages and do not bypass sign-in, certificate errors or access controls.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Spacer(Modifier.height(MemoTokens.Spacing.sm))
                OutlinedButton(onClick = onLock, modifier = Modifier.fillMaxWidth().height(48.dp)) { Icon(Icons.Rounded.Lock, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Lock workspace now") }
            }
        }
        item {
            Text("Memo · 3.0.0", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun ConfirmDialog(title: String, body: String, confirmLabel: String, onDismiss: () -> Unit, onConfirm: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(body, style = MaterialTheme.typography.bodyMedium) },
        confirmButton = { Button(onClick = onConfirm, colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)) { Text(confirmLabel) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

private fun formatDate(timestamp: Long): String = runCatching {
    SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(Date(timestamp))
}.getOrDefault("Recently")
