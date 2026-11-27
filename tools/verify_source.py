#!/usr/bin/env python3
"""Offline structural checks for the Memo Android source package.

This script is intentionally not presented as an Android build/test. CI must still
run Gradle unit tests, lint, and APK assembly, then exercise a real device.
"""
from __future__ import annotations

import re
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "settings.gradle",
    "build.gradle",
    "core/build.gradle",
    "core/src/main/kotlin/app/memo/android/data/Models.kt",
    "core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt",
    "core/src/main/kotlin/app/memo/android/data/OutcomeParser.kt",
    "core/src/main/kotlin/app/memo/android/runtime/RuntimeContracts.kt",
    "core/src/test/kotlin/app/memo/android/data/OutcomeParserTest.kt",
    "core/src/test/kotlin/app/memo/android/data/ActionApprovalLedgerTest.kt",
    "app/build.gradle",
    "app/src/main/AndroidManifest.xml",
    "app/src/main/java/app/memo/android/MainActivity.kt",
    "app/src/main/java/app/memo/android/ui/MemoLocalization.kt",
    "app/src/main/java/app/memo/android/notifications/MemoNotificationCenter.kt",
    "app/src/main/res/drawable/ic_notification.xml",
    "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt",
    "app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt",
    "app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt",
    "app/src/main/java/app/memo/android/ui/MemoTheme.kt",
    "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt",
    "app/src/main/java/app/memo/android/data/EncryptedVault.kt",
    "app/src/main/java/com/synthos/runtime/data/SynthStore.java",
    "app/src/androidTest/java/app/memo/android/data/EncryptedStoreUpgradeTest.kt",
    "app/src/main/java/app/memo/android/runtime/MemoRuntimeBridge.java",
    "app/src/main/java/com/synthos/runtime/engine/AutomationAccessibilityService.java",
    "remote-runner/server.py",
    "remote-runner/tests/test_runner.py",
    ".github/workflows/build-apk.yml",
    "FEATURE_AUDIT.md",
    "PRODUCT_GAP_REGISTRY.md",
    "DEVICE_ACCEPTANCE_CHECKLIST.md",
    "CHANGELOG.md",
    "README.md",
    "TASKS.md",
    "BUILD_STATUS.md",
    "app/src/test/java/app/memo/android/ui/MemoLocalizationTest.kt",
]

def fail(message: str) -> None:
    print(f"FAIL: {message}", file=sys.stderr)
    raise SystemExit(1)

for rel in REQUIRED:
    if not (ROOT / rel).is_file():
        fail(f"required file is missing: {rel}")
print(f"PASS: {len(REQUIRED)} required integration files exist")
core_gradle = (ROOT / "core/build.gradle").read_text()
app_gradle = (ROOT / "app/build.gradle").read_text()
if "org.jetbrains.kotlin.jvm" not in core_gradle or "implementation project(':core')" not in app_gradle:
    fail("the Android app is not wired to the platform-independent :core module")
for path in (ROOT / "core/src/main/kotlin").rglob("*.kt"):
    if re.search(r"^\s*import\s+android(?:x)?\.", path.read_text(), re.M):
        fail(f"platform-independent :core contains Android import: {path.relative_to(ROOT)}")
print("PASS: platform-independent domain module is isolated from Android framework imports")

approval_ledger = (ROOT / "core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt").read_text()
approval_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/ActionApprovalLedgerTest.kt").read_text()
browser_session = (ROOT / "app/src/main/java/app/memo/android/browser/BrowserSession.kt").read_text()
workflow_screen = (ROOT / "app/src/main/java/app/memo/android/ui/BrowserWorkflowScreen.kt").read_text()
for marker in ("contentHash", "fun approve(", "fun beginExecution(", "fun completeVerified(", "verifyAuditChain", "recoverAfterRestart"):
    if marker not in approval_ledger:
        fail(f"exact-content action ledger missing required contract: {marker}")
for marker in ("changedPayloadOrDestinationCannotReuseApproval", "tamperedStoredPayloadAndTamperedAuditEventFailClosed", "schemaFourRoundTripsApprovalAndRejectsTamperedPayload"):
    if marker not in approval_test:
        fail(f"action ledger regression test missing: {marker}")
if "approveCurrentPrompt" not in browser_session or "awaitingPromptApproval" not in browser_session:
    fail("visible browser session lacks the exact-content approval state gate")
for marker in ("Approve exact external prompt?", "workflowState.currentPrompt", "Approve and send", "Reject and stop", "completeExternalAction"):
    if marker not in workflow_screen:
        fail(f"browser workflow UI is missing exact-content approval behavior: {marker}")
print("PASS: exact-content action ledger, regression tests, visible prompt gate and evidence completion are wired in source")
approval_inbox = (ROOT / "app/src/main/java/app/memo/android/ui/ActionApprovalInboxScreen.kt").read_text()
app_navigation = (ROOT / "app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt").read_text()
for marker in ("Action queue", "Needs review", "History", "Exact outbound payload", "Open live approval", "Mark outcome for review"):
    if marker not in approval_inbox:
        fail(f"approval inbox is missing required behavior: {marker}")
if "ActionApprovalInboxScreen(" not in app_navigation or "browserState = browser.workflowState" not in app_navigation:
    fail("central approval inbox is not integrated with the live browser session")
if "This proposal is not connected to the matching live prompt" not in approval_inbox:
    fail("approval inbox does not guard against approving a stale proposal from history")
if 'if (accepted && matchesLivePrompt) browser.rejectCurrentPrompt()' not in app_navigation:
    fail("rejecting a pending live proposal does not stop the awaiting browser workflow")
ledger = (ROOT / "core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt").read_text()
for marker in ("URI(destination)", "uri.rawUserInfo != null", "uri.rawQuery != null", "it.actionId !in byId"):
    if marker not in ledger:
        fail(f"action ledger is missing an envelope or audit integrity guard: {marker}")
print("PASS: central approval inbox is integrated; stale proposals cannot be approved from history; live rejection stops the awaiting workflow")
for marker in ("fun stopActiveWork()", "browser.stopBrowserWorkflow()", "browser.cancelPendingTask()", "aiRuntime.cancelRun()", "AutomationAccessibilityService.requestCancel()", "viewModel.markExternalActionNeedsReview(", "client.cancel(jobId)", "Stop active work"):
    if marker not in app_navigation:
        fail(f"product-wide stop control is missing cancellation/review wiring: {marker}")
