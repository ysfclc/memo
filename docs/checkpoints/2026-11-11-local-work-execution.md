# Session checkpoint — Memo local Work execution lifecycle

**Date:** 11 November 2026  
**Repository:** `ysfclc/memo`  
**Branch:** `stabilization/usable-stage-2026-11-10`  
**Baseline HEAD:** `3d9db9b7ace91a751803b7c476eb5dc8c36007b7`  
**Current branch head at handoff:** `39d1b2b6bef7b5b64858bba36209667736539f45`  
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

## Work completed

- `WorkspaceScreens.kt`: local-model Work execution now calls `startJobStep` before context assembly and inference. The persisted coordinator therefore gates dependency readiness, exact runtime/capability availability, active-step exclusion and external-action refusal before the model is invoked.
- The prompt is assembled from the post-start workspace snapshot. Successful inference still creates a `WORK_ARTIFACT` marked `requiresHumanReview`, then moves the step to `NEEDS_REVIEW` and job to `WAITING_FOR_USER`.
- Model failure and artifact-save failure both move the checkpoint to `NEEDS_REVIEW` and the job to `WAITING_FOR_USER`; no failure path leaves a phantom RUNNING step or reports completion.
- `tools/verify_source.py` now checks the gate-before-inference ordering and review/failure lifecycle wiring.
- `TASKS.md`, `PRODUCT_GAP_REGISTRY.md` and `IMPLEMENTATION_LOG.md` now describe this as a partial first local-runtime dispatch slice, not a completed DAG executor.

## Verification and current blocker

- Existing core tests cover the checkpoint coordinator's dependency, runtime/capability, active-step and external-action rules. The new source verifier assertion is committed.
- GitHub Actions PR runs #379–#383 all failed in the preflight job before exposing any steps/logs; build, Gradle tests, lint and APK assembly were skipped. This is an existing pipeline/preflight blocker, not evidence of a Kotlin compile failure or success.
- No local Gradle/Android SDK environment or device execution was available in this session. Source-level compilation and physical-device behavior remain unverified.
- PR #8 remains draft; `main` and Memo 2 remain untouched. No APK was built or distributed.

## Next action

Repair the PR preflight failure so exact-head CI can reach source verification, Gradle tests, lint and Android compilation. Then continue the durable per-node runtime journal/adapter work; do not package an APK until meaningful product behavior and validation justify it.


## Follow-up hardening — 2026-11-11

The local Work dispatch path now persists the draft EvidenceRecord and the checkpoint/job transition to `NEEDS_REVIEW` / `WAITING_FOR_USER` in one workspace snapshot. The save is accepted only while the exact checkpoint is still `RUNNING`, and the artifact is linked to the owning job. Model/save failures use one review transition instead of two independent writes. This closes the partial-commit window where evidence could be saved while the job still appeared active, or the job status could change without its step.

The source verifier checks the atomic success and failure wiring. The source SHA-256 manifest was refreshed and independently cross-checked against an unchanged file entry. Exact-head PR run #388 still failed in the preflight job before exposing any steps or logs; Gradle, lint, APK assembly and device behavior remain unverified. Vercel's linked build project also reported its daily deployment API limit, so no preview build is available from this push.
