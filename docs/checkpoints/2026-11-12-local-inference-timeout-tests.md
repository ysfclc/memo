# Session checkpoint — local inference timeout regression coverage

## Session identity

- Date/time/timezone: 12 November 2026, Europe/Warsaw
- Repository: `ysfclc/memo`
- Branch: `stabilization/usable-stage-2026-11-10`
- Source branch head before this checkpoint: `d52ba948828fd28b003ea52f8518c54b16960126`
- Base branch / PR: `main` / draft PR [#8](https://github.com/ysfclc/memo/pull/8)
- Working tree state: changes written directly through GitHub Contents API; no local checkout was mounted.
- Product/requirement IDs: `MEMO-AI-001`, `MEMO-ORCH-001`
- Engineering task: `M3.8.A8` (bounded local inference timeout)

## User outcome

- Prevent a local model request from occupying the serialized LiteRT engine indefinitely.
- Preserve the distinction between a timeout and explicit parent/user cancellation.
- Ensure cancellation of timed-out coroutine work and successful complete-result behavior are covered by executable tests.
- APK packaging, new runtime adapters and cross-runtime orchestration were out of scope.

## Baseline inspected

- Product contract: `docs/MEMO_PRODUCT_CONTRACT.md`; product remains the connected Talk / Live / Work / Studio execution environment.
- Governance: `DEVELOPMENT_GOVERNANCE.md`.
- Requirements: `docs/REQUIREMENT_TRACEABILITY.md`.
- Engineering runbook: `TASKS.md`, section M3.8.A8.
- Prior checkpoint: `docs/checkpoints/2026-11-12-workflow-attempt-journal.md`.
- Source: `app/src/main/java/app/memo/android/ai/LocalModelRunner.kt`; implementation constructor is `LocalInferenceTimeoutException(timeoutMs: Long)`, and `withLocalInferenceTimeout` translates only `TimeoutCancellationException` into that dedicated failure.
- Test baseline: `app/src/test/java/app/memo/android/ai/LocalModelRunnerPolicyTest.kt` constructed `LocalInferenceTimeoutException()` with no required argument. This disagreed with the implementation and would fail compilation.
- Existing runtime constraint: the 180-second timeout includes mutex wait, engine initialization and response collection. LiteRT conversation cancellation is cooperative; a native call that never yields cannot be forcibly terminated by Kotlin.
- Existing CI history: repeated GitHub Actions preflight failures on PR #8 have prevented Gradle tests/lint/build from starting. These failures are not Kotlin compilation results.

## Work completed

- Fixed the timeout test to pass `LocalModelRunner.INFERENCE_TIMEOUT_MS` and assert the expected 180-second failure message.
- Expanded `LocalModelRunnerPolicyTest` to five tests:
  1. the configured deadline is 180 seconds;
  2. timeout is a distinct inference failure with a partial-response warning;
  3. timeout cancels the in-flight coroutine and returns `LocalInferenceTimeoutException`;
  4. successful inference returns the complete result;
  5. parent/user cancellation remains `CancellationException`, not a timeout.
- Refreshed the SHA-256 entry for that exact test file in `SOURCE_SHA256SUMS.txt`.
- Added the missing `docs/STATE.md` context ledger; updated the runbook, changelog, implementation log and requirement traceability. This checkpoint completes the session documentation set.
- No production implementation, storage schema, dependency, permission, provider integration or external side effect was changed.

## Files changed

- `app/src/test/java/app/memo/android/ai/LocalModelRunnerPolicyTest.kt`
- `SOURCE_SHA256SUMS.txt`
- `docs/STATE.md`
- `TASKS.md`
- `CHANGELOG.md`
- `IMPLEMENTATION_LOG.md`
- `docs/REQUIREMENT_TRACEABILITY.md`
- `docs/checkpoints/2026-11-12-local-inference-timeout-tests.md`

## Verification evidence

| Check | Exact command / CI run / device steps | Environment and commit | Result | Evidence |
|---|---|---|---|---|
| Test source review | Re-fetched the exact file and confirmed the parameterized exception call and five `@Test` cases | GitHub branch, `d52ba948828fd28b003ea52f8518c54b16960126` | source inspection only | current branch file |
| Checksum consistency | Local `sha256sum` of the exact five-test source; matched the refreshed manifest entry | `63df0cd04d610938a2bbcc7dfe047024b8e24196bbf0c51adeebc47286760755` | pass | `SOURCE_SHA256SUMS.txt` |
| Gradle/JVM tests | `./gradlew --no-daemon --stacktrace :core:test :app:testDebugUnitTest` | Not available in this session; no local checkout/Gradle distribution/Android SDK | not run | — |
| Exact-head CI | GitHub Actions run #521 on `d52ba948828fd28b003ea52f8518c54b16960126` | PR #8 | blocked: ZIP preflight failed; validation/build/signing jobs skipped | https://github.com/ysfclc/memo/actions/runs/43144202615 |
| CI logs | Fetch preflight job log for job `130938079778` | GitHub Actions | unavailable: `BlobNotFound` | log endpoint |
| Lint / APK / emulator / real device | Not run because preflight did not reach downstream jobs and no local Android toolchain/device was available | — | not run | — |

## Requirement and task ledger updates

- `MEMO-AI-001`: timeout failure, cancellation distinction and bounded local inference behavior now have regression-test source; execution and device behavior remain unverified.
- `MEMO-ORCH-001`: the local-model dispatch slice retains its checkpoint/attempt journal; shared cross-runtime timeout/cancellation and runtime adapters remain open.
- `M3.8.A8`: implementation and regression tests are present; acceptance remains open until exact-head test/lint/build and device timing evidence exist.
- Changelog and implementation log explicitly record that tests were added but not executed.

## Decisions and deviations

- No product or architecture decision was needed. The implementation's timeout constructor/API was treated as authoritative; the broken test was corrected to match it.
- No dependency or permission was added.
- Work stayed on the existing PR branch. `main` and Memo 2 were not modified.

## Risks, blockers and unresolved questions

| Item | Impact / severity | Current mitigation | Next action |
|---|---|---|---|
| Actions preflight failure | High: exact-head Gradle, lint and APK evidence unavailable | Repeated failures are documented; avoid blind reruns | Restore GitHub Actions runner/preflight service/capacity, then run full checks |
| Cooperative LiteRT cancellation | Medium/high: a non-yielding native inference call may outlive coroutine cancellation | Bounded timeout and explicit cancellation semantics; no false successful output | Measure cancellation/timeout latency on the supported physical phone |
| Cross-runtime executor incomplete | High product gap, not introduced by this increment | Browser/native/external dispatch remains isolated and approval-gated | Continue only after exact-head build/test gate is available |

## Recovery / rollback

- The change is test and documentation only; no persisted user data or schema migration is affected.
- Revert the focused test/doc commits on this PR branch if necessary; do not rewrite history or modify `main`.
- The checksum manifest must remain synchronized with the final test source.

## Next recommended action

- Resolve the GitHub Actions ZIP-preflight/runner blocker and run the exact-head core/app test suite, lint and Android debug build.
- Then measure timeout and Stop generation behavior against the actual LiteRT-LM binary on an Android device.
- Only after those gates pass, choose the next registered runtime adapter slice for the durable workflow executor.

## User-facing handoff

- What is genuinely working now: timeout regression tests are present in source; checksum and project docs are aligned.
- What remains incomplete: the tests have not run; local native cancellation has not been device-validated; the full cross-runtime DAG executor remains incomplete.
- What was not tested: Gradle compilation, JVM tests, Android tests, lint, APK assembly/signing, emulator and physical-device behavior.
- What requires user decision: nothing for this reversible test-only increment; PR #8 remains a draft and unmerged.


## Follow-up — CI preflight retry (12 November 2026, 09:00 Europe/Warsaw)

- Re-inspected `.github/workflows/build-apk.yml`. Its `pull_request` branch explicitly accepts PRs targeting `main`, sets `source_only=true`, and exits successfully without importing/signing/writing repository source. PR #8 base is confirmed as `main`.
- Latest run #525, ID `43144626609`, at head `1c3bde60a78232b82a8f1cfbb021bf11bf75a11d`, failed in the preflight job. GitHub's jobs response contains `steps: null`; no runner ID/name is present; downstream build/sign/commit jobs were skipped.
- Explicitly retried the failed preflight job (attempt 2). It failed again within seconds with `steps: null`; the log endpoint returned `BlobNotFound`.
- The same no-step failure pattern is visible on recent PR runs and a push to `main` (run #507), which makes a PR base-branch guard or this test patch an unlikely cause. The workflow script itself did not execute, so no ZIP validation or Gradle failure has been demonstrated.
- Environment check: no `/testbed` or Memo checkout exists in the execution container; no Gradle executable or Android SDK is installed; outbound DNS/network access to GitHub is unavailable, so a local clone/build could not be established.
- **Conclusion:** verification is blocked before runner execution, not failed by a demonstrated source defect. Do not edit the workflow or application code to guess at an infrastructure error. Resume when a functioning GitHub Actions runner/service or an approved local CI environment is available.
