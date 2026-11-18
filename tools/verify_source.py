#!/usr/bin/env python3
"""Offline structural checks for the Memo Android source package.

This script is intentionally not presented as an Android build/test. CI must still
run Gradle unit tests, lint, and APK assembly, then exercise a real device.
"""
from __future__ import annotations

import re
import subprocess
import sys
import xml.etree.ElementTree as ET
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
REQUIRED = [
    "settings.gradle",
    "build.gradle",
    "core/build.gradle",
    "core/src/main/kotlin/app/memo/android/data/Models.kt",
    "core/src/main/kotlin/app/memo/android/data/WorkflowAttemptJournal.kt",
    "core/src/main/kotlin/app/memo/android/data/FrontierResponseCaptureCoordinator.kt",
    "core/src/test/kotlin/app/memo/android/data/FrontierResponseCaptureCoordinatorTest.kt",
    "core/src/main/kotlin/app/memo/android/data/WorkflowRuntimeReceipt.kt",
    "core/src/main/kotlin/app/memo/android/data/LocalWorkArtifactCoordinator.kt",
    "core/src/test/kotlin/app/memo/android/data/LocalWorkArtifactCoordinatorTest.kt",
    "core/src/test/kotlin/app/memo/android/data/WorkspaceSchemaV3Test.kt",
    "core/src/test/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinatorTest.kt",
    "core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt",
    "core/src/main/kotlin/app/memo/android/data/LiveTimelineProjector.kt",
    "core/src/main/kotlin/app/memo/android/data/WorkArtifactFactory.kt",
    "core/src/main/kotlin/app/memo/android/orchestration/WorkflowProposalApplier.kt",
    "core/src/main/kotlin/app/memo/android/data/OutcomeParser.kt",
    "core/src/main/kotlin/app/memo/android/runtime/RuntimeContracts.kt",
    "core/src/main/kotlin/app/memo/android/runtime/EntityProposalRuntimeAdapter.kt",
    "core/src/main/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinator.kt",
    "core/src/test/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinatorTest.kt",
    "core/src/test/kotlin/app/memo/android/data/OutcomeParserTest.kt",
    "core/src/test/kotlin/app/memo/android/data/ActionApprovalLedgerTest.kt",
    "core/src/test/kotlin/app/memo/android/data/LiveTimelineProjectorTest.kt",
    "core/src/test/kotlin/app/memo/android/data/WorkArtifactFactoryTest.kt",
    "core/src/test/kotlin/app/memo/android/orchestration/WorkflowProposalApplierTest.kt",
    "app/build.gradle",
    "app/src/main/java/app/memo/android/ai/LocalModelRunner.kt",
    "app/src/test/java/app/memo/android/ai/LocalModelRunnerPolicyTest.kt",
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
browser_workflow = (ROOT / "app/src/main/java/app/memo/android/browser/BrowserWorkflow.kt").read_text()
browser_workflow_test = (ROOT / "app/src/test/java/app/memo/android/browser/BrowserWorkflowPromptsTest.kt").read_text()
view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
for marker in ("contentHash", "fun approve(", "fun beginExecution(", "fun completeVerified(", "verifyAuditChain", "recoverAfterRestart"):
    if marker not in approval_ledger:
        fail(f"exact-content action ledger missing required contract: {marker}")
for marker in ("changedPayloadOrDestinationCannotReuseApproval", "tamperedStoredPayloadAndTamperedAuditEventFailClosed", "schemaFourRoundTripsApprovalAndRejectsTamperedPayload"):
    if marker not in approval_test:
        fail(f"action ledger regression test missing: {marker}")
if "approveCurrentPrompt" not in browser_session or "awaitingPromptApproval" not in browser_session:
    fail("visible browser session lacks the exact-content approval state gate")
for marker in ("Approve exact external prompt?", "workflowState.currentPrompt", "Approve and send", "Reject · return to editing"):
    if marker not in workflow_screen:
        fail(f"browser workflow UI is missing exact-content approval behavior: {marker}")
revision_action = browser_session.split("fun reviseCurrentPrompt", 1)[1].split("/** Continue only when", 1)[0]
if "callbacks.onApprovalSuperseded" not in revision_action or "callbacks.onApprovalRequired" not in revision_action or "sendPreparedPrompt" in revision_action:
    fail("prompt revision must invalidate the old pending approval, create a new approval and never send")
reject_action = browser_session.split("fun rejectCurrentPrompt", 1)[1].split("/** Continue only when", 1)[0]
if "onApprovalSuperseded" not in reject_action or "stopBrowserWorkflow" in reject_action:
    fail("rejecting a pending prompt must return to editing without cancelling the full consultation")
for marker in ("fun canEditPreparedPrompt", "MAX_PROVIDER_PROMPT_LENGTH"):
    if marker not in browser_workflow:
        fail(f"prepared prompt edit policy is missing: {marker}")
for marker in ("preparedPromptCanBeEditedOnlyBeforeSubmissionAndWithoutUncertainty", "preparedPromptEditRejectsBlankAndOversizedContent"):
    if marker not in browser_workflow_test:
        fail(f"prepared prompt editing regression test is missing: {marker}")
for marker in ("Original request", "Prompt to send", "Apply revision · request approval"):
    if marker not in workflow_screen:
        fail(f"editable prompt review UI is missing: {marker}")
print("PASS: exact-content approval, safe prompt revision and reject-to-edit behavior are wired in source")

for marker in ("object BrowserWorkflowRecoveryPolicy", "status == BrowserWorkflowStatus.WAITING_FOR_USER", "sentForCurrentStep", "providerPageMatches", "MIN_MANUAL_RESPONSE_LENGTH"):
    if marker not in browser_workflow:
        fail(f"manual browser-response recovery policy is missing: {marker}")
capture_action = browser_session.split("fun captureCurrentResponseManually", 1)[1].split("/** Continue only when", 1)[0]
if "completeWorkflowStep" not in capture_action or "sendPreparedPrompt" in capture_action or "BrowserScripts.sendPreparedPrompt" in capture_action:
    fail("manual response capture must use the existing result path and must never resend the provider prompt")
retry_action = browser_session.split("fun retryCurrentResponseDetection", 1)[1].split("/** Resume after a login", 1)[0]
if "pollWorkflowResponse" not in retry_action or "sendPreparedPrompt" in retry_action or "BrowserScripts.sendPreparedPrompt" in retry_action:
    fail("response detection retry must poll the current page only and must never resend the provider prompt")
if "canRetryResponseDetection" not in browser_workflow or "retryCurrentResponseDetection" not in workflow_screen:
    fail("response detection retry is not guarded and exposed as a distinct user action")
for marker in ("canSkipProvider", "canStopCurrentProviderStep", "submissionUncertain", "fun skipCurrentProvider()", "fun stopCurrentProviderStep()", "onStepSkipped", "onStepStopped"):
    if marker not in browser_workflow + browser_session:
        fail(f"step-level recovery contract is missing: {marker}")
if "rejectExternalAction(pendingApproval.id" not in workflow_screen:
    fail("skipping a provider at the approval checkpoint must reject its pending exact-content approval")
if "Cancel entire run" not in workflow_screen or "Skip this provider" not in workflow_screen or "Stop this provider step & continue" not in workflow_screen:
    fail("browser workflow must expose separate skip, step-stop and whole-run cancel controls")
if "skippedProviderIds" not in workflow_screen or "stoppedProviderIds" not in workflow_screen:
    fail("partial provider execution must be recorded and must not be reported as full success")
for marker in ("Retry answer detection", "never resends the approved prompt", "Capture answer without resending", "browser.captureCurrentResponseManually", "captureMethod"):
    if marker not in workflow_screen:
        fail(f"manual response capture UI/provenance is missing: {marker}")
for marker in ("skipAndStopPoliciesKeepDifferentScopes", "responseDetectionRetryIsAvailableOnlyForASentStepWaitingForRecovery", "manualCaptureRequiresSentPromptWaitingStateMatchingProviderAndUsefulText", "manualCaptureRejectsUncertainSendWrongPageAndInvalidText", "manualCaptureRejectsProviderMismatchEvenWhenAResponseWasSent"):
    if marker not in browser_workflow_test:
        fail(f"manual response capture regression test is missing: {marker}")
print("PASS: manual browser-response recovery is capture-only, provider-bound and records capture provenance")

capture_coordinator = (ROOT / "core/src/main/kotlin/app/memo/android/data/FrontierResponseCaptureCoordinator.kt").read_text()
capture_coordinator_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/FrontierResponseCaptureCoordinatorTest.kt").read_text()
if 'put("captureMethod", request.captureMethod.take(40))' not in capture_coordinator:
    fail("frontier response capture method provenance is missing from the atomic evidence record")

for marker in ("SAVING_RESPONSE", "workflowCapturePending", "finishCaptureAcknowledgement", "advanceWorkflowAfterCapture"):
    if marker not in browser_session + browser_workflow:
        fail(f"browser response must wait for durable capture acknowledgement: {marker}")
if "commitFrontierResponseCaptureAsync" not in workflow_screen or "FrontierResponseCaptureRequest" not in workflow_screen:
    fail("frontier browser workflow is not connected to the encrypted response-capture transaction")
if "completeCapture(true" not in workflow_screen or "result.evidenceId.isNotBlank()" not in workflow_screen:
    fail("browser workflow must advance only after a durable response/evidence identifier is returned")
if "viewModel.addEvidence(responseEvidence)" in workflow_screen or "saveFrontierResponseEntity(response" in workflow_screen:
    fail("frontier response must use one atomic capture path, not the legacy multi-write callback")
for marker in ("successfulCaptureAtomicallySavesEvidenceTranscriptEntityAndStep", "approvedInProgressActionCompletesOnlyAgainstTheExactSavedStepEvidence", "repeatedIdenticalCaptureIsIdempotent", "conflictingResponseForSameProviderStepIsRejectedWithoutMutation"):
    if marker not in capture_coordinator_test:
        fail(f"frontier response transaction regression test is missing: {marker}")
if "commitFrontierResponseCaptureAsync" not in view_model or "persistAndAwait()" not in view_model:
    fail("frontier response capture must run in ViewModel scope and await encrypted persistence")
print("PASS: frontier response is durably committed before provider workflow advances")


live_timeline = (ROOT / "core/src/main/kotlin/app/memo/android/data/LiveTimelineProjector.kt").read_text()
live_timeline_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/LiveTimelineProjectorTest.kt").read_text()
live_screen = (ROOT / "app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt").read_text()
for marker in ("object LiveTimelineProjector", "snapshot.actionAuditEvents", "snapshot.evidence", "sortedWith"):
    if marker not in live_timeline:
        fail(f"Live timeline projection is missing: {marker}")
for marker in ("combinesWorkspaceRecordsIntoOneNewestFirstTimeline", "legacyActionWithoutAuditEventsAppearsOnceUsingItsLatestKnownTimestamp", "timelineLimitIsAppliedAfterNewestFirstOrdering"):
    if marker not in live_timeline_test:
        fail(f"Live timeline regression coverage is missing: {marker}")
if "LiveTimelineProjector.build(snapshot)" not in live_screen or "Workspace timeline" not in live_screen:
    fail("Live does not render the durable cross-workspace timeline")
print("PASS: Live combines durable activity, jobs, checkpoints, tasks, action audit and evidence records")
for marker in ("onOpenBrowserWorkflow", "browserState.awaitingPromptApproval", "Review prompt and controls", "Open workflow controls"):
    if marker not in live_screen:
        fail(f"Live active browser workflow re-entry is missing: {marker}")


studio_source = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
artifact_factory = (ROOT / "core/src/main/kotlin/app/memo/android/data/WorkArtifactFactory.kt").read_text()
artifact_factory_test = (ROOT / "core/src/test/kotlin/app/memo/android/data/WorkArtifactFactoryTest.kt").read_text()
for marker in ("StudioDraftDialog(", "WorkArtifactFactory.create(title, kind, content)", '"WORK_ARTIFACT", "ARTIFACT_DRAFT", "SELF_MODEL_ARTIFACT"'):
    if marker not in studio_source:
        fail(f"Studio's general private-draft flow is missing: {marker}")
for marker in ('"user-authored"', '"requiresHumanReview"', "MAX_CONTENT_CHARS"):
    if marker not in artifact_factory:
        fail(f"Private artifact factory is missing: {marker}")
for marker in ("createsPrivateUserAuthoredDraftWithReviewRequiredMetadata", "rejectsMissingTitleTypeAndContent", "enforcesThePersistedArtifactBounds"):
    if marker not in artifact_factory_test:
        fail(f"Private artifact factory regression coverage is missing: {marker}")
print("PASS: Studio creates validated private drafts in the existing editable artifact store")

work_source = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
# Talk's conversation index and all Work sections must share one page-level
# LazyColumn. This guards against fixed header stacks and nested vertical lists
# that make content unreachable on short screens.
talk_index = work_source.split("if (selectedConversation == null) {", 1)[1].split("    } else {\n        Column(Modifier.fillMaxSize()) {", 1)[0]
if "LazyColumn(" not in talk_index or "MemoPageHeader(title = \"Talk\"" not in talk_index or "items(filtered, key = { it.id })" not in talk_index:
    fail("Talk conversation index must include its header, empty state and cards in one page-level lazy list")
talk_thread = work_source.split("    } else {\n        Column(Modifier.fillMaxSize()) {", 1)[1].split("Surface(tonalElevation = 2.dp", 1)[0]
if talk_thread.index("LazyColumn(") > talk_thread.index("if (modelOptions.isEmpty())"):
    fail("Talk request controls must scroll with the message history rather than consume a fixed page section")
work_screen = work_source.split("fun WorkWorkspaceScreen(", 1)[1].split("@Composable\nfun SelfModelStudioScreen", 1)[0]
if work_screen.count("LazyColumn(") != 1 or "todayWorkItems(" not in work_screen or "items(visibleEntities, key = { it.id })" not in work_screen or "items(snapshot.jobs.sortedByDescending { it.updatedAt }" not in work_screen:
    fail("Work sections must use one page-level lazy list without nested vertical scrolling")

workflow_screen = (ROOT / "app/src/main/java/app/memo/android/ui/BrowserWorkflowScreen.kt").read_text()
# Android WebView can render above Compose siblings. Keep it on the dedicated
# Browser page, not mounted inside the independently scrolling workflow page.
if "AndroidView(" in workflow_screen or "browser.webView" in workflow_screen:
    fail("Browser workflow page must not embed the live WebView over its scrollable controls")
if "Open browser" not in workflow_screen or "verticalScroll(rememberScrollState())" not in workflow_screen:
    fail("Browser workflow page must remain scrollable and link to the dedicated browser")
proposal_applier = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/WorkflowProposalApplier.kt").read_text()
proposal_applier_test = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/WorkflowProposalApplierTest.kt").read_text()
for marker in ("expandDraftWorkflowWithLocalModel(", "WorkflowTaskProposalParser.parse(raw, intent, runtimes)", "viewModel.applyValidatedWorkflowProposal(jobId", "Expand with local model", "Expand the plan with a local model", "onStartConversation(intent, expandWithLocalModel)", "canExpandCurrentDraft", "IntentPlanner.shouldCreateWorkflow(draft)"):
    if marker not in work_source:
        fail(f"Talk/Work local TaskSpec expansion is missing: {marker}")
for marker in ("original user goal", "runtimeStatus != \"AVAILABLE\"", "externalSideEffect", "No nodes were executed"):
    if marker not in proposal_applier:
        fail(f"Validated workflow proposal application is missing: {marker}")
for marker in ("appliesValidatedDraftGraphWithoutExecutingNodes", "Depends on: scope", "refusesToReplaceAPlanThatHasAlreadyStarted", "refusesAProposalThatChangesTheOriginalUserGoal", "refusesCapabilitiesOnAnOptionalRuntimeThatIsNotAvailable", "refusesApprovalRuntimeEvenWhenTheNodeIsNotMarkedAsAnExternalWrite", "refusesApprovalGatedOrExternalWriteProposalsInTheDraftOnlyExpansionFlow"):
    if marker not in proposal_applier_test:
        fail(f"Workflow proposal applier regression coverage is missing: {marker}")
print("PASS: local TaskSpec expansion is validated against the current runtime registry and stays draft-only")
# A local-model Work step must pass the persisted dependency/runtime gate before inference.
run_start = work_source.index("onRunWithModel = { stepId, modelId ->")
run_end = work_source.index("onOpenEvidence = onOpenEvidence", run_start)
local_run = work_source[run_start:run_end]
if "viewModel.startJobStep(job.id, stepId)" not in local_run:
    fail("local-model Work execution bypasses the persisted checkpoint start gate")
if local_run.index("viewModel.startJobStep(job.id, stepId)") > local_run.index("runtime.runPrompt("):
    fail("local-model inference starts before the checkpoint is reserved")
for marker in ('"requiresHumanReview"', '"local-model-draft"'):
    if marker not in local_run:
        fail(f"local-model Work execution is missing its review-required artifact metadata: {marker}")
# Review statuses are owned by the pure-core transition coordinator, not duplicated in UI callback code.
review_coordinator_source = (ROOT / "core/src/main/kotlin/app/memo/android/data/LocalWorkArtifactCoordinator.kt").read_text()
for marker in ('status = "NEEDS_REVIEW"', 'status = "WAITING_FOR_USER"', 'WorkflowAttemptJournal.outputSaved(', 'WorkflowAttemptJournal.failed('):
    if marker not in review_coordinator_source:
        fail(f"local Work transition coordinator is missing its review/failure lifecycle: {marker}")
if "executionSnapshot.evidence" not in local_run:
    fail("local-model Work execution does not build context from the post-start workspace snapshot")
if "viewModel.saveWorkArtifactForReview(currentJob.id, currentStep.id, artifact)" not in local_run:
    fail("local-model Work artifact and checkpoint review state are not committed atomically")
if "viewModel.markJobStepNeedsReview(" not in local_run:
    fail("local-model Work failure path can leave a checkpoint RUNNING")
view_model_source = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
local_artifact_coordinator = (ROOT / "core/src/main/kotlin/app/memo/android/data/LocalWorkArtifactCoordinator.kt").read_text()
local_artifact_tests = (ROOT / "core/src/test/kotlin/app/memo/android/data/LocalWorkArtifactCoordinatorTest.kt").read_text()
for marker in ("LocalWorkArtifactCoordinator.saveForReview(", "LocalWorkArtifactCoordinator.markNeedsReview("):
    if marker not in view_model_source:
        fail(f"ViewModel does not persist the tested local Work transition: {marker}")
for marker in ("fun saveForReview(", "step.status == \"RUNNING\"", "evidence = \"evidence:", "status = \"WAITING_FOR_USER\"", "fun markNeedsReview("):
    if marker not in local_artifact_coordinator:
        fail(f"pure local Work transition coordinator is missing: {marker}")
for marker in ("savesDraftAndMovesStepAndJobToReviewInOneSnapshot", "receiptBindsTheExactSavedBytesAndRejectsLaterArtifactMutation", "legacySchemaSevenOutputAttemptsWithoutReceiptsRemainValid", "refusesToSaveOutputWhenThereIsNoUniqueActiveRuntimeAttempt", "refusesToAttachAnArtifactToAStaleCheckpoint", "refusesAnArtifactThatBelongsToAnotherJob", "refusesDuplicateEvidenceIdentity", "failureTransitionMovesOnlyTheActiveStepToReview", "refusesFailureTransitionForACheckpointThatIsNotRunning"):
    if marker not in local_artifact_tests:
        fail(f"local Work transition regression coverage is missing: {marker}")
print("PASS: local-model Work inference is checkpoint-gated; pure-core transitions atomically persist drafts/review state with regression tests")
receipt_source = (ROOT / "core/src/main/kotlin/app/memo/android/data/WorkflowRuntimeReceipt.kt").read_text()
for marker in ("data class WorkflowRuntimeReceipt", "outputSha256", "postconditionStatus", "NOT_VERIFIED", "validateAgainst", "fromEvidence"):
    if marker not in receipt_source:
        fail(f"typed runtime receipt is missing a content-binding or honest-verification contract: {marker}")
for marker in ("WorkflowRuntimeReceipt.create(", "WorkflowRuntimeReceipt.attach(", "receipt.validateAgainst("):
    if marker not in local_artifact_coordinator:
        fail(f"local Work output is not bound to a typed runtime receipt: {marker}")
journal_receipt_source = (ROOT / "core/src/main/kotlin/app/memo/android/data/WorkflowAttemptJournal.kt").read_text()
for marker in ("WorkflowRuntimeReceipt.DETAIL_MARKER", "receiptMarker == receiptMetadata", "receipt.validateAgainst(attempt, item)"):
    if marker not in journal_receipt_source:
        fail(f"workflow journal does not enforce durable runtime receipt integrity: {marker}")

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
for marker in ('"EN" to "English"', '"PL" to "Polski"', '"TR" to "Türkçe"', '"Notifications & reminders"', '"Voice input"', '"TALK / LIVE / WORK / STUDIO"'):
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
for marker in ("onContinueWithMemo", "IntentPlanner.shouldCreateWorkflow", "RecognizerIntent.ACTION_RECOGNIZE_SPEECH", "speechResultLauncher", "Manifest.permission.RECORD_AUDIO", "Make sense of what matters.", "Needs your decision", "sourceMessageId = sourceMessageId"):
    if marker not in home:
        fail(f"Memo-first home is missing product marker: {marker}")
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
for marker in (
    "private fun WorkspaceRouteStrip(",
    "WorkspaceRouteContext(",
    'WorkspaceRouteContext(AppTab.LIVE, "Browser session"',
    'WorkspaceRouteContext(AppTab.WORK, toolSection.label, emptyList())',
    'tab == AppTab.BROWSER -> tab = if (toolSection == ToolSection.BROWSER) AppTab.LIVE else AppTab.WORK',
    'AppTab.BROWSER -> if (toolSection == ToolSection.BROWSER) AppTab.LIVE else AppTab.WORK',
    'AppTab.LIVE -> tab in setOf(AppTab.LIVE, AppTab.APPS, AppTab.APPROVALS) || (tab == AppTab.BROWSER && toolSection == ToolSection.BROWSER)',
    'AppTab.WORK -> tab == AppTab.WORK || (tab == AppTab.BROWSER && toolSection != ToolSection.BROWSER)',
    'ToolSection.entries.filter { it != ToolSection.BROWSER }',
    'WorkspaceRouteContext(AppTab.LIVE, "Phone apps"',
    'WorkspaceRouteContext(AppTab.LIVE, "Approval inbox"',
    "onBack = { openPrimary(context.parent) }",
    "systemReturnTab",
    "fun isPrimarySelected(destination: AppTab): Boolean",
    "val selected = isPrimarySelected(dest)",
    "AutomationAccessibilityService.isExecuting()",
    "if (showStop)",
    "onOpenSettings = { openSystem(AppTab.SETTINGS) }",
):
    if marker not in home:
        fail(f"Memo contextual navigation contract is missing: {marker}")
if "AppTab.MORE" in home or "MoreWorkspaceScreen(" in home:
    fail("Models and Settings must not sit behind a separate competing More workspace")
if 'Text("TALK / LIVE / WORK / STUDIO"' not in home:
    fail("Memo header must name all four connected product surfaces")
live_ui_source = (ROOT / "app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt").read_text()
if "horizontalScroll(rememberScrollState())" not in live_ui_source or live_ui_source.count('Text("Browser session")') != 1:
    fail("Live runtime destinations must be grouped into one compact action row")
view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
for marker in ('sourceConversationId: String = ""', 'sourceConversationId = sourceId'):
    if marker not in view_model:
        fail(f"Work-plan source conversation linkage is missing: {marker}")
for marker in ("Start with Memo", "onInitialPromptConsumed", "onOpenCouncil", "onStartConversation", "onStartConversation: (String, Boolean) -> Boolean"):
    if marker not in chat:
        fail(f"Talk entry/composer is missing: {marker}")
live_route = home.split("AppTab.LIVE -> LiveWorkspaceScreen(", 1)[1].split("AppTab.STUDIO ->", 1)[0]
if "onOpenBrowserWorkflow = { tab = AppTab.BROWSER; toolSection = ToolSection.WORKFLOWS }" not in live_route:
    fail("Live active browser must reopen the existing workflow workspace")
if "liveBrowserWorkflowActionsTranslateInPolishAndTurkish" not in localization_test.read_text():
    fail("Live workflow re-entry must have Polish/Turkish localization regression coverage")
if "workExecutionRoutesTranslateInPolishAndTurkish" not in localization_test.read_text():
    fail("Work execution routes must have Polish/Turkish localization regression coverage")
for marker in (
    "onOpenBrowserWorkflows: () -> Unit = {}",
    "onOpenBrowserTasks: () -> Unit = {}",
    "onOpenEvidenceLibrary: () -> Unit = {}",
    'title = "Execution tools"',
    'label = { Text("Browser workflows") }',
    'label = { Text("Browser tasks") }',
):
    if marker not in chat:
        fail(f"Work execution entry points are missing: {marker}")
if 'ToolSection.BROWSER -> "Browser session"' not in home or 'ToolSection.WORKFLOWS -> "Workflows"' not in home or 'ToolSection.EVIDENCE -> "Evidence"' not in home:
    fail("Browser and Work subroutes need distinct headings so their purpose is visible")
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
vercel_build = (ROOT / "scripts/vercel-build-apk.sh").read_text()
if 'if [[ "$SIGNING_COUNT" -ne 4 ]]' not in vercel_build:
    fail("Vercel APK build must fail closed unless all four persistent Memo signing values are configured")
if 'keytool -list -keystore "$MEMO_UPLOAD_KEYSTORE_FILE"' not in vercel_build:
    fail("Vercel APK build must validate the configured Memo keystore before Gradle")
if "a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a" not in vercel_build:
    fail("Vercel APK build must verify the pinned Memo signing certificate")
if 'SIGNING_DESCRIPTION="ephemeral' in vercel_build or 'if [[ "$SIGNING_COUNT" -eq 4 ]]; then' in vercel_build:
    fail("Vercel APK build must not retain an ephemeral-key publication fallback")
if vercel_build.index("SIGNING_COUNT") > vercel_build.index("Installing checksum-verified Eclipse Temurin"):
    fail("Vercel signing preflight must run before downloading build toolchains")
if vercel_build.index('keytool -list') > vercel_build.index("# Resolve Google's"):
    fail("Vercel keystore validation must run before Android SDK installation")
try:
    subprocess.run(["bash", "-n", str(ROOT / "scripts/vercel-build-apk.sh")], check=True, capture_output=True, text=True)
except subprocess.CalledProcessError as error:
    fail(f"Vercel APK build script has invalid Bash syntax: {error.stderr.strip()}")
if "      - 'BUILD_STATUS.md'" in workflow or "BUILD_STATUS[.]md$" in workflow:
    fail("BUILD_STATUS is generated by CI and must not trigger another CI run")
if "incoming/memo.zip" not in workflow:
    fail("GitHub workflow does not use the canonical incoming/memo.zip source upload")
if "Memo-Android-Repository.zip" in workflow:
    fail("GitHub workflow must not depend on a legacy root-level source ZIP")
for task in (":core:test", ":app:testDebugUnitTest", ":app:lintDebug", ":app:assembleDebug", ":app:assembleDebugAndroidTest"):
    if task not in workflow:
        fail(f"GitHub workflow missing Android gate: {task}")
if any(alternate in workflow for alternate in ("Memo-Android-source.zip", "Memo-Android-Download-Fresh.zip")):
    fail("GitHub workflow must not fall back to an alternate/stale ZIP")
if "Expected exactly one Android Gradle project root" not in workflow or "Memo-Android-debug-APK" not in workflow:
    fail("GitHub workflow is missing canonical-root or artifact-delivery checks")
app_gradle = (ROOT / "app/build.gradle").read_text()
if "applicationId 'app.memo.android'" not in app_gradle or "applicationIdSuffix = memoPreviewPackage ? '.preview' : '.debug'" not in app_gradle:
    fail("Memo debug and preview application IDs must remain deliberately distinct")
if "INTERNAL-DO-NOT-INSTALL-Memo-Android-ci-signing-input" not in workflow:
    fail("The ephemeral CI signing input must be unmistakably internal and not installable")
if workflow.count("INTERNAL-DO-NOT-INSTALL-Memo-Android-ci-signing-input") != 2:
    fail("The internal CI artifact upload and isolated download must use the same protected name")
if "retention-days: 1" not in workflow:
    fail("The ephemeral-key CI signing input must have short retention")
if "a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a" not in workflow:
    fail("The final APK must remain pinned to the persistent Memo signing certificate")

if "sha256sum -c SOURCE_SHA256SUMS.txt" not in workflow or "Memo-Source-Archive-SHA256.txt" not in workflow:
    fail("GitHub workflow is missing source-integrity verification/receipt")
if "build_current_source" not in workflow or "MEMO_BUILD_CURRENT_SOURCE: ${{ inputs.build_current_source }}" not in workflow:
    fail("GitHub workflow must expose an explicit source-only manual APK build input")
if 'if [[ "$GITHUB_EVENT_NAME" == "workflow_dispatch" && "$MEMO_BUILD_CURRENT_SOURCE" == "true" ]]' not in workflow:
    fail("Manual source APK dispatch must be handled explicitly in preflight")
if "Manual source signing is restricted to stabilization/usable-stage-2026-11-10" not in workflow:
    fail("Manual source APK signing must be restricted to the reviewed stabilization branch")
sign_job = workflow.split("\n  sign:", 1)[1].split("\n  commit:", 1)[0] if "\n  sign:" in workflow and "\n  commit:" in workflow else ""
if not sign_job or "if: github.event_name != 'pull_request'" not in sign_job:
    fail("Isolated signing must remain disabled for unreviewed pull-request builds")
if "actions/checkout@" in sign_job or "actions/download-artifact@" not in sign_job:
    fail("Isolated signing must consume only the verified APK artifact and must not check out repository source")

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
for marker in ("app-debug-androidTest.apk", "run_store_acceptance_phase seed", "run_store_acceptance_phase verify", 'adb install -r "$CURRENT_APK"'):
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
checkpoint_source = (ROOT / "core/src/main/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinator.kt").read_text()
checkpoint_tests = (ROOT / "core/src/test/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinatorTest.kt").read_text()
workspace_view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
work_screen = (ROOT / "app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt").read_text()
for marker in ("WorkflowGraph.fromJson", "node.dependsOn.all", "runtime.status == \"AVAILABLE\"", "USER_REPORTED:", "not independently verified", "externalSideEffect", "Legacy jobs without a graph"):
    if marker not in checkpoint_source:
        fail(f"durable workflow checkpoint safety contract is missing: {marker}")
for marker in ("refusesToStartAChildBeforeItsDependenciesAreComplete", "refusesAWorkflowWithExternalEffects", "rechecksRuntimeAvailability", "legacySequentialJobsRemainUsable", "legacyManualJobsCannotStartAnExternalActionShortcut"):
    if marker not in checkpoint_tests:
        fail(f"workflow checkpoint regression coverage is missing: {marker}")
if "WorkflowCheckpointCoordinator.start(" not in workspace_view_model or "WorkflowCheckpointCoordinator.recordUserOutcome(" not in workspace_view_model:
    fail("Work does not persist checkpoint transitions through the workflow coordinator")
if "dependencyReadyStepIds" not in work_screen:
    fail("Work UI does not expose dependency-ready workflow checkpoints")
for marker in ("workflowAttempts = snapshot.workflowAttempts.filter", "val stepAttempts = workflowAttempts.filter", "latestAttempt.status", "MAX_ATTEMPTS_PER_STEP", "Local work generation is running", "onClick = runtime::cancelRun", "partial text is not saved as a completed artifact"):
    if marker not in work_screen:
        fail(f"Work UI does not expose or enforce checkpoint attempt state: {marker}")
attempt_journal = (ROOT / "core/src/main/kotlin/app/memo/android/data/WorkflowAttemptJournal.kt").read_text()
workspace_models = (ROOT / "core/src/main/kotlin/app/memo/android/data/Models.kt").read_text()
live_projector = (ROOT / "core/src/main/kotlin/app/memo/android/data/LiveTimelineProjector.kt").read_text()
live_projector_tests = (ROOT / "core/src/test/kotlin/app/memo/android/data/LiveTimelineProjectorTest.kt").read_text()
for marker in ("fun create(", "fun outputSaved(", "fun failed(", "fun recover(", "fun validate(", "MAX_ATTEMPTS_PER_STEP = 4", "legacyIdempotencyKey"):
    if marker not in attempt_journal:
        fail(f"workflow attempt journal is missing a lifecycle or integrity guard: {marker}")
for marker in ("WorkflowAttemptJournal.create(", "workflowAttempts = snapshot.workflowAttempts + attempt"):
    if marker not in checkpoint_source:
        fail(f"checkpoint start does not persist its attempt atomically: {marker}")
for marker in ("WorkflowAttemptJournal.outputSaved(", "WorkflowAttemptJournal.failed(", "exact checkpoint"):
    if marker not in local_artifact_coordinator:
        fail(f"local Work transition does not close its durable attempt: {marker}")
for marker in ('"schemaVersion", 7', '"workflowAttempts"', "WorkflowAttemptJournal.validate(this)", "WorkflowAttemptJournal.recover("):
    if marker not in workspace_models:
        fail(f"workspace schema does not persist, validate or recover workflow attempts: {marker}")
for marker in ("ATTEMPT(", "snapshot.workflowAttempts.forEach"):
    if marker not in live_projector:
        fail(f"Live timeline does not project durable workflow attempts: {marker}")
for marker in ("WorkflowRuntimeReceipt.fromEvidence(", "Output receipt SHA-256", "postconditionStatus"):
    if marker not in live_projector:
        fail(f"Live timeline does not surface receipt identity and honest verification state: {marker}")
if "liveShowsReceiptFingerprintAndUnverifiedPostconditionWithoutModelPayload" not in live_projector_tests:
    fail("Live timeline receipt visibility and payload privacy regression coverage is missing")
for marker in ("manualRetriesKeepOneIdempotencyIdentityAndStopAtFourAttempts", "canGenerateAnotherDraftWithoutDiscardingThePreviousReviewArtifact", "cannotRecordAUserOutcomeWhileLocalModelInferenceIsStillRunning", "startsAndRecordsAUserReportedCheckpointWithoutClaimingRuntimeVerification", "refusesToStartAChildBeforeItsDependenciesAreComplete"):
    if marker not in checkpoint_tests:
        fail(f"workflow attempt safety regression coverage is missing: {marker}")
for marker in ("OUTPUT_SAVED", "FAILED", "refusesToAttachAnArtifactToAStaleCheckpoint", "refusesAnArtifactBoundToAnotherCheckpointInTheSameJob", "failureTransitionMovesOnlyTheActiveStepToReview"):
    if marker not in local_artifact_tests:
        fail(f"local Work attempt lifecycle regression coverage is missing: {marker}")
print("PASS: workflow checkpoints are dependency-aware, runtime-revalidated, attempt-journaled, external-effect-gated and legacy-compatible")

print("PASS: typed TaskSpec proposals compile into bounded DAGs with exact-runtime capability resolution and fail-closed validation")


print("PASS: CI safely handles canonical ZIP imports and checked-out direct-source builds, verifies source integrity, and runs Android unit tests/lint/APK assembly")

runner_tests = list((ROOT / "remote-runner/tests").glob("test_*.py"))
if not runner_tests:
    fail("no remote runner tests found")
print(f"PASS: remote runner test suite is present ({len(runner_tests)} test module(s)); run it separately with unittest")

local_model_runner = (ROOT / "app/src/main/java/app/memo/android/ai/LocalModelRunner.kt").read_text()
local_model_runner_test = (ROOT / "app/src/test/java/app/memo/android/ai/LocalModelRunnerPolicyTest.kt").read_text()
for marker in ("withLocalInferenceTimeout(INFERENCE_TIMEOUT_MS)", "INFERENCE_TIMEOUT_MS = 180_000L", "conversation?.cancelProcess()", "LocalInferenceTimeoutException(timeoutMs)", "shouldRetryOnCpuAfterGpuFailure(usingGpu, emittedAnyText)"):
    if marker not in local_model_runner:
        fail(f"local inference timeout/cancellation contract is missing: {marker}")
for marker in (
    "localInferenceHasABoundedThreeMinuteDeadline",
    "timeoutIsReportedAsAnInferenceFailureNotAUserCancellation",
    "deadlineCancelsInFlightWorkAndReturnsTheDedicatedTimeoutFailure",
    "successfulInferenceReturnsItsCompleteResult",
    "parentCancellationIsNotConvertedIntoAnInferenceTimeout",
    "gpuFallbackDoesNotReplayAfterPartialOutput",
    "LocalInferenceTimeoutException(LocalModelRunner.INFERENCE_TIMEOUT_MS)",
):
    if marker not in local_model_runner_test:
        fail(f"local inference timeout regression coverage is missing: {marker}")
print("PASS: local inference is time-bounded and cancellation reaches the LiteRT conversation")

runtime_adapter_source = (ROOT / "core/src/main/kotlin/app/memo/android/runtime/WorkflowRuntimeAdapter.kt").read_text()
runtime_adapter_tests = (ROOT / "core/src/test/kotlin/app/memo/android/runtime/WorkflowRuntimeAdapterTest.kt").read_text()
postcondition_source = (ROOT / "core/src/main/kotlin/app/memo/android/runtime/WorkflowPostcondition.kt").read_text()
postcondition_tests = (ROOT / "core/src/test/kotlin/app/memo/android/runtime/WorkflowPostconditionTest.kt").read_text()
for marker in (
    "data class WorkflowRuntimeRequest(",
    "capabilityManifestFingerprint(capabilityManifest)",
    "WorkflowRuntimeAdapterResolver",
    "WorkflowRuntimeDispatcher",
    "remainingTimeMs(now: Long)",
    "WorkflowCancellationSignal",
    "WorkflowRuntimeDeadlineExceededException",
    "MAX_INPUT_CHARS = 64_000",
    "MAX_OUTPUT_CHARS = 256_000",
    "taskSpecSha256",
    "externalSideEffect",
):
    if marker not in runtime_adapter_source:
        fail(f"shared runtime adapter contract is missing: {marker}")
for marker in (
    "interface WorkflowRuntimeAdapter",
    "suspend fun execute(request: WorkflowRuntimeRequest)",
    "The selected runtime adapter does not implement every required TaskSpec capability",
    "Adapters return output; they do not persist workspace state",
):
    if marker not in runtime_adapter_source:
        fail(f"runtime adapter boundary is missing: {marker}")
for marker in (
    "interface WorkflowPostconditionObserver",
    "WorkflowPostconditionEvidence",
    "WorkflowPostconditionMethod",
    "WorkflowPostconditionOutcome",
    "WorkflowRuntimeReceipt.OUTPUT_SAVED",
    "outputEvidenceId",
    "postconditionSha256",
    "permitsCheckpointCompletion",
    "registeredObservers",
):
    if marker not in postcondition_source:
        fail(f"deterministic postcondition evidence contract is missing: {marker}")
for marker in (
    "requestBindsThePersistedAttemptToExactRuntimeAndCompiledCapabilities",
    "resolverSelectsOnlyTheAdapterForTheExactRuntime",
    "requestRejectsRuntimeAvailabilityOrCapabilityPolicyDrift",
    "approvalGatedTasksCannotBypassTheDedicatedApprovalFlow",
    "runtimeDeadlineAndCancellationAreEnforcedByTheSharedRequest",
    "runtimeOutputIsBoundedAndDoesNotClaimPostconditionVerification",
    "dispatcherRunsOnlyTheResolvedAdapterAndValidatesItsOutput",
    "dispatcherRejectsExpiredRequestBeforeInvokingAdapter",
    "dispatcherRechecksCancellationAfterAdapterReturns",
    "dispatcherRejectsAdapterResultOutsideTheExactAttemptDeadline",
    "remainingTimeUsesThePersistedAttemptDeadlineAndRejectsExpiredWork",
):
    if marker not in runtime_adapter_tests:
        fail(f"runtime adapter contract regression coverage is missing: {marker}")
for marker in (
    "postconditionRequestRequiresAValidatedSavedOutputAndRegisteredObserver",
    "postconditionEvidenceIsBoundToTheExactTaskAttemptAndObserver",
    "postconditionObserverMustBeIndependentAndReadOnly",
    "nonVerifiedOutcomesNeverPermitCheckpointCompletion",
):
    if marker not in postcondition_tests:
        fail(f"postcondition evidence regression coverage is missing: {marker}")
print("PASS: shared runtime requests are manifest-bound, approval-gated, time-bounded and covered by pure-core tests")
print("PASS: postcondition evidence requires a registered independent deterministic observer and exact saved-attempt binding")

proposal_adapter_source = (ROOT / "core/src/main/kotlin/app/memo/android/runtime/EntityProposalRuntimeAdapter.kt").read_text()
proposal_coordinator_source = (ROOT / "core/src/main/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinator.kt").read_text()
proposal_workflow_tests = (ROOT / "core/src/test/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinatorTest.kt").read_text()
proposal_view_model = (ROOT / "app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt").read_text()
for marker in (
    'override val runtimeId: String = "app.entities"',
    'override val capabilityIds: Set<String> = setOf("entity.propose")',
    'const val CONTENT_TYPE = "application/vnd.memo.entity-proposals+json"',
    'MAX_PROPOSALS = 12',
    'request.validate(registeredRuntimes)',
    'request.checkActive(System.currentTimeMillis())',
    'WorkflowRuntimeResult.Output(',
):
    if marker not in proposal_adapter_source:
        fail(f"deterministic entity-proposal adapter is missing its bounded runtime contract: {marker}")
for marker in (
    'WorkflowParticipant.DETERMINISTIC',
    'requiredCapabilities = listOf("entity.propose")',
    'EntityProposalRuntimeCodec.encodeInput(',
    'kind = "WORKFLOW_INPUT"',
    'inputEvidenceId = inputEvidenceId',
    'fun restorePrepared(',
    'EntityProposalRuntimeCodec.parseInput(inputEvidence.content)',
    'inputMetadata.optString("inputSha256") == sha256(inputEvidence.content)',
    'normalize(message.content).contains(normalizedExcerpt)',
    'LocalWorkArtifactCoordinator.saveForReview(',
    'WorkflowRuntimeReceipt.fromEvidence(',
    'WorkflowPostconditionRequest.create(',
    'WorkflowPostconditionOutcome.VERIFIED',
    'kind = "WORKFLOW_VERIFICATION"',
    'WorkflowAttemptJournal.validate(it)',
    'reviewStatus = "PROPOSED"',
):
    if marker not in proposal_coordinator_source:
        fail(f"deterministic entity-proposal workflow is missing its persistence/verification guard: {marker}")
for marker in (
    'EntityProposalWorkflowCoordinator.prepare(',
    'WorkflowRuntimeDispatcher.dispatch(',
    'request.remainingTimeMs(nowMillis())',
    'withTimeout(remainingTimeoutMs)',
    'persistAndAwait()',
    'EntityProposalPostconditionObserver(_snapshot.value).observe(',
    'recordOutcomeProposalProgress(',
):
    if marker not in proposal_view_model:
        fail(f"Talk-to-Work deterministic runtime wiring is missing: {marker}")
if "saveOutcomeProposalsThroughRuntime(jobId, current.id, proposals)" not in chat:
    fail("Talk outcome extraction still bypasses the deterministic entity-proposal runtime")
for marker in (
    "executesEntityProposalThroughExactRuntimeAndIndependentReadAfterWriteVerification",
    "restoresTheExactEncryptedInputAfterWorkspaceRecreation",
    "refusesToRestoreWhenTheExactEncryptedInputEvidenceIsMissing",
    "repeatedIdenticalProposalBatchesGetDistinctDurableInputRecords",
    "rejectsEvidenceThatCannotBeLinkedToOneOriginalConversationMessage",
    "skipsExistingDuplicateAndPersistsOnlyTheNewProposals",
    "missingPersistedProposalCannotPassTheIndependentPostcondition",
):
    if marker not in proposal_workflow_tests:
        fail(f"deterministic entity-proposal workflow regression coverage is missing: {marker}")
print("PASS: Talk outcome proposals use the deterministic app.entities adapter, durable journal and separate read-after-write verification")

print("NOTE: This script does not compile Kotlin, run Gradle, prove Android runtime behavior, or verify a device.")
