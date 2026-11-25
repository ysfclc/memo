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
- PR #22 was inspected but not reused: it is based on an older mainline, diverges by 28 commits and is 4 commits behind; its milestone label collides with existing M3.A4-S7 and its integration has no exact-head verification on current main.

## Implemented in this increment
- Added pure `AgentRunContinuationPolicy` in the core module.
- Eligibility requires `NEEDS_REVIEW`, valid conversation/goal, no pending approval reference, consistent capability counters, no unverified started/failed invocation, and equal started/successfully verified call counts.
- Added optional `parentRunId` to the existing encrypted workspace run record without changing schema version 7.
- Included the field in size estimation, serialization, deserialization and backup validation.
- Added tests for eligibility, uncertain calls, invalid counters, approval references, run state and schema-7 backward compatibility.

## Safety boundary
- Eligibility is not authorization and does not grant a capability.
- No previous tool call ID, idempotency key or approval is reusable.
- No prior run is modified or automatically replayed.
- The later ViewModel/UI increment must recheck eligibility immediately before creating a new run and require fresh capability discovery and fresh approval.

## Verification
- Source changes are committed to this branch.
- Exact-head CI: pending.
- Local Gradle/Android build: not run; no `/testbed` checkout, Gradle project or Android SDK is available in this runtime.
- Physical-device acceptance: not run.
- No APK/AAB produced.

## Rollback
Revert this branch's commits. The new field is additive and optional; older schema-7 backups without `parentRunId` continue to import as independent runs. No existing app data is rewritten by this source change until the app writes a new snapshot.

## Next step
Port the ViewModel revalidation and Live → Talk reviewable draft UI onto this branch, then add ViewModel/UI regression coverage and run exact-head CI. Keep the feature unreleased until physical-device acceptance.
