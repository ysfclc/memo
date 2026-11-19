# State Ledger
_Last updated: 2026-11-19 — exact-head Actions run #928 (attempt 2) passed for commit `7ad0eb0426f953a8946741c59247b081a258f94a`; signed APK artifact `14565824727` produced; API 35 encrypted-store in-place upgrade passed. Physical-device acceptance remains pending._

## 0. Latest exact-head build and data-preservation evidence — 19 November 2026

- Exact branch head tested: `7ad0eb0426f953a8946741c59247b081a258f94a` on `stabilization/usable-stage-2026-11-10`; PR #8 remains draft/open and unmerged; `main` untouched.
- GitHub Actions [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135): all jobs succeeded, including the isolated pinned-certificate signing job.
- Source verifier: PASS; static UI translation coverage 434/434 (532 literals scanned). Remote runner: PASS. Core tests 188/188; app tests 44/44; lint 0 errors, 42 warnings, 5 hints; debug and Android-test APK assembly: PASS.
- API 35 signed install-over-existing: PASS. App-private data and both real encrypted Memo stores survived; legacy recovery and corruption checks passed. No uninstall or clear-data step occurred.
- Signed APK: `app.memo.android.debug`, versionCode `928`, versionName `4.0.0-memo-debug`; artifact [Memo-Android-debug-APK, ID 14565824727](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727). APK SHA-256: `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`. Artifact ZIP SHA-256: `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`. Pinned certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- Source fingerprint: `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`; build reports artifact [14566587233](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14566587233).
- **Boundary:** automated CI/emulator/signing acceptance is complete for this exact head. The user's physical phone has not been touched or tested. Physical-device upgrade, visual/touch/navigation and real provider-session walkthrough remain open. Do not uninstall Memo, clear app data, merge PR #8, or release without explicit authorization.

## 1. Product snapshot

Memo is the personal AI operating agent on the phone: it understands what the user wants, assembles and runs the right workflow across the user's information, models, websites and Android apps, produces useful work, and keeps the user in control.

The normative product source is `docs/MEMO_PRODUCT_CONTRACT.md`; every user correction, requirement, bug report, feedback item, idea and use-case boundary is preserved in `docs/USER_INPUT_LEDGER.md`. The social operating-layer requirements are in `docs/SOCIAL_PLATFORM_PRODUCT_SPEC.md`. `TASKS.md` is the execution ledger; `docs/DECISION_LOG.md` records material decisions. `AGENTS.md` and `DEVELOPMENT_GOVERNANCE.md` enforce one authority hierarchy.

A historical baseline of recovered user feedback on prompt feeding, workflows, browser automation, native Android app automation, Council and recovery is recorded in [MEMO_REORIENTATION_AND_FEEDBACK_BASELINE_2026-11-17.md](MEMO_REORIENTATION_AND_FEEDBACK_BASELINE_2026-11-17.md). It supplements rather than replaces the Product Contract and append-only user-input ledger. It is not a complete input record, and later explicit corrections take precedence.

One candidate operating path is: capture intent → clarify and retrieve permitted context → assemble a typed, versioned workflow → route steps to suitable deterministic/AI/runtime capabilities → show meaningful progress and checkpoints → verify actual postconditions → save evidence and deliverables in Work/Studio. This is an implementation model to validate, not a more detailed user-locked sequence. Conversation is the interface, not the database.

Primary surfaces remain Talk, Live, Work and Studio. Work is the local-first persistent graph; Today prioritizes MUST / SHOULD / COULD / NOT TODAY. Memo is not a chatbot skin, generic browser, CRM clone, static task manager or dashboard of disconnected modules. Client discovery is one optional acceptance scenario, not an outbound product or the product's defining direction; Sales Operations remains an optional domain scenario pack.

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
| CI / Android build | prior exact-head verified; current head unverified | `BUILD_STATUS.md` records successful run #821 on exact source commit `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0` (178 core tests, 43 app tests, lint, APK assembly, API 35 data-preservation upgrade and pinned signing). This predates the branch head inspected for this governance pass (`a604a553ce2f77cb5fbb05cd4bc38479835fcdb5`) and all subsequent edits. The newer exact head is not verified by that run. |
| Physical device | pending | The user has not accepted the latest #821 build on the phone. Emulator/data-preservation evidence is not physical-device UX acceptance. Do not uninstall Memo or clear app data. |

## 3. Current branch and delivery state

