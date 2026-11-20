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


## 2026-11-20 — ARCH-001: One intent-driven execution engine across providers and capabilities

- **Decision:** Memo has one outcome-first Talk entry path and one shared, bounded execution lifecycle/dispatcher for all tool-using requests. Local and frontier models are interchangeable intelligence providers subject to explicit data permissions. Browser, Android, code, files, image, PDF, video, workspace and future operations are separate real callable capabilities. The engine selects capabilities dynamically, validates policy, invokes them, observes results, verifies postconditions, and continues/re-plans or stops.
- **Hard constraints:** Examples are acceptance tests, not hardcoded domains or dedicated workflows. A capability is advertised only when a real adapter can execute it. Plans, TaskSpec graphs, manifests and model-generated text are not execution. Deterministic code owns permission, risk, approval, audit, budgets, cancellation, idempotency and recovery. Actual results must be observable and durable.
- **Reason:** Current Talk generates text directly, while the keyword planner creates static draft plans; typed graph/parser/compiler foundations are not connected to a shared executor. This does not deliver the user's stated operating-agent experience.
- **Consequences:** `docs/AI_EXECUTION_INVARIANTS.md` is binding; root `AGENTS.md`, `DEVELOPMENT_GOVERNANCE.md`, `IMPLEMENTATION_CONTRACT.md`, Product Contract, TASKS and requirement traceability enforce it. No new domain-specific engine, workflow or screen may be used to avoid the common architecture. Existing bounded implementations must be migrated behind adapters with regression coverage.
- **Scope:** This decision does not require every task to use an LLM, does not require multi-agent orchestration by default, does not grant arbitrary tool access, and does not claim all modalities/providers already exist.
- **Status:** Active; changes to these invariants require the user's explicit product decision.

