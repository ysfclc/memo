# Memo session checkpoint — CI credential isolation

**Date:** 2026-11-09  
**Branch:** `security/isolate-ci-signing-2026-11-09`  
**Base:** `ysfclc-patch-1`  
**Checkpoint commit:** `16d979d3f920a4e879bce1496233d90ffed81914`

## User outcome

Prevent imported source, Gradle scripts, tests or emulator execution from reading the persistent Memo APK signing key or using a repository-write token.

## Changes on this branch

- `.github/workflows/build-apk.yml`
  - Workflow default permission reduced to `contents: read`.
  - Preflight uses read-only access and does not execute project code.
  - Build/test job has read-only permission, no signing secrets and no persisted checkout credential.
  - Build/emulator tests use the ephemeral runner debug key.
  - Persistent APK signing moved to a separate artifact-only job with no repository checkout and no write permission; secrets are scoped to the signing step.
  - Repository handoff moved to a separate write-only job with no signing secrets and no project-code execution.
  - Artifact extraction checks path traversal, absolute/Windows paths, symlinks/special files, CRC and size limits; `.github/` and `incoming/` are preserved.
  - Pull requests run build/test only; signing and repository writes are skipped.
- `docs/security/CI_CREDENTIAL_BOUNDARIES.md` — threat model, job boundaries and acceptance gates.
- `TASKS.md` — P0 status and remaining acceptance work.

## Verification performed

- Re-fetched the modified workflow and checked its job boundaries.
- Confirmed workflow default is read-only; only the commit job requests `contents: write`.
- Confirmed build/preflight do not reference signing secrets.
- Confirmed build checkout does not persist credentials.
- Confirmed signing job does not check out or execute repository source.
- Confirmed commit job has no signing secrets and contains no Gradle invocation.
- Confirmed pull-request conditions skip signing and repository writes.

## Not yet verified

- GitHub Actions YAML/workflow validation and PR build.
- Actual isolated signing run with the configured private key.
- Final signed APK install-over-existing.
- Device-level behavior on a physical phone.

No signing key was read, rotated, copied or exposed during this change. No workflow was manually dispatched and no production deployment was performed.

## Next action

Open a PR against `ysfclc-patch-1`, inspect the PR build, fix any workflow/test failures, then verify a normal signed run before treating the P0 as closed.
