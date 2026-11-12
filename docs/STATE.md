# State Ledger
_Last updated: 2026-11-12 by ChatGPT engineering agent — session 2_

## 1. Product snapshot

Memo is an Android-first, local-first personal AI execution environment. Talk captures an intent; Memo assembles and routes bounded work across the user's workspace, local models, visible browser sessions and permitted Android runtimes; Work stores durable records; Live exposes execution and review state; Studio produces editable, evidence-grounded deliverables. Complete means a connected intent-to-outcome loop with explicit checkpoints, durable recovery, observable postconditions, privacy controls and real-device acceptance—not a chatbot shell, disconnected screens or an APK alone.

## 2. Status board

| Area | State | Notes |
|---|---|---|
| Product contract | ✅ established | `docs/MEMO_PRODUCT_CONTRACT.md` is normative; `TASKS.md` is the engineering runbook. |
| Talk | 🟡 partial | Persistent conversations and text/voice entry exist; local TaskSpec expansion is connected, but arbitrary dynamic planning/execution remains incomplete. |
| Work graph | 🟡 partial | Typed records, TaskSpec DAG proposals and checkpoint coordination exist; workspace and native workflow stores remain separate. |
| Local-model Work dispatch | 🟡 partial | One local-model checkpoint can be reserved, dispatched and saved as a review-required artifact. |
| Attempt journal | 🟡 implemented / unverified | Schema-7 per-node attempts, four-attempt cap, output/failure/interruption states and migration coverage exist in source; Gradle tests have not run. |
| Local inference timeout | 🟡 implemented / unverified | 180-second coroutine deadline and Work stop control exist; five regression tests now cover deadline, timeout mapping, cooperative cancellation, success and parent cancellation. Tests have not executed. |
| Live | 🟡 partial | Timeline projection includes jobs, steps, action audit, evidence and attempts; cross-runtime pause/resume/takeover is incomplete. |
| Studio | 🟡 partial | Private typed draft creation/edit/export foundation exists; richer revision comparison and artifact lifecycle remain. |
| Browser | 🟡 bounded slice | Visible provider sessions and exact-content approval exist; no generic browser runtime adapter in the DAG executor. |
| Native Android | 🟡 bounded / unverified | Accessibility workflows are opt-in and foreground/unlocked; not integrated as a generic DAG adapter. |
| Security and storage | 🟡 partial | Encrypted workspace/native stores and migration safeguards exist; eventual single-store boundary and device acceptance remain open. |
| CI / Android build | 🔴 blocked | Vercel builds reached SDK setup and source verification, exposing stale verifier assertions and six missing Polish/Turkish Work strings. Corrections are committed through 3e11e4e, but the exact head has not been rebuilt: GitHub Actions still fails before runner assignment, and Vercel's deployment API quota is exhausted (HTTP 402, 100 deployments/day). |

## 3. Currently in flight

- Draft PR #8 remains open and unmerged on stabilization/usable-stage-2026-11-10. Current exact branch head: 3e11e4ee95ba1c057e4f5613d4c0a402a1fef152. Keep the PR draft; main is untouched.
- The Vercel Android build path is functional through JDK 17 and Android SDK 35 installation. Build dpl_D7crCSwGU6ybcPWeM4NYJAZx2DFm at ffd738a exposed a stale local Work verifier assertion; dpl_7XpJxFes4q85xNAkF5t47JBxruxD at 888d541 exposed an obsolete Talk label assertion; dpl_EaAFvbgqFqAmVA6dYY9WJUvDJq6C at eb255f5 passed the 421/421 static UI translation scan but exposed an obsolete attempt-test marker. The source verifier has been aligned to the current coordinator, composer handoff and existing checkpoint tests in commits through 3e11e4e.
- Added Polish/Turkish translations and regression coverage for the two four-attempt warnings, Generate another draft, active generation, Stop generation and partial-output safety. The Vercel static translation scan passed 421/421 strings on the build that included these translations.
- No exact-head Gradle test, lint, APK assembly, signature check or device test has passed. The latest source verifier correction at 3e11e4e has not yet been executed. Vercel deployment creation now returns HTTP 402 (api-deployments-free-per-day, 100/day, retry-after 86,400 seconds); GitHub Actions still fails before any runner step is created. Do not treat the preceding source-verifier failures as Kotlin compile failures or build successes.
## 4. Known gaps & stubs

- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinator.kt`: reserves and journals checkpoints but is not a runtime dispatcher; only the local-model Work path is dispatched.
- `M3.8.A7` in `TASKS.md`: typed runtime receipts, deterministic/browser/native adapters, shared time/cancellation budgets, pause/resume/takeover and independently observed postconditions remain open.
- Workspace vault and native workflow store remain separate; single-store migration is explicitly deferred until both stores have safe upgrade acceptance.
- Dynamic TaskSpec proposal/compilation is not a complete autonomous DAG engine; no blind external side-effect replay is allowed.
- Android compile, full JVM/Android unit tests, lint, APK assembly/signature validation, emulator upgrade test and physical-device acceptance are not verified on the current PR head.
- Local LiteRT cancellation is cooperative; a native call that does not yield cannot be forcibly stopped by coroutine cancellation. Measure actual timeout and Stop generation latency on-device.
- `docs/STATE.md` did not exist at session start. There is no `docs/SPEC.md` or `docs/RISKS.md`; their roles are covered by `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `PRODUCT_GAP_REGISTRY.md` and `DEVELOPMENT_GOVERNANCE.md`.

## 5. Environment & how to run

- Requirements: JDK 17, Android SDK platform 35, Build Tools 35.0.0, Gradle wrapper 8.14.6.
- Full source gate: `./gradlew --no-daemon --stacktrace :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Additional instrumentation package: `:app:assembleDebugAndroidTest`; the upgrade acceptance workflow requires an API 35 emulator.
- Repository checkout is available through the connected GitHub integration. No local `/testbed` checkout or local Android SDK/Gradle execution environment is mounted in this session.
- Current blocker: GitHub Actions runs #525 (including retry attempt 2), #526–#528 failed before steps/runner assignment; job logs return BlobNotFound. Vercel's Android build route did install JDK/SDK and run the source verifier, but exact-head deployment creation is blocked by the free deployment API quota (HTTP 402, 100/day; retry-after 24 hours). No local /testbed, Android SDK or Gradle executable is mounted here; network access to clone GitHub is unavailable. No Gradle, lint, APK or signing result exists for the current head.

## 6. Next three increments

1. Restore an exact-head build route after Vercel's 24-hour deployment quota resets, or after GitHub Actions assigns a runner. Do not spend further deployments on intermediate commits.
2. On the exact current PR head, run tools/verify_source.py, remote-runner tests, :core:test, :app:testDebugUnitTest, :app:lintDebug, :app:assembleDebugAndroidTest and :app:assembleDebug; then verify package ID, zip alignment, signature identity and SHA-256.
3. Validate install/update behavior and LiteRT timeout/Stop behavior on an Android device. Only then mark the APK clean and proceed to the next runtime adapter.
## 7. Open questions for the human

- None blocking the current reversible test-only increment. PR #8 remains draft; production deployment, signing changes, data migration and merge are not authorized by this session.
