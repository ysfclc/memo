package app.memo.android.ui

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Cloud
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.FolderOpen
import androidx.compose.material.icons.rounded.Lock
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Security
import androidx.compose.material.icons.rounded.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import app.memo.android.ui.MemoText as Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.memo.android.ai.AiRuntimeViewModel
import app.memo.android.ai.CouncilPattern
import app.memo.android.ai.ModelCatalog
import app.memo.android.ai.ModelCatalogEntry
import app.memo.android.ai.REMOTE_MODEL_ID
import app.memo.android.data.AppSnapshot
import app.memo.android.data.CouncilRunRecord
import app.memo.android.data.CouncilTurnRecord
import app.memo.android.data.EvidenceRecord
import app.memo.android.ui.components.DotLabel
import app.memo.android.ui.components.EmptyState
import app.memo.android.ui.components.SectionHeading
import app.memo.android.ui.components.SurfaceCard
import java.util.Locale

private data class CouncilStarter(val label: String, val template: String)

private val councilStarters = listOf(
    CouncilStarter(
        "Research",
        "RESEARCH TASK\nInvestigate: [topic]\n\nSeparate verified facts from assumptions. Use supplied evidence by source label, identify missing evidence and competing explanations, and finish with the next best questions to investigate."
    ),
    CouncilStarter(
        "Assessment",
        "ASSESSMENT TASK\nAssess: [subject / proposal]\nCriteria: [what matters]\n\nSeparate facts, assumptions and judgments. Identify strengths, weaknesses, dependencies, failure modes and evidence gaps. Do not present an unverified claim as a fact."
    ),
    CouncilStarter(
        "Decision",
        "DECISION TASK\nDecision to make: [decision]\nOptions: [available options]\nConstraints: [constraints]\n\nCompare trade-offs, uncertainty, reversibility and consequences. Recommend a next step with reasons, but keep the final decision with me."
    ),
    CouncilStarter(
        "Build",
        "BUILD TASK\nDesired outcome: [what must exist and work]\nCurrent state: [what exists]\nConstraints: [constraints]\n\nRecover requirements before proposing implementation. Specify real behavior, architecture, acceptance tests and proof of completion. Do not stop at a visual mockup or claim anything was tested unless evidence exists."
    )
)

