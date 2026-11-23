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

- **UI regression lock.** Before changing Talk, Live, Work, Studio or shared layout primitives, inspect the last accepted UI behavior and its regression evidence. Preserve the page-level scrolling contract documented in docs/UI_REGRESSION_CONTRACT.md. Do not reintroduce independently scrolling nested content regions or fixed content above/below a partial scroll container where the contract requires one continuous page. A green compile/unit-test run is not UI acceptance. Any UI source import/snapshot must be diff-audited against the accepted baseline; never replace current source wholesale without a file-level regression review.
- **Release candidate integrity.** An APK is not releasable on build/signing success alone. The exact source commit and APK hash must be recorded; UI regression checks and the device acceptance checklist must be completed for the exact candidate. If physical-device verification is unavailable, label the APK unverified/test-only and do not describe it as a user-ready update. Never ask the user to install a candidate with known or unverified regressions.
