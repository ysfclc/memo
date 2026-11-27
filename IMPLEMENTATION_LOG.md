# Memo 1 — Implementation log and evidence ledger

This file is the durable record of decisions and verified progress for the Memo 2 → Memo 1 implementation. Append entries; do not erase failed attempts or silently rewrite status.

## Current governing state

- Target repository: `ysfclc/memo`.
- Blueprint repository: `ysfclc/memo-2` (read-only reference for this mission).
- Canonical ZIP input: `incoming/memo.zip` (complete project snapshot; one upload at a time).
- Active contract: `IMPLEMENTATION_CONTRACT.md`.
- Task-level plan: `TASKS.md`.
- Scope crosswalk: `MEMO2_ALIGNMENT_MATRIX.md`.
- Product gap evidence: `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, `MEMO-PATCH1-AUDIT.md`.
- Product implementation is **not considered complete** because the package compiles or an APK is produced.

## Entry template (copy for each work package)

### YYYY-MM-DD — WP-ID / TASK-ID — short title

- **Requirement:**
- **Blueprint reference:**
- **Memo 1 baseline commit / ZIP SHA-256:**
- **Files changed:**
- **Data compatibility impact:** none / described migration:
- **Implementation summary:**
- **Tests added/updated:**
- **Commands and results:**
- **CI run URL / run ID:**
- **Exact commit SHA tested:**
- **Artifact path, size, SHA-256:**
- **Device/emulator evidence:** verified / pending / not applicable (explain):
- **Security and failure-path review:**
- **Known gaps / follow-up task IDs:**
- **Status:** `[ ]` open / `[~]` partial or awaiting evidence / `[x]` verified

## Initial entry — plan package

### 2026-10-28 — PLAN-001 — Install strict implementation governance in source ZIP

- **Requirement:** Implement the Memo 2 product ambition in Memo 1; preserve Memo 1 and use the user's personal context safely.
- **Blueprint reference:** `docs/TASKS.md`, `docs/BUILD_PLAN.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, `docs/PROGRESS.md` in `ysfclc/memo-2`.
- **Files changed:** `IMPLEMENTATION_CONTRACT.md`, `MEMO2_ALIGNMENT_MATRIX.md`, `IMPLEMENTATION_LOG.md`; cross-links in `TASKS.md` and `README.md`; source checksum manifest refreshed.
- **Implementation summary:** Adds mandatory source/baseline gate, phase exit criteria M0–M6, evidence protocol, no-scope-cut rules, and a requirement-to-acceptance crosswalk. No product feature is marked implemented by this documentation change.
- **Tests and results:** Pending archive integrity and manifest/source verification in this packaging step; no Android build claimed for this documentation-only change.
- **Status:** `[~]` governance packaged; G0 source reconciliation and actual implementation remain open.

### 2026-10-28 — WP-02 through WP-07 — context execution and guarded artifact workflows

- **Requirement:** Implement concrete vertical slices from the full Memo product plan within Memo 1 without replacing the app shell, vault identity, or stored-data compatibility.
- **Blueprint reference:** `TASKS.md` context routing, capability compiler, Self Model, encrypted backup, live execution/stop and human-review requirements; `MEMO2_ALIGNMENT_MATRIX.md` M0–M6 crosswalk.
- **Memo 1 baseline commit / ZIP SHA-256:** Repository branch upload commit observed: `4df5bcddb7572fee9a2d8fe8a599ecc24ebc8b1b`. Working source is a derived local ZIP snapshot; final repack SHA-256 is recorded separately with the delivered archive.
- **Files changed:** Added core `WorkspaceContextRetriever.kt`, `CapabilityCompiler.kt`, `SelfModelFactualityGuard.kt`, `EncryptedBackupCodec.kt` and associated tests; modified `IntentPlanner.kt`, `MemoWorkspaceViewModel.kt`, `WorkspaceScreens.kt`, `MemoWorkspaceApp.kt`, `OpenAiCompatibleClient.kt`, `AiRuntimeViewModel.kt`, localization catalog; updated task, audit, gap, README, changelog and governance docs.
- **Data compatibility impact:** No persisted model schema bump in this increment. Existing primary vault filenames, Keystore aliases, app ID and workflow vault are preserved. Encrypted backup is a new portable format and does not replace the on-device vault.
- **Implementation summary:** Adds bounded source-linked workspace retrieval for Work prompts; lets an untouched DRAFT plan regenerate safely; compiles task intents into registered capability IDs with stable manifest fingerprint and permission/approval state; adds narrow unsupported numeric/date claim flags for Self Model drafts; adds authenticated, bounded AES-256-GCM passphrase backup/restore; adds incremental compatible remote response streaming and best-effort active-connection cancellation. Remote use of workspace context is still separately opt-in.
- **Tests added/updated:** `CapabilityCompilerTest`, `WorkspaceContextRetrieverTest`, `SelfModelFactualityGuardTest`, `EncryptedBackupCodecTest`, planner/outcome tests.
- **Commands and results:** Pure Kotlin helper compilation via local `kotlinc`: PASS. Manual smoke harness for deterministic capability manifest / optional Android status / external approval: PASS. Context retrieval/provenance/action gate/factuality harness: PASS. Encrypted backup round-trip, plaintext absence, wrong-passphrase rejection and tamper rejection: PASS. Final `python3 tools/verify_source.py`: PASS (32 integration files, Android isolation and wiring checks, XML/manifest, EN/PL/TR static UI catalog 363/363). Remote runner tests: 12/12 PASS from `remote-runner/` working directory. Android Gradle, Android unit tests, lint and device/runtime behavior are not claimed by these source/helper checks.
- **CI run URL / run ID:** `https://github.com/ysfclc/memo/actions/runs/40704538951` was triggered on uploaded source; no CI result is used as evidence for these feature claims.
- **Exact commit SHA tested:** Local working source includes additional edits not contained in upload commit `4df5bcddb7572fee9a2d8fe8a599ecc24ebc8b1b`; do not treat that commit as testing these later edits.
- **Artifact path, size, SHA-256:** The final archive is `/mnt/data/Memo-Android-Repository.zip`; byte size and archive SHA-256 are in the adjacent `.sha256.txt` receipt supplied with the package. The archive hash is kept outside the ZIP to avoid a self-referential checksum.
- **Device/emulator evidence:** Pending.
- **Security and failure-path review:** Backup authenticates header and ciphertext before JSON decode/replacement; password and transient byte arrays are cleared on completion/error paths; context output is bounded and marked untrusted; external action steps cannot be started through the generic plan runner. Remote stream cancellation is best-effort and must not be confused with proof the server stopped computing.
- **Known gaps / follow-up task IDs:** Unified stores and migration/rollback, source-file and multi-file share intake, local ASR/TTS and model backend breadth, generic cross-runtime DAG, full versioned skill/tool/surface registry, restore preview, remote idempotency receipts, and device acceptance remain open.
- **Status:** `[~]` partial feature implementation; full M0–M6 scope remains open.


