# Memo 1 — usable-stage stabilization checkpoint (10 November 2026)

## Scope and source identity

- Canonical implementation: `ysfclc/memo`.
- Blueprint only: `ysfclc/memo-2`. This stabilization does not modify Memo 2.
- Baseline: `main` at `b31554726538b32564eee428aeed047bfa61aaa8`.
- Stabilization branch: `stabilization/usable-stage-2026-11-10`.
- The product remains Memo as a broad personal execution environment. Talk, Live, Work and Studio are primary surfaces; sales/account research is an optional Work capability, not the product definition.

## Why a fresh APK is required

The last recorded successful APK is from 8 November:
- Source commit: `c4cfba812362fd5f69cc72026a935fe77a4d6dfc`.
- Workflow run: [#223](https://github.com/ysfclc/memo/actions/runs/42530596381).
- Package: `app.memo.android.debug`, version code `223`.
- APK SHA-256: `670acd9192f4b02e7aea1c23e456036b2ad3a98f5d48d57ef7c895a72e8d22b2`.
- Signing certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.

Current main is 188 commits ahead of that APK. The intervening changes include the Signal & Graphite design foundation, rebuilt Memo theme and primary navigation, Talk/Live/Work/Studio integration, Today priority changes, native automation policy hardening, Talk draft-preservation behavior, and the optional industrial account research slice. The old APK does not contain this complete integration and must not be presented as the current build.

## CI investigation

- Latest mainline source commit: `b31554726538b32564eee428aeed047bfa61aaa8`.
- GitHub Actions run #243: [run 42853164772](https://github.com/ysfclc/memo/actions/runs/42853164772).
- First attempt failed in the preflight job in approximately two seconds. GitHub returned an empty step list and no job logs; the build, signing and commit jobs were skipped.
- The failed job was retried. Attempt 2 also failed before exposing any steps or logs. This is not evidence of a Kotlin/Gradle failure; the runner/preflight failure remains undiagnosed.
- **CircleCI is not required and will not be used for this stabilization.** Its checked-in config is an optional legacy path; do not ask the user to enable it or configure CircleCI variables.
- Use the existing GitHub Actions workflow as the preferred validation path. If its preflight remains unavailable, keep implementing source-level work and report that build/device verification is blocked; do not weaken the gates or invent a successful APK.

## Stabilization gates — all still required

1. Run the current exact source through the PR build workflow: source verifier, remote-runner tests, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, debug APK assembly, Android 35 emulator instrumentation, and install-over-existing encrypted-store/private-data checks.
2. Run the main-branch isolated signing job and verify the pinned certificate fingerprint.
3. Record the APK artifact URL, package/version, source commit, APK SHA-256 and certificate SHA-256.
4. Complete the physical-phone install-over-existing check and confirm existing Memo chats, vault data, workflow records and app-private data remain intact.
5. Update `BUILD_STATUS.md`, `IMPLEMENTATION_LOG.md` and `DEVICE_ACCEPTANCE_CHECKLIST.md` only with evidence from the exact tested commit. A successful build is not full product completion.

## Honest current status

No app source, storage schema, package identity, signing configuration or runtime behavior is changed by this checkpoint. No current-head APK has been built or device-tested yet. The current UI integration is present in source, but its compile/runtime stability is unverified until the gates above pass. Do not merge unverified app changes or distribute the 8 November APK as if it contains the 10 November integration.


## Source hardening started on the stabilization branch

- `core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt`: new external-action proposals now require nonblank saved job and step IDs and reject overlong identities instead of silently truncating them. Completion verification also refuses to mark an action complete when its job/step binding is absent; the result is routed to `NEEDS_REVIEW`.
- `core/src/test/kotlin/app/memo/android/data/ActionApprovalLedgerTest.kt`: added regression coverage for missing job/step IDs and for an unassigned evidence record being used to claim completion.
- Commits: `a51337c61f7edc47d43929c6441eadbb3d920abd` and `027778102ceb8958d4b7573c5296d3ac60961a82`.
- The PR workflow for the latest test commit again failed in its first preflight job with no exposed steps or logs. The Android/core test jobs were skipped. These tests are added but **not yet executed**; they are not recorded as passing.
- The PR now contains three commits and three changed files. The source change is intentionally limited to the external-action approval boundary.


### Additional orchestration correction

- `core/src/main/kotlin/app/memo/android/orchestration/WorkflowGraph.kt`: planned capability availability is now checked as a `(runtimeId, capabilityId)` pair rather than by capability name alone. Build, career, content and capture proposals no longer assign chat/browser capabilities to the model or entity runtime. The artifact step only receives `model.generate` on `app.models`; job verification only receives `job.verify` on `app.jobs`. If any capability required by the domain step is unavailable, the graph degrades to an explicit user checkpoint instead of presenting the unavailable runtime as ready.
- `core/src/test/kotlin/app/memo/android/orchestration/WorkflowGraphTest.kt`: added runtime-ownership coverage across build/career/content/capture plans and a missing-model-capability fallback test.
- Additional commits: `c1b03a16c6d6828f8ddb2c90772a50f18fb0b828`, `a7c5a2ee28c669dbd0a4b20ea5b45a6cfba74152`, `c9be42a96d95c8325d2b5ebb0913266f97021be5`, `594c8805da0d7ccee439c020e102c735e7b07e19`.
- The latest GitHub Actions PR run at the latest source checkpoint is `42874396121`; it failed in the same preflight job before any steps were exposed.
- These are source-level fixes and regression tests. Until a runner executes the test suite, they remain unverified by execution.


### GitHub runner startup diagnosis

- A separate minimal diagnostic workflow (one `ubuntu-latest` job with a single shell step and no checkout or third-party actions) was briefly added and triggered on the PR.
- Its run `42873553407` failed in the same way as the Android workflow: the job has no recorded steps, no runner logs URL and no downloadable log blob. The diagnostic file was removed after the test.
- This isolates the blocker to GitHub-hosted job startup for this repository/account, before the Memo workflow logic, Gradle, Android SDK or source code executes. The precise account/runner reason is not exposed by the available GitHub API response, so it remains unknown.
- This is not a reason to switch to CircleCI. Continue Memo source development; keep tests committed and mark them unexecuted until a runner is available.


### Job/step existence is now enforced

- The external-action proposal boundary now verifies that the supplied job ID exists in the encrypted workspace and that the step ID belongs to that job. Completion verification rechecks that the linked job and step still exist; otherwise the action moves to `NEEDS_REVIEW`.
- Regression tests now build a real saved job/step fixture and cover unknown jobs and unknown steps, in addition to blank IDs and unassigned evidence.
- Commits: `ee28375c88619d2db138dfcb0f8212193164a158`, `ed491969e94c7c53e1936a94f0cfdc9a937e114b`, `9d60c79e576723cbc6faf1214a44f6bc6f473f8b`.


### Audit integrity for the job-step binding

- New `ACTION_PROPOSED` audit events include a versioned binding marker containing the exact job and step IDs. Because event details are included in the chained event hash, changing either association now fails ledger verification.
- Older proposal events without the marker remain readable for backward compatibility; completion still requires a present job and step.
- Added regression test `auditChainBindsActionToItsOriginalJobAndStep`.
- Commits: `18bed085606bcd73a99f1d36f341f679322c8d71`, `e7c4ff5e551c306bab0446274d6ac30ab231c6c8`.


### Capability manifest routing now uses exact runtime IDs

- `CapabilityCompiler.compile` now declares each requirement as a `(runtimeId, capabilityId)` pair rather than globally searching by capability name. This prevents a duplicate capability name in another runtime from stealing a route.
- The compiler rejects duplicate runtime IDs and duplicate capability IDs within a runtime, and records unresolved requirements with their runtime-qualified identity.
- Added `intentRoutesResolveDuplicateCapabilityNamesOnTheirDeclaredRuntime`, which creates a deliberate name collision and verifies research still routes to the browser runtime.
- Commits: `ccb7c882a354221f9789027481959476e1b6f75c`, `4b4e76106e04c9b259b1446214f9715488dc90ba`, `ddb2e94d6ca4b9cf639a355da06792c67aee62eb`.


### Completion evidence is now step-specific

- External-action completion now requires evidence whose `taskId` matches the saved job **and** whose metadata `stepId` matches the exact approved job step. Evidence from another step in the same job, missing metadata or malformed metadata routes the action to `NEEDS_REVIEW`.
- The visible browser workflow now writes the provider step ID into the captured response evidence metadata.
- Added regression coverage for wrong-step evidence; updated the successful completion fixture with the matching step ID.
- Commits: `750180be86a5e0e3b5d6d73acd3d497b3a0dc44b`, `522099c9cc2e29b93936fff260ba59794e7f3c98`, `f9b155cdb8446d8571176ee67a774791e9ad3bff`.


### Latest checkpoint

- Branch head: `4d419f8ff16b31845d2feb839ad570ef3d0dfe33` (one clean stabilization commit on top of main).
- PR #8 remains draft and targets the unchanged main commit `b31554726538b32564eee428aeed047bfa61aaa8`.
- Latest workflow attempt: `42874816696`; the preflight job again failed without steps or logs. The workflow run had not yet finalized when this checkpoint was written.
- The current branch changes seven files: action approval core + tests, capability compiler + graph + tests, browser response evidence metadata, and this handoff.
- Main has not been modified. Memo 2 has not been modified.
- No APK was produced. All new regression tests are committed but unexecuted because GitHub-hosted runners are not starting.


- The job/step audit binding uses length-prefixed identifiers, so even imported IDs containing separator characters cannot be ambiguously re-split. Regression test: `auditedJobStepBindingCannotBeAmbiguouslySplitOnSeparators`. Commits: `386ee5d8a72dde161b2743b12e04fbf27f6830db`, `e595c29338d7c7138435632fcb1e471dbb7e6e1d`.


## 2026-11-10 — Live timeline implementation increment

This checkpoint supersedes the earlier branch-head/file-count snapshot above.

- **Latest branch head:** `db2a83e971f93d3421f7b7a22a2d1746c3413896`.
- **Product scope:** Memo remains the broad personal execution environment. This increment improves the shared Live surface; it does not narrow Memo to Sales Operations.
- **Core projection:** added `core/src/main/kotlin/app/memo/android/data/LiveTimelineProjector.kt`. It creates a deterministic newest-first projection from persisted workspace activity, jobs, job checkpoints, tasks, action-audit events and captured evidence. Entries retain stable source IDs and timestamps. It is read-only and does not infer that an external side effect succeeded.
- **Compatibility:** action records imported without audit events remain visible once using their latest known lifecycle timestamp. When audit events exist, the projection uses those transitions and does not add a duplicate fallback action row. Raw audit details/payloads are not rendered.
- **UI:** `LiveWorkspaceScreen.kt` now renders one chronological workspace timeline. The current visible browser session remains a separate panel; native runtime panes, cross-runtime event journaling, runtime-backed pause/resume/takeover and global durable cancellation remain open.
- **Regression tests added:** `LiveTimelineProjectorTest.kt` covers newest-first cross-record ordering, exact action/audit linkage, raw audit-detail non-disclosure, legacy action fallback without duplication, bounded result count and invalid limits.
- **Localization/source checks:** added English/Polish/Turkish catalog entries and structural checks for the projection, UI integration and regression cases in `tools/verify_source.py`.
- **Documentation updated:** `TASKS.md`, `FEATURE_AUDIT.md`, `PRODUCT_GAP_REGISTRY.md` (see remaining cross-runtime gaps), `docs/MEMO_PRODUCT_CONTRACT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `docs/product/MEMO_UX_RELIABILITY_AUDIT.md`, and `docs/product/MEMO_DESIGN_SYSTEM.md`.
- **Validation status:** tests are written but **not executed**. The GitHub-hosted runner startup issue remains the known blocker; no Gradle test, Android compile, lint, APK build or device check is claimed. No new APK exists.
- **Branch safety:** `main` remains unchanged; Memo 2 remains untouched; PR #8 remains the isolated draft review boundary. Do not merge or distribute an old APK as this source increment.


## 2026-11-10 — General Studio private drafts

- **Source increment:** added `core/src/main/kotlin/app/memo/android/data/WorkArtifactFactory.kt`, which validates title/type/content bounds and creates `WORK_ARTIFACT` evidence records with user-authored provenance and `requiresHumanReview=true`. It does not mark drafts approved or published.
- **UI:** Studio now exposes a general “New draft” action with Note, Plan, Proposal, Research brief and Content draft types. Saved drafts use the existing encrypted workspace/evidence store, appear alongside Self Model-generated artifacts, and open in the existing edit/export dialog.
- **Revision behavior:** existing `updateWorkArtifact` preserves up to five prior revision snapshots when a draft is edited. The current UI does not yet browse, compare or restore those snapshots; autosave and richer document export remain open.
- **Regression tests added:** `WorkArtifactFactoryTest.kt` covers metadata/provenance, review-required state, invalid inputs and storage bounds. Tests are **not executed**.
- **Localization check:** the static `Text(...)` literal scan across all seven UI files used by `tools/verify_source.py` now reports zero missing Polish/Turkish catalog keys. This is a source scan only, not manual language/device acceptance.
- **Source verifier:** required-file and structural checks now cover both the Live timeline projection and the Studio draft factory/UI/test contract. The verifier itself has not run because the hosted runner fails before steps.
- **Latest source checkpoint before this handoff update:** `4a9a2d5361f5cf80ce9387c0a5cd2035988ea431`. Main remains unchanged and PR #8 remains draft.
- **CI:** latest observed PR workflow run on the earlier Live checkpoint `7967238376ea9e23333a3fb4ca5fed5409953c10` was run `42885619158`; its preflight job failed with an empty step list and the remaining jobs were skipped. No test/build/APK result is claimed for the Studio increment.


## Consolidated source checkpoint — Live + Studio + Talk audit

- **Latest source head before this note:** `258b25be90d492d834e566a4550a3cea16afb7c3`.
- Talk's active primary route is confirmed as `ChatsWorkspaceScreen`: one text/voice entry composer, searchable conversation history and active conversation. The UX audit was corrected; attachments and dynamic TaskSpec-to-Talk routing remain the next Talk gaps.
- Studio now has a primary “New draft” action for Note, Plan, Proposal, Research brief and Content draft. Drafts are created by `WorkArtifactFactory`, stored as `WORK_ARTIFACT`, and remain private/review-required. The list includes generic drafts and Self Model artifacts; existing edit/export behavior and revision snapshot storage are reused.
- The product contract, UX audit, design-system status, requirement traceability, feature audit, product-gap registry and TASKS ledger were reconciled to the source changes. No product requirement was marked complete from source presence alone.
- The static translation-key scan over the seven Compose files used by the source verifier returned **0 missing keys** after the additions. This is not a manual language/accessibility/device review.
- The latest observed GitHub Actions run remains `42885619158` on the earlier Live-only checkpoint; preflight failed with zero exposed steps and downstream jobs skipped. The later Studio commits have no recorded successful build/test run in the available results.
- **No Gradle tests, Android compile, lint, APK assembly, signing or device acceptance are claimed.** The newly added Live and Studio core tests remain unexecuted.
- **Main is unchanged; Memo 2 is untouched; PR #8 remains draft.** The current branch must not be merged or distributed as a verified build until the exact head passes the required gates.


## 2026-11-10 — Local-model TaskSpec expansion in Work

- **UI path:** untouched DRAFT plans in Work now expose “Expand with local model” when a local model is installed and the original intent does not contain a detected external-write request. The action is local-model-only; it does not use a remote provider.
- **Proposal prompt:** includes the exact original goal, existing checkpoints and the current runtime/capability catalogue. The exact goal is JSON-quoted before insertion into the prompt. The model is instructed to return a strict schema-v1 TaskSpec proposal and not to create approval/external-write nodes.
- **Parser and policy:** the response is parsed by `WorkflowTaskProposalParser`; capability names are resolved against exact runtime IDs. The applier recompiles against the current registry, requires the exact original goal, rejects optional/unavailable runtimes, approval-runtime/approval-gated tasks and external side effects, and accepts only an untouched DRAFT.
- **Persistence:** accepted nodes are topologically ordered and saved as PENDING job checkpoints with their input/output/postcondition contracts in the step detail and the full schema-v1 graph in `workflowGraphJson`. The job remains DRAFT and the result summary explicitly says no node was executed.
- **Regression tests added:** `WorkflowProposalApplierTest.kt` covers draft-only application, goal preservation, already-started plan rejection, unavailable optional runtime rejection, approval-runtime rejection and external/approval-gated task rejection. Tests remain unexecuted.
- **Remaining boundary:** this is a local-model proposal-expansion step in Work, not Talk-native dynamic planning and not a DAG executor. No node is dispatched automatically; no per-node run journal, pause/resume, cancellation or postcondition evidence protocol is added.

## 2026-11-10 — Talk-native local TaskSpec expansion

- **Talk entry:** the Start with Memo composer now has an explicit opt-in checkbox, shown only when at least one local model is installed: “Expand the plan with a local model”.
- **Privacy:** expansion uses the first installed local model and passes allowRemote=false; the option does not route through a configured remote provider. Remote model participants are excluded from the generated TaskSpec prompt.
- **Flow:** Talk still saves the source conversation and deterministic initial plan first. When the checkbox is selected, the same draft-only proposal pipeline used by Work expands that untouched plan, validates it against the current runtime registry, and persists it only as a DRAFT. The user is routed to Work to review the checkpoints.
- **Work:** the existing “Expand with local model” action remains available for later expansion of an untouched draft.
- **Localization:** both new Talk strings are present in the EN/PL/TR localization catalogue; a conservative static scan across seven Compose screens reports zero missing catalog keys.
- **Validation:** the GitHub Actions run for the source branch still fails in the preflight job before any step is exposed (steps: null, no logs URL); build/test/sign jobs are skipped. This is consistent with the hosted-runner startup problem observed earlier, not evidence of a Kotlin build result. The source verifier, Kotlin tests, lint, APK build, emulator/device install and upgrade/data-preservation checks remain unexecuted.


## Final branch checkpoint — 2026-11-10 23:25 CET

- Stabilization source has been consolidated into **one commit** on `stabilization/usable-stage-2026-11-10`: `59813678d78032a621b0738511cf0f9c3b9cef55`.
- PR #8 remains draft and targets `main`; compare reports 1 commit ahead, 0 behind, 26 changed files.
- `main` remains at `b31554726538b32564eee428aeed047bfa61aaa8`. Memo 2 remains untouched.
- Latest GitHub Actions attempt: run `42916291369`. The preflight job completed as failure with `steps: null` and no logs URL; build, signing and source-write jobs were skipped. This is not a source compilation result.
- No Kotlin tests, source verifier, lint, APK build, emulator install or device/upgrade acceptance have been completed. No APK is available for this head.
- CircleCI is not required or configured as a prerequisite.


## 2026-11-11 — Dependency-aware workflow checkpoint coordination

- Added `WorkflowCheckpointCoordinator` as the shared core transition boundary used by Work's start and outcome-recording actions.
- Graph-backed plans now require an exact match between saved graph nodes and persisted job steps. A checkpoint can start only when all declared dependencies are complete, no other checkpoint is running, every referenced runtime is currently registered/available, and every capability still belongs to the exact runtime.
- The generic Work path rejects workflows containing approval-gated nodes, external side effects or the dedicated approval runtime. Those actions remain in the exact-content approval ledger and cannot be completed with a note.
- User-entered outcomes are stored as `USER_REPORTED` and explicitly described as not independently verified. This does not assert that a browser, Android app, remote provider or external service completed the action.
- Work now exposes dependency-ready branches instead of forcing graph-backed plans into a simple list order. Only one checkpoint can be active at a time.
- Backward compatibility: older jobs without a graph retain sequential checkpoint behavior; legacy external-action shortcuts remain blocked.
- Added regression coverage for dependency gating, deterministic branch progression, active-step exclusion, runtime revalidation, external-effect rejection and legacy-job compatibility. Static source-contract checks were extended.
- **Boundary:** this is durable checkpoint coordination, not automatic runtime execution. Memo still needs the registered runtime dispatcher, attempt/idempotency journal, typed runtime evidence, timeout/cancellation, pause/resume and verified postcondition protocol before it can truthfully claim autonomous execution.
- **Validation:** source files and tests are committed to the stabilization branch. GitHub Actions run `42920165450` again failed in preflight with no exposed steps or logs; downstream build/test/sign jobs were skipped. No Gradle tests, Kotlin compilation, lint, APK assembly, emulator/device acceptance or upgrade test has run on this head. Do not treat the source additions as compiled or release-ready.
