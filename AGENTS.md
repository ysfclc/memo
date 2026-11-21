# Repository agent constraints — Memo

These instructions apply to every human or AI contributor working in this repository.

## Read before changing anything

1. `docs/AI_EXECUTION_INVARIANTS.md` — binding agent/execution architecture constraints.
2. `docs/MEMO_PRODUCT_CONTRACT.md` — product vision and boundaries.
3. `DEVELOPMENT_GOVERNANCE.md` — Fresh Start, change control, verification and handoff.
4. `docs/REQUIREMENT_TRACEABILITY.md` and `TASKS.md` — stable requirements and active work.
5. `docs/DECISION_LOG.md` and the latest session checkpoint — decisions and current state.

## Hard stops

- **One intent path.** Every tool-using request from Talk must enter the same provider-agnostic execution loop and shared capability dispatcher. Do not build another route for a new example or modality.
- **Outcome, not category.** The user states what they want. Memo discovers the needed capabilities. Never turn examples into keyword domains, fixed workflows, templates, mandatory mode selectors or dedicated primary screens.
- **Model is not a tool.** Local and frontier models provide intelligence. Browser, Android, code, files, image, document/PDF, video and workspace operations are separate, real capabilities. A text response is not execution.
- **No phantom capabilities.** A registry entry must resolve to a callable adapter, validated schema, truthful readiness, policy and observable result. Never register a stub as available.
- **Plans are not execution.** TaskSpec, graphs and manifests must connect to the real dispatcher, run journal and result-observation loop before being described as operational.
- **One lifecycle.** Use shared authorization, approvals, event journal, cancellation, idempotency, recovery and verification. Do not introduce competing engines, stores or approval ledgers.
- **Prove outcomes.** Verify real files, artifacts, state changes, receipts or postconditions. Model claims, screens, mocks and green compilation do not prove user outcomes.
- **No scope drift.** Do not prioritize isolated entity CRUD, entity-page polish, specialist browser screens or other attractive secondary work while the central intent-to-outcome path is missing, unless the user explicitly requests that work.
- **Preserve boundaries.** Work only in `ysfclc/memo`; do not modify `memo-2`. Preserve app identity, encrypted data, keys, storage paths and signing assumptions. Never merge to `main` without explicit user approval.
- **No silent compromises.** Record blockers and partial implementation honestly. Do not drop prior requirements, fake progress, substitute manual steps without disclosure, or claim device acceptance without device evidence.

When instructions conflict, the user's latest explicit product decision prevails. The execution invariants are binding until the user changes them. A test, CI pass or implementation convenience cannot override them.
