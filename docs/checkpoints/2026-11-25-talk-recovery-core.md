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
- PR #22 was not merged because it was based on an older mainline (28 commits ahead, 4 behind) and contained a weaker continuation policy. Its relevant UI/ViewModel work was ported selectively onto PR #25, which passed integrated code-bearing CI #1213. PR #22 was then closed as superseded; no code from that branch was merged.

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
- Integrated exact-head CI run #1225 on `7638fd8e96b18b23c83fa01cfe430a461b49f367` passed: 154 core tests (including conversation-bound parent resolution), 38 Android unit tests, source verifier, remote-runner tests, debug/release lint, debug/release APK assembly and APK identity/signature checks.
- Emulator install-over-existing passed; both encrypted stores survived the release-package upgrade and both instrumentation acceptance phases returned `OK (1 test)`.
- Lint remained at the same 59 warnings and 5 hints as core-only baseline #1196; no new warnings were introduced by the continuation integration.
- Coverage comparison and dependency vulnerability scanning remain unverified; GitHub dependency graph was disabled in this CI environment.
- Physical-device acceptance: not run. Pull-request CI did not publish an APK/AAB.

## Rollback
Revert this branch's commits. The new field is additive and optional; older schema-7 backups without `parentRunId` continue to import as independent runs. No existing app data is rewritten by this source change until the app writes a new snapshot.

## Next step
Perform physical-device continuation and install-update acceptance. Then complete coverage/dependency security assessment and the remaining release gates. Keep the feature unreleased until device acceptance.

## 2026-11-26 — M3.RECOVERY-S4 — Atomic request/run start

### Finding
The continuation Talk path created and persisted a new RUNNING AgentRunRecord in `launchAgentExecution`, then appended the user's message in the execution block. A process death in that interval could leave a durable child run with no corresponding user request. The device checklist already required message-before-run ordering, but the code only guaranteed message persistence before the model/capability loop, not before the run record was created.

### Implemented
- Added a pure `AgentRunStartTransaction` in the platform-independent core module. It validates the conversation, goal, user message, run ID, history limits and (for continuation) the exact parent eligibility before preparing any changes.
- The transaction adds the USER message and new AgentRunRecord with parent lineage to one immutable AppSnapshot. It also updates the conversation title/entity using the existing chat behavior.
- The ViewModel persists that single snapshot and reads back the exact message and run record before invoking the execution block. The UI clears the draft and consumes continuation state only after that read-back succeeds.
- The execution block receives the exact persisted message ID; the previous second write through `appendChatMessageDurably` was removed from the Talk start path.
- Shared the 249-user-message / 250-total-message limits between normal chat append, serialization and the atomic start transaction.
- Added pure core tests for atomic preparation, linked parent lineage, unsafe-parent rejection without partial append, message-limit rejection and invalid/duplicate inputs. Extended the source verifier to prevent reintroducing a separate post-run message append.

### Compatibility and safety
- No encrypted workspace schema or backup schema bump, package ID, Keystore alias, signing identity, approval ledger or existing stored data change.
- The immutable source snapshot is unchanged on any rejected preparation. An AtomicFile write can leave the prior snapshot or the complete new snapshot; it cannot intentionally publish a run-only or message-only candidate.
- No capability is invoked until the combined snapshot has been read back and verified.

### Verification
- Source changes are committed to the existing PR #25 branch; exact-head CI is pending.
- Physical-device acceptance, coverage comparison and dependency vulnerability assessment remain open.
