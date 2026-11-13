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
- The source-verifier guard and manifest refresh are being added in the follow-up integrity commit.
- Automated tests: **not run**.
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
1. Verify this patch at source level after commit and run the relevant test when an executor is available.
2. Continue WF-02: make retry detection an explicit capture-only action and prove it cannot invoke the send script.
3. Implement WF-03 step-level Skip/Stop semantics without changing whole-run cancellation or deleting prior evidence.
4. Keep reporting source status separately from test/device verification.
