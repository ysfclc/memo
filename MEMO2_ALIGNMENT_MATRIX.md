# Memo 2 → Memo 1 scope alignment matrix

This matrix keeps Memo 2's ambitious product scope intact while requiring Memo 1-specific implementation and evidence. It is a scope ledger, not a claim that the listed features already work.

| Workstream | Blueprint reference | Memo 1 execution plan | Acceptance evidence required | Status at plan creation |
|---|---|---|---|---|
| Source reconciliation / product baseline | Memo 2 `TASKS.md` Part 0; `BUILD_PLAN.md` §0 | `TASKS.md` P1-0.1–P1-0.8 | Canonical ZIP identity, inventory/diff, storage map, baseline CI and source-to-requirement ledger | Open until recorded |
| Core domain, workflow engine, recovery | M0; architecture §2 | `TASKS.md` M0 | Core/JVM tests, durable restart/recovery tests, audit and gate invariants | Must be independently verified in Memo 1 |
| Policy, approvals, audit, privacy, kill switch | M0; architecture §§2a, 6–7 | `TASKS.md` M0, cross-cutting gates | Negative tests for bypass, changed hash, wrong target, uncertain send, stop and redaction | Must be independently verified in Memo 1 |
| Vault, migration, backup and app lock | M0.C1/C2; M6.2–M6.4 | `TASKS.md` M0/M6 | Existing-data preservation, migration rollback, encryption/integrity and restore tests | Must be independently verified in Memo 1 |
| Local model hub and inference | M1.A1–A3, M1.B1–B3 | `TASKS.md` M1 | Supported format/revision/hash, real load/inference/stream/stop, memory failure and restart tests | Must be independently verified in Memo 1 |
| Chat and conversation state | M1.C1/C2 | `TASKS.md` M1 | Streaming, cancellation, regenerate, model picker, persistent history and truthful failures | Must be independently verified in Memo 1 |
| Local speech, voice UX and screen-off entry | M2.A1–A3, M2.B1, M2.C1–C3 | `TASKS.md` M2 | On-device ASR/TTS tests, interruptions, permissions, explicit visible controls and device acceptance | Must be independently verified in Memo 1 |
| Design system and motion | M2.B0a–d, M2.5.A2/A3 | `TASKS.md` M2.B0 and M2.5 | Shared tokens/components, accessibility/reduced motion, real performance measurements | Must be independently verified in Memo 1 |
| Dynamic planning and relevant personal context | M3.A1–A4 | `TASKS.md` M3 | Typed plans, context provenance/scope, bounded tool loop, resumability and user approval | Must be independently verified in Memo 1 |
| Artifact-first approvals and live status | M3.B1–B3 | `TASKS.md` M3/M4 | Real populated artifact, hash-bound gate, event/evidence timeline, pause/stop/takeover | Must be independently verified in Memo 1 |
| CRUD, preferences, language and daily utilities | M3.5, M3.6, M3.7 | `TASKS.md` M3.5–M3.7 | Real edits persisted, EN/PL/TR workflow fixtures, scheduling/reboot/permission tests | Must be independently verified in Memo 1 |
| Capability Compiler | M3.8 / BUILD_PLAN §2k | `TASKS.md` M3.8 | Requirement-to-capability selection, schemas, versioning, tests and inspectable rationale | Must be independently verified in Memo 1 |
| Self Model and Artifact Studio | M3.9 / BUILD_PLAN §2l | `TASKS.md` M3.9 | Source-grounded personal facts, provenance, fabrication guards and editable artifacts | Must be independently verified in Memo 1 |
| Browser runtime and frontier providers | M4.A1–A3, M4.C2, BUILD_PLAN §2a–b | `TASKS.md` M4 | WebView fixture automation, profile isolation, exact approval envelope, postconditions and takeover | Must be independently verified in Memo 1 |
| Native app automation and discovery | M4.B1/B2/B3, M4.C1, M4.D1/D2 | `TASKS.md` M4 | Wrong-app/lock/password refusal, accessibility/OCR fixtures, permission/kill-switch checks | Must be independently verified in Memo 1 |
| Teach & Select, recorder, schema healing | M4.E0–E6 / BUILD_PLAN §2m | `TASKS.md` M4 | Fixture selection, no password capture, stable locators, replay and approved healing | Must be independently verified in Memo 1 |
| Council / multi-model debate | M4.A4a–d / BUILD_PLAN §2n | `TASKS.md` M4 | Role relevance, source/claim ledger, critique, anti-sycophancy and privacy tests | Must be independently verified in Memo 1 |
| Live execution surface | M4.L1–L4 / BUILD_PLAN §2o | `TASKS.md` M4 | Real target highlights/timeline, live preview, touch auto-pause, stop and takeover | Must be independently verified in Memo 1 |
| End-to-end product workflows | M5.A1, M5.B1, M5.C1/C1a/C1b, M5.C2 | `TASKS.md` M5 | E2E evidence for brainstorm, CRM, jobs, content+metrics and generic plans | Must be independently verified in Memo 1 |
| Hardening and release | M6.1–M6.9; M2.5 | `TASKS.md` M6 | Strict lint, test sweep, secure backup, diagnostics, release provenance, artifact SHA and review | Must be independently verified in Memo 1 |

