# State Ledger
_Last updated: 2026-11-12 by ChatGPT engineering agent — session 1_

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
| CI / Android build | 🔴 blocked | Exact-head run #521 failed in ZIP preflight; validation, Gradle, lint and APK jobs were skipped. |

## 3. Currently in flight

- Draft PR #8, branch `stabilization/usable-stage-2026-11-10`. Latest source commit with the timeout test is `a44c6c35c013163aa649553c64fa34eda26a809e`; checksum manifest refresh is `d52ba948828fd28b003ea52f8518c54b16960126`. The current branch also contains the new checkpoint and state-ledger documentation. Keep the PR draft and do not merge without the exact-head gates.
- M3.8.A8 regression coverage is present and documented, but **not executed**. Latest observed exact-head workflow run #521 on `d52ba948828fd28b003ea52f8518c54b16960126` failed before downstream validation; the job-log endpoint returned `BlobNotFound`.

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
- Current blocker: GitHub Actions run #521 preflight failed with no usable job log; downstream jobs were skipped. Earlier PR runs also failed before runner assignment. Do not infer a source compilation result from these failures.

## 6. Next three increments

1. Resolve the GitHub Actions incoming-ZIP preflight/runner blocker (run #521) or restore an approved equivalent CI path; do not keep blindly rerunning while no runner/diagnostic is available.
2. On the exact PR head, run `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug`; record exact commit and results before making further execution-engine claims.
3. Validate real LiteRT timeout and user Stop behavior on an Android device. After those gates pass, continue the next registered runtime adapter with typed receipts and visible Live state while preserving approval isolation and NEEDS_REVIEW recovery.

## 7. Open questions for the human

- None blocking the current reversible test-only increment. PR #8 remains draft; production deployment, signing changes, data migration and merge are not authorized by this session.
