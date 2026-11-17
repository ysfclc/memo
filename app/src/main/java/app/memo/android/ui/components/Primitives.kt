package app.memo.android.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import app.memo.android.ui.MemoText as Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import app.memo.android.data.TaskStatus
import app.memo.android.ui.MemoPanel
import app.memo.android.ui.MemoSectionHeading
import app.memo.android.ui.MemoStatusTag
import app.memo.android.ui.MemoStatusTone
import app.memo.android.ui.MemoTokens

/**
 * Compatibility adapter for older screens. Visual styling lives in MemoPanel.
 * Keep this thin until all callers have migrated to the canonical primitive.
 */
@Composable
fun SurfaceCard(
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit
) {
    MemoPanel(modifier = modifier, content = content)
}

/**
 * Compatibility adapter for older screens. New screen code should call
 * MemoSectionHeading directly so there is only one section-heading style.
 */
@Composable
fun SectionHeading(
    title: String,
    subtitle: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    MemoSectionHeading(title = title, supporting = subtitle, trailing = trailing)
}

/** Domain-aware status adapter; the visual treatment is owned by MemoStatusTag. */
@Composable
fun StatusPill(status: TaskStatus) {
    val tone = when (status) {
        TaskStatus.COMPLETED -> MemoStatusTone.SUCCESS
        TaskStatus.FAILED, TaskStatus.BLOCKED -> MemoStatusTone.ERROR
        TaskStatus.NEEDS_REVIEW -> MemoStatusTone.WARNING
        TaskStatus.RUNNING -> MemoStatusTone.INFO
        TaskStatus.QUEUED -> MemoStatusTone.NEUTRAL
    }
    MemoStatusTag(label = status.label, tone = tone)
}

/** A compact semantic dot label used where a status pill would be too prominent. */
@Composable
fun DotLabel(label: String, color: Color = MaterialTheme.colorScheme.tertiary) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
        Box(Modifier.size(7.dp).clip(CircleShape).background(color))
        Text(label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

/** Shared empty-state treatment; caller supplies the specific next-step guidance. */
@Composable
fun EmptyState(title: String, body: String, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier.fillMaxWidth().padding(vertical = MemoTokens.Spacing.xxl, horizontal = MemoTokens.Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface, textAlign = TextAlign.Center)
        Text(body, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
    }
}
