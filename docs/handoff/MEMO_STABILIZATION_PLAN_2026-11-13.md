# Memo — STOP-WORK stabilization plan

**Plan date:** 13 November 2026  
**Repository:** `ysfclc/memo` (Memo 1; Memo 2 is out of scope)  
**Working branch:** `stabilization/usable-stage-2026-11-10`  
**Plan baseline commit:** `99f79d975cf7276c21eb32adee6831824c3ff35b`  
**Tracking PR:** [#8](https://github.com/ysfclc/memo/pull/8) — draft, unmerged  
**Status:** STOP-WORK / stabilization only

## 1. Mandate and change freeze

Memo is treated as broken/unaccepted until the existing application passes the gates in this plan. This is an engineering recovery plan, not a product roadmap.

Effective immediately:

- No new features, product-scope expansion, visual redesign, refactoring unrelated to a reproduced blocker, or dependency upgrades.
- No merge to `main`, production deployment, release, or distribution of a candidate APK before all applicable gates pass.
- No weakening, skipping, deleting, or reclassifying tests, source checks, signing checks, permission checks, or security boundaries to obtain a green result.
- No ephemeral signing key as a substitute for Memo's pinned signing identity. Never install a differently signed preview package over the user's existing installation.
- Preserve user data and existing encrypted stores. No destructive migration, reset, clear-data instruction, or store consolidation during stabilization.
- A source edit, successful deployment page, APK assembly, or passing unit test is not by itself proof that the app works.
- Every claim must identify the exact commit, command/workflow, result, and evidence. Unexecuted tests remain unverified.

Only changes directly necessary to reproduce, diagnose, fix, or verify an existing failure are in scope. A change that cannot be tied to a failure or acceptance gate must wait.

## 2. Current source and release identity

- Exact branch head when this plan was written: `99f79d975cf7276c21eb32adee6831824c3ff35b`.
- The head contains the narrowly scoped local-model fallback change from commit `99f79d9`: CPU fallback is allowed only before any visible text has streamed; a regression test and source-verifier assertion were added.
- That patch is **not verified**. Its test has not been executed on this commit.
- The branch is not `main`. PR #8 remains draft and unmerged.
- The last recorded persistent signing certificate is SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`. Any release candidate must prove it matches this pinned certificate.
- The 8 November APK and the previous isolated `app.memo.android.preview` APK are not acceptance evidence for the current source. The latter used an ephemeral debug key and must not be installed over the existing Memo app.

## 3. Known failures and open acceptance blockers

| ID | Area | Reproduction / expected result | Current evidence / actual result | Priority |
|---|---|---|---|---|
| STAB-01 | GitHub Actions runner startup | Trigger the PR workflow; preflight should obtain a runner and execute its first step. | Recent jobs fail before step 1; `runner_id=0`, no usable job logs. A minimal workflow failed at the same boundary. No source/build failure can be inferred. | P0 blocker |
| STAB-02 | Vercel build capacity | Submit a deployment for the exact candidate SHA; it should execute the pinned Gradle build. | Deployment API returned HTTP 402 `api-deployments-free-per-day` (100/day limit); latest candidate has no deployment. | P0 blocker |
| STAB-03 | Local model GPU fallback | Stream partial output, force/encounter a GPU execution failure, and inspect the response. A request must not be replayed after visible output. | Prior code could replay on CPU and append a second answer. A guard and regression test are committed in `99f79d9`; not yet executed. | P1 code fix pending verification |
| STAB-04 | Encrypted storage upgrade | Seed both encrypted stores, install a higher-version APK with `adb install -r`, reopen, and verify both stores and existing records. | `EncryptedStoreUpgradeTest` exists and is included in the intended API 35 instrumentation path, but exact-head emulator execution is not evidenced. Physical-device upgrade is also open. | P0 data-safety gate |
| STAB-05 | Persistent signing identity | Compare the release candidate certificate to the pinned SHA-256 above. | Previous preview was signed with an ephemeral debug key; no current candidate with the pinned certificate is available. | P0 release gate |
| STAB-06 | Android runtime acceptance | Install and exercise launch, auth/unlock, navigation, chat persistence, import/export, voice, browser approval, native automation, notifications and cancellation. | Device checklist remains unchecked; no current-head physical-device evidence. | P0 acceptance gate |
| STAB-07 | Local inference Stop semantics | Start a long generation, press Stop, measure visible stop latency and ensure native work does not continue or save partial output as complete. | Coroutine deadline and `cancelProcess()` are cooperative; a blocking native call may not stop promptly. No on-device measurement. | P1 runtime gate |
| STAB-08 | Workflow completion semantics | Run a workflow and verify the requested postcondition independently before reporting success. | Current local receipt explicitly reports `NOT_VERIFIED`; generic cross-runtime dispatch and independent postcondition observers remain incomplete. | Existing product limitation; no scope expansion in this incident |

### Known issues deliberately not being treated as current crashes

The repository documents incomplete generic cross-runtime DAG execution, browser/native runtime adapters, pause/resume/takeover, independent postcondition observers, and a future single-store boundary. These are product completeness gaps, not reproduced crashes in this incident. They remain frozen and must not be expanded while stabilizing the currently implemented paths. The app must never claim a workflow outcome is verified when the receipt says `NOT_VERIFIED`.

## 4. Stabilization sequence and exit criteria

### Phase 0 — preserve and establish truth

1. Keep this branch and PR isolated; record the exact source SHA before every run.
2. Preserve the existing signing identity and both encrypted stores.
3. Capture failing run IDs, job IDs, deployment IDs, response codes and logs where available.
4. Do not repeatedly submit identical CI/deployment attempts while the external quota/runner condition is unchanged.

**Exit:** source identity and data/signing constraints recorded; no untracked production changes.

### Phase 1 — restore one trustworthy build executor

1. Diagnose GitHub job startup using the minimal workflow result and available account/repository runner evidence. Escalate the runner/account issue where the platform does not expose a cause.
2. Wait for the Vercel daily quota to reset or use an already-authorized, available build executor. Do not create another service/account or spend money without explicit authorization.
3. Use one route as the canonical exact-SHA build and keep the other as a diagnostic/fallback only.
4. Run the source verifier and remote-runner tests before Gradle. Preserve full logs and exit codes.

**Exit:** the exact candidate SHA has an actual executor log showing that the first job step started. A READY web deployment without build logs is not a build.

### Phase 2 — deterministic source and Android verification

Run, without weakening the workflow:

```bash
python3 tools/verify_source.py
python3 -m unittest discover -s remote-runner -v
./gradlew --no-daemon --stacktrace \
  :core:test \
  :app:testDebugUnitTest \
  :app:lintDebug \
  :app:assembleDebug \
  :app:assembleDebugAndroidTest
```

- Inspect test reports, not just the final Gradle exit code.
- Confirm the new GPU fallback regression test executes and passes on the exact head.
- If a failure occurs, record the failing test/task and fix only its root cause. Re-run the full required gate after the fix.
- Never report instrumentation assembly as instrumentation execution.

**Exit:** source verifier, remote-runner suite, all core/app unit tests, lint and both APK assemblies pass on one exact commit, with retained reports.

### Phase 3 — API 35 emulator and encrypted-store acceptance

1. Execute the instrumentation suite on an API 35 emulator; include `EncryptedStoreUpgradeTest`.
2. Seed the real encrypted workspace and native workflow stores using the isolated test fixture.
3. Perform a version-increasing signed `adb install -r` upgrade.
4. Verify old records decrypt and remain available; verify both legacy migration directions and AtomicFile recovery.
5. Verify corrupt, unknown-format and damaged-header inputs fail closed and are preserved for recovery; do not silently overwrite them.
6. Record instrumentation XML/logs and emulator image/API version.

**Exit:** emulator instrumentation actually ran and passed on the exact candidate; no data-loss or migration regression.

### Phase 4 — signing and artifact integrity

1. Build the release/upgrade candidate through the isolated signing job using the existing authorized secrets.
2. Extract and compare the signing certificate SHA-256 with the pinned value in section 2.
3. Record source SHA, package ID, version code/name, artifact URL, APK SHA-256 and certificate SHA-256.
4. Reject the candidate if the signing key is missing, ephemeral, mismatched or unverifiable. Do not silently fall back to debug signing.

**Exit:** reproducible artifact metadata and matching pinned certificate are recorded. No release or merge yet.

### Phase 5 — physical-device acceptance

Use the actual user's Android device and the signed candidate. Back up through the existing supported process before upgrade; do not clear app data.

Minimum checks:

- Install over the existing Memo app; confirm package/signature continuity and preserve existing chats, vault data, workflow records and app-private data.
- Launch, unlock/authenticate, force-stop/reopen and navigate all primary surfaces without crashes.
- Create, rename, reopen and delete a test chat; verify persistence.
- Import shared text/file, create an entity/relationship, export a backup and restore only into a disposable test state.
- Test local model discovery, load, generation, GPU failure fallback, streaming, Stop and timeout. Confirm no duplicate partial output and measure Stop latency.
- Test voice permission denial/grant and English, Polish and Turkish input without automatic submission.
- Test browser session visibility and exact-content approval; ensure navigation drift pauses for takeover.
- Test native automation only with explicit permission and harmless foreground actions; lock/background/cancel must stop execution.
- Test notifications/reminders and confirm private content is not exposed.
- Record every failure with steps, expected/actual result, severity, device model, Android version and logs/screenshots.

**Exit:** every applicable device checklist item is marked pass with evidence or recorded as an explicit remaining blocker. No data loss, unauthorized action, crash or signing mismatch.

### Phase 6 — closeout decision

1. Update `BUILD_STATUS.md`, `DEVICE_ACCEPTANCE_CHECKLIST.md`, `docs/STATE.md` and this plan with exact evidence.
2. Produce a bug-by-bug report: fixed, reproduced/open, not reproducible, or blocked by environment.
3. Keep PR #8 draft and do not merge until all P0 gates pass and the product owner explicitly authorizes the next release decision.
4. If an external runner/quota/device blocker persists, stop and escalate with evidence; do not relabel it as success.

**Final exit:** exact-head source tests, Android tests, lint, APK assembly, API 35 instrumentation, pinned signing verification and physical-device acceptance all pass. Only then may the baseline be described as verified working.

## 5. Immediate next actions

1. Do not make further source changes until the current `99f79d9` candidate can be built and its regression test executed.
2. Recheck GitHub runner startup and Vercel quota once, using the existing authorized project and branch. Capture the result.
3. If neither executor is available, report the platform blocker and wait; do not generate more unverified commits or APKs.
4. When execution is restored, begin at Phase 2 against the exact branch head, then proceed sequentially. Do not skip emulator, signing or device gates.

## 6. Reporting contract

Every status update must use:

- **What is broken:** exact bug IDs and reproduction evidence.
- **What changed:** commit and files; no claim that a source edit is a verified fix.
- **How verified:** exact command/workflow, commit SHA, exit code, test report and artifact hashes.
- **What remains:** unexecuted tests and device checks.
- **Current blockers:** external and technical blockers distinguished.
- **ETA:** only after a working executor and reproducible duration are established; otherwise state that no defensible ETA exists.

A green build is necessary but not sufficient. A working baseline is an installed, upgrade-safe application whose implemented critical paths have been exercised and whose failures are reported honestly.
