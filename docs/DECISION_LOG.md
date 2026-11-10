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
