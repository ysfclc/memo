# Memo Engineering Log

Append-only session and implementation record. Do not rewrite historical entries. Detailed requirements remain in `TASKS.md`; user intent remains in `docs/USER_INPUT_LEDGER.md`.

## 2026-11-19 — Responsible stabilization continuation

- **Base:** `stabilization/usable-stage-2026-11-10` at `2fde1f0793d969192f38fc09465b14cee5310676`.
- **User instruction:** “Ok. Build responsibly”.
- **Scope:** one narrow blocker only: missing Polish/Turkish catalog entries for five already-present Compose UI strings. No product expansion, redesign, storage migration, signing change or app identity change.
- **Observed failure:** GitHub Actions run #923 / `44489842476` fails in `tools/verify_source.py` before Gradle because five strings are missing from `MemoLocalization.kt`: `Back to workflow`, `Open Work`, `Open browser`, `Plan only · no action has been executed`, `Workflow browser`.
- **Evidence rule:** do not count #821 or another PR's passing run as validation of this head. Only a completed run for the exact resulting commit can establish CI status.
- **Safety:** preserve package ID, pinned signing identity, encrypted data formats and existing installation. No uninstall, clear-data or destructive operation.
- **Next:** add accurate PL/TR translations and a focused regression test; then observe exact-head CI through signing/artifact checks. If blocked, record the actual failing stage and stop rather than claiming a build.
- **Start status:** no source change made yet.


## 2026-11-19 — Exact-head build, upgrade acceptance and pinned APK signing passed

- **Verified commit:** `7ad0eb0426f953a8946741c59247b081a258f94a`; branch `stabilization/usable-stage-2026-11-10`.
- **Actions:** [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135), final conclusion `success`. Attempt 1 hit a transient corrupted Android system-image ZIP during SDK installation; rerunning the failed job succeeded.
- **Source repairs:** added PL/TR translations for `Back to workflow`, `Open Work`, `Open browser`, `Plan only · no action has been executed`, `Workflow browser`; added a PL/TR localization regression test. Added the registered read-only `browser.capture_evidence` capability to `ConversationWorkflowDraftFactory`'s allowlist, fixing the existing 188-test suite's draft-factory failure. Removed the premature composable-closing brace in `BrowserWorkflowScreen.kt`, keeping its approval dialog within the composable and fixing Kotlin top-level syntax errors.
- **Verification:** source verifier PASS (78 integration files; 434/434 static UI Text literals translated); remote-runner PASS; `:core:test` 188/188; `:app:testDebugUnitTest` 44/44; lint 0 errors, 42 warnings, 5 hints; `:app:assembleDebug` and `:app:assembleDebugAndroidTest` PASS; APK identity/signature PASS.
- **Upgrade safety:** API 35 emulator installed a lower-version baseline, seeded private and encrypted stores, then installed the newer signed APK in place. App-private data and both real encrypted Memo stores survived; legacy recovery and corruption checks passed. No uninstall or clear-data operation was used.
- **Signing:** isolated job signed and verified using pinned persistent certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`; no ephemeral key fallback.
- **Artifact:** [Memo-Android-debug-APK, ID 14565824727](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727); package `app.memo.android.debug`, versionCode `928`, versionName `4.0.0-memo-debug`; APK SHA-256 `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`; artifact ZIP SHA-256 `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`; source fingerprint `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- **Open boundary:** lint warnings remain; physical-device install/UX/provider-session acceptance remains pending. PR #8 remains draft/open, `main` untouched, and no release/merge is authorized.
