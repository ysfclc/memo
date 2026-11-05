# Memo Android — private operating environment

Memo is a local-first personal execution environment. Conversation is the easiest way in; durable records, relationships, source evidence, tasks and verified work live behind it. Browser sessions, local models and optional Android automation are capabilities around Memo—not separate sources of truth. Other project archives contribute bounded primitives only; they do not define Memo’s product shell. This archive is a development build, not a production-readiness claim.

## What is implemented in this source tree

### Interface language

Memo provides a persisted interface-language setting for **English, Polish, and Turkish** under Settings → Language. The same translation layer is used by the workspace, navigation, reusable UI components, and lock screen. It translates exact known interface labels and messages; all unmatched text passes through unchanged. User-authored conversations, evidence, prompts, imported text, and AI responses are not intentionally translated. A user string that exactly matches a short catalog label could also match the translator. Some specialist descriptions and provider-specific text may still appear in English until their UI copy is added to the translation catalog.

### One workspace

- Compose-based mobile workspace for persistent AI conversations, projects and typed entities, jobs/tasks, source evidence, browser workflows, Council/model flows, settings, and a new **Apps & automation** workspace.
- Android share-sheet intake and app-to-Memo capture intents; shared content remains source material until the user decides what to do with it.
- Local encrypted workspace vault with Android Keystore-backed AES-GCM encryption, import/export, biometric/device-credential unlock, model/provider settings, and explicit per-request consent before remote inference.
- Source-grounded outcome proposals: accepted proposals must cite an excerpt found in the captured source corpus; they remain reviewable until approved.

### Frontier-AI workflows use the real provider websites

- The browser-based workflow can consult selected signed-in frontier-AI sites in a visible WebView, capture each response, pass bounded earlier responses to later providers for critique/refinement, and use the configured local model as coordinator/synthesizer.
- Provider sessions are sequential and visible, not hidden simultaneous sessions. Before each prompt is sent, Memo persists a proposal and shows the full exact prompt plus destination in a confirmation dialog. The More menu also includes an action approval inbox for pending proposals, uncertain outcomes and completed/rejected history; stale records cannot be approved outside their matching live browser session. The approval is one-use and bound to the exact payload/destination; changed content requires reapproval. The visible browser send is blocked if the live destination differs. A saved response-evidence record is required before the action is marked complete; uncertain outcomes go to review, never blind replay. This is the first browser-specific safety slice, not yet a universal gate for every runtime.
- Captured provider responses retain the source URL/time and can become typed evidence/entities. The UI must pause for takeover when a provider site cannot be safely recognized.

### Voice, notifications, and language

- The interface language can be set to English, Polish, or Turkish. The translation catalog covers core navigation, controls, key workflow strings (including exact external-prompt approval), lock screen, chat voice entry, and notification settings. Localization is deliberately exact-string and incomplete: uncovered UI copy falls back to its original wording; user content and model output are not translated.
- Chat includes **tap-to-talk** speech input. Memo requests the microphone permission, opens Android speech recognition, and inserts the transcript into the editable draft. `EXTRA_PREFER_OFFLINE` is a preference only; actual offline recognition depends on the device's speech service.
- Settings provides optional **local** workflow status notifications and a daily reminder with a selected time. Notification bodies avoid private workflow names/content, and the user can test notifications or change permission in Android settings. Reminders use Android local alarms and can be delayed by battery policies. This package has no push-notification server or Firebase push delivery.
- Memo does **not** claim an always-listening wake word. Voice starts only after the user taps the microphone. A wake-word service needs a separate opt-in, visible microphone foreground service, on-device wake detector, explicit off control, and Android-version/device testing.

### Native Android app workflows

The **Apps & automation** workspace adds an installed-app inventory and launcher, a versioned native workflow editor, ordered steps, workflow history, local run trace, explicit run/stop controls, Accessibility permission setup, and optional floating capture controls.

Supported native step types are deliberately bounded: launch an app, tap a unique exact visible label, wait for visible text, press Back, and delay. Each workflow must end with a visible-text success condition. The Accessibility service avoids editable-field values/passwords; it pauses if the device locks or the target app leaves the foreground, and it will not automate password entry, payment confirmation, CAPTCHA/security bypass or ad/reward engagement. Passing the final text check verifies that the text was observed; it is not proof that every side effect succeeded.

The optional remote guest runner can be configured from Apps & automation. It requires a separately provisioned and paired runner, HTTPS, a bearer token of at least 32 characters, an explicit preview/approval, and the guest's own local policy. Only the bounded workflow definition is sent, not Memo conversations, prompts, attachments or model context. The runner's Python tests cover authentication, approval, policy bounds, durable interrupted-job status and final-postcondition verification.

### Versioning and execution safety

- Native workflow edits are archived as revisions; a run records/pins the workflow version it used. Browser provider sends now use the core `ActionApprovalLedger` and a hash-chained event history; the central approval inbox now covers the browser action ledger, while universal runtime adoption remains open.
- Workflows are policy-validated and bounded, with required final visible-text postconditions.
- Interrupted native/guest runs are not automatically replayed. Status remains reviewable and actions require user initiation.
- Outbound remote guest execution requires a preview and an explicit approval action. The bearer token is encrypted using Android Keystore in app-private preferences.
- The Apps workflow registry/run history is encrypted using the backward-compatible `SynthStore` vault. The main Compose workspace still uses its own `EncryptedVault`; **these data stores are not yet unified**. Imported/share-captured text is not silently merged into the work graph.

## Current product behaviors in this source pass

