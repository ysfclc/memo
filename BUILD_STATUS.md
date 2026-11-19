# Latest exact-head Android-verified increment — 19 November 2026

**STATUS: RUN #928 ATTEMPT 2 PASSED ON THE EXACT BRANCH HEAD; DEBUG APK BUILT, VERIFIED, API 35 IN-PLACE DATA PRESERVATION PASSED, AND ISOLATED PINNED SIGNING PASSED. PHYSICAL-DEVICE ACCEPTANCE REMAINS OPEN.**

- Exact tested source commit: `7ad0eb0426f953a8946741c59247b081a258f94a` on `stabilization/usable-stage-2026-11-10`; PR #8 remains draft/open and unmerged; `main` is untouched.
- GitHub Actions [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135): all jobs succeeded. The earlier attempt 1 failed only while downloading/unzipping the Android 35 Google APIs emulator image; rerunning the failed job succeeded.
- Source verification: PASS, including **434/434** static UI Text literals translated (532 scanned; URLs/placeholders excluded), 78 required integration files, durable-input/approval guards and encrypted-store migration invariants.
- Remote-runner tests: PASS. Gradle: **BUILD SUCCESSFUL**. Core JVM tests: **188/188 passed**. Android unit tests: **44/44 passed**. Android lint: **0 errors, 42 warnings, 5 hints**. Debug APK and Android-test APK assembly: PASS.
- API 35 install-over-existing acceptance: PASS. A lower-version baseline was upgraded in place; the signed APK preserved app-private data and both real encrypted Memo stores. Legacy recovery and corruption tests passed. No uninstall or clear-data operation was used.
- APK identity: package `app.memo.android.debug`, versionCode `928`, versionName `4.0.0-memo-debug`.
- Final installable artifact: [`Memo-Android-debug-APK`, artifact ID `14565824727`](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727). The ZIP contains `Memo-Android-debug.apk`, identity report, source fingerprint and SHA-256 manifest.
- APK SHA-256: `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`.
- Artifact ZIP SHA-256: `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`.
- Persistent signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a` (matches the pinned existing Memo certificate).
- Source fingerprint: `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- Build reports: [artifact ID `14566587233`](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14566587233).
- APK size: 114,841,689 bytes (about 110 MiB); the bundled LiteRTLM native runtime libraries account for much of the size.
- **Still pending:** physical-device in-place upgrade, visual/touch/navigation acceptance and real signed-in provider walkthrough on the user's existing phone. Do not claim phone acceptance; do not uninstall Memo or clear app data.
- No merge, production release/deployment, signing identity change or destructive migration is authorized. PR #8 remains draft; `main` remains untouched.

---

# Latest exact-head Android-verified increment — 18 November 2026

**STATUS: UIX-05 EXACT-HEAD CI #821 PASSED; API 35 IN-PLACE DATA-PRESERVATION AND ISOLATED PINNED APK SIGNING PASSED. PHYSICAL-DEVICE UX ACCEPTANCE REMAINS PENDING.**

**19 November status note:** Later product-governance, documentation, legacy-router annotations and source-verifier changes have been committed after this tested source SHA. Run #821 below verifies only `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0`; it does not verify the newer branch head. Consult `docs/STATE.md` for the exact current branch head and validation boundary. Do not treat this as a current-head build claim.

