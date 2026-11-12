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

The journal, schema-7 persistence, lifecycle integration and regression tests are now implemented in source. Inspect the next exact-head CI run; if the preflight still fails before assigning a runner, resolve that Actions execution blocker before treating any Kotlin/Gradle test as verified.


## Implementation update — 12 November 2026

- **Source implemented:** added `WorkflowAttemptRecord` / `WorkflowAttemptJournal`; checkpoint start now appends an attempt bound to the saved TaskSpec runtime, capability list and idempotency key.
- **Persistence:** `AppSnapshot` schema 7 serializes the bounded journal; schema 1–6 imports remain additive and produce an empty journal. Schema 7 parsing is strict; snapshot validation checks unique/sequential attempt identity, exact graph binding, state/timestamp consistency and evidence references.
- **Lifecycle:** local model output closes the attempt as `OUTPUT_SAVED` with the evidence ID; inference failure closes it as `FAILED`; process death closes it as `INTERRUPTED` and returns the owning checkpoint to `NEEDS_REVIEW`. User-reported completion is stored separately. A node has a maximum of four user-initiated attempts; this is not an automatic retry policy.
- **Live:** attempt status, timestamp, runtime/capability references and evidence link are projected into the existing timeline; private model payloads are not copied into timeline entries.
- **Work UI:** each checkpoint shows its latest attempt number, state and runtime; users can generate another local draft without discarding prior evidence; the retry action is suppressed after four attempts. The local artifact coordinator and schema validator both verify exact job/step metadata and the human-review flag before accepting OUTPUT_SAVED.
- **Files changed:** `WorkflowAttemptJournal.kt`, `Models.kt`, `WorkflowCheckpointCoordinator.kt`, `LocalWorkArtifactCoordinator.kt`, `LiveTimelineProjector.kt`, corresponding core tests, `tools/verify_source.py`, `TASKS.md`, `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `IMPLEMENTATION_LOG.md`.
- **Verification still open:** no Gradle/test/lint/build result is claimed. The last observed PR run #416 failed in the incoming ZIP preflight; its job-log download returned BlobNotFound. The current commits have not yet received an exact-head passing CI run. No APK or device acceptance is claimed.
- **Next:** inspect the next exact-head workflow run; if preflight still fails, repair the archive intake workflow before treating any source tests as verified. Then continue runtime adapters and cancellation/recovery.

## User-facing state

- Implemented in source: per-node attempt journaling, additive schema-7 backup migration, local output/failure/interruption transitions and Live projection.
- Not yet verified: Kotlin compilation, Gradle JVM tests, lint, APK build, install/upgrade and real-device behavior.
- No external runtime execution was enabled; no production deployment or merge was performed.


## CI follow-up — exact-head run #439

- Commit tested: `a19d5d2c552560cc1c85656548ec8affb4e1f710`.
- Run: https://github.com/ysfclc/memo/actions/runs/43136772590
- Preflight job #130913889225 failed in approximately two seconds with `runner_id: 0`, empty runner name and an empty step list. The log download endpoint returned Azure `BlobNotFound`.
- Build, Gradle tests, lint and APK assembly were skipped. This is a runner/preflight execution failure; it provides no Kotlin compile or test result.
- Subsequent commits added a regression test that prevents a user-reported completion from racing an active local-model inference, clarified lifecycle comments and strengthened source-verifier assertions. Those newer commits still need exact-head CI.

## Latest verification — exact-head run #462

- Commit: `e7948ab0fd1a57c2064860f3eea9731815d1f909`.
- Run: https://github.com/ysfclc/memo/actions/runs/43138553360
- The incoming ZIP preflight job ended with failure, `runner_id: 0`, no runner name and zero steps. The remaining validation/build/signing jobs were skipped. The job-log endpoint did not provide a diagnostic.
- A separate local `kotlinc` harness for the attempt journal passed state-transition smoke checks (start, failure, retry identity, user outcome, recovery and validation). It used lightweight dependency stubs; it does not verify the full Android/core source tree.
- Full Gradle JVM tests, source verifier, lint, APK assembly/signing and device acceptance remain unverified.

## Latest CI update — exact-head run #480

- Commit: `c50b22c581d404bd43524d2a3422a2cfae382be9`.
- Run: https://github.com/ysfclc/memo/actions/runs/43139301026
- The incoming ZIP preflight job again failed before runner assignment (`runner_id: 0`, zero steps); downstream validation, Gradle, lint and APK jobs were skipped. This is the latest exact-head CI result.
- The prior run #462 was also retried and failed the same way on its second attempt. Repeated retries are not useful until GitHub Actions runner availability or repository Actions capacity is restored.

## Current branch-head CI — run #483

- Current branch head at this checkpoint: `9d098c33f94b656b241026559565459c07b53c91`.
- Run: https://github.com/ysfclc/memo/actions/runs/43139439135
- The incoming ZIP preflight job failed before runner assignment (`runner_id: 0`, zero steps); validation, tests, lint, APK build and signing were skipped. No source compile/test result is available.
