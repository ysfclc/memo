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
