package app.memo.android.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Apps
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.CloudUpload
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.History
import androidx.compose.material.icons.rounded.OpenInNew
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import app.memo.android.ui.MemoText as Text
import app.memo.android.notifications.MemoNotificationCenter
import app.memo.android.runtime.MemoRuntimeBridge
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import app.memo.android.ui.components.EmptyState
import app.memo.android.ui.components.SectionHeading
import app.memo.android.ui.components.SurfaceCard
import com.synthos.runtime.data.RunRecord
import com.synthos.runtime.data.SynthStore
import com.synthos.runtime.data.Workflow
import com.synthos.runtime.data.WorkflowStep
import com.synthos.runtime.engine.AutomationAccessibilityService
import com.synthos.runtime.engine.WorkflowPolicy
import com.synthos.runtime.remote.RemoteRunnerClient
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONObject

private data class MemoLaunchableApp(val packageName: String, val label: String)
private data class DraftNativeStep(val type: String, val value: String = "", val delayMillis: Int = 1000)

@Composable
fun AppsWorkspaceScreen(onShowMessage: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var store by remember { mutableStateOf<SynthStore?>(null) }
    var loadError by remember { mutableStateOf("") }
    var apps by remember { mutableStateOf(emptyList<MemoLaunchableApp>()) }
    var workflows by remember { mutableStateOf(emptyList<Workflow>()) }
    var runs by remember { mutableStateOf(emptyList<RunRecord>()) }
    var refreshCounter by remember { mutableStateOf(0) }
    var showBuilder by remember { mutableStateOf(false) }
    var editingWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var deleteWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var historyWorkflow by remember { mutableStateOf<Workflow?>(null) }
    var runDetails by remember { mutableStateOf<RunRecord?>(null) }
    var remoteSettings by remember { mutableStateOf(false) }
    var remoteWorkflowPreview by remember { mutableStateOf<Workflow?>(null) }
    var remoteMessage by remember { mutableStateOf("") }
    var remoteBusy by remember { mutableStateOf(false) }
    var showAppInventory by remember { mutableStateOf(false) }
    var preferredAppPackage by remember { mutableStateOf<String?>(null) }
    var remoteJobDetail by remember { mutableStateOf("") }

    val remoteClient = remember(context) { RemoteRunnerClient(context.applicationContext) }
    val remotePrefs = remember(context) { context.getSharedPreferences("memo.remote.receipt", Context.MODE_PRIVATE) }
    var lastRemoteJobId by remember { mutableStateOf(remotePrefs.getString("jobId", "").orEmpty()) }
    var lastRemoteJobStatus by remember { mutableStateOf(remotePrefs.getString("status", "").orEmpty()) }
    var accessibilityConnected by remember { mutableStateOf(AutomationAccessibilityService.isConnected()) }
    var lastObservedRunStates by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    val workspaceOpenedAt = remember { System.currentTimeMillis() }
    val currentOnShowMessage by rememberUpdatedState(onShowMessage)

    DisposableEffect(context) {
        val listener = object : MemoRuntimeBridge.Listener {
            override fun onRuntimeChanged() {
                refreshCounter += 1
                accessibilityConnected = AutomationAccessibilityService.isConnected()
            }

            override fun onServiceMessage(message: String) {
                currentOnShowMessage(message)
            }
        }
        MemoRuntimeBridge.addListener(listener)
        onDispose { MemoRuntimeBridge.removeListener(listener) }
    }

    fun refreshNow() {
        refreshCounter += 1
    }

    LaunchedEffect(context) {
        apps = queryMemoLaunchableApps(context)
        try {
            store = withContext(Dispatchers.IO) { SynthStore.shared(context.applicationContext) }
        } catch (error: Throwable) {
            loadError = error.message ?: "The encrypted app-workflow store could not be opened."
        }
    }

    LaunchedEffect(store, refreshCounter) {
        val activeStore = store ?: return@LaunchedEffect
        try {
            val data = withContext(Dispatchers.IO) { activeStore.getWorkflows() to activeStore.getRuns() }
            workflows = data.first
            runs = data.second
            accessibilityConnected = AutomationAccessibilityService.isConnected()
        } catch (error: Throwable) {
            loadError = error.message ?: "Could not refresh native workflow history."
        }
    }

    LaunchedEffect(store) {
        while (true) {
            delay(1_200)
            accessibilityConnected = AutomationAccessibilityService.isConnected()
            val activeStore = store ?: continue
            runCatching {
                val data = withContext(Dispatchers.IO) { activeStore.getWorkflows() to activeStore.getRuns() }
                workflows = data.first
                runs = data.second
            }.onFailure { loadError = it.message ?: "Could not refresh run history." }
        }
    }

    LaunchedEffect(runs) {
        val terminal = setOf(RunRecord.SUCCEEDED, RunRecord.UNVERIFIED, RunRecord.FAILED, RunRecord.CANCELLED, RunRecord.INTERRUPTED)
        val active = setOf(RunRecord.QUEUED, RunRecord.RUNNING, RunRecord.PAUSED_LOCKED, RunRecord.PAUSED_EXTERNAL)
        runs.forEach { run ->
            val previous = lastObservedRunStates[run.id]
            val newlyFinished = run.status in terminal && (
                previous in active || (previous == null && run.startedAt >= workspaceOpenedAt)
            )
            if (newlyFinished) MemoNotificationCenter.notifyWorkflowFinished(context, run.id, run.status)
        }
        lastObservedRunStates = runs.associate { it.id to it.status }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(18.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            SectionHeading(
                "Apps & automation",
                "Open real Android apps, build reusable workflows and verify visible outcomes. Nothing runs until you start it."
            )
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Surface(
                        color = if (accessibilityConnected) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.errorContainer,
                        shape = RoundedCornerShape(12.dp), modifier = Modifier.size(44.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                if (accessibilityConnected) Icons.Rounded.CheckCircle else Icons.Rounded.WarningAmber,
                                contentDescription = null,
                                tint = if (accessibilityConnected) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(if (accessibilityConnected) "Cross-app control is enabled" else "Cross-app control needs permission", style = MaterialTheme.typography.titleMedium)
                        Text(
                            if (accessibilityConnected) "Memo can observe supported visible labels and run your explicitly saved steps while the device is unlocked."
                            else "Android requires you to enable Memo in Accessibility settings. You can browse and manage workflows without granting it.",
                            style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    Button(onClick = {
                        runCatching { context.startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { onShowMessage("Could not open Accessibility settings: ${it.message.orEmpty()}") }
                    }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.Settings, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Accessibility settings")
                    }
                    OutlinedButton(onClick = { remoteSettings = true }, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Rounded.CloudUpload, contentDescription = null); Spacer(Modifier.width(6.dp)); Text("Guest runner")
                    }
                }
                OutlinedButton(onClick = {
                    if (!accessibilityConnected) onShowMessage("Enable Memo in Android Accessibility settings first.")
                    else if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(context)) {
                        runCatching { context.startActivity(Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:${context.packageName}")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                            .onFailure { onShowMessage("Could not open overlay permission settings: ${it.message.orEmpty()}") }
                    } else if (!AutomationAccessibilityService.showGlobalAssistant(context)) {
                        onShowMessage("Memo's Accessibility service is not connected yet. Return here after enabling it.")
                    } else onShowMessage("Floating Memo controls enabled. Capture remains manual; tap Capture when you want to save accessible screen labels.")
                }, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.Apps, contentDescription = null); Spacer(Modifier.width(7.dp)); Text("Enable floating Memo controls")
                }
                Text(
                    "Privacy boundary: Memo never reads editable-field values or passwords. Runs pause when the phone locks or the target app leaves the foreground. Every workflow ends with a visible-text check; passing it verifies that observation, not every possible side effect.",
                    style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (loadError.isNotBlank()) {
            item {
                SurfaceCard {
                    Text("Workflow storage issue", style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.error)
                    Text(loadError, style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = { loadError = ""; refreshNow() }) { Icon(Icons.Rounded.Refresh, null); Text("Retry") }
                }
            }
        }
        if (lastRemoteJobId.isNotBlank()) {
            item {
                SurfaceCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text("Last guest run", style = MaterialTheme.typography.titleMedium)
                            Text("${lastRemoteJobId.take(24)} · ${lastRemoteJobStatus.ifBlank { "status unknown" }}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        IconButton(enabled = !remoteBusy, onClick = {
                            remoteBusy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { runCatching { remoteClient.getJob(lastRemoteJobId) } }
                                result.onSuccess { job ->
                                    lastRemoteJobStatus = job.optString("status", "UNKNOWN")
                                    remoteJobDetail = job.optJSONArray("evidence")?.toString()?.take(4_000).orEmpty()
                                    remotePrefs.edit().putString("status", lastRemoteJobStatus).apply()
                                    onShowMessage("Guest run status: $lastRemoteJobStatus. A verified status requires the guest's final visible-text postcondition.")
                                }.onFailure { remoteJobDetail = "Status check failed: ${it.message.orEmpty()}"; onShowMessage(remoteJobDetail) }
                                remoteBusy = false
                            }
                        }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh guest run status") }
                    }
                    if (remoteJobDetail.isNotBlank()) Text(remoteJobDetail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 5, overflow = TextOverflow.Ellipsis)
                    if (lastRemoteJobStatus in setOf("QUEUED", "RUNNING")) {
                        TextButton(enabled = !remoteBusy, onClick = {
                            remoteBusy = true
                            scope.launch {
                                val result = withContext(Dispatchers.IO) { runCatching { remoteClient.cancel(lastRemoteJobId) } }
                                result.onSuccess { onShowMessage("Cancellation requested for guest job $lastRemoteJobId.") }
                                    .onFailure { onShowMessage("Cancel request failed: ${it.message.orEmpty()}") }
                                remoteBusy = false
                            }
                        }) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(5.dp)); Text("Request cancellation") }
                    }
                }
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Reusable workflows", style = MaterialTheme.typography.titleLarge)
                    Text("${workflows.size} saved · ${runs.size} recorded runs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Button(onClick = { editingWorkflow = null; showBuilder = true }, enabled = apps.isNotEmpty() && store != null) {
                    Icon(Icons.Rounded.Add, null); Spacer(Modifier.width(4.dp)); Text("New")
                }
            }
        }
        if (workflows.isEmpty()) {
            item {
                EmptyState("No native workflows yet", "Start with a repeatable task in an installed app. Add exact visible text for controls and a final success condition that Memo must observe.")
            }
        } else {
            items(workflows, key = { it.id }) { workflow ->
                NativeWorkflowCard(
                    workflow = workflow,
                    latestRun = runs.firstOrNull { it.workflowId == workflow.id },
                    canStartLocal = accessibilityConnected,
                    canStartRemote = remoteClient.isConfigured(),
                    onRunLocal = {
                        if (AutomationAccessibilityService.isExecuting()) {
                            onShowMessage("A native workflow is already running. Stop it or let it finish first.")
                        } else if (!AutomationAccessibilityService.requestRun(context, workflow)) {
                            onShowMessage("Memo's Accessibility service is not connected. Enable it in Android settings first.")
                        } else {
                            onShowMessage("Started '${workflow.name}' v${workflow.version}. Watch the target app and this run trace for the result.")
                            refreshNow()
                        }
                    },
                    onPreviewRemote = { remoteWorkflowPreview = workflow },
                    onEdit = { editingWorkflow = workflow; showBuilder = true },
                    onHistory = { historyWorkflow = workflow },
                    onDelete = { deleteWorkflow = workflow }
                )
            }
        }
        item {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("Run history", style = MaterialTheme.typography.titleLarge)
                    Text("Interrupted runs are never silently replayed.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = { refreshNow() }) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh run history") }
                if (AutomationAccessibilityService.isExecuting()) {
                    OutlinedButton(onClick = {
                        AutomationAccessibilityService.requestCancel()
                        onShowMessage("Stop requested. Memo will stop at the next safe action boundary.")
                        refreshNow()
                    }) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(5.dp)); Text("Stop") }
                }
            }
        }
        if (runs.isEmpty()) {
            item { Text("No app automation runs recorded.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant) }
        } else {
            items(runs.take(12), key = { it.id }) { run -> RunHistoryCard(run = run, onClick = { runDetails = run }) }
        }
        item {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f)) {
                        Text("Installed apps", style = MaterialTheme.typography.titleMedium)
                        Text("${apps.size} launchable apps visible to Memo", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    TextButton(onClick = { showAppInventory = !showAppInventory }) { Text(if (showAppInventory) "Hide" else "Browse") }
                }
                if (showAppInventory) {
                    apps.forEach { app ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
                            Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.primary)
                            Column(Modifier.weight(1f).padding(horizontal = 9.dp)) {
                                Text(app.label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                Text(app.packageName, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            IconButton(onClick = {
                                val launch = context.packageManager.getLaunchIntentForPackage(app.packageName)
                                if (launch == null) onShowMessage("Android did not expose a launch intent for ${app.label}.")
                                else runCatching { context.startActivity(launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
                                    .onFailure { onShowMessage("Could not open ${app.label}: ${it.message.orEmpty()}") }
                            }) { Icon(Icons.Rounded.OpenInNew, contentDescription = "Open ${app.label}") }
                            TextButton(onClick = {
                                editingWorkflow = null
                                preferredAppPackage = app.packageName
                                showBuilder = true
                            }) { Text("Workflow") }
                        }
                    }
                }
            }
        }
    }

    if (showBuilder) {
        NativeWorkflowEditorDialog(
            apps = apps,
            existing = editingWorkflow,
            preferredAppPackage = preferredAppPackage,
            onDismiss = { showBuilder = false; editingWorkflow = null; preferredAppPackage = null },
            onSave = { workflow ->
                val s = store
                if (s == null) {
                    onShowMessage("Encrypted workflow storage is unavailable.")
                } else {
                    scope.launch {
                        val error = withContext(Dispatchers.IO) {
                            runCatching { s.saveWorkflow(workflow); null }.getOrElse { it.message ?: "Could not save workflow." }
                        }
                        if (error == null) {
                            onShowMessage("Saved '${workflow.name}' v${workflow.version}. Runs pin this definition; future edits create a new revision.")
                            showBuilder = false
                            editingWorkflow = null
                            preferredAppPackage = null
                            refreshNow()
                        } else onShowMessage(error)
                    }
                }
            }
        )
    }

    if (deleteWorkflow != null) {
        val target = deleteWorkflow!!
        AlertDialog(
            onDismissRequest = { deleteWorkflow = null },
            title = { Text("Delete workflow?") },
            text = { Text("'${target.name}' and its active registry entry will be removed. Archived revisions and run history are retained.") },
            confirmButton = {
                Button(onClick = {
                    val activeStore = store
                    scope.launch {
                        val error = withContext(Dispatchers.IO) { runCatching { activeStore?.deleteWorkflow(target.id); null }.getOrElse { it.message ?: "Could not delete workflow." } }
                        onShowMessage(error ?: "Workflow deleted.")
                        deleteWorkflow = null
                        refreshNow()
                    }
                }) { Text("Delete") }
            },
            dismissButton = { TextButton(onClick = { deleteWorkflow = null }) { Text("Cancel") } }
        )
    }

    if (historyWorkflow != null) {
        val target = historyWorkflow!!
        val versions = remember(target.id, store, refreshCounter) { runCatching { store?.getWorkflowVersions(target.id).orEmpty() }.getOrDefault(emptyList()) }
        AlertDialog(
            onDismissRequest = { historyWorkflow = null },
            title = { Text("${target.name} · version history") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Current revision: v${target.version}", style = MaterialTheme.typography.labelLarge)
                    if (versions.isEmpty()) Text("No prior revisions. Editing this workflow will archive its previous definition before saving the new version.")
                    else versions.forEach { version ->
                        val versionNum = version.optInt("version", 1)
                        val stepCount = version.optJSONArray("steps")?.length() ?: 0
                        Text("v$versionNum · $stepCount steps · archived ${version.optLong("archivedAt", 0L)}", style = MaterialTheme.typography.bodySmall)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { historyWorkflow = null }) { Text("Close") } }
        )
    }

    if (runDetails != null) {
        val run = runDetails!!
        AlertDialog(
            onDismissRequest = { runDetails = null },
            title = { Text("${run.workflowName} · ${run.status.lowercase().replace('_', ' ')}") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    Text("Pinned workflow revision: v${run.workflowVersion}", style = MaterialTheme.typography.labelLarge)
                    Text(run.message, style = MaterialTheme.typography.bodyMedium)
                    run.events.takeLast(25).forEach { event ->
                        Text(event.substringAfter('|', event), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            },
            confirmButton = { TextButton(onClick = { runDetails = null }) { Text("Close") } }
        )
    }

    if (remoteSettings) {
        RemoteRunnerSettingsDialog(
            client = remoteClient,
            onDismiss = { remoteSettings = false },
            onSaved = { message -> remoteMessage = message; remoteSettings = false; onShowMessage(message) }
        )
    }

    if (remoteWorkflowPreview != null) {
        val workflow = remoteWorkflowPreview!!
        AlertDialog(
            onDismissRequest = { remoteWorkflowPreview = null },
            title = { Text("Approve guest execution?") },
            text = {
                Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("${workflow.name} · v${workflow.version}", style = MaterialTheme.typography.titleMedium)
                    Text("Target: ${workflow.targetLabel} (${workflow.targetPackage})", style = MaterialTheme.typography.bodySmall)
                    workflow.steps.forEachIndexed { index, step -> Text("${index + 1}. ${step.label()}", style = MaterialTheme.typography.bodySmall) }
                    Text("Only this bounded workflow definition is sent. Chat history, prompts, attachments and model context are not included. Execution happens on your separately paired device and is subject to its local policy.", style = MaterialTheme.typography.bodySmall)
                    Text("This action starts work on another device. Review every step and destination before approving.", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.error)
                    if (remoteMessage.isNotBlank()) Text(remoteMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            },
            confirmButton = {
                Button(enabled = !remoteBusy && remoteClient.isConfigured(), onClick = {
                    remoteBusy = true
                    scope.launch {
                        val outcome = withContext(Dispatchers.IO) {
                            runCatching { remoteClient.submit(workflow) }
                        }
                        outcome.onSuccess { result ->
                            lastRemoteJobId = result.optString("id", "")
                            lastRemoteJobStatus = result.optString("status", "QUEUED")
                            remotePrefs.edit().putString("jobId", lastRemoteJobId).putString("status", lastRemoteJobStatus).apply()
                            remoteMessage = "Guest runner accepted job $lastRemoteJobId · $lastRemoteJobStatus. This is queued, not proof of completion."
                            onShowMessage(remoteMessage)
                            remoteWorkflowPreview = null
                        }.onFailure { remoteMessage = "Guest submission failed: ${it.message.orEmpty()}"; onShowMessage(remoteMessage) }
                        remoteBusy = false
                    }
                }) { Text(if (remoteBusy) "Submitting…" else "Approve & send") }
            },
            dismissButton = { TextButton(onClick = { remoteWorkflowPreview = null }) { Text("Cancel") } }
        )
    }
}

@Composable
private fun NativeWorkflowCard(
    workflow: Workflow,
    latestRun: RunRecord?,
    canStartLocal: Boolean,
    canStartRemote: Boolean,
    onRunLocal: () -> Unit,
    onPreviewRemote: () -> Unit,
    onEdit: () -> Unit,
    onHistory: () -> Unit,
    onDelete: () -> Unit
) {
    SurfaceCard {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(40.dp)) {
                    Box(contentAlignment = Alignment.Center) { Icon(Icons.Rounded.Apps, null, tint = MaterialTheme.colorScheme.onPrimaryContainer) }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(workflow.name, style = MaterialTheme.typography.titleMedium)
                    Text("${workflow.targetLabel} · ${workflow.steps.size} steps · v${workflow.version}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(workflow.steps.lastOrNull()?.takeIf { it.type == WorkflowStep.WAIT_TEXT }?.let { "Success check: “${it.value}”" } ?: "Missing final success check", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                    if (latestRun != null) Text("Last run: ${latestRun.status.lowercase().replace('_', ' ')} · ${latestRun.message.take(120)}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                workflow.steps.take(6).forEachIndexed { index, step ->
                    Text("${index + 1}. ${step.label()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
                if (workflow.steps.size > 6) Text("+ ${workflow.steps.size - 6} more steps", style = MaterialTheme.typography.labelSmall)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = onRunLocal, enabled = canStartLocal && !AutomationAccessibilityService.isExecuting(), modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.PlayArrow, null); Spacer(Modifier.width(4.dp)); Text("Run on phone")
                }
                OutlinedButton(onClick = onPreviewRemote, enabled = canStartRemote, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Rounded.CloudUpload, null); Spacer(Modifier.width(4.dp)); Text("Run on guest")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(2.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onEdit) { Text("Edit") }
                TextButton(onClick = onHistory) { Icon(Icons.Rounded.History, null); Spacer(Modifier.width(4.dp)); Text("Versions") }
                Spacer(Modifier.weight(1f))
                IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, contentDescription = "Delete workflow", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}

@Composable
private fun RunHistoryCard(run: RunRecord, onClick: () -> Unit) {
    SurfaceCard {
        Column(Modifier.fillMaxWidth().clickable(onClick = onClick), verticalArrangement = Arrangement.spacedBy(5.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(run.workflowName, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
                Surface(color = if (run.status == RunRecord.SUCCEEDED) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(50.dp)) {
                    Text(run.status.lowercase().replace('_', ' '), modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text("v${run.workflowVersion} · ${run.message}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
            Text("Tap for trace and observations", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
        }
    }
}

@Composable
private fun NativeWorkflowEditorDialog(
    apps: List<MemoLaunchableApp>,
    existing: Workflow?,
    preferredAppPackage: String?,
    onDismiss: () -> Unit,
    onSave: (Workflow) -> Unit
) {
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: "New workflow") }
    var selectedApp by remember(existing?.id, apps, preferredAppPackage) { mutableStateOf(apps.firstOrNull { it.packageName == existing?.targetPackage } ?: apps.firstOrNull { it.packageName == preferredAppPackage } ?: apps.firstOrNull()) }
    val initialSteps = remember(existing?.id) {
        val workflowSteps = existing?.steps.orEmpty()
        workflowSteps.mapIndexedNotNull { index, step ->
            if (index == 0 && step.type == WorkflowStep.LAUNCH_APP) null
            else if (index == workflowSteps.lastIndex && step.type == WorkflowStep.WAIT_TEXT) null
            else DraftNativeStep(step.type, step.value, step.delayMillis)
        }
    }
    var draftSteps by remember(existing?.id) { mutableStateOf(initialSteps) }
    var successText by remember(existing?.id) { mutableStateOf(existing?.steps?.lastOrNull()?.takeIf { it.type == WorkflowStep.WAIT_TEXT }?.value.orEmpty()) }
    var kind by remember { mutableStateOf(WorkflowStep.TAP_TEXT) }
    var value by remember { mutableStateOf("") }
    var delaySeconds by remember { mutableStateOf("1") }
    var appMenu by remember { mutableStateOf(false) }
    var kindMenu by remember { mutableStateOf(false) }
    var validation by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (existing == null) "Create native workflow" else "Edit workflow · next revision") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 480.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it; validation = "" }, label = { Text("Workflow name") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Box {
                    OutlinedTextField(
                        value = selectedApp?.let { "${it.label} (${it.packageName})" }.orEmpty(),
                        onValueChange = {}, readOnly = true, enabled = apps.isNotEmpty(),
                        label = { Text("Target app") }, modifier = Modifier.fillMaxWidth().clickable { appMenu = true },
                        trailingIcon = { Icon(Icons.Rounded.Apps, contentDescription = "Choose app") }
                    )
                    DropdownMenu(expanded = appMenu, onDismissRequest = { appMenu = false }) {
                        apps.forEach { app -> DropdownMenuItem(text = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) }, onClick = { selectedApp = app; appMenu = false }) }
                    }
                }
                Text("Steps run in order. LAUNCH APP is inserted automatically; the final visible-text check is mandatory.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (draftSteps.isNotEmpty()) {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        draftSteps.forEachIndexed { index, step ->
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                Column(Modifier.weight(1f)) {
                                    Text("${index + 1}. ${draftLabel(step)}", style = MaterialTheme.typography.bodySmall)
                                    if (step.value.isNotBlank()) Text(step.value, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                                }
                                IconButton(onClick = { draftSteps = draftSteps.toMutableList().also { it.removeAt(index) } }) { Icon(Icons.Rounded.Delete, contentDescription = "Remove step") }
                            }
                        }
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        OutlinedButton(onClick = { kindMenu = true }) { Text(kindLabel(kind)) }
                        DropdownMenu(expanded = kindMenu, onDismissRequest = { kindMenu = false }) {
                            listOf(WorkflowStep.TAP_TEXT, WorkflowStep.WAIT_TEXT, WorkflowStep.BACK, WorkflowStep.DELAY).forEach { option ->
                                DropdownMenuItem(text = { Text(kindLabel(option)) }, onClick = { kind = option; kindMenu = false; value = "" })
                            }
                        }
                    }
                    if (kind == WorkflowStep.DELAY) {
                        OutlinedTextField(value = delaySeconds, onValueChange = { delaySeconds = it.filter(Char::isDigit).take(2) }, label = { Text("Seconds") }, modifier = Modifier.weight(1f), keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number), singleLine = true)
                    } else if (kind == WorkflowStep.TAP_TEXT || kind == WorkflowStep.WAIT_TEXT) {
                        OutlinedTextField(value = value, onValueChange = { value = it; validation = "" }, label = { Text("Exact visible text") }, modifier = Modifier.weight(1f), singleLine = true)
                    } else {
                        Text("Press Back", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                    }
                    Button(onClick = {
                        val step = when (kind) {
                            WorkflowStep.TAP_TEXT, WorkflowStep.WAIT_TEXT -> if (value.isBlank()) null else DraftNativeStep(kind, value.trim())
                            WorkflowStep.BACK -> DraftNativeStep(kind)
                            WorkflowStep.DELAY -> (delaySeconds.toIntOrNull()?.takeIf { it in 0..30 })?.let { DraftNativeStep(kind, delayMillis = it * 1000) }
                            else -> null
                        }
                        if (step == null) validation = if (kind == WorkflowStep.DELAY) "Delay must be 0–30 seconds." else "Enter the required visible text first."
                        else { draftSteps = draftSteps + step; value = ""; validation = "" }
                    }) { Icon(Icons.Rounded.Add, null) }
                }
                OutlinedTextField(value = successText, onValueChange = { successText = it; validation = "" }, label = { Text("Final success condition · visible text") }, modifier = Modifier.fillMaxWidth(), minLines = 1, maxLines = 2)
                Text("Memo will mark the run verified only if it observes this text at the end. It will not type into editable fields, enter passwords, approve payments, solve CAPTCHAs, or automate reward/ad engagement.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (validation.isNotBlank()) Text(validation, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
            }
        },
        confirmButton = {
            Button(onClick = {
                val app = selectedApp
                if (app == null) { validation = "Choose an installed app."; return@Button }
                if (successText.isBlank()) { validation = "Add the final visible-text success condition."; return@Button }
                val workflow = existing ?: Workflow(name.trim(), app.packageName, app.label)
                workflow.name = name.trim()
                workflow.targetPackage = app.packageName
                workflow.targetLabel = app.label
                workflow.steps.clear()
                workflow.steps.add(WorkflowStep(WorkflowStep.LAUNCH_APP, app.label, 0))
                draftSteps.forEach { workflow.steps.add(WorkflowStep(it.type, it.value, it.delayMillis)) }
                workflow.steps.add(WorkflowStep(WorkflowStep.WAIT_TEXT, successText.trim(), 0))
                val error = WorkflowPolicy.validate(workflow)
                if (error != null) validation = error else onSave(workflow)
            }, enabled = apps.isNotEmpty()) { Text("Save workflow") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}

@Composable
private fun RemoteRunnerSettingsDialog(client: RemoteRunnerClient, onDismiss: () -> Unit, onSaved: (String) -> Unit) {
    var endpoint by remember { mutableStateOf(client.endpoint()) }
    var token by remember { mutableStateOf("") }
    var status by remember { mutableStateOf("") }
    var busy by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Remote guest runner") },
        text = {
            Column(Modifier.fillMaxWidth().heightIn(max = 420.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Configure a separately paired Android guest. The endpoint must use trusted HTTPS; the bearer token is encrypted with Android Keystore.", style = MaterialTheme.typography.bodySmall)
                OutlinedTextField(value = endpoint, onValueChange = { endpoint = it; status = "" }, label = { Text("HTTPS base URL") }, modifier = Modifier.fillMaxWidth(), singleLine = true, placeholder = { Text("https://runner.example/ ") })
                OutlinedTextField(value = token, onValueChange = { token = it; status = "" }, label = { Text("Bearer token (32+ characters)") }, modifier = Modifier.fillMaxWidth(), singleLine = true)
                Text("Leave the token blank to retain an already saved token. No chat history or model context is uploaded.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (status.isNotBlank()) Text(status, style = MaterialTheme.typography.bodySmall, color = if (status.startsWith("Error")) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary)
            }
        },
        confirmButton = {
            Button(enabled = !busy && endpoint.isNotBlank(), onClick = {
                busy = true
                scope.launch {
                    val result = withContext(Dispatchers.IO) { runCatching { client.saveConfig(endpoint, token); client.testConnection() } }
                    result.onSuccess { health ->
                        val message = "Connected to runner protocol ${health.optString("protocol", "unknown")}; guest connected=${health.optBoolean("guestConnected", false)}; AI policy=${health.optString("aiPolicy", "unknown")}."
                        status = message
                        onSaved(message)
                    }.onFailure { status = "Error: ${it.message.orEmpty()}" }
                    busy = false
                }
            }) { Text(if (busy) "Testing…" else "Save & test") }
        },
        dismissButton = {
            Row {
                TextButton(onClick = {
                    scope.launch {
                        withContext(Dispatchers.IO) { client.clearConfig() }
                        onSaved("Remote guest runner credentials cleared.")
                    }
                }) { Text("Clear") }
                TextButton(onClick = onDismiss) { Text("Cancel") }
            }
        }
    )
}

private fun queryMemoLaunchableApps(context: Context): List<MemoLaunchableApp> {
    val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
    val packageManager = context.packageManager
    val list = try {
        if (Build.VERSION.SDK_INT >= 33) packageManager.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0))
        else {
            @Suppress("DEPRECATION")
            packageManager.queryIntentActivities(intent, 0)
        }
    } catch (_: Throwable) { emptyList() }
    return list.mapNotNull { info ->
        val packageName = info.activityInfo?.packageName ?: return@mapNotNull null
        if (packageName == context.packageName) return@mapNotNull null
        val label = runCatching { info.loadLabel(packageManager).toString() }.getOrDefault(packageName)
        MemoLaunchableApp(packageName, label)
    }.distinctBy { it.packageName }.sortedBy { it.label.lowercase() }
}

private fun kindLabel(type: String): String = when (type) {
    WorkflowStep.TAP_TEXT -> "Tap text"
    WorkflowStep.WAIT_TEXT -> "Wait for text"
    WorkflowStep.BACK -> "Back"
    WorkflowStep.DELAY -> "Delay"
    else -> "Action"
}

private fun draftLabel(step: DraftNativeStep): String = when (step.type) {
    WorkflowStep.TAP_TEXT -> "Tap visible text"
    WorkflowStep.WAIT_TEXT -> "Wait until visible text"
    WorkflowStep.BACK -> "Press Back"
    WorkflowStep.DELAY -> "Wait ${step.delayMillis / 1000}s"
    else -> "Unsupported step"
}