- Exact tested source commit: `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0` on `stabilization/usable-stage-2026-11-10`; PR #8 remains draft and unmerged; `main` is untouched.
- GitHub Actions run **#821**, ID `44211136288`: [successful exact-source build and signing](https://github.com/ysfclc/memo/actions/runs/44211136288).
- UIX-05 moves browser workflows, browser tasks and evidence into Work; Live remains the active browser/phone runtime and approval surface. Parent navigation, Android Back, primary selection and system-menu return context agree. Existing runtime/data handlers are reused.
- Passed source verification, 432/432 static UI translation checks, remote-runner tests, **178 core tests**, **43 app unit tests**, Android lint (0 errors; 42 warnings and 5 hints), debug and Android-test APK assembly, package/version/signature verification.
- API 35 in-place upgrade passed: lower-version baseline was upgraded with `adb install -r`; app-private data and both encrypted Memo stores reopened successfully; legacy recovery and corruption checks passed.
- Final installable artifact: [`Memo-Android-debug-APK`, artifact ID `14387410285`](https://github.com/ysfclc/memo/actions/runs/44211136288/artifacts/14387410285). The archive contains `Memo-Android-debug.apk`, identity report and SHA-256 manifest.
- APK identity: package `app.memo.android.debug`, versionCode `821`, versionName `4.0.0-memo-debug`.
- APK SHA-256: `55c25716c5d7540e5bdf15b86183c78bd260bdc22b29d774832e1d45c0a02dfb`.
- Artifact ZIP SHA-256: `3a21448731d10aae5fdbb3e2c16561f8185813c433ea110306f4e6d9e32a3ab2`.
- Persistent signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a` (matches the previously installable Memo certificate).
- Source fingerprint: `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- Build reports: [artifact ID `14386322351`](https://github.com/ysfclc/memo/actions/runs/44211136288/artifacts/14386322351).
- **Still pending:** physical-device visual, touch and Back-navigation acceptance on the user's existing phone. Do not claim phone acceptance; do not uninstall Memo or clear app data.
- No merge, release, production deployment, signing identity change or destructive migration is authorized. PR #8 remains draft; `main` remains untouched.

---

# Current verified increment — 16 November 2026

**STATUS: M3.8.A10 EXACT-HEAD CI PASSED. PHYSICAL-DEVICE ACCEPTANCE AND FINAL PINNED APK HANDOFF REMAIN PENDING.**

- Exact tested source commit: `989ac28fb6b0da3e5fc66525b1f7b3aff36329df` on `stabilization/usable-stage-2026-11-10`; PR #8 remains draft/unmerged; `main` untouched.
- GitHub Actions run **#767**, ID `43816799963`: [successful exact-head validation](https://github.com/ysfclc/memo/actions/runs/43816799963).
- Passed: source checksum refresh and verifier; remote-runner tests; `:core:test`; `:app:testDebugUnitTest`; `:app:lintDebug`; `:app:assembleDebug`; `:app:assembleDebugAndroidTest`; APK package/version/signature checks.
- API 35 emulator upgrade test passed: signed install-over-existing preserved app-private data and both real encrypted Memo stores; legacy recovery and corruption tests passed. No uninstall or clear-data step was used.
- M3.8.A10 remains partial at product/device level: the deterministic app.entities/entity.propose path is implemented and automated gates pass, but physical-device Talk → Work review acceptance is not yet performed.
- This was a pull-request validation event. Debug APK assembly and emulator checks passed, but pinned signing and final APK artifact handoff are intentionally skipped for PR events. Only the build-report artifact exists; no new signed installable APK is claimed.
- No merge, release, production deployment, signing identity change or destructive migration is authorized. Do not uninstall Memo or clear app data.

---

# Current source increment — 16 November 2026

**Status: SOURCE INTEGRATION COMMITTED; EXACT-HEAD CI PENDING. NO NEW APK OR DEVICE ACCEPTANCE CLAIMED.**

- Increment: M3.8.A10, first deterministic Memo app-runtime capability for app.entities/entity.propose.
- Talk outcome extraction now dispatches through the validated runtime request and durable checkpoint attempt. Exact source-message excerpts are required; records are saved as PROPOSED and remain subject to human review.
- Runtime output is persisted with the existing content-bound receipt. A separate read-only Work observer checks persisted entities and source provenance before a separate WORKFLOW_VERIFICATION record can complete the checkpoint.
- Regression tests cover the full adapter/observer path, missing source evidence, duplicate handling and missing persisted records. Static verifier guards the integration.
- Source commit and exact-head Actions run are pending final repository synchronization. No APK artifact, checksum or signing result is attributed to this increment until a successful exact-head run is observed.
- Physical-device update/product-flow acceptance remains pending. Do not uninstall Memo or clear app data. PR #8 remains draft/unmerged; main, signing identity and production deployment remain untouched.

---

# Current stabilization status — 15 November 2026

**Status: EXACT-HEAD BUILD, TESTS, API 35 IN-PLACE UPGRADE AND PINNED APK SIGNING VERIFIED; PHYSICAL-DEVICE ACCEPTANCE PENDING.**

- Exact tested source commit: `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34` on `stabilization/usable-stage-2026-11-10`; PR #8 remains draft and unmerged.
- GitHub Actions run **#678**, ID `43666486553`: [successful exact-head build](https://github.com/ysfclc/memo/actions/runs/43666486553).
- Passed: source invariants/checksum verification, remote-runner tests, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, APK identity/signature checks, API 35 encrypted-store in-place upgrade instrumentation and isolated pinned signing.
- Build identity: package `app.memo.android.debug`, versionCode `678`, versionName `4.0.0-memo-debug`.
- Final artifact: [`Memo-Android-debug-APK`, artifact ID `14143185526`](https://github.com/ysfclc/memo/actions/runs/43666486553/artifacts/14143185526).
- APK SHA-256: `026a9be7743e1e91cbc3b7807bed19504db6ea93b3d9249c91046ea15100f469`.
- Artifact ZIP SHA-256: `cbc98602361f95d7383c2f7ccf6bdf92c62347c1fa1ce32435b96e9693ea8e6e`.
- Pinned signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- API 35 upgrade test passed: baseline encrypted workspace/native stores were seeded, the new APK installed with `adb install -r`, both stores reopened, and legacy recovery/corruption checks passed. No uninstall or clear-data step was used.
- **Still pending:** physical-device update and product-flow acceptance on the user's existing phone. The user installed the earlier #674 APK but has not checked it. Do not claim phone acceptance; do not uninstall Memo or clear app data.
- This is a core runtime-contract increment, not a user-visible workflow feature. Concrete runtime adapters and postcondition observers remain next work.
- No merge, release, production deployment, signing-key change or destructive data migration is authorized. PR #8 and `main` remain untouched.

---

---

# Verified APK evidence — 8 November 2026

- **Workflow:** [Memo Android build #223](https://github.com/ysfclc/memo/actions/runs/42530596381), run ID `42530596381`, completed successfully.
- **Built commit:** `c4cfba812362fd5f69cc72026a935fe77a4d6dfc`.
- **Artifact:** `Memo-Android-debug-APK`, artifact ID `13637183644`.
- **APK:** `Memo-Android-debug.apk`, package `app.memo.android.debug`, version code `223`, version name `4.0.0-memo-debug`.
- **APK SHA-256:** `670acd9192f4b02e7aea1c23e456036b2ad3a98f5d48d57ef7c895a72e8d22b2`.
- **Pinned signing certificate SHA-256:** `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Source continuity:** the 9 November baseline `9edf9876010371a6619d0f7a43fd5024a90c904b` is one commit after the built source. Its only changed paths were `BUILD_STATUS.md`, `SOURCE_SHA256SUMS.txt`, `notes/README.md` and one import-history note; Android source paths were unchanged.
- **Not established by this record:** the newer UX/navigation branch, the current mainline integration candidate, and physical-device install-over-existing acceptance. A fresh CI build is still required for any newer integration commit.

This dated evidence supersedes older historical statements below that say no APK had ever been built. Those statements remain as records of earlier checkpoints; do not rewrite their historical context.

---

# Build and verification status — 8 November 2026

## Current source increment — typed TaskSpec DAG proposal boundary (CI pending)

- Added the canonical typed TaskSpec contract, dynamic fan-out/join DAG compiler and exact-runtime capability assembly.
- Added a strict parser for untrusted model task proposals. Runtime/capability mismatches, malformed contracts, cycles, invalid budgets and unapproved external writes fail closed. Memo generates idempotency keys itself.
- Added core JVM regression tests and static source-verifier checks. Existing schema-v1 graph serialization is preserved.
- This is not yet connected to Talk's model planner and does not execute tasks. Durable node state, runtime dispatch, policy integration, pause/resume and postcondition evidence remain future P2 work.

### Previous source increment — encrypted storage isolation (acceptance pending)

- Working branch: `ysfclc-patch-1`; latest inspected source/workflow/acceptance branch snapshot before this status-only update: `1a9a8f4ca84b8dfe7257ebfa270e68c586885d89`.
- `EncryptedVault` and `SynthStore` now use separate canonical files while preserving their original Android Keystore aliases, encryption envelopes and schema. The old `synthos-vault.bin` remains untouched as a migration source.
- Legacy ciphertext must authenticate with the corresponding existing key before it is copied. Corrupt, unknown or damaged-header payloads fail closed. AtomicFile workspace backup recovery is handled.
- Added Android instrumentation coverage for real encrypted workspace and native workflow records, version-increasing signed install-over-existing, both legacy migration paths, independent writes, corruption, damaged headers and unknown formats. The test uses isolated private storage to avoid touching user data.
- CI has been updated to build the instrumentation APK and run seed / verify around `adb install -r` on an API 35 emulator. Direct source changes use the checked-out branch source; the historical ZIP is not re-extracted over newer source.
- The workflow was restored to one preflight/build path after a duplicated tail was caught in review. Direct-source mode is now a single checked-out-source path. `BUILD_STATUS.md` is excluded from triggers because CI itself writes that file; the source verifier enforces this to prevent recursive builds.
- The vault-error screen now exposes both plain JSON and passphrase-encrypted backup restore; share-sheet capture waits until vault recovery succeeds. The instrumentation fixture lives in its own persistent private-files subdirectory, separate from Memo's root vault files.
- Instrumentation also simulates an interrupted AtomicFile write by restoring a valid encrypted workspace from the .bak file, and verifies that the backup is consumed into the canonical file.

### Verification not yet confirmed

- **GitHub Actions:** the connected repository status/run lookup returned no checks or workflow-run records for the inspected source commit. This is not a passing result and does not establish whether Actions started. Inspect the workflow run and its artifacts before treating the increment as verified.
- **Android build:** Kotlin/Java compilation, `:core:test`, `:app:testDebugUnitTest`, lint, APK assembly, instrumentation execution and emulator upgrade acceptance have not been observed for this increment.
- **Local environment:** no usable Gradle installation or Android SDK is available to execute the Android build here.
- **Physical device:** no install-over-existing test has been performed on the user's phone. Emulator success, when obtained, is not a substitute for this gate.
- The 5 November run #99 and its APK remain historical evidence for the then-current source; they do not prove this 8 November change.

**Status: source implementation committed; CI/device acceptance open. Do not distribute a new APK or mark M0.C1/M0.C2 complete until the exact branch revision passes the full workflow and the physical-device checklist is recorded.**

# Build and verification status — 28 October 2026

## Verified in the current source workspace

- `python3 tools/verify_source.py`: passed. Checks required integration files, confirms the `:core` module contains no Android framework imports, parses Android XML, checks microphone/notification/boot permissions and receivers, verifies language/notification catalog markers, and confirms CI accepts only the canonical ZIP, verifies its per-file SHA-256 manifest, checks the Memo-only launcher boundary, and includes core/app test, lint, and APK tasks.
- `python3 -m unittest discover -s tests -v` from `remote-runner/`: **12/12 tests passed**. Tests cover bearer-token authentication, explicit approval, workflow bounds, sensitive-text rejection, durable interrupted-job status, and final postcondition verification.
- All **10 Android XML files** parse successfully.
- The current source tree contains the persisted English/Polish/Turkish language preference, exact-string translation catalog/tests, tap-to-talk input, local notification center, daily reminder scheduling, workflow status notifications, and their manifest permissions/receivers.
- Cross-build feature inventory exists in `FEATURE_AUDIT.md`; priorities and acceptance criteria exist in `PRODUCT_GAP_REGISTRY.md`.

## Not verified — do not claim as passed

- Android/Kotlin compilation, Gradle JVM tests, Android lint, APK assembly, emulator tests, Accessibility behavior, real-device voice recognition, notifications, app automation, and remote paired-device behavior.
- I attempted `./gradlew --version`; its pinned Gradle download failed because this environment could not resolve `services.gradle.org`. No Android SDK/Gradle installation is available to finish the build locally.
- No APK is included or claimed to have been built. Use `.github/workflows/build-apk.yml` in GitHub Actions as the build gate, then install the resulting APK and complete `DEVICE_ACCEPTANCE_CHECKLIST.md`.

## Current release status

**Development source package; not release-certified.** Do not distribute as production-ready until Android CI is green, real-device acceptance is complete, the remaining product gaps are reviewed, and release signing/security checks have been completed.


## Director's Cut source reset — 28 October 2026

- Replaced the old duplicate Activity UI with `app.memo.android.runtime.MemoRuntimeBridge`; the accessibility service now reports status through Memo's listener boundary.
- Recentered the home experience on unsent conversational capture, linked records/sources, review attention and work continuity.
- Renamed active Compose app, viewmodel and design-token entry points to Memo-specific names. Extracted platform-independent domain contracts and deterministic rules into a new `:core` JVM module. Existing encrypted-vault aliases and filenames are intentionally unchanged.
- Updated `tools/verify_source.py` to cover the new product boundary and prompt handoff.
- Local Gradle compilation remains unavailable in this environment because the wrapper distribution cannot be downloaded. Run the delivered GitHub Actions workflow before treating this archive as build-verified.


## 28 October 2026 — exact-content approval slice

### Verified in this workspace

- `python3 tools/verify_source.py`: passed after adding the exact approval dialog's Polish/Turkish strings. Static translation scan: 283/283 eligible literal `Text()` labels across six scanned Compose UI source files have catalog entries.
- `PYTHONPATH=. python3 -m unittest discover -s tests -v` from `remote-runner/`: 12/12 passed. (Running the same discovery command from the project root fails because `server` is imported relative to `remote-runner/`; use the command above.)
- The actual `ActionApprovalLedger.kt` compiled with `kotlinc` in an isolated harness using minimal snapshot/evidence stubs. That harness passed proposal → approval → begin execution → evidence-backed completion, missing-evidence review, changed-payload invalidation, and audit-tamper detection. This validates the ledger logic's tested branches, not the Android/core integration or JSON codec.
- Added `ActionApprovalLedgerTest.kt` and schema v4 round-trip/tamper test. These tests have **not** been run through Gradle yet. Added `ActionApprovalInboxScreen.kt` under More with queue/review/history filters, exact-payload/audit inspection, rejection, guarded routing to a matching live browser approval, and explicit review for uncertain outcomes. The inbox cannot approve saved history.

### Blocking verification

- `./gradlew :core:test --offline --no-daemon` could not run because the Gradle wrapper attempted to download Gradle 8.14.6 and DNS resolution for `services.gradle.org` failed. Kotlin compilation of the full project, core JSON tests, Android unit tests, lint and APK build remain unverified.
- Browser WebView behavior and exact prompt approval must be tested on a real device after CI compilation passes. Global pause/stop and adoption of the shared ledger by native/guest/publication runtimes remain open product tasks. The new central inbox still needs Android compile and device acceptance.


## 2026-10-28 source increment

The action inbox and first top-level stop control are present in source. Static/source verification and guest runner tests are not substitutes for Kotlin/Compose compilation. Full Gradle testing remains blocked locally by the unavailable Gradle distribution download; no APK or device acceptance is claimed.


## 5 November 2026 — CI run #71 regression and correction prepared

- The single `incoming/memo.zip` contract now passed preflight; the duplicate archive from run #70 was no longer present.
- Run #71 passed safe ZIP validation/extraction, full source-tree synchronization, checksum refresh, `tools/verify_source.py`, and all 12 remote-runner tests.
- Kotlin compilation also passed. `:core:test` ran 52 tests and found one failure: `CapabilityCompilerTest.optionalRuntimeIsSelectedButNeverPretendedToBeEnabled`.
- Root cause: the generic `app` BUILD keyword was checked before the explicit `automate` AUTOMATION keyword. The request “Automate a sequence in my Android app” was therefore routed to BUILD.
- This snapshot corrects classifier precedence and strengthens the regression tests. An isolated Kotlin/JVM harness passed the explicit automation route, the optional-permission runtime contract, and a negative BUILD classification for a workflow-editor implementation request.
- Full Gradle tests, Android app unit tests, lint and APK assembly remain pending CI confirmation after this correction. Do not mark the fix integrated or build-green until a later Actions run passes.
- Detailed evidence: `docs/handoff/ci-history/run71-failure-2026-11-05/README.md` and the retained Gradle XML/HTML test reports.

## 5 November 2026 — CI run #72 ZIP metadata failure and repair

- Run #72 (`41965700176`) reached the new canonical `incoming/memo.zip` workflow and passed preflight.
- Safe extraction stopped before source integration with: `ZIP validation failed: symlink or special file is forbidden: 'Memo-Android/.github/workflows/build-apk.yml'`.
- The project files were ordinary files. The archive producer had written Unix permission bits (for example `0644`) without the required Unix file-type bits (`S_IFREG`). The ZIP validator correctly rejects ambiguous/special-file metadata, so it rejected those entries.
- Rebuilt the same source archive with Unix ZIP metadata explicitly set to regular files/directories. Source contents are unchanged by the metadata normalization; the archive SHA-256 changed.
- Revalidated CRCs and archive entry modes; no symlinks or special files remain. `tools/verify_source.py` passes and the remote-runner suite passes 12/12.
- The next CI run must still confirm ZIP validation, the full 52+ Kotlin core test suite, Android unit tests, lint and APK assembly. Do not mark CI green based on local archive checks alone.

## 5 November 2026 — CI run #73 Android Kotlin compile failure and repair prepared

- Run #73 (`41966362723`) passed single-ZIP preflight, ZIP validation, exact source synchronization, all 182 source checksums, `tools/verify_source.py`, the remote runner suite (12/12), and `:core:test`.
- Android compilation then failed in `:app:compileDebugKotlin` with eight diagnostics across four files: cross-module smart-cast of `transition.action`, a missing `heightIn` import, two references to `browser` declared later in the composable, a missing rounded `Edit` icon import, unsafe access to nullable `selectedConversation`, a missing rounded `Bookmark` icon import, and an unresolved `Divider` in `WorkspaceScreens.kt`.
- Corrected the underlying seven source defects: capture the action in a local immutable value; add the missing Compose layout/icon imports; move the stop helper below browser-session initialization; use a nullable-safe conversation lookup; and use Material 3 `HorizontalDivider`.
- The source verifier and runner suite are to be rerun on this repaired archive. This environment has no Gradle installation or Android SDK, so the corrected app has not been compiled here.
- Android Kotlin compilation, app unit tests, lint and APK assembly remain pending the next Actions run. No APK or green-build claim is made.
- Detailed diagnostics and edits: `docs/handoff/ci-history/run73-kotlin-compile-2026-11-05/README.md` and `Memo-Build-Validation.txt`.


## 5 November 2026 — CI run #74 lint diagnosis and repair prepared

- Run #74 (`41967437305`) passed ZIP preflight/validation, source synchronization and checksum verification, `tools/verify_source.py`, the remote runner suite, Kotlin compilation, all 53 core tests, and all 14 Android app unit tests.
- The build stopped at `:app:lintDebug`: one blocking `MissingPermission` error at `MemoNotificationCenter.kt:119`, plus 54 non-blocking warnings and 5 hints. APK assembly and signature verification were not reached.
- The cause was the notification call lacking an explicit Android 13+ `POST_NOTIFICATIONS` runtime-permission check. The manifest declaration and UI permission-request flow were already present.
- The current source snapshot now checks `ContextCompat.checkSelfPermission` before posting and catches `SecurityException` if permission is revoked between check and notify. No lint suppression or baseline was added.
- The warning backlog remains open and is not claimed as resolved. Full Gradle/lint/APK validation is pending the next Actions run; the local environment lacks the pinned Gradle distribution and Android SDK.
- Detailed evidence and repair notes: `docs/handoff/ci-history/run74-lint-notification-permission-2026-11-05/README.md`.

## Automated import — 2026-11-05 05:35:41 UTC

- Source upload commit: `27fa8c6c69ce760699c5bd4969e047921da91d4a`
- Archive SHA-256: `27ffccf2e162fb40ac002547bcc16cb3c568f9606ef72806a571a1dac0668d02`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/41974521703

## Automated import — 2026-11-05 13:14:24 UTC

- Source upload commit: `d173d5fa59c29fba66b6af95ad32360932698a14`
- Archive SHA-256: `de8084c2e7375eae9eb2b01ac0e12bb926ce3fc71678cbda6be9d2c47fc9ab48`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42033097252

## Automated import — 2026-11-05 13:34:04 UTC

- Source upload commit: `ddc1496d62c8105b513c710cb026a2fb8b38a2c4`
- Archive SHA-256: `25e45ddb043dbbdacf7e10e8a0463d5d4454331c43f7a440b7adec3c17524840`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42036053682

## Automated import — 2026-11-05 14:01:19 UTC

- Source upload commit: `9445f36c82121ced367549313c95d247296b5465`
- Archive SHA-256: `bb70a6874e16f5d0787c319d82fa28c7dfd8ea84087dd6771eaacf93bc69eae0`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42040141893

## Automated import — 2026-11-05 14:52:49 UTC

- Source upload commit: `382c65c1bf6dbc37d87fd766bf4796a2e76a400d`
- Archive SHA-256: `5d63a35d2ec33c52059870291badd4d6d4688f438f7fb6ae3acbd91dca857022`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: pinned persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42048273575

## Automated import — 2026-11-05 18:15:11 UTC

- Source upload commit: `d11b83ad13e731e851d6e169662d0a3c91ddb2a6`
- Archive SHA-256: `36d53269900d3b92a0eb8d77ad7a271be1f3d0bbe08666ae450b8019c7ff941a`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: pinned persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42081787582

## Automated import — 2026-11-05 18:24:30 UTC

- Source upload commit: `2ffb812c689bea3e5b91be803cce022aad47be29`
- Archive SHA-256: `aa74c88ba1563cb5bd176a07f5c93307c29260263d2104cdffe65f827976cbe1`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: pinned persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42083077434

## Automated import — 2026-11-08 22:38:19 UTC

- Source upload commit: `c4cfba812362fd5f69cc72026a935fe77a4d6dfc`
- Archive SHA-256: `71d0a807785119da052b5b7ac7647f8d3463b9bc764a2448022cbf801903bac7`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Signing mode: pinned persistent upload keystore
- Workflow run: https://github.com/ysfclc/memo/actions/runs/42530596381


## Verified source build — 2026-11-17 (run #802)

- Exact source commit: `090a2de99dacbc65cfcc0351be9333bbf00aa182` on `stabilization/usable-stage-2026-11-10`.
- GitHub Actions: [run #802](https://github.com/ysfclc/memo/actions/runs/44016406556) — **success**.
- Source verifier and localization coverage: PASS.
- Remote-runner tests: PASS.
- `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug` and Android-test APK assembly: PASS.
- APK package/version/identity checks: PASS.
- API 35 install-over-existing upgrade: PASS; app-private sentinel, both encrypted stores, legacy recovery and corruption tests passed.
- Isolated pinned signing job: PASS; persistent certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- Signed APK: package `app.memo.android.debug`, versionCode `802`, versionName `4.0.0-memo-debug`; artifact [Memo-Android-debug-APK #14298479094](https://github.com/ysfclc/memo/actions/runs/44016406556/artifacts/14298479094).
- APK SHA-256: `69345f8b680feb0b7cbb59d83161561693e3f1d3f079fe1d52c288c82b224db3`.
- This is automated/emulator/signing acceptance only. Physical-device and real provider-session acceptance remain pending.
