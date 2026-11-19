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


### WF-04 verifier correction — 14 November 2026

- Exact-head push run #668 (43651535178) reached source verification and failed one stale assertion: the verifier expected capture-method metadata to be written in the Compose screen.
- The implementation intentionally moved that metadata into FrontierResponseCaptureCoordinator so the evidence, transcript, entity and step are committed through one transaction.
- Corrected the verifier to assert the metadata at its new authoritative location. This was a test-guard mismatch; the run stopped before Gradle compilation, JVM tests, lint or APK assembly, so those gates remain unverified.
- Next: rerun the full exact-head pipeline after this verifier-only correction.


### WF-04 verifier follow-up — 14 November 2026

- Exact-head push run #669 (43651708677) passed the corrected provenance assertion and then found a second stale assertion: the approval UI marker list also expected the new ViewModel capture method to be present in the approval-dialog section.
- The capture method is correctly checked in the dedicated WF-04 integration guard later in the verifier. Removed the duplicate/misplaced assertion from the older approval-only marker list; no application code change was needed.
- This run stopped during source verification, before Gradle tests, lint, APK assembly or emulator validation. Those remain pending.
- Next: rerun exact-head CI and proceed only from the first complete green run.


### Branch-tree restoration — 14 November 2026

- A verifier-only follow-up commit was accidentally created from the previous base tree rather than the immediately preceding application commit. CI caught that the branch no longer contained the SAVING_RESPONSE application integration.
- Restored the six affected application/test/task files exactly from known commit 77f2a6c86553318e626ae6bc8167decb2cfbe0f7 while preserving the corrected verifier and progress history already present at the branch head.
- This restoration commit is a source-integrity correction. The next exact-head CI run must prove the complete tree, including the coordinator and its caller, before further work proceeds.


## 14 November 2026 — WF-04 exact-head CI and pinned APK acceptance

- **Exact tested source commit:** `672144a391e04ae33e487064b717932ee9ebf146`.
- **GitHub Actions:** run #674, ID `43652062415`, completed successfully: https://github.com/ysfclc/memo/actions/runs/43652062415.
- **Source gates:** 52 required integration files, platform-independent core isolation, exact-content approval, manual capture/retry/skip/stop controls, atomic response-capture transaction and acknowledgement gate all passed the source verifier. Remote-runner tests passed.
- **Android gates:** `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest` and APK identity/signature verification passed. Gradle reported `BUILD SUCCESSFUL in 4m 57s`.
- **APK identity:** package `app.memo.android.debug`; versionCode `674`; versionName `4.0.0-memo-debug`.
- **API 35 instrumentation:** baseline installed, encrypted workspace/native stores seeded, updated APK installed in place, both stores reopened; legacy recovery and corruption checks passed. Final log: `PASS: signed in-place upgrade preserved app-private data and both real encrypted Memo stores; legacy recovery and corruption tests passed.`
- **Pinned signing:** isolated signing job passed and verified certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Artifacts:** final signed APK `Memo-Android-debug-APK`, ID `14136712960`, artifact ZIP SHA-256 `59dd1a6a7a7240f08cf5feb056fb8c6d9a82f9d03a839eda2950d74fa1a9083d`; integrated repository ID `14136667838`; build reports ID `14136219292`.
- **Status:** source/build/signing gates for WF-04 passed. WF-04 remains open only for live provider/device acceptance; WF-05 and STAB-05 remain open for physical-device reproduction and upgrade checks. PR #8 remains draft; `main` was not modified.


## 15 November 2026 — ORCH-007 shared runtime and postcondition contracts

- **User direction:** resume Memo development one coherent increment at a time while preserving the complete product contract, social platform scope, prior bugs and existing backlog. No merge/release/deployment/data-migration authorization.
- **Branch/source:** `stabilization/usable-stage-2026-11-10`; exact source commit `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34`.
- **Change:** added shared runtime invocation/resolution contracts and deterministic postcondition observer/evidence contracts. Runtime requests validate exact TaskSpec, active attempt, compiled capability-manifest fingerprint, current runtime/capability registry, approval/side-effect boundary, bounded payloads, deadline and cancellation.
- **Verification rule:** adapters return candidate output only. A separately registered read-only observer must prove the exact declared postcondition with evidence tied to the attempt and task fingerprint. Only `VERIFIED` permits checkpoint completion. Model output is never proof.
- **Tests:** new `WorkflowRuntimeAdapterTest` and `WorkflowPostconditionTest`; source verifier guards the contract and tests.
- **GitHub Actions run #678:** ID `43666486553`, exact-head success: https://github.com/ysfclc/memo/actions/runs/43666486553. Source invariants, remote runner, core/app unit tests, lint, APK assembly, APK identity/signature, API 35 in-place encrypted-store upgrade and isolated pinned signing all passed.
- **Build:** `BUILD SUCCESSFUL in 4m 49s`; package `app.memo.android.debug`, versionCode `678`, versionName `4.0.0-memo-debug`.
- **APK:** artifact `Memo-Android-debug-APK`, ID `14143185526`; APK SHA-256 `026a9be7743e1e91cbc3b7807bed19504db6ea93b3d9249c91046ea15100f469`; pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Upgrade proof:** API 35 emulator installed the new signed APK over the existing package, reopened both real encrypted stores, and passed legacy recovery/corruption checks. No uninstall or clear-data step.
- **Not claimed:** no generic runtime dispatcher or concrete adapter is wired; no browser/native observer implementation; no physical-device acceptance. The user installed #674 but has not checked it.
- **Next:** deterministic app-runtime adapter through the checkpoint journal with shared timeout/cancellation and process-death recovery. Browser/native adapters remain later increments.


