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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import app.memo.android.data.AppSnapshot
import app.memo.android.data.LiveTimelineEntry
import app.memo.android.data.LiveTimelineKind
import app.memo.android.data.LiveTimelineProjector
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
    onOpenBrowser: () -> Unit,
    onOpenApps: () -> Unit,
    onOpenApprovals: () -> Unit,
    onOpenWork: () -> Unit,
    onOpenTalk: (String) -> Unit,
    onResumeTask: (TaskRecord) -> Unit
) {
    val timeline = remember(snapshot) { LiveTimelineProjector.build(snapshot) }
    val tasksById = remember(snapshot.tasks) { snapshot.tasks.associateBy { it.id } }
    val jobsById = remember(snapshot.jobs) { snapshot.jobs.associateBy { it.id } }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = MemoTokens.Spacing.pageMargin, vertical = MemoTokens.Spacing.md),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)
    ) {
        item {
            MemoPageHeader(
                title = "Live",
                eyebrow = "MEMO / LIVE CONTROL",
                description = "One timeline of recorded work, checkpoints, actions and evidence. A recorded status is not proof of an external result."
            )
        }
        item {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedButton(onClick = onOpenBrowser, modifier = Modifier.weight(1f).height(MemoTokens.Spacing.minimumTouch)) {
                    Text("Browser session")
                }
                OutlinedButton(onClick = onOpenApps, modifier = Modifier.weight(1f).height(MemoTokens.Spacing.minimumTouch)) {
                    Text("Phone apps")
                }
            }
            OutlinedButton(onClick = onOpenApprovals, modifier = Modifier.fillMaxWidth().height(MemoTokens.Spacing.minimumTouch)) {
                val pending = snapshot.actionApprovals.count { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS", "NEEDS_REVIEW") }
                Text(if (pending == 0) "Approval inbox" else "Review actions · $pending")
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
                        body = "A visible browser session will appear here while it is active. Its saved outcomes are listed in the timeline below."
                    )
                }
            }
        }
        item {
            MemoSectionHeading(
                "Workspace timeline",
                supporting = "${timeline.size} recorded entries · newest first"
            )
        }
        if (timeline.isEmpty()) {
            item {
                EmptyState(
                    title = "No activity recorded yet",
                    body = "Plans, checkpoints, tasks, action decisions and captured evidence will appear here as Memo records them."
                )
            }
        } else {
            items(timeline, key = { it.id }) { entry ->
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    MemoPanel(modifier = Modifier.clickable {
                        when (entry.kind) {
                            LiveTimelineKind.ACTION -> onOpenApprovals()
                            LiveTimelineKind.TASK -> {
                                val task = tasksById[entry.relatedId]
                                if (task != null && task.status in setOf(TaskStatus.QUEUED, TaskStatus.NEEDS_REVIEW, TaskStatus.FAILED, TaskStatus.BLOCKED)) {
                                    onResumeTask(task)
                                } else {
                                    onOpenWork()
                                }
                            }
                            else -> onOpenWork()
                        }
                    }) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(entry.kind.label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(formatLiveTime(entry.occurredAt), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Spacer(Modifier.height(3.dp))
                        Text(entry.title, style = MaterialTheme.typography.titleSmall)
                        if (entry.detail.isNotBlank()) {
                            Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        MemoStatusTag(
                            label = entry.status.replace('_', ' '),
                            tone = when (entry.status.uppercase()) {
                                "COMPLETED", "SUCCEEDED", "CAPTURED" -> MemoStatusTone.SUCCESS
                                "FAILED", "BLOCKED", "ERROR" -> MemoStatusTone.ERROR
                                "NEEDS_REVIEW", "PENDING", "WAITING_FOR_USER", "WARN" -> MemoStatusTone.WARNING
                                else -> MemoStatusTone.INFO
                            }
                        )
                    }
                    if (entry.kind == LiveTimelineKind.JOB) {
                        val job = jobsById[entry.relatedId]
                        if (job?.sourceConversationId?.isNotBlank() == true) {
                            TextButton(onClick = { onOpenTalk(job.sourceConversationId) }) {
                                Text("Open originating conversation")
                            }
                        }
                    }
                }
            }
        }
    }
}

private fun formatLiveTime(timestamp: Long): String = runCatching {
    SimpleDateFormat("dd MMM · HH:mm", Locale.getDefault()).format(Date(timestamp))
}.getOrDefault("Unknown time")
