# Memo ZIP import workflow — Patch1

## What belongs in the repository

- One persistent workflow: `.github/workflows/build-apk.yml`.
- One source input at a time: `incoming/memo.zip`.
- One target branch: `ysfclc-patch-1`.

The full project source and its handoff material live inside `memo.zip`. Do not upload a second archive or keep a root-level baseline ZIP. The workflow YAML is also embedded inside the source ZIP for backup, but the importer deliberately preserves the already-installed live `.github/` folder so a source upload cannot overwrite its own import workflow.

## One-time setup

1. On branch `ysfclc-patch-1`, replace the complete contents of `.github/workflows/build-apk.yml` with the standalone YAML delivered beside this ZIP.
2. Ensure `incoming/.gitkeep` exists so the upload folder is retained even when empty.
3. Commit the workflow and `.gitkeep` to `ysfclc-patch-1`. The workflow is branch-restricted and will not run on `main`.
4. In repository Settings → Actions → General, allow GitHub Actions to have read/write repository contents permission. The workflow needs `contents: write` only to commit validated source and import notes.
5. Optional but recommended for APK update continuity: configure all four repository Actions secrets: `MEMO_UPLOAD_KEYSTORE_BASE64`, `MEMO_UPLOAD_STORE_PASSWORD`, `MEMO_UPLOAD_KEY_ALIAS`, `MEMO_UPLOAD_KEY_PASSWORD`. Do not put a keystore or passwords in the ZIP. When all four secrets exist, the app's debug APK is signed with that stable key. With none configured, the build uses the runner's debug keystore and a reinstall can be necessary if its certificate changes.

## Normal upload — repeat this for every source version

1. Create a ZIP of the complete Memo project working tree. The Gradle project root can be at the ZIP root or inside a folder. Keep `settings.gradle[.kts]`, `gradlew`, root `build.gradle[.kts]`, `app/build.gradle[.kts]`, `app/src/main/AndroidManifest.xml`, `gradle/wrapper/gradle-wrapper.properties`, `TASKS.md`, `tools/verify_source.py` and `SOURCE_SHA256SUMS.txt` together.
2. Exclude `.git`, build caches, `local.properties`, `.env`, keystores, private keys, signing property files, credentials and machine-specific files. The archive itself is committed as an immutable input revision before the workflow reads it.
3. On `ysfclc-patch-1`, upload/replace exactly `incoming/memo.zip` and commit the change.
4. GitHub Actions verifies the ZIP paths/CRC/size, detects exactly one Android Gradle project root, validates the source checksum manifest, and extracts it. It then mirrors the project as a full snapshot: stale files absent from the ZIP are deleted. `.git/`, the live `.github/` workflow, `incoming/`, and the import-history `notes/` are preserved.
5. The workflow regenerates `SOURCE_SHA256SUMS.txt`, runs source invariants, the remote-runner test suite, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, then verifies the APK package/version/signature.
6. On success, it updates `BUILD_STATUS.md`, writes `notes/imports/<upload-commit-sha>.md`, appends `notes/README.md`, commits the new source snapshot and uploads the APK/repository/report artifacts. On failure, the source integration is not committed; the report artifact retains logs and build reports.

## What gets updated by each successful upload

- Project files: exact mirror of the incoming ZIP (no stale source overlay).
- `SOURCE_SHA256SUMS.txt`: regenerated from the integrated source tree.
- `BUILD_STATUS.md`: appended with the upload commit, archive hash, gate results and run URL.
- `notes/imports/<upload-commit>.md`: immutable per-upload record.
- `notes/README.md`: import index.
- Actions artifacts: debug APK, integrated repository ZIP, checksum receipt and build reports.

A cleanup-only deletion that leaves `incoming/` without any ZIP is treated as a no-op. Keep only `incoming/memo.zip` when making an upload. The workflow is idempotent for the same upload commit and will not create duplicate import records.

## Security and behavior

- Only branch `ysfclc-patch-1` is accepted.
- ZIP traversal, absolute paths, symlinks/special files, duplicate paths, encrypted entries, oversized payloads and obvious secret material are rejected.
- The source archive is never trusted to replace the live workflow or Git metadata.
- A build that fails does not commit extracted project source.
- A successful CI APK build is not proof of installation or device acceptance. Use the device acceptance checklist after downloading the APK.
