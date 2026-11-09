# Memo UI/UX and reliability audit — 2026-11-09

Status: source-based audit and implementation contract. No visual/device acceptance is claimed.

## Product standard

Memo's four primary surfaces remain Talk, Live, Work and Studio. The user should be able to express an outcome once, understand what Memo is doing, intervene at meaningful checkpoints, and find the resulting work again. The UI must be the visible expression of one coherent product and one source of truth—not a launcher for unrelated mini-apps.

Do not redesign by adding more cards, buttons, tabs, or decorative dashboard metrics. Reduce choices, clarify state, and make the main journeys complete.

## Source observations

Inspected the active Android Compose source on this security branch, based on ysfclc-patch-1.

1. **Navigation is conceptually four-surface but implementation routes are broader.** The bottom bar exposes Talk, Live, Work and Studio, while Browser, Apps, Approvals, Council, Models and Settings are secondary destinations. This is directionally consistent with the product contract. The header's More route is the entry to system functions; keep those secondary.
2. **Talk entry and legacy home are not yet a single clean mental model.** The Talk destination currently routes to a HomeScreen with workspace search, a capture composer, conversation shortcuts, connected-work shortcuts, Studio shortcut, metrics, review status and recent items; the separate ChatsWorkspaceScreen is another route. This creates a home/dashboard layer between the primary Talk surface and the actual conversation list/composer. The user should not have to understand that internal distinction.
3. **The entry screen has competing actions.** The capture composer is the main action, but Conversations, Connected work and Self Model & Artifact Studio appear directly below it, alongside global search and dashboard sections. This competes with the single-intent-entry principle and makes the first screen longer and more administrative than it needs to be.
4. **Global search is a local in-memory scan, not a unified indexed search.** It searches conversations/messages, entities, jobs and evidence directly from the current snapshot, caps each result type at three, and returns different destinations (entities go to Work rather than the matching record). The field is honestly labelled local in its result state, but "Search everything" can imply broader coverage. Search needs one result model, stable deep links, type filters and an explicit scope/coverage statement.
5. **More is too implementation-facing.** It exposes a runtime registry and raw capability descriptors. That may help diagnostics, but it is not an everyday user task. Keep diagnostics available under an advanced/system section, and present user-facing controls in terms of what the user can accomplish.
6. **Screen ownership is fragmented across large files.** MemoWorkspaceApp.kt and WorkspaceScreens.kt are each about 1,900 lines and contain navigation/orchestration alongside screen implementations. This increases regression risk and makes it harder to test individual journeys. Refactor by feature and state ownership, not by cosmetic file splitting alone.
7. **Live is currently a summary with links.** It surfaces active jobs/tasks, approvals and recent activity, then sends the user to separate Browser and Apps screens. The product contract calls for one chronological cross-runtime execution timeline with pause, resume, takeover, cancellation, evidence and uncertainty. Do not call a summary dashboard a complete control room.
8. **Social platform is a product-scope gap, not a design omission.** No first-class social publishing, inbox, feed, discovery, engagement or analytics journey is represented in the four primary surfaces. Preserve this requirement in the product roadmap; do not silently omit it or add a disconnected social dashboard.

## First reliability fix landed on this branch

The Talk capture handoff previously cleared the composer before the parent had confirmed that a conversation and source message were durably accepted. If the workspace hit its conversation limit or message persistence failed, the user's draft could disappear. The callback now returns an acceptance result; the composer clears only when the handoff succeeds. A plan-generation failure after the source message is saved still preserves the request in its linked conversation. This is a narrow reliability fix, not a completed UX redesign; it still needs Android compilation, automated UI coverage and device verification.

## UX implementation sequence

### UX-1 — Make Talk a true single entry point
- Reshape the current Talk landing state so its primary purpose is unmistakable: one composer for text/voice/share/attachments as capabilities become real.
- Keep conversation history and resume actions within Talk, not as a competing top-level concept.
- Remove the three competing shortcut buttons from the primary capture card; expose contextual destinations after intent interpretation or in the relevant surface.
- Preserve explicit review-before-run and no-external-send-by-default behavior.
- Never clear a draft before its handoff has been durably accepted. Show recoverable errors and preserve user text on failure.

### UX-2 — Make Live trustworthy
- One chronological event stream across all runtimes, with clear actor, target, current step, last verified outcome, timestamps and evidence.
- Use consistent states: queued, running, waiting for approval, blocked, failed, cancelled, completed, needs review.
- Put pause/resume/cancel/takeover beside the active run; global Stop remains visible.
- Distinguish "request sent", "provider acknowledged", "side effect started", and "verified complete". Never flatten these into a green success state.

### UX-3 — Make Work navigable and forgiving
- One searchable graph with record detail routes, relationships, provenance, revisions, archive/undo and safe merge previews.
- Provide Today (MUST / SHOULD / COULD / NOT TODAY) as the default useful view, not a large set of CRUD tables.
- Every record search result must open the matching record, not merely its containing section.

### UX-4 — Make Studio a real deliverable workspace
- Consistent editor, autosave status, revision history, compare/restore, evidence links and export.
- Private drafts by default. Publishing remains a separate destination-bound approval flow.
- Remove Self Model as a standalone dead-end; make it a source for grounded artifacts.

### UX-5 — System capabilities are contextual
- Browser, native app automation, model management, Council, permissions and diagnostics remain available, but appear when relevant to a task.
- Advanced diagnostics show technical details; everyday UI shows plain-language status and next action.
- Capability unavailability must be visible before a run, not discovered after failure.

### UX-6 — Reliability and accessibility
- Every button has a single clear action and disabled/loading/error/success state.
- Preserve drafts and scroll/navigation state across configuration changes and recoverable failures.
- Keyboard/IME-safe layouts, screen-reader labels, minimum touch targets, contrast, large text and reduced motion.
- Test empty, loading, offline, vault-locked, permission-denied, stale-runtime, interrupted and large-data states.
- Add Compose UI tests for the critical paths and screenshot/regression tests for the primary surfaces; verify on a real Android device before claiming UX completion.

## Acceptance journeys

1. New user opens Memo, enters one intent, receives a clear interpretation/clarifying question or saved draft, and can continue without choosing a subsystem.
2. Returning user resumes the correct conversation or active job without searching several screens.
3. A run shows its actual runtime and current step; stop/cancel is available; uncertain side effects become review, never auto-replayed.
4. Search finds a conversation, project/person, job/task and artifact/evidence, and each result opens the exact record with source/provenance.
5. User edits a deliverable, sees saved state, restores a prior revision, and exports without losing work.
6. User can reach model/provider, browser, native apps, approvals and settings without those controls dominating the main product.
7. Social workflows remain part of Memo's product contract and are integrated into Talk/Live/Work/Studio rather than shipped as a disconnected dashboard.

## Guardrails for this pass

- Do not make a large visual rewrite before stabilizing navigation/state ownership and adding UI journey tests.
- Do not remove user-visible capabilities just to simplify screenshots; move them to contextual/advanced entry points.
- Do not add placeholder screens, fake metrics, inert controls or generic dashboard cards.
- Preserve encrypted workspace data, signing identity, migrations, English/Polish/Turkish support and the user's existing workflows.
- Treat the current audit as source inspection only. CI, emulator and physical-device acceptance are still required.