if "could not be confirmed" not in app_navigation or "Verify its final state" not in app_navigation:
    fail("guest runner cancellation must remain explicitly best-effort and evidence-sensitive")
print("PASS: top-level stop requests local AI/browser/native cancellation, marks uncertain external actions for review, and reports remote cancellation honestly")

agent_broker = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
talk_screen = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in (
    "fun launchAgentExecution(",
    "suspend fun awaitAgentApproval(",
    "suspend fun approveAgentActionDurably(",
    "suspend fun rejectAgentActionDurably(",
    "settleAgentApprovals(conversationId)",
    "CompletableDeferred<AgentApprovalGrant?>",
):
    if marker not in agent_broker:
        fail(f"durable Talk approval broker is missing: {marker}")
for marker in (
    "viewModel.launchAgentExecution(selectedConversation.id, userText, selectedModelId)",
    "onApprovalRequired = { request ->",
    "viewModel.awaitAgentApproval(selectedConversation.id, request)",
    "viewModel.updateAgentExecutionStage(event.summary)",
    "viewModel.stopAgentExecution()",
):
    if marker not in talk_screen:
        fail(f"Talk execution lifecycle is not ViewModel-owned or brokered: {marker}")
if "agentScope.launch" in talk_screen or "var agentJob" in talk_screen:
    fail("Talk execution is still owned by the disposable composable destination scope")
for marker in ("Approve exact action", "onApproveAgentAction", "onRejectAgentAction", "agent-run:"):
    if marker not in approval_inbox:
        fail(f"approval inbox is missing the Talk exact-action decision flow: {marker}")
if "val accepted = viewModel.approveAgentActionDurably(id)" not in app_navigation:
    fail("the root approval inbox does not call the durable Talk approval API")
if "val accepted = viewModel.rejectAgentActionDurably(id, reason)" not in app_navigation:
    fail("the root approval inbox does not call the durable Talk rejection API")
if "viewModel.viewModelScope.launch" not in app_navigation:
    fail("approval decisions are not owned by the ViewModel lifecycle")
if "actionScope" in approval_inbox or "rememberCoroutineScope" in approval_inbox:
    fail("approval decisions are owned by the disposable inbox destination")
for marker in (
    "agentApprovalReviewed",
    "enabled = agentApprovalReviewed",
    "LaunchedEffect(expanded, action.id, action.contentHash)",
    "Text(action.destination, style = MaterialTheme.typography.bodySmall)",
):
    if marker not in approval_inbox:
        fail(f"Talk approval review gate is missing: {marker}")
if "viewModel.stopAgentExecution()" not in app_navigation:
    fail("the product-wide stop action does not cancel the ViewModel-owned Talk execution")
print("PASS: Talk execution survives destination changes, waits on the encrypted exact-content approval broker, and exposes approve/reject controls")

agent_models = (ROOT / "core/src/main/kotlin/app/memo/android/data/Models.kt").read_text()
agent_engine = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/AgentExecutionEngine.kt").read_text()
agent_run_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/AgentRunLifecycleTest.kt").read_text()
workspace_schema_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/WorkspaceSchemaV3Test.kt").read_text()
live_screen = (ROOT / "app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt").read_text()
for marker in (
    "data class AgentRunRecord(",
    "val agentRuns: List<AgentRunRecord>",
    "fun AgentRunRecord.applyEvent(",
    "val interruptedAgentRuns = agentRuns.count { it.isActive }",
    'root.put("agentRuns"',
    'root.put("schemaVersion", 7)',
    "require(schema in 1..7)",
):
    if marker not in agent_models:
        fail(f"encrypted durable run model/recovery is missing: {marker}")
for marker in ("suspend fun executeWithRunId(", "executionRunId = UUID.randomUUID().toString()"):
    if marker not in agent_engine:
        fail(f"agent engine cannot share its persisted run identity: {marker}")
for marker in (
    "createAgentRunDurably(",
    "appendAgentEventDurably(",
    "finishAgentRunDurably(",
    "closeUnfinishedAgentRunDurably(",
    "persistAgentRunAndVerify(",
):
    if marker not in agent_broker:
        fail(f"Talk durable run journal is missing: {marker}")
for marker in (
    "viewModel.launchAgentExecution(selectedConversation.id, userText, selectedModelId)",
    "engine.executeWithRunId(",
    "executionRunId = runId",
    "viewModel.appendAgentEventDurably(",
    "viewModel.finishAgentRunDurably(",
):
    if marker not in talk_screen:
        fail(f"Talk execution is not bound to its durable run journal: {marker}")
for marker in ("Memo executions", "AgentRunSummary(", "run.recoveryReason", "Open conversation", "FINALIZING", "Saving result"):
    if marker not in live_screen:
        fail(f"Live does not expose persisted Memo run state: {marker}")
for marker in ("eventsRecordApprovalToolAndTerminalLifecycle", "rejectedOrUncertainToolOutcomesRequireReview", "eventsFromAnotherRunAreRejected"):
    if marker not in agent_run_test:
        fail(f"agent run lifecycle regression coverage is missing: {marker}")
for marker in ("agentRunRecordsRoundTripWithExactRunAndConversationIdentity", "schemaSixBackupMigratesWithoutAgentRunRecords", "interruptedAgentRunsRecoverToReviewAndTerminalRunsStayTerminal"):
    if marker not in workspace_schema_test:
        fail(f"schema-v7 run persistence/recovery test is missing: {marker}")
print("PASS: Talk execution has a schema-versioned encrypted run record, atomic event journal/readback, truthful Live state and no-replay recovery")

if "import app.memo.android.orchestration.AgentExecutionEventType" not in agent_broker:
    fail("the ViewModel run closeout must import the engine event types it compares")
if "fun launchAgentExecution(conversationId: String, block: suspend () -> Unit)" in agent_broker:
    fail("the obsolete non-durable Talk launcher must not remain")
if "run.status != \"FINALIZING\"" not in agent_broker:
    fail("final response persistence must require a durable FINALIZING state")
if '"FINALIZING"' not in agent_models or "AgentExecutionEventType.RUN_FINISHED" not in agent_models:
    fail("the durable run reducer must preserve finalization as a recoverable nonterminal state")

xml_files = sorted((ROOT / "app/src/main").rglob("*.xml"))
for file in xml_files:
    try:
        ET.parse(file)
    except ET.ParseError as exc:
        fail(f"invalid XML in {file.relative_to(ROOT)}: {exc}")
