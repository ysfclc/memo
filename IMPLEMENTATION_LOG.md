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


### 2026-11-11 — Local-model Work checkpoint lifecycle

- **Requirements:** MEMO-ORCH-001, MEMO-LIVE-001, MEMO-AI-001; TASKS M3.A3, M3.A4, M3.8.A7.
- **Baseline:** `stabilization/usable-stage-2026-11-10` at `3d9db9b7ace91a751803b7c476eb5dc8c36007b7`; fresh-start checkpoint `docs/checkpoints/2026-11-11-local-work-execution.md`.
- **Gap found:** the Work local-model callback invoked inference directly. It did not reserve/start the saved checkpoint through `WorkflowCheckpointCoordinator`, so the dependency/runtime gate and persisted RUNNING transition could be bypassed.
- **Implementation:** Work now calls `startJobStep` before building the execution context or invoking local inference. It reads the post-start workspace snapshot for context. A successful response is still saved as a private `WORK_ARTIFACT` requiring human review. Model failure and evidence-save failure both move the checkpoint to `NEEDS_REVIEW` and the job to `WAITING_FOR_USER`; neither path leaves a phantom active step or reports completion.
- **Regression guard:** `tools/verify_source.py` now checks that local inference is ordered after the persisted start gate and that review/failure states remain wired. Existing `WorkflowCheckpointCoordinatorTest` covers dependency refusal, active-step exclusion, runtime/capability revalidation, external-action refusal and truthful user-reported completion.
- **Validation:** PR validation runs #379–#385 failed in preflight with no runner steps/logs; retrying the latest failed preflight also failed. Source verifier, Gradle suites, lint and APK assembly were skipped. No build, emulator or physical-device success is claimed.
- **Boundary:** this is one local-model runtime adapter path, not a durable cross-runtime DAG executor. Attempt journal/idempotency, typed runtime receipts, timeout/cancellation propagation, pause/resume/recovery and postcondition verification remain open.
- **Follow-up hardening:** local-model draft persistence now commits the evidence record, NEEDS_REVIEW step state, WAITING_FOR_USER job state and activity entry in one workspace snapshot. The failure path also moves the active checkpoint to review in one snapshot. A stale/non-RUNNING step is rejected rather than receiving an orphan artifact. Source verifier now guards these atomic paths.
- **Status:** implemented in source; verification pending. Latest exact-head PR run #388 again failed before exposing any preflight steps/logs; no Gradle/build result is available.

- Follow-up: extracted local Work artifact transitions into a pure-core coordinator with six JVM regression tests; Android ViewModel persists the returned snapshot atomically. Exact-head run #397 failed before steps/logs; tests remain unexecuted.


### 2026-11-11 — Vercel Android APK build experiment

- Inspected the linked `memo-android-build` Vercel project. Earlier READY deployments were empty no-op builds: Vercel ran its default build in 238 ms and published no APK.
- Added a real preview build command that provisions checksum-verified JDK 17 and Android command-line tools, installs API 35/build-tools 35.0.0, runs source and Python checks, core/app JVM tests, lint, Android-test APK assembly, and debug APK assembly.
- The Vercel output contains the APK, build identity, and SHA-256 checksums. When the configured Blob token is available, the APK is uploaded to a commit-addressed public Blob path so deployment SSO does not block the download.
- The Vercel project currently has no persistent Memo upload-keystore credentials. This experiment therefore produces an ephemeral debug-key-signed test APK, not an upgrade-compatible replacement for the pinned-certificate APK. Do not uninstall the existing app to test it.
- Preview deployment and exact build results are pending. See `docs/checkpoints/2026-11-11-vercel-android-build.md`.

