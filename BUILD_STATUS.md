# Current PR candidate — 27 November 2026

## M3.A4-S10 — Work executes eligible local-model nodes (exact-head CI pending)

- Branch: `feature/work-local-model-step-execution-2026-11-27`, based on main `f628517699b51cac28c09c8fd0723a1334cf0f9f`.
- Dynamic TaskSpec nodes are mapped to the executable Work checkpoint type only when participant is `LOCAL_MODEL`, runtime is exactly `app.models`, and the only requested capability is `model.generate`. Remote model generation and approval-gated nodes remain review-only.
- Work exposes a direct action for the next eligible local-model checkpoint. The existing ordered checkpoint gate starts it first; the installed local model generates a draft artifact using the selected step and retrieved workspace context. The artifact is saved to Evidence and the checkpoint moves to `NEEDS_REVIEW`; it is not marked complete until the user reviews and records an outcome.
- The UI routes users to model setup when no compatible local model is installed. No browser, Android-app, remote-provider or external-write action is executed by this increment.
- Added policy tests for exact local-model classification and remote-model exclusion, plus source-verifier checks for the one-tap path and dependency order.
- **Local:** no Gradle/Android SDK or checked-out project tree is available here; no local compile or test is claimed.
- **CI:** exact-head Actions validation pending. No APK is claimed for this candidate.
- **Device:** physical-device acceptance remains open.

---

# Current PR candidate — 21 November 2026

## M3.A4-S7 — Shared capability postcondition verification (CI passed; device acceptance pending)

- PR #17: `https://github.com/ysfclc/memo/pull/17`; branch `agent/postcondition-verification-20261121`; baseline main `a6c893c7808c5f042f9fe3cb8254b5ac5b90a2c4`.
- The shared Talk engine requires explicit adapter postcondition verification before counting a capability as successful. Read-only verification failure is a failed observation and can be re-planned; local/external/destructive uncertainty stops for review. Writes require matching invocation/result/verifier evidence IDs.
- The six current workspace/browser adapters verify deterministic search output, encrypted note/record read-back, settled browser navigation and capture digests. Six engine regressions cover missing/mismatched postconditions, cancellation during write invocation/verification, and write adapter failure without re-planning. Source verifier guards the protocol
- Baseline source commit `adf58109af909358940e7361251669377e2a84d5` was validated by Actions run `44901392061`; observed jobs passed source/runner tests, Android unit tests, lint, debug/instrumentation assembly, APK identity/signature and install-over-existing preservation. This is baseline evidence only, not S7 validation.
- Exact-head CI run `44907881554` passed on `bfe40a182b1e283699a8c4c36bf9c612f83981c7`: source verifier, runner tests, core/app unit tests, lint, debug/instrumentation assembly, identity/signature and install-over-existing preservation. Write-interruption review hardening is included
- **Local:** no Gradle/Android SDK or checked-out project tree, so no local test/lint/build was run.
- **Release:** no canonical release APK produced. Available CI debug identity `app.memo.android.debug` is not an update for `app.memo.android`; physical-device acceptance remains open.

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

## Current source increment — typed TaskSpec DAG proposal boundary (CI verified; execution integration pending)

- Added the canonical typed TaskSpec contract, dynamic fan-out/join DAG compiler and exact-runtime capability assembly.
- Added a strict parser for untrusted model task proposals. Runtime/capability mismatches, malformed contracts, cycles, invalid budgets and unapproved external writes fail closed. Memo generates idempotency keys itself.
- Added core JVM regression tests and static source-verifier checks. Existing schema-v1 graph serialization is preserved.
- Exact-head integration CI run #1149 (`45002690812`) passed on `060853bd8e2ff07cb1445980430461e048067e7f`, including core/app tests, lint, APK assembly and emulator install-over-existing data preservation. This is not yet connected to Talk's model planner and does not execute tasks. Durable node state, runtime dispatch, policy integration, pause/resume and postcondition evidence remain future P2 work.

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

## Automated import — 2026-11-21 11:59:23 UTC

- Source revision: `adf58109af909358940e7361251669377e2a84d5`
- Archive SHA-256: `cf4058bd600219ee1e3c0b84430007a3256ea3cbef2ba6e4a8a2618912d0fded`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Build/test signature: ephemeral CI debug key; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/44901392061

## Automated import — 2026-11-21 17:01:35 UTC