print(f"PASS: {len(xml_files)} Android XML files parse")

manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
for permission in (
    "android.permission.RECORD_AUDIO",
    "android.permission.POST_NOTIFICATIONS",
    "android.permission.RECEIVE_BOOT_COMPLETED",
):
    if permission not in manifest:
        fail(f"manifest permission not declared: {permission}")
for receiver in (
    "app.memo.android.notifications.MemoDailyNudgeReceiver",
    "app.memo.android.notifications.MemoReminderBootReceiver",
):
    if receiver not in manifest:
        fail(f"notification receiver missing from manifest: {receiver}")
print("PASS: microphone/notification/boot permissions and notification receivers are declared")

android_ns = "{http://schemas.android.com/apk/res/android}"
root_manifest = ET.fromstring(manifest)
launcher_activities = []
for activity in root_manifest.findall("application/activity"):
    name = activity.get(android_ns + "name", "")
    actions = [item.get(android_ns + "name", "") for item in activity.findall("intent-filter/action")]
    categories = [item.get(android_ns + "name", "") for item in activity.findall("intent-filter/category")]
    if "android.intent.action.MAIN" in actions and "android.intent.category.LAUNCHER" in categories:
        launcher_activities.append(name)
if launcher_activities != [".MainActivity"]:
    fail(f"expected one Memo launcher Activity, found: {launcher_activities}")
print("PASS: exactly one Memo launcher Activity is declared")
notifications = (ROOT / "app/src/main/java/app/memo/android/notifications/MemoNotificationCenter.kt").read_text()
for marker in ("setAndAllowWhileIdle", "cancelNudge", "VISIBILITY_PRIVATE", "getBoolean(WORKFLOW_NOTIFICATIONS_ENABLED, false)", "R.drawable.ic_notification"):
    if marker not in notifications:
        fail(f"notification center missing safety/behavior marker: {marker}")
print("PASS: local reminder scheduling/cancellation, monochrome notification icon, private lock-screen visibility, and opt-in workflow alerts are present")

localization = (ROOT / "app/src/main/java/app/memo/android/ui/MemoLocalization.kt").read_text()
for marker in ('"EN" to "English"', '"PL" to "Polski"', '"TR" to "Türkçe"', '"Notifications & reminders"', '"Voice input"'):
    if marker not in localization:
        fail(f"language catalog missing marker: {marker}")
localization_test = ROOT / "app/src/test/java/app/memo/android/ui/MemoLocalizationTest.kt"
if not localization_test.exists():
    fail("MemoLocalization JVM unit test file missing")
print("PASS: English, Polish, Turkish and voice/notification translations are represented")

# Memo product-specific regressions: one Talk entry point, safe intent routing, and no second legacy Activity UI.
legacy_activity = ROOT / "app/src/main/java/com/synthos/runtime/MainActivity.java"
if legacy_activity.exists():
    fail("legacy duplicate SynthOS Activity still exists; runtime UI must route through MemoRuntimeBridge")
bridge = ROOT / "app/src/main/java/app/memo/android/runtime/MemoRuntimeBridge.java"
if "interface Listener" not in bridge.read_text() or "openForExternalCapture" not in bridge.read_text():
    fail("Memo runtime bridge is missing listeners or capture entry points")
home = (ROOT / "app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt").read_text()
chat = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in ("onContinueWithMemo", "RecognizerIntent.ACTION_RECOGNIZE_SPEECH", "speechResultLauncher", "Manifest.permission.RECORD_AUDIO", "Make sense of what matters.", "Needs your decision"):
    if marker not in home:
        fail(f"Memo-first home is missing product marker: {marker}")
if "IntentPlanner.shouldCreateWorkflow" in home:
    fail("new user intents must enter the canonical Talk execution path, not a keyword-routed template planner")
for marker in ("pendingChatPrompt = clean", "activeConversationId = id", "tab = AppTab.HOME"):
    if marker not in home:
        fail(f"home-to-Talk intent handoff is missing marker: {marker}")
for marker in ("initialPrompt", "onInitialPromptConsumed", "draft = initialPrompt.take(6_000)"):
    if marker not in chat:
        fail(f"home-to-chat draft handoff is missing marker: {marker}")
if "MainActivity.notifyRuntimeChanged" in (ROOT / "app/src/main/java/com/synthos/runtime/engine/AutomationAccessibilityService.java").read_text():
    fail("accessibility runtime still targets legacy Activity helpers")
if "var tab by remember { mutableStateOf(AppTab.HOME) }" not in home:
    fail("Memo must open on the universal Talk intent entry point")
for marker in (
    'Triple(AppTab.HOME, Icons.Rounded.ChatBubbleOutline, "Talk")',
    'Triple(AppTab.LIVE, Icons.Rounded.Bolt, "Live")',
    'Triple(AppTab.WORK, Icons.Rounded.Hub, "Work")',
    'Triple(AppTab.STUDIO, Icons.Rounded.Article, "Studio")',
    "AppTab.LIVE -> LiveWorkspaceScreen(",
    "AppTab.STUDIO -> SelfModelStudioScreen(",
):
    if marker not in home:
        fail(f"Memo four-surface shell is missing: {marker}")
if 'Triple(AppTab.BROWSER' in home:
    fail("Browser must remain a runtime route, not a primary navigation destination")
view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
for marker in ('sourceConversationId: String = ""', 'sourceConversationId = sourceId'):
    if marker not in view_model:
        fail(f"Work-plan source conversation linkage is missing: {marker}")
for marker in ("Start with Memo", "AgentExecutionEngine(", "onOpenCouncil", "onStartConversation"):
    if marker not in chat:
        fail(f"Talk entry/composer is missing: {marker}")
print("PASS: one Memo Activity, universal Talk intent router, linked source conversations, four product surfaces, and safe draft handoff")

# Estimate coverage of the static string literals passed to the custom UI Text wrapper.
# URLs and empty placeholders are intentionally not translations. Dynamic/interpolated
# and string-valued labels are outside this conservative static scan.
ui_dir = ROOT / "app/src/main/java/app/memo/android/ui"
localization_sources = (ui_dir / "MemoLocalization.kt").read_text()
translation_keys = set(re.findall(r'^\s*"((?:[^"\\]|\\.)*)"\s+to\s+\(', localization_sources, re.M))
scanned = []
for ui_file in ("MemoWorkspaceApp.kt", "WorkspaceScreens.kt", "LiveWorkspaceScreen.kt", "AppsWorkspaceScreen.kt", "BrowserWorkflowScreen.kt", "AiScreens.kt", "ActionApprovalInboxScreen.kt"):
    source = (ui_dir / ui_file).read_text()
    scanned.extend(re.findall(r'\bText\(\s*"((?:[^"\\]|\\.)*)"', source))