- **Vercel bootstrap correction:** the first preview run (# deployment `dpl_HCqXbg6z4dpxtbxsD5CxZm1ZKM3L`) started the custom build but Adoptium metadata returned HTTP 403 before JDK installation. Replaced that dependency with a pinned Temurin 17.0.20.1+1 GitHub release asset and its SHA-256. Retry is pending.

- **Retry blocked:** after pinning Temurin 17.0.20.1+1, Vercel refused a new deployment with HTTP 402 `api-deployments-free-per-day` (>100 deployments/day; retry-after 86,400 seconds). The Vercel Sandbox API also rejected creation. The current branch is prepared, but no APK has been produced. Do not call this a successful Vercel build.


2026-11-11 — Vercel build diagnosis corrected: the failed deployment ran the pre-fix commit and actually failed when Python urllib fetched Google's Android SDK repository manifest (HTTP 403), before SDK installation. Added explicit browser-compatible User-Agent and XML Accept headers to that manifest request. Updated checkpoint; awaiting an exact-head Vercel build to validate the change. No APK produced or claimed.


### 2026-11-12 — Durable workflow attempt journal

- **Requirements:** MEMO-ORCH-001, MEMO-LIVE-001, MEMO-SECURITY-001; tasks M3.8.A7 and M4.L4.
- Added `WorkflowAttemptRecord` and a pure `WorkflowAttemptJournal` state machine. Each attempt records job/step, attempt number, stable idempotency key, exact runtime/capability manifest, start/finish timestamps, result state, evidence reference and a separate user-reported outcome.
- Workspace backup format is now schema 7. Schema 1–6 backups remain importable; schema 7 requires a bounded, strictly parsed attempt array. Serialization/import validation rejects duplicate IDs, malformed sequences, missing job/step/evidence references, changed TaskSpec bindings and attempts attached to approval-gated/external nodes.
- Checkpoint start appends the attempt in the same immutable snapshot as the RUNNING job/step transition. Local output save closes it as OUTPUT_SAVED in the same snapshot as evidence and NEEDS_REVIEW; model failure closes it as FAILED. Process recovery closes RUNNING attempts as INTERRUPTED and leaves the checkpoint for human review.
- User-reported outcomes are retained separately from model output. Manual retries preserve the node idempotency identity and stop after four attempts. This is not automatic retry or external side-effect replay.
- Live now projects attempt state and runtime/capability references without rendering private model prompt/output payloads. Work checkpoint cards also show the latest attempt number/status/runtime, preserve the user-reported outcome separately, allow another local draft without discarding prior review evidence, and remove the retry action at the four-attempt cap.
- Artifact linkage is checked against the exact job and step IDs in its metadata, as well as the human-review flag; an artifact from another checkpoint in the same job cannot close the attempt.
- Regression tests were added/updated for start binding, retry ceiling, output/failure closure, process-death recovery, schema-7 round trip, schema-6 migration, tamper rejection and Live projection.
- **Validation:** source and test changes are committed; exact-head Gradle/JVM/lint/build verification remains pending because GitHub Actions PR preflight failed before downstream jobs and the log artifact endpoint returned BlobNotFound. No local checkout, Gradle distribution or Android SDK is mounted. No APK/device acceptance is claimed.
- **Boundary:** local-model and user-reported checkpoint lifecycles only. Browser/native/deterministic runtime adapters, typed runtime receipts, shared budgets/timeouts, cancellation propagation, pause/resume/takeover and independently observed postconditions remain open. PR #8 remains draft; main and Memo 2 are untouched.

- A standalone Kotlin compiler/smoke harness for the attempt state machine passed: start, failure closure, stable retry identity, user-reported outcome, process recovery and journal validation. This harness used lightweight stubs and is not a substitute for the real Gradle/JVM suite.
- Latest exact-head GitHub Actions run #462 failed in the ZIP-preflight job before runner assignment (`runner_id: 0`, zero steps); downstream build/tests/lint/APK jobs were skipped. The log endpoint is unavailable. Full project verification remains pending.

- Exact-head PR run #480 (`c50b22c581d404bd43524d2a3422a2cfae382be9`) repeated the preflight failure: incoming ZIP job failed before runner assignment (`runner_id: 0`, zero steps); all source validation, Gradle, lint and APK jobs were skipped. No full build result is available.

- Latest exact-head run #483 (`9d098c33f94b656b241026559565459c07b53c91`) also failed before runner assignment: ZIP-preflight job `runner_id: 0`, zero steps; all downstream jobs skipped. This confirms the Actions runner/preflight blocker is still present on the current branch head.


### 2026-11-12 — M3.8.A8 — Bound local inference and propagate cancellation

- **Requirement:** Prevent a local model request from occupying the single LiteRT engine indefinitely; preserve cancellation semantics and never represent a timed-out response as completed output.
- **Baseline:** `stabilization/usable-stage-2026-11-10` at the durable attempt-journal checkpoint; `WorkflowAttemptJournal` remains the durable Work attempt record.
- **Files changed:** `app/src/main/java/app/memo/android/ai/LocalModelRunner.kt`, `app/src/test/java/app/memo/android/ai/LocalModelRunnerPolicyTest.kt`, `tools/verify_source.py`, `TASKS.md`, `CHANGELOG.md`, `docs/checkpoints/2026-11-12-workflow-attempt-journal.md`.
- **Implementation:** Local inference now runs under a 180-second coroutine deadline that includes model initialization and response collection. Timeout cancellation reaches the LiteRT conversation's `cancelProcess()` handler and is surfaced as a dedicated inference-timeout failure; normal parent/user cancellation remains a coroutine cancellation. Partial chunks may remain visible, but the runner never returns them as a completed response.
- **Tests added:** `LocalModelRunnerPolicyTest` asserts the bounded deadline and distinct timeout failure contract. `tools/verify_source.py` now guards the timeout wrapper, cancellation hook and test presence.
- **Verification:** Source inspection confirms the timeout encloses the mutex and serialized engine dispatcher, and cancellation is caught inside the conversation lifecycle. Full Gradle/app unit tests, lint and device behavior remain unverified because the latest Actions preflight failed before runner assignment; no Kotlin build result is claimed.
- **Risk / limitation:** Coroutine timeout is cooperative. If a native LiteRT call blocks without returning or checking cancellation, Kotlin cannot forcibly terminate that native call; the single-thread engine remains serialized until the native call unwinds. Device-level timeout/cancel latency must be measured.
- **Status:** `[~]` implemented in source; pending Gradle and real-device acceptance.