## 17 November 2026 — shared UI primitives and editable provider prompt

- **Working branch:** `stabilization/usable-stage-2026-11-10`; PR #8 remains draft/unmerged.
- **UI cleanup commit:** `49fa0900fdca7ae9b28a32d13d408d4de9528ac0`. Screen call sites were moved to the existing `MemoPanel`, `MemoSectionHeading` and `MemoStatusTag`; duplicate panel/heading implementations were removed while unique empty-state, dot-label and domain status helpers were retained.
- **UI cleanup verification:** exact-head CI runs [#44012466706](https://github.com/ysfclc/memo/actions/runs/44012466706) and [#44012460788](https://github.com/ysfclc/memo/actions/runs/44012460788) both passed. This verifies the shared-primitive consolidation compiles and passes the build workflow on that exact commit.
- **Prompt editing commit:** `2ee5f8adce415828a9a1900e9621bcedbda64acd`. Added a pre-submission editor showing original intent and prepared provider prompt, exact-hash revision, invalidation of the previous pending approval, fresh approval proposal, and reject-to-edit behavior. A rejected prompt no longer cancels the entire consultation. Approval-gate failures after execution begins still stop into review.
- **Regression tests:** added eligibility tests for pre-submission editing, uncertain/sent states, blank prompts and the 7,900-character provider prompt bound.
- **First exact-head validation attempt:** [run #44014394039](https://github.com/ysfclc/memo/actions/runs/44014394039) and duplicate [run #44014387122](https://github.com/ysfclc/memo/actions/runs/44014387122) reached source verification but failed one stale guard that expected the old literal dialog label `Reject and stop`. No Gradle unit tests, lint or APK assembly ran on commit `2ee5f8a`. The guard is being updated to require the new reject-to-edit and exact-revision invariants; the failed run is not treated as a code compilation result.
- **Next:** rerun exact-head source verification, core/app tests, lint and APK build for the corrected verifier. Do not claim physical-device acceptance until the user journey is exercised on the actual phone.


## Update 4 — prompt revision, design-system cleanup and exact-head acceptance (17 November 2026)

### Source changes
- Design-system cleanup commit `49fa0900fdca7ae9b28a32d13d408d4de9528ac0`: five screens migrated to canonical Memo primitives; duplicate panel/heading definitions deleted; unique helpers preserved. Exact-head runs #44012466706 and #44012460788 passed on that cleanup commit.
- Prompt-editing commit `2ee5f8adce415828a9a1900e9621bcedbda64acd`: pre-submission prompt editing and exact-hash approval revision added.
- Follow-up verifier commits `2a7186ceec0dacdad97880866cc0921f99558251` and `ad364b3ed62a93f534cdee63f3486142cdde2b6c`: guards strengthened to enforce reject-to-edit, approval supersession, no send from edit and workflow-policy regression tests; a verifier initialization-order error was corrected.
- Prompt/UX follow-up commit `b428bca63c09ed3ed7240532f2f5bbdf12cd61dd`: Polish/Turkish prompt UI translations added and whole-run cancel made available inside the modal approval checkpoint.
- Final integration fix `090a2de99dacbc65cfcc0351be9333bbf00aa182`: imported `BrowserWorkflowRecoveryPolicy` into the screen and removed a repeated provider label.

### Validation history and corrections
- The first prompt-edit attempt failed a source guard because the dialog title had been changed. The stable title was retained and the provider name moved into dialog content.
- The next source-verifier iteration exposed variable initialization order; corrected before proceeding.
- The next source check caught untranslated prompt-editor strings; Polish/Turkish translations were added rather than suppressing the localization check.
- The first Android compile then found the missing `BrowserWorkflowRecoveryPolicy` import. The import was added; the repeated provider label was removed in the same source fix.
- These failed attempts did not pass app compilation and are not acceptance evidence. The final exact-head run below supersedes them.

### Exact-head verification
- **Commit:** `090a2de99dacbc65cfcc0351be9333bbf00aa182`.
- **GitHub Actions run #802:** [44016406556](https://github.com/ysfclc/memo/actions/runs/44016406556) — completed successfully.
- Source verification and static localization coverage: PASS.
- Remote runner tests: PASS.
- Gradle: `BUILD SUCCESSFUL`; `:core:test`, `:app:testDebugUnitTest`, lint, debug APK and Android-test APK assembly: PASS.
- APK identity and signature verification: PASS.
- API 35 in-place upgrade/data-preservation: PASS. The emulator installed a higher-version APK over the baseline, verified app-private sentinel and both encrypted Memo stores, and passed legacy recovery/corruption tests.
- Isolated pinned signing: PASS; certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- Signed APK artifact: [Memo-Android-debug-APK, artifact 14298479094](https://github.com/ysfclc/memo/actions/runs/44016406556/artifacts/14298479094).
- Package `app.memo.android.debug`; versionCode `802`; versionName `4.0.0-memo-debug`.
- APK SHA-256: `69345f8b680feb0b7cbb59d83161561693e3f1d3f079fe1d52c288c82b224db3`.

### Acceptance boundary and next
- The shared primitives and prompt editor are source-, JVM-, lint-, APK-, emulator-upgrade- and signing-verified on this exact commit.
- Physical-device UX and real signed-in provider interaction have not been exercised. WF-01–WF-13 and the full prompt-to-result journey remain OPEN / NOT DEVICE-ACCEPTED.
- No merge to `main`, production release/deployment, signing identity change or destructive migration occurred.
- Next increment: task-attached browser runtime/Live presentation, only after this verified handoff and preserving the current four-surface design system.


## Update 5 — Live browser workflow re-entry (17 November 2026)

- Live's active browser session card now routes to the existing `AppTab.BROWSER / ToolSection.WORKFLOWS` screen, not the generic browser. This restores access to the task prompt, approval checkpoint, browser pane and recovery controls without creating another session or WebView.
- The ordinary Browser session action at the top of Live remains unchanged.
- The active card labels its action “Review prompt and controls” while prompt approval is pending, and “Open workflow controls” otherwise. Both are included in Polish/Turkish localization and unit tests.
- The source verifier guards the dedicated callback, state-aware action and destination route.
- PR validation is limited to opened/reopened events; push validation handles subsequent commits and the signed APK stage, avoiding duplicate PR/push builds.
- Exact-head CI and physical-device navigation acceptance pending.


## Update 6 — Shared runtime dispatcher (17 November 2026)

- **Work package:** M3.8.A7 / MEMO-ORCH-001 — extract generic adapter invocation from the first deterministic app capability.
- **User outcome:** future Memo-owned, browser and native capabilities can share one validated invocation boundary rather than each implementing a different runtime-selection path.
- **Changed:** WorkflowRuntimeAdapter.kt adds WorkflowRuntimeDispatcher and persisted-deadline remaining-time calculation. MemoWorkspaceViewModel routes the existing app.entities/entity.propose adapter through it and wraps execution in the remaining timeout. WorkflowRuntimeAdapterTest adds dispatcher selection, expired-attempt refusal, post-execution cancellation and invalid-result tests. tools/verify_source.py guards the integration.
- **Safety boundary:** dispatcher only invokes the exact registered adapter and validates candidate output. It cannot persist records, approve external actions or mark postconditions verified. The current request contract still rejects approval-gated/external-side-effect work.
- **Verification:** targeted source review completed; exact-head source verifier, JVM tests, lint, APK assembly, emulator upgrade and isolated signing are pending.
- **Not claimed:** generic DAG scheduling, browser/native adapters, device acceptance, merge or release.
- **Next:** add a second Memo-owned deterministic capability and its independent read-only postcondition observer through the same dispatcher, then generalize output/verification persistence without bypassing the durable attempt journal.


## Update 7 — Exact-head stabilization build and signed APK passed (19 November 2026)

- **Source commit:** `7ad0eb0426f953a8946741c59247b081a258f94a` on `stabilization/usable-stage-2026-11-10`.
- **Actions:** [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135) — all jobs succeeded.
- **Repairs in this increment:** completed the missing PL/TR translations and regression coverage; aligned the Talk workflow draft read-only allowlist with the registered research evidence-capture capability; fixed the browser approval dialog's composable boundary.
- **Passed:** source verifier (434/434 static UI Text literals), remote-runner tests, 188 core tests, 44 app tests, lint (0 errors; 42 warnings and 5 hints), debug/Android-test APK assembly, APK identity/signature, API 35 signed in-place upgrade preserving app-private data and both real encrypted stores, legacy recovery and corruption checks, isolated pinned signing.
- **APK:** package `app.memo.android.debug`, versionCode `928`, versionName `4.0.0-memo-debug`; [artifact 14565824727](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727). APK SHA-256 `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`; artifact ZIP SHA-256 `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`; pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Not accepted:** physical-device UX/navigation and real signed-in provider-session walkthrough. The user's phone and stored data were not touched. WF-01–WF-13 and STAB-05/STAB-06 remain open until their separate acceptance requirements are met.
- **Governance:** PR #8 remains draft/open; `main` untouched; no merge, release, signing change or destructive migration.
- **Next:** user tests the signed APK as an in-place update on the existing phone; capture any regression without uninstalling or clearing data.