translatable = [value for value in scanned if len(value.strip()) > 1 and "$" not in value and "\\n" not in value and not value.startswith(("https://", "http://"))]
missing = sorted(set(value for value in translatable if value not in translation_keys))
if missing:
    fail("static UI strings are missing Polish/Turkish catalog entries: " + " | ".join(missing[:30]))
print(f"PASS: {len(translatable)}/{len(translatable)} static UI Text literals requiring translation are present in the catalog ({len(scanned)} literals scanned; URLs/placeholders excluded)")

workspace_app = (ui_dir / "MemoWorkspaceApp.kt").read_text()
for marker in (
    "LaunchedEffect(sharedText, sharedSubject, vaultReady, vaultError)",
    "vaultError == null && shared.isNotBlank()",
    "onImportEncrypted = { encryptedImportLauncher.launch",
    'Text("Restore encrypted backup")',
):
    if marker not in workspace_app:
        fail(f"vault recovery UI is missing a safe recovery gate: {marker}")
if "Restore encrypted backup" not in localization_sources:
    fail("encrypted backup recovery action is missing from the English/Polish/Turkish catalog")
if "plain JSON or passphrase-encrypted" not in workspace_app:
    fail("vault recovery UI must explain both supported backup formats")
print("PASS: vault failure blocks share capture and keeps encrypted-backup recovery available")


source_manifest = ROOT / "SOURCE_SHA256SUMS.txt"
if not source_manifest.is_file():
    fail("source SHA-256 manifest is missing")

workflow = (ROOT / ".github/workflows/build-apk.yml").read_text()
if "      - 'BUILD_STATUS.md'" in workflow or "BUILD_STATUS[.]md$" in workflow:
    fail("BUILD_STATUS is generated by CI and must not trigger another CI run")
if "incoming/memo.zip" not in workflow:
    fail("GitHub workflow does not use the canonical incoming/memo.zip source upload")
if "Memo-Android-Repository.zip" in workflow:
    fail("GitHub workflow must not depend on a legacy root-level source ZIP")
for task in (":core:test", ":app:testDebugUnitTest", ":app:lintDebug", ":app:lintRelease", ":app:assembleDebug", ":app:assembleRelease", ":app:assembleReleaseAndroidTest"):
    if task not in workflow:
        fail(f"GitHub workflow missing Android gate: {task}")
if any(alternate in workflow for alternate in ("Memo-Android-source.zip", "Memo-Android-Download-Fresh.zip")):
    fail("GitHub workflow must not fall back to an alternate/stale ZIP")
if "testBuildType 'release'" not in app_gradle:
    fail("encrypted-store install-upgrade instrumentation must target the normal release package")
for marker in ("app/build/outputs/apk/release/app-release.apk", "app/build/outputs/apk/androidTest/release/app-release-androidTest.apk", "Memo-Android-release.apk", "app.memo.android"):
    if marker not in workflow:
        fail(f"normal release package build/signing/delivery contract is missing: {marker}")
if "Expected exactly one Android Gradle project root" not in workflow or "Memo-Android-APKs" not in workflow:
    fail("GitHub workflow is missing canonical-root or artifact-delivery checks")
if "sha256sum -c SOURCE_SHA256SUMS.txt" not in workflow or "Memo-Source-Archive-SHA256.txt" not in workflow:
    fail("GitHub workflow is missing source-integrity verification/receipt")

# All build paths must derive the same safe APK version from the checked-out source commit.
gradle_build = (ROOT / "app/build.gradle").read_text()
circleci_build = (ROOT / "tools/ci/circleci-android.sh").read_text()
if "ProcessBuilder('git', 'log', '-2', '--format=%ct', 'HEAD')" not in gradle_build or 'Math.floorDiv(commitTimes[0].toLong(), 4L)' not in gradle_build:
    fail("Gradle APK versionCode must derive from the checked-out source commit")
if "refusing an unsafe fallback version" not in gradle_build:
    fail("Gradle must fail closed when no safe source-derived APK version is available")
if "GITHUB_RUN_NUMBER" in gradle_build or "CIRCLE_BUILD_NUM" in circleci_build:
    fail("APK versionCode must not depend on runner-specific counters")
if 'git log -2 --format=%ct HEAD' not in circleci_build or 'CURRENT_VERSION_CODE > PARENT_VERSION_CODE' not in circleci_build or 'VERSION_TIMES[0] / 4' not in circleci_build:
    fail("CircleCI must reject non-monotonic source timestamps before building APKs")
if "GITHUB_RUN_NUMBER" in workflow or 'CURRENT_CODE="$(( $(git show -s --format=%ct HEAD) / 4 ))"' not in workflow:
    fail("GitHub APK identity/upgrade checks must use the source-derived APK version")
if "commit_times = subprocess.check_output(['git', 'log', '-2', '--format=%ct', 'HEAD']" not in workflow or 'current_code = int(commit_times[0]) // 4' not in workflow:
    fail("GitHub APK version validation must match the source commit timestamp")

workspace_vault = (ROOT / "app/src/main/java/app/memo/android/data/EncryptedVault.kt").read_text()
native_store = (ROOT / "app/src/main/java/com/synthos/runtime/data/SynthStore.java").read_text()
upgrade_test = (ROOT / "app/src/androidTest/java/app/memo/android/data/EncryptedStoreUpgradeTest.kt").read_text()
for marker in ("memo-workspace-vault.bin", "synthos-vault.bin", "synthos_fieldwork_vault_v1", "Memo-Android:v1", "getExistingKey()", "copyLegacyWorkspaceAtomically", 'canonicalBase.path + ".bak"', "atomicFile.openRead()", 'keyStore.getKey("synthos.runtime.store.v1", null)', "cipher.doFinal(encrypted)", "damaged SYV1 header"):
    if marker not in workspace_vault:
        fail(f"primary encrypted workspace migration contract is missing: {marker}")
for marker in ("memo-native-workflows.bin", "synthos-vault.bin", "synthos.runtime.store.v1", "getExistingKey()", "writePayloadAtomically(legacyPayload)"):
    if marker not in native_store:
        fail(f"native workflow store migration contract is missing: {marker}")