- Source revision: `d189a732416c241d1e5d94d6a20d6a7e11345034`
- Archive SHA-256: `bb6e6447790aac0d4390a0cc36b4f7dfb770c823c0ad134064569ee2e6689d58`
- Required source, runner, JVM, lint and APK identity gates: PASS
- Build/test signature: ephemeral CI debug key; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/44927379252

## Product integration candidate — 21 November 2026

- Integration branch: `build/product-core-integration-2026-11-21`, based on current main `d80f19ae6ff956392cbe11c2b1aaead1758a7d19`.
- Integrated M3.A4-S7 shared capability postcondition verification from PR #17. Exact source head `bfe40a182b1e283699a8c4c36bf9c612f83981c7` passed Actions run `44907881554` (source verifier, remote runner, core/app unit tests, lint, debug/instrumentation APK assembly, package/signature and emulator install-over-existing data preservation).
- Preserved the current main Talk/Work scrolling repair and added the binding UI regression/device acceptance gate from PR #18.
- Exact-head integration CI run #1149 (`45002690812`) passed on source head `060853bd8e2ff07cb1445980430461e048067e7f`; documentation-updated head `a7eb5e5787b70f7517dbbe5acf490e71da52ac52` passed run #1150 (`45003595826`). No canonical release APK or physical-device acceptance is claimed.

## M3.B1-S1 — Talk-created private Markdown artifacts (integration branch)

- Added `workspace.save_artifact` as a registered local-write capability in the shared Talk dispatcher.
- Saves encrypted `STUDIO_ARTIFACT` records with source conversation/message provenance, source excerpt digest, content digest, version and DRAFT status. The shared engine requires exact read-back verification before reporting success.
- Studio lists Talk-created drafts. The existing Evidence dialog supports edit/revision history, copy and Markdown export; user edits refresh the content digest and increment the version.
- Added core policy tests and source-verifier/runtime-registration guards.
- Exact-head Android CI run #1149 (`45002690812`) passed on `060853bd8e2ff07cb1445980430461e048067e7f`; documentation-updated head also passed run #1150. No phone acceptance or release APK claimed. Supported output is Markdown text only; file/code, PDF/media and publishing remain open.

## M3.A4-S8 — Approved browser prompt submission (updated integration head)

- `browser.submit_prompt` is wired into Talk as an `EXTERNAL_WRITE` capability with exact destination/prompt approval, one-use action-ledger consumption, visible composer submission verification and encrypted evidence read-back.
- Action-ledger completion occurs only in the adapter postcondition after evidence verification. Changed page, hash mismatch, cancellation or uncertain outcome stops at `NEEDS_REVIEW` without replay.
- Exact-head integration CI run #1149 (`45002690812`) passed on `060853bd8e2ff07cb1445980430461e048067e7f`, including source verification, remote runner, Android unit tests, lint, APK assembly, identity/signature and emulator install-over-existing data preservation. Physical-device approval/rejection/stale-page/uncertain-send acceptance remains pending.
- Physical-device approval/rejection/stale-page/uncertain-send acceptance remains pending. No release APK is claimed.

## M3.A4-S9 — Consent-gated stable browser response capture (updated integration head)

- Added `browser.capture_response`, available only when browser-page sharing consent is enabled and a completed approved prompt submission is present.
- It verifies the original prompt baseline, waits for a new stable response, saves encrypted response evidence and returns the response as explicitly untrusted source material. It never resends a prompt; baseline loss/timeout becomes NEEDS_REVIEW.
- Studio exposes both submission and response records in a dedicated Browser research evidence section. Runtime policy and source-verifier regression guards added. Exact-head integration CI run #1149 passed on `060853bd8e2ff07cb1445980430461e048067e7f`; physical-device response/consent acceptance remains pending. No release APK is claimed.

## M3.B1-S1 artifact slice — exact-head CI PASS

- Run: `44953421742` — commit `ae6ef31976d7f35339715a16933ec73e21210af5`.
- Passed: source checksum/invariant verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, package/version identity, APK signature verification, and emulator install-over-existing with app-private data preservation.
- Release-signing job was skipped; this was not a canonical release APK. Physical-device acceptance remains pending.
- This run predates the subsequent approved browser prompt/response additions. Those changes require the newer exact-head CI run.

## M3.A4-S8 — approved browser prompt submission CI PASS

