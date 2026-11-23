package app.memo.android.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import app.memo.android.ui.MemoText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.memo.android.data.ActionApprovalRecord
import app.memo.android.data.AgentRunRecord
import app.memo.android.data.AgentRunContinuationPolicy
import app.memo.android.data.AppSnapshot
import app.memo.android.data.TaskRecord
import app.memo.android.data.TaskStatus
import app.memo.android.browser.BrowserWorkflowUiState
import app.memo.android.ui.components.EmptyState
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun LiveWorkspaceScreen(
    snapshot: AppSnapshot,
    browserState: BrowserWorkflowUiState,
    uiLanguage: String,
    onOpenBrowser: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenApprovals: () -> Unit,
    onOpenWork: () -> Unit,
    onOpenTalk: (String) -> Unit,
    onContinueRun: (AgentRunRecord) -> Unit,
    onResumeTask: (TaskRecord) -> Unit
) {
    val activeJobs = snapshot.jobs
        .filter { it.status in setOf("RUNNING", "WAITING_FOR_USER", "NEEDS_REVIEW", "FAILED", "BLOCKED") }
        .sortedByDescending { it.updatedAt }
    val attentionTasks = snapshot.tasks
        .filter { it.status in setOf(TaskStatus.QUEUED, TaskStatus.RUNNING, TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED, TaskStatus.BLOCKED) }
        .sortedByDescending { it.updatedAt }
    val reviewActions = snapshot.actionApprovals
        .filter { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS", "NEEDS_REVIEW") }
        .sortedByDescending { it.createdAt }
    val recentAgentRuns = snapshot.agentRuns.sortedByDescending { it.updatedAt }.take(6)
    val recentActivity = snapshot.activity.sortedByDescending { it.at }.take(10)

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)
    ) {
        item {
            MemoPageHeader(
                title = "Live",
                eyebrow = "MEMO / LIVE CONTROL",
                description = "See active work, browser sessions, checkpoints and decisions in one place. Memo only reports completion when it has an observable result."
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenBrowser, modifier = Modifier.weight(1f).height(MemoTokens.Spacing.minimumTouch)) { Text("Browser session") }
                OutlinedButton(onClick = onOpenApps, modifier = Modifier.weight(1f).height(MemoTokens.Spacing.minimumTouch)) { Text("Phone apps") }
            }
            OutlinedButton(onClick = onOpenApprovals, modifier = Modifier.fillMaxWidth().height(MemoTokens.Spacing.minimumTouch)) {
                if (reviewActions.isEmpty()) {
                    Text("Approval inbox")
                } else {
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                        Text("Review action(s)")
                        Text(reviewActions.size.toString(), style = MaterialTheme.typography.labelLarge)
                    }
                }
            }
        }
        item {
            MemoPanel {
                MemoSectionHeading("Current browser session", supporting = "Visible frontier-AI and website work")
                Spacer(Modifier.height(8.dp))
                if (browserState.isActive) {
                    Text(
                        browserState.title.ifBlank { browserState.currentProviderName.ifBlank { "Browser workflow" } },
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(
                        "${browserState.status.name.replace('_', ' ')} · ${browserState.currentStep.coerceAtLeast(0)}/${browserState.totalSteps}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary
                    )
                    if (browserState.currentProviderName.isNotBlank()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(5.dp), verticalAlignment = Alignment.CenterVertically) {
                            Text("Current participant", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(browserState.currentProviderName, style = MaterialTheme.typography.bodySmall)
                        }
                    }
                    Text(browserState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    TextButton(onClick = onOpenBrowser) { Text("Inspect live browser") }
                } else {
                    EmptyState(
                        title = "No active browser workflow",
                        body = "When a website or frontier-AI workflow is running, its real session and approval checkpoint will appear here."
                    )
                }
            }
        }
        item { MemoSectionHeading("Memo executions", supporting = "Recent Talk runs and their recorded outcome") }
        if (recentAgentRuns.isEmpty()) {
            item { EmptyState("No Memo executions yet", "Your Talk runs will appear here with their last durable stage and recovery state.") }
        } else {
            items(recentAgentRuns, key = { "agent-run-${it.id}" }) { run ->
                AgentRunSummary(
                    run,
                    onOpenConversation = { onOpenTalk(run.conversationId) },
                    onOpenApprovals = onOpenApprovals,
                    onContinueRun = { onContinueRun(run) },
                    uiLanguage = uiLanguage
                )
            }
        }
        item { MemoSectionHeading("Work in motion", supporting = "Resumable plans and tasks that need attention") }
        if (activeJobs.isEmpty() && attentionTasks.isEmpty()) {
            item {
                EmptyState(
                    title = "Nothing needs attention right now",
                    body = "New work will appear here with its current state, next checkpoint and evidence."
                )
            }
        } else {
            items(activeJobs.take(6), key = { "job-${it.id}" }) { job ->
                MemoPanel(modifier = Modifier.clickable(onClick = onOpenWork)) {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(job.title, style = MaterialTheme.typography.titleMedium)
                            Text(job.goal.substringBefore("\n\nMemo route:").take(180), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(
                                "${job.status.lowercase().replace('_', ' ')} · ${job.steps.count { it.status == "COMPLETED" }}/${job.steps.size} checkpoints",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        TextButton(onClick = onOpenWork) { Text("Open") }
                    }
                    val activeStep = job.steps.firstOrNull { it.status in setOf("RUNNING", "NEEDS_REVIEW") }
                    if (activeStep != null) {
                        Text("Next checkpoint", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(activeStep.title, style = MaterialTheme.typography.bodySmall)
                        if (activeStep.detail.isNotBlank()) Text(activeStep.detail, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    if (job.sourceConversationId.isNotBlank()) {
                        TextButton(onClick = { onOpenTalk(job.sourceConversationId) }) { Text("Open originating conversation") }
                    }
                }
            }
            items(attentionTasks.take(6), key = { "task-${it.id}" }) { task ->
                MemoPanel {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(task.title, style = MaterialTheme.typography.titleSmall)
                            Text(task.lastMessage, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2)
                        }
                        MemoStatusTag(
                            label = task.status.name.replace('_', ' '),
                            tone = when (task.status) {
                                TaskStatus.NEEDS_REVIEW -> MemoStatusTone.WARNING
                                TaskStatus.FAILED, TaskStatus.BLOCKED -> MemoStatusTone.ERROR
                                else -> MemoStatusTone.INFO
                            }
                        )
                    }
                    if (task.status in setOf(TaskStatus.QUEUED, TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED, TaskStatus.BLOCKED)) {
                        OutlinedButton(onClick = { onResumeTask(task) }, modifier = Modifier.fillMaxWidth()) {
                            Text(if (task.status == TaskStatus.NEEDS_REVIEW) "Inspect before resuming" else "Open task")
                        }
                    }
                }
            }
        }
        item {
            MemoPanel {
                MemoSectionHeading("Action decisions", supporting = "External actions stay blocked until you approve the exact proposal")
                Spacer(Modifier.height(8.dp))
                if (reviewActions.isEmpty()) {
                    Text("No external action is waiting for a decision.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                } else {
                    reviewActions.take(4).forEach { action -> ActionSummary(action, onOpenApprovals) }
                    TextButton(onClick = onOpenApprovals) { Text("Open approval inbox") }
                }
            }
        }
        item { MemoSectionHeading("Recent activity", supporting = "Recorded events from this workspace") }
        if (recentActivity.isEmpty()) {
            item { EmptyState("No activity recorded yet", "Memo will record meaningful workflow, capture and review events here.") }
        } else {
            items(recentActivity, key = { it.id }) { event ->
                MemoPanel {
                    Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                        MemoStatusTag(
                            label = event.level,
                            tone = when (event.level) {
                                "ERROR" -> MemoStatusTone.ERROR
                                "WARN" -> MemoStatusTone.WARNING
                                else -> MemoStatusTone.INFO
                            }
                        )
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                            Text(event.message, style = MaterialTheme.typography.bodySmall)
                            Text(formatLiveTime(event.at), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }
    }
}


@Composable
private fun AgentRunSummary(
    run: AgentRunRecord,
    onOpenConversation: () -> Unit,
    onOpenApprovals: () -> Unit,
    onContinueRun: () -> Unit,
    uiLanguage: String
) {
    val continuation = AgentRunContinuationPolicy.assess(run)
    val tone = when (run.status) {
        "NEEDS_REVIEW", "WAITING_FOR_APPROVAL" -> MemoStatusTone.WARNING
        "FAILED" -> MemoStatusTone.ERROR
        "COMPLETED" -> MemoStatusTone.SUCCESS
        else -> MemoStatusTone.INFO
    }
    MemoPanel {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(run.goal, style = MaterialTheme.typography.titleSmall, maxLines = 3)
            MemoStatusTag(label = agentRunStatusLabel(run.status), tone = tone)
            if (run.stage.isNotBlank()) {
                Text("Last recorded stage", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.stage, style = MaterialTheme.typography.bodySmall, maxLines = 3)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Column {
                    Text("Model calls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.modelCalls.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Capability calls", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.toolCalls.toString(), style = MaterialTheme.typography.bodyMedium)
                }
                Column {
                    Text("Succeeded", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(run.successfulToolCalls.toString(), style = MaterialTheme.typography.bodyMedium)
                }
            }
            if (run.lastEventType.isNotBlank()) {
                Text("Last durable event", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.lastEventType.replace('_', ' ').lowercase().replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodySmall)
            }
            if (run.lastToolId.isNotBlank()) {
                Text("Last capability", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.lastToolId, style = MaterialTheme.typography.bodySmall)
            }
            if (run.lastEvidenceReference.isNotBlank()) {
                Text("Last evidence reference", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.lastEvidenceReference, style = MaterialTheme.typography.bodySmall, maxLines = 2)
            }
            if (run.pendingApprovalCallId.isNotBlank()) {
                MemoStatusTag(label = "Approval call requires inspection", tone = MemoStatusTone.WARNING)
                Text("A pending approval reference is recorded. It is not reusable after restart.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (run.parentRunId.isNotBlank()) {
                Text("Continues prior run", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(run.parentRunId.take(8), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (run.recoveryReason.isNotBlank()) {
                Text(run.recoveryReason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (run.status == "NEEDS_REVIEW") {
                Text("This run is preserved as history. Memo will not replay its last step automatically.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary)
            }
            if (run.status == "NEEDS_REVIEW") {
                if (continuation.allowed) {
                    OutlinedButton(onClick = onContinueRun, modifier = Modifier.fillMaxWidth().height(MemoTokens.Spacing.minimumTouch)) {
                        Text("Continue in Talk")
                    }
                } else {
                    Text(MemoLocalization.translate(continuation.reason, uiLanguage), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onOpenConversation) { Text("Open conversation") }
                if (run.status in setOf("WAITING_FOR_APPROVAL", "NEEDS_REVIEW")) {
                    TextButton(onClick = onOpenApprovals) { Text("Review actions") }
                }
            }
        }
    }
}

private fun agentRunStatusLabel(status: String): String = when (status) {
    "RUNNING" -> "Running"
    "WAITING_FOR_APPROVAL" -> "Waiting for approval"
    "TOOL_RUNNING" -> "Using a capability"
    "FINALIZING" -> "Saving result"
    "COMPLETED" -> "Completed"
    "LIMIT_REACHED" -> "Limit reached"
    "NEEDS_REVIEW" -> "Needs review"
    "FAILED" -> "Failed"
    "CANCELLED" -> "Stopped"
    else -> "Needs review"
}

@Composable
private fun ActionSummary(action: ActionApprovalRecord, onOpen: () -> Unit) {
    Column(Modifier.fillMaxWidth().padding(vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(action.title, style = MaterialTheme.typography.titleSmall)
        Text(action.destination, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
        MemoStatusTag(
            label = action.status.replace('_', ' '),
            tone = when (action.status) {
                "PENDING", "NEEDS_REVIEW" -> MemoStatusTone.WARNING
                "FAILED", "BLOCKED" -> MemoStatusTone.ERROR
                "COMPLETED", "SUCCEEDED" -> MemoStatusTone.SUCCESS
                else -> MemoStatusTone.INFO
            }
        )
        Text("${action.runtimeId} · ${action.capabilityId}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        TextButton(onClick = onOpen) { Text("Review exact action") }
    }
}

private fun formatLiveTime(timestamp: Long): String = runCatching {
    SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))
}.getOrDefault("Unknown time")
