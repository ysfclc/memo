# State Ledger
_Last updated: 2026-11-16 — M3.8.A10 deterministic app.entities proposal workflow passed exact-head CI #767, including Android build, unit tests and API 35 install-over-existing encrypted-store preservation. PR #8 remains draft/unmerged; physical-device acceptance and final signed APK handoff remain pending._

## 1. Product snapshot

Memo is the personal AI operating agent on the phone: it understands what the user wants, assembles and runs the right workflow across the user's information, models, websites and Android apps, produces useful work, and keeps the user in control.

The normative product source is `docs/MEMO_PRODUCT_CONTRACT.md`; the complete social operating-layer requirements are in `docs/SOCIAL_PLATFORM_PRODUCT_SPEC.md`. These are user-approved scope, not optional future ideas to be silently reduced. `TASKS.md` is the execution ledger; `docs/DECISION_LOG.md` records material changes.

The intended continuous path is: capture intent → clarify and retrieve permitted context → assemble a typed, versioned workflow → route steps to deterministic code/local AI/frontier AI/visible browser/Android runtime/user → show progress and checkpoints in Live → verify actual postconditions → save evidence and deliverables in Work/Studio. Conversation is the interface, not the database.

Primary surfaces remain Talk, Live, Work and Studio. Work is the local-first persistent graph; Today prioritizes MUST / SHOULD / COULD / NOT TODAY. Memo is not a chatbot skin, generic browser, CRM clone, static task manager or dashboard of disconnected modules.

## 2. Status board

| Area | State | Notes |
|---|---|---|
| Product contract | established | Normative scope preserved; social platform remains first-class and unimplemented until provider-backed evidence exists. |
| Talk | partial | Persistent conversations and text/voice intent entry exist; local TaskSpec expansion is available. Full arbitrary intent-to-outcome orchestration and richer capture remain incomplete. |
| Work graph | partial | Typed DAG proposals, dependency-aware checkpoint reservation, schema-7 attempt journal and content-bound runtime output receipts exist. |
| Shared runtime adapter contract | verified | ORCH-007 request/resolver contract is covered by pure-core tests and exact-head CI. The first concrete adapter is app.entities/entity.propose; a generic dispatcher is not yet wired. |
| Postcondition evidence contract | verified | Registered deterministic observer methods and exact attempt/task/evidence binding exist. No browser/native observer implementation is connected; runtime receipts remain NOT_VERIFIED until independent observation. |
| Deterministic app runtime | partial | app.entities/entity.propose is wired from Talk through the durable attempt journal. Exact validated input is stored as encrypted WORKFLOW_INPUT evidence and can be restored only after job/step/source/hash checks. It saves source-linked PROPOSED entities and requires an independent read-only Work postcondition before checkpoint completion. Exact-head CI #767 passed; wider app handlers and a generic dispatcher remain open. Physical-device acceptance is pending. |
| Local-model Work dispatch | partial | Local-model checkpoint can be reserved, dispatched and saved as a review-required artifact. Runtime output receipt proves saved bytes only, not goal completion. |
| Live | partial | Newest-first projection includes persisted activity, jobs/checkpoints, tasks, action audit, evidence and attempt state. Cross-runtime panes, pause/resume/takeover and verified postconditions remain incomplete. |
| Studio | partial | Private typed draft creation/edit/export foundation exists; richer revision comparison, restore and templates remain open. |
| Browser | bounded slice | Visible provider sessions, exact-content approval, capture/retry controls and durable response acknowledgement exist in source. Live provider/device acceptance remains open. |
| Native Android | bounded | Accessibility automation is opt-in, permission-aware and foreground/unlocked; no generic DAG adapter is wired. |
| Local/frontier AI | partial | LiteRT local model path and optional API-compatible provider exist. Council is not yet one unified Talk/Live workflow across visible signed-in provider sessions. |
| Social platform | specified, not implemented | Account management, feed, inbox, engagement, relationships, listening, publishing, analytics, personal brand and monetization remain in scope. Provider support must be verified individually. |
| Security and storage | partial | Exact-content approval, audit, encrypted workspace/native stores and upgrade safeguards exist. Stores remain separate; single-store migration is deferred. |
| CI / Android build | verified on exact head | Run #767 passed source checks, remote runner, core/app unit tests, lint, debug and Android-test APK assembly/identity, and API 35 in-place encrypted-store upgrade/data-preservation tests. PR validation intentionally skipped pinned signing and final APK artifact handoff. |
| Physical device | pending | User installed the earlier #674 APK but has not checked it. No physical-device behavior or upgrade acceptance is claimed for #678. |

## 3. Current branch and delivery state

