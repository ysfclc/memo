# Memo System Architecture

**Status:** historical source-based implementation map, 10 November 2026; not product authority or a current implementation-status report. It records inspected boundaries and known limitations at that snapshot. The user's latest directions in [USER_INPUT_LEDGER.md](../USER_INPUT_LEDGER.md), [AGENTS.md](../../AGENTS.md) and [MEMO_PRODUCT_CONTRACT.md](../MEMO_PRODUCT_CONTRACT.md) govern product scope; current implementation truth is tracked in [STATE.md](../STATE.md), [TASKS.md](../../TASKS.md) and exact-head CI evidence. Optional domain designs below do not define Memo's core product or authorize their implementation.

## Product shape

Memo is one Android application and one user-owned workspace. Talk is the intent and capture surface; Work owns durable records and plans; Live observes and controls execution; Studio prepares editable artifacts. Browser, local-model, remote-runner and native Android capabilities are adapters around the product shell, not separate databases or parallel Activity trees.

## Runtime and module boundaries

| Layer | Canonical location | Responsibility | Boundary |
|---|---|---|---|
| Android composition root | `app/src/main/java/app/memo/android/MainActivity.kt`, `ui/MemoWorkspaceApp.kt` | App lifecycle, Compose navigation, permissions and UI | Android-only; domain rules should not live only in composables |
| Platform-independent core | `:core` | Canonical records, validators, outcome parsing, context retrieval, workflow graph, capability compiler, runtime descriptors and approval ledger | Kotlin/JVM; no Android framework imports |
| Android application services | `app/.../data`, `ai`, `notifications` | Encrypted vault adapter, ViewModel, model/provider adapters, notifications | Android framework and OS APIs live here |
| Browser runtime | `app/.../browser/BrowserSession.kt`, `BrowserWorkflow.kt`, `BrowserScripts.kt` | Visible WebView sessions, generic HTTPS navigation/page inspection/evidence capture, plus optional provider-specific prompt preparation/submission and response evidence | User-visible; generic read tasks are site-independent, while chat interaction remains a provider-specific adapter; no hidden browser or assumed API parity |
| Native app runtime | `app/src/main/java/com/synthos/runtime/engine` | Foreground/unlocked Accessibility workflows and visible-label postconditions | Optional Android permission; actions remain bounded and fail closed when policy/approval is missing |
| Remote guest runner | `remote-runner/server.py`, `app/.../RemoteRunnerClient.kt` | Separately paired HTTPS worker for a bounded workflow definition | Receives only the approved workflow payload, not the full workspace or model context |
| Build and supply chain | `.github/workflows/build-apk.yml`, `tools/verify_source.py` | Source validation, tests/lint, APK assembly and isolated signing | Build/test receives no signing secrets; signing receives the APK artifact only; write-only handoff does not execute project code |

## Canonical data ownership

The existing `:core` records are the product data model:

- `AppSnapshot` owns the workspace snapshot and bounded serialization.
- `ConversationRecord` and `ChatMessageRecord` retain conversation history and exact source-message identity.
- `EntityRecord` and `EntityRelationship` form the typed, linked Work graph for projects, tasks, people, organizations and artifacts.
- `WorkJobRecord` and `JobStepRecord` represent plans/jobs and checkpoint state.
- `EvidenceRecord` stores captured material with source URL, capture time, kind, content, summary and metadata.
- `ActionApprovalRecord` and `ActionAuditEvent` are owned by `ActionApprovalLedger`; they preserve the exact payload hash, destination, approval/transition status and audit history.

The Android `EncryptedVault` persists the main workspace snapshot using Android Keystore-backed encryption. The native workflow registry/run history currently uses a separate `SynthStore` file and key/storage path. This is known migration debt: do not silently combine or rename either store. A future unified-store change needs an explicit backward-compatible migration and install-over-existing tests.

## Control and execution flow (implementation model)

This sequence is a design/implementation model from the dated source snapshot, not a user-approved fixed architecture or proof that a complete orchestrator exists.

