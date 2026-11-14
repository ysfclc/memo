# Memo — workflow stabilization progress

**Date:** 13 November 2026  
**Repository:** `ysfclc/memo`  
**Branch:** `stabilization/usable-stage-2026-11-10`  
**Tracking PR:** [#8](https://github.com/ysfclc/memo/pull/8), draft/unmerged  
**Execution plan:** [MEMO_WORKFLOW_RECOVERY_EXECUTION_PLAN_2026-11-13.md](MEMO_WORKFLOW_RECOVERY_EXECUTION_PLAN_2026-11-13.md)  
**Requirements:** [MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md](MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md)

## Update 1 — Plan and first recovery slice

### Baseline
- Starting source head: `3abb7d728107d2ce1fed45fa58ecc59c88fe0a7f`.
- Execution-plan documentation commit: `1ae37aeb7cafe0f2c80d5bcd55d4119d3426b6c5`.
- Existing branch and draft PR #8 retained; `main` is not targeted.
- No deployment, merge, release, signing change or data migration performed.

### Source findings that drive the order
- Browser workflows have their own route/state, separate from Council.
- Saved `BROWSER_WORKFLOW` entities are definitions, not run history.
- Automatic answer capture depends on provider-specific DOM selectors and stability polling.
- The waiting state can retain `sentForCurrentStep=true`, but the inspected UI offered only a continue-polling action and whole-workflow Stop. There was no manual answer capture control.
- The existing response completion callback already persists response evidence, transcript message and a `FRONTIER_RESPONSE` entity. Reusing it avoids a competing persistence path.
- The current prompt approval dialog displays the payload but does not expose a complete edit/revision checkpoint.
- The full browser route can take the user away from the task controls.

### WF-01 — manual response capture

**Implementation started in this increment:**
- Added a pure recovery policy requiring: `WAITING_FOR_USER`, a confirmed sent prompt, a matching provider page, and at least 8 non-whitespace response characters.
- Added `BrowserSession.captureCurrentResponseManually`. It calls the existing completion/persistence path with `USER_PASTED`; it does not call prompt preparation or the send script.
- Added a paste-answer field and explicit “Capture answer without resending” control in the existing workflow screen.
- Added `captureMethod` provenance to the response model, response entity properties and evidence metadata.
- Made step/evidence descriptions distinguish automatic detection from user-pasted capture.
- Added pure policy tests for accepted capture and rejection of unsent/uncertain prompts, wrong provider pages, wrong workflow state and short text.
- Updated `TASKS.md` to link the execution plan and list WF-01 through WF-13 as open tasks.

**Status:** source patch committed as `b98053bb015c90b6622dbb9cf225d545b79bfe5c`; not verified. WF-01 remains unchecked until the source verifier, relevant JVM test and device behavior pass.

### Verification
- Source inspection and exact-anchor patch validation: completed.
- Commit: `b98053bb015c90b6622dbb9cf225d545b79bfe5c` (`fix(browser): add manual response capture recovery`).
- GitHub read-back checks confirmed the recovery policy, capture-only call path, UI action, capture provenance and regression-test declarations are present.
- Follow-up integrity commit: `e77551825ff024a190a085abfb49299f38b9400e` added source-verifier guards for the manual-capture policy, capture-only path, provider binding, provenance and regression tests.
- `SOURCE_SHA256SUMS.txt` is being refreshed for all changed tracked sources, tests, verifier and TASKS entries; the final checksum values are validated against the fetched file contents.
- Automated tests: **not run**.
- Exact-head GitHub Actions run #606 (`43365637627`) failed in the incoming ZIP preflight before runner assignment; the preflight job has no steps/logs and all downstream validation/build/signing jobs were skipped. This is an executor/import failure, not a source test result.
- Gradle compile/test, lint and APK assembly: **not run**.
- Android emulator/physical-device reproduction: **not run**.
- Provider-specific behavior: **not verified**.
- No crash-free or working-app claim is made.

### Blockers and risks
- GitHub Actions runner/preflight has repeatedly failed before runner assignment (`runner_id=0`, no steps); exact-head Gradle/JVM/lint/APK verification remains blocked.
- Vercel previously returned HTTP 402 for the free daily deployment limit. No new deployment attempt is claimed.
- No physical Android device/emulator was available to this session.
- Manual capture is intentionally restricted to a confirmed submitted step on the matching provider host. It cannot safely resolve a send whose submission is still uncertain.
- A user-pasted answer is attributed to the current provider page and timestamp; the app cannot cryptographically prove that pasted text came from the page. UI wording must remain honest about that.

### Next
1. Validate the full source-manifest checksum set against the latest committed file contents.
2. Run `tools/verify_source.py` and the focused `BrowserWorkflowPromptsTest` JVM suite when an executor is available.
3. Reproduce the capture, retry, skip, uncertain-send and stop flows on an Android device.
4. Continue WF-04: verify response persistence before declaring a step or job successful.
5. Keep source status separate from compile, test and device verification.


## Update 2 — WF-02 explicit detection retry

**Source increment:** committed in this update; WF-02 remains open pending execution evidence.

- Added a separate `retryCurrentResponseDetection()` action. It is allowed only when the workflow is waiting for user recovery and the current provider step is known to have been sent.
- The method changes the state to response checking and invokes only `pollWorkflowResponse`. It does not call prompt preparation, approval, or the provider send script.
- The previous general Continue path now delegates its already-sent branch to the same capture-only retry method.
- The UI now labels the control **“Retry answer detection · no resend”** and explains that it checks only the current provider page.
- Added pure policy tests for valid and invalid retry states.
- Extended `tools/verify_source.py` to assert that retry polls only and contains no send-script call; it also checks that the UI exposes the distinct action.
- Updated `TASKS.md` and the source checksum manifest for the changed tracked files.

**Verification status:** source/read-back checks only. The policy tests, source verifier, Gradle tests, lint and APK build have not run. WF-02 is not marked complete.

**Next:** WF-03 — implement distinct Skip provider / Stop current step / Cancel entire run semantics while preserving captured outputs.


### WF-02 follow-up hardening

- Follow-up source review caught and corrected a missing generation local in the general Continue path after extracting the capture-only retry method.
- Both manual capture and retry now explicitly refuse calls after the browser session is disposed.
- This is a source-level correction only; it has not been compiled or run. The branch's current Actions runner blocker still prevents execution.


## Update 3 — WF-03 step-level recovery controls

**Source commits:** `b336ff1f7d0f3b6da282fd1eea9112023df4e100` and `54e373a44ed5b7bd49b40f5fac6c5e432aebea9e`; approval-stage skip correction: `c720df39510e7c1305061c80a96e470ebd58d966`.

- Added separate state-gated operations for **Skip this provider** (only before submission) and **Stop this provider step & continue** (only after a confirmed submission while waiting for a response/recovery).
- Both actions advance only the current provider step and preserve earlier captured answers. A stopped submitted step is marked `NEEDS_REVIEW`; a pre-send skip is marked `SKIPPED`.
- Added separate callbacks so the job-step ledger records the correct terminal/review state.
- Added skipped/stopped provider IDs to workflow state and consultation entity properties. Partial runs are explicitly described and are not reported as fully successful; a run with no captured answers remains `WAITING_FOR_USER`.
- The whole-run control is now labelled **Cancel entire run** and remains separate.
- UI explicitly warns that stopping Memo's step cannot cancel generation already running on the provider's remote website.
- Added policy tests and source-verifier guards for the scope distinctions.

**Verification:** source/read-back review only; no JVM tests, Gradle compile, lint, APK build or device run executed. WF-03 remains open pending verification.

**Next:** review the state transition and persistence path for partial runs, then WF-04 truthful completion/persistence verification.


### WF-03 safety correction — ambiguous send state

A source review found an important edge case: a provider send script can return without confirming a click. Memo must not label that state “not sent” and offer a normal skip. The state now records `submissionUncertain`; Skip is disabled, and the only step-level continuation is Stop this provider step, which marks the job step `NEEDS_REVIEW`. Manual send verification failures also retain uncertainty instead of automatically preparing another send. This prevents a duplicate submission from being hidden behind a “skip” label.

This correction is source-only and still awaits compilation, unit tests and device verification.


### WF-03 approval-stage skip correction

The first step-level version hid Skip while the exact-prompt approval dialog was open. Commit `c720df39510e7c1305061c80a96e470ebd58d966` makes Skip available at that checkpoint by rejecting the matching still-PENDING approval record before marking the provider step SKIPPED and advancing. This allows the remaining providers to continue instead of forcing the user to cancel the entire run. Skip remains blocked for any submitted or uncertain send; those states require Stop this provider step and leave the step in Needs Review.


### Verification update — exact-head CI

Exact-head Actions run #616 (`43366806207`) for the preceding step-recovery commit again failed in the incoming ZIP preflight before a runner was assigned; the validation/build/signing jobs were skipped. This is not a source compile result. The current follow-up commits have not been built or tested.


### Follow-up commits and latest executor result

- `7856b1e48afaf07b570de79f9c90279e1d9d0cb2` — corrected source-verifier coverage for approval-stage skip and updated the runbook/progress.
- `2d349f79810f13cbf2e6ada0c88870c0df6ffe79` — refreshed tracked source SHA-256 entries after the workflow changes.
- Exact-head Actions run #621 (`43367294009`) failed in the same incoming ZIP preflight before runner assignment; no source tests, Gradle build, lint or APK steps ran.


### Latest source review and CI result

- `7d22b8a360a4ab602b75da07a77fb1ee8dabce69` adds the explicit `SkipNext` icon import and records the approval-stage skip correction.
- Exact-head Actions run #623 (`43367418437`) failed in the incoming ZIP preflight before runner assignment. No Gradle compile, JVM test, lint or APK job ran.
- The committed SHA-256 manifest was read back and its entries for the six changed tracked source/test/runbook files were compared against a SHA-256 implementation that passed the standard `abc` test vector. All six entries matched.
- Source read-back confirms the manual capture action is capture-only, detection retry only polls, uncertain submission blocks Skip, pending approval is rejected before a safe Skip, and Stop-step / Cancel-run controls are distinct. These are source checks, not runtime verification.


### Follow-up source review and CI

- `7d22b8a360a4ab602b75da07a77fb1ee8dabce69` adds the explicit `SkipNext` icon import and records the approval-stage skip correction.
- Exact-head Actions run #623 (`43367418437`) failed in the incoming ZIP preflight before runner assignment. No Gradle compile, JVM test, lint or APK job ran.
- Source read-back confirmed the manual capture action is capture-only, detection retry only polls, uncertain submission blocks Skip, pending approval is rejected before safe Skip, and Stop-step / Cancel-run controls are distinct.
- These are source checks only, not runtime verification. WF-01 through WF-03 remain open pending executable tests and Android device acceptance.

### Next engineering task — WF-04

Inspect and correct the response persistence boundary. Current source marks the provider job step `COMPLETED` before transcript, evidence, response-entity and action-ledger writes are all checked. `addEvidence` returns no result, so the caller cannot tell whether the evidence-limit or vault-readiness guard prevented insertion. The next patch must make each write outcome observable, mark incomplete captures `NEEDS_REVIEW`, and avoid claiming durable disk persistence from queue acceptance alone.


### Source-verifier self-review

A follow-up review found the new persistence-verifier guard referenced `view_model` without loading that source file. The verifier now explicitly reads `MemoWorkspaceViewModel.kt` before checking the evidence-persistence contract. This is a verifier-only correction; the WF-04 application patch has not yet been committed.


## 13 November 2026 — APK freshness and data-loss audit

- Added `docs/handoff/MEMO_APK_FRESHNESS_AND_DATA_LOSS_AUDIT_2026-11-13.md` with the three distinct package identities, signing behavior, Keystore/data lifecycle, current executor blockers, last confirmed artifact metadata, and ordered acceptance gates.
- Confirmed the latest stabilization source head `d6826ea6e30dd0eea23de04f0e3e0e9718e85742` has no successful build. Actions run #629 (`43368087849`) failed in preflight before a runner step; build/signing jobs did not execute. Vercel also reported the free daily deployment quota (HTTP 402); no current-head deployment exists.
- Retrieved and inspected the last confirmed signed artifact from 8 November, run `42530596381`: package `app.memo.android.debug`, version code 223, pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`, APK SHA-256 `670acd9192f4b02e7aea1c23e456036b2ad3a98f5d48d57ef7c895a72e8d22b2`. It is not the current candidate.
- Source audit found the Vercel preview build uses package `app.memo.android.preview` and can continue with an ephemeral debug key when pinned credentials are absent; it only checks the pinned fingerprint when all credentials are supplied. This must be made fail-closed before publishing any candidate.
- The exact APK installed during the user's reported data-loss incident is not yet identified; uninstall/clear-data versus in-place update and package/signature continuity remain to be established. No single device-level cause is claimed.
- No Kotlin/Gradle/source change or new APK is claimed in this audit. The build executor is still blocked. PR #8 remains draft/unmerged.
- **Backlog continuity:** discovery of new bugs does not defer, remove, downgrade, or close previously reported work. WF-01 through WF-12 and the wider TASKS.md milestones remain active; implementation and verification status must stay separate. Continue in priority order as execution becomes available.


## 14 November 2026 — APK delivery signing guard

- **Task:** P0 APK freshness/data-loss audit, release rejection rule; WF-13 release gate remains open.
- **Branch:** `stabilization/usable-stage-2026-11-10`.
- **Change:** `scripts/vercel-build-apk.sh` now requires all four protected Memo upload-signing values before downloading JDK/Android SDK or invoking Gradle. It validates the keystore alias/password before building, uses the pinned certificate only, removes the ephemeral debug-key fallback, and unconditionally verifies the resulting APK against pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`. The temporary keystore is removed on exit.
- **Guard:** `tools/verify_source.py` now checks that Vercel signing is mandatory, keystore validation precedes Gradle, the pinned certificate check exists, and no ephemeral publication fallback is present.
- **Why:** the prior Vercel path could publish a side-by-side `app.memo.android.preview` APK signed with an ephemeral key when protected credentials were absent. It was not an update-compatible Memo APK and could encourage a destructive uninstall/replacement. Vercel project credentials were previously absent; deployment is intentionally blocked until the pinned signing secrets are provisioned.
- **Not verified:** source verifier, GitHub Actions exact-head run, Gradle tests/lint/APK, pinned-signature output, emulator upgrade, and physical-device update. No APK is published by this change.
- **Next:** inspect exact-head Actions result. Keep PR #8 draft. Resume WF-04 only after this P0 delivery guard and build executor are verified; WF-01–WF-03 source work remains unaccepted until executable/device evidence.


## 14 November 2026 — GitHub Actions run #649: source and encrypted-store acceptance

- **Exact source commit:** `e7bc8b8f4452ac5b34fe5cb3a040678e7e8cd8c2` on `stabilization/usable-stage-2026-11-10`.
- **Workflow:** https://github.com/ysfclc/memo/actions/runs/43643456778
- **Runner:** assigned successfully; no pre-runner ZIP/preflight failure.
- **Passed:** source invariants (including the Vercel signing fail-closed and Bash syntax guards); remote-runner tests; 144 core JVM tests; 37 app JVM tests; Android lint; debug APK assembly; APK package/version/signature validation.
- **Build identity:** package `app.memo.android.debug`; versionCode `649`; versionName `4.0.0-memo-debug`. The build-stage APK uses an ephemeral CI debug key and is an intermediate only.
- **API 35 emulator:** passed both encrypted-store instrumentation phases (`OK (1 test)` each). The baseline APK seeded workspace and native workflow stores; the new APK installed in place; both stores reopened; legacy recovery and corruption checks passed. Exact log: `PASS: signed in-place upgrade preserved app-private data and both real encrypted Memo stores; legacy recovery and corruption tests passed.`
- **Reports:** `Memo-Android-Build-Reports` artifact ID `14132399597`, SHA-256 `c58dcbf9dc25e2ac6284c8dd6c7e5c6f1176465133fc5235e29065d203e751e3`.
- **Important:** this run was a pull-request validation. The isolated pinned-signing job and APK delivery artifacts were intentionally skipped. Do not install the intermediate CI-debug APK.
- **Follow-up implemented:** `.github/workflows/build-apk.yml` now has an explicit `workflow_dispatch` boolean `build_current_source`, restricted to the stabilization branch, which runs the exact committed source through tests and the isolated pinned-signing job. `docs/ci/GITHUB_ACTIONS_BUILD.md` documents the one-click path. The source verifier protects the dispatch branch restriction and ensures the signer remains artifact-only/no-checkout.
- **Still pending:** a manual workflow dispatch with `build_current_source=true`, final pinned-certificate APK artifact verification, and physical-device in-place upgrade against the user's actual installed package/certificate. Do not uninstall Memo or clear app data.


## 14 November 2026 — WF-04 durable browser-response acknowledgement

- **Starting head:** `f80873299fc246c49be7f281999fd4acc0f08fa3`.
- **Task:** WF-04, with regression protection for WF-01/WF-02.
- **Source finding:** the previous response callback advanced the browser to the next provider immediately, while evidence, transcript, action completion and response-entity creation were separate writes. The new coordinator/ViewModel transaction from the prior increment was not yet connected to this live callback.
- **Change:** browser workflow now enters `SAVING_RESPONSE` after detecting or manually receiving a provider answer. It waits for a ViewModel-scoped encrypted-persistence acknowledgement before adding that response to workflow results or navigating to another provider. A failed save returns to user recovery with the original submitted state intact; the prompt is never resent. The legacy multi-write response callback was removed in favor of the atomic coordinator.
- **Safety:** a provider step advances only when the result contains a non-empty evidence ID and the transaction did not return `FAILED`. A saved response that still needs action/entity review is retained and may be followed by the next provider, while the individual step remains `NEEDS_REVIEW`.
- **Tests/guards:** added a JVM state test for the saving state; extended `tools/verify_source.py` to require the acknowledgement gate, the coordinator transaction tests, and absence of the old multi-write path.
- **Exact files:** `BrowserWorkflow.kt`, `BrowserSession.kt`, `BrowserWorkflowScreen.kt`, `MemoWorkspaceViewModel.kt`, `BrowserWorkflowPromptsTest.kt`, `FrontierResponseCaptureCoordinator.kt`, `FrontierResponseCaptureCoordinatorTest.kt`, `tools/verify_source.py`, `TASKS.md`, this progress log and `SOURCE_SHA256SUMS.txt`.
- **Verification at commit time:** source-anchor validation only. No Gradle tests, lint, APK build, emulator or physical-device test is claimed until the exact resulting commit's GitHub run completes.
- **Next:** run exact-head CI; fix any source/test failures before beginning WF-05 or UI expansion. Keep PR #8 draft and `main` untouched.