- Repository: `ysfclc/memo`.
- Development branch: `stabilization/usable-stage-2026-11-10`.
- Draft PR #8 remains open and unmerged, targeting `main`: https://github.com/ysfclc/memo/pull/8
- Exact tested source commit for M3.8.A10: `989ac28fb6b0da3e5fc66525b1f7b3aff36329df`. `main` was not modified.
- GitHub Actions run #767, ID `43816799963`: https://github.com/ysfclc/memo/actions/runs/43816799963 — completed successfully.
- Passed: source invariants and checksum verification; remote-runner tests; `:core:test`; `:app:testDebugUnitTest`; `:app:lintDebug`; `:app:assembleDebug`; `:app:assembleDebugAndroidTest`; APK package/version/signature checks; API 35 in-place upgrade instrumentation proving app-private data and both encrypted Memo stores survive `adb install -r`, including legacy recovery and corruption tests.
- This was a pull-request validation run. The APK was assembled and emulator-tested, but pinned signing and final APK artifact handoff are intentionally skipped for PR events; no new signed installable APK is available from run #767.
- APK: package `app.memo.android.debug`, versionCode `678`, versionName `4.0.0-memo-debug`.
- Final signed artifact: [Memo-Android-debug-APK, artifact 14143185526](https://github.com/ysfclc/memo/actions/runs/43666486553/artifacts/14143185526).
- APK SHA-256: `026a9be7743e1e91cbc3b7807bed19504db6ea93b3d9249c91046ea15100f469`.
- Artifact ZIP SHA-256: `cbc98602361f95d7383c2f7ccf6bdf92c62347c1fa1ce32435b96e9693ea8e6e`.
- Pinned signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- API 35 upgrade test passed: baseline stores seeded, new APK installed over the existing package, both encrypted stores reopened, legacy recovery and corruption checks passed. No uninstall or clear-data step was used.
- The user installed the previous #674 APK but has not tested it. This is explicitly not physical-device acceptance. The #678 build is a core-contract increment with no new visible workflow; do not ask the user to reinstall merely to see no UI change.

No merge, production release/deployment, signing identity change or destructive data migration is authorized.

## 4. Development authorization and guardrails

On 15 November the user explicitly asked to resume building Memo one coherent increment at a time while preserving the original brainstorming and all prior requirements. This supersedes the earlier feature freeze only for reversible development on the stabilization branch.

It does not authorize merging PR #8, modifying `main`, production deployment, changing signing identity, destructive migration or silently narrowing scope. Work directly in GitHub. Keep each increment bounded, preserve all earlier bugs/tasks, verify the exact commit, and do not start the next increment until the current one passes its required gates.

## 5. Product scope that must remain intact

- Talk: multi-chat, text/voice/URL/file/image/share intake, context selection, local/frontier routing and Council with distinct participants, evidence and disagreement.
- Live: one chronological execution view across runtimes, visible status, pause/resume/cancel/takeover, approval checkpoints and NEEDS_REVIEW for uncertain side effects.
- Work: encrypted, searchable, typed graph for projects, goals, tasks, people, companies, relationships, jobs, applications, notes, sources, evidence, decisions, workflows, capabilities, providers and artifacts.
- Studio: private, editable, versioned, evidence-grounded deliverables, including career documents, research, plans, proposals and content. External submission/publishing is a separate approved action.
- General workflows: brainstorm-to-project, CRM capture, job search/application, content creation/publishing/learning, arbitrary user goals, social operations and Sales Operations. These are not separate silo products.
- Social platform: connect/revoke accounts; provider-aware feed and inbox; supported messages/comments/reactions; relationship management; listening/discovery; publishing; analytics; personal brand; customer acquisition and monetization. Never invent provider support or metrics.
- Intelligence: local-first encrypted source of truth; local models coordinate where supported; frontier providers are visible and consent-bound, never silent fallbacks.
- Safety: deterministic code owns validation, budgets, approvals, state transitions, idempotency and verification. Model output is not proof. Consequential actions require exact content/destination/scope approval; uncertain effects are not blindly replayed.
- Device/design: Android-first, English/Polish/Turkish, coherent premium mobile UX, useful motion/haptics and accessibility. Preserve package/signing identity and encrypted data across updates.

## 6. Latest engineering increment — M3.8.A10 deterministic app-runtime proposal

The first deterministic app capability, app.entities/entity.propose, is connected to Talk's source-grounded outcome extraction. The adapter is bound to the exact TaskSpec/runtime/capability and persisted attempt; it returns bounded candidate output only. The exact validated input is encrypted as WORKFLOW_INPUT evidence with job, step, source identities and SHA-256 binding, and `restorePrepared` fails closed if it is missing or altered. The coordinator writes PROPOSED entities with source-message provenance, a PROPOSES relationship and a content-bound output receipt. A separate read-only app.jobs observer verifies the persisted records and writes separate verification evidence before the checkpoint completes. Proposals remain subject to human review. Exact-head CI #767 passed all automated gates including API 35 install-over-existing preservation. Physical-device acceptance remains pending. PR #8 remains draft/unmerged; `main` untouched. No browser/native/external dispatch, merge, release or data migration is authorized.



- Source commit: `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34`.
- Added `WorkflowRuntimeRequest` and `WorkflowRuntimeAdapterResolver`: validate saved graph, exact TaskSpec runtime/capabilities, capability-manifest fingerprint and current registry, active attempt identity, bounded input/output, deadline and cancellation.
- Approval-gated, external-write and external-side-effect tasks are rejected at this adapter boundary. Adapters return candidate output; they cannot persist workspace state, grant approvals or mark checkpoints complete.
- Added deterministic postcondition method/observer contracts. Observer must be registered, read-only, available and independent of the capability that produced the output. Evidence binds the exact attempt, TaskSpec fingerprint, declared postcondition, observer identity, separate evidence ID/hash and observation time.
- Only `VERIFIED` postcondition evidence permits checkpoint completion. Model assertions and runtime success messages are not accepted as proof.
- Added pure-core tests for exact manifest/runtime/attempt binding, registry drift, approval bypass, adapter resolution, bounds/cancellation, saved-output receipt binding, independent observer requirements and non-verified outcomes.
- Exact-head CI #678 passed all source, unit-test, lint, build, emulator-upgrade and signing gates.
- This is a contract foundation, not a connected runtime executor. No concrete deterministic/browser/native adapter or postcondition observer has been wired yet.

## 7. Known gaps and active defects

- General DAG execution remains incomplete. The first deterministic app.entities/entity.propose vertical slice now uses the validated runtime request, journal, output receipt and independent observer; no generic dispatcher or broad set of deterministic app handlers exists yet.
- Broader deterministic app handlers, a generic dispatcher, browser adapter and native Android adapter remain to be implemented and integrated.
- Shared workflow-wide step/time/model-call/external-write budgets, cross-runtime cancellation, pause/resume/takeover, process-death recovery through the generic adapter boundary and independently persisted postcondition evidence remain open.
- Workflow runtime receipts remain `NOT_VERIFIED` until an independent observer proves the declared condition.
- Workspace vault and native workflow store remain separate. Single-store consolidation must wait for a tested migration preserving both stores.
- Physical-device acceptance, including the user's existing installation and the reported stale/data-loss history, remains open.
- WF-01 through WF-13, STAB-05 and the wider M0–M7 roadmap remain active unless individually closed with exact evidence. Discovery of a new bug does not defer or delete an older one.
- Full social platform, Council, broader CRM/job workflows, device-wide model discovery, local GGUF/llama.cpp and rich Studio lifecycle remain product scope, not silently deferred.

## 8. Verification evidence

- Exact source commit for M3.8.A10: `989ac28fb6b0da3e5fc66525b1f7b3aff36329df`.
- Actions run #767: https://github.com/ysfclc/memo/actions/runs/43816799963.
- Source verifier reports the deterministic Talk outcome proposal adapter, durable journal and independent read-after-write verification guards passed.
- Gradle: `BUILD SUCCESSFUL`; `:core:test`, `:app:testDebugUnitTest`, lint, debug APK and Android test APK assembly passed.
- Emulator: `PASS: signed in-place upgrade preserved app-private data and both real encrypted Memo stores; legacy recovery and corruption tests passed.`
- APK package/version/signature checks passed. Pinned signing/final APK artifact handoff were skipped because this was a PR validation event.
- Physical device: not tested by the user yet.

## 9. Next three increments

1. **Completed — ORCH-007:** shared runtime adapter contract and deterministic postcondition evidence interface, exact-head CI verified.
2. **Automated acceptance passed — M3.8.A10:** app.entities/entity.propose is wired into Talk, saves review-only records through the attempt journal, persists recoverable encrypted input and requires independent read-after-write verification. Exact-head CI #767 passed core/app tests, lint, APK identity and API 35 install-over-existing encrypted-store preservation. Physical-device acceptance remains pending.
3. **Next — expand the safe deterministic runtime boundary:** introduce one or two additional Memo-owned capability handlers with the same output-only adapter, durable receipt and independent observer discipline. Only later should browser/native adapters be connected under their existing approval and permission boundaries.

## 10. User-facing workflow and control audit

- WF-04 source integration now enters `SAVING_RESPONSE` after detecting or manually receiving a provider answer. The workflow waits for the durable encrypted response-capture acknowledgement before advancing. A failed save returns to user recovery without resending the prompt.
- WF-04 source verifier, coordinator tests, core/app tests and exact-head CI passed in the #674 source run; this does not close WF-04 because live provider behavior and physical-device acceptance remain open.
- WF-01 through WF-03 remain open for manual capture/retry/skip/recovery acceptance and device reproduction. The user's report of weak after-send controls is not claimed as reproduced on-device.
- Council still does not automatically reuse browser-authenticated frontier-provider sessions; browser workflows remain a separate visible route/runtime.
- Saved workflow definitions are not equivalent to a complete cross-runtime execution history. Live still needs runtime-backed pause/resume/takeover and independent postcondition evidence.
- Do not claim the end-to-end prompt → enhancement → approval → browser → captured-result journey accepted until the user-facing device walkthrough passes.

