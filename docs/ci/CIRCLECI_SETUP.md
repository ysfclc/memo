# Memo CircleCI Android build

CircleCI is the primary external build runner for the private Memo repository. GitHub remains the source of truth; this pipeline builds the checked-out commit and does not import ZIPs, rewrite source files, or push commits back to the repository.

## Enable the project

1. In CircleCI, open the connected `ysfclc/memo` project.
2. Ensure the active configuration file is `.circleci/config.yml` and enable the project.
3. Trigger the `ux/experience-navigation-rebuild-2026-11-09` branch.

The job uses CircleCI's Android machine image with nested virtualization, because the final acceptance gate boots an Android 35 emulator. A Docker-only executor is not sufficient.

## Required signing environment variables

In CircleCI Project Settings → Environment Variables, add the same four values used by the existing GitHub Actions workflow:

- `MEMO_UPLOAD_KEYSTORE_BASE64`
- `MEMO_UPLOAD_STORE_PASSWORD`
- `MEMO_UPLOAD_KEY_ALIAS`
- `MEMO_UPLOAD_KEY_PASSWORD`

Use the exact existing Memo upload keystore and credentials. Do not generate a new key, paste secrets into chat, commit them, or use a temporary debug keystore. The build refuses to publish if the pinned certificate SHA-256 does not match the certificate already required by Memo.

## Gates

- Source invariant verifier
- Remote-runner Python unit tests
- `:core:test`
- `:app:testDebugUnitTest`
- `:app:lintDebug`
- `:app:assembleDebug` and `:app:assembleDebugAndroidTest`
- Package, version and pinned signing-certificate verification
- Android 35 emulator test: encrypted-store seed, install-over-existing update, encrypted-store verification and app-private-data preservation

Only the APK, identity receipt, checksum receipt and build diagnostics are retained as CircleCI artifacts. The large integrated repository ZIP is deliberately not duplicated.

## Resource and failure behavior

The job requests the Android `large` machine class so Gradle and the emulator can coexist. CircleCI Free includes monthly credits; machine-class usage consumes credits faster than small Docker jobs. Check the CircleCI usage page after the first run. If the free plan does not permit the selected machine class or the account's credits are exhausted, the job will queue/fail without creating an APK; do not downgrade by silently removing emulator or signing gates.