1. Talk captures the user's intent or source material; capture is not permission to act.
2. A bounded planning/proposal path may propose work. The capability compiler selects only registered capabilities whose requirements and permissions are satisfied. Current Talk can save a validated read-only workflow draft; a generic cross-runtime DAG executor is not yet complete.
3. Work persists the job/steps and source links. Runtime adapters advertise what they can really do; a platform name or UI button is not evidence of an integration.
4. Before a consequential external side effect, the executor must use the shared exact-scope approval contract: destination/account, capability, exact payload and hash. Any material payload change invalidates approval.
5. The selected runtime executes visibly and within its bounds. A timeout or interrupted in-flight side effect is `NEEDS_REVIEW`, not an automatic retry.
6. Completion requires a saved source/result receipt or independently verified postcondition. A model's statement alone cannot mark an action successful.
7. Live exposes progress, blockers, stop/takeover and evidence. Work records retain the durable outcome.

The browser action ledger is an implemented source slice, not proof that every runtime uses a universal gate. Native high-risk actions remain constrained by `WorkflowPolicy`; the separate native-approval workstream remains under review and is not promoted as verified behavior here.

## Optional Sales Operations scenario pack (not Memo core architecture)

This section preserves a prior domain-design example. It is optional, user-specific scenario material, not Memo's product definition, default architecture or active build priority. It does not authorize outbound/cold-contact automation. Select it for implementation only when the user separately asks for this domain capability. It can reuse the existing record and evidence contracts:

- **Account:** typed projection over `EntityRecord(type = "ORGANIZATION")`; do not add a second CRM database.
- **Contact:** a `PERSON` entity only when personal business data is necessary and lawfully sourced. The first account-research slice does not require contact records.
- **Signal/source fact:** retain as `EvidenceRecord` with source URL, retrieval time, source title, claim kind, confidence and a link to the account entity. Keep source excerpt/raw content separate from generated interpretation.
- **Opportunity:** an entity/job relationship with explicit confirmed-problem, client-statement and hypothesis distinctions. Do not invent budget, value, timing or probability.
- **Permission/suppression/approval:** a separate policy record; a public email address, phone number or profile does not create marketing consent.
- **Engagement/delivery:** links approved scope, client-authorised data boundary, tasks, deliverables, milestones and acceptance evidence.

A previously proposed first slice (M7.A1 / MEMO-SALES-001) was read-only: create/reuse an account, attach source evidence, calculate transparent factor-level fit, detect possible duplicates without destructive merge, and ask the operator to accept/reject. This proposal is inactive unless the user explicitly selects the scenario. No email, DMs, connection requests or contact forms are authorized by the general client-discovery example.

## Quality and release boundaries

- Keep schema versions and backward-compatible JSON parsing in `:core`; every persisted extension needs round-trip and legacy-fixture tests.
- Keep UI controls thin: loading/error/empty state and user decision in Compose, validation/scoring/policy in pure core code.
- Keep remote data sharing opt-in and previewed. Do not put secrets, tokens or cookies in entities, evidence, logs or source.
- Test duplicates, missing sources, unknown ICP factors, suppression, changed payloads, idempotent retries, uncertain sends, interrupted jobs and legacy backup restoration.
- CI build/test must use checked-out repository source. Only the isolated signing job may receive the pinned upload keystore, and only after required checks pass.
- Emulator upgrade acceptance is distinct from physical-device acceptance. Physical-device acceptance remains open until recorded on the user's phone.

## Known limits at this baseline

- The main workspace and native workflow stores are separate; there is no proven migration to one transaction boundary.
- A universal cross-runtime durable DAG executor, idempotent recovery, shared Live timeline and global pause/takeover remain incomplete.
- Social provider connections, real feeds/inbox/publishing/analytics and CRM-style customer acquisition are specified, not verified integrations.
- Local-model discovery/provider/device coverage and Accessibility behavior still require device acceptance.
- The 8 November APK build is evidence for commit `c4cfba8`, not every later feature branch or integration commit.
