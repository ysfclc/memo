# State Ledger
_Last updated: 2026-11-12 by ChatGPT engineering agent — session 5_

## 1. Product snapshot

Memo is an Android-first, local-first personal AI execution environment. Talk captures an intent; Memo assembles and routes bounded work across the user's workspace, local models, visible browser sessions and permitted Android runtimes; Work stores durable records; Live exposes execution and review state; Studio produces editable, evidence-grounded deliverables. Complete means a connected intent-to-outcome loop with explicit checkpoints, durable recovery, observable postconditions, privacy controls and real-device acceptance—not a chatbot shell, disconnected screens or an APK alone.

## 2. Status board

| Area | State | Notes |
|---|---|---|
| Product contract | ✅ established | `docs/MEMO_PRODUCT_CONTRACT.md` is normative; `TASKS.md` is the engineering runbook. |
| Talk | 🟡 partial | Persistent conversations and text/voice entry exist; local TaskSpec expansion is connected, but arbitrary dynamic planning/execution remains incomplete. |
| Work graph | 🟡 partial | Typed TaskSpec DAG proposals, dependency-aware checkpoint reservation, durable attempts and first content-bound runtime receipts exist. Generic cross-runtime execution is not yet implemented. |
| Local-model Work dispatch | 🟡 partial | A local-model checkpoint can be reserved, dispatched and saved as a review-required artifact. New output receipts bind the saved bytes to the exact attempt. |
| Attempt journal | 🟡 implemented / unverified | Schema-7 per-node attempts, four-attempt cap, output/failure/interruption states and migration coverage exist in source. New output attempts require a matching typed receipt in both journal and artifact metadata; legacy schema-7 output attempts remain compatible. Gradle tests have not run. |
| Runtime postconditions | 🔴 incomplete | Receipts explicitly remain `NOT_VERIFIED`; independent deterministic/browser/native observers are not yet connected to the DAG. |
| Local inference timeout | 🟡 implemented / unverified | 180-second coroutine deadline and Work stop control exist; five regression tests cover deadline, timeout mapping, cooperative cancellation, success and parent cancellation. Tests have not executed. |
| Live | 🟡 partial | Timeline projects jobs, steps, action audit, evidence and attempts. Receipt-backed attempts now show a short output-hash fingerprint and explicit `NOT_VERIFIED` postcondition state without exposing model payloads. Cross-runtime pause/resume/takeover remains incomplete. |
| Studio | 🟡 partial | Private typed draft creation/edit/export foundation exists; richer revision comparison and artifact lifecycle remain. |
| Browser | 🟡 bounded slice | Visible provider sessions and exact-content approval exist; no generic browser runtime adapter in the DAG executor. |
| Native Android | 🟡 bounded / unverified | Accessibility workflows are opt-in and foreground/unlocked; not integrated as a generic DAG adapter. |
| Security and storage | 🟡 partial | Encrypted workspace/native stores and migration safeguards exist; eventual single-store boundary and device acceptance remain open. |
| CI / Android build | 🔴 blocked | GitHub Actions run #587 failed before runner assignment (`runner_id=0`, no steps). Vercel's next deployment was rejected with HTTP 402 due to the 100-deployments/day quota. No exact-head full test, lint or signed APK acceptance exists. |

## 3. Current branch and delivery state

- Private repo: `ysfclc/memo`.
- Development branch: `stabilization/usable-stage-2026-11-10`.
- Draft PR #8 remains open and unmerged, targeting `main`: https://github.com/ysfclc/memo/pull/8
- Latest source/documentation checkpoint before this ledger update: `fca5453aa02561bec33a4785d7b6fd2a0daa4214`. Keep `main` untouched.
- Vercel build investigation: JDK 17 and Android SDK 35 installed; compilation and `:app:assembleDebug` were reached. The script then exited 141 (SIGPIPE) during package/certificate parsing. The parsing pipelines were replaced with full-stream-safe parsing and the source checksum refreshed. A subsequent deployment was rejected with HTTP 402 `api-deployments-free-per-day` (100 deployments/day; retry after 24 hours). No complete build result is claimed.
- GitHub Actions latest inspected run #587 fails before any runner or step starts. This does not demonstrate a Kotlin/Gradle failure. Do not repeatedly trigger identical runs while the runner/account issue is unresolved.
- Vercel Preview still needs the existing four signing secrets configured by the user before a pinned-certificate upgrade APK can be produced. Never substitute an ephemeral signing key for the persistent Memo signer.
- No production deployment, signing identity change, data migration or merge is authorized.