- Run: 44954196829 — commit 23073011b9c411945c12531ce8483ac0a6d2b4b5.
- Passed: source checksum/invariant verification, remote runner, core/app unit tests, lint, debug APK assembly, package/version identity, signature verification and emulator install-over-existing data preservation.
- Release-signing job was skipped. This run predates M3.A4-S9 response capture and the latest Studio browser-evidence visibility changes. The newer response-capable head needs its own exact-head CI. Physical-device external-action acceptance remains pending.

## M3.A4-S9 — integrated browser prompt/response CI PASS

- Run: 44954878877 — commit bac787645ee85958132d56e8524c53325b536c06.
- Passed: source verification, remote runner, Android unit tests, lint, debug APK assembly, package/version identity, APK signature verification and emulator install-over-existing app-private data preservation.
- Release artifact preparation and isolated signing were skipped. Physical-device browser consent, provider DOM, approve/reject and uncertain-send acceptance remain pending.


## M3.B1-S2 — Verified user-directed Studio Markdown export

- Added a core export policy for bounded UTF-8 text payloads, safe filenames, supported text MIME types and SHA-256 digests.
- Studio export writes only to the user-selected Android document URI, then reopens that same URI and verifies the exact bytes before reporting success.
- A write error, unreadable destination, truncated file or digest mismatch is reported as unverified. No arbitrary filesystem access, new plaintext storage, PDF/binary generation or background export is claimed.
- Core tests cover filename sanitization, MIME/size limits, UTF-8, exact digest read-back and failure cases.
- Exact-head GitHub Actions run #1149 (`45002690812`) passed on source commit `060853bd8e2ff07cb1445980430461e048067e7f`; the documentation-updated PR head `a7eb5e5787b70f7517dbbe5acf490e71da52ac52` also passed run #1150 (`45003595826`). Both passed source invariants, remote-runner tests, Android unit tests, lint, debug APK assembly, APK identity/signature checks and emulator install-over-existing data preservation. Isolated final signing and release artifact publication were skipped. Physical-device export acceptance remains pending; no production-signed APK is claimed.

## Automated import — 2026-11-23 22:56:29 UTC

- Source revision: `aebc7d9a1e1631bba7b4985e5853d2d603efe2cd`
- Archive SHA-256: `c5693f9d601f5498cede032432c107884bc263e74cbf233f9d3cdca4d4b10732`
- Required source, runner, JVM, debug/release lint, release instrumentation and APK identity gates: PASS
- Release package: ; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/45283218487

## Automated import — 2026-11-25 09:23:11 UTC

- Source revision: `804c6e3da5abff8081816509e8f67cbef45efde3`
- Archive SHA-256: `ba632879e884c460ea16308a1972a3f58ac73eecf007eed1bb2dc34c0dbebbd3`
- Required source, runner, JVM, debug/release lint, release instrumentation and APK identity gates: PASS
- Release package: ; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/45583930081

# Build and verification status — 26 November 2026

## WF-13 — APK version monotonicity (exact-head CI passed; device acceptance pending)

