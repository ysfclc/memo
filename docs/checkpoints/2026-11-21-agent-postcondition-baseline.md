# 2026-11-21 — Baseline checkpoint: shared capability postcondition verification

## Session identity
- Repository: `ysfclc/memo`
- Default branch: `main`
- Baseline main commit: `a6c893c7808c5f042f9fe3cb8254b5ac5b90a2c4`
- Latest Android-source commit represented in the import/CI record: `adf58109af909358940e7361251669377e2a84d5`
- Working branch: `agent/postcondition-verification-20261121`
- Product requirements: MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-LIVE-001
- Planned task: M3.A4-S7 — require capability-owned postcondition verification before success

## Baseline inspected
- `AGENTS.md`, `DEVELOPMENT_GOVERNANCE.md`, `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/AI_EXECUTION_INVARIANTS.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `TASKS.md`, `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, `ROADMAP.md`, `BUILD_STATUS.md`, `IMPLEMENTATION_LOG.md`, `.github/workflows/build-apk.yml`.
- Recent history includes the durable Talk run record/recovery work and the M3.A4-S6 fixes. The current main commit is an import/status-note commit after the CI-tested source head; Android source paths are unchanged.
- Open draft PRs inspected: #15 browser page capture sharing, #11 browser research reliability, #10 Compose UX recovery, #9 APK version/downgrade protection, #8 safety/runtime routing. No PR was merged or altered.
- Talk uses `AgentExecutionEngine`, a runtime-validated `AgentCapabilityRegistry`, and the existing encrypted workspace/run journal. Recovery intentionally marks interrupted work NEEDS_REVIEW and does not replay it.
- Existing tool results have only status/output/summary/evidenceReference. The engine increments successful calls and proceeds after an adapter reports SUCCEEDED; there is no shared explicit postcondition verification contract.
- Production workspace note/record and browser capture adapters already perform persistence/read-back checks; search/navigation return observable results. Verification must build on these adapters and not invent evidence.

## Baseline verification
- GitHub Actions run `44901392061` on source commit `adf58109af909358940e7361251669377e2a84d5`: all observed jobs completed successfully, including source checks, remote-runner tests, Android core/app unit tests, lint, debug/instrumentation assembly, APK identity/signature checks, emulator install-over-existing preservation, isolated signing and source handoff.
- CI artifacts include integrated repository `14705942879`, CI debug APK `14705833127`, debug APK `14705349236`, and build reports `14705209315`. These are not a canonical release APK.
- Combined GitHub commit status on the source and import commits reports Vercel success only; the Android GitHub Actions result is confirmed by the workflow run/jobs above, not by the combined status API.
- The latest build outputs are debug variants, not a release package with canonical identity `app.memo.android`; no release/device acceptance is claimed.

## User outcome / selected increment
A capability must not be counted as successful merely because its adapter returned SUCCEEDED. The shared engine will require an explicit adapter postcondition verifier for successful results, with a bounded verification description and evidence reference. The verifier runs after invocation, is timeout-bounded, and its failure must prevent success from being recorded or the model from claiming verified completion. Unverified read-only results may fail and permit re-planning; unverified local/external/destructive writes must stop at NEEDS_REVIEW. For write capabilities, verification evidence must match the adapter's returned evidence reference.

## Non-goals
- No new workflow/domain, primary screen, executor, runtime, database, backup schema or app identity.
- No external-write adapter, action replay or approval-gate relaxation.
- No automatic resumption after interruption.
- No unrelated UI polish or product scope change.

## Acceptance / rollback
- Core unit tests cover verified success, missing verifier, verifier rejection, mismatched evidence, and write uncertainty; existing engine scenarios remain green.
- Production adapters verify actual saved note/record/capture state and current browser observation; read-only workspace search is recomputed against its permitted candidate set.
- Source verifier, core/app tests, lint and debug build must run in PR CI on the exact branch head. Release build/device acceptance remain separate.
- If regressions occur, revert the coherent S7 source/docs commits; no persisted schema or workspace migration is involved.

## Next action
Implement the core postcondition contract, wire it into the shared engine and real adapters, add focused regression tests, update traceability/task/invariant/gap/build/handoff records, and request exact-head CI. Do not merge to main.
