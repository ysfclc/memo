# CI run #74 — notification runtime permission lint failure

**Date:** 5 November 2026  
**Run:** [41967437305](https://github.com/ysfclc/memo/actions/runs/41967437305)  
**Upload commit:** `f8b5799d36fc58d423a994345d981f4362495fe2`  
**Uploaded archive SHA-256:** `7aceaa744be09ec74332b616bfae3488be0e10b5039d5ca06dadb62c4704f792`

## Verified run result

Run #74 passed the single-ZIP preflight, safe archive validation/extraction, exact source snapshot synchronization, checksum verification, `tools/verify_source.py`, and the remote-runner suite. The Gradle reports show **53/53 core tests** and **14/14 Android app unit tests** passing. Android Kotlin compilation also passed, so the fixes made for run #73 resolved those compiler errors.

The run stopped at `:app:lintDebug`. The lint report contains **1 error, 54 warnings and 5 hints**. The single blocking error was `MissingPermission` at `MemoNotificationCenter.kt:119`: `NotificationManagerCompat.notify()` was called without an explicit runtime check for `POST_NOTIFICATIONS` on Android 13+.

The permission was already declared in the manifest and the notification settings UI already requests it when the user enables reminders/workflow notifications or sends a test notification. However, `areNotificationsEnabled()` is an app-level notifications-enabled check; it is not a substitute for checking the runtime permission.

## Source repair in this handoff

Updated `app/src/main/java/app/memo/android/notifications/MemoNotificationCenter.kt`:

- On Android 13+ (`TIRAMISU`), explicitly calls `ContextCompat.checkSelfPermission(..., POST_NOTIFICATIONS)` and exits without posting when the permission is not granted.
- Keeps the existing app-level `areNotificationsEnabled()` guard.
- Catches a `SecurityException` around `notify()` to cover the narrow race where the user revokes permission after the explicit check.
- Leaves notification permission requests in the existing UI flow; no automatic or unsolicited permission prompt was added.

This is a targeted source fix. It does not suppress `MissingPermission`, weaken lint, or add a lint baseline.

## Remaining CI work

The 54 warnings and 5 hints were non-blocking in run #74 and are not silently treated as fixed. They include version/dependency freshness notices and source-level warnings; they should be reviewed as a separate cleanup task rather than addressed by untested bulk dependency upgrades.

Run #74 failed before APK assembly and APK signature verification. The next GitHub Actions run must confirm the explicit permission check clears lint, then pass app tests, lint, debug APK assembly, identity/signature verification, and artifact publication. No APK or green-build claim is made by this handoff.

## Local verification boundary

The source-level permission gate, manifest declaration, and existing UI permission-request flow were inspected together. The source verifier and remote-runner tests are rerun for this package. This environment does not have the pinned Gradle 8.14.6 distribution and Android SDK needed to run the full Android Gradle build locally; GitHub Actions remains the authoritative Kotlin/lint/APK gate.