if "printf '%s\\n' \"$CHANGED_FILES\"" not in workflow or "INCOMING_ARCHIVE_CHANGE" not in workflow:
    fail("GitHub workflow must build direct source changes without depending on, or overwriting them from, a stale ZIP")
if "Direct source verification requested" not in workflow or "SOURCE_ONLY=true" not in workflow:
    fail("GitHub workflow is missing the checked-out-source verification route")
if "legacySharedFile.delete()" in native_store or "legacySharedFile.delete()" in workspace_vault:
    fail("legacy encrypted source file must never be deleted by automatic migration")
if workspace_vault.index("AppSnapshot.fromJson(decrypt(legacyBytes)") > workspace_vault.index("copyLegacyWorkspaceAtomically(legacyBytes)"):
    fail("primary legacy ciphertext must be authenticated before it is copied")
if native_store.index("decryptPayload(legacyPayload)") > native_store.index("writePayloadAtomically(legacyPayload)"):
    fail("native legacy ciphertext must be authenticated before it is copied")
for marker in ("phase=seed", "phase=verify", "memo-storage-upgrade-acceptance", "File(target.filesDir, TEST_DIRECTORY)", "legacyWorkspace.writeBytes(workspace.readBytes())", "First launch after the in-place update", "AtomicFile backup was not restored", "Corrupt primary", "Unknown formats"):
    if marker not in upgrade_test:
        fail(f"encrypted-store upgrade instrumentation coverage is missing: {marker}")
for marker in ("app-release-androidTest.apk", "run_store_acceptance_phase seed", "run_store_acceptance_phase verify", 'adb install -r "$CURRENT_APK"'):
    if marker not in workflow:
        fail(f"CI does not execute the encrypted-store in-place upgrade test: {marker}")
print("PASS: encrypted workspace/native store separation, authenticate-before-copy migration, legacy preservation, and in-place upgrade instrumentation are wired")

task_spec_source = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/WorkflowGraph.kt").read_text()
capability_source = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/CapabilityCompiler.kt").read_text()
proposal_parser = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/WorkflowTaskProposalParser.kt").read_text()
task_graph_tests = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/WorkflowGraphTest.kt").read_text()
proposal_tests = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/WorkflowTaskProposalParserTest.kt").read_text()
runtime_registry = (ROOT / "core/src/main/kotlin/app/memo/android/runtime/RuntimeContracts.kt").read_text()
runtime_registry_tests = (ROOT / "core/src/test/kotlin/app/memo/android/runtime/RuntimeRegistryTest.kt").read_text()
for marker in ("RuntimeDescriptor(\"app.approvals\"", "approval.approve_exact", "approval.reject", "model.generate_remote", "externalSideEffect: Boolean = false", "browser.submit_prompt", "true, true"):
    if marker not in runtime_registry:
        fail(f"human approval runtime contract is missing: {marker}")
if "localInferenceAndRemoteProviderConsentAreDifferentCapabilities" not in runtime_registry_tests:
    fail("local and remote model approval policy test is missing")
if "humanApprovalInboxIsARegisteredRuntimeDistinctFromTheJobWorkbench" not in runtime_registry_tests:
    fail("human approval runtime isolation test is missing")
for marker in ("data class TaskSpec(", "externalSideEffect: Boolean = false", "typealias WorkflowNodeSpec = TaskSpec", "fun compile(\n        goal: String", "Workflow model-call budget exceeded", "Workflow total timeout exceeds", "approval-gated capability", "dedicated approval checkpoint", "cannot be automatically retried", "User checkpoints must target"):
    if marker not in task_spec_source:
        fail(f"typed dynamic workflow graph contract is missing: {marker}")
for marker in ("fun compileForTasks(", "task.runtimeId to it", "externalSideEffect = capability.externalSideEffect", "unresolvedRequirements = missing", "registeredRuntimeIds = registeredRuntimes.map", "mode=task-spec"):
    if marker not in capability_source:
        fail(f"dynamic capability assembly is missing: {marker}")
for marker in ("MAX_RAW_CHARS = 64_000", "UUID.randomUUID()", "CapabilityCompiler.compileForTasks", "WorkflowGraphCompiler.compile", "private fun integer(", "numeric % 1.0 == 0.0"):
    if marker not in proposal_parser:
        fail(f"untrusted TaskSpec proposal parser is missing: {marker}")
for marker in ("dynamicTaskSpecsCompileIntoAValidatedFanOutAndJoinDag", "NeverBorrowsAnIdenticallyNamedCapability", "dynamicGraphRejectsModelAndDurationBudgetOverruns"):
    if marker not in task_graph_tests:
        fail(f"dynamic workflow graph regression coverage is missing: {marker}")
for marker in ("parsesDynamicTaskSpecsAndCreatesFreshAppOwnedIdempotencyKeys", "rejectsCapabilitiesThatExistOnlyOnAnotherRuntime", "rejectsAnUnregisteredRuntimeEvenWhenTheTaskNeedsNoCapabilities", "approvalGatedCapabilitiesRequireAnExplicitApprovalTask", "remoteModelConsentUsesApprovalWithoutBeingClassifiedAsExternalWrite", "externalSideEffect", "rejectsCyclesAndUnapprovedExternalWrites", "rejectsMalformedAndOversizedTaskFields"):
    if marker not in proposal_tests:
        fail(f"TaskSpec proposal parser regression coverage is missing: {marker}")
print("PASS: typed TaskSpec proposals compile into bounded DAGs with exact-runtime capability resolution and fail-closed validation")


agent_engine = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/AgentExecutionEngine.kt").read_text()
agent_registry = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/AgentCapabilityRegistry.kt").read_text()
agent_registry_tests = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/AgentCapabilityRegistryTest.kt").read_text()
browser_url_policy = (ROOT / "core/src/main/kotlin/app/memo/android/runtime/BrowserUrlPolicy.kt").read_text()
browser_url_tests = (ROOT / "core/src/test/kotlin/app/memo/android/runtime/BrowserUrlPolicyTest.kt").read_text()
browser_session = (ROOT / "app/src/main/java/app/memo/android/browser/BrowserSession.kt").read_text()
agent_tools = (ROOT / "app/src/main/java/app/memo/android/ai/MemoWorkspaceAgentTools.kt").read_text()
agent_engine_tests = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/AgentExecutionEngineTest.kt").read_text()
for marker in ('RuntimeDescriptor("app.workspace"', 'RuntimeCapability("workspace.search"', 'id = "runtime.browser"', 'RuntimeCapability("browser.navigate_https"', 'RuntimeCapability("browser.capture_evidence"', 'RuntimeCapability("browser.open_observed_link"'):
    if marker not in runtime_registry:
        fail(f"Talk runtime capability contract is missing: {marker}")
