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
| Live | 🟡 partial | Timeline projection includes jobs, steps, action audit, evidence and attempts; cross-runtime pause/resume/takeover is incomplete. |
| Studio | 🟡 partial | Private typed draft creation/edit/export foundation exists; richer revision comparison and artifact lifecycle remain. |
| Browser | 🟡 bounded slice | Visible provider sessions and exact-content approval exist; no generic browser runtime adapter in the DAG executor. |
| Native Android | 🟡 bounded / unverified | Accessibility workflows are opt-in and foreground/unlocked; not integrated as a generic DAG adapter. |
| Security and storage | 🟡 partial | Encrypted workspace/native stores and migration safeguards exist; eventual single-store boundary and device acceptance remain open. |
| CI / Android build | 🔴 blocked | Current GitHub Actions runs fail in preflight before runner assignment; Gradle, lint, APK and device checks are not established for the active PR head. |

## 3. Currently in flight

- Draft PR #8, branch `stabilization/usable-stage-2026-11-10`, head `60ce82e78e0f11cd95ded3c3b33f0a24864b68c6` — durable Work attempt journal, local-model dispatch timeout and Work cancellation; implementation is in source, but exact-head build/test validation remains blocked.
- Local inference timeout contract — current policy test has a no-argument constructor call while `LocalInferenceTimeoutException` requires `timeoutMs`; repair and add cancellation/timeout behavior tests before expanding runtime dispatch.

## 4. Known gaps & stubs

- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinator.kt`: reserves and journals checkpoints but is not a runtime dispatcher; only the local-model Work path is dispatched.
- `M3.8.A7` in `TASKS.md`: typed runtime receipts, deterministic/browser/native adapters, shared time/cancellation budgets, pause/resume/takeover and independently observed postconditions remain open.
- Workspace vault and native workflow store remain separate; single-store migration is explicitly deferred until both stores have safe upgrade acceptance.
- Dynamic TaskSpec proposal/compilation is not a complete autonomous DAG engine; no blind external side-effect replay is allowed.
- Android compile, full JVM/Android unit tests, lint, APK assembly/signature validation, emulator upgrade test and physical-device acceptance are not verified on the current PR head.
- `docs/STATE.md` did not exist at session start. There is no `docs/SPEC.md` or `docs/RISKS.md`; their roles are covered by `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `PRODUCT_GAP_REGISTRY.md` and `DEVELOPMENT_GOVERNANCE.md`. Avoid creating competing documents without need.

## 5. Environment & how to run

- Requirements: JDK 17, Android SDK platform 35, Build Tools 35.0.0, Gradle wrapper 8.14.6.
- Full source gate: `./gradlew --no-daemon --stacktrace :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Additional instrumentation package: `:app:assembleDebugAndroidTest`; the upgrade acceptance workflow requires an API 35 emulator.
- Repository checkout is available through the connected GitHub integration. No local `/testbed` checkout or local Android SDK/Gradle execution environment is mounted in this session.
- Current blocker: recent GitHub Actions preflight jobs end with `runner_id: 0` and zero steps; downstream tests/build/lint are skipped. Do not infer a source compilation result from these failures.

## 6. Next three increments

1. Fix the local inference timeout test/API mismatch and add deterministic timeout, timeout-cancellation and parent-cancellation regression coverage; then obtain an exact-head CI result.
2. Resolve the Actions preflight runner blocker or establish a supported equivalent CI path; run core/app tests, lint and Android compilation before adding more runtime adapters.
3. Implement the next smallest registered runtime adapter with typed receipts and visible Live state, preserving approval isolation and NEEDS_REVIEW recovery.

## 7. Open questions for the human

- None blocking this increment. The product contract and active PR establish the current scope; no irreversible architecture or external side effect is proposed.
