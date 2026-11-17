# Build Memo directly from GitHub

This workflow builds the exact committed Android source. It does not import or extract the historical `incoming/memo.zip`.

## Signed stabilization build

1. Open the [Memo Android build workflow](https://github.com/ysfclc/memo/actions/workflows/build-apk.yml).
2. Select **Run workflow**.
3. Select branch `stabilization/usable-stage-2026-11-10`.
4. Enable **Build this branch's committed source directly (no ZIP)**.
5. Run the workflow and wait for all gates.

The manual source-build option is deliberately restricted to the stabilization branch. Pull-request validation never receives signing secrets and never publishes a signed APK.

## Gates

The workflow runs source invariants, remote-runner tests, core/app unit tests, lint, APK assembly, APK identity checks, and API 35 encrypted-store install-over-existing instrumentation. Only after the build succeeds does a separate artifact-only job use the pinned Memo signing key and verify the certificate fingerprint. The signing job does not check out or execute repository source.

## Artifacts

- `Memo-Android-debug-APK`: **the only installable APK artifact**. Final APK signed with the pinned Memo certificate; this is the one to download.
- `Memo-Android-Build-Reports`: test, lint, identity and build evidence.
- `Memo-Android-Integrated-Repository`: exact source snapshot and checksums.

An internal artifact named `INTERNAL-DO-NOT-INSTALL-Memo-Android-ci-signing-input` is retained for one day solely as the isolated signing job's input. It is signed with an ephemeral CI key. Never download or install it. If that internal artifact is ever exposed to end users by mistake, Android can reject it as a signature conflict.

## Update safety

An APK is an in-place update only when its application ID and signing certificate match the installed app and its version code is not lower. Never uninstall Memo or clear app storage to work around an install conflict. The stabilization build targets `app.memo.android.debug`, keeps the pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`, and increases `versionCode` from the GitHub run number. The Vercel preview deliberately uses a different package, `app.memo.android.preview`; it is not an update for the debug installation. Physical-device upgrade acceptance remains required even after emulator acceptance.