for marker in ("AgentCapabilityRegistry(RuntimeRegistry().listRuntimes(), adapters)", "capabilityRegistry.availableAdapters()", "capabilityRegistry.resolve(decision.toolId)", "val runtimeId: String =", "workspace.create_work_plan when it is available", "saves a DRAFT only"):
    if marker not in agent_engine:
        fail(f"Talk dynamic capability discovery is missing: {marker}")
for marker in ("definition.runtimeId", "no matching policy entry", "policy.requiresUserApproval == definition.requiresUserApproval", "policy.externalSideEffect == hasExternalSideEffect", "fun availableAdapters()"):
    if marker not in agent_registry:
        fail(f"runtime capability policy enforcement is missing: {marker}")
for marker in ("adaptersMustMatchARegisteredRuntimeCapability", "readinessIsRecheckedInsteadOfFrozenAtConstruction", "adapterCannotWeakenRegisteredApprovalPolicy"):
    if marker not in agent_registry_tests:
        fail(f"runtime capability registry regression coverage is missing: {marker}")
for marker in ("https://", "uri.rawUserInfo != null", "uri.port !in setOf(-1, 443)", "explicitlySuppliedUrls", "wasExplicitlySupplied"):
    if marker not in browser_url_policy:
        fail(f"browser URL policy is missing a public HTTPS / user-supplied URL guard: {marker}")
for marker in ("rejectsHttpCredentialsLocalIpAndNonstandardPortUrls", "modelMayNavigateOnlyToAnExactUrlInTheUsersMessage"):
    if marker not in browser_url_tests:
        fail(f"browser URL safety regression coverage is missing: {marker}")
for marker in ("canNavigateForAgent", "canCaptureForAgent", "navigateForAgent", "captureCurrentPageForAgent", "sameAgentPageTarget", "agentNavigationPending", "onReceivedSslError"):
    if marker not in browser_session:
        fail(f"visible BrowserSession agent integration or fail-closed check is missing: {marker}")
for marker in ('id = "browser.navigate_https"', 'id = "browser.capture_evidence"', "BrowserUrlPolicy.explicitlySuppliedUrls", "BROWSER_PAGE_CAPTURE", "saveWorkArtifactDurably", 'runtimeId = "runtime.browser"'):
    if marker not in agent_tools:
        fail(f"real Memo browser evidence adapter is incomplete: {marker}")
for marker in ("capabilitiesAreRediscoveredAfterEachSuccessfulStep", "browser.navigate_https", "browser.capture_evidence"):
    if marker not in agent_engine_tests:
        fail(f"dynamic sequential browser capability test is missing: {marker}")
browser_scripts = (ROOT / "app/src/main/java/app/memo/android/browser/BrowserScripts.kt").read_text()
for marker in ("fun verifyObservedLink(", "navigateToObservedLinkForAgent", "BrowserUrlPolicy.isSameOriginHttpsLink"):
    if marker not in browser_session and marker not in browser_scripts:
        fail(f"observed-link navigation is missing its live-origin or DOM verification: {marker}")
for marker in ('id = "browser.open_observed_link"', "scope.observedLinks", "scope.capturedPageUrl", "latest captured page's same-origin link set"):
    if marker not in agent_tools:
        fail(f"Talk observed-link adapter is missing its source binding or availability guard: {marker}")
for marker in ("observedLinksCannotEscapeCapturedOrigin", "isSameOriginHttpsLink"):
    if marker not in browser_url_tests:
        fail(f"observed-link origin policy regression test is missing: {marker}")
talk_ui = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in ("shareBrowserPageRemotely", "includeBrowserPageContent", "AndroidView(", "browser.webView", "showAgentBrowserPane"):
    if marker not in talk_ui:
        fail(f"Talk browser visibility or separate remote-page privacy control is missing: {marker}")
for marker in (
    "data class AgentToolPostconditionVerification(",
    "suspend fun verifyPostcondition(",
    "adapter.verifyPostcondition(decision.arguments, invocation, toolResult)",
    "verification.evidenceReference != toolResult.evidenceReference",
    "POSTCONDITION VERIFIED:",
    "A capability may be reported as successful only after its adapter returns a verified postcondition",
):
    if marker not in agent_engine:
        fail(f"shared Talk engine does not enforce verified capability outcomes: {marker}")
if agent_tools.count("override suspend fun verifyPostcondition(") != 10:
    fail("each of the ten current Talk adapters must implement an explicit postcondition verifier")
for marker in (
    "deterministic recomputation from the permitted candidate set",
    "exact note title, content and digest",
    "exact typed record, properties, source conversation/message",
    "navigationEvidenceReference(page.url)",
    "lastCapturedContentDigest",
    "read back from encrypted workspace evidence",
):
    if marker not in agent_tools:
        fail(f"current Talk adapter verification is incomplete: {marker}")
for marker in (
    "compoundGoalsAreDirectedToTheDurableWorkPlanCapability",
    "successfulReadOnlyResultWithoutVerifierFailsAndAllowsReplanning",
    "localWriteWithoutPostconditionVerifierStopsForReview",
    "writePostconditionMustMatchReturnedEvidenceReference",
):
    if marker not in agent_engine_tests:
        fail(f"shared Talk postcondition regression coverage is missing: {marker}")
studio_artifact_policy = (ROOT / "core/src/main/kotlin/app/memo/android/data/StudioArtifactPolicy.kt").read_text()
studio_artifact_tests = (ROOT / "core/src/test/kotlin/app/memo/android/data/StudioArtifactPolicyTest.kt").read_text()
workspace_view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
for marker in ("data class StudioArtifactDraft(", "object StudioArtifactPolicy", "fun prepare(", "MessageDigest.getInstance(\"SHA-256\")"):
    if marker not in studio_artifact_policy:
        fail(f"Studio artifact validation/digest contract is incomplete: {marker}")
