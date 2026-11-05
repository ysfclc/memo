## 28 October 2026 — exact-content external prompt approval

- Added pure-core `ActionApprovalLedger` with runtime/capability/destination/payload hash binding, expiring one-use approval, mismatch invalidation, audit-chain verification, process-death recovery and evidence-backed completion.
- Wired visible frontier website submissions to a full prompt/destination approval dialog before each send. The browser rechecks the live destination and exact prompt immediately before submission; uncertain outcomes are sent to review instead of automatically retried.
- Added workspace schema v4 fields with v1-v3 import support, plus JVM tests for approval transitions, restart/import behavior, hash mismatch, audit tampering and evidence requirements.
- Updated Polish/Turkish strings, feature audit and product-gap status. Structural checks pass; remote runner tests pass 12/12; targeted ledger behavior passes a standalone Kotlin harness. Gradle tests, Android compilation/lint, APK and real-device acceptance remain unverified because the pinned Gradle distribution could not be downloaded in this environment.

# Changelog

## 28 October 2026 — Memo Director's Cut reset

- Restored Memo's identity as a local-first personal execution environment rather than a model/browser-centric shell.
- Rebuilt the home around quick capture into an unsent conversation draft, connected records, source evidence, review attention, and resumed work.
- Renamed the active Compose application/viewmodel/theme design-system entry points to Memo-specific names.
- Added a JVM `:core` module for durable record schemas, deterministic task/outcome policy and runtime capability contracts, moving their pure tests alongside the domain code.
- Removed the duplicate, unlaunched legacy SynthOS Activity UI and introduced `MemoRuntimeBridge` for runtime-change and service-message delivery.
- Added runtime-listener hookup in Apps & automation. Preserved encrypted vault filenames and aliases for existing user data.
- Added the consolidated product contract/backlog in `TASKS.md` and updated static source checks.
- This increment has not been Gradle/device verified in the editing environment; CI remains the Android build gate.

# Changelog

## 2026-10-26 — Voice entry, local reminders, and whole-product audit

- Added permission-gated tap-to-talk input in chat. Speech transcripts enter the editable message draft; the feature does not send automatically or claim always-on listening.
- Added opt-in local daily reminder scheduling, workflow terminal-status notifications, generic/private lock-screen text, notification permission/settings handling, and reboot/time-change rescheduling.
- Added Polish/Turkish strings for voice and notification controls and tests for the language catalog.
- Added `FEATURE_AUDIT.md` and `PRODUCT_GAP_REGISTRY.md` to distinguish current source features from source-branch-only ideas, planned work, and unverified Android behavior.
- Release status remains unchanged: Android build/lint/device acceptance are not claimed until they pass.


## 2026-10-26 — Interface language settings

- Added a persisted interface-language preference with English, Polish, and Turkish options in Settings.
- Routed workspace UI text through the translation catalog while preserving unrecognized/user-entered text.
- Localized navigation, core settings, workspace entry points, workflow controls and lock-screen copy where catalog entries exist.
- Added tests for English fallback, Polish/Turkish core labels, and preserving unknown/user text.

## 4.0.0-memo — Integration build (2026-10-26)

- Consolidated the richer Compose workspace, browser-based frontier-AI workflows, encrypted typed work graph, model/Council capabilities and source-grounded outcome review into the Memo Android application ID and branding.
- Added **Apps & automation**: installed app inventory/launcher, revisioned workflow editor, run/history details, exact-text actions, final visible-text postconditions, explicit stop handling, Accessibility permission onboarding and manual floating capture entry points.
- Registered External Android as a separate runtime; its action capabilities are distinct from browser and in-app capabilities, and label taps explicitly require user initiation/approval.
- Added optional HTTPS guest runner configuration, encrypted bearer-token storage, connection checks, explicit per-workflow approval, guest status/cancel controls and companion Python tests.
- Updated build tooling to Gradle 8.14.6 and Kotlin 2.4.21, added native workflow policy tests, and embedded the GitHub workflow at `.github/workflows/build-apk.yml`.
- Corrected stale documentation that described native app inventory/automation as absent.

### Verification boundary

- XML resources/manifest parse successfully.
- Guest runner tests: 12/12 passed.
- Android Gradle build, Kotlin compilation, lint, emulator and device tests could not be run in the editing environment because no Android SDK/Gradle distribution is installed and network DNS resolution for `services.gradle.org` failed. They remain unverified; do not treat this integration source as a release-signed or device-validated APK.