Memo now includes editable draft-to-checkpoint planning, deterministic capability manifests, bounded source-linked context retrieval across its saved workspace, local Self Model fact/evidence review, and passphrase-protected encrypted portable workspace backups. Compatible remote model responses stream incrementally and can be cancelled; partial output remains visible. These are integrated source slices, not a claim that the full end-state plan is complete. See `TASKS.md`, `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, and the mandatory `IMPLEMENTATION_CONTRACT.md` / `MEMO2_ALIGNMENT_MATRIX.md` / `IMPLEMENTATION_LOG.md` governance set.

## Architecture

- `:core` is a platform-independent Kotlin/JVM module for canonical records, task/job contracts, deterministic outcome validation, runtime capability descriptors, safe task rules, and pure unit tests. It does not import Android framework APIs.
- `:app` owns Android lifecycle/UI, encrypted persistence adapters, local model/provider adapters, visible browser sessions, notifications, and opt-in Android runtime integrations. The product shell is Memo's Compose UI; specialist runtimes do not create a second Activity tree.
- The package keeps existing encrypted storage file names and Android Keystore aliases intact for compatibility. The separate workflow/run vault is documented as migration debt rather than silently replaced.

## Build it

Requirements: JDK 17, Android SDK platform 35, Build Tools 35.0.0, Gradle 8.14.6 or the included wrapper, Android Gradle Plugin 8.13.0, Kotlin 2.4.21.

```bash
./gradlew --no-daemon --stacktrace :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug
```

Expected debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

The standalone GitHub Actions workflow is provided as `.github/workflows/build-apk.yml`. Use only the canonical `Memo-Android-Repository.zip` at the repository root so CI cannot accidentally build a stale alternate archive. The delivered archive contains one `Memo-Android/` project root. The workflow validates the ZIP and safe entry paths, verifies `SOURCE_SHA256SUMS.txt`, and requires exactly one matching Gradle project; it does not fall back to any alternate source archive. It runs offline source checks and the guest-runner tests, then requires Android unit tests, lint and debug APK assembly to pass. The APK artifact includes a SHA-256 checksum; test/lint reports are retained separately.

## Verification status

Verified in the available editing environment:

- All Android XML resources and the manifest parse as XML.
- The remote guest-runner Python suite passes 12/12 tests.
- Archive contents and Android source structure are inspected in the packaged source.

Not verified here:

- Android/Kotlin compile, Gradle unit tests, Android lint, APK generation, emulator tests and real-device/Accessibility behavior. This environment has neither an installed Android SDK nor a cached Gradle distribution; downloading Gradle failed because `services.gradle.org` could not be resolved. The provided GitHub Actions workflow is the build gate, not proof that the APK has built.

## Feature audit and gap registry

- `FEATURE_AUDIT.md` lists every supplied source build, the feature lineage, current source status and verification gate.
- `PRODUCT_GAP_REGISTRY.md` prioritizes additional capabilities inferred from the full product vision, including cross-runtime workflows, durable recovery, media/capture, content studio, CRM/job flows, remote push policy and optional voice wake-up.
- `tools/verify_source.py` performs offline source/package checks; it explicitly does not substitute for Android compilation or device acceptance.

## Explicit product gaps

This is a stronger integrated implementation, but not yet the complete end-state product. Still required before describing it as production-ready:

1. Successfully run Gradle build/unit tests/lint in GitHub Actions and install/test on at least one real Android device.
2. Unify the native workflow/run vault with the primary encrypted conversation/entity/job store, with a migration that keeps prior user data.
3. Add generic dependency-graph (DAG) execution across browser and app steps, durable checkpoints, idempotency keys and automatic recovery rules that never repeat uncertain external side effects.
4. Generalize browser surface schemas to versioned DOM/semantic maps with selectors, page-change detection, postconditions and replay tests—not just provider-specific scripts.
5. Add a reviewable versioned skill/tool/agent/schema registry, with run manifests pinning all relevant versions.
6. Add full file/image/media ingestion, robust streaming/progress rendering, editable schema tooling, and cross-channel publishing/analytics connectors with full artifact/destination approval.
7. Validate Accessibility behavior, overlay permission flows, background execution constraints, screen-lock handling, and manufacturer-specific Android behavior on-device. Android does not allow a normal app to arbitrarily operate other apps while the device is locked; the current runner is explicitly foreground/unlocked.
8. Expand local-model discovery and installation to provider/device APIs beyond the supported LiteRT-LM model format. Local model support does not mean every Android-installed model is directly accessible.
9. Add automated browser/app compatibility fixtures and privacy/security review, plus an actual signed release process before public distribution.

Memo uses a bounded executor and explicit human checkpoints rather than claiming universal app virtualization or unrestricted autonomous access.

## Product direction

The normative product contract and ordered backlog are in [`TASKS.md`](TASKS.md). It explicitly separates Memo’s own experience from optional borrowed runtimes and records what remains unverified.

## Repository handoff and source import

This project uses one complete source archive at `incoming/memo.zip` and one persistent GitHub Actions workflow at `.github/workflows/build-apk.yml`. The workflow is restricted to `ysfclc-patch-1`. It treats each upload as the complete source snapshot, validates the project and checksums, removes stale source files, runs the Android gates, updates `BUILD_STATUS.md`, `SOURCE_SHA256SUMS.txt` and `notes/imports/`, and commits integrated source only after required checks pass.

See [`docs/handoff/ZIP-IMPORT-OPERATIONS.md`](docs/handoff/ZIP-IMPORT-OPERATIONS.md) for the repeatable upload procedure and [`docs/handoff/SOURCE-SNAPSHOT-PROVENANCE.md`](docs/handoff/SOURCE-SNAPSHOT-PROVENANCE.md) for the exact source baseline and unresolved Patch1 compile regression.