## Personal-context acceptance (cross-cutting)

Memo should improve usefulness from the user's context without becoming a hardcoded biography or exposing private context by default. Verify that:

- Projects, goals, decisions, preferences, source files and unfinished tasks are stored as inspectable records with provenance and user-editable corrections.
- Context retrieval is task-relevant, bounded and privacy-scoped; each output can show what evidence informed it.
- Personal facts in generated CVs, outreach, plans and content are supported by saved/user-provided evidence; unknowns are asked about or omitted, never invented.
- The user can inspect, edit, delete, export and restrict personal context; external sharing requires a clear preview/consent gate.
- The system can resume a project from prior decisions and next steps without forcing the user to restate the whole context, while showing uncertainty when history is incomplete.

## Rule for status updates

Only update a row to complete when `TASKS.md` points to Memo 1 source paths and specific passing tests/CI/device evidence. A feature can be implemented but unverified; keep those states distinct. If Memo 1 architecture differs from Memo 2, record the adapter/migration decision instead of silently skipping the requirement.

## Latest implementation record — 2026-10-28

The following rows now have Memo 1 source slices in addition to the initial plan. This is a status note, not a blanket acceptance mark; complete gates still require the test/runtime evidence listed in each row above.

| Workstream | New source slice | Current status and boundary |
|---|---|---|
| Dynamic planning and relevant personal context | `WorkspaceContextRetriever.kt`, Work prompt integration, editable `IntentPlanner` drafts | **Partial source implementation.** Bounded deterministic relevance/provenance is present; no embedding/vector retrieval or full tool loop yet. |
| Capability Compiler | `CapabilityCompiler.kt`, `CapabilityCompilerTest.kt`, hashed manifest added to draft plan | **Partial source implementation.** Selects registered capabilities and exposes status/missing requirements; no unified versioned schema registry or per-executor revision pinning. |
| Self Model and Artifact Studio | `SelfModelFactualityGuard.kt`, Studio generation path and evidence references | **Partial source implementation.** Numeric/date claims are heuristically flagged; this is not a comprehensive factuality system. |
| Vault, migration, backup | `EncryptedBackupCodec.kt`, encrypted export/restore UI and codec tests | **Partial source implementation.** Authenticated AES-GCM backup and fail-closed decrypt; restore preview, rollback strategy and device acceptance remain open. |
| Chat and conversation state | `OpenAiCompatibleClient.completeStreaming`, `cancelStreaming`, `AiRuntimeViewModel` progress/cancel wiring | **Partial source implementation.** Compatible remote responses stream and cancellation attempts connection disconnect; complete device/lifecycle coverage remains open. |

The implementation and smoke-test evidence is appended in `IMPLEMENTATION_LOG.md`. Do not change any matrix row to complete until its gate has specific Memo 1 evidence.
