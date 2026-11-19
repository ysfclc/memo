# Memo — workflow recovery execution plan

**Date:** 13 November 2026  
**Repository:** `ysfclc/memo` (Memo 1)  
**Working branch:** `stabilization/usable-stage-2026-11-10`  
**Tracking PR:** [#8](https://github.com/ysfclc/memo/pull/8), draft and unmerged  
**Source baseline:** `3abb7d728107d2ce1fed45fa58ecc59c88fe0a7f`  
**Status:** ACTIVE — existing-journey stabilization only

## 1. Mission

Restore the existing Memo journey from user intent to a captured, reviewable result. The current user-facing journey is not accepted: the prompt can be approved and sent through a provider's visible website, but answer capture/recovery, task context, and user control are not dependable.

The requirements are defined in [the user-facing workflow/control audit](MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md). This plan converts those findings into implementation and verification tasks. It is not a new product roadmap.

## 2. Change boundary

Allowed:
- Reproduce and fix existing workflow defects.
- Add the minimum missing controls needed to recover a response already produced by a provider.
- Preserve existing approval, audit, encrypted storage, evidence and user-data boundaries.
- Add deterministic tests and improve diagnostics directly tied to these defects.

Not allowed during this incident:
- Unrelated features, broad visual redesign, architecture rewrites or dependency upgrades.
- Treating browser cookies as API credentials or bypassing provider login/usage restrictions.
- Automatically resending a prompt when submission is uncertain.
- Deleting/replacing raw provider evidence when the user edits a working copy.
- Merge to `main`, production deployment, release, signing changes or destructive migrations.

A source patch is not a verified fix. Each task must pass its stated tests and device acceptance before closure.

## 3. Priority order

### P0 — Recover answers and preserve work

- [ ] **WF-01 — Manual response capture (first implementation slice).** When a prompt has been submitted and automatic DOM detection reaches a user-review state, let the user paste/edit the visible provider answer and capture it without resending. Validate that the active provider page still matches the current step. Persist through the existing response/evidence callback so the result is linked to the same job, step, conversation and provider. Reject blank input and duplicate capture. Preserve all earlier captured responses.
- [ ] **WF-02 — Capture retry is not resend.** Make retry detection a distinct action from sending. The retry path must only poll/read the current page and must never call the send script. If submission is uncertain, keep the state as review-required and explain the duplicate-send risk.
- [ ] **WF-03 — Step-level recovery.** Add explicit, state-aware Skip provider and Stop current step actions. Keep Cancel entire run separate. A failed current provider must not erase completed results. Skipping must move to the next provider only after the current action is placed in a truthful terminal/review state.
- [ ] **WF-04 — Truthful completion.** Clicking Send is never response completion. A step is complete only after automatic or user-confirmed capture has been persisted and linked. Persistence failure must remain visible and must not be reported as success.
- [ ] **WF-05 — Reproduction evidence.** Reproduce happy path, detector timeout, delayed answer, changed-page/context loss and app background/recreation on a real Android device/provider. Record device/OS, exact branch SHA, steps and logs.

### P1 — Make the existing journey controllable

- [ ] **WF-06 — Editable prompt checkpoint.** Preserve original intent. Allow editing/regenerating the prepared prompt before approval. Recompute payload hash and invalidate old approval on every edit. Show exact destination and final payload at approval.
- [ ] **WF-07 — Keep task context with browser.** Remove mandatory navigation into a separate full-page browser flow for active work. Keep task, prompt, approvals, progress, response draft and recovery controls reachable while inspecting the provider page. Full-screen viewing may remain optional. Preserve browser session and task state across return.
- [ ] **WF-08 — Run history separate from templates.** Keep reusable workflow definitions distinct from run records. Join each run to original prompt, revisions, provider/runtime, approvals, per-step status, captures, evidence and failure/cancel reason. Reopen must not restart or resend.
- [ ] **WF-09 — Council runtime truth.** Label local, configured API and browser-session participants separately. Do not claim browser sign-in creates API access. Make unavailable runtimes and their reasons visible.
- [ ] **WF-10 — Result workspace.** Provide editable working copies, comparison and follow-up on captured provider answers while preserving immutable raw capture evidence and provenance. Final synthesis is separate from provider outputs.

### P2 — Stabilization verification

- [ ] **WF-11 — Regression suite.** Add pure tests for response capture state transitions, duplicate-capture prevention, retry-without-resend, skip/stop scope, approval invalidation and persistence failure. Add source-verifier guards where appropriate.
- [ ] **WF-12 — Device and localization acceptance.** Test English, Polish and Turkish; small screens/IME; page navigation; provider selector drift; delayed streaming; process recreation; partial multi-provider success; and stop/cancel semantics.
- [ ] **WF-13 — Release gates.** Complete the separate STOP-WORK plan: exact-head source verifier, remote-runner tests, Gradle unit tests, lint, debug/test APK assembly, API 35 encrypted-store instrumentation, pinned signing certificate and physical-device upgrade acceptance. Keep PR #8 draft and do not merge/release without explicit owner authorization.

## 4. Implementation sequence and dependencies

1. WF-01 first: a manual recovery path closes the most damaging gap without relying on brittle site selectors. Reuse the existing `onStepCompleted` callback and persistence path; do not create a second persistence mechanism.
2. WF-02 and WF-03 next: make the current step recoverable without duplicate external writes or loss of completed work.
3. WF-04 and WF-11 alongside each state transition: encode the invariants in pure tests before adding more UI.
4. WF-06: make the existing enhancement/approval boundary genuinely editable and content-bound.
5. WF-07 and WF-08: task context and run history depend on durable run identity and state; avoid a second competing store.
6. WF-09 and WF-10: clarify runtime availability and make captured output useful without conflating raw evidence with user edits.
7. WF-05, WF-12 and WF-13: device reproduction and full release gates close the incident.

If implementation reveals that an existing storage or callback contract cannot support a requirement safely, stop and document the smallest migration/design decision before coding around it.

## 5. Acceptance invariants

- No provider prompt is sent without explicit approval of the exact payload and destination.
- Editing a prompt invalidates prior approval.
- No uncertain send is automatically repeated.
- Manual capture does not trigger provider submission.
- Every captured result records provider, source URL, prompt/run/step linkage, timestamp and capture method.
- Raw captured text remains recoverable when a user edits a working copy.
- A response is not called complete until capture and persistence are confirmed.
- Skipping/stopping one step does not delete or cancel already captured results.
- A failed provider does not block access to other completed provider results.
- Browser navigation, page reload or process recreation must not silently convert unknown state into success or trigger a duplicate send.
- No crash-free or release-ready claim without physical-device evidence.

## 6. Reporting cadence

Update `docs/handoff/MEMO_STABILIZATION_PROGRESS_2026-11-13.md` after every meaningful implementation/verification increment and whenever a blocker or scope decision changes. Each update records:
- exact branch head before/after;
- task IDs touched;
- files and behavior changed;
- tests actually run and their exact result;
- tests not run;
- remaining risks/blockers;
- next concrete task.

Do not batch several unverified fixes and then report them as complete. Mark a task complete only when its acceptance and verification evidence are recorded.

## 7. Current blockers

- GitHub Actions has repeatedly failed in preflight before runner assignment (`runner_id=0`, no steps); this prevents exact-head Gradle/JVM/lint/APK validation.
- Vercel's free deployment API previously returned HTTP 402 for the daily deployment limit. Recheck only after the stated reset; do not burn more attempts while quota is blocked.
- No physical Android device/emulator execution was available in the previous audit session.
- The current branch has an unverified local-model GPU fallback patch and outstanding encrypted-store, signing and physical-device gates. See [STOP-WORK stabilization plan](MEMO_STABILIZATION_PLAN_2026-11-13.md).

These blockers do not prevent careful source inspection, targeted implementation and adding tests, but they do prevent calling a patch verified or the app fixed.

## 8. Addendum — generic browser, approval discoverability and checkpoint clarity (19 November 2026)

The user's latest device feedback is recorded verbatim in INPUT-2026-11-19-004 through -006. Add these tasks to the existing recovery sequence:

1. **BRW-15 diagnosis first:** reproduce the selected-provider/no-browser symptom. Trace Talk/Work route → optional local intent brief → startBrowserWorkflow → beginWorkflowStep → navigate(provider.startUrl) → WebView onPageStarted/onPageFinished/error → provider matching. Record the first state transition that diverges. Verify the actual Memo WebView account session; external Chrome sign-in is not proof of embedded WebView sign-in.
2. **BRW-14 generalize browser target resolution:** separate a reusable, user-goal-driven visible browser runtime from the Frontier consultation preset. Use registered capabilities and target metadata; no fixed provider catalogue as the universe of browser destinations. Preserve Frontier as an optional scenario.
3. **APR-01 expose one approval surface:** add a visible approve/reject action to the pending-action inbox and keep the same action available in context. Reuse the canonical ActionApprovalLedger. Do not create a parallel ledger or allow approval from an inbox card whose exact payload/destination cannot be revalidated.
4. **CHK-01 simplify state semantics:** progress checkpoints are informational; read-only/local work does not need external authorization; missing user input and uncertain outcomes are separate states; artifact review is not action approval. Only a concrete external/destructive side effect creates a pending approval. Explain every pause and show one relevant next action.
5. Add tests for generic target resolution, navigation callback failures, no-op prevention, approval discoverability/one-use/stale hash, no-approval read-only tasks, and status-to-UI mapping. Then run exact-head CI and the full physical-device journey.

These are additive to WF-01–WF-13 and STAB-05; they do not defer existing capture, retry, cancellation, data-preservation or provider/device acceptance.