if 'STUDIO_ARTIFACT' not in workspace_view_model or 'setOf("WORK_ARTIFACT", "ARTIFACT_DRAFT", "SELF_MODEL_ARTIFACT", "STUDIO_ARTIFACT")' not in workspace_view_model:
    fail("Talk-created Studio drafts must be editable through the existing revision-preserving artifact lifecycle")
for marker in ("StudioArtifactPolicy.prepare(cleanTitle, artifactType, cleanContent)", "metadata.put(\"contentDigest\", revisedDraft.contentDigest)", "metadata.put(\"version\", metadata.optInt(\"version\", 1) + 1)"):
    if marker not in workspace_view_model:
        fail(f"Studio artifact edits must refresh the content digest and revision version: {marker}")
if "studioArtifactSaveIsRegisteredAsAReversibleLocalWorkspaceWrite" not in agent_registry_tests:
    fail("Studio artifact runtime must be explicitly registered as a reversible local write")
for marker in (
    'RuntimeCapability("workspace.save_artifact"',
    'id = "workspace.save_artifact"',
    "StudioArtifactPolicy.prepare",
    'kind = "STUDIO_ARTIFACT"',
    "sourceExcerptDigest",
    "CONTENT SHA-256:",
):
    if marker not in runtime_registry + agent_tools:
        fail(f"Talk-to-Studio artifact capability is missing its runtime, persistence or evidence contract: {marker}")
for marker in (
    "fun normalizesDescriptiveTypeAndProducesStableDigest",
    "fun digestBindsTitleTypeAndContent",
    "fun rejectsBlankAndOversizedArtifactFields",
    "fun acceptsNewArtifactTypesWithoutAddingSpecializedWorkflows",
):
    if marker not in studio_artifact_tests:
        fail(f"Studio artifact policy regression coverage is missing: {marker}")
for marker in ('"STUDIO_ARTIFACT"', "Your generated artifacts", "artifactType", '"BROWSER_PROMPT_SUBMISSION"', '"BROWSER_PROMPT_RESPONSE"', "Browser research evidence"):
    if marker not in talk_ui:
        fail(f"Studio does not expose Talk-created artifacts for review: {marker}")
agent_work_plan_policy = (ROOT / "core/src/main/kotlin/app/memo/android/data/AgentWorkPlanPolicy.kt").read_text()
agent_work_plan_tests = (ROOT / "core/src/test/kotlin/app/memo/android/data/AgentWorkPlanPolicyTest.kt").read_text()
for marker in ("object AgentWorkPlanPolicy", "WorkflowGraphValidator.validate(graph)", "WorkflowGraphValidator.topologicalOrder(graph)", 'status = "DRAFT"', 'workflowGraphJson = graph.toJson()', "No step has run"):
    if marker not in agent_work_plan_policy:
        fail(f"dynamic Work plan persistence policy is incomplete: {marker}")
for marker in ("createsReviewableWorkStepsInDependencyOrderAndPreservesApprovalBoundary", "rejectsMissingSourceAndInvalidTitle", "doesNotMarkAnyProposedStepAsCompleted"):
    if marker not in agent_work_plan_tests:
        fail(f"dynamic Talk work plan regression coverage is missing: {marker}")
for marker in ('RuntimeCapability("workspace.create_work_plan"', 'id = "workspace.create_work_plan"', "WorkflowTaskProposalParser.parse(", "createAgentWorkPlan(", "persistLatestSnapshotAndReadSafely()", "job.workflowGraphJson == expected.graphJson", "DRAFT"):
    if marker not in runtime_registry + agent_tools + workspace_view_model:
        fail(f"Talk dynamic Work plan capability is missing its runtime, persistence or read-back contract: {marker}")
print("PASS: Talk can save source-linked private Markdown drafts through the registered workspace runtime; digest, bounds and Studio review surface are guarded")

work_ui = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in (
    'node.participant == app.memo.android.orchestration.WorkflowParticipant.LOCAL_MODEL',
    'node.runtimeId == "app.models"',
    'node.requiredCapabilities == listOf("model.generate")',
):
    if marker not in agent_work_plan_policy:
        fail(f"Only the exact local model capability may become an executable Work checkpoint: {marker}")
for marker in (
    'assertEquals("LOCAL_MODEL", job.steps[0].checkpointKind)',
    "remoteModelGenerationIsNotClassifiedAsAnExecutableLocalModelStep",
):
    if marker not in agent_work_plan_tests:
        fail(f"Local-model Work execution boundary regression is missing: {marker}")
for marker in (
    'step.checkpointKind == "LOCAL_MODEL"',
    'onOpenModels = onOpenModels',
    'onOpenModels: () -> Unit',
    'onRunWithModel(step.id, localModel.id)',
    'currentStep.status !in setOf("PENDING", "RUNNING")',
    'Run this step with',
    'result is saved as a draft for your review',
):
    if marker not in work_ui:
        fail(f"Work does not expose the safe one-tap local-model execution path: {marker}")
local_model_branch_start = work_ui.index('isNext && step.checkpointKind == "LOCAL_MODEL"')
local_model_branch_end = work_ui.index('isNext && (step.requiresApproval || step.checkpointKind == "EXTERNAL_ACTION")', local_model_branch_start)
local_model_branch = work_ui[local_model_branch_start:local_model_branch_end]
if 'onStartStep(step.id)' in local_model_branch:
    fail("Work must not start a checkpoint separately from its shared execution run")
for marker in (
    'fun buildWorkStep(',
    'StudioArtifactAdapter(viewModel, sourceConversationId, sourceMessageId, sourceExcerpt, reuseExistingDrafts = false)',
    'sourceMessageId.isNotBlank()',
    'it.id == sourceMessageId && it.role == "USER"',
):
    if marker not in agent_tools:
        fail(f"Work artifact adapter is not restricted to its exact source and private-write capability: {marker}")
for marker in (
    'launchSharedWorkStepExecution(',
    'viewModel.launchAgentExecution(',
    'AgentExecutionEngine(',
    'MemoWorkspaceAgentTools.buildWorkStep(',
    'runtime.generateAgentTurn(',
    'appendAgentEventDurably(',
    'finishAgentRunDurably(',
    'finishAgentWorkStepDurably(',
    'startJobStep(freshJob.id, freshStep.id, runId)',
    'AgentExecutionConfig(maxToolCalls = 1)',
    'AgentExecutionEventType.TOOL_FINISHED',
    'val selectedModelId = if (modelId.startsWith("local:")) modelId else "local:$modelId"',
):
    if marker not in work_ui:
        fail(f"Work local-model execution bypasses the shared durable engine contract: {marker}")
