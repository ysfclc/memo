# Memo Decision Log

This log records material product and engineering decisions. Add dated entries; do not rewrite or delete superseded decisions. For a changed decision, record the prior entry and its replacement.

## 2026-11-09 — GOV-001: Repository is the durable source of project truth

- **Decision:** Product scope, requirement IDs, implementation state, engineering sequence, material decisions and session handoffs must be maintained in repository documents and linked to source/test evidence.
- **Reason:** Conversation-only context can be lost or misapplied across sessions. The repository must let a fresh contributor understand the intended product and current state without reconstructing private chat history.
- **Consequences:** Adopt DEVELOPMENT_GOVERNANCE.md, REQUIREMENT_TRACEABILITY.md and SESSION_CHECKPOINT_TEMPLATE.md. Chat history is supporting context, not the authoritative project ledger.
- **Status:** Active.

## 2026-11-09 — GOV-002: Requirements cannot silently disappear during rebaselining

- **Decision:** Stable requirement IDs and dated change records are required for material scope changes. Reductions to product scope or non-negotiables require explicit user approval.
- **Reason:** Prior product documentation did not preserve the complete social platform scope.
- **Consequences:** Social capabilities remain first-class requirements in SOCIAL_PLATFORM_PRODUCT_SPEC.md and the MEMO-SOCIAL requirement family. A deferral is not deletion.
- **Status:** Active.

## 2026-11-09 — GOV-003: Implementation claims require evidence at the correct level

- **Decision:** Distinguish specified, designed, in_progress, implemented, verified and device_verified. A UI, class, fixture, source scan, CI run and real-device test prove different things.
- **Reason:** Prevent partial slices and stale CI results from being presented as complete product behavior.
- **Consequences:** Every substantial work package records exact commit, changed files, commands, test outcomes, CI artifacts and device evidence where required. Unknown remains unknown until inspected.
- **Status:** Active.

## 2026-11-09 — GOV-004: Build in user-value vertical slices and keep approval boundaries

- **Decision:** Prefer one connected user outcome over disconnected screens or infrastructure-only work. Preserve exact-content/destination approval, audit and NEEDS_REVIEW recovery for consequential external actions.
- **Reason:** Memo is an execution agent over sensitive personal data and external accounts; reliability and user control are product features.
- **Consequences:** Every work package includes dependencies, security/privacy, failure states, acceptance and rollback/recovery. Production deployment, signing and destructive data changes remain approval-gated.
- **Status:** Active.

## 2026-11-12 — ORCH-006: Runtime output receipts are not postcondition proof

- **Decision:** A runtime output receipt must bind the exact workflow attempt, runtime/capability manifest, idempotency identity, timestamps, evidence ID and output hash. A receipt records only that output was returned and durably saved; it must retain an explicit `NOT_VERIFIED` postcondition state until an independent observer proves the task's declared condition.
- **Reason:** Memo must distinguish a model response from an achieved outcome. A saved draft can be useful evidence without being treated as completion or permission to continue through consequential actions.
- **Consequences:** The first local Work receipt is stored in existing evidence metadata and cross-checked against the per-node journal during workspace validation. Existing schema-7 records without receipt markers remain compatible. Browser/native adapters and independent postcondition observers remain separate follow-up work.
- **Status:** Active.



## 2026-11-15 — ORCH-007: shared runtime and postcondition contracts

- **Decision:** Every runtime adapter receives an invocation bound to the saved TaskSpec, exact runtime/capabilities, compiled capability-manifest fingerprint, active attempt and idempotency identity. It returns typed candidate output only; it cannot persist workspace state, grant approvals or complete a checkpoint.
- **Decision:** Workflow completion requires a separate registered, deterministic, read-only postcondition observer. Evidence must bind the exact attempt, TaskSpec fingerprint, declared postcondition, observer and separate persisted evidence record. Only `VERIFIED` can permit checkpoint completion; model/runtime success claims are not proof.
- **Reason:** Memo's product contract requires observable outcomes, safe recovery and exact approval boundaries across local models, browser and Android runtimes.
- **Consequences:** The shared contract and regression tests are implemented and passed exact-head CI #678. Concrete deterministic/browser/native adapters, cross-runtime budgets/cancellation and observer wiring remain open. The next increment is the deterministic app-runtime adapter.
- **Scope authorization:** The user explicitly requested one coherent Memo increment at a time on 15 November. This supersedes the earlier feature freeze only for reversible development on the stabilization branch; it does not authorize merge, release, production deployment, signing identity changes or destructive migration.
- **Status:** Active.
