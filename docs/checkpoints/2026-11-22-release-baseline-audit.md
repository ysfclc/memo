# Memo release baseline audit — 22 November 2026

## Purpose

Establish an evidence-backed baseline after repeated uncertainty about whether delivered APKs were stale, incomplete, or actually built from current source. This checkpoint records what was inspected; it does not claim physical-device acceptance or product completion.

## Repository identity

- Repository: `ysfclc/memo`
- Default branch at audit: `main`
- Latest main handoff commit inspected: `d80f19ae6ff956392cbe11c2b1aaead1758a7d19`
- Android source revision built by CI: `d189a732416c241d1e5d94d6a20d6a7e11345034`
- Latest source change in that revision: restore page-level scrolling in Talk and Work while preserving message-history scrolling.
- Audit branch: `audit/memo-release-baseline-2026-11-22`

## Confirmed CI evidence

- GitHub Actions run: https://github.com/ysfclc/memo/actions/runs/44927379252
- Run ID: `44927379252`
- Build job: `136916369795` — completed successfully.
- Isolated signing job: `136917570426` — completed successfully.
- Separate write-only source handoff job: `136917683817` — completed successfully.
- Source invariants: PASS.
- Remote runner suite: 12/12 PASS.
- `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`: PASS (the run's build job completed successfully).
- APK identity/version verification: PASS.
- Instrumented signed install-over-existing test: PASS; app-private sentinel and both encrypted Memo stores were preserved; legacy recovery/corruption checks passed.
- The install-over-existing test is CI/emulator evidence, not a physical-phone test.

## Exact APK artifact inspected

- Artifact name: `Memo-Android-debug-APK`
- Artifact ID: `14716387789`
- Artifact retention expiry: 2026-12-05 17:02 UTC.
- Artifact ZIP SHA-256: `5392cf97b8bc919b4afc91124014a6d7ac539c69f298c1269d99a4f190e096f3`
- APK filename: `Memo-Android-debug.apk`
- APK size: 114,776,153 bytes.
- APK SHA-256: `9e075f944de83615295445e01cfd7cfcbdf3fee2712326e9bf110a8e5f42b4af`
- Package/application ID: `app.memo.android.debug`
- Version name from Gradle configuration: `4.0.0-memo-debug`
- Signing: pinned persistent upload certificate, isolated signing job.
- Certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`
- Artifact ZIP was downloaded and inspected; it contains the APK, APK identity record, source fingerprint and checksum manifest. The APK SHA-256 matches the supplied checksum manifest.
- This is a signed debug/test package, not a production Play Store release.

## Product reality — do not confuse buildability with completion

The source and product documents explicitly leave substantial scope open:

1. Talk's current execution path is not yet the complete dynamic, model-directed, capability-dispatching outcome engine. Typed TaskSpec/compiler foundations exist, but the durable cross-runtime DAG executor and per-node journal are not complete.
2. Primary encrypted workspace storage and native workflow/run storage remain separate; safe unification/migration is still open.
3. Browser automation remains provider-specific, not a universal DOM/surface-schema engine.
4. Native app automation is deliberately bounded and requires Accessibility permission plus foreground/unlocked operation; real-device/OEM acceptance remains open.
5. Local model support is limited; device-wide discovery and arbitrary model formats such as GGUF/llama.cpp are not integrated.
6. The full social platform (connected accounts, feed, inbox, engagement, listening, analytics and growth) is specified but not implemented.
7. General artifact Studio, complete media ingestion, and several end-to-end job/content workflows remain partial or unimplemented.
8. No physical-device acceptance evidence was found for the current release candidate.

## Release interpretation

- The 21 November APK is not an obviously truncated or stale package: it is a real APK produced from the identified source revision, signed with the pinned certificate, and its artifact/checksums were independently inspected in this audit.
- CI proves compilation, named automated tests, lint, package identity, signature and an emulator/instrumented upgrade/data-preservation path.
- CI does not prove that every Memo feature works on the user's Redmi phone, that provider sessions work with the user's accounts, or that Memo is product-complete.
- Older APKs cannot all be retroactively classified from this one artifact. Each older delivery needs its own commit/build/artifact provenance before it can be called current or stale.
- Do not uninstall the user's existing Memo installation. Any real-phone test should be an in-place update, with a user-owned encrypted backup available first.
- Before installation, confirm the currently installed package ID and signing certificate are compatible; the new package ID is `app.memo.android.debug`.

## Next work package

**P0 — Physical-device release acceptance and failure capture.**

Use this exact APK as the release candidate. First preserve the user's data with Memo's encrypted backup. Then verify package/signature compatibility and install over the existing app (never uninstall as a workaround). Record app startup, unlock, Talk, Live, Work, Studio, local/remote model behavior, voice, storage, navigation, browser and native automation outcomes. Capture crashes/logcat and reproduce any reported issue against this exact APK hash. Do not add features or change architecture until the baseline is accepted or concrete defects are isolated.

After device acceptance, prioritize the durable shared Talk execution path and storage unification, preserving all existing requirements and regression coverage.

## Status

- Source/CI build baseline: verified on the exact source revision above.
- Downloaded APK artifact integrity and pinned signing identity: verified.
- Emulator/instrumented update/data preservation: verified by CI.
- Physical-device acceptance: not yet performed.
- Product completeness: not achieved.
