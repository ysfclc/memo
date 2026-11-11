# Memo — Vercel Android build experiment

Date: 2026-11-11
Branch: `stabilization/usable-stage-2026-11-10`
Vercel project: `memo-android-build` (`prj_13avNJF6CIjRVdMgh1lsqVKZKfyD`)
Target: preview only

## Why this exists

The linked Vercel project was previously reporting READY while its build log showed only the default empty Vercel build (238 ms) and no build outputs. A READY web deployment was not evidence that an Android APK had been compiled.

## Build contract

- Install JDK 17 from Eclipse Temurin after resolving the official Adoptium package metadata; verify its published SHA-256 before extraction.
- Resolve Google's current Linux Android command-line tools package from the official SDK repository manifest and verify its published SHA-1/SHA-256.
- Install Android platform 35 and build-tools 35.0.0.
- Run the source verifier, remote-runner Python tests, core JVM tests, app JVM tests, Android lint, Android test APK assembly, and debug APK assembly.
- Publish the debug APK, build metadata, and SHA-256 checksums in the Vercel static output.
- If the project's configured `BLOB_READ_WRITE_TOKEN` is present, upload the APK to the configured public Vercel Blob store using a commit-addressed path. The token must never be printed or written to the output.
- The build version code uses the UTC epoch timestamp so it is greater than historical run-number-based debug builds.

## Signing and installation boundary

The Vercel project has a Blob token but no persistent Memo upload-keystore credentials. The experiment therefore uses the Android debug signing key generated in the ephemeral Vercel build environment. The APK package is `app.memo.android.debug`, but its certificate is not the pinned Memo upload certificate. It is suitable only for isolated testing; Android will reject it as an update over an installation signed with a different certificate. Do not uninstall the existing app merely to test this experiment.

A future upgrade-compatible Vercel build needs a deliberately provisioned, protected signing secret or a separate isolated signing stage that uses the existing pinned certificate. Do not copy signing material into Git, public Blob storage, or deployment artifacts.

## Acceptance

A Vercel deployment marked READY is insufficient. Accept only when build logs show the Gradle test/lint/assembly tasks completed, the APK exists, its SHA-256 is recorded, and the public Blob URL returns the exact APK. Device installation and runtime acceptance remain separate gates.
