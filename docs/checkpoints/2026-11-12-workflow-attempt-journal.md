# Session checkpoint — Memo durable workflow attempt journal

## Session identity

- Date/time/timezone: 12 November 2026, 02:35 CET (Europe/Warsaw)
- Repository: `ysfclc/memo`
- Branch: `stabilization/usable-stage-2026-11-10`
- Baseline HEAD: `544ae2c50d06cbab1e11dd5ff9150a202818432e`
- Base branch / PR: `main` / draft PR #8
- Working tree state: No local `/testbed` checkout is mounted; GitHub source is inspected and writable through the connected repository integration.
- Product/requirement IDs: MEMO-ORCH-001, MEMO-LIVE-001, MEMO-SECURITY-001; engineering task M3.8.A7 and M4.L4.

## User outcome

- Add durable, inspectable per-node execution-attempt history so a checkpoint start, model output, failure and process interruption cannot disappear into only a mutable step status.
- This is the next prerequisite for safe retry, pause/resume and truthful Live history. It advances the central intent-to-outcome loop rather than spending this increment on APK packaging.
- Keep local-model execution as the only dispatched runtime. Browser/native/external actions remain behind their dedicated approval and runtime contracts and are not newly enabled.

## Baseline inspected

- Product contract: `docs/MEMO_PRODUCT_CONTRACT.md`; the conversation-to-workflow-to-runtime-to-evidence loop is the product authority.
- Governance: `DEVELOPMENT_GOVERNANCE.md`; Fresh Start and evidence gates followed.
- Latest checkpoint: `docs/checkpoints/2026-11-11-local-work-execution.md`.
- Source inspected: `WorkflowCheckpointCoordinator.kt`, `WorkflowGraph.kt`, `LocalWorkArtifactCoordinator.kt`, `Models.kt`, `RuntimeContracts.kt`, `LiveTimelineProjector.kt`.
- Tests inspected: `WorkflowCheckpointCoordinatorTest.kt`, `LocalWorkArtifactCoordinatorTest.kt`, `LiveTimelineProjectorTest.kt`, `WorkspaceSchemaV3Test.kt`.
- Exact-head CI: PR run #416 on baseline HEAD failed in the incoming `memo.zip` preflight job; downstream validation/build jobs were skipped. The log endpoint returned Azure `BlobNotFound`, so the root cause remains unknown. This is not evidence of a Kotlin compile/test result.
- Existing behavior to preserve: schema-v6 imports, encrypted vault migration boundaries, local inference gate-before-dispatch, atomic evidence/review transition, exact-content external-action approvals, legacy sequential jobs and the no-blind-replay recovery policy.

## Work package

- Add a bounded `WorkflowAttemptRecord` journal to `AppSnapshot`, persisted in workspace schema 7 with additive schema 1–6 import compatibility.
- Start an attempt atomically with the checkpoint RUNNING transition, binding job, step, runtime, capability IDs and the graph's stable idempotency key.
- Complete the attempt as OUTPUT_SAVED only when the linked review-required artifact is persisted; record local-model failures distinctly; mark active attempts INTERRUPTED on process recovery.
- Record user-reported outcomes separately from runtime attempt status.
- Project attempts into the existing Live timeline without exposing private prompt/output payloads.
- Add core regression and schema-migration tests. Do not dispatch browser/native runtimes, add a permission or dependency, or package/distribute an APK in this increment.

## Acceptance

- No checkpoint can start without an appended durable attempt record in the same returned workspace snapshot.
- No local model output can be saved unless a matching attempt is RUNNING; output persistence closes the attempt and saves evidence plus step/job state atomically.
- Failure and process death close an attempt into a reviewable terminal state; no RUNNING attempt remains after recovery.
- Imported schema-v6 backups continue to load with an empty attempt journal and export as schema 7.
- Duplicate attempt IDs, malformed identities/statuses/timestamps, orphan job/step/evidence references and oversized journals are rejected.
- Live shows attempt state/timing/runtime references, never private model payloads.
- Existing approval gates, legacy sequential jobs and action-audit behavior remain unchanged.

## Verification baseline

| Check | Exact command / CI run / device steps | Environment and commit | Result | Evidence link |
|---|---|---|---|---|
| Unit tests | PR workflow run #416 | GitHub Actions, `544ae2c50d06cbab1e11dd5ff9150a202818432e` | blocked in preflight; downstream tests skipped | https://github.com/ysfclc/memo/actions/runs/43061035590 |
| Build / package | Same PR workflow | Same commit | skipped | same run |
| Real device | Not run | No device session | not run | — |

## Risks and recovery

- Schema migration is additive; old schema versions import without attempts. Revert source changes to restore schema 6 if needed; preserve original encrypted-vault file and key aliases.
- Attempts are execution metadata, not proof of external side effects. Interrupted or ambiguous work stays NEEDS_REVIEW and is never replayed automatically.
- PR remains draft; `main` and Memo 2 remain untouched. No production deployment or signing changes.

## Next action

Implement the journal, schema-7 persistence, lifecycle integration and tests in this branch. Then inspect exact-head CI; if the preflight remains blocked, investigate that pipeline independently without claiming Gradle success.
