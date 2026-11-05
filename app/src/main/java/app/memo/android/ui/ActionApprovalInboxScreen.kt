package app.memo.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import app.memo.android.ui.MemoText as Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.memo.android.browser.BrowserWorkflowUiState
import app.memo.android.data.ActionApprovalRecord
import app.memo.android.data.AppSnapshot
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class ApprovalFilter(val label: String) {
    QUEUE("Action queue"), REVIEW("Needs review"), HISTORY("History")
}

@Composable
fun ActionApprovalInboxScreen(
    snapshot: AppSnapshot,
    browserState: BrowserWorkflowUiState,
    onOpenBrowser: () -> Unit,
    onReject: (String) -> Boolean,
    onMarkNeedsReview: (String, String) -> Boolean,
    onShowMessage: (String) -> Unit
) {
    var filter by remember { mutableStateOf(ApprovalFilter.QUEUE) }
    var expandedId by remember { mutableStateOf("") }
    val pendingCount = snapshot.actionApprovals.count { it.status == "PENDING" }
    val reviewCount = snapshot.actionApprovals.count { it.status == "NEEDS_REVIEW" }
    val activeCount = snapshot.actionApprovals.count { it.status in setOf("APPROVED", "IN_PROGRESS") }
    val rows = when (filter) {
        ApprovalFilter.QUEUE -> snapshot.actionApprovals.filter { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS") }
        ApprovalFilter.REVIEW -> snapshot.actionApprovals.filter { it.status == "NEEDS_REVIEW" }
        ApprovalFilter.HISTORY -> snapshot.actionApprovals.filter { it.status !in setOf("PENDING", "APPROVED", "IN_PROGRESS", "NEEDS_REVIEW") }
    }.sortedByDescending { it.createdAt }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text("MEMO / CONTROL", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.height(5.dp))
            Text("Action approval inbox", style = MaterialTheme.typography.headlineLarge, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(5.dp))
            Text("Inspect the exact content Memo proposes to send, see uncertain outcomes, and review the audit trail. This inbox never replays an action on its own.", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CountPanel("Waiting", pendingCount, Modifier.weight(1f))
                CountPanel("Active", activeCount, Modifier.weight(1f))
                CountPanel("Needs review", reviewCount, Modifier.weight(1f))
            }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(7.dp), modifier = Modifier.fillMaxWidth()) {
                ApprovalFilter.values().forEach { option ->
                    FilterChip(
                        selected = filter == option,
                        onClick = { filter = option },
                        label = { Text(option.label) }
                    )
                }
            }
        }
        if (rows.isEmpty()) {
            item {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f), shape = RoundedCornerShape(18.dp), modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(if (filter == ApprovalFilter.REVIEW) "No actions need review" else if (filter == ApprovalFilter.HISTORY) "No resolved actions yet" else "No action is waiting", style = MaterialTheme.typography.titleMedium)
                        Text("External browser prompts appear here before sending. The exact-content confirmation remains in the live browser workflow.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
        items(rows, key = { it.id }) { action ->
            val liveApproval = action.status == "PENDING" && browserState.awaitingPromptApproval &&
                browserState.currentPromptHash == action.contentHash && browserState.approvalDestination == action.destination &&
                action.capabilityId == "browser.submit_prompt"
            ActionApprovalCard(
                action = action,
                expanded = expandedId == action.id,
                liveApproval = liveApproval,
                auditEvents = snapshot.actionAuditEvents.filter { it.actionId == action.id }.takeLast(5),
                onToggle = { expandedId = if (expandedId == action.id) "" else action.id },
                onOpenBrowser = onOpenBrowser,
                onReject = {
                    val accepted = onReject(action.id)
                    onShowMessage(if (accepted) "Action rejected. Memo will not send this proposal." else "The proposal could not be rejected. Review the action ledger.")
                },
                onMarkNeedsReview = {
                    val accepted = onMarkNeedsReview(action.id, "User moved this action to review from the action inbox. Inspect the external service before deciding what to do next.")
                    onShowMessage(if (accepted) "The action is marked for review. Memo will not retry it automatically." else "The action could not move to review. Inspect its audit history.")
                }
            )
        }
        item {
            Text("Safety rule: a saved approval is not permission to reuse a browser session after its state changes. If Memo restarts or the outcome is uncertain, review the external service before creating a new action.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun CountPanel(label: String, count: Int, modifier: Modifier = Modifier) {
    Surface(modifier = modifier, shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)) {
        Column(Modifier.padding(11.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(count.toString(), style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
        }
    }
}

@Composable
private fun ActionApprovalCard(
    action: ActionApprovalRecord,
    expanded: Boolean,
    liveApproval: Boolean,
    auditEvents: List<app.memo.android.data.ActionAuditEvent>,
    onToggle: () -> Unit,
    onOpenBrowser: () -> Unit,
    onReject: () -> Unit,
    onMarkNeedsReview: () -> Unit
) {
    Surface(shape = RoundedCornerShape(18.dp), color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.30f), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.Top, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text(action.title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(action.destination, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    Text("${statusLabel(action.status)} · ${formatApprovalDate(action.createdAt)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Surface(shape = RoundedCornerShape(100.dp), color = statusContainer(action.status)) {
                    Text(statusLabel(action.status), modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp), style = MaterialTheme.typography.labelSmall)
                }
            }
            Text(if (expanded) "Exact content" else action.payload.take(260) + if (action.payload.length > 260) "…" else "", style = MaterialTheme.typography.bodySmall, maxLines = if (expanded) Int.MAX_VALUE else 5, overflow = TextOverflow.Ellipsis)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onToggle) { Text(if (expanded) "Show less" else "Inspect payload and audit") }
                if (liveApproval) Button(onClick = onOpenBrowser) { Text("Open live approval") }
            }
            if (expanded) {
                SelectionContainer {
                    Column(Modifier.fillMaxWidth().heightIn(max = 400.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Exact outbound payload", style = MaterialTheme.typography.labelLarge)
                        Text(action.payload, style = MaterialTheme.typography.bodySmall)
                    }
                }
                Text("Content hash", style = MaterialTheme.typography.labelLarge)
                SelectionContainer { Text(action.contentHash, style = MaterialTheme.typography.labelSmall) }
                Text("Runtime · ${action.runtimeId}", style = MaterialTheme.typography.bodySmall)
                Text("Capability · ${action.capabilityId}", style = MaterialTheme.typography.bodySmall)
                Text("Expiry · ${if (action.expiresAt > 0L) formatApprovalDate(action.expiresAt) else "not available"}", style = MaterialTheme.typography.bodySmall)
                if (action.evidenceRef.isNotBlank()) Text("Evidence · ${action.evidenceRef}", style = MaterialTheme.typography.bodySmall)
                if (action.resolution.isNotBlank()) Text(action.resolution, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Audit events", style = MaterialTheme.typography.labelLarge)
                if (auditEvents.isEmpty()) Text("No audit events found.", style = MaterialTheme.typography.bodySmall)
                auditEvents.forEach { event ->
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text("${event.sequence}. ${event.eventType} · ${event.actor}", style = MaterialTheme.typography.labelMedium)
                        Text("${formatApprovalDate(event.at)} · ${event.details}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            if (action.status == "PENDING") {
                if (!liveApproval) Text("This proposal is not connected to the matching live prompt. Memo will not approve or send it from history.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = onReject, modifier = Modifier.fillMaxWidth()) { Text("Reject proposal") }
            } else if (action.status == "NEEDS_REVIEW" || action.status == "IN_PROGRESS" || action.status == "APPROVED") {
                Text("Do not retry automatically. Inspect the destination and confirm whether the side effect occurred. Marking for review does not undo a submission already started.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (action.status != "NEEDS_REVIEW") OutlinedButton(onClick = onMarkNeedsReview, modifier = Modifier.fillMaxWidth()) { Text("Mark outcome for review") }
            }
        }
    }
}

private fun statusLabel(status: String): String = when (status) {
    "PENDING" -> "Pending"
    "APPROVED" -> "Approved"
    "IN_PROGRESS" -> "In progress"
    "NEEDS_REVIEW" -> "Needs review"
    "COMPLETED" -> "Completed"
    "REJECTED" -> "Rejected"
    "EXPIRED" -> "Expired"
    "INVALIDATED" -> "Invalidated"
    "FAILED" -> "Failed"
    "CANCELLED" -> "Cancelled"
    else -> status.lowercase().replace('_', ' ')
}

@Composable
private fun statusContainer(status: String) = when (status) {
    "REJECTED", "FAILED", "CANCELLED", "INVALIDATED", "EXPIRED" -> MaterialTheme.colorScheme.errorContainer
    "COMPLETED" -> MaterialTheme.colorScheme.tertiaryContainer
    "NEEDS_REVIEW", "IN_PROGRESS", "APPROVED", "PENDING" -> MaterialTheme.colorScheme.primaryContainer
    else -> MaterialTheme.colorScheme.surfaceVariant
}

private fun formatApprovalDate(timestamp: Long): String = runCatching {
    SimpleDateFormat("yyyy-MM-dd · HH:mm", Locale.getDefault()).format(Date(timestamp))
}.getOrDefault("Unknown time")