## 4. Latest product increment — typed local runtime receipts

- Added `core/src/main/kotlin/app/memo/android/data/WorkflowRuntimeReceipt.kt`.
- Local Work output receipts bind attempt ID/number, job/checkpoint, runtime ID, exact capability IDs, stable idempotency key, start/completion timestamps, Evidence ID and SHA-256 of the saved output bytes.
- The receipt is attached atomically with the review-required artifact and attempt transition, inside existing Evidence metadata. No schema migration was introduced.
- The journal marker and Evidence metadata must agree; workspace validation rechecks receipt identity and output hash. Schema-7 historical output attempts without a receipt marker remain compatible.
- The receipt explicitly says `postconditionStatus=NOT_VERIFIED`. It proves only that Memo saved the exact runtime output; it does not prove the user's requested outcome or authorize an external action.
- Live now surfaces a short output-hash fingerprint and the unverified postcondition state for receipt-backed attempts, without rendering the saved model output.
- Regression tests cover receipt binding, workspace validation, output mutation, legacy schema-7 compatibility and Live payload privacy. `tools/verify_source.py` and `SOURCE_SHA256SUMS.txt` were updated.
- Full details: `docs/checkpoints/2026-11-12-runtime-receipts.md`.

## 5. Known gaps & stubs

- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowCheckpointCoordinator.kt` reserves and journals safe checkpoints but is not a generic runtime dispatcher; only the local-model Work path is dispatched.
- Remaining orchestration work: shared runtime adapter contract; deterministic/browser/native adapters; shared duration/model-call/external-write budgets; cross-runtime cancellation and pause/resume/takeover; independent postcondition observers and evidence.
- Workspace vault and native workflow store remain separate; single-store migration is deferred until both stores have safe upgrade acceptance.
- Dynamic TaskSpec proposal/compilation is not a complete autonomous DAG engine; no blind external side-effect replay is allowed.
- Android compile, full JVM/Android unit tests, lint, APK assembly/signature validation, emulator upgrade test and physical-device acceptance are not verified on the current PR head.
- Local LiteRT cancellation is cooperative; a native call that does not yield cannot be forcibly stopped by coroutine cancellation. Measure actual timeout and Stop generation latency on-device.

## 6. Environment & verification

- Requirements: JDK 17, Android SDK platform 35, Build Tools 35.0.0, Gradle wrapper 8.14.6.
- Full source gate: `./gradlew --no-daemon --stacktrace :core:test :app:testDebugUnitTest :app:lintDebug :app:assembleDebug`.
- Additional instrumentation package: `:app:assembleDebugAndroidTest`; upgrade acceptance requires an API 35 emulator.
- No local `/testbed` checkout, Android SDK or Gradle executable is mounted in this session. Do not claim source tests ran.
- The source manifest tracks the new receipt source, modified journal/coordinator/tests, Live projection/tests and source verifier. It was refreshed in commit `f02dc633f450bf1be041c0f30a03be95b0736752`.

## 7. Next three increments

1. Add a shared runtime-adapter contract and deterministic postcondition evidence interface. It must accept only the exact runtime/capability manifest compiled into the TaskSpec and return a typed receipt; it must not grant approvals.
2. Implement the deterministic app-runtime adapter slice and wire it through the durable checkpoint journal with shared timeout/cancellation and process-death recovery. Prove it with pure-core tests before connecting browser/native actions.
3. Add browser/native adapters as visible, permission-aware runtimes with their existing exact-content approval boundaries, then unify Live pause/resume/takeover and independently observed postconditions.

## 8. Open questions for the human

- None blocking the next reversible core-engine increment. PR #8 remains draft; keep production deployment, signing changes, data migration and merge out of scope.
