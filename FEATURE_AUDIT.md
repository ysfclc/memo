# Memo product feature audit

Audit date: 28 October 2026 (updated for exact-content approval and inbox slice)  
Scope: every supplied build/archive reviewed for integration potential, plus the current `Memo-Android/` source tree.

## How to read the statuses

- **Source present** — identifiable implementation exists in the current Memo source. This is not the same as a successful Android build or real-device validation.
- **Partial** — a useful subset is implemented, but there are meaningful boundaries or missing paths.
- **Source branch only** — a feature exists in one of the supplied projects but is not integrated into the current Memo app.
- **Not implemented** — the current Memo project does not yet implement it as a functioning end-to-end capability.
- **Verified** — the specific check named in the evidence column passed in this environment.
- **Not verified** — Android compilation, APK generation or device behavior could not be confirmed here.

Never infer runtime success from a button, screen, source file or a passing Python-only test.

## 0. Current implementation increment (2026-10-28)

| Capability | Current source behavior | Still not claimed |
|---|---|---|
| Intent → checkpoint plan | `IntentPlanner` creates a durable DRAFT plan; `reviseIntentPlan` regenerates untouched drafts; external writes get a separate approval checkpoint. | Not a general cross-runtime DAG and does not execute actions by itself. |
| Capability compilation | `CapabilityCompiler` maps domains to registered capability IDs and produces a deterministic hash manifest; missing capability IDs and optional runtime status remain visible. | Not an editable, versioned skill/tool registry and not yet pinned across all real executor runs. |
| Context retrieval | `WorkspaceContextRetriever` ranks local saved records deterministically with provenance IDs and a bounded context budget; used in Work execution prompts. | Not a semantic vector index, and retrieved records remain untrusted source material. |
| Self Model Studio | Confirmed facts and immutable source evidence feed personal artifact generation; numeric/date heuristic flags claims not matched to approved source text. | Heuristic is not a general fact checker and every output still requires user review. |
| Portable encrypted backup | Encrypted export/restore UI backed by a versioned AES-256-GCM envelope; wrong passphrase/tampering fail closed before snapshot replacement. | Restore preview, migration rollback, device verification and signed-release testing are still open. |
| Streaming/cancellation | Remote compatible chat responses stream incrementally; active connection can be disconnected and partial output remains visible. | Remote API compatibility, lifecycle races, local model behavior and Android device experience still require integration/device tests. |

## 1. Source builds examined

| Supplied build | What it contributed / why it matters | Current integration status |
|---|---|---|
| `synthos-fieldwork - 2.zip` | Richest Fieldwork branch: Compose workspace, persistent chats, encrypted work graph, tasks/jobs, evidence/provenance, Council strategies, local-model boundary, browser provider workflows, and source-checked proposed outcomes with tests. | **Historical code lineage only** for parts of the current Compose/model/browser implementation. It is not Memo’s product contract. Continue extracting bounded primitives into Memo-owned interfaces; avoid copying complete screens or app shells. Some data/runtime systems remain separate. |
| `synthos-fieldwork.zip` | Earlier, smaller Fieldwork branch with browser session/tasks, model client, typed evidence/entities, and core Compose UI. | **Superseded source reference**, selectively retained; do not count as a separate feature set in the delivered app. |
| `synthos-pocket.zip` | Capture-first home/inbox, immutable original capture text, attach sources to mission context, multi-file share intake, encrypted context selection, one-tap LiteRT-LM model onboarding, and external handoff/return flow. | **Partially integrated**. Current Memo supports text/share capture and local models, but does not yet reproduce Pocket's full attachment-aware immutable capture pipeline or mission context contract. |
| `threadline-vo.zip` | Voice-first UX, speech-to-text and TTS patterns, local reminders/daily nudges, floating capture bubble, local model download/rollback/checksums, encrypted backup primitive, private/public-safe Studio concepts and focus plans. | **Selected patterns reused**: tap-to-talk input, floating entry/capture concepts and local notification design. A first passphrase-protected backup/restore flow is now integrated (see current increment table); restore preview, rollback, TTS, always-listening wake word and a complete publishing Studio remain incomplete. |
| `threadline-app.zip` | Earlier Threadline assistant, floating assistant, accessibility service, catalogue/source mirror workers, embedded web content. | **Reference only**; not a second runnable module in Memo. Keep its model-sync/assistant features marked as branch-only unless present in current Memo source. |
| `app-release.zip` | SynthOS local-only runtime, LiteRT-LM catalogue, local Council, guest runner, bounded native app actions, workflow/run stores, and explicit whole-product scope/status. | **Selected runtime and policy ideas reused**, especially bounded native steps/guest runner. Its local-only AI policy differs from Memo’s explicit per-run consent model for visible external frontier websites; the Memo product follows the latter. |
| `memo-v2-prototype.zip` plus supplied `index.html` / `explainer.html` | Product explanation and UI/interaction prototype for the broader Memo idea. | **Concept/prototype only**. No screen or feature described only in the HTML prototype is counted as an integrated working capability. |
| Current `Memo-Android/` source tree | Consolidation of the selected features plus native workflow automation, testable guest runner, language selector, voice input and local notifications. | **Current source of truth**. Android build and device behaviors still need the release gates listed below. |
| `Memo_Architecture_Design_Blueprint.md`, `.docx`, and design-pack ZIP | Product contract, unified record/source graph, local-first privacy, capability interfaces, action approval/hash binding, durable checkpoints, evidence/postcondition protocol, safe recovery, mobile keyboard/accessibility, and staged CI/device acceptance gates. | **Requirements and acceptance reference**; checked against the implementation and reflected in the feature audit/gap registry. It is architecture documentation, not additional runnable source code. |