- New branch: `fix/apk-version-monotonicity-mainline-2026-11-26`, based directly on current main.
- Gradle now derives the default versionCode as `floor(sourceCommitUnixSeconds / 4)`. It rejects missing Git history, malformed timestamps, a scaled value not strictly greater than the parent's, and values above 2,100,000,000. Four-second source timestamp resolution leaves a multi-century version-code range rather than consuming the limit in roughly a decade. The explicit `MEMO_VERSION_CODE` override is retained for controlled CI baseline construction and is range-validated.
- GitHub Actions reads and validates the same two-commit timestamp sequence and scaled values before comparing the built APK identity; its install-over-existing test uses the same scaled source value. CircleCI validates the same scaled parent/current ordering before setting the explicit Gradle value.
- `tools/verify_source.py` now guards against runner-specific counters and requires the shared source-derived policy in all three build paths.
- No application ID, versionName, signing key/certificate, encryption key alias, storage schema, backup format, or application data behavior was changed.
- This is a mainline transplant of the useful PR #9 versioning change, not a merge of its diverged branch. Its unrelated Talk source-verifier change was excluded.
- **CI iterations:** run #1270 caught Groovy's `/` operator producing a decimal; no APK was produced. Replaced it with `Math.floorDiv` so Gradle emits an integer versionCode. Run #1272 passed on the preceding code head `fff5b1b954090d5a17a30956083e02e22d2fbecb`; exact latest PR head validation is recorded below.
- **Exact-head CI evidence:** Actions run #1273 (`45841609952`) passed on exact PR head `b20677e4e3e3353ea53f97a3443bd7814b953f0d`: source verification, remote-runner tests, core/app unit tests, debug/release lint, debug/release APK assembly, APK identity/version/signature inspection, and release-package emulator install-over-existing. Both encrypted-store instrumentation phases reported `OK (1 test)`; both encrypted stores and the private-data sentinel survived the in-place update.
- **Version evidence:** CI merge-ref versionCode `448926027`; previously verified mainline release versionCode `1190`; package remains `app.memo.android`. The scaled timestamp formula is `floor(sourceCommitUnixSeconds / 4)`, with current scaled code strictly greater than the parent's and bounded by `2,100,000,000`.
- **Artifact boundary:** PR validation uploaded only `Memo-Android-Build-Reports` diagnostics. It skipped persistent signing and final APK publication; no installable release APK is claimed.
- **Still outstanding for the overall APK gate:** physical-device update using the pinned certificate, dependency vulnerability assessment, and coverage comparison. No uninstall/reinstall workaround is permitted.
- **Release blocker remains:** the user-installed package and certificate must be confirmed before installing any candidate. The existing known release artifact is package `app.memo.android`, version code `1190`, pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Four-second bucket edge case:** the documentation-only branch head `fd7fb3e771d4294a7a539ccacdde4b6782ba7627` was committed at `15:48:31Z`, only three seconds after its parent (`15:48:28Z`); both direct-branch timestamps floor to versionCode `448927027`. A direct build of that branch head must fail closed rather than reuse a versionCode. GitHub pull-request validation checks out the synthetic PR merge ref, so its timestamp/parent pair is distinct; do not infer direct-branch behavior from the PR run. Mainline push and CircleCI behavior must still be verified on the actual merge commit.


## Automated import — 2026-11-26 16:22:58 UTC

- Source revision: `cce4cfd8c5f951697408004ab105a56cfd2c60ad`
- Archive SHA-256: `e643afa5684f71d29e9d7fb40ac0157d0bad1f374ca4ba137ffe950105c0354c`
- Required source, runner, JVM, debug/release lint, release instrumentation and APK identity gates: PASS
- Release package: ; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/45857259677


# Product build status — 27 November 2026

## Talk → Work dynamic plan creation (source implementation; exact-head CI pending)

- Branch: `feature/talk-dynamic-work-plans-2026-11-27`, based on main `00f32176e2e996a4b53715190270e76438716008`.
- All new home intents now enter Talk's canonical execution path; the keyword-based deterministic template shortcut was removed.
- Added the registered `workspace.create_work_plan` capability to Talk. The model proposes a typed dependency graph; `WorkflowTaskProposalParser` validates exact runtime/capability ownership, graph structure, budgets and approval gates.
- A validated graph becomes an encrypted Work draft with ordered pending checkpoints, contracts and exact Talk source-message provenance. The adapter verifies the saved graph and DRAFT state by reading it back from the workspace.
- Added pure-core policy tests and source-verifier checks. No schema, encryption, backup, package, signing or external-action behavior changed.
- **Important boundary:** this creates a real, editable plan only. It does not execute graph nodes. The existing iterative Talk engine still executes only its registered callable adapters; full durable DAG execution remains future product work.
- Exact-head GitHub Actions validation: pending. No local Gradle/Android build, release APK or physical-device acceptance is claimed.
- Checkpoint: `docs/checkpoints/2026-11-27-talk-dynamic-work-plans.md`.

## Automated import — 2026-11-27 04:45:16 UTC

- Source revision: `f1f865ac1bcea73f68eab89c28fea77544f88561`
- Archive SHA-256: `92d3f73c60e6372a2fc060ba733d58072b5f95eb7ea809671b78b3a0f316a1f3`
- Required source, runner, JVM, debug/release lint, release instrumentation and APK identity gates: PASS
- Release package: ; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/45941121247

## Automated import — 2026-11-27 15:21:39 UTC

- Source revision: `8aa7e54dd02e91831ce9650bd3fe62c9355479b8`
- Archive SHA-256: `bb7aed178d399e4444f1aad046b3f0ac3de5ee3a2231bdacee91c792d2051708`
- Required source, runner, JVM, debug/release lint, release instrumentation and APK identity gates: PASS
- Release package: ; final pinned signing is isolated.
- Workflow run: https://github.com/ysfclc/memo/actions/runs/46023201077