### 2026-11-05 — CI-REG-071 — Fix automation intent precedence

- **Requirement:** Ensure explicit automation requests select the registered external Android runtime even when the same request contains generic app/build vocabulary.
- **Baseline CI:** Run #71 (`41964174031`), upload commit `ac6b2069ad768362ca05ecc0a83eea20e5ff1f98`, archive SHA-256 `1467ee49a95840d4cd65fc82bae6cc8259f9dc89f7c7d2077dee0730c362b6f9`.
- **Failure:** `CapabilityCompilerTest.optionalRuntimeIsSelectedButNeverPretendedToBeEnabled` failed because `IntentPlanner` returned BUILD for “Automate a sequence in my Android app”; the generic `app` match preceded the explicit automation match.
- **Files changed:** `core/src/main/kotlin/app/memo/android/orchestration/IntentPlanner.kt`; `core/src/test/kotlin/app/memo/android/orchestration/CapabilityCompilerTest.kt`; CI evidence and status/handoff documentation.
- **Implementation:** Explicit automation verbs/action language now take precedence. Broad `browser`/`workflow` terms stay below BUILD to preserve feature-implementation routing.
- **Tests added/updated:** The failing test now asserts AUTOMATION domain, Android launch capability and OPTIONAL_PERMISSION status. Added a negative case that a request to build a workflow editor stays BUILD.
- **Local verification:** Isolated Kotlin/JVM harness compiled the actual planner/compiler/runtime-contract source and passed three behavior checks. `tools/verify_source.py`, source-manifest integrity and remote-runner 12/12 are also expected to be rerun on the corrected archive before upload.
- **CI status:** Full Gradle suite, app unit tests, lint and APK build are pending another Actions run; no green-build claim is made.
- **Status:** `[~]` source correction prepared; pending full CI confirmation.

## 5 November 2026 — Run #72 archive metadata repair

Run #72 failed during ZIP validation, before source extraction or Kotlin/Gradle work. The uploaded archive had Unix permission bits but omitted Unix file-type bits; the validator therefore treated ordinary entries such as `.github/workflows/build-apk.yml` as special files. Repacked the identical source content with explicit `S_IFREG`/`S_IFDIR` metadata. Rechecked archive CRC and entry modes, source invariants, and the 12 remote-runner tests. No workflow relaxation was made: ZIP safety validation remains strict. Full Gradle/Android verification remains pending the next CI run.

### 2026-11-05 — CI-APK-073 — Repair Android app compile diagnostics

- **Requirement:** Resolve all Kotlin compiler diagnostics from the first Android app compilation after the source ZIP passed strict import and core tests.
- **Baseline CI:** Run #73 (`41966362723`), upload commit `1fe27603eab915d177c56db935b5e49c02335b59`, archive SHA-256 `af55ae68b413f9e45748f0b19541d3e5e9f1f5bf7c12c29ae908234fe9a8078f`.
- **Failure:** `:app:compileDebugKotlin` reported eight diagnostics in `MemoWorkspaceViewModel.kt`, `ActionApprovalInboxScreen.kt`, `MemoWorkspaceApp.kt`, and `WorkspaceScreens.kt`.
- **Files changed:** The four Kotlin files above; `BUILD_STATUS.md`; `CHANGELOG.md`; this log; `SOURCE_SHA256SUMS.txt`; and `docs/handoff/ci-history/run73-kotlin-compile-2026-11-05/`.
- **Implementation:** Avoided repeated cross-module property access by capturing `transition.action` in a local immutable value; imported `heightIn`, `Edit`, and `Bookmark`; moved the emergency-stop helper below `BrowserSession` initialization; guarded nullable conversation access; replaced the unresolved separator with `HorizontalDivider`.
- **Tests and checks:** The failed CI source snapshot had already passed source checksum validation, `tools/verify_source.py`, remote runner tests (12/12), and `:core:test`. The repaired source must pass those checks again. The local environment has Java/Kotlin but no Gradle 8.14.6 distribution or Android SDK, so a full app compile cannot be claimed locally.
- **CI status:** Android compile, Android app tests, lint and APK assembly remain pending a new upload/Actions run.
- **Exact commit SHA tested:** `1fe27603eab915d177c56db935b5e49c02335b59` is the failed baseline, not the repaired upload.
- **Device/emulator evidence:** Not run.
- **Known gaps:** Await next CI result; device acceptance remains pending after a successful APK build.
- **Status:** `[~]` source repair prepared; awaiting CI validation.


### 2026-11-05 — CI-APK-074 — Gate notification delivery on runtime permission

- **Requirement:** Never call Android notification delivery APIs when the Android 13+ `POST_NOTIFICATIONS` runtime permission is absent; do not crash if the user revokes it during delivery.
- **Baseline CI:** Run #74 (`41967437305`), upload commit `f8b5799d36fc58d423a994345d981f4362495fe2`, archive SHA-256 `7aceaa744be09ec74332b616bfae3488be0e10b5039d5ca06dadb62c4704f792`.
- **Verified baseline:** ZIP/source gates, source verifier, remote runner, Kotlin compile, `:core:test` 53/53 and `:app:testDebugUnitTest` 14/14 passed. `:app:lintDebug` failed with one `MissingPermission` error, 54 warnings and 5 hints; APK assembly did not run.
- **Root cause:** `NotificationManagerCompat.notify()` in `MemoNotificationCenter.show()` was guarded by the app-level notifications-enabled state but not by an explicit `POST_NOTIFICATIONS` runtime-permission check.
- **Files changed:** `app/src/main/java/app/memo/android/notifications/MemoNotificationCenter.kt`; `BUILD_STATUS.md`; `CHANGELOG.md`; this log; `SOURCE_SHA256SUMS.txt`; and the run #74 handoff note.
- **Implementation:** Check `ContextCompat.checkSelfPermission` on API 33+ before constructing/posting the notification; retain the enabled-state guard; catch `SecurityException` to handle permission revocation between check and notify. The existing UI remains responsible for requesting permission.
- **Lint policy:** No suppression, lint baseline, or workflow relaxation. The 54 warnings and 5 hints remain a separate non-blocking review backlog.
- **Local verification:** Static source assertions, `tools/verify_source.py`, source-manifest integrity, archive CRC/mode checks, and remote-runner tests are rerun for this package. Full Gradle execution is unavailable locally because the pinned Gradle distribution and Android SDK are not installed.
- **CI status:** The run #74 source snapshot is not green. The next Actions run must confirm lint passes and proceed through APK assembly/signature verification; do not claim an APK until an artifact is produced.
- **Status:** `[~]` permission fix packaged; awaiting authoritative CI validation.