## 2. Feature-by-feature verification ledger

| Feature | Current status | Evidence / acceptance criterion |
|---|---|---|
| Native Android shell and Compose navigation | Source present; Android build not verified | `MainActivity.kt`, `ui/MemoWorkspaceApp.kt`. Verify cold start, navigation, keyboard/IME and rotation on device. |
| English / Polish / Turkish interface option | Source present, localization partial | `MemoLocalization.kt`, persisted `ui_language`, settings and tests. A static scan found 283/283 literal `Text()` strings in the six scanned Compose UI files represented in the translation catalog after excluding URLs/placeholders; dynamic labels, custom component strings, system dialogs and device testing still require review. Run JVM tests and manually inspect every screen in PL/TR. |
| Dark / light / system theme | Source present | Theme preference and Compose theme. Device validation needed. |
| Persistent chats and message history | Source present | `MemoWorkspaceViewModel`, `EncryptedVault`, `WorkspaceScreens.kt`. Test force-stop/reopen, import/export and failure paths on device. |
| Encrypted primary workspace | Source present; security review not complete | `data/EncryptedVault.kt`; AES-GCM with Android Keystore. Separate from the legacy `SynthStore` native-workflow vault, so there is not yet one unified transaction/migration boundary. |
| Typed entities, relationships, evidence and source-grounded proposed outcomes | Source present, partial | `data/Models.kt`, `OutcomeParser.kt`, `MemoWorkspaceViewModel.kt`. Proposals validate evidence before review; this is not yet a full CRM with organization/person enrichment and duplicate-review workflows everywhere. |
| Task/job records and follow-up task flow | Source present, partial | Models/view model and visible jobs. No general dependency graph across all runtimes. |
| Local model runtime / compatible LiteRT-LM files | Source present, model/device behavior not verified | `LocalModelRunner.kt`, `ModelCatalog.kt`, `AiRuntimeViewModel.kt`. Not every installed Android model is accessible; Edge Gallery discovery is not integrated. |
| Optional API-compatible remote provider | Source present, per-request consent | `OpenAiCompatibleClient.kt`, `ProviderSettingsStore.kt`. Test denial, timeout, malformed response and credential storage on device. |
| Visible frontier AI website workflow | Source present, provider-specific | `BrowserWorkflow.kt`, `BrowserSession.kt`, `BrowserScripts.kt`, `BrowserWorkflowScreen.kt`. Sequential visible signed-in web sessions with source capture; not a universal DOM automation engine and cannot bypass sign-in/CAPTCHA/provider restrictions. |
| Multi-provider Council / compare and critique | Source present, incomplete orchestration | Local Council/runtime and browser-based provider workflow both exist, but they are not one universal dependency-DAG engine that can freely mix every provider, app and tool. Preserve disagreements and failures; do not describe as hidden parallel access. |
| Native app inventory/launch | Source present; device behavior not verified | `AppsWorkspaceScreen.kt`, installed-app query path. Verify package visibility on Android 11+ and behavior with launchable apps. |
| Bounded native app control | Source present, bounded/unverified | `AutomationAccessibilityService.kt`, `WorkflowPolicy.java`, `WorkflowStep.java`. Supports declared limited actions and visible-text conditions; requires user-enabled Accessibility and a foreground/unlocked phone. It is not unrestricted control of arbitrary apps. |
| Native workflow revisions / run history | Source present, partial | Native workflow data store records revisions/run status. This is not a fully version-pinned manifest for every browser selector, skill, tool, provider or model. |
| Explicit review before guest execution | Source present; real pairing not verified | `RemoteRunnerClient.kt`, guest settings/approval UI and Python runner. Needs a paired-device HTTPS acceptance test. |
| Exact-content approval for visible frontier chat submissions | **Source implementation added; Gradle/build/device verification pending** | `core/.../ActionApprovalLedger.kt`, `BrowserSession.kt`, `BrowserWorkflowScreen.kt`, schema-v4 workspace persistence, and `ActionApprovalLedgerTest.kt`. Each provider prompt is shown in full; approval binds runtime, capability, URL destination and exact payload hash; stale/changed approvals are invalidated; action completion requires a saved `EvidenceRecord`. A new central inbox exposes queue, review and history, shows the full payload plus the latest audit events, allows rejection and moves uncertain/active actions to review. It only routes a still-matching pending action back to the live browser dialog; it cannot approve saved history. Other runtimes still do not use this shared gate. |
| Top-level Stop active work | **First source slice added; runtime/device verification pending** | Header stop requests local AI cancellation, browser workflow/task cancellation and AccessibilityService cancellation; pending proposals are rejected and approved/in-flight actions move to `NEEDS_REVIEW`. Nonterminal guest jobs get a best-effort cancel request with an explicit unconfirmed outcome. A durable cross-runtime cancellation token and device/race tests remain open. |
| Central action approval inbox | **Source implementation added; build/device verification pending** | `ui/ActionApprovalInboxScreen.kt`, `MemoWorkspaceApp.kt`, `ActionApprovalLedger.kt`. Queue, needs-review and history views expose exact payload/hash, destination, runtime/capability, event trail, reject and mark-review actions. Only the current live prompt can be routed to the browser confirmation. No approve-from-history or automatic replay. |
| Remote guest runner | **Verified: Python tests 12/12**; Android client not verified | `remote-runner/server.py`, `remote-runner/tests/test_runner.py`. It is optional and separately provisioned; do not imply a cloud service is hosted for the user. |
| Voice input in chat | Source present, tap-to-talk | `WorkspaceScreens.kt`, `RECORD_AUDIO` permission and platform speech recognizer. Transcript is inserted into the editable draft. Offline behavior depends on Android's recognizer; `EXTRA_PREFER_OFFLINE` is a preference, not a guarantee. |
| Spoken wake-word / always-listening command | **Not implemented** | No always-listening microphone service or wake-word model. Android restricts microphone foreground services from background starts and applies while-in-use rules; implement only with a clear opt-in, persistent privacy UI, tested wake engine and device/OEM validation. See [Android background FGS restrictions](https://developer.android.com/develop/background-work/services/fgs/restrictions-bg-start). |
| Local workflow-completion notifications | Source present; device behavior not verified | `MemoNotificationCenter.kt` and `AppsWorkspaceScreen.kt`. Workflow alerts default off until the user enables them. Generic private notifications depend on Android notification permission and app settings. |
| Daily local reminder/nudge | Source present; device behavior not verified | User-controlled time, local AlarmManager, reschedule after reboot/time/time-zone change. Battery policy can delay it; it is not a server push. |
| Remote push notifications | **Not implemented** | No FCM setup, push backend, account/paired-device infrastructure or end-to-end push delivery. This is deliberately distinct from local notifications. Android 13+ also requires runtime notification permission for non-exempt notifications. [Android notification permission](https://developer.android.com/develop/ui/compose/notifications/notification-permission). |
| Android share-in text capture | Source present, text-only | `MainActivity.captureShareIntent`; this does not yet provide Pocket's complete multi-file `ACTION_SEND_MULTIPLE`/URI import path. |
| Floating capture bubble and quick actions | Partial / device unverified | Legacy/native service and intent routing are present; overlay permission, lifecycle, manufacturer restrictions and useful end-to-end capture need on-device acceptance. |
| Full attachment/image/PDF/OCR/video ingestion | Source branch only / partial | Pocket and Threadline contain useful attachment/capture building blocks, but Memo does not yet provide a complete, bounded, resumable media pipeline for all formats. |
| Rich content Studio with private draft → public-safe review | Source branch only | Threadline includes private-draft/public-safe concepts; not yet a fully integrated Memo Studio with real channel connectors and artifact approvals. |
| Job search → match → approve → apply | Not implemented end-to-end | Can be represented as work entities/tasks, but no complete source-aware job discovery, match score, human application review and verified apply flow. |
| Content generation → approve → publish → measure | Not implemented end-to-end | No complete channel connectors, publication receipts, engagement ingestion, or measurable feedback loop. |
| Universal app/web schemas and reusable DOM selectors | Not implemented | Current browser automation uses provider-specific scripts; native steps use bounded exact labels. Need versioned surface schemas, postconditions and replay fixtures. |
| Cross-runtime dependency graph (DAG) with durable checkpoints | Partial / not complete | Current workflows are ordered bounded steps in specific runtimes. No general executor that runs browser + native app + local AI + guest runner dependencies in a single resilient DAG. |
| Idempotency and safe crash recovery for external side effects | Partial; browser prompt approvals now invalidate on restart and uncertainty enters review | `ActionApprovalLedger.recoverAfterRestart`; no universal idempotency key/receipt protocol across browser/app/channel side effects. Full Gradle/device verification is pending. |
| Encrypted portable backup and restore | Partial / security issue to close | Primary JSON export is readable/plaintext by design; Threadline has encryption-related source patterns but the complete encrypted backup/restore flow is not integrated. |
| One-tap universal device model discovery | Not implemented | Current local support uses Memo's model catalog/LiteRT-LM path; arbitrary Edge Gallery/other runtime models are not automatically discoverable or callable. |
| User-editable skill/tool/agent/surface-schema registry | Partial | `SkillRegistry` and native workflow revisioning exist, but no single governed registry with versioned all-tool/all-schema run manifests. |
| Push-to-talk, accessible quick launch, lockscreen entry | Partial / roadmap | Mic button is present when chat is open; floating entry exists in legacy native path. Add launcher shortcut/notification action/Quick Settings Tile, but never imply unlock bypass. |
| Production signing, privacy/security review and device acceptance | Not verified | No signed release artifact or emulator/real-device test evidence in this environment. CI and device acceptance are mandatory release gates. |


## 2A. Product-contract cross-check against the architecture blueprint

The separate design blueprint was read as a product contract, not counted as another app build. These invariants were cross-checked against the current source:

- **Conversation is not the database:** conversations and typed work entities both exist, but the encrypted Compose vault and native workflow vault remain separate. **Partial; P0 migration/transaction boundary remains open.**
- **Local-first by default:** local workspace, local model path and bounded native steps exist; remote model/site use is explicitly surfaced and consented. **Partial; provider-denial/timeout tests on an Android device remain open.**
- **Models propose; deterministic code validates:** source-excerpt checks and explicit approval paths exist for some outcomes/runs. **Partial; not one policy boundary for every model, browser, native app, and future connector.**
- **Provenance for important facts:** typed entities, source URLs, captured responses, and evidence links exist in the Fieldwork lineage. **Partial; media lineage and duplicate/merge provenance require more work.**
- **Capabilities are explicit and replaceable:** provider list, native workflow policy and guest runner exist. **Partial; a shared versioned capability contract/manifest across all runtimes is not complete.**
- **Success requires observation:** native app workflows require a final visible-text condition; the guest runner refuses success without a verified postcondition. Browser provider steps still require provider-specific checks and do not form a universal DOM engine. **Partial.**
- **Interruption must not duplicate a side effect:** runner tests cover interrupted jobs and the current UI leaves uncertain work for review. **Partial; global idempotency/receipts across browser and native actions remain P0.**
- **Mobile use comes first:** Compose navigation, IME resize, voice draft capture and app settings exist in source. **Not device-verified; narrow screen, keyboard, accessibility scale and lock/unlock remain release gates.**

## 2B. Product additions inferred from the complete source review

These are included in `PRODUCT_GAP_REGISTRY.md` with priority/acceptance criteria, rather than silently treated as delivered features: approval inbox with exact destination/payload previews; pause-all/emergency stop; notification actions that resume or review a run without exposing private content; task due dates, snooze, quiet hours and project mutes; launch shortcut/Quick Settings voice entry; on-device wake-word feasibility gate; source deduplication and reversible entity merges; repeatable workflow fixture tests; model storage/RAM/thermal checks and rollback; offline task queue with no hidden remote fallback; privacy profiles/context allowlists; safe encrypted backup/restore and schema migration; and a post-action metrics loop that distinguishes observed facts from optimization proposals.

## 3. Cross-project feature migration checklist

- [x] Fieldwork: Compose workspace, evidence/source-linking, typed entity proposals, browser/provider workflow, local/remote model runtime boundary audited.
- [x] Fieldwork: provider sessions are visible and sequential; web automation boundaries documented.
- [x] Pocket: capture-first context, share/file intake, immutable capture originals, model onboarding and backups audited.
- [x] Threadline: voice capture, speech/TTS, local reminder/nudge, floating bubble, model catalogue/rollback, private/public-safe Studio and encrypted backup patterns audited.
- [x] app-release: local-only runtime variant, bounded app control, guest runner and policy decisions audited. Memo keeps explicitly consented external frontier-site workflows rather than adopting a blanket local-only policy silently.
- [x] Memo V2 HTML: classified as product explainer/prototype; none of its visual-only ideas are counted as production features.
- [x] Combined current source: notification, speech input, localization, manifest, GitHub workflow and remote runner checks inspected.
- [ ] Build all Android sources, unit tests and lint in GitHub Actions.
- [ ] Install the exact resulting APK on a real phone; test language, notification permission, reminder timing, voice input, overlay and app automation.
- [ ] Verify remote guest runner pairing across HTTPS using a disposable secondary device.

## 4. Release gates

1. The exact delivered source ZIP must pass ZIP integrity and include the latest Kotlin/resources/tests/workflow files.
2. GitHub Actions must succeed at `:app:testDebugUnitTest`, `:app:lintDebug`, and `:app:assembleDebug`; a successful Python-only suite is not a substitute.
3. Run `DEVICE_ACCEPTANCE_CHECKLIST.md` on a real device and document Android version and APK SHA-256.
4. Before public release, replace debug signing with a private release key and complete the security/privacy review.

Last source audit update: 28 October 2026. This audit is deliberately explicit about unknowns; statuses must move only when the corresponding test/evidence exists.


## Director's Cut correction — 28 October 2026

The previous consolidation over-weighted inherited Fieldwork/SynthOS screens and left a second, unlaunched `com.synthos.runtime.MainActivity` in the same APK. That was not a sufficient interpretation of Memo's own product vision. The current increment removes that duplicate Activity UI, introduces a small `MemoRuntimeBridge`, makes the home prompt lead into an unsent conversation draft, and elevates linked records, source evidence, review and resumable work. The historical feature statuses above remain a source audit; they are not a claim that every planned capability is complete. Track open work in `TASKS.md` and the existing device checklist.


## Exact-content action gate update — 28 October 2026

A new pure-core `ActionApprovalLedger` and browser-screen binding now establish a first vertical slice of the HITL contract. A pending provider send stores the exact payload, runtime, capability and sanitized HTTPS destination; the UI displays the full prompt and target; a human approval hash is rechecked before the visible browser send; the approval is single-use; a payload/destination mismatch invalidates it; process death invalidates approvals bound to the lost browser context; and verified completion points to a saved provider-response evidence record. Audit events form a SHA-256 hash chain, with a chain verification guard on transitions.

**Status: partial / source implementation only.** The central inbox is now present for the browser action ledger, but is not yet a unified approvals queue across native/guest/publishing runtimes. The global stop, universal capability policy, native action-ledger adapter, durable DAG execution, authenticated audit store, and all crash/race guarantees remain open. New JVM tests have been added but cannot be claimed as run under Gradle in this environment. A standalone Kotlin behavior harness passed the targeted ledger scenarios; structural source checks passed; remote runner tests passed 12/12. Android compilation, Gradle unit tests, lint, APK generation and on-device WebView behavior remain unverified.


## 2026-10-28 implementation increment — stop control

A top-level Stop active work control now requests cancellation of the current local AI generation, browser workflow, pending browser task and active native AccessibilityService run. Pending external proposals are rejected; approved or in-flight ones become `NEEDS_REVIEW`. Optional remote guest-runner cancellation is attempted only for a job whose last-known state is nonterminal; the user is told that final state must be verified. This is a cooperative first slice, not proof that a remote task or already-submitted website action was rolled back.
