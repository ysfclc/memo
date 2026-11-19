# Memo Session Checkpoint — 2026-11-19 Responsible Stabilization

## Session identity
- Repository: `ysfclc/memo`
- Branch: `stabilization/usable-stage-2026-11-10`
- Inspected base SHA: `2fde1f0793d969192f38fc09465b14cee5310676`
- User instruction: “Ok. Build responsibly”.
- User-input ledger: read; no new product requirement or scope expansion was introduced.

## User outcome
Restore a trustworthy build path without risking the existing Memo installation or changing the product direction.

## Baseline inspected
- Read agent/governance rules, user-input ledger, product contract, decision log, requirement traceability, state ledger, task ledger, product integrity audit, fresh-start master ledger, CI workflow and relevant source.
- Exact-head run #923 (`44489842476`) failed before Gradle in `tools/verify_source.py` due to five missing PL/TR translations.
- Run #821 and PR #10 run #709 are historical exact-SHA evidence only, not validation of the current branch head.

## Work package
- Add Polish and Turkish catalog entries for existing UI strings only.
- Add/confirm focused localization regression coverage.
- Preserve all unrelated features and all existing bug/feedback records.

## Safety constraints
- No merge to main, production release, destructive migration, app-data clearing, uninstall, applicationId change or signing-certificate change.
- Existing encrypted-store upgrade tests must continue to pass.

## Verification
| Check | Baseline result |
|---|---|
| Source verifier | Failed on five missing PL/TR catalog entries |
| JVM/unit tests | Not reached in run #923 |
| Android compile/lint/package | Not reached in run #923 |
| API 35 encrypted-store upgrade | Not reached in run #923 |
| Pinned signing/artifact | Skipped because build job failed |
| Physical device | Not tested; remains pending |

## Next action
Fix only the five localization omissions, run the exact-head CI, inspect tests/build/instrumentation/signing/artifact results, and update this checkpoint with the resulting SHA and evidence. Do not install an artifact unless package identity and pinned certificate match the existing installation.


## Final outcome — 19 November 2026

| Gate | Exact-head result |
|---|---|
| Source verifier | PASS; 434/434 static UI Text literals translated |
| Remote runner | PASS |
| Core JVM tests | 188/188 passed |
| Android unit tests | 44/44 passed |
| Android lint | 0 errors, 42 warnings, 5 hints |
| Debug APK and Android-test APK assembly | PASS |
| APK package/version/signature verification | PASS |
| API 35 install-over-existing | PASS; app-private data and both real encrypted Memo stores preserved; legacy recovery/corruption checks passed |
| Isolated pinned signing | PASS; certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a` |
| Physical-device acceptance | NOT TESTED; remains open |

- Exact source commit: `7ad0eb0426f953a8946741c59247b081a258f94a`.
- Actions: [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135), conclusion `success`.
- Signed APK artifact: [14565824727](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727), versionCode `928`, versionName `4.0.0-memo-debug`.
- APK SHA-256: `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`; artifact ZIP SHA-256: `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`; source fingerprint: `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- Remaining work: physical-device in-place update, visual/touch/navigation acceptance and real signed-in provider walkthrough. Preserve the existing installation; no uninstall or clear-data workaround.
- PR #8 remains draft/open; `main` untouched; no merge or production release authorized.

## Next action
The automated stabilization gates are green. Hand the signed APK to the user for an in-place update on the existing phone and collect actual UX/provider-session feedback before starting another implementation increment.
