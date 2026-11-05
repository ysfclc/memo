# MEMO — Fresh Start Master Ledger

**Review version:** 0.1  
**Audit date:** 5 November 2026  
**Repository:** [`ysfclc/memo`](https://github.com/ysfclc/memo)  
**Working branch under review:** `ysfclc-patch-1`  
**Status:** Review draft — no source changes made

---

## Executive status

**Memo is not at a verified build baseline today.** The project has substantial Android source and workflow work, but the latest source upload currently selected by CI does not compile. Before implementing another feature, we must reconcile the actual source archive, branch divergence, model/schema mismatch, and storage compatibility.

The latest inspected run, [Actions #68](https://github.com/ysfclc/memo/actions/runs/41727396903), successfully completed upload resolution, ZIP validation/extraction, source-manifest verification, pre-build invariant checks, and the optional Python guest-runner suite (12/12). It then failed at `:core:compileKotlin` with 101 Kotlin compiler diagnostics. Run #67 reports the same compiler-error set. The primary failure area is the workflow engine/graph-policy layer referring to fields and constructor parameters absent from the data models it is compiling against.

The last recent successful Android pipeline is [Actions #54](https://github.com/ysfclc/memo/actions/runs/40942879971), dated 29 October 2026. Its report contains **98 JVM tests, 0 failures, 0 errors, 0 skipped**, and lint reports **0 errors, 53 warnings, 5 hints**. A debug APK artifact was produced and uploaded in that run. This proves that an earlier source snapshot built; it does not prove that the current upload builds, that the APK installs/updates safely, or that the app works on a real device.

**Immediate decision:** freeze feature work. Finish source reconciliation (P1-0) first, then repair the current compile regression against the selected source of truth. No source files, workflows, branches, or repository settings have been changed during this audit.

## Audit scope and evidence limits

Reviewed:

- Live GitHub repository metadata, branches, recursive trees, commits, issue/PR searches, workflow definitions, recent Actions runs, job steps, build logs and downloadable report artifacts.
- Saved Memo project materials: `TASKS.md`, `MEMO-PATCH1-AUDIT.md`, `Memo-Feature-Audit.md`, `Memo-Product-Gap-Registry.md`, `ARCHITECTURE.md`, `BUILD_STATUS.md`, the architecture/design blueprint, CI diagnostics, ZIP-import workflow instructions/verification, signing instructions and device acceptance checklist.
- Retained conversation context for product intent and decisions, including the distinction between Memo 1 and the separate `memo-2` reference project.

Limitations:

- The live GitHub repo stores the Android project primarily as ZIP archives rather than as a checked-out source tree. I inspected repository trees and CI's archive-validation/build reports, but did **not** unpack and independently compare every byte of the default-branch 2.0 MB ZIP against every branch/upload archive. That comparison is explicitly still open.
- The project files `TASKS.md`, feature audit, gap registry and architecture notes are present in the saved project library, but are not visible as standalone root files in the inspected GitHub branch trees. Their contents therefore need to be reconciled into the chosen canonical source archive before treating them as current repository documentation.
- I did not find a standalone `MEMO_TO_FIRMA.md` in the visible library listing or repository root. Prior project context identifies it as a product-vision source; its presence/content inside one of the archived source ZIPs remains to be checked during P1-0.
- The 26 October architecture blueprint is explicitly a review draft. Its proposed iOS/Android React Native direction is not treated as an approved migration. The present delivery line and current repo are Android-native; no framework migration is authorized by this ledger.

---

# PART 1 — WORK LOG

## 1.1 Live repository state (5 November 2026)

| Item | Observed state | Consequence |
|---|---|---|
| Default branch | `main` at `e3838f0b1d674a75f317e7ae77ce5888208f5b73` (30 Oct) | GitHub default remains `main`, but it is not the branch currently running the newest ZIP-import workflow. |
| Working branch | `ysfclc-patch-1` at `05fb6b8607203b7c8269e42568249c1704cd15c5` (3 Nov) | This is the branch used by the recent upload/build runs and the branch named in the saved workflow instructions. |
| Branch relation | `ysfclc-patch-1` is **44 commits ahead and 1 behind** `main`; GitHub reports the branches as diverged | Do not merge or replace either branch/archive blindly. A source comparison and an explicit integration decision are required. |
| Branch protection | Neither inspected branch is protected | No server-enforced protection currently prevents a direct push or accidental branch replacement. |
| Issues / PRs | No GitHub issues and no PRs returned for the repository | The project backlog is maintained in documents, not GitHub Issues/PRs. |
| `main` root tree | `.github/workflows/build-apk.yml` (46,704 B), `Memo-Android-Repository.zip` (2,008,638 B), `README.md` (6 B) | The README is not a useful handoff; the Android source is archived, not expanded in the repository tree. |
| `ysfclc-patch-1` root tree | `.github/workflows/build-apk.yml` (64,899 B), `Memo-Android-Repository.zip` (474,821 B), `incoming/Memo-Android-Repository.zip` (375,855 B), `incoming/memo.zip` (381,525 B), `memo1-task-runbook.zip` (13,580 B), `Memo-Agent-OS-Patch-ysfclc-patch-1.zip` (9,986 B), `incoming/.gitkeep`, `README.md` (6 B) | The working branch has several competing project snapshots and a different workflow. It does not contain a normal expanded Android source tree at branch root. |

### Source archive candidates — unresolved

| Candidate | Location / branch | Observed size | Observed identity / CI evidence | Status |
|---|---|---:|---|---|
| Default-branch archive | `main/Memo-Android-Repository.zip` | 2,008,638 B | Git blob `52e6ad6c627670a717a22330493934549f1c837f` | **Not unpacked or reconciled** |
| Working-branch baseline | `ysfclc-patch-1/Memo-Android-Repository.zip` | 474,821 B | Git blob `446e8d8e69e39b7f8b0cad1da3164cb0ee8b39e7`; this is the archive used by successful run #54 | Last known good baseline candidate, not automatically the latest source |
| Incoming repository archive | `ysfclc-patch-1/incoming/Memo-Android-Repository.zip` | 375,855 B | Present in branch tree; not selected by run #68 | **Not reconciled** |
| Incoming Memo archive | `ysfclc-patch-1/incoming/memo.zip` | 381,525 B | Run #68 validated this archive; SHA-256 `eb1786ec1db02afd74ecb688bc3bf3d943a15c0a6ab3a295c110fd1b46a7429f`; upload commit `9e7ac4ac20af8aa0b5e3f48563e8a746c6658670` | Latest CI-selected source, **compile fails** |

Run #68's report records two different inventories: the retained working-branch baseline ZIP is 474,821 bytes with 198 ZIP entries and 121 source-checksum entries; the selected `incoming/memo.zip` is 178 ZIP entries with 104 source-manifest entries and a detected root of `Memo-Android`. The workflow validated both archive handling and the selected import. This is not a path-level comparison: it does not tell us which 17 manifest entries differ, whether those files were intentionally removed, or whether the default-branch 2.0 MB archive contains additional authoritative work. **Do not select an archive based on size or timestamp alone.**

## 1.2 CI / build history

| Run | Date | Result | What it proves |
|---|---|---|---|
| [#54](https://github.com/ysfclc/memo/actions/runs/40942879971) | 29 Oct 2026 | **Success** | Earlier branch ZIP passed the configured source, Android tests, lint and debug APK pipeline. Report: 98 JVM tests; lint 0 errors / 53 warnings / 5 hints; APK artifact uploaded. |
| #57 (`41052805467`) | 30 Oct 2026 | Failed | Reached `:core:test`; 52 tests ran and one `CapabilityCompilerTest.optionalRuntimeIsSelectedButNeverPretendedToBeEnabled` assertion failed. The earlier diagnosis was an intent-classification ordering bug (`app` matched before explicit `automate`). |
| #58 / #59 | 30 Oct 2026 | Failed in preflight | Older workflow failed to resolve the uploaded archive path from the push event payload. It did not reach extraction or Gradle. |
| [#67](https://github.com/ysfclc/memo/actions/runs/41687402330) | 3 Nov 2026 | Failed | Reached `:core:compileKotlin`; same compiler-error set as #68. |
| [#68](https://github.com/ysfclc/memo/actions/runs/41727396903) | 3 Nov 2026 | Failed | Import, archive validation, source checks, invariants and guest-runner tests pass; Kotlin compilation fails before Android unit tests, lint, APK packaging, signing validation and delivery. |

### Latest run #68 — verified pipeline checkpoints

**Passed:** checkout; JDK 17 / Android SDK setup; ZIP safety/integrity validation; Gradle project-root detection; source checksum verification; source-boundary/product-invariant checks; Python guest-runner tests **12/12**; build-report upload.

**Failed:** `:core:compileKotlin` in `WorkflowExecutionEngine.kt` and `WorkflowGraphPolicy.kt` due to unresolved/missing model properties and constructor parameters. Reported missing contract elements include workflow audit-event fields/sequences/hashes, job graph/trigger fields, step dependency/retry/timeout/output/condition fields, and execution token/attempt/deadline/error fields. The engine/policy code and data-model schema are out of alignment in the selected source snapshot; exact field ownership and migration semantics must be confirmed from the reconciled source before editing.

**Skipped as a consequence:** core/app Gradle test suites after compilation, lint, debug APK assembly, APK signature/identity checks, release packaging, delivery artifacts and the workflow's post-success source integration commit/note. No APK is claimed for run #68.

## 1.3 What exists in source (as last audited; not all current-runtime verified)

| Capability | Evidence-based status | Boundary / remaining proof |
|---|---|---|
| Native Android shell and Compose workspace | Source present in the audited Android source lineage | Current exact ZIP must be inventoried; Android build is failing on the newest upload. No real-device sign-off. |
| Persistent chats and workspace records | Source present | Force-stop/reopen, migration, import/export and consistency need device/integration verification. |
| Encrypted workspace | Source present; security/migration review incomplete | Audit documents separate encrypted Compose-vault and legacy native-workflow store paths; a single atomic persistence/event boundary is not proven. |
| Typed contacts, companies, opportunities, projects, source/evidence records | Source present, partial | No complete general relationship graph, deduplication/merge workflow, or end-to-end CRM acceptance proof. |
| Local model path | Partial | LiteRT-LM-compatible runtime/catalogue exists in audited source; recognizing `.gguf`, `.onnx`, `.tflite` and other files is not proof Memo can execute them. GGUF/llama.cpp integration remains a task. Device fit, streaming, cancellation, OOM and recovery are unverified. |
| Remote model/API path | Partial and consent-gated in the audited source | Optional API-compatible provider requires per-request consent. This is not the default route for frontier consultation. |
| Frontier consultation through provider websites | Partial | Visible, sequential, provider-specific browser scripts exist. Not a universal DOM engine; site structure changes, sign-in walls and provider restrictions must fail safely. |
| Browser action approval ledger / inbox | Partial source increment | Exact prompt/payload hash, runtime/capability/destination binding, one-use approval, expiry, stale approval invalidation, evidence-backed completion and chained audit are documented for the browser path. This is not universal approval across every runtime/channel. |
| Global Stop / cancellation | Partial source increment | Requests cancellation from local inference, browser and native accessibility work; uncertain external in-flight work goes to `NEEDS_REVIEW`. Durable cross-runtime token, race/timeout tests and paired-runner cancellation are not proven. |
| Bounded native app automation | Partial | Accessibility-based, allowlisted/foreground-oriented; depends on user-granted permission and device behavior. Not unrestricted control of arbitrary apps. |
| Optional Python guest runner | **12/12 Python tests passed in #68** | Android client pairing, HTTPS deployment, real paired-device execution and restart/cancel acceptance remain unverified. Not a hosted service. |
| Voice | Partial | Tap-to-talk platform speech recognition inserts an editable draft. It is not guaranteed offline and does not imply continuous listening or wake-word support. Full local ASR/TTS remains open. |
| Local notifications / daily nudge | Source present; device unverified | User-controlled local notification/reminder behavior; no remote push service is present. Reboot/time-zone/battery/OEM behavior needs a device check. |
| English / Polish / Turkish | Partial | Static literal-string coverage checks passed on a prior source snapshot; dynamic/system strings, screen-by-screen language checks and spoken workflow acceptance remain. |
| Approval-backed job application workflow | Not complete end-to-end | Job records and general workflows are foundations; source-aware search/matching, complete application preview, explicit submit authorization and verified submission receipt are not yet proven. |
| Full Studio, content publishing and metrics loop | Not complete end-to-end | Private/public-safe drafting ideas exist in source references; complete channel publishing, receipts and post-performance data loop are not verified. |
| Full workflow DAG / capability compiler | Partial / incomplete | Existing ordered, bounded workflows and compiler components are not yet one durable cross-runtime graph with pinned capabilities, resumable checkpoints, cancellation and postcondition verification. |
| Production release / real-device acceptance | **Not verified** | No current successful build for run #68 and no completed device acceptance record. Do not call Memo production-ready. |

## 1.4 Historical implementation claims retained as partial

The 28 October Memo 1 implementation ledger records a browser-ledger slice: `ActionApprovalLedger`, `Models`, `BrowserWorkflow`, browser session/screen, central action inbox, workspace integration, tests and source-verifier updates. Its documented behavior includes exact-payload hash checks, one-use approval, invalidation after payload/destination mismatch or restart/import, `NEEDS_REVIEW` for uncertain in-flight actions, saved response evidence, and a SHA-256 chained audit. It also states that the audit chain is tamper-evident, not a cryptographic signature against a fully privileged source editor.

That same checkpoint recorded: source verifier passed; a static localization scan covered 283/283 eligible literal labels across six Compose files; remote runner 12/12; standalone `kotlinc` harness passed selected approval/ledger scenarios. The new Gradle JVM tests had not yet been run in that checkpoint. The current CI compiler failure means the old source-workspace checks cannot be used to declare the newest upload complete.

---

# PART 2 — ARCHITECTURE LOG

## 2.1 Product definition

Memo is a **voice-first, local-first, checkpoint-driven personal execution environment**. Conversation is the interface, not the database. Durable work lives in typed records, relationships, sources, tasks, jobs, artifacts and an execution/evidence journal. Memo captures intent, prepares the required capability, executes only within policy, exposes progress and uncertainty, and can resume work without pretending a side effect succeeded.

The primary product is an **Android application**. The current native source lineage is Kotlin/Java with Compose surfaces and a Kotlin `:core` module plus Android `:app` integration. The exact module and file map must be re-confirmed from the selected archive during P1-0. The separate Python guest runner is optional, explicitly configured and not a replacement for the Android product.

## 2.2 Operating model and trust boundaries

1. **Local-first default:** local records, capture, search, planning and supported local inference do not require a Memo cloud account.
2. **Local model as orchestrator/proposer:** local inference interprets intent, prepares structured proposals and coordinates bounded work. It cannot grant itself permission or become the source of truth.
3. **Frontier AI through visible authorized sessions:** when a task needs a frontier service, Memo should use the user's normal authorized app/site session through the visible browser/app workflow. Direct provider API use is optional, clearly disclosed and consented per request; no hidden remote fallback.
4. **Deterministic core:** schemas, validation, workflow state, routing, budgets, permissions, approval, retries, cancellation, audit, redaction and success verification live in deterministic code, not prompt instructions.
5. **Human approval before side effects:** approval binds to the exact payload/content hash, destination, runtime/capability and scope. It expires, is one-use, and is invalidated when the payload or target changes. A proposal is not an execution.
6. **Evidence before success:** a model's assertion is insufficient. A saved observation/receipt/postcondition must support success. Unknown external outcomes become `NEEDS_REVIEW`; they are not blindly retried.
7. **Stop dominates:** user pause/takeover/stop must be available and respected at asynchronous boundaries. Stop must not claim to undo an action already submitted to a third party.
8. **Replaceable capabilities:** model, speech, browser, app automation, vault, scheduler and notification adapters sit behind stable contracts. Platform-specific functions do not own canonical task/entity state.
9. **Privacy and secrets:** local vault encryption and OS-backed key protection; no secrets/cookies/private prompts in source archives, logs, screenshots, fixtures or public commits. Any remote data transfer must be visible, scoped and audited.
10. **No hidden scope substitution:** no mock screen in place of a working feature; no invisible PC/ADB/remote worker standing in for the Android product. Remote execution must be opt-in, isolated, authenticated and auditable.

## 2.3 Current-state architecture map (provisional until archive reconciliation)

| Layer | Expected responsibility | Observed lineage / current concern |
|---|---|---|
| Android presentation | Conversation/workspace, browser, apps, jobs/tasks, library/records, settings; responsive keyboard-safe UI; EN/PL/TR | Compose screens and native Android components appear in the audited source lineage. An older `ARCHITECTURE.md` describes a view-based shell, so the UI implementation document needs updating after exact source inventory. |
| Domain / `:core` | Typed models, deterministic policies, workflow graph/execution, approval ledger, evidence/provenance, routing, capability contracts | Latest CI reaches `:core:compileKotlin` and fails because engine/graph policy references fields not present in the data-model contract. This is the immediate source-level seam to reconcile. |
| Android `:app` adapters | Activity/Compose, WebView, AccessibilityService, Keystore, LiteRT-LM, speech, AlarmManager/notifications, package/model discovery | Source presence is documented; build and device behavior must be re-verified on the canonical source. |
| Persistence | One durable, versioned, encrypted workspace boundary with migrations, transactions and backup/restore | Audited source refers to an encrypted Compose vault and a separate legacy native-workflow store. Convergence and compatibility-safe migration remain open; preserve old app identity, keys, filenames and data until verified. |
| Model adapters | Explicit local model catalog/runtime; streaming/cancel/resource reporting; optional consented remote API | LiteRT-LM path exists in prior audit. GGUF/llama.cpp is planned, not to be assumed present or functional. File discovery does not equal runtime compatibility. |
| Browser | Visible in-app WebView, isolated provider sessions, capture/source evidence, provider adapters, manual takeover | Current implementation is provider-specific and sequential. Generic versioned DOM/surface engine and replay fixtures are backlog. |
| Native automation | Opt-in Accessibility channel with allowlisted actions, foreground/lock checks, sensitive-window refusal and final postcondition | Bounded source implementation exists in prior audit; no real-device verification. It must not become arbitrary or hidden cross-app control. |
| Guest runner | Optional isolated Python HTTPS worker with auth, bounded jobs, explicit approval and receipts | Python tests pass 12/12 in run #68; real pairing and Android-client acceptance remain open. |
| CI / release | Safe ZIP import, source checksums, static boundaries, runner tests, Android tests, lint, APK build/sign/verify, reports and traceable source integration | The active branch workflow has these stages; run #68 passed import/pre-build checks but failed compilation, preventing APK delivery and integration commit. `main` uses a different/older workflow. |

## 2.4 Architecture decisions and boundaries

| Decision | Ledger position |
|---|---|
| Platform | Android-only for the current delivery. Do not migrate to React Native/iOS based on the 26 October blueprint alone; that document is a review draft, not a confirmed implementation directive. |
| UI | Retain the selected Android native/Compose source lineage; first reconcile the exact source snapshot and update stale architecture notes. No second UI shell or duplicate Activity tree. |
| Storage | One canonical logical workspace is the target. Do not force a destructive store rewrite until app ID, file names, schema versions, Keystore aliases, backup formats and upgrade behavior are inventoried and migration/rollback tests exist. |
| Local / frontier AI | Local-first orchestration; visible browser/app sessions for authorized frontier consultation; optional API provider only with explicit per-request disclosure and consent. |
| Automation | Bounded and allowlisted capabilities; user approval for external/destructive effects; no arbitrary shell execution or unattended external actions. |
| Persistence and task semantics | Versioned typed records and durable task/run state; async steps, explicit timeouts, retry budgets, process-death recovery and `NEEDS_REVIEW` for ambiguous outcomes. |
| Audit and evidence | Append-only/hash-chained event history, evidence pointers, exact action envelopes, postcondition-based completion. Hash chaining is tamper-evident but is not a replacement for key-backed signatures where authenticity is required. |
| Guest runner | Optional, paired to a host/device the user controls, HTTPS, bearer auth, least privilege and separate approval. No implied cloud-hosted service. |
| Languages | English, Polish and Turkish are first-class app languages; new user-facing strings must be localized and tested. |
| UX quality | Premium but restrained Android experience: coherent design tokens, clear active/paused/blocked/review states, responsive keyboard behavior, accessible controls, meaningful motion/haptics and no decorative controls that do nothing. |

## 2.5 Documentation authority / conflict resolution

For implementation decisions, use this order:

1. **Selected current Memo 1 source archive** after P1-0 identity and path-level reconciliation.
2. **Memo 1 execution contract (`TASKS.md`)** and accepted project-specific product decisions; task completion claims must be supported by Memo 1 evidence.
3. **Feature audit / gap registry / device checklist** as status and acceptance references, refreshed against the chosen code.
4. **Architecture blueprint** as design rationale and reference material where it does not conflict with later Memo 1 decisions.
5. **Memo 2 (`ysfclc/memo-2`)** as a reference for ideas/tests only. Never clone its UI, data assumptions, codebase, or status marks; do not commit or mutate that repository.

The saved `TASKS.md` states explicitly that upstream Memo 2 `[x]` marks are not Memo 1 completion evidence. This ledger follows that rule.

---

# PART 3 — TASK LEDGER

## 3.1 Status legend

- **[x] Verified:** Memo 1-specific acceptance evidence exists.
- **[~] Partial / blocked:** useful work exists, but at least one required boundary or verification gate is open.
- **[ ] Not started / not verified:** no sufficient implementation evidence is available.

No task is marked complete solely because a screen, button, source file, prior green workflow or Python test exists.

## 3.2 Wave 0 — P1-0 source reconciliation (mandatory; no feature coding)

| ID | Task | Current status | Definition of Done |
|---|---|---|---|
| **P1-0.1** | Fetch and identify the live Memo 1 source | **[~] Partial** | For every candidate archive and branch, record commit SHA, Git blob SHA, byte size, SHA-256, ZIP CRC/integrity, entry count, project root and source-manifest result. Explicitly select one source-of-truth snapshot/branch, with rationale. Preserve every candidate unchanged. |
| **P1-0.2** | Inventory source before extraction or replacement | **[~] Partial** | Produce a complete path inventory for the selected archive: Gradle modules/wrapper/plugins, application ID/version/min/target SDK, manifest activities/services/permissions, Compose/UI entry points, stores/schema/Keystore aliases, model runtimes/catalogues, browser/provider code, native automation, guest runner, tests/scripts/assets/docs/workflows. Include secret scan outcome without printing secrets. |
| **P1-0.3** | Compare source snapshots | **[ ] Open** | Generate a path-level diff for `main` archive, working-branch baseline archive, `incoming/Memo-Android-Repository.zip`, `incoming/memo.zip`, and any relevant preserved library snapshots. Classify additions/removals/renames/duplicates/content changes. Explain the baseline 121 vs incoming 104 manifest-entry difference. No snapshot is discarded or overwritten. |
| **P1-0.4** | Map current implementation to intended architecture | **[~] Partial** | For each existing component, mark keep/adapt/retire/migrate; map dependencies and ownership across domain/core/Android UI/storage/model/browser/native/guest. Resolve the Compose-vs-view-based documentation mismatch and update the actual map. |
| **P1-0.5** | Storage and signing compatibility inventory | **[ ] Open** | Record application ID, persisted filenames/preferences, each store's ownership, schema versions, encryption, key aliases, backup/export formats, migration order, recovery/rollback path and signing identity. Add tests before compatibility-sensitive migration. Raw signing credentials remain outside source, logs and artifacts. |
| **P1-0.6** | Establish unmodified source baseline | **[~] Partial** | Run the chosen exact source unchanged. Record compiler, core/app tests, lint, debug/release packaging and source checks. Distinguish archive/import failure from compiler/test/lint failure. Run #54 is historical evidence only; run #68 is the current failed baseline. |
| **P1-0.7** | Create product acceptance mapping | **[~] Partial** | Map every feature/task ID to current source path, named test, CI run, APK and device/security acceptance as required. Reconcile `TASKS.md`, `FEATURE_AUDIT.md`, `PRODUCT_GAP_REGISTRY.md`, `BUILD_STATUS.md`, `README.md`, changelog and progress log with the selected source. Recover or explicitly supersede `MEMO_TO_FIRMA.md`. |
| **P1-0.8** | Align one canonical ZIP/import/build workflow | **[~] Partial** | Keep one active workflow per chosen branch; prove push and manual upload selection, ZIP safety, idempotency, branch-race protection, checksum/manifest, tests, lint, APK/signature checks, report upload and source integration note. A successful run must publish the APK + SHA-256 and an immutable source/run record. |

**Wave 0 exit gate:** P1-0.1 through P1-0.8 have evidence; archive/source identity and baseline are known; no destructive replacement or application-code work has occurred before approval of the reconciled baseline.

## 3.3 Wave 1 — Restore a trustworthy build and safety foundation (M0 / P0)

| ID | Task | Priority / status | Definition of Done |
|---|---|---|---|
| **M0.B1 / BUILD-01** | Repair the current Kotlin model/engine contract regression | **P0 / [ ]** | On the selected source snapshot, resolve all `:core:compileKotlin` diagnostics in the workflow engine/graph-policy/data-model boundary. Preserve explicit schema meaning; do not hide errors with broad suppressions or duplicate fields. Add regression tests for graph version, triggers, dependencies, retries, timeouts, conditions, execution tokens, attempts, deadlines, errors and audit-event sequencing/hashes. `:core:compileKotlin` passes. |
| **M0.A3–A4 / DATA-01** | Stabilize typed records and schema migration contract | **P0 / [~]** | Define versioned entities and migration behavior for supported Memo records. Round-trip old/new fixtures; unknown-version and failed-migration tests; atomic rollback; preserve user data. Prove app and native workflow stores do not silently diverge. |
| **M0.C1–C2 / VAULT-01** | Close encrypted-vault and persistence boundary | **P0 / [~]** | Map all stores/keys; verify encryption in the actual build; secure key lifecycle and lock behavior; backup-before-migration; encrypted backup/restore, wrong-key/corruption handling, recovery and rollback. No last-good workspace is destroyed on failure. |
| **M0.A1–A2 / ENGINE-01** | Make workflow execution asynchronous, durable and auditable | **P0 / [~]** | External steps are resumable with explicit pending/result state, IDs, timeouts, cancellation, stale-result handling and process-death recovery. Every state transition/side effect has a durable event; app restart exposes interrupted work instead of replaying it. |
| **M0.A5–A7 / POLICY-01** | Centralize permissions, routing and platform interfaces | **P0 / [~]** | One deterministic policy boundary classifies risk/destructive effects, checks user grants, chooses allowed runtime, discloses outbound context, redacts, enforces retry/action budgets and kill switch. Pure core stays Android-free; each platform capability has a replaceable typed interface. |
| **M0.F1 / APPROVAL-01** | Extend exact-content approval beyond the current browser slice | **P0 / [~]** | Every external/destructive action across browser, native, guest and future channel paths binds approval to exact payload hash, destination, runtime, capability and scope; expiry/one-use/revalidation enforced; changed payload requires new approval; uncertain outcome → `NEEDS_REVIEW`; success requires receipt/postcondition evidence. |
| **M0.F2–F4 / REVIEW-01** | Independently review engine, app integration and publish/outbound confirmation | **P0 / [ ]** | Independent review findings are recorded, severity-ranked and fixed with regression tests. No approval bypass, stale replay, fabricated success or secret exposure remains open. |
| **M0.R1–R2 / SECURITY-01** | Independent security/architecture review | **P0 / [ ]** | Two documented review passes cover key/data handling, external action gates, permission boundaries, logging, remote runner, audit authenticity and restore. Material findings resolved or explicitly accepted by user. |
| **BUILD-02** | Re-run full CI and produce a verifiable debug APK | **P0 / [ ]** | Exact integration commit passes source checks, `:core:test`, `:app:testDebugUnitTest`, strict lint and `:app:assembleDebug`. APK exists, installs, package/version are recorded, signature verifies against the intended persistent key, and APK + source + report checksums are published. Warnings are triaged; 0 errors. |
| **DEVICE-01** | Complete real Android device acceptance | **P0 / [ ]** | Execute the full device checklist on the agreed Android phone/OS and record date, APK SHA, run URL, steps, outcomes and screenshots: install/unlock, chats/persistence, capture, vault backup/restore, local inference, remote-consent denial, browser, approvals, stop/recovery, Accessibility, voice, languages, reminders and notifications. Guest pairing remains separately optional. |

**Wave 1 close rule:** M0 closes only when core tests, Android tests, lint and build pass on the integration commit; data compatibility is evidenced; no critical security finding remains; external action approval is universal; and required device acceptance is recorded. A green workflow alone does not close M0.

## 3.4 Wave 2 — Local AI and conversation (M1)

| ID | Task | Definition of Done |
|---|---|---|
| **M1.A1** | LiteRT-LM adapter | Real streaming, cancellation, capability reporting, initialization failure/OOM handling and supported-device test behind the model interface. |
| **M1.A2** | llama.cpp / GGUF adapter | Pinned API/source, JNI lifecycle, compatibility limits, memory/OOM behavior, cancel/unload and device benchmark; no claim that discovered files are runnable until tested. |
| **M1.A3** | Inference manager | Resource/concurrency budgets, runtime selection, unload, lifecycle and truthful ready/loading/unsupported/failed states. |
| **M1.B1–B3** | Model catalog, safe import/download and hub | Verified source/license/revision, size/hash, storage/RAM checks, resumable download, cancellation, rollback, compatibility labels and real progress/error states. |
| **M1.C1–C2** | Encrypted chat persistence and complete chat | Streaming, stop/regenerate, model picker, context view, editable voice transcript, history recovery, empty/loading/error/accessibility states; install → load → stream → cancel → restart → recover demonstrated on device. |

## 3.5 Wave 3 — Voice and design system (M2 / M2.B0 / M2.5)

| ID | Task | Definition of Done |
|---|---|---|
| **M2.A1–A3** | Speech contracts and local ASR/TTS | Explicit permission/lifecycle, foreground-service compliance, VAD, language support, barge-in, stop, transcript correction, interruption and recoverable errors; real on-device speech tests. |
| **M2.B1 / M2.C1** | Voice conversation and entry points | Voice messages integrated with chat; screen/lock entry respects Android policy and unlock; no hidden microphone use. |
| **M2.C2** | Wake-word feasibility gate | No implementation until privacy, foreground service, visibility, battery/thermal cost, mic controls and OEM behavior are reviewed and approved. |
| **M2.C3** | Quick settings / bubble / ongoing notification | User-triggered entry, explicit stop/return behavior, permission denial and lock-screen privacy all work on device. |
| **M2.B0a–d** | Unified design system and navigation | Canonical tokens, typography, theme, accessible components, reduced-motion support, haptics, responsive layouts, keyboard-safe screens and predictable back behavior. |
| **M2.5.A1–A3** | Signing and performance baselines | Stable private signing, monotonically increasing version code, no secret leakage, macrobenchmark baselines and measured optimizations. |

## 3.6 Wave 4 — Orchestrator, records and product flows (M3 / M3.5–M3.9)

| ID | Task | Definition of Done |
|---|---|---|
| **M3.A1–A4** | Planner, retrieval and bounded agent engine | User intent compiles into typed, bounded workflow; context respects provenance/privacy/freshness; tool schemas and budgets enforced; state/artifacts/checkpoints visible in one flow; stop always wins. |
| **M3.B1–B3** | Artifact approval and Live view | Populated artifact is reviewed before action; every step shows state/time/rationale/result/evidence; pause/takeover/cancel/stop are functional; glance status is truthful. |
| **M3.C1–C2** | User preferences and CRM enrichment | Person/company identity resolution, sourced facts/confidence, duplicate review, user approval and persisted entity + follow-up task. |
| **M3.5.A1–C1** | Standing rules, safe editors and settings | Narrow/revocable scopes and expiry; optional humanizer cannot change facts; real CRUD for records; versioned workflow editor with dry-run/test/rollback; complete privacy/autonomy/channel/runtime settings. |
| **M3.6.A1–C1** | English / Polish / Turkish | Localized UI and intent/time grammar; language survives restart; test fixtures and voice acceptance for all three languages. |
| **M3.7.A1–C4** | Reminders, capture, sources, memory, Today, onboarding, updates and contacts | Real scheduling/time-zone recovery; share/launcher capture with explicit permissions; immutable provenance; indexed source retrieval; correctable relationship graph; notification and contact merge behavior verified. |
| **M3.8.A1–C1** | Capability Compiler/library | Versioned schemas/migrations, readiness/gap/conflict checks, deterministic assembly, finite retry/evaluation, evidence-backed promotion, seeded capability tests and inspectable UI. |
| **M3.9.A1–C1** | Self Model and Artifact Studio | Encrypted user-owned facts with sources/conflicts/versioning; evidence-reviewed CV/job/content artifacts; fabricated facts rejected; export format/page-fit tests; publishing/sync only by explicit action and approval. |

## 3.7 Wave 5 — Browser/native automation, channels and Council (M4)

| ID | Task | Definition of Done |
|---|---|---|
| **M4.A0–A3 / L1–L2** | Automation contracts and browser runtime | Closed typed action vocabulary, bounded inputs, provider/session isolation, versioned surfaces/fixtures, safe selectors, visible Live view, manual takeover, postconditions and audit. |
| **M4.B1–B4 / C1–C2 / D1–D2 / L3** | Native app automation and channel routing | Accessibility is opt-in and allowlisted; sensitive windows refused; app identity verified; user touch pauses/takes over; no password/secret automation; device-wide discovery carries evidence; settings/router explain selected channel and permissions. |
| **M4.A4a–d** | Council | Pure bounded state machine, roles/seats/budgets, distinct source evidence, claim/disagreement ledger, no sycophantic merge, evidence-backed extraction and clear failure states. |
| **M4.A5** | Optional API channel | Per-request context preview, disclosure, redaction, HTTPS, consent and audit; never a silent fallback. |
| **M4.E0–E6 / L4** | Web/native element schemas and recorder | Versioned schemas, safe element picker, list generalization, shadow-DOM fixtures, parameterized recorder, postconditions, quarantined self-healing proposals and one coherent Live timeline. |
| **M4 close** | End-to-end automation acceptance | Demonstrate user-approved browser + native workflow with live view, pause/takeover, verified postcondition, restart behavior and audit on an actual device. |

## 3.8 Wave 6 — User outcomes and release (M5–M6)

| ID | Task | Definition of Done |
|---|---|---|
| **M5.A1** | Brainstorm-to-project workflow | Intent → contextual planning → configured frontier consultation/Council → saved project, sourced decisions and actionable tasks. |
| **M5.B1** | Job discovery and applications | Search configured sources, deduplicate/rank with explainable criteria, create Job records, tailor factual CV/cover letter, show a complete application preview, require explicit final-submit approval and store verified receipt/status. |
| **M5.C1 / C1a / C1b** | Content workflow | Source idea → evidence-checked draft → real generated assets where supported → preview → explicit publish approval → receipt and measured outcomes. No fake publishing. |
| **M5.C2** | Arbitrary bounded plans | Typed decomposition, iteration/time/action limits, checkpoints, deterministic stop and evidence-backed outcomes demonstrated end to end. |
| **M6.1–M6.11** | Hardening and release | Offline/restart recovery, redacted diagnostics, encrypted backup, app lock, permission audit, strict lint, safety-path coverage, updated docs, complete feature evidence map, accessibility checks, signed release artifact, checksum/provenance and rollback. |
| **v1.0.0** | Release gate | User explicitly approves release; all critical use cases and security/device/build gates pass; only then tag/publish. |

## 3.9 Cross-cutting acceptance rules (apply to every task)

- **Unit:** named test and exact command/result.
- **Integration:** real app/runtime/storage/adapter behavior, not merely isolated mocks.
- **CI:** run URL/ID, commit SHA, job summaries and artifact references.
- **Artifact:** APK or exported file path, build variant/version, size and SHA-256.
- **Device:** model, Android version, steps, result, limitation and screenshots.
- **Security:** review/test evidence for permissions, secrets, data flow, authentication, destructive effects and logs.
- **Documentation:** task state, feature audit, progress, changelog, build status and architecture all match the same source commit.
- **Definition of done:** observable behavior + named tests + required CI + reviewed diff + security review where relevant + device acceptance where applicable. If any required proof is missing, status remains `[~]` or `[ ]`.

## 3.10 Immediate execution order after ledger approval

1. **Approve source arbitration criteria and finish P1-0.1–0.3.** No code changes. Select the authoritative archive only after its content is compared.
2. **Finish P1-0.4–0.8.** Produce the exact source map, storage compatibility report and baseline failure record; align the one import/build workflow.
3. **Start one implementation task only: `BUILD-01` / M0.B1**, repairing the current compile contract against the approved archive. Add named regression tests before changing the model/engine contract.
4. Run the complete Gradle/CI gate; inspect APK and checksums; then request approval before moving to another atomic implementation task.
5. Run device acceptance before calling the restored build usable or closing M0.

---

## Approval checkpoint

This ledger is a review draft and a proposed execution contract. **No code, workflow, branch, issue, PR, or repository setting has been changed.**

To begin Phase 3, approve this ledger (or specify changes). The first execution action after approval is source reconciliation—not feature coding—and the first implementation task after the P1-0 gate is `BUILD-01`.
