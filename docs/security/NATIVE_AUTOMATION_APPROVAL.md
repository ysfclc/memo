# Native automation approval boundary

Status: interim fail-closed mitigation implemented; full destination-bound approval flow is not implemented.

## Interim mitigation now present in source

- WorkflowPolicy now classifies known native step types into stable capability IDs and allows only launch, visible-text observation and bounded delay without approval.
- Native TAP_TEXT and BACK steps are rejected before the run begins. Unknown step types fail closed.
- The native workflow editor defaults to WAIT_TEXT and no longer offers Tap or Back as new step choices. Existing drafts containing those steps remain visible for explicit removal/editing.
- Seven JVM policy tests now cover safe workflow acceptance, tap/back rejection, unknown action classification, sensitive text and workflow bounds.

This mitigation prevents the current native runner from executing those unapproved UI side effects. It does not create an approval proposal, approval UI, one-use consumption, shared-ledger lifecycle or native action evidence. Do not describe the full native approval feature as implemented.


## Evidence and current gap

The Android source currently validates native workflows in `app/src/main/java/com/synthos/runtime/engine/WorkflowPolicy.java`. Its sensitive-action protection is a small regular-expression denylist over `WorkflowStep.value`. The native runner in `AutomationAccessibilityService` executes `TAP_TEXT` by finding an exact visible label and calling `AccessibilityNodeInfo.ACTION_CLICK`; `BACK` invokes Android's global Back action. The native run begins after policy validation, without creating or consuming a destination-bound approval for each click.

This is insufficient for real user data and external side effects. The visible label may be "Buy", "Delete", "Publish", "Send", "Transfer", "Invite", "Share", "Confirm", or a provider-specific equivalent. No finite keyword denylist can safely infer every side effect from UI text. A final visible-text postcondition proves only that text appeared; it does not prove that an external action was authorized or that the intended result occurred.

The browser workflow already has a stronger exact-content approval path, but the current action inbox is not yet a universal approval ledger. Do not describe native automation as protected by the browser approval mechanism.

## Required security model

### 1. Capability classification, fail closed

- Replace keyword-only safety as the authorization boundary with typed capabilities and a deterministic policy decision.
- Classify every executable native step before execution. `LAUNCH_APP`, `WAIT_TEXT`, and bounded `DELAY` may be non-mutating under explicit limits.
- Treat `TAP_TEXT` as an unknown UI side effect by default. It must not execute until an approval gate is available and consumed.
- Treat `BACK` as potentially destructive (for example, dismissing an unsaved form or abandoning a draft); it must also pass the applicable policy gate.
- A future read-only exception must be proven by a typed capability contract, not guessed from button text. Unknown action types, unknown app identity, sensitive windows, stale UI observations, and missing policy metadata must block execution.

### 2. Approval binds to the actual action

A native approval proposal must bind at minimum to:
- unique action ID and run ID;
- workflow ID and immutable workflow version;
- exact target package and observed foreground package;
- capability ID and step index;
- exact visible target label and the resolved UI node/action identity available at execution time;
- a canonical payload hash over the complete approval scope;
- creation/expiry time and one-use state.

The approval UI must show the target app, exact control label, action being authorized, and the consequence/uncertainty. No approval may be inferred from a saved workflow, a previous click, a browser approval, or a broad "allow automation" toggle.

### 3. Consume immediately before the side effect

- Re-check device unlocked state, target package foreground, current workflow version, current step, current UI observation, and approval expiry immediately before the click/back action.
- Re-resolve the exact node. If it is missing, ambiguous, disabled, changed, or belongs to another package/window, invalidate the proposal and do not act.
- Consume the approval atomically and exactly once before invoking the Android action.
- If the process dies or the action result is uncertain after consumption, transition to `NEEDS_REVIEW`; never replay automatically.
- Cancellation, app switch, screen lock, workflow edit, target change, or payload mismatch invalidates any pending approval for that step.
- A postcondition is separate evidence of outcome; it cannot substitute for authorization.

### 4. Shared ledger and lifecycle

- Move approval state behind one app-wide contract used by browser, native automation, and future providers. Preserve browser exact-content behavior while migrating; do not create a second native-only approval database.
- Use explicit states such as `PENDING`, `APPROVED`, `IN_PROGRESS`, `COMPLETED`, `REJECTED`, `EXPIRED`, `INVALIDATED`, and `NEEDS_REVIEW`.
- Record append-only audit events for proposal, approval/rejection, invalidation, consumption, outcome, and uncertainty.
- Approvals are short-lived, scoped, one-use, revocable, and never portable across runtime, package, workflow version, or action payload.

## Acceptance tests (required before enabling native side effects)

1. A normal launch/wait/delay workflow remains bounded and can complete with a verified postcondition.
2. A `TAP_TEXT` workflow creates a pending proposal and performs zero clicks before approval.
3. A `BACK` workflow cannot execute a potentially destructive navigation action without its applicable approval.
4. Exact approval for one label cannot authorize a different label, package, window, workflow version, run, or step.
5. Any change to the target package, visible label, resolved node, workflow version, or payload hash invalidates approval.
6. Approval expires and cannot be replayed or consumed twice, including concurrent callbacks.
7. Lock screen, target-app backgrounding, service interruption, process death, cancellation, and stale UI all fail closed.
8. A crash or uncertain result after consumption moves the action to `NEEDS_REVIEW`; there is no automatic retry.
9. A visible-text postcondition without a consumed approval cannot mark a side-effecting action successful.
10. Tests exercise the real policy/ledger implementation, not only source-text assertions. Run JVM tests, Android tests, lint, emulator install/upgrade tests, and physical-device acceptance.

## Implementation sequence

1. Define typed native action/capability and approval-scope contracts in the shared core.
2. Extend the canonical action ledger with native action proposals and one-use consumption while retaining browser regression coverage.
3. Add a native approval UI that displays the exact action scope and supports explicit approve/reject.
4. Gate the AccessibilityService at the final side-effect boundary; remove keyword denylisting as the authorization mechanism (it may remain as defense in depth).
5. Add lifecycle invalidation, uncertain-outcome recovery, and audit evidence.
6. Enable native side effects only after all acceptance tests pass on CI and a real device.

## Current release posture

Native `TAP_TEXT` and `BACK` remain a known security gap until this contract is implemented and verified. Do not claim the native runner is safe for unattended use, do not add standing blanket approvals, and do not merge a partial gate as complete protection.
