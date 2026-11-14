# Memo — APK freshness, signing identity and data-loss audit

**Date:** 13 November 2026  
**Repository:** `ysfclc/memo` (Memo 1)  
**Inspected branch:** `stabilization/usable-stage-2026-11-10`  
**Inspected source head:** `d6826ea6e30dd0eea23de04f0e3e0e9718e85742`  
**Tracking PR:** #8 (draft, unmerged)  
**Status:** source/configuration audit complete; exact device incident and current-head runtime remain unverified.

## Executive finding

The report that the last APK looked unchanged is consistent with the delivery evidence: the latest stabilization source head has not produced a successful build. GitHub Actions runs #629 and preceding runs failed in the preflight job before any runner step executed; build, test, instrumentation and signing jobs were skipped. The existing Vercel Android build project also reported the free daily deployment quota (HTTP 402, `api-deployments-free-per-day`), so it did not produce a deployment for the current candidate.

The latest confirmed signed APK artifact retrievable during this audit is from 8 November 2026, workflow run `42530596381`:
- Artifact: `Memo-Android-debug-APK`, 114,074,234 bytes.
- APK: `Memo-Android-debug.apk`.
- Package: `app.memo.android.debug`.
- Version code: `223`.
- Version name: `4.0.0-memo-debug`.
- Signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- APK SHA-256: `670acd9192f4b02e7aea1c23e456036b2ad3a98f5d48d57ef7c895a72e8d22b2`.
- Source fingerprint: `71d0a807785119da052b5b7ac7647f8d3463b9bc764a2448022cbf801903bac7`.

This is an older artifact, not proof of the 13 November source. It must not be labelled fresh.

## 1. There are multiple Android package identities

The inspected source has three distinct package outcomes:

| Build path | Application ID | Signing behavior | Meaning |
|---|---|---|---|
| GitHub Actions debug build | `app.memo.android.debug` | Intermediate CI debug key, then isolated re-signing with pinned Memo certificate | Intended signed CI install/test artifact |
| Vercel preview script | `app.memo.android.preview` | Uses pinned certificate only when all four credentials are configured; otherwise continues with ephemeral debug key | Side-by-side preview, explicitly not update-compatible |
| Gradle release variant | `app.memo.android` | Falls back to Gradle debug key when persistent signing credentials are absent | Must never be treated as a compatible production/update APK |

The GitHub workflow's final isolated signing job is fail-closed: it requires all four secrets, verifies package identity, and compares the certificate to the pinned SHA-256. The intermediate `Memo-Android-debug-ci-signed.apk` is not the final artifact.

The Vercel script is not fail-closed. It warns that an ephemeral key is not update-compatible, but still builds and publishes the preview APK if signing secrets are absent. It checks the pinned certificate only when all four credentials exist. This contradicts the stabilization rule that an ephemeral key must never be offered as an upgrade candidate. The preview package suffix does protect the existing `app.memo.android` or `.debug` package from a direct Android package replacement, but it is still unsafe to present as an update. If the user uninstalls the old app to replace it, Android removes the old app's private data and Keystore aliases.

**Required correction after build execution is restored:** Vercel must fail before publishing unless the pinned Memo signing identity is configured and verified, or it must be clearly restricted to a disposable side-by-side test install with a persistent preview key and a prominent non-upgrade label. No APK should be published with a silent/implicit debug-key fallback.

## 2. Why uninstalling or changing package identity destroys data

The encrypted workspace and native workflow stores are both inside app-private `filesDir` and encrypted with Android Keystore keys:
- `memo-workspace-vault.bin`, alias `synthos_fieldwork_vault_v1`.
- `memo-native-workflows.bin`, alias `synthos.runtime.store.v1`.
- Legacy shared migration source: `synthos-vault.bin`.

An in-place Android update preserves app-private files and Keystore entries only when the package identity and signing lineage are compatible and the install is performed as an update (for example, `adb install -r`). Uninstalling the old package or clearing its data removes the private files and normally its app-scoped Keystore keys. A new APK cannot decrypt old ciphertext if the old Keystore key has been removed.

The manifest sets `android:allowBackup="false"` and `android:fullBackupContent="false"`. This is not the direct cause of data loss during a normal in-place update, but it means Android backup cannot be relied on as a recovery path after uninstall/reset.

The existing `EncryptedStoreUpgradeTest` is a useful isolated test: it seeds both stores, performs a version-increasing install-over update, checks both stores and legacy migrations, and tests corruption/AtomicFile recovery. However, no successful exact-head emulator execution or physical-device upgrade is evidenced.

**Incident boundary:** the exact APK installed on the user's phone and whether the old app was uninstalled, cleared, or replaced under another package ID have not been captured. Therefore the source audit identifies concrete unsafe delivery paths but does not claim a proven single cause for the reported device data loss.

## 3. Why the last APK was not fresh

- Latest inspected stabilization head: `d6826ea6e30dd0eea23de04f0e3e0e9718e85742`.
- Latest GitHub Actions run observed: #629, run ID `43368087849`, failed in preflight; the runner was not assigned and downstream jobs were skipped.
- The previous series of runs failed at the same pre-runner boundary. No Gradle, unit, lint, instrumentation, APK assembly or signing result exists for those commits.
- Vercel's project reported HTTP 402 because its daily free deployment quota had been exhausted. There is no successful current-head deployment to use as build evidence.
- The last retrievable signed artifact is the 8 November artifact listed above.

