# Memo UI/UX Recovery

**Started:** 2026-11-15  
**Branch:** `work/ui-ux-recovery-2026-11-15`  
**Baseline inspected:** `c307f94f5d296622e9e3b2fa92b02fe9c58361f2`  
**Status:** in progress; no screen or device acceptance claimed.

## Objective

Make Memo's existing product capabilities understandable, coherent and dependable through one consistent Android experience. This is a recovery of the real product, not a visual reskin and not a feature-category redesign.

## Source findings

- The repository already contains the Signal & Graphite token system and shared primitives (`MemoPageHeader`, `MemoSectionHeading`, `MemoPanel`, `MemoStatusTag`, `MemoMessageBubble`). Their existence is not proof that all screens use them correctly or consistently.
- `MemoWorkspaceApp.kt` and `WorkspaceScreens.kt` remain very large files that mix shell/navigation/state orchestration with screen composition. Refactoring must follow state ownership and testability, not line count alone.
- The current primary Talk destination is `ChatsWorkspaceScreen`; the old dashboard-style `HomeScreen` remains as an unused private composable in `MemoWorkspaceApp.kt`. The earlier UX audit describes that legacy screen as if it were the active Talk entry, so its route-level description is stale and must not drive a blind rewrite.
- Search is a bounded in-memory scan and does not yet behave like one reliable, deeply linked search surface.
- Live is not yet a unified cross-runtime execution control room.
- The current test setup had JVM/unit coverage but no Compose UI journey suite. This branch adds the Compose UI test dependency and initial primitive smoke tests; they still require CI execution.
- Existing docs explicitly distinguish source implementation from visual/device acceptance. Preserve that honesty.

## Principles

1. Treat all prior use cases as test scenarios, never as hardcoded product categories.
2. Preserve existing capabilities and user data; simplify presentation without silently removing access.
3. One primary action per surface; contextual secondary actions.
4. Every interactive control must have a real action and visible loading, disabled, success, failure or unavailable state.
5. Preserve drafts, navigation context, and user input across recoverable failures and configuration changes.
6. Show what Memo is observing, reasoning about, executing, waiting for, and has actually verified.
7. English, Polish and Turkish must remain first-class; layout must tolerate long translated strings and large text.
8. No fake dashboard metrics, placeholder screens, decorative controls or success states without evidence.

## Work sequence

### UXR-0 — Establish regression gates
- [x] Add Compose UI test support.
- [x] Add smoke coverage for shared page header, status semantics and message rendering.
- [x] Wire the Compose smoke suite into the Android API 35 emulator workflow (execution result still pending).
- [ ] Observe a green CI run and correct any compile/runtime failures. The first PR run exposed a stale static invariant that still required ephemeral `remember` after the app-shell state was correctly upgraded to `rememberSaveable`; the invariant now checks the actual saveable contract. The next run must verify this correction.
- [ ] Extend coverage to real primary journeys before broad screen changes.
- Keep build, emulator and physical-device evidence separate.

### UXR-1 — Talk as the universal entry
- Remove the unnecessary home/dashboard detour and competing shortcut hierarchy.
- One intent surface accepts text, voice and incoming share/capture material.
- Conversation history/resume belongs in the same continuous Talk experience.
- Draft clears only after durable acceptance; failure preserves the text.

### UXR-2 — Navigation and state continuity
- Independent destination back stacks and scroll state.
- Deep links open exact records and return to their owning context.
- Back behavior is deterministic; overlays/details close before destination changes.
- Adaptive compact/expanded layouts, keyboard/IME-safe composer behavior.

### UXR-3 — Live as a trustworthy execution surface
- One chronological stream across runtimes.
- Consistent states and explicit distinction between attempted, acknowledged, completed and verified.
- Pause/resume/cancel/takeover and evidence at the point of action.

### UXR-4 — Work and Studio coherence
- Search results deep-link to exact records.
- Today is useful without forcing a domain taxonomy.
- Studio provides durable drafts, revision state, preview and export.

### UXR-5 — Cross-app polish and accessibility
- Shared actions, inputs, rows, overlays and error/empty/offline/recovery states.
- Touch targets, semantics, contrast, large text, reduced motion and translations.
- Screenshot and journey regression coverage.

## Acceptance gates

A screen is not complete because it compiles or uses shared components. Each journey must pass:
- visual review in light and dark themes;
- interaction and navigation tests;
- loading, empty, offline, permission-denied, interrupted and failure states;
- English, Polish and Turkish string/layout review;
- accessibility semantics and minimum touch-target checks;
- Android CI build and emulator tests;
- physical-device acceptance on an upgrade install, with evidence recorded.

## First increment

- Added Compose UI test support and smoke tests for the shared page header, status semantics and message bubble. The Android API 35 workflow now runs this suite explicitly after the install-over-existing encrypted-store verification; execution is pending a workflow run.
- Changed the app-shell tab, selected conversation, pending Talk prompt, browser sub-section and Work section request from ephemeral `remember` state to `rememberSaveable`. The Talk composer draft is also now keyed by conversation and uses `rememberSaveable`, so a rotation/Activity recreation does not silently discard an unsent message. These are saved-instance-state protections, not a substitute for durable persistence in the encrypted workspace.
- Source inspection found that the old `HomeScreen` dashboard composable has no call site. The actual Talk destination is `ChatsWorkspaceScreen`. Future cleanup should remove or isolate the dead implementation only after confirming no reflection/preview/test dependency; do not design against stale audit wording.

These are regression foundations and a narrow state-recovery fix only; they do not claim the Talk/Live/Work/Studio journeys are fixed.


## Follow-up found during verification

- The first PR CI run failed before Gradle because `tools/verify_source.py` still asserted the old `remember` implementation for the default Talk tab. This was a stale assertion, not a product regression. The check now requires the new `rememberSaveable` behavior instead of rejecting it.
- Active Talk composer drafts previously used plain `remember(selectedConversationId)`. They now use `rememberSaveable(selectedConversationId)`; switching conversations still starts a separate draft, while Activity recreation restores the current one.
- The correction is committed; CI has been retriggered. Android compilation, lint, emulator execution and physical-device behavior remain unverified until the new run completes.
