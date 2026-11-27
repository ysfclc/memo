# 2026-11-27 — Strict persisted Work-graph decoding

## Session identity
- Repository: `ysfclc/memo` (Memo, not Memo-2)
- Branch: `fix/strict-workflow-graph-decoding-2026-11-27`
- Base: `main` at `f628517699b51cac28c09c8fd0723a1334cf0f9f`
- Product requirement: MEMO-ORCH-001

## User outcome
Ensure that a saved Work plan or imported encrypted workspace backup cannot silently reinterpret malformed TaskSpec graph fields as a different, apparently valid graph.

## Baseline inspected
- `AGENTS.md`, `README.md`, `ROADMAP.md`, `TASKS.md`
- `docs/AI_EXECUTION_INVARIANTS.md`, `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `docs/DECISION_LOG.md`
- `docs/UI_REGRESSION_CONTRACT.md` and the 2026-11-27 dynamic Work-plan checkpoint
- `WorkflowGraph.kt`, `WorkflowGraphTest.kt`, `WorkflowTaskProposalParserTest.kt`, `AgentWorkPlanPolicy.kt`
- Current main source revision and latest GitHub Actions run #1287, which passed for source commit `f1f865ac1bcea73f68eab89c28fea77544f88561`.

## Work completed
- Replaced permissive `optString`, `optBoolean`, `optLong` and silent truncation in `WorkflowGraph.fromJson` with required typed readers.
- Require budget and node arrays and every schema-v1 field.
- Reject wrong JSON types, fractional/overflowing numeric values, overlong strings and arrays, missing budget data, and mixed legacy/current side-effect classification. Original schema-v1 graphs from before the `externalSideEffect` field was added remain readable only when all nodes omit it.
- Preserve the existing graph validator after strict decoding.
- No execution behavior, workspace schema, encryption, key alias, package identity or signing configuration changed.

## Changed files
- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowGraph.kt`
- `core/src/test/kotlin/app/memo/android/orchestration/WorkflowGraphTest.kt`
- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowTaskProposalParser.kt`
- `core/src/test/kotlin/app/memo/android/orchestration/WorkflowTaskProposalParserTest.kt`
- `TASKS.md`
- `docs/REQUIREMENT_TRACEABILITY.md`
- `IMPLEMENTATION_LOG.md`
- This checkpoint

## Verification evidence
| Check | Result | Evidence |
|---|---|---|
| Exact-head source, remote-runner, core and Android unit tests | Passed | Actions #1292 (`45998744182`), commit `d5cdbb83a1f8770dc99e2f32592d9622e3712c42` |
| Android lint and debug/release APK assembly | Passed | Actions #1292 |
| APK identity/signature and emulator install-over-existing/data preservation | Passed | Actions #1292 |
| Protected persistent signing and APK publication | Skipped | Pull-request validation; no installable release APK is claimed |
| Dependency assessment | Incomplete | Repository Dependency Graph is disabled |
| Physical device | Not run | Requires the user's phone |

## Risks and boundaries
- This is a persistence/backup integrity correction, not DAG execution. The decoder explicitly preserves the original schema-v1 serialized form from before `externalSideEffect` was added.
- Work plans remain DRAFT and no node is dispatched.
- Do not claim device acceptance or a user-ready APK from CI alone.
- No changes were made to Memo-2 or another repository.

## Next increment
Implement Work-plan execution only by extending the existing shared Talk lifecycle, not by adding a parallel workflow engine. It must include durable per-node state, cancellation, exact capability revalidation, approval reuse prevention, postcondition evidence and process-death recovery.
