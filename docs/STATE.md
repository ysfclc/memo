# State Ledger
_Last updated: 2026-11-12 by ChatGPT engineering agent — session 3_

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
| CI / Android build | 🔴 blocked | Source verifier and 421/421 translation scan have been corrected; stabilization-branch push now targets the existing isolated pinned-certificate signer without allowing repository writes to this branch. GitHub Actions still fails before runner assignment (latest run #535); Vercel's deployment API quota is exhausted (HTTP 402, 100 deployments/day). |

## 3. Currently in flight

- Draft PR #8 remains open and unmerged on stabilization/usable-stage-2026-11-10. The last pre-wiring branch head was ef362ff3766cace4b01ce59c07fb0eec9cebc345; this increment adds a branch-specific source-only build path. Keep main untouched.
- The Vercel Android build path successfully installed JDK 17 and Android SDK 35 and reached source verification. Static UI translation scan passed 421/421 strings. Exact-head Vercel deployment is blocked by HTTP 402 api-deployments-free-per-day (100/day, retry-after 86,400 seconds).
- GitHub Actions PR source-only runs fail before runner assignment: latest run #535 has runner_id 0, empty runner_name, steps [], and downstream build/sign jobs skipped. No Gradle execution is demonstrated.
- The workflow now accepts direct source pushes on this stabilization branch. It runs source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, emulator install-over-existing acceptance and then the existing isolated pinned-certificate signing job. The separate repository-write job and permanent import-note step are restricted to main.
- No exact-head Gradle test, lint, emulator, APK assembly or signature check has passed. Do not call any APK clean until the pinned certificate SHA-256, package identity, zip alignment and artifact checksum are verified from a successful run.
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
- Current blocker: GitHub Actions runs #525–#528 and latest #535 failed before runner assignment; run #535 has runner_id 0, no steps and no job logs. Vercel exact-head deployment creation is blocked by HTTP 402 (100 deployments/day; retry-after 24 hours). No local /testbed, Android SDK or Gradle executable is mounted here; network access to clone GitHub is unavailable. No Gradle, lint, APK or signing result exists for the current head.

## 6. Next three increments

1. Let the stabilization-branch push trigger the newly enabled source-only workflow. If GitHub still returns runner_id 0 before steps, wait for GitHub-hosted runner service/capacity; do not repeat blind job retries.
2. On the first run with a real runner, require source verification, remote-runner tests, :core:test, :app:testDebugUnitTest, :app:lintDebug, :app:assembleDebug, :app:assembleDebugAndroidTest and API 35 emulator upgrade acceptance to pass; then require the isolated signer to verify the pinned Memo certificate.
3. Download the final pinned-certificate APK artifact, independently verify its package, version, alignment, certificate and SHA-256, then install it on the target Android device. Only then mark the APK clean.
## 7. Open questions for the human

- None blocking the current reversible test-only increment. PR #8 remains draft; production deployment, signing changes, data migration and merge are not authorized by this session.