A changed source file, a deployment error page, a previously successful APK, or a build workflow definition is not a fresh APK.

## 4. Existing product work remains active

Finding another bug does not defer, remove, downgrade, or close previously reported work. A stabilization freeze means sequencing and preventing unsafe releases; it does not mean the user's backlog has been abandoned.

The current stabilization work WF-01 through WF-12 remains active. In particular:
- WF-01 manual answer capture;
- WF-02 capture retry without resend;
- WF-03 separate skip/stop-step/cancel-run recovery;
- WF-04 truthful completion only after durable response persistence;
- WF-05 real-device reproduction;
- WF-06 editable prompt checkpoint and approval invalidation;
- WF-07 browser/task context continuity;
- WF-08 run history separate from templates;
- WF-09 truthful local/API/browser Council runtime labels;
- WF-10 editable result workspace preserving raw evidence;
- WF-11 regression suite;
- WF-12 device and EN/PL/TR acceptance.

The wider TASKS.md milestones and product gaps also remain in the active backlog. No item may be marked deferred or complete merely because a new defect is discovered. Each task's implementation status and verification status must remain distinct.

## 5. Ordered next work

### P0 — stop data loss and establish a trustworthy APK
1. Do not uninstall Memo, clear app data, or install a differently signed preview over the current installation.
2. Identify the exact APK currently on the device (application ID, version code/name, signer SHA-256). Preserve any remaining installation and data.
3. Restore one build executor. Diagnose the GitHub runner/preflight failure without repeatedly triggering identical failed runs; wait for or use an already-authorized executor. Do not spend money or create another service/account without approval.
4. Correct the Vercel signing fallback so it cannot publish an ephemeral-key APK as an update.
5. Build one exact candidate SHA and retain full logs, test reports, source SHA, package ID, version, APK SHA-256 and signer SHA-256.
6. Verify the signer equals the pinned certificate and the package ID equals the existing installed package before any install attempt.
7. Run the API 35 encrypted-store instrumentation test with a version-increasing install-over update.
8. Only then perform a carefully controlled physical-device install-over test. No uninstall or clear-data workaround is permitted.

### P1 — resume existing bug fixes without dropping the backlog
1. Once a build executor is available, run source verifier, remote-runner tests, core/app unit tests, lint, APK assembly and instrumentation on one exact commit.
2. Continue WF-04 response-persistence correctness; the current progress ledger identifies that the step can be marked COMPLETED before all transcript/evidence/entity/audit writes are confirmed.
3. Verify WF-01 through WF-03 on-device; source-only checks do not close them.
4. Continue WF-05 through WF-12 in priority order, keeping every prior task visible and open until its own acceptance evidence exists.
5. Update TASKS.md, BUILD_STATUS.md, DEVICE_ACCEPTANCE_CHECKLIST.md and the handoff ledger with exact evidence.

## 6. Release rejection rules

Reject any candidate when:
- source SHA is not the requested/current branch head;
- package ID differs from the installed package for an upgrade;
- signer SHA-256 is missing or differs from the pinned certificate;
- version code does not increase;
- the APK has no exact source/build provenance and checksum;
- tests were assembled but instrumentation was not executed;
- encrypted stores do not survive install-over;
- a user is told to uninstall or clear data to work around an install conflict.

**Current decision:** no fresh installable upgrade APK is verified. Keep PR #8 draft/unmerged. Preserve the existing installation and data. The next engineering action is to restore the build executor and make the Vercel signing path fail closed, then execute the data-preservation gates before any further delivery.


## 14 November 2026 — Vercel signing fallback closed in source

The stabilization branch now changes the Vercel APK script so all four protected Memo signing values are mandatory before toolchain downloads or Gradle execution. It validates the configured keystore, unconditionally checks the output certificate against the pinned SHA-256, removes the ephemeral debug-key publication path, and deletes the temporary keystore on exit. `tools/verify_source.py` guards these requirements.

This intentionally means the existing Vercel project (previously audited as lacking the four signing secrets) cannot produce an APK until those secrets are configured. That is safer than publishing a misleading, non-update-compatible preview artifact. The preview application ID remains `app.memo.android.preview`; it is not a replacement for an installed Memo package.

**Status:** source patch committed; exact-head Actions, Gradle, signature, emulator and physical-device verification are pending. The incident is not closed and no APK is being distributed.


## 14 November 2026 — GitHub source build and emulator evidence

Run #649 validated exact source commit `e7bc8b8f4452ac5b34fe5cb3a040678e7e8cd8c2` on the stabilization branch. Source verification, remote-runner tests, 144 core tests, 37 app tests, lint, debug APK assembly and package/version identity checks passed. API 35 instrumentation passed both seed and verify phases; installing the newer APK over the baseline preserved both encrypted Memo stores and passed legacy recovery/corruption checks.

The run was a pull-request validation, so the isolated pinned-signing job was correctly skipped. Its debug APK is an intermediate and must not be installed. A manual source-build dispatch option is now available in the GitHub workflow, restricted to the stabilization branch, and runs the exact branch source through the isolated signing job. The final pinned-signed artifact and physical-device update remain pending. The incident is not closed.