@Composable
fun CouncilScreen(
    snapshot: AppSnapshot,
    runtime: AiRuntimeViewModel,
    onOpenModels: () -> Unit,
    onRunSaved: (CouncilRunRecord) -> Unit,
    onDecisionSaved: (String, String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val state by runtime.uiState.collectAsState()
    var prompt by remember { mutableStateOf("") }
    var pattern by remember { mutableStateOf(CouncilPattern.SPECIALIST) }
    var selectedModels by remember { mutableStateOf(setOf<String>()) }
    var selectedEvidenceIds by remember { mutableStateOf(setOf<String>()) }
    var cloudConsent by remember { mutableStateOf(false) }
    var selectedRunId by remember { mutableStateOf<String?>(null) }
    var decisionDraft by remember { mutableStateOf("") }

    val modelChoices = buildList {
        state.installedModels.forEach { add("local:${it.id}" to (it.name + " · on device")) }
        if (state.providerConfigured) add(REMOTE_MODEL_ID to (state.providerModel + " · remote"))
    }
    LaunchedEffect(modelChoices.map { it.first }.joinToString("|")) {
        if (selectedModels.isEmpty() && modelChoices.isNotEmpty()) selectedModels = setOf(modelChoices.first().first)
        else selectedModels = selectedModels.intersect(modelChoices.map { it.first }.toSet())
    }
    val displayedRun = snapshot.councilRuns.firstOrNull { it.id == selectedRunId }
        ?: state.lastRun
        ?: snapshot.councilRuns.firstOrNull()
    LaunchedEffect(displayedRun?.id, displayedRun?.finalDecision) {
        decisionDraft = displayedRun?.finalDecision.orEmpty()
        if (selectedRunId == null && displayedRun != null) selectedRunId = displayedRun.id
    }
    val selectedEvidence = snapshot.evidence.filter { it.id in selectedEvidenceIds }

    Column(
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MemoTokens.Spacing.pageMargin),
        verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DotLabel("MEMO / COUNCIL")
            Text("Think together. Keep the evidence.", style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.onBackground)
            Text("Run a traceable workflow across models: independent analysis, handoffs, criticism, debate, or a specialist panel. The final synthesis keeps disagreement visible.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Icon(Icons.Rounded.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Council workspace", style = MaterialTheme.typography.titleLarge)
                    Text("${state.installedModels.size} local model(s) installed · ${if (state.providerConfigured) "remote endpoint configured" else "local-only until you configure a provider"}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TextButton(onClick = onOpenModels) { Text("Models") }
            }
            if (modelChoices.isEmpty()) {
                Spacer(Modifier.height(8.dp))
                EmptyState("No models available", "Install a local model or configure an HTTPS OpenAI-compatible endpoint to run your first Council.")
                Button(onClick = onOpenModels, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Install or connect a model") }
            } else {
                Spacer(Modifier.height(12.dp))
                Text("Choose participants · up to 4", style = MaterialTheme.typography.titleSmall)
                modelChoices.forEach { (id, label) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 3.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = id in selectedModels, enabled = !state.running, onCheckedChange = { checked ->
                            val previous = selectedModels
                            val next = when {
                                checked && previous.size < 4 -> previous + id
                                !checked -> previous - id
                                else -> previous
                            }
                            if ((REMOTE_MODEL_ID in previous) != (REMOTE_MODEL_ID in next)) cloudConsent = false
                            selectedModels = next
                        })
                        Column(Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.bodyMedium)
                            if (id == REMOTE_MODEL_ID) Text("Sends only with per-run permission", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                        }
                    }
                }
            }
        }

        SurfaceCard {
            Text("Your task", style = MaterialTheme.typography.titleLarge)
            Text("Start with a task frame, then edit it to fit the actual problem.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                councilStarters.forEach { starter ->
                    FilterChip(
                        selected = false,
                        enabled = !state.running,
                        onClick = { prompt = starter.template },
                        label = { Text(starter.label) }
                    )
                }
            }
            Spacer(Modifier.height(4.dp))
            OutlinedTextField(
                value = prompt,
                onValueChange = { prompt = it.take(8_000) },
                enabled = !state.running,
                modifier = Modifier.fillMaxWidth().heightIn(min = 125.dp),
                placeholder = { Text("What should the Council investigate, compare, decide, or build?") },
                supportingText = { Text("State the goal and what a useful result looks like. Answers are kept as separate, inspectable turns.") }
            )
            Spacer(Modifier.height(10.dp))
            Text("Workflow", style = MaterialTheme.typography.titleSmall)
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                CouncilPattern.entries.chunked(2).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.fillMaxWidth()) {
                        row.forEach { candidate ->
                            FilterChip(
                                selected = pattern == candidate,
                                enabled = !state.running,
                                onClick = { pattern = candidate },
                                label = { Text(candidate.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        if (row.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
            Text(CouncilPattern.valueOf(pattern.name).description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            if (snapshot.evidence.isNotEmpty()) {
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(8.dp))
                Text("Attach saved evidence · up to 6 sources", style = MaterialTheme.typography.titleSmall)
                Text("Selected source excerpts become model context. Attach only material appropriate for the selected providers; each source is retained in the run's provenance.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("${selectedEvidenceIds.size.coerceAtMost(6)} of 6 selected", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary)
                snapshot.evidence.take(30).forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth()
                            .then(if (state.running) Modifier else Modifier.clickable {
                                selectedEvidenceIds = if (item.id in selectedEvidenceIds) selectedEvidenceIds - item.id else selectedEvidenceIds + item.id
                            }).padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(checked = item.id in selectedEvidenceIds, enabled = !state.running && (item.id in selectedEvidenceIds || selectedEvidenceIds.size < 6), onCheckedChange = { checked ->
                            selectedEvidenceIds = when {
                                checked && selectedEvidenceIds.size < 6 -> selectedEvidenceIds + item.id
                                !checked -> selectedEvidenceIds - item.id
                                else -> selectedEvidenceIds
                            }
                        })
                        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(item.title, style = MaterialTheme.typography.bodyMedium, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Text(item.summary.ifBlank { item.sourceUrl.ifBlank { item.kind } }, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 2, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
            if (REMOTE_MODEL_ID in selectedModels) {
                Spacer(Modifier.height(8.dp))
                Surface(color = MaterialTheme.colorScheme.tertiaryContainer, shape = RoundedCornerShape(14.dp), modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.padding(10.dp), verticalAlignment = Alignment.Top) {
                        Checkbox(checked = cloudConsent, enabled = !state.running, onCheckedChange = { cloudConsent = it })
                        Column(Modifier.weight(1f)) {
                            Text("Allow this run to send my prompt and selected sources to the configured cloud endpoint", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                            Text("This permission applies to this run only. Without it, cloud transmission is blocked. Local-only selections never use the remote endpoint.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
            Spacer(Modifier.height(12.dp))
            Button(
                onClick = {
                    runtime.runCouncil(prompt, pattern, selectedModels.toList(), selectedEvidence, cloudConsent) { run, error ->
                        if (error != null) onShowMessage(error)
                        if (run != null) {
                            onRunSaved(run)
                            selectedRunId = run.id
                            decisionDraft = run.finalDecision
                            cloudConsent = false
                            onShowMessage(if (run.synthesis.startsWith("Council run cancelled")) "Partial Council trace saved for review." else "Council run saved with ${run.turns.count { it.status == "COMPLETED" }} completed turn(s).")
                        }
                    }
                },
                enabled = !state.running && modelChoices.isNotEmpty() && prompt.isNotBlank() && selectedModels.isNotEmpty() && (REMOTE_MODEL_ID !in selectedModels || cloudConsent),
                modifier = Modifier.fillMaxWidth().height(50.dp)
            ) {
                if (state.running) CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                else Icon(Icons.Rounded.PlayArrow, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(if (state.running) state.stage else "Run Council")
            }
            if (state.running) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                Text(state.stage, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedButton(onClick = { runtime.cancelRun() }, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(8.dp)); Text("Cancel run") }
            }
            state.lastError?.let { err ->
                Text(err, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                TextButton(onClick = { runtime.clearError() }) { Text("Dismiss") }
            }
        }

        val visibleTurns = if (state.running) state.activeTurns else displayedRun?.turns.orEmpty()
        if (visibleTurns.isNotEmpty()) {
            SectionHeading(if (state.running) "Live Council trace" else "Council trace", "Each contribution is preserved under its model and role")
            visibleTurns.forEachIndexed { index, turn -> TurnCard(index + 1, turn) }
        }

        if (!state.running && displayedRun != null) {
            SurfaceCard {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Synthesis · ${CouncilPattern.entries.firstOrNull { it.name == displayedRun.pattern }?.label ?: displayedRun.pattern}", style = MaterialTheme.typography.titleLarge)
                        Text("${formatDuration(displayedRun.durationMs)} · ${formatDate(displayedRun.createdAt)} · ${displayedRun.turns.size} retained turn(s)", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.height(10.dp))
                SelectionContainer { Text(displayedRun.synthesis, style = MaterialTheme.typography.bodyMedium) }
                Spacer(Modifier.height(10.dp))
                Text("Your decision", style = MaterialTheme.typography.titleMedium)
                Text("The Council does not make the decision for you. Record what you accept, reject, or want to verify next.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                OutlinedTextField(
                    value = decisionDraft,
                    onValueChange = { decisionDraft = it.take(4_000) },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 90.dp),
                    placeholder = { Text("Decision, accepted conclusion, rejected claim, or next action…") }
                )
                Button(onClick = { onDecisionSaved(displayedRun.id, decisionDraft); onShowMessage("Decision saved to this device.") }, modifier = Modifier.fillMaxWidth()) { Text("Save my decision") }
                if (displayedRun.sources.isNotEmpty()) {
                    Text("Sources carried into this run", style = MaterialTheme.typography.titleSmall)
                    displayedRun.sources.forEach { source ->
                        Text("• ${source.title}${source.sourceUrl.takeIf { it.isNotBlank() }?.let { " · $it" }.orEmpty()}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Text("Cloud permission: ${if (displayedRun.cloudPermissionGranted) "granted for this run" else "not granted / local-only"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }

        if (snapshot.councilRuns.isNotEmpty()) {
            SectionHeading("Recent runs", "Open a prior trace and its recorded decision")
            snapshot.councilRuns.take(12).forEach { run ->
                SurfaceCard(modifier = Modifier.fillMaxWidth().clickable { selectedRunId = run.id }) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(run.prompt, style = MaterialTheme.typography.titleSmall, maxLines = 2, overflow = TextOverflow.Ellipsis)
                            Text("${CouncilPattern.entries.firstOrNull { it.name == run.pattern }?.label ?: run.pattern} · ${formatDate(run.createdAt)} · ${run.turns.size} turns", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        RadioButton(selected = displayedRun?.id == run.id, onClick = { selectedRunId = run.id })
                    }
                    if (run.finalDecision.isNotBlank()) Text("Decision: ${run.finalDecision}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.tertiary, maxLines = 2, overflow = TextOverflow.Ellipsis)
                }
            }
        } else if (!state.running && state.lastRun == null) {
            EmptyState("No Council history yet", "Your first finished run will leave a trace of model contributions, synthesis and your recorded decision on this device.")
        }
    }
}

@Composable
private fun TurnCard(index: Int, turn: CouncilTurnRecord) {
    var showInput by remember(turn.id) { mutableStateOf(false) }
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top) {
            Surface(color = if (turn.status == "FAILED") MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(10.dp), modifier = Modifier.size(34.dp)) {
                Box(contentAlignment = Alignment.Center) { Text(index.toString(), style = MaterialTheme.typography.titleSmall, color = if (turn.status == "FAILED") MaterialTheme.colorScheme.onErrorContainer else MaterialTheme.colorScheme.onPrimaryContainer) }
            }
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                Text(turn.role, style = MaterialTheme.typography.titleMedium)
                Text("${turn.modelName} · ${turn.status.lowercase().replaceFirstChar { it.titlecase() }} · ${formatDuration(turn.durationMs)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (turn.requestSummary.isNotBlank()) {
            TextButton(onClick = { showInput = !showInput }, contentPadding = PaddingValues(horizontal = 0.dp, vertical = 2.dp)) {
                Text(if (showInput) "Hide input given to this model" else "Inspect input given to this model")
            }
            if (showInput) {
                Surface(color = MaterialTheme.colorScheme.surfaceVariant, shape = RoundedCornerShape(12.dp), modifier = Modifier.fillMaxWidth()) {
                    SelectionContainer {
                        Text(turn.requestSummary, modifier = Modifier.padding(10.dp), style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }
        if (turn.status == "FAILED") Text(turn.error.ifBlank { "The model did not complete this turn." }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        else SelectionContainer { Text(turn.response, style = MaterialTheme.typography.bodyMedium) }
    }
}

@Composable
fun ModelsScreen(runtime: AiRuntimeViewModel, onShowMessage: (String) -> Unit) {
    val context = LocalContext.current
    val state by runtime.uiState.collectAsState()
    var endpoint by remember { mutableStateOf("") }
    var model by remember { mutableStateOf("") }
    var apiKey by remember { mutableStateOf("") }
    LaunchedEffect(state.providerEndpoint, state.providerModel) {
        endpoint = state.providerEndpoint
        model = state.providerModel
    }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri != null) runtime.importModel(uri, context.contentResolver) { error -> onShowMessage(error ?: "Local model imported.") }
    }

    Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(MemoTokens.Spacing.pageMargin), verticalArrangement = Arrangement.spacedBy(MemoTokens.Spacing.md)) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            DotLabel("MEMO / MODEL RUNTIME")
            Text("Choose the intelligence. Keep control.", style = MaterialTheme.typography.headlineLarge)
            Text("Model runtimes are replaceable. Local weights remain on this phone; cloud access is configured separately and requires a per-run permission.", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Lock, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Local models", style = MaterialTheme.typography.titleLarge)
                    Text("Weights download only when you tap Install. They are never included inside the APK.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                IconButton(onClick = runtime::refreshModels) { Icon(Icons.Rounded.Refresh, contentDescription = "Refresh installed models") }
            }
            state.installedModels.filterNot { it.isCatalogModel }.forEach { installed ->
                Divider(color = MaterialTheme.colorScheme.outlineVariant)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 8.dp)) {
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(installed.name, style = MaterialTheme.typography.titleSmall)
                        Text("Installed · ${formatBytes(installed.bytes)} · on-device", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
                    }
                    IconButton(onClick = { runtime.deleteModel(installed.id); onShowMessage("Removing model…") }) { Icon(Icons.Rounded.Delete, contentDescription = "Delete ${installed.name}", tint = MaterialTheme.colorScheme.error) }
                }
            }
            ModelCatalog.entries.forEach { entry ->
                val isInstalled = state.installedModels.any { it.id == entry.id }
                val download = state.downloads[entry.id]
                ModelCatalogCard(
                    entry = entry,
                    installed = isInstalled,
                    download = download,
                    onInstall = { runtime.installModel(entry.id) },
                    onCancel = { runtime.cancelModelDownload(entry.id) },
                    onDelete = { runtime.deleteModel(entry.id) }
                )
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/octet-stream", "application/x-binary", "*/*")) }, modifier = Modifier.fillMaxWidth()) {
                Icon(Icons.Rounded.FolderOpen, contentDescription = null); Spacer(Modifier.width(8.dp)); Text("Import .litertlm model file")
            }
            Text("Compatibility note: the built-in models use Google LiteRT-LM. A random GGUF file or an Ollama model folder cannot be loaded directly in this runtime; import a compatible .litertlm artifact.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }

        SurfaceCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Rounded.Cloud, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(25.dp))
                Spacer(Modifier.width(8.dp))
                Column(Modifier.weight(1f)) {
                    Text("Remote model endpoint", style = MaterialTheme.typography.titleLarge)
                    Text("Optional · OpenAI-compatible chat completions over HTTPS", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            OutlinedTextField(value = endpoint, onValueChange = { endpoint = it.take(500) }, modifier = Modifier.fillMaxWidth(), label = { Text("HTTPS base URL") }, placeholder = { Text("https://api.example.com/v1") }, singleLine = true)
            OutlinedTextField(value = model, onValueChange = { model = it.take(200) }, modifier = Modifier.fillMaxWidth(), label = { Text("Model identifier") }, placeholder = { Text("The exact model name accepted by your provider") }, singleLine = true)
            OutlinedTextField(
                value = apiKey,
                onValueChange = { apiKey = it.take(4_000) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text(if (state.providerHasKey && apiKey.isBlank()) "API key · saved securely" else "API key (optional if provider needs none)") },
                placeholder = { Text(if (state.providerHasKey) "Leave blank to keep the saved key" else "Paste provider key") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
            )
            Text("API keys are encrypted using Android Keystore and stored outside workspace backups. Memo does not validate billing or provider quotas. Remote requests are disabled for each run unless you explicitly permit data upload on the Council screen.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                Button(onClick = {
                    runtime.saveProvider(endpoint, model, apiKey) { error ->
                        if (error == null) { apiKey = ""; onShowMessage("Remote provider settings encrypted and saved.") }
                        else onShowMessage(error)
                    }
                }, modifier = Modifier.weight(1f)) { Text("Save endpoint") }
                if (state.providerConfigured) {
                    OutlinedButton(onClick = {
                        runtime.saveProvider(endpoint, model, "", removeKey = true) { error -> onShowMessage(error ?: "Saved remote API key removed.") }
                    }) { Text("Remove key") }
                }
            }
            if (state.providerConfigured) Text("Configured · ${state.providerEndpoint} · model ${state.providerModel} · ${if (state.providerHasKey) "credential stored" else "no credential stored"}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.tertiary)
        }
        state.lastError?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error) }
        Text("Memo 2.0 · local-first model runtime", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ModelCatalogCard(
    entry: ModelCatalogEntry,
    installed: Boolean,
    download: app.memo.android.ai.DownloadStatus?,
    onInstall: () -> Unit,
    onCancel: () -> Unit,
    onDelete: () -> Unit
) {
    SurfaceCard {
        Row(verticalAlignment = Alignment.Top) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(entry.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text("Download size about ${entry.expectedBytes / 1_000_000} MB", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                Text(entry.ramNote, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (download != null) {
            Spacer(Modifier.height(8.dp))
            val progress = download.progress
            if (progress != null) LinearProgressIndicator(progress = progress, modifier = Modifier.fillMaxWidth()) else LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            Text("${download.stage} · ${formatBytes(download.bytesRead)} / ${formatBytes(download.totalBytes)}", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedButton(onClick = onCancel, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.StopCircle, null); Spacer(Modifier.width(6.dp)); Text("Cancel download") }
        } else if (installed) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("Ready for Council", modifier = Modifier.weight(1f), color = MaterialTheme.colorScheme.tertiary, style = MaterialTheme.typography.labelMedium)
                TextButton(onClick = onDelete) { Text("Remove") }
            }
        } else {
            Button(onClick = onInstall, modifier = Modifier.fillMaxWidth()) { Icon(Icons.Rounded.Download, null); Spacer(Modifier.width(8.dp)); Text("Download and install") }
        }
    }
}

private fun formatBytes(bytes: Long): String {
    if (bytes <= 0L) return "0 MB"
    val mb = bytes / 1_000_000.0
    return if (mb >= 1_000) String.format(Locale.getDefault(), "%.1f GB", mb / 1_000) else String.format(Locale.getDefault(), "%.0f MB", mb)
}

private fun formatDuration(durationMs: Long): String = when {
    durationMs < 1_000 -> "${durationMs.coerceAtLeast(0)} ms"
    durationMs < 60_000 -> String.format(Locale.getDefault(), "%.1f s", durationMs / 1_000.0)
    else -> "${durationMs / 60_000}m ${(durationMs / 1_000) % 60}s"
}

private fun formatDate(timestamp: Long): String = java.text.SimpleDateFormat("MMM d · HH:mm", Locale.getDefault()).format(java.util.Date(timestamp))
