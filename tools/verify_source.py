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
    "app/src/main/java/app/memo/android/ui/MemoTheme.kt",
    "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt",
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

# Memo product-specific regressions: no second legacy Activity UI, and the home prompt remains an unsent draft.
legacy_activity = ROOT / "app/src/main/java/com/synthos/runtime/MainActivity.java"
if legacy_activity.exists():
    fail("legacy duplicate SynthOS Activity still exists; runtime UI must route through MemoRuntimeBridge")
bridge = ROOT / "app/src/main/java/app/memo/android/runtime/MemoRuntimeBridge.java"
if "interface Listener" not in bridge.read_text() or "openForExternalCapture" not in bridge.read_text():
    fail("Memo runtime bridge is missing listeners or capture entry points")
home = (ROOT / "app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt").read_text()
chat = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in ("onStartConversation", "Make sense of what matters.", "Needs your decision"):
    if marker not in home:
        fail(f"Memo-first home is missing product marker: {marker}")
for marker in ("initialPrompt", "onInitialPromptConsumed", "draft = initialPrompt.take(6_000)"):
    if marker not in chat:
        fail(f"home-to-chat draft handoff is missing marker: {marker}")
if "MainActivity.notifyRuntimeChanged" in (ROOT / "app/src/main/java/com/synthos/runtime/engine/AutomationAccessibilityService.java").read_text():
    fail("accessibility runtime still targets legacy Activity helpers")
print("PASS: one Memo Activity, narrow runtime bridge, conversation-first home, and safe draft handoff")

# Estimate coverage of the static string literals passed to the custom UI Text wrapper.
# URLs and empty placeholders are intentionally not translations. Dynamic/interpolated
# and string-valued labels are outside this conservative static scan.
ui_dir = ROOT / "app/src/main/java/app/memo/android/ui"
localization_sources = (ui_dir / "MemoLocalization.kt").read_text()
translation_keys = set(re.findall(r'^\s*"((?:[^"\\]|\\.)*)"\s+to\s+\(', localization_sources, re.M))
scanned = []
for ui_file in ("MemoWorkspaceApp.kt", "WorkspaceScreens.kt", "AppsWorkspaceScreen.kt", "BrowserWorkflowScreen.kt", "AiScreens.kt", "ActionApprovalInboxScreen.kt"):
    source = (ui_dir / ui_file).read_text()
    scanned.extend(re.findall(r'\bText\(\s*"((?:[^"\\]|\\.)*)"', source))
translatable = [value for value in scanned if len(value.strip()) > 1 and "$" not in value and "\\n" not in value and not value.startswith(("https://", "http://"))]
missing = sorted(set(value for value in translatable if value not in translation_keys))
if missing:
    fail("static UI strings are missing Polish/Turkish catalog entries: " + " | ".join(missing[:30]))
print(f"PASS: {len(translatable)}/{len(translatable)} static UI Text literals requiring translation are present in the catalog ({len(scanned)} literals scanned; URLs/placeholders excluded)")

source_manifest = ROOT / "SOURCE_SHA256SUMS.txt"
if not source_manifest.is_file():
    fail("source SHA-256 manifest is missing")

workflow = (ROOT / ".github/workflows/build-apk.yml").read_text()
if "incoming/memo.zip" not in workflow:
    fail("GitHub workflow does not use the canonical incoming/memo.zip source upload")
if "Memo-Android-Repository.zip" in workflow:
    fail("GitHub workflow must not depend on a legacy root-level source ZIP")
for task in (":core:test", ":app:testDebugUnitTest", ":app:lintDebug", ":app:assembleDebug"):
    if task not in workflow:
        fail(f"GitHub workflow missing Android gate: {task}")
if any(alternate in workflow for alternate in ("Memo-Android-source.zip", "Memo-Android-Download-Fresh.zip")):
    fail("GitHub workflow must not fall back to an alternate/stale ZIP")
if "Expected exactly one Android Gradle project root" not in workflow or "Memo-Android-debug-APK" not in workflow:
    fail("GitHub workflow is missing canonical-root or artifact-delivery checks")
if "sha256sum -c SOURCE_SHA256SUMS.txt" not in workflow or "Memo-Source-Archive-SHA256.txt" not in workflow:
    fail("GitHub workflow is missing source-integrity verification/receipt")
print("PASS: CI builds only the canonical ZIP, verifies its source manifest, checks the one-Activity boundary, and runs Android unit tests/lint/APK assembly")

runner_tests = list((ROOT / "remote-runner/tests").glob("test_*.py"))
if not runner_tests:
    fail("no remote runner tests found")
print(f"PASS: remote runner test suite is present ({len(runner_tests)} test module(s)); run it separately with unittest")
print("NOTE: This script does not compile Kotlin, run Gradle, prove Android runtime behavior, or verify a device.")