helper_start = work_ui.index('private fun launchSharedWorkStepExecution(')
helper_end = work_ui.index('@Composable\nfun SelfModelStudioScreen(', helper_start)
if 'runtime.runPrompt(' in work_ui[helper_start:helper_end]:
    fail("Work local-model execution must not bypass the shared AgentExecutionEngine")
for marker in (
    "This run was interrupted or its outcome is uncertain.",
    "Inspect execution transcript",
):
    if marker not in work_ui:
        fail(f"Interrupted Work checkpoints must remain review-only: {marker}")
print("PASS: Work local-model checkpoints use the shared durable Talk engine, exact artifact adapter, event journal and review-only completion boundary")

for marker in (
    'RuntimeCapability("browser.submit_prompt"',
    'id = "browser.submit_prompt"',
    "AgentApprovalEnvelope(",
    "ActionApprovalLedger.contentHash(",
    "beginExternalActionDurably(",
    "completeExternalActionDurably(",
    "markExternalActionNeedsReviewDurably(",
    "BROWSER_PROMPT_SUBMISSION",
    "submitPromptForAgent(",
):
    if marker not in runtime_registry + agent_tools:
        fail(f"Approved browser prompt submission is missing its exact approval/evidence boundary: {marker}")
for marker in (
    "suspend fun submitPromptForAgent(",
    "BrowserScripts.preparePrompt(cleanPrompt)",
    "BrowserScripts.sendPreparedPrompt(cleanPrompt)",
    "BrowserScripts.verifyPromptSubmission(cleanPrompt, preserveState = true)",
):
    if marker not in browser_session:
        fail(f"Browser prompt submission must use the visible, verified composer path: {marker}")
if "fun verifyPromptSubmission" not in browser_scripts:
    fail("The browser prompt submission verifier is missing")
for marker in ('prepared.optString("content")', 'sent.optString("content")'):
    if marker not in browser_session:
        fail(f"Browser submission failures must surface the actual visible-page reason: {marker}")
browser_prompt_safety_tests = (ROOT / "app/src/test/java/app/memo/android/browser/BrowserPromptSubmissionSafetyTest.kt").read_text()
browser_execution_invariants = (ROOT / "docs/AI_EXECUTION_INVARIANTS.md").read_text()
for marker in (
    "**Exact browser prompt submission.**",
    "must never replace a non-empty composer",
    "newly observed user-authored message",
    "stop at `NEEDS_REVIEW`",
):
    if marker not in browser_execution_invariants:
        fail(f"Browser exact-submission architecture invariant is missing: {marker}")
for marker in (
    "existing.length>0 && existing!==normalize(prompt)",
    "value() !== wanted",
    "message=>message===normalize(wanted)",
    "newExactUserMessage=afterExactMatches>Number(state.beforeExactMatches||0)",
    "composerCleared && newExactUserMessage",
):
    if marker not in browser_scripts:
        fail(f"Browser prompt submission safety invariant is missing: {marker}")
for marker in (
    "preparationPreservesAnExistingDifferentComposerDraft",
    "sendRequiresTheComposerToMatchTheApprovedPrompt",
    "verificationRequiresANewExactUserMessageAndAnEmptyComposer",
):
    if marker not in browser_prompt_safety_tests:
        fail(f"Browser prompt safety regression test is missing: {marker}")
if "browserPromptSubmissionIsAnExplicitlyApprovedExternalWrite" not in agent_registry_tests:
    fail("Browser prompt submission must have a regression test for explicit external-write approval")
if "browserResponseCaptureDoesNotAuthorizeExternalSideEffects" not in agent_registry_tests:
    fail("Browser response capture must remain read-only and non-approvable as an external write")
for marker in ('id = "browser.capture_response"', "BrowserPromptResponseAdapter(", "UNTRUSTED EXTERNAL RESPONSE", "captureAssistantResponseForAgent(", "BrowserScripts.pollAssistantResponse"):
    if marker not in agent_tools + browser_session:
        fail(f"Consent-gated stable browser response capture is incomplete: {marker}")
if "add(BrowserPromptResponseAdapter(" not in agent_tools:
    fail("Stable browser response capture must be wired only into the consent-enabled Talk tool set")
consent_block_start = agent_tools.find("if (includeBrowserPageContent)")
consent_block_end = agent_tools.find("\n            }", consent_block_start)
if consent_block_start < 0 or consent_block_end < 0 or "add(BrowserPromptResponseAdapter(" not in agent_tools[consent_block_start:consent_block_end]:
    fail("Browser response capture must be gated by explicit browser-page sharing consent")
if "var lastPromptResponseSubmissionEvidenceId" not in agent_tools or "scope.lastPromptResponseSubmissionEvidenceId = submissionId" not in agent_tools:
    fail("Browser response evidence must be idempotent for each prompt submission")
print("PASS: browser prompt submission is exact-content approved and visibly verified; response capture is consent-gated, baseline-bound and idempotent")


print("PASS: capability success is gated by explicit postcondition verification; unverified reads fail and uncertain writes require review")
for marker in ("writeOutcomeUncertainOnCancellation", "AgentExecutionEventType.RUN_NEEDS_REVIEW"):
    if marker not in agent_engine:
        fail(f"write interruption can be misclassified as cancellation without review: {marker}")
for marker in (
    "cancellationDuringLocalWriteInvocationRequiresReview",
    "cancellationDuringLocalWriteVerificationRequiresReview",
    "failedWriteAdapterStopsForReviewInsteadOfReplanning",
):
    if marker not in agent_engine_tests:
        fail(f"write-interruption safety regression coverage is missing: {marker}")
print("PASS: started/uncertain writes stop for review on cancellation or adapter failure; they are not replayed")

print("PASS: runtime-backed per-turn capability discovery, approval-policy matching, user-supplied HTTPS browser navigation, encrypted page evidence and separate remote-page consent are enforced and tested")


print("PASS: CI safely handles canonical ZIP imports and checked-out direct-source builds, verifies source integrity, and runs Android unit tests/lint/APK assembly")

runner_tests = list((ROOT / "remote-runner/tests").glob("test_*.py"))
if not runner_tests:
    fail("no remote runner tests found")
print(f"PASS: remote runner test suite is present ({len(runner_tests)} test module(s)); run it separately with unittest")
print("NOTE: This script does not compile Kotlin, run Gradle, prove Android runtime behavior, or verify a device.")
