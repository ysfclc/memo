

## 19 November 2026 — Fresh Start: responsible stabilization continuation

- **Repository / branch:** `ysfclc/memo` / `stabilization/usable-stage-2026-11-10`.
- **Inspected base:** `2fde1f0793d969192f38fc09465b14cee5310676` (exact branch head before this session's changes).
- **Authority read:** `AGENTS.md`, `docs/USER_INPUT_LEDGER.md`, `DEVELOPMENT_GOVERNANCE.md`, `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/DECISION_LOG.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `docs/STATE.md`, `TASKS.md`, `docs/PRODUCT_INTEGRITY_AUDIT_2026-11-19.md`, `docs/handoff/MEMO-FRESH-START-MASTER-LEDGER.md`, and the relevant CI workflow, verifier, localization and UI source.
- **User authorization:** “Ok. Build responsibly”. This authorizes responsible continuation of the existing work; it does not authorize merge to `main`, production release, signing identity changes, destructive migration, clearing app data, or new product scope.
- **Product boundary:** preserve Memo as the general-purpose Android personal AI operating agent. Keep Capture → Observe → Plan → Execute and smart routing as user-stated direction. Do not promote a use case into product identity or create new scope.
- **Exact baseline evidence:** latest stabilization head `2fde1f0793d969192f38fc09465b14cee5310676`; workflow run #923 (`44489842476`) failed at `tools/verify_source.py` before Gradle. The only failure reported by that run is five static Compose UI strings absent from the Polish/Turkish catalog: `Back to workflow`, `Open Work`, `Open browser`, `Plan only · no action has been executed`, `Workflow browser`. Other printed source invariants passed. This does not prove compilation or tests.
- **Historical evidence boundary:** run #821 is green only for its own earlier SHA; PR #10 run #709 is green for its own PR SHA and is not a signed current APK or device acceptance.
- **Data safety:** do not uninstall Memo, clear app data, change applicationId/package identity, replace the pinned signing certificate, or rewrite storage schemas as part of this work. Preserve the user's existing installation and encrypted stores.
- **First bounded work package:** repair the five missing PL/TR UI catalog entries in the existing localization source; add a regression assertion if the existing tests do not already enforce all five; run the exact-head CI pipeline and inspect every stage. No unrelated UI or architecture redesign.
- **Acceptance:** source verifier passes; unit/JVM tests pass; Android compile/lint/package and existing API 35 encrypted-store upgrade instrumentation pass; signed APK is produced only through the existing pinned signer; exact commit and artifact identity/checksum are recorded. Physical-device in-place upgrade remains a separate gate and must not be claimed without testing.
- **Rollback:** revert only the bounded localization/test commit if checks fail. No data migration or app identity change is included.
- **Status at start:** implementation not yet changed; current head unverified; user-facing release not authorized.


## 19 November 2026 — Fresh Start outcome: exact-head build passed

- **Verified commit:** `7ad0eb0426f953a8946741c59247b081a258f94a` on `stabilization/usable-stage-2026-11-10`.
- **Actions:** [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135), conclusion `success`; all build, upgrade-test and isolated-signing jobs succeeded.
- **Repairs made:** five missing Polish/Turkish UI catalog entries and their regression test; read-only `browser.capture_evidence` added to the Talk draft factory allowlist to match the runtime registry/research compiler; corrected the premature closing brace that placed the browser approval dialog outside its composable.
- **Source and tests:** source verifier passed 434/434 static UI literals; remote runner passed; 188 core tests and 44 app tests passed; lint reported 0 errors, 42 warnings and 5 hints; APK and Android-test APK assembled.
- **Data safety:** API 35 signed install-over-existing passed; app-private data and both real encrypted Memo stores survived, with legacy recovery and corruption tests passing. No user phone data was touched.
- **Signed APK:** artifact `14565824727`, versionCode `928`, versionName `4.0.0-memo-debug`, package `app.memo.android.debug`; APK SHA-256 `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`; artifact ZIP SHA-256 `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`; pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Source fingerprint:** `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- **Remaining gate:** physical-device update/UX/provider-session acceptance. PR #8 stays draft/open; `main` remains untouched; no release/merge/destructive operation authorized.
- **Next:** do not start another broad implementation slice before the user has tested the signed APK on the existing installation and recorded device acceptance or concrete regressions.
