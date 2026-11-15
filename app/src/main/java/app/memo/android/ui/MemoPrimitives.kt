package app.memo.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.unit.dp

/**
 * Reusable page-level hierarchy. Keep eyebrow optional and keep descriptions concise.
 * The trailing slot is for one contextual action, not a row of competing buttons.
 */
@Composable
fun MemoPageHeader(
    title: String,
    modifier: Modifier = Modifier,
    eyebrow: String? = null,
    description: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)
    ) {
        if (!eyebrow.isNullOrBlank()) {
            MemoText(
                text = eyebrow,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)
        ) {
            MemoText(
                text = title,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.headlineLarge,
                color = MaterialTheme.colorScheme.onBackground
            )
            trailing?.invoke()
        }
        if (!description.isNullOrBlank()) {
            MemoText(
                text = description,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/** Section title with optional supporting text and one trailing contextual control. */
@Composable
fun MemoSectionHeading(
    title: String,
    modifier: Modifier = Modifier,
    supporting: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            MemoText(title, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onSurface)
            if (!supporting.isNullOrBlank()) {
                MemoText(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        trailing?.invoke()
    }
}

/** A restrained content surface. Use for grouped tasks, not as a wrapper around every row. */
@Composable
fun MemoPanel(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(MemoTokens.Radius.large),
        color = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        tonalElevation = MemoTokens.Elevation.flat
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(MemoTokens.Spacing.md),
            verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.sm)
        ) {
            content()
        }
    }
}

/** Semantic status treatment; state meaning is always visible as text, never color alone. */
@Composable
fun MemoStatusTag(
    label: String,
    tone: MemoStatusTone,
    modifier: Modifier = Modifier
) {
    val (container, foreground) = when (tone) {
        MemoStatusTone.NEUTRAL -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
        MemoStatusTone.SUCCESS -> MemoTokens.SemanticDark.successContainer to MemoTokens.SemanticDark.onSuccessContainer
        MemoStatusTone.WARNING -> MemoTokens.SemanticDark.warningContainer to MemoTokens.SemanticDark.onWarningContainer
        MemoStatusTone.INFO -> MemoTokens.SemanticDark.infoContainer to MemoTokens.SemanticDark.onInfoContainer
        MemoStatusTone.ERROR -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.onErrorContainer
    }
    val colors = if (MaterialTheme.colorScheme.background == MemoTokens.Dark.background) {
        when (tone) {
            MemoStatusTone.SUCCESS -> MemoTokens.SemanticDark.successContainer to MemoTokens.SemanticDark.onSuccessContainer
            MemoStatusTone.WARNING -> MemoTokens.SemanticDark.warningContainer to MemoTokens.SemanticDark.onWarningContainer
            MemoStatusTone.INFO -> MemoTokens.SemanticDark.infoContainer to MemoTokens.SemanticDark.onInfoContainer
            else -> container to foreground
        }
    } else {
        when (tone) {
            MemoStatusTone.SUCCESS -> MemoTokens.SemanticLight.successContainer to MemoTokens.SemanticLight.onSuccessContainer
            MemoStatusTone.WARNING -> MemoTokens.SemanticLight.warningContainer to MemoTokens.SemanticLight.onWarningContainer
            MemoStatusTone.INFO -> MemoTokens.SemanticLight.infoContainer to MemoTokens.SemanticLight.onInfoContainer
            else -> container to foreground
        }
    }
    val language = LocalMemoLanguage.current
    val accessibleLabel = MemoLocalization.translate(label, language)
    val accessibleStatus = MemoLocalization.translate("status", language)
    Surface(
        modifier = modifier.semantics { contentDescription = "$accessibleLabel $accessibleStatus" },
        color = colors.first,
        contentColor = colors.second,
        shape = RoundedCornerShape(MemoTokens.Radius.pill)
    ) {
        MemoText(
            text = label,
            modifier = Modifier.padding(horizontal = MemoTokens.Spacing.sm, vertical = MemoTokens.Spacing.xxs),
            style = MaterialTheme.typography.labelSmall
        )
    }
}

enum class MemoStatusTone {
    NEUTRAL, SUCCESS, WARNING, INFO, ERROR
}

/**
 * Conversation message treatment. User messages align to the end; assistant and
 * imported-source messages align to the start. Content remains selectable.
 */
@Composable
fun MemoMessageBubble(
    isUser: Boolean,
    isSource: Boolean,
    label: String,
    timestamp: String,
    content: String,
    modifier: Modifier = Modifier,
    actions: @Composable () -> Unit = {}
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (isUser) 0.88f else 0.98f),
            color = when {
                isUser -> MaterialTheme.colorScheme.primaryContainer
                isSource -> MaterialTheme.colorScheme.tertiaryContainer
                else -> MaterialTheme.colorScheme.surface
            },
            contentColor = when {
                isUser -> MaterialTheme.colorScheme.onPrimaryContainer
                isSource -> MaterialTheme.colorScheme.onTertiaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            },
            shape = RoundedCornerShape(
                topStart = MemoTokens.Radius.large,
                topEnd = MemoTokens.Radius.large,
                bottomEnd = if (isUser) MemoTokens.Radius.small else MemoTokens.Radius.large,
                bottomStart = if (isUser) MemoTokens.Radius.large else MemoTokens.Radius.small
            ),
            border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                Modifier.padding(MemoTokens.Spacing.md),
                verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.xs)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MemoText(label, style = MaterialTheme.typography.labelSmall, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold)
                    Spacer(Modifier.weight(1f))
                    MemoText(timestamp, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    actions()
                }
                SelectionContainer {
                    Text(content, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }
    }
}