### Known integration debt

- The primary Compose workspace vault and legacy native workflow vault remain separate encrypted stores.
- Browser workflow surfaces are not yet a generic versioned DOM schema executor.
- Cross-runtime dependency DAG execution, full app/web schema editing, file/media pipeline completion, channel publishing/engagement analytics and broad device model discovery still need implementation.

## 3.1.0 — Frontier browser workflows and source-grounded outcomes

Retained from the integrated source branch: visible browser-based frontier consultations, sequential multi-provider comparison, critique chains, provider-response entities, local output mapping and source-checked entity proposals.


## 28 October 2026 — central approval inbox slice

- Added a first-class Action approval inbox under More, with Action queue, Needs review and History filters, waiting/active/review counts, exact payload inspection, destination and content fingerprint, evidence reference and chained audit-event details.
- Pending proposals can be rejected from the inbox. Approvals are not granted from history: a proposal can only route back to the browser confirmation when its hash, destination and live prompt match exactly.
- Active/approved outcomes can be flagged for review. The UI explicitly warns that a review flag does not undo a submission already started; uncertain side effects are never automatically replayed.
- Tightened ledger import verification to reject orphaned audit events as well as broken chains/status mismatches; included audit event status and timestamp in the workspace size estimate.
- Structural checks and remote runner tests pass; targeted ledger logic passes an isolated `kotlinc` harness. Gradle, Android compile/lint/APK and device behavior are still unverified.


## 28 October 2026 — action inbox and global stop increment

- Added central Action approval inbox with queue, needs-review and history; exact payload/hash/destination inspection; safe routing only to matching live browser approvals; reject and mark-review actions.
- Added a top-level Stop active work control to request cancellation of local inference, browser work and current native accessibility automation. In-flight external actions are marked for review; optional guest-runner cancellation is reported as best-effort.
- Added structural verification for stop wiring and honest remote cancellation messaging.
- The product remains an in-progress source build: Gradle tests, Android compilation/lint, APK and device behavior remain unverified.

## 28 October 2026 — context execution, capability compilation, and encrypted backup

- Added bounded workspace retrieval for local task execution: conversations, approved records, prior jobs and evidence are ranked deterministically and passed with source identifiers and excerpts; remote prompts receive this context only after the explicit per-run opt-in.
- Added editable untouched draft plans, with regenerated checkpoints and an explicit external-action approval gate.
- Added a pure-core capability compiler that selects only registered capabilities and emits a deterministic SHA-256 manifest while preserving optional-runtime status and approval flags.
- Added Self Model numeric/date claim review prompts and retained human-review/versioning behavior for generated personal artifacts. The heuristic is intentionally narrow and is not represented as a general fact checker.
- Added an authenticated, passphrase-protected AES-256-GCM portable backup/restore flow with bounded parsing and wrong-passphrase/tamper rejection; plaintext JSON remains visibly separate.
- Added incremental remote response streaming and a connection-level cancellation path that keeps partial output visible.
- Verification: pure Kotlin helper compilation and targeted smoke harnesses pass for capability compilation, context selection/provenance, external-action safety, factuality warnings, and encrypted backup round-trip/tamper rejection. Android compile/runtime/device validation remains open; no M0–M6 milestone is marked complete.


## 2026-11-05 — Automation intent classification regression fix

- Corrected priority so explicit automation verbs are not swallowed by the generic `app`/BUILD keyword.
- Preserved BUILD classification for implementation requests such as building an Android workflow editor.
- Strengthened capability compiler regression tests to verify the selected Android runtime remains marked `OPTIONAL_PERMISSION` rather than enabled by classification alone.
- Isolated Kotlin/JVM behavior checks pass; full Gradle, lint and APK CI confirmation remains pending.

## 5 November 2026 — Android app Kotlin compile repair

- Resolved the eight compiler diagnostics from CI run #73: cross-module action smart-cast, missing Compose layout/icon imports, browser-session local declaration order, nullable conversation access and the Material 3 divider symbol.
- No workflow changes were required. Android compile, app tests, lint and APK assembly remain pending the next CI run.


## 5 November 2026 — Android notification permission safety

- Notification delivery now checks Android 13+ `POST_NOTIFICATIONS` runtime permission immediately before posting and safely ignores a permission-revocation race. The existing user-initiated permission request flow remains unchanged.
- This repairs the sole blocking lint error reported by CI run #74; the non-blocking warning/hint backlog remains open pending separate review.
