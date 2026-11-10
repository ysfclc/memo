# Memo CI credential boundaries

**Status:** Implemented in the security-isolation branch; not accepted until the pull-request build and a normal signing run pass.

## Finding

The previous `.github/workflows/build-apk.yml` executed the imported Android project, Gradle build scripts, tests and emulator operations in the same job that held the persistent APK keystore/passwords and a repository-write-capable checkout token. A compromised build script or dependency could therefore read signing material or use the GitHub credential. This was a credible exposure path; the audit found no evidence that either credential had actually been stolen.

## Required job boundaries

1. **Preflight — read only.** Validates the triggering branch and import archive. It does not execute project code. The checkout credential is read-only.
2. **Build and test — read only, no signing secrets.** Runs source checks, Gradle, unit/instrumentation tests and emulator acceptance using the runner's temporary debug key. Checkout credentials are not persisted. It uploads the CI-debug-signed test APK and integrated source snapshot as artifacts.
3. **Sign — read only, signing secrets scoped to one step.** Receives only the APK artifact. It does not check out the repository, invoke Gradle, run project scripts or receive repository-write permission. The isolated step aligns and re-signs the APK, then verifies package identity, alignment and the pinned certificate fingerprint before publishing the final APK artifact.
4. **Commit — repository write, no signing secrets.** Receives the integrated source artifact, validates archive paths/CRC/size/symlink constraints, preserves `.git/`, `.github/` and `incoming/`, rejects secret-bearing paths, checks that the target branch has not advanced, and commits without executing repository code.

The workflow-wide permission is read-only. Only the final commit job receives `contents: write`. Pull-request validation runs the build/test job only; it does not sign with production credentials or write to the repository.

## Acceptance gates

- [ ] Pull-request workflow parses and the build/test job passes on the exact PR merge commit.
- [ ] A normal run proves the isolated sign job can re-sign the CI APK and verify the pinned certificate fingerprint.
- [ ] The final APK installs over the currently installed Memo package without an uninstall or data reset.
- [ ] The write job commits the source snapshot while executing no repository-controlled code.
- [ ] Job permissions, artifact contents and logs are reviewed; no keystore/password appears outside the isolated signing step.
- [ ] Physical-device install-over-existing and app-private-data preservation are recorded separately from emulator evidence.

## Operational notes

- The CI debug signature is temporary and is used only for build/emulator tests. The downloadable `Memo-Android-debug-APK` is produced by the isolated signing job with the existing pinned upload certificate.
- The build artifact is untrusted input to the sign/commit jobs. The sign job treats it only as an APK; the commit job treats its repository ZIP only as data and validates it before extraction.
- The commit job excludes `.github/` from artifact restoration so a build script cannot indirectly rewrite the privileged workflow.
- Do not rotate the upload key solely because the exposure path existed. Rotate it if logs, repository history or other evidence indicates actual disclosure; keep the existing certificate to preserve upgrade compatibility.
- This change does not authorize production deployment or a Play Store release.
