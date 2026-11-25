# Memo recovery checkpoint — 25 November 2026

## Session identity
- Repository: `ysfclc/memo`
- Branch: `feature/talk-safe-continuation-core-2026-11-25`
- Base: `main` at `7cc5923c59dc18c12213e164bff9117b587492e9`
- Work package: `M3.RECOVERY-S1`
- Requirement: `MEMO-RECOVERY-001`

## User outcome
Establish a deterministic, fail-closed eligibility boundary and durable lineage for a future user-controlled continuation of an interrupted Talk run. A continuation must be a new run, not a replay or restart.

## Baseline inspected
- `AGENTS.md`, `DEVELOPMENT_GOVERNANCE.md`, `docs/AI_EXECUTION_INVARIANTS.md`, `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/DECISION_LOG.md`, `docs/UI_REGRESSION_CONTRACT.md`
- `TASKS.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `docs/checkpoints/2026-11-22-release-baseline-audit.md`
- `AgentRunRecord`, `AppSnapshot` serialization/size accounting, `AgentRunRecord.applyEvent`, Talk ViewModel durable run writer, Live run summary and Talk request assembly.
- PR #22 was not merged because it is based on an older mainline (28 commits ahead, 4 behind) and contains a weaker continuation policy. Its relevant UI intent was ported selectively onto this branch; the PR remains open until the integrated flow passes exact-head CI, then can be closed as superseded.

## Implemented in this increment
- Added pure `AgentRunContinuationPolicy` in the core module.
- Eligibility requires `NEEDS_REVIEW`, valid conversation/goal, no pending approval reference, consistent capability counters, no unverified started/failed invocation, and equal started/successfully verified call counts.
- Added optional `parentRunId` to the existing encrypted workspace run record without changing schema version 7.
- Included the field in size estimation, serialization, deserialization and backup validation.
- Added tests for eligibility, uncertain calls, invalid counters, approval references, run state and schema-7 backward compatibility.
- Added source-verifier guards for the fail-closed policy and every parentRunId persistence/compatibility boundary.
- Ported the Live → Talk continuation flow from PR #22 onto this current-mainline branch while retaining the stricter policy: eligibility reason, translated Continue action, parent lineage, editable draft and ViewModel revalidation.
- The continuation draft survives tab navigation and is consumed only after the user's prompt is durably appended. Switching conversations/backing out clears the pending continuation. Remote requests omit prior AGENT_EVENT details and saved-run metadata unless the separate workspace-context sharing permission is enabled.
- Added EN/PL/TR localization regression coverage and static source-verifier guards for the full cross-layer safety boundary.

## Safety boundary
- Eligibility is not authorization and does not grant a capability.
- No previous tool call ID, idempotency key or approval is reusable.
- No prior run is modified or automatically replayed.
- The later ViewModel/UI increment must recheck eligibility immediately before creating a new run and require fresh capability discovery and fresh approval.

## Verification
- Source changes are committed to this branch.
- Core-only exact-head CI run #1196 on `83e1443c983258ba0c67615a4d8eb4030d8afe5` passed 151 core tests, 37 Android unit tests, source invariants, remote-runner tests, debug/release lint and APK identity/signature checks.
- Emulator install-over-existing passed: both encrypted stores survived the release-package upgrade and instrumentation recovery/corruption checks passed.
- The integrated ViewModel/Live/Talk UI changes were committed after #1196 and await a fresh exact-head CI run.
- Lint reports 59 warnings and 5 hints in existing untouched files; coverage comparison and dependency vulnerability scanning are not available in this run.
- Physical-device acceptance: not run. Pull-request CI did not publish an APK/AAB.

## Rollback
Revert this branch's commits. The new field is additive and optional; older schema-7 backups without `parentRunId` continue to import as independent runs. No existing app data is rewritten by this source change until the app writes a new snapshot.

## Next step
Run exact-head CI for the integrated ViewModel/Live/Talk continuation flow. If green, perform physical-device continuation and install-update acceptance. Keep the feature unreleased until device acceptance and the remaining security/quality gates are satisfied.