- Repository: `ysfclc/memo`.
- Development branch: `stabilization/usable-stage-2026-11-10`.
- Draft PR #8 remains open and unmerged, targeting `main`: https://github.com/ysfclc/memo/pull/8
- Latest exact Android-verified source commit before this governance pass: `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0` (run #821). The branch head inspected at the start of the product-integrity audit was `a604a553ce2f77cb5fbb05cd4bc38479835fcdb5`; all subsequent governance, product-doc, architecture, prompt and source-verifier changes require exact-head validation. `main` was not modified.
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
- General workflows: brainstorm-to-project, CRM capture, job search/application, content creation/publishing/learning, arbitrary user goals and social operations. These are examples/domains supported by the same general platform, not separate silo products. Sales Operations/client discovery is an optional scenario pack, not a core product track or authorization for outbound automation.
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



## 11. Latest implementation and verified APK — 17 November 2026

- **Current source commit:** `090a2de99dacbc65cfcc0351be9333bbf00aa182`; branch `stabilization/usable-stage-2026-11-10`; PR #8 remains draft and unmerged.
- **UI cleanup:** existing `MemoPanel`, `MemoSectionHeading` and `MemoStatusTag` are now the shared primitives for the migrated screens. Duplicate panel/heading implementations were deleted; unique status and empty-state helpers remain.
- **Prompt flow:** original intent stays visible beside an editable provider prompt. Revision rejects the previous pending ledger record and creates a new exact hash and approval. Editing does not send. Reject, provider skip and whole-run cancel are distinct actions. Prompt UI strings are translated in English, Polish and Turkish.
- **Automated acceptance:** [Actions run #802](https://github.com/ysfclc/memo/actions/runs/44016406556) passed source verification, localization checks, remote-runner tests, core/app JVM tests, lint, APK assembly, APK identity/signature, API 35 encrypted-store in-place upgrade and isolated pinned signing.
- **APK:** `app.memo.android.debug`, versionCode `802`, versionName `4.0.0-memo-debug`; artifact [14298479094](https://github.com/ysfclc/memo/actions/runs/44016406556/artifacts/14298479094). SHA-256 `69345f8b680feb0b7cbb59d83161561693e3f1d3f079fe1d52c288c82b224db3`; pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Data preservation:** API 35 emulator passed install-over-existing and verified app-private data, both real encrypted stores, legacy recovery and corruption tests. This did not use or modify the user's physical phone.
- **Still open:** physical-device UX and provider-session walkthrough; browser task-context integration; unified workflow execution across browser/native runtimes; the broader automation roadmap. WF-01–WF-13 are not closed by source/CI alone.


## 17 November 2026 — shared runtime dispatch boundary (source committed; CI pending)

- Added a shared dispatcher over the existing exact TaskSpec, capability-manifest and persisted-attempt request.
- The first deterministic app capability now invokes through the dispatcher. The Android host applies the remaining original attempt deadline; the dispatcher checks cancellation/deadline before and after execution and validates the returned candidate.
- The dispatcher does not persist workspace records, approve actions or verify postconditions. Existing output persistence and independent observer remain separate.
- No generic DAG executor, browser/native adapter, physical-device acceptance, merge or release is claimed.

## 12. UI/UX increment — 17 November 2026

- The four product surfaces remain the only primary navigation destinations.
- Specialist destinations retain parent context and provide direct sibling navigation. Browser, Phone apps and Approval inbox belong to Live; Council belongs to Talk; Models and Settings are system-level routes that return to the originating surface.
- The More landing page is removed from the active shell. The header menu is reserved for Models and Settings.
- Stop is contextual to observable active work rather than permanently competing with Lock in the header.
- Live runtime entry points are grouped into one horizontal action row.
- Source/device acceptance status: exact-head CI and APK update tests pending; physical-device visual/touch/back-navigation acceptance remains pending.

## 13. UIX-05 — Work and Live responsibility boundary (18 November 2026; exact-head CI and signed APK passed)

- Work exposes browser workflows, browser tasks and the evidence library as execution tools alongside Today, Plans and the work graph. These routes reuse the existing browser/task/evidence implementations; no runtime or data model was replaced.
- Live retains the live browser session, phone apps and the external-action approval inbox. Opening active browser workflow controls enters the Work-owned workflow route, while ordinary browsing remains a Live route.
- Browser session back navigation returns to Live; browser workflows, browser tasks and evidence return to Work. Android Back, the visible parent route strip, primary-tab selection and Models/Settings return context follow the same ownership rule.
- Work subroutes have distinct headings and descriptions. Their section switcher no longer presents ordinary Browser session as if it were a Work tool.
- Shared page headers/section headings use the language-aware text wrapper; route labels/descriptions have English, Polish and Turkish catalog coverage.
- Existing tasks, evidence, approvals, browser session, exact-content approvals, recovery controls, encrypted storage, app ID and package/signing identity are preserved.
- **Verified:** Actions #821 exact-source build, 432/432 static UI translation coverage, remote-runner tests, 178 core tests, 43 app tests, lint with zero errors, debug APK assembly, persistent pinned signing and API 35 install-over-existing preservation of both encrypted stores.
- **Pending:** physical-device visual, touch and Back-navigation acceptance on the user's phone. Do not uninstall Memo or clear app data. PR #8 remains draft/unmerged; main is untouched.

## 14. Permanent user-input governance and scope correction — 19 November 2026

- `AGENTS.md` is the mandatory instruction file for agents and contributors; `docs/USER_INPUT_LEDGER.md` is the append-only source for user corrections, requirements, feedback, bug reports, ideas and use-case boundaries.
- The user explicitly corrected the client-finding example: **Memo is not an outbound product; client finding is only a use case.** It must not define architecture, product identity, global classification or default roadmap priority.
- Sales Operations documentation is preserved as an optional domain scenario. It is not core Memo architecture, and channel-send/outbound tasks are not authorized by that scenario.
- Ideas remain uncommitted until explicitly adopted. Bug reports and feedback remain active until evidence-backed closure or explicit user closure. Corrections override assistant interpretations; conflicts with prior explicit user decisions must be surfaced rather than silently resolved.
- Earlier UWF-01–UWF-08 and WF-01–WF-13 feedback remains active according to its existing acceptance evidence; this governance change does not close or postpone it.
- **Validation boundary at audit start:** PR #8 head was `a604a553ce2f77cb5fbb05cd4bc38479835fcdb5`. The queried PR-triggered Android workflow lookup returned no run for that head and combined status showed only a failed Vercel check. Run #821 is mapped to exact source commit `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0` and remains valid evidence for that commit only. The inspected branch head and subsequent governance changes are not verified by run #821. This governance-only work must not be described as Android-build or device verified.

## 15. Product-integrity audit — 19 November 2026

- Audit findings and durable remediations are recorded in [PRODUCT_INTEGRITY_AUDIT_2026-11-19.md](PRODUCT_INTEGRITY_AUDIT_2026-11-19.md). It is an audit record, not product authority.
- Mandatory agent instructions and the append-only user-input ledger now govern new corrections, requirements, reported bugs, feedback, uncommitted ideas and use-case boundaries.
- Conflicting authority language, historical Memo 2 blueprint language, Sales Operations priority drift, and over-attribution of the proposed Classify/router architecture were corrected across the Product Contract, governance, implementation contract, task/roadmap/gap records, README, architecture map and dated feedback baseline.
- Fixed keyword routing is explicitly legacy/prototype. Talk entry no longer imports the legacy classifier; Work compatibility helpers remain clearly labelled and bounded by separate validators.
- CI source verification now checks governance markers, optional-use-case boundaries, legacy-router labels, and append-only preservation of prior user-input/decision text. Governance document changes trigger the source/build workflow.
- Latest branch head observed at handoff before this State Ledger update: `aab9917e600944228a07e6f9726497e72777de95` (PR #8, draft/open, base `main`). No exact-head Android Actions success evidence was available in this audit. The combined status exposed only a failed Vercel check due to build-rate limit; this is not an Android build result. Run #821 is a verified Android build for SHA `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0` only. The audit changes after that exact commit have not passed exact-head CI. Physical-device acceptance remains pending. Do not claim the new verifier or newer branch head is build-verified until a run for the final head is recorded.

## 16. Current stabilization verification — 19 November 2026

- Exact branch: `stabilization/usable-stage-2026-11-10`; current tested head `f62540cda08dc658309de16e037619201707f43a`. This documentation-only handoff commit follows source commit `7ad0eb0426f953a8946741c59247b081a258f94a`.
- Exact-head Actions run [#929](https://github.com/ysfclc/memo/actions/runs/44599220097) passed: 434/434 static UI Text translations, remote-runner tests, 188 core tests, 44 app tests, lint with 0 errors (42 warnings, 5 hints), APK/test-APK assembly, API 35 in-place encrypted-store preservation and isolated pinned signing.
- Signed APK: versionCode 929, package `app.memo.android.debug`; [artifact 14567058852](https://github.com/ysfclc/memo/actions/runs/44599220097/artifacts/14567058852); APK SHA-256 `9b60929f720ad8fade98dd23c03441dd0199b5c377994883d9cbe66e60635264`; artifact ZIP SHA-256 `8c166e2aae7576a5ee969fae5c7f3295c48aeb2a5a992bf5b411e827f20e77ad`; pinned signing certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- Emulator upgrade acceptance is not physical-device acceptance. The user's phone, installed app and private data have not been accessed in this workspace. Physical-device in-place upgrade, visual/touch/navigation testing and signed-in provider workflow remain pending.
- Keep STAB-05, STAB-06 and applicable WF device/provider acceptance open. Do not uninstall or clear data. PR #8 remains draft/unmerged; `main` is untouched; no release/merge/destructive action is authorized.

## 17. New browser automation and approval usability reports — 19 November 2026

The user tested browser automation and reported that it is hardcoded around Frontier providers; selecting providers with accounts already signed in did not open the browser as expected. The user also cannot find the approval button and reports confusing checkpoints and repeated approval requests even when there is nothing apparent to approve. These are preserved as active records INPUT-2026-11-19-004 through -006 in the append-only user-input ledger.

**Repository findings from source inspection (not device reproduction):**
- BrowserWorkflowScreen is explicitly a "Frontier workflows" screen. It sources its picker from the fixed BrowserModelProviders.all list; BrowserSession.startBrowserWorkflow rejects unknown provider IDs and opens configured provider startUrl values. This confirms a Frontier-specific workflow slice, not a general-purpose browser automation runtime.
- The selected-provider code does call navigate(provider.startUrl) and waits for WebView callbacks before preparing the prompt. The user's "no browser happened" symptom is not yet reproduced; investigate the actual entry route, local-model stage, navigation callback, WebView load/error state and session identity before assigning a cause. Chrome sign-in cookies must not be assumed to exist in Memo's embedded WebView.
- "Approve and send" is currently a conditional AlertDialog action shown only while awaitingPromptApproval is true. The Action Approval Inbox lists pending actions but does not expose an approve callback; this is a discoverability gap in the current UI contract.
- Workflow checkpoint state and external-action authorization are separate mechanisms, but the current UI/status wording makes them easy to confuse. Audit all producers of WAITING_FOR_USER, NEEDS_REVIEW and pending approval records; do not treat every checkpoint as an approval.

**Active next tasks:** BRW-14 general-purpose target resolution; BRW-15 selected-target navigation regression; APR-01 visible approval surface; CHK-01 checkpoint/approval semantics. Existing WF-01–WF-13 remain active. Physical-device reproduction and acceptance are still pending; no claim of device fix is made.


## 18. Browser workflow visibility and approval usability implementation — 19 November 2026

- **Source diagnosis:** the workflow called BrowserSession navigation while its Android WebView was mounted only in the dedicated Browser screen. The workflow did not route to that screen, so the user could see no browser even though navigation had been requested. The approval inbox's previous action routed to the ordinary browser rather than the workflow confirmation dialog.
- **Implemented on stabilization branch:** workflow start now switches to the dedicated Browser screen; the browser has a prominent "Review prompt & approve" return action when exact approval is waiting; approving returns to the visible browser for execution. The inbox uses the same review label and routes to the live workflow dialog. Active external-action identity is held by BrowserSession across screen recreation.
- **Approval and checkpoint behavior:** local intent structuring is opt-in by default. The duplicate broad preflight consent checkbox was removed because the exact per-provider prompt dialog already displays the destination and full content and still requires a one-use explicit approval before sending. Manual sign-in/page-setup waits no longer become NEEDS_REVIEW approval checkpoints; exact approval, sent and uncertain actions remain review-gated. Paused Work jobs say "Needs your input".
- **Still open:** BRW-14 general-purpose browser target resolution; physical-device reproduction and provider-session validation for BRW-15; direct inbox approval only if it can share the same canonical execution gate for APR-01; broader cross-runtime checkpoint audit for CHK-01.
- **Verification boundary:** source and docs are committed on the stabilization branch; exact-head CI, APK assembly/signing and device acceptance are pending. No claim that the phone or real provider sessions have been tested. Do not uninstall or clear Memo data. PR #8 remains draft/open; main remains untouched.


## 19 November 2026 — Final source state queued for exact-head validation

- Browser visibility, approval re-entry, shared action identity and checkpoint-friction changes are complete in source. The integrity manifest and this state update are committed atomically so CI validates one consistent final revision.
- Exact-head CI is pending. No signed APK, physical-device or real provider-session acceptance is claimed yet.


## 19 November 2026 — Approval capability boundary correction

- Safe browser prompt preparation and multi-chat orchestration no longer require blanket approvals. Only browser.submit_prompt is classified as an external side effect requiring exact approval. CapabilityCompiler now binds external browser approval to submission rather than preparation; unit tests and source invariants cover this.
- General read-only BrowserTask capabilities remain site-independent for supported HTTPS targets. Universal DOM/action execution and dynamic target resolution remain open under BRW-14.
- Exact-head CI remains pending after these latest changes; no APK or device acceptance is claimed.
