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

- `Memo-Android-debug-APK`: final APK signed with the pinned Memo certificate.
- `Memo-Android-Build-Reports`: test, lint, identity and build evidence.
- `Memo-Android-Integrated-Repository`: exact source snapshot and checksums.

Do not install `Memo-Android-ci-debug-APK`; it is an intermediate artifact signed with an ephemeral CI key and exists only as input to the isolated signing job.

## Update safety

An APK is an in-place update only when its application ID and signing certificate match the installed app and its version code is not lower. Never uninstall Memo or clear app storage to work around an install conflict. The stabilization build currently targets `app.memo.android.debug`; confirm that this matches the installed package before installing. Physical-device upgrade acceptance remains required even after emulator acceptance.