### 2026-11-10 — Security, experience and Work capability consolidation

- Integrated the fail-closed native action policy and Talk draft-preservation changes with the Signal & Graphite design foundation, unified experience/navigation rebuild, Today priority model, and evidence-backed industrial account intake.
- Preserved the mainline product scope and existing execution history; did not replace the main task ledger with the shortened feature-branch copy.
- Verification remains open: run authoritative Gradle unit tests, lint and Android APK pipeline against the integrated commit. No APK or device acceptance is claimed by this source merge.


### 2026-11-20 — GOV-ARCH-001 — Lock the intent-driven execution architecture before further feature work

- **Requirement:** MEMO-AGENT-001, MEMO-CAP-001, MEMO-ARTIFACT-001; existing MEMO-ORCH-001 and MEMO-AI-001.
- **Baseline:** `ysfclc/memo` main at `c307f94f5d296622e9e3b2fa92b02fe9c58361f2`; changes isolated on `governance/agent-execution-invariants-2026-11-20`.
- **Files changed:** Added `AGENTS.md` and `docs/AI_EXECUTION_INVARIANTS.md`; updated DEVELOPMENT_GOVERNANCE, IMPLEMENTATION_CONTRACT, Product Contract, TASKS, REQUIREMENT_TRACEABILITY, DECISION_LOG and this log.
- **Implementation summary:** Established binding rules for one Talk-to-outcome path, separation of intelligence providers from callable action capabilities, real adapter contracts, shared dispatch and observe/verify/replan loop, durable execution, deterministic policy and regression acceptance. Explicitly prohibited per-example workflows/domains/screens, phantom capabilities, model-only claims of action, and parallel orchestrators.
- **Product status:** Documentation/constraints only. No agent loop, dispatcher or capability implementation is claimed by this entry. Current source gaps remain open.
- **Validation:** Repository source and existing docs inspected before writing. Branch comparison against `main` reported 13 commits ahead / 0 behind and 11 changed documentation files; final pre-closeout branch head was `ae9fa19260f33c0b56ef9a75f837971310b3562c`. All referenced governance files and requirement IDs were fetched back from the branch. No build or device test is applicable to this documentation-only change.
- **Review:** Draft PR [#12](https://github.com/ysfclc/memo/pull/12) opened; not merged.
- **Known gaps:** Talk `runPrompt()` still generates text directly; `IntentPlanner` remains keyword/template based; `WorkflowGraph` and `WorkflowTaskProposalParser` remain proposal/validation foundations; universal callable dispatch, observe/replan loop and durable cross-runtime execution remain open.
- **Next gate:** Any feature implementation must first show how it uses these invariants; the first engineering work package must connect Talk to the shared executor, not add another specialist workflow.
- **Status:** specified / governance locked; product implementation remains open.

### 2026-11-20 — WP-AGENT-001 — First connected Talk execution loop

- **Requirement:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001; one general-purpose Talk entry point using the selected local or explicitly permitted frontier model as intelligence provider and real, dynamically selected capabilities.
- **Baseline:** Governance branch `governance/agent-execution-invariants-2026-11-20`, based on main commit `c307f94f5d296622e9e3b2fa92b02fe9c58361f2`. Implementation branch: `agent/talk-execution-loop-2026-11-20`.
- **Files changed:** `AgentExecutionEngine.kt`, `AgentExecutionEngineTest.kt`, `AiRuntimeViewModel.kt`, `MemoWorkspaceAgentTools.kt`, `MemoWorkspaceViewModel.kt`, `WorkspaceScreens.kt`, `MemoLocalization.kt`, `core/build.gradle`.
- **Data compatibility impact:** No vault schema bump, encryption-alias change, app ID change, or replacement store. Existing encrypted workspace remains canonical. New agent event records use the existing conversation message model with role `TOOL`; local draft notes use existing EvidenceRecord; record proposals use the existing EntityRecord model and native source conversation/message provenance.
- **Implementation summary:** Talk now runs a bounded, provider-agnostic model-decision loop. It advertises only adapters whose implementations are currently connected: permission-gated local workspace search, encrypted private draft-note save, and source-linked `PROPOSED` record creation. Model decisions are parsed and schema-validated; unknown tools are rejected; external/destructive actions pause before invocation; uncertain write failures/timeouts/cancellation stop for review; actual tool outcomes feed the next model turn. Tool/lifecycle events (run/call IDs, status, capability version and evidence reference, without raw arguments/private payloads) are persisted to the existing vault and authenticated by read-after-write. Stop cancels the agent coroutine and active model request.
- **Tests added:** `AgentExecutionEngineTest` covers tool → observed result → final response, unavailable capability recovery, schema rejection, exact approval blocking for an external write, uncertain local-write failure requiring review, normal direct text response and JSON-without-protocol handling.
- **Commands and results:** No local Gradle build was available in this environment. CI #989 first exposed a stale source-verifier assertion for the obsolete `Continue in Talk` label; the verifier was corrected to assert the actual `AgentExecutionEngine` Talk connection. CI #995 then exposed a missing `EvidenceRecord.kind` argument in `SalesAccountResearchTest`; the fixture now explicitly identifies the evidence as `PUBLIC_WEB_PAGE`. CI #996 compiled the core tests and exposed a missing Compose `Spacer` import in `MemoPrimitives.kt`; that import was added. **CI #997 on head `d4ba59293ea9ccef11ba119ccd2ddcd8b32432cd` passed:** source invariants, remote-runner test, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, APK package/version identity and `apksigner` verification. The APK reported package `app.memo.android.debug`, version code `997`. The Android emulator in-place upgrade gate also passed: app-private data survived the replacement and both encrypted Memo stores reopened; legacy recovery and corruption tests passed. The build itself reported `BUILD SUCCESSFUL` (Gradle 8.14.6).
- **Artifact / device evidence:** CI successfully built and cryptographically verified the debug APK (package `app.memo.android.debug`, version code `997`) using the ephemeral CI debug key. Because this was a pull-request validation run, the workflow intentionally skipped publishing the APK and integrated-repository artifacts and skipped the isolated persistent-signing job; the available run artifact is build diagnostics only. The emulator install-over-existing and encrypted-store acceptance gates passed. No physical-device acceptance is claimed.
- **Security and failure-path review:** Uses existing explicit remote-use gate; workspace search is not included for remote models without the existing workspace-sharing permission. No browser/Android/code/media/PDF/video adapters are exposed. No unapproved external writes. Local writes are private draft/proposal operations. Uncertain outcomes become NEEDS_REVIEW rather than blind retries.
- **Known gaps:** No registry-wide capability discovery, generic browser session adapter, Android app-action adapter, code runner, image/PDF/video renderer, durable process-death resume/reconciliation, structured user approval UX, postcondition evaluator, generic task capability, or Talk→Live/Work/Studio projection. Automated JVM, lint, APK and emulator upgrade acceptance passed; physical-device acceptance remains pending.
- **Status:** `[~]` in progress; first Talk execution slice is implemented and CI/emulator validated at the recorded head. Code remains on the separate draft PR branch and is not merged.


### 2026-11-20 — WP-AGENT-002 — Runtime-validated capabilities and visible browser integration in Talk

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001.
- **Baseline:** Green first Talk slice at CI #998; implementation branch remains agent/talk-execution-loop-2026-11-20, draft PR #14.
- **Implementation:** Added AgentCapabilityRegistry and registered the app.workspace runtime contract. Every adapter must refer to a registered runtime/capability and exactly match its approval and external-side-effect policy. Capability readiness is rechecked before every model decision, so later steps can become available after earlier actions.
- **Browser integration:** Added real browser.navigate_https and browser.capture_evidence adapters over the existing BrowserSession and BrowserScripts.capturePage. Navigation accepts only exact public HTTPS URLs literally included in the user's request. Memo opens that address in the shared, visible WebView and waits for the same host/path/query to finish loading; redirects away from the exact requested page fail closed. It does not click links, manipulate composers, fill forms or submit prompts. Read-only capture verifies the page remains the same before and after extraction.
- **Evidence and privacy:** Full visible-page text is saved through saveWorkArtifactDurably as BROWSER_PAGE_CAPTURE in the existing encrypted workspace, with URL, timestamp, digest, runtime/run/call IDs and source conversation/message provenance. Duplicate content is detected for the same page and source conversation. Only a bounded excerpt is fed back to the model and is explicitly labelled untrusted source data. A separate per-request checkbox is required before page text may be sent to a remote provider; default is off. The shared WebView is rendered in Talk during browser activity and can be opened in the full browser surface.
- **Tests:** Runtime mismatch and approval-policy mismatch are rejected; live readiness is tested; a model-directed navigate → newly-available capture → final-response sequence is tested; HTTPS URL normalization and rejection of HTTP, credentials, local/IP destinations and nonstandard ports are covered.
- **Validation:** CI #999 passed for the registry/core tests. CI #1001 passed on combined source head 4f87acab1cf729275666df14944c8b950993be03: source invariants, remote-runner tests, :core:test, :app:testDebugUnitTest, :app:lintDebug, debug and instrumentation APK assembly, APK package/version/signature checks, and Android emulator in-place upgrade with encrypted-store/data preservation. Gradle reported BUILD SUCCESSFUL; APK package app.memo.android.debug, version code 1001. The pull-request workflow intentionally does not publish the APK or run isolated persistent signing; no downloadable signed APK is claimed. No physical-device test is claimed.
- **Known gaps retained:** Browser links/forms and provider prompt submission are not exposed by this Talk adapter; the latter remains in the older approval-gated browser workflow. Native Android, code/file and image/PDF/video adapters, global process-death invocation recovery, universal postconditions, full Work/Studio/Live execution projection and physical-device acceptance remain open.
- **Status:** [~] in progress; browser read/capture is an initial working vertical slice, not full browser automation or a completed Memo milestone.

### 2026-11-20 — WP-AGENT-003 — Verified same-origin link navigation in Talk

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-BROWSER-001.
- **Baseline:** PR #14 branch before this slice; WP-AGENT-001/002 had passed CI #1003.
- **Implementation:** Added `browser.open_observed_link` to the registered browser runtime and Talk adapter catalogue. The adapter is dynamically unavailable until a visible-page capture has completed and yielded safe same-origin links. It accepts only an exact HTTPS URL from that capture's bounded link set. The browser rechecks that the current page is still the captured source, confirms that the exact anchor remains visible in the live DOM, and then navigates to that target in the same visible WebView.
- **Safety boundary:** Page-derived navigation is restricted to the exact HTTPS origin (not sibling/subdomains), rejects insecure/local/IP/credentialed/nonstandard-port targets through the existing URL policy, never clicks an anchor or arbitrary page control, and clears the link set after navigation so another link requires a fresh capture. Page text and links remain untrusted data. No form filling or provider prompt submission is exposed.
- **Evidence and tests:** Added pure-core same-origin URL policy coverage and source-verifier assertions for live DOM verification, exact page/link binding, runtime registration and adapter readiness. Captured same-origin links are included in encrypted page-evidence metadata and returned to the model as untrusted references.
- **Validation:** CI #1018 passed on head `4000d03c5ce58249f5954acd4378fab2c3ef87a7`: source invariants, remote-runner tests, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, debug APK assembly, APK identity/signature and emulator install-over-existing encrypted-store/app-private-data preservation. The PR workflow skips delivery artifacts and isolated release signing. No real-site behavior or physical-device acceptance is claimed.
- **Status:** `[~]` implementation and CI validated; broader browser execution remains incomplete.


### 2026-11-21 — M3.A4-S1 — Fail closed on execution event sink failure

- **Requirement:** Never continue a model-directed run if its execution event cannot be persisted/delivered; avoid blind replay when a capability may have run.
- **Files changed:** `core/src/main/kotlin/app/memo/android/orchestration/AgentExecutionEngine.kt`; `core/src/test/kotlin/app/memo/android/orchestration/AgentExecutionEngineTest.kt`.
- **Implementation summary:** Event callback failures are wrapped and handled separately. When a capability call has started, the engine returns `NEEDS_REVIEW` and does not ask the model for another decision. The user-facing message tells them to inspect the action outcome before retrying. When event failure occurs before any capability, the run fails closed. A regression test makes the event sink throw after a capability result and asserts one model decision, one capability invocation, and `NEEDS_REVIEW`.
- **Tests and results:** Regression test executed successfully in CI #1033 on head edf6e618; see the run linked below.
- **Exact commit SHA:** `5a5eff69038cc49004a05964f9c8f9e1472d6910` (test commit; engine commits precede it on branch).
- **Device/emulator evidence:** Pending.
- **Known gaps:** Durable execution journal/process-death recovery, pausing/resuming approvals, global postcondition verification, broader adapter coverage remain open.
- **Status:** `[~]` source change + test authored, verification pending.


### 2026-11-21 — M3.A4-S2 — Talk transcript approval lifecycle coverage

- **Requirement:** Every execution event emitted by the shared engine must be safely and visibly represented in Talk.
- **Files changed:** `app/src/main/java/app/memo/android/ai/MemoWorkspaceAgentTools.kt`.
- **Implementation summary:** Added transcript mappings for approval requested, granted and rejected event types. The transcript metadata continues to include run/call IDs, status, capability version and evidence reference.
- **Tests and results:** Added `MemoWorkspaceAgentToolsTest.transcriptExplainsAllApprovalLifecycleEvents`, covering all three approval events and run/capability metadata. Compile/CI pending.
- **Exact commit SHA:** `fe1278a5bda0e742e31e9fc6b76441ec78c67e7`.
- **Known gaps:** Approval callback currently defaults to no grant in the Talk execution call site; this mapping is visibility only and does not implement durable approval pause/resume. Full approval UI/ledger path is still open.
- **Status:** `[~]` implementation pending CI.


### 2026-11-21 — M3.A4-S3 — Journal failure hard-stop invariant

- **Requirement:** An execution journal/event persistence failure must stop the execution loop; no further model/tool decisions after failure. If an action may have begun, outcome must be reviewed rather than replayed.
- **Files changed:** `docs/AI_EXECUTION_INVARIANTS.md`; execution guard already committed in `AgentExecutionEngine.kt`.
- **Implementation summary:** The rule is now part of the binding execution invariants, not merely a code comment. Approval lifecycle events must also be visible in Talk's transcript.
- **Verification:** Run 1026 (`5a5eff6`) failed compilation due to missing exhaustive approval-event transcript branches; this was corrected in `fe1278a`. Run 1027 on `e4fca2d` was still active at last check. Newer runs were queued/cancelled by subsequent commits. CI for current source remains pending.
- **Status:** `[~]` rule and code committed; verification pending.

 
### 2026-11-21 — M3.A4-S4 — ViewModel-owned Talk execution and exact approval broker

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-APPROVAL-001, MEMO-RECOVERY-001.
- **Files changed:** app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt; app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt; app/src/main/java/app/memo/android/ui/ActionApprovalInboxScreen.kt; app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt; tools/verify_source.py; docs/AI_EXECUTION_INVARIANTS.md; TASKS.md; IMPLEMENTATION_LOG.md.
- **Implementation:** Talk runs now belong to ViewModel scope and remain alive while the user changes destinations. The engine's existing approval callback stages an exact-content proposal in the existing encrypted action ledger, registers a waiter before suspending, and resumes only after the inbox durably approves the matching hash. Rejection returns no grant. The inbox exposes explicit approve/reject controls only for a pending action tied to the currently active Talk conversation; its decisions run in app-root scope so navigation cannot cancel durable read-back.
- **Termination:** leftover pending actions are durably rejected and unused approvals are moved to NEEDS_REVIEW; process restart still invalidates or flags actions and does not resume/replay the model run.
- **Tests:** structural source assertions added for ViewModel ownership, callback wiring, wait registration, inbox decisions and global Stop. Existing engine tests already exercise exact approval grants, rejection, mismatched envelopes and fail-closed behavior. CI #1038 passed the exact S4 head `07f100c6`: source verifier, remote-runner tests, Android unit tests, lint, debug APK assembly, APK identity/signature checks and install-over-existing preservation. The workflow uploaded diagnostics only and skipped APK publication/re-signing. The S5 changes below require their own CI run.
- **Known gap:** no external-write adapter is currently included in MemoWorkspaceAgentTools; this commits the generic approval handshake, not an end-to-end external side-effect capability.
- **Status:** [~] S4 CI verified; M3.A4 remains partial.

### 2026-11-21 — M3.A4-S5 — Exact approval review acknowledgement and durable decision ownership

- **Requirements:** MEMO-APPROVAL-001, MEMO-RECOVERY-001.
- **Files changed:** app/src/main/java/app/memo/android/ui/ActionApprovalInboxScreen.kt; app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt; app/src/main/java/app/memo/android/ui/MemoLocalization.kt; app/src/test/java/app/memo/android/ui/MemoLocalizationTest.kt; tools/verify_source.py; TASKS.md; IMPLEMENTATION_LOG.md.
- **Implementation:** approve is only rendered after expanding the action. The full destination and exact payload are then visible, and the user must explicitly acknowledge having reviewed them. Collapsing the card clears the acknowledgement. Rejection remains available at all times. The durable approval/rejection coroutines now belong to ViewModel scope and survive Activity recreation.
- **Validation:** PL/TR translation unit assertions and source-verifier checks added. Exact S5 head a6ed188c passed CI #1039: source verifier, remote runner, Android unit tests, lint, APK assembly, identity/signature and install-over-existing preservation. The PR event skipped APK packaging/re-signing and uploaded build reports only.
- **Build artifact:** CI #1038 uploaded only the 140,719-byte `Memo-Android-Build-Reports` ZIP; APK packaging/re-signing upload steps were skipped for the PR event. No new installable APK was delivered.
- **Known gap:** no external-write adapter is currently included in MemoWorkspaceAgentTools; this increments the approval broker/UI but does not claim an end-to-end external side effect is usable from Talk.
- **Status:** [~] implementation; verify exact commit in CI before marking complete.

 
### 2026-11-21 — M3.A4-S6 — Encrypted durable Talk run record and restart recovery

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-RECOVERY-001.
- **Files changed:** core/src/main/kotlin/app/memo/android/data/Models.kt; core/src/main/kotlin/app/memo/android/orchestration/AgentExecutionEngine.kt; app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt; app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt; app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt; app/src/main/java/app/memo/android/ui/MemoLocalization.kt; tools/verify_source.py; schema/engine/localization tests; TASKS.md; IMPLEMENTATION_LOG.md; docs/AI_EXECUTION_INVARIANTS.md.
- **Data compatibility:** additive workspace backup schema 7. Readers accept schemas 1–7; schemas 1–6 import with an empty agent-run history. Existing primary vault path, encryption format, Keystore alias and action ledger are unchanged.
- **Implementation:** run identity is allocated by Memo before model execution and injected into the engine. Run creation is durably read back before the engine starts. Lifecycle events update the run index and conversation TOOL transcript in one encrypted snapshot write; read-after-write verification gates the next model/capability action. Engine end events enter `FINALIZING`; only the same verified snapshot write as the final assistant/system message changes the record to terminal.
- **Recovery:** on unlock/import, nonterminal runs (including FINALIZING) become NEEDS_REVIEW; pending approval references are cleared and the saved recovery note explicitly says that no step was replayed. Runs with uncertain capability starts remain review-only. No automatic execution resume was added.
- **UI:** Live includes a recent Memo executions section with status, last recorded stage, model/capability counters, recovery reason, and links to conversation/approval review.
- **Tests:** deterministic-clock event reducer, exact event/run identity, approval/tool lifecycle, uncertain outcome, FINALIZING crash-window recovery, schema-v7 round-trip, schema-v6 migration, Council backup v7 assertion, restart recovery and PL/TR translations.
- **Validation:** CI run `44901392061` on source commit `adf58109af909358940e7361251669377e2a84d5` subsequently completed with all observed jobs successful, including source/runner checks, core and app unit tests, lint, debug/instrumentation assembly, APK identity/signature and install-over-existing data preservation. This supersedes the earlier exact-head pending note for S6; physical-device acceptance remains open. PR-event APK publication/re-signing was skipped, so no release APK is claimed.
- **Known limitation:** MemoWorkspaceAgentTools still does not expose an end-to-end external-write adapter. This milestone makes the generic run lifecycle durable/observable; it does not claim a submitted/sent action can yet be executed from every Talk request.
- **Status:** [~] source implementation; exact-head CI and physical-device acceptance required.


### 2026-11-21 — M3.A4-S7 — Shared capability postcondition verification

- **Requirement:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-LIVE-001.
- **Baseline:** main `a6c893c7808c5f042f9fe3cb8254b5ac5b90a2c4`; source head `adf58109af909358940e7361251669377e2a84d5`; baseline checkpoint `docs/checkpoints/2026-11-21-agent-postcondition-baseline.md`. Work is on `agent/postcondition-verification-20261121`; no changes were merged to main.
- **Files changed:** `core/src/main/kotlin/app/memo/android/orchestration/AgentExecutionEngine.kt`, `core/src/test/kotlin/app/memo/android/orchestration/AgentExecutionEngineTest.kt`, `app/src/main/java/app/memo/android/ai/MemoWorkspaceAgentTools.kt`, `tools/verify_source.py`, task/gap/traceability/invariant/product/build documents and checkpoints.
- **Implementation:** introduced `AgentToolPostconditionVerification` and a default-null verifier contract on `AgentCapabilityAdapter`. The shared engine invokes verification under the capability timeout, requires observable output, checks the evidence reference, and only counts/records `TOOL_FINISHED` after verification. Missing/failed/mismatched read-only verification becomes `FAILED` and may be re-planned; local/external/destructive uncertainty becomes `NEEDS_REVIEW`. Write verification must match the invocation's evidence ID.
- **Adapters:** search recomputes its bounded result and source IDs; note/proposal verifiers read exact workspace records; browser navigation compares the returned URL/title to the settled visible WebView; capture reopens encrypted evidence and checks source/content/link digests. A failed navigation clears old capture state before trying a new destination.
- **Tests:** six engine regressions cover missing read verifier, missing write verifier, mismatched write evidence, cancellation during write invocation, cancellation during write postcondition verification, and a write adapter returning `FAILED`. The latter three stop for review without re-planning. Existing successful adapter tests now provide test verifiers; the source verifier checks the protocol, all six adapters and interruption guards.
- **Compatibility:** no workspace/backup schema, encryption key, application identity, signing or external approval policy changed.
- **Validation:** PR exact-head CI run `44907881554` passed on `bfe40a182b1e283699a8c4c36bf9c612f83981c7`: source verifier, remote-runner tests, core/app unit tests, lint, debug/instrumentation assembly, APK identity/signature and install-over-existing data preservation. No local Gradle/Android SDK checkout; release signing skipped, so no canonical release APK is claimed.
- **Status:** `[~]` source implementation and exact-head CI verified; physical-device acceptance pending.

### 2026-11-21 — WP-INTEGRATION-001 — Bring verified execution integrity and UI release gates onto current main

- **Baseline:** `main` at `d80f19ae6ff956392cbe11c2b1aaead1758a7d19`; preserved the current Talk/Work scrolling repair from `d189a732416c241d1e5d94d6a20d6a7e11345034`.
- **Integrated source:** M3.A4-S7 shared capability postcondition verification from `agent/postcondition-verification-20261121` (exact source head `bfe40a182b1e283699a8c4c36bf9c612f83981c7`). Its exact-head Actions run `44907881554` completed successfully. The integration branch carries the same implementation and tests; it still requires its own CI run.
- **Integrated release governance:** binding UI regression contract, real-device Talk/Work scroll checklist and release-candidate integrity rules from `governance/memo-ui-regression-release-gate`.
- **Compatibility:** no workspace schema, backup schema, encryption format, Keystore alias, package/application identity, signing material or runtime permission was changed by this integration.
- **Verification:** upstream S7 exact-head CI PASS; current-main source import CI `44927379252` PASS as recorded in the import note; integration-branch CI pending. No local Android SDK/Gradle build and no physical-device acceptance are claimed.
- **Status:** `[~]` integrated on a dedicated branch; awaiting exact-head CI and device acceptance. Do not merge to main or distribute an APK without explicit review and the required acceptance gates.

### 2026-11-21 — M3.B1-S1 — Talk-created private Markdown artifacts

- **Capability:** added `workspace.save_artifact` to `RuntimeRegistry` and `MemoWorkspaceAgentTools`. It is a reversible local workspace write, not an external side effect. The artifact type is descriptive metadata and does not route to a specialized workflow.
- **Contract:** `StudioArtifactPolicy` validates bounded title/type/content and computes a canonical SHA-256 digest. The adapter stores a `STUDIO_ARTIFACT` in the encrypted evidence store with source conversation/message, source excerpt digest, content digest, version, draft status and run/call/idempotency metadata.
- **Verification:** the shared Talk engine requires the artifact ID, exact content digest and source provenance to be read back from the encrypted workspace before success. Duplicate drafts are reused only for the same content and source message; uncertain writes stop for review.
- **Studio:** the Studio list now includes Talk-created drafts. The existing Evidence dialog opens them for edit, revision history, copy and Markdown export. User edits refresh the content digest and increment the version; source captures remain immutable.
- **Tests/source gates:** added `StudioArtifactPolicyTest` for normalization, size limits and digest binding; added a runtime registration test; extended `tools/verify_source.py` to guard the adapter, verifier, Studio listing, edit allowlist and digest/version updates.
- **Boundary:** private Markdown text only. No filesystem code execution, PDF/image/video generation, publishing or external submission is claimed. Exact-head CI and physical-device acceptance pending.

### 2026-11-21 — M3.A4-S8 — Approved browser prompt submission from Talk

- **Capability:** added `browser.submit_prompt` to `MemoWorkspaceAgentTools` using the existing `runtime.browser` registration. It is classified `EXTERNAL_WRITE` and requires exact human approval; no unapproved invocation path sends a prompt.
- **Approval binding:** `AgentApprovalEnvelope` contains the normalized HTTPS destination and exact prompt. The adapter recomputes `ActionApprovalLedger.contentHash`, rechecks page availability after approval, and durably consumes the one-use action grant before touching the visible browser.
- **Visible action:** `BrowserSession.submitPromptForAgent` rechecks the same page, uses the existing visible-composer prepare/send scripts, verifies the exact prompt appears as a submitted message and the composer is cleared, then captures the settled page for a digest.
- **Evidence/action lifecycle:** saves encrypted `BROWSER_PROMPT_SUBMISSION` evidence with exact prompt, prompt digest, page digest, source conversation/message, action ID/hash and run/call IDs. The shared postcondition verifies the evidence and in-progress action record, then durably completes the action ledger against the evidence reference.
- **Failure policy:** changed/stale page, approval hash mismatch, uncertain send, cancellation, evidence write/read-back failure or action completion mismatch becomes `NEEDS_REVIEW`; the prompt is never automatically replayed.
- **Boundary:** visible text composer only; no general controls/forms, file uploads, provider response polling or native app actions. Reading page responses remains behind the separate page-content consent.
- **Validation:** explicit runtime policy regression and source-verifier guards added. Exact-head Android CI and physical-device acceptance pending.

### 2026-11-21 — M3.A4-S9 — Consent-gated stable browser response capture

- **Capability:** added `browser.capture_response` only to the Talk tool set when the user has separately enabled browser-page sharing. It requires the exact BROWSER_PROMPT_SUBMISSION evidence ID from the prior approved prompt action and verifies the action ledger is COMPLETED.
- **Baseline and stability:** `BrowserSession.captureAssistantResponseForAgent` re-verifies the original prompt submission baseline, polls the shared stable-response script with bounded provider-specific selectors, requires a new response, and captures the settled page. A lost baseline, unsupported structure or 35-second timeout returns NEEDS_REVIEW; it never resends the prompt.
- **Evidence:** persists a `BROWSER_PROMPT_RESPONSE` record with prompt-submission reference, action ID, prompt/response/page digests, source conversation/message, run/call IDs and capture timestamp. The postcondition reads back the exact response and source binding.
- **Studio visibility:** Studio now lists both BROWSER_PROMPT_SUBMISSION and BROWSER_PROMPT_RESPONSE records under Browser research evidence; each opens in the existing evidence viewer.
- **Trust boundary:** response text is returned with an explicit UNTRUSTED EXTERNAL RESPONSE label. The adapter is not advertised without page-sharing consent and is a local evidence write, not another external side effect.
- **Boundary:** provider-specific stable response detection only; arbitrary page events, general controls/forms, uploads and native-app actions remain open.
- **Validation:** runtime policy regression and source-verifier guards added. Exact-head Android CI and physical-device acceptance pending.

#### CI result — M3.B1-S1 artifact slice

- Run `44953421742` on commit `ae6ef31976d7f35339715a16933ec73e21210af5` passed: source invariants/checksum verification, remote runner, core and app unit tests, lint, debug APK assembly, package/version identity, APK signature verification, and emulator install-over-existing with app-private data preservation.
- The run predates the subsequent M3.A4-S8/S9 browser prompt/response additions. It does not prove those later adapters, physical-device behavior or release signing.

#### CI result — M3.A4-S8 approved browser prompt submission

- Run 44954196829 on commit 23073011b9c411945c12531ce8483ac0a6d2b4b5 passed source checksum/invariant verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, package/version identity, APK signature verification and emulator install-over-existing with app-private data preservation.
- Release-signing job was skipped. This run includes the prompt-submission adapter but predates M3.A4-S9 response capture and the later Studio browser-evidence list. Physical-device approval/rejection/stale-page/uncertain-send acceptance remains pending.

#### CI result — M3.A4-S9 consent-gated browser response capture

- Run 44954878877 on commit bac787645ee85958132d56e8524c53325b536c06 passed source verification, remote-runner tests, Android unit tests, lint, debug APK assembly, package/version identity, signature verification and emulator install-over-existing app-private data preservation.
- Release artifact preparation and isolated release signing were skipped. The run does not prove physical-device browser behavior. Later local/remote consent wording and metadata-only changes are documented separately; no functional browser adapter changes followed this validated source slice.


### 2026-11-22 — M3.B2-S1 — Per-run Live execution timeline

- **Requirements:** MEMO-LIVE-001, MEMO-ORCH-001, MEMO-RECOVERY-001.
- **Baseline:** integration branch `build/product-core-integration-2026-11-21`, exact parent `a9de80ef22b4ab2b6939d68db32b7a36c9ee316a`; current Memo 1 repository only. No Memo-2 changes.
- **Implementation:** added a read-only Live projection that finds `AGENT_EVENT` messages in the run's originating encrypted conversation by the exact Run ID field in the final metadata line, orders them by their persisted timestamps, and presents the most recent four events. Each event retains its saved summary and status/evidence metadata. Live also shows run start/update times, the saved final response and the latest evidence reference. The conversation link remains the full durable trace.
- **Compatibility:** no schema or backup changes; no new store or event journal; package ID, encryption format, Keystore aliases, storage paths and signing configuration are unchanged.
- **Tests:** new app unit tests cover exact run correlation, combined metadata fields, event source filtering, chronological ordering, missing conversations and EN/PL/TR timeline labels.
- **Verification:** exact-head GitHub Actions validation pending. No local Gradle/Android SDK build or physical-device acceptance was performed in this run. No release APK is claimed.
- **Remaining:** cross-runtime Live event projection/control, pause/resume/takeover, global stop/action gate and physical-device acceptance.

### 2026-11-25 — M3.A4-S10 — Browser submission safety correction

- **Starting point:** main b979d103432f710d6d67bf91abcfbc0dfd5c9063; branch memo/mission-1-safe-browser-submission.
- **Observed defect:** prompt preparation could replace a non-empty composer draft; send accepted substring matches; verification could accept an unrelated new user message or page-wide prompt prefix.
- **Correction:** preserve any different non-empty composer draft; require exact normalized composer equality before Send; verify a new exact user-authored message and an empty visible composer. Unknown provider DOM structures stop for review. The Talk adapter surfaces the page script's concrete failure explanation so draft preservation is visible to the user.
- **Tests and contracts:** added BrowserPromptSubmissionSafetyTest, source-verifier guards and the exact-submission rule to docs/AI_EXECUTION_INVARIANTS.md.
- **Data/signing compatibility:** no persistent schema, encryption, package identity, signing or user-data changes.
- **Validation:** PR #24 exact-head Actions run #1185 is pending; earlier superseded runs are not acceptance evidence. Physical-device verification is still required.


## 2026-11-27 — MEMO-ORCH-001 — Fail-closed persisted workflow graph decoding

- **Baseline:** `main` at `f628517699b51cac28c09c8fd0723a1334cf0f9f`; feature branch `fix/strict-workflow-graph-decoding-2026-11-27`.
- **Reason:** `WorkflowGraph.fromJson` is used by workspace backup validation/import as well as Work-plan reads. Its previous use of `optString`, `optBoolean`, `optLong` and silent string truncation could accept malformed serialized graphs as altered graphs. Missing `externalSideEffect` could be defaulted to false, and malformed numeric values could be coerced.
- **Change:** replaced permissive reads with required typed readers; string and array bounds are checked without truncation; booleans and whole-number limits are type/range checked; budget and node arrays are mandatory; graph validation still runs after decoding. Original schema-v1 graphs written before `externalSideEffect` was added remain readable only when every node omits that field; legacy action nodes recover the old `externalWrite`-checkpoint classification, consent-only gates remain non-side-effect, and mixed representations fail closed.
- **Tests:** added regressions for overlong strings, mixed side-effect classification, non-string capability values, wrong boolean types, fractional timeouts, missing budget and legacy schema-v1 round-trip compatibility.
- **Compatibility:** valid current `toJson()` output is unchanged, and original schema-v1 graphs without `externalSideEffect` remain readable when the field is absent from every node; legacy side-effect classification is reconstructed from matching `externalWrite` approval dependencies, preserving the pre-field validator semantics. No workspace/backup schema, encryption/key alias, package ID, signing identity or persisted user data changed.
- **Boundary:** no DAG executor or new capability was added; saved plans remain DRAFT and non-executing.
- **Verification:** exact-head GitHub Actions run #1288 (`45994138742`) on commit `470ba4d149ea9caaeba8c7e651f4e67df40d87b3` passed source verification, remote-runner tests, core and Android unit tests, lint, debug/release assembly, APK identity/signature checks and emulator release-package install-over-existing with both encrypted stores plus legacy recovery/corruption tests. The dependency graph was disabled, so no complete dependency assessment is claimed. PR validation skipped APK publication and final pinned signing; no installable release APK was produced. Physical-device acceptance remains pending.


### 2026-11-27 — MEMO-ORCH-001 — Strict model-proposed Work envelope

- **Baseline:** stacked on fix/strict-workflow-graph-decoding-2026-11-27 at 1d679f8bb317a34b88d884ff8a4c4d3b45fbbf2d; Memo 1 only.
- **Finding:** the persisted graph decoder is being hardened separately, but the upstream untrusted model-proposal parser still accepted schemaVersion through optInt and goal through optString. These accessors can coerce malformed JSON values.
- **Change:** proposal schema version now uses the parser's whole-number validation and must equal 1; goal must be a JSON string before normalization and length validation. Numeric-field diagnostics now use a scope-neutral field label because the helper also validates the root envelope.
- **Tests:** reject string, fractional, boolean and missing schema versions and reject a non-string goal; retain the existing valid proposal, capability resolution, approval gate, cycle and limit tests.
- **Compatibility:** valid proposals remain unchanged. No workspace/backup format, encryption, key alias, package ID, signing identity, or persisted user data changed.
- **Boundary:** proposals are still validated and saved as DRAFTs; this does not add DAG execution or bypass the shared Talk dispatcher.
- **Verification:** exact-head GitHub Actions run #1292 (`45998744182`) on commit `d5cdbb83a1f8770dc99e2f32592d9622e3712c42` passed source verification, remote-runner tests, core and Android unit tests, lint, debug/release APK assembly, APK identity/signature checks and emulator install-over-existing/data-preservation. PR validation skipped protected persistent signing and APK publication; no installable release APK is claimed. The repository Dependency Graph remains disabled, so no complete dependency assessment is claimed. Physical-device end-to-end Talk plan creation remains open.
