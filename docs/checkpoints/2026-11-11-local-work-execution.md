# Session checkpoint — Memo local Work execution lifecycle

**Date:** 11 November 2026  
**Repository:** `ysfclc/memo`  
**Branch:** `stabilization/usable-stage-2026-11-10`  
**Baseline HEAD:** `3d9db9b7ace91a751803b7c476eb5dc8c36007b7`  
**Base / PR:** `main` / draft PR #8  
**Working tree:** Remote GitHub source inspected; no local checkout available in this session.  
**Product requirements:** MEMO-ORCH-001, MEMO-LIVE-001, MEMO-AI-001  
**Engineering tasks:** M3.A3, M3.A4, M3.8.A7, M4.L1, M4.L4

## User outcome

Make a Work checkpoint that uses an installed local model behave like a real, observable execution attempt: it must enter RUNNING before inference, respect the saved workflow's dependency/runtime/policy checks, save the generated draft as evidence, and leave an explicit review state. Failures must not leave a phantom RUNNING job or claim success.

This is a first vertical slice toward the durable DAG executor. It is not a claim that all runtime dispatch, retries, cancellation, pause/resume or verified postconditions are complete.

## Baseline inspected

- Product contract: `docs/MEMO_PRODUCT_CONTRACT.md`.
- Governance: `DEVELOPMENT_GOVERNANCE.md`.
- Requirements: `docs/REQUIREMENT_TRACEABILITY.md`.
- Engineering plan: `TASKS.md`, especially M3.A1–A4 and M3.8.A7.
- Latest prior checkpoint: `docs/checkpoints/2026-11-10-mainline-consolidation.md`.
- Source inspected: `core/.../data/Models.kt`, `core/.../orchestration/WorkflowCheckpointCoordinator.kt`, `core/.../orchestration/WorkflowGraph.kt`, `core/.../runtime/RuntimeContracts.kt`, `core/.../data/LiveTimelineProjector.kt`, `app/.../data/MemoWorkspaceViewModel.kt`, `app/.../ui/WorkspaceScreens.kt`, `app/.../ai/AiRuntimeViewModel.kt`.
- PR #8 description explicitly states checkpoint coordination is a persisted manual layer, not an autonomous runtime executor; CI/device validation has not passed for the current branch head.
- Current local-model Work callback builds a context-bounded prompt, invokes `runtime.runPrompt`, saves a `WORK_ARTIFACT` EvidenceRecord and moves the step to `NEEDS_REVIEW`. It does not first reserve/start the checkpoint through `startJobStep`; this bypasses dependency readiness and the persisted RUNNING transition. Its failure branch only displays a message and can leave lifecycle state ambiguous if a step was started separately.

## Work package

- **In scope:** enforce coordinator start before local-model inference; ensure failure/cancellation paths transition to a truthful recoverable state; keep generated output as review-required evidence; make state visible through existing Work/Live projections; add regression tests where the behavior can be expressed in core.
- **Out of scope:** external writes, browser/native dispatch, autonomous retries, new persistence schema, APK packaging, Memo 2, CircleCI, PR merge.
- **Safety:** local inference remains user-initiated and local-only; model output is untrusted draft content; no external action is performed; no user note or model output becomes runtime-verified evidence.
- **Acceptance:** blocked dependencies/runtime/policy prevent inference; successful local inference starts a checkpoint first and ends in NEEDS_REVIEW with linked evidence; model failure/cancellation does not leave RUNNING; legacy sequential jobs remain usable.
- **Rollback:** revert the focused UI lifecycle change and tests; no schema or user-data migration.

## Verification

| Check | Result |
|---|---|
| Source inspection | completed at baseline HEAD above |
| Core unit tests | not run yet |
| Android unit tests | not run yet |
| Lint / build | not run; no local Android SDK/Gradle execution environment established |
| Emulator / physical device | not run |
| APK | intentionally out of scope |

## Next action

Implement the focused local-model Work lifecycle slice, add/update tests and documentation, then run every available source/core/Android validation. Keep PR #8 draft and main untouched until exact-head CI and device acceptance exist.
