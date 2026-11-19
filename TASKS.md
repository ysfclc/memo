# Memo 1 — Master execution runbook

**Target repository:** `ysfclc/memo` (Product 1)  
**Historical cross-reference only:** `ysfclc/memo-2/docs/TASKS.md`, `docs/BUILD_PLAN.md`, `docs/FEATURES.md`, and `docs/PROGRESS.md`. These can inform comparisons but cannot add requirements, override corrections or define Memo's product direction.  
**Reference TASKS blob observed:** `666fb7a030b71294ba06fe44d2f37839f4ce4430`  
**Rule:** this document is the engineering execution ledger for Memo 1. Accepted scope comes from the user's latest inputs in `docs/USER_INPUT_LEDGER.md` and the user-approved `docs/MEMO_PRODUCT_CONTRACT.md`; Memo 1 remains its own product and codebase.

> **Important status rule:** upstream `[x]` marks from Memo 2 are not completion evidence for Memo 1. Every task below starts unverified in Memo 1 and must pass its own evidence gate before it may be marked `[x]`.

**Agent instructions:** [AGENTS.md](AGENTS.md)  
**User-input ledger:** [docs/USER_INPUT_LEDGER.md](docs/USER_INPUT_LEDGER.md)  
**Delivery governance:** [DEVELOPMENT_GOVERNANCE.md](DEVELOPMENT_GOVERNANCE.md)  
**Stable requirement IDs and acceptance crosswalk:** [docs/REQUIREMENT_TRACEABILITY.md](docs/REQUIREMENT_TRACEABILITY.md)  
**Material decision history:** [docs/DECISION_LOG.md](docs/DECISION_LOG.md)  

Every substantial task must reference requirement IDs and close only with evidence under the governance rules.

## P0 — STOP-WORK stabilization (13 November 2026)

**Sequencing gate:** defer broad scope expansion, redesign, unrelated refactoring, merge, production deployment, signing change and destructive migration until the current app passes the stabilization gates. The user has explicitly authorized bounded, reversible implementation increments from the existing product roadmap on the stabilization branch; this does not relax verification, merge, release or data-preservation gates.

**Execution plan:** [Memo STOP-WORK stabilization plan](docs/handoff/MEMO_STABILIZATION_PLAN_2026-11-13.md).

**Product journey acceptance:** [User-facing workflow and control audit](docs/handoff/MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md). This records the reported prompt → enhancement → approval → browser → captured-result failure as P0/P1 acceptance requirements.

**Ordered implementation plan:** [Workflow recovery execution plan](docs/handoff/MEMO_WORKFLOW_RECOVERY_EXECUTION_PLAN_2026-11-13.md). It sequences WF-01 through WF-13, defines safety invariants and requires evidence before closure.

**Live progress:** [Workflow stabilization progress log](docs/handoff/MEMO_STABILIZATION_PROGRESS_2026-11-13.md). Update after every meaningful code or verification increment.

### Workflow recovery tasks (active; no task is accepted until verified)

- [ ] **WF-01 — Manual response capture:** add a capture-only paste/review fallback after a confirmed send; require matching provider page; persist capture method and use the existing result/evidence callback. Source patch committed in `b98053b`; verification pending.
- [ ] **WF-02 — Retry detection without resend:** retry only reads/polls the current page; never replays provider submission. Explicit capture-only retry action committed in `WF-02` source increment; verification pending.
- [ ] **WF-03 — Step-level recovery:** Skip provider while no send is pending or uncertain (including rejecting its pending approval); stop a submitted/uncertain provider step with Needs Review; cancel the whole run separately. Source controls/state changes committed through `c720df3`, with verifier/manifest/UI follow-ups through `7d22b8a`; verification pending. Preserve completed outputs.
- [ ] **WF-04 — Truthful completion:** a send click is not a captured answer; verify persistence before declaring success. Source integration now waits for an encrypted response-capture acknowledgement before the browser advances; exact-head CI run #674 passed source verification, JVM tests, lint, APK assembly and API 35 encrypted-store upgrade instrumentation; acceptance remains open pending live provider/device verification.
- [ ] **WF-05 — Real-device reproduction:** document happy path, detector failure, delayed response, navigation drift and process recreation.
- [ ] **WF-06 — Editable enhancement checkpoint:** preserve original intent; edit/regenerate enhanced prompt; invalidate approval on edits.
- [ ] **WF-07 — Persistent task/browser context:** keep active task controls available while inspecting the provider page; full-screen browser remains optional.
- [ ] **WF-08 — Run history:** separate execution history from reusable templates and join prompts, approvals, provider steps, captures and failures.
- [ ] **WF-09 — Council runtime clarity:** distinguish local, configured API and browser-session participants; never equate cookies with API credentials.
- [ ] **WF-10 — Result workspace:** editable working copies, compare/follow-up and provenance while preserving raw captures.
- [ ] **WF-11 — Regression suite:** test capture/retry/skip/approval/persistence invariants.
- [ ] **WF-12 — Device and localization acceptance:** EN/PL/TR, small screens, IME, navigation, process recreation and partial success. Automated evidence: exact-head run #928 passed 434/434 static UI Text translation coverage and 44/44 app unit tests; physical-device and interaction acceptance remain open.
- [ ] **WF-13 — Release gates:** complete the separate STOP-WORK build, encrypted-store, signing and physical-device gates. Automated gates now pass on exact head `7ad0eb0426f953a8946741c59247b081a258f94a` in run #928 (188 core tests, 44 app tests, lint, APK identity, API 35 encrypted-store upgrade and pinned signing); physical-device acceptance remains open, so WF-13 stays unchecked.

- [x] **STAB-01 — Restore a trustworthy build executor:** GitHub Actions runner allocation is restored for the stabilization branch. Exact-head run #649 (14 Nov 2026) assigned a runner and completed source verification plus remote-runner tests. The Vercel path is separately fail-closed until pinned signing credentials exist. Evidence: https://github.com/ysfclc/memo/actions/runs/43643456778.
- [x] **STAB-02 — Verify exact candidate `e7bc8b8f4452ac5b34fe5cb3a040678e7e8cd8c2`:** run #649 passed source invariants, remote-runner tests, 144 core tests, 37 app unit tests, lint, debug APK assembly and APK identity checks. Package `app.memo.android.debug`, versionCode `649`, versionName `4.0.0-memo-debug`. Reports artifact `Memo-Android-Build-Reports` attached to run #649. The final pinned-signing job was intentionally skipped because this was a pull-request event; the intermediate CI-debug APK must not be installed.
- [x] **STAB-03 — API 35 instrumentation:** run #649 passed seed and verify instrumentation phases. The emulator installed a baseline, seeded both encrypted stores, installed the newer APK in place, reopened both stores, and passed legacy recovery/corruption checks. Log: `PASS: signed in-place upgrade preserved app-private data and both real encrypted Memo stores; legacy recovery and corruption tests passed.` Run #674 repeated the in-place upgrade and legacy/corruption checks successfully. Exact-head run #928 (commit `7ad0eb0426f953a8946741c59247b081a258f94a`) repeated the signed in-place upgrade and passed app-private data, both encrypted stores, legacy recovery and corruption checks.
- [x] **STAB-04 — Signing integrity:** exact-head push run #674 (14 Nov 2026; commit `672144a391e04ae33e487064b717932ee9ebf146`) completed the isolated signing job and uploaded `Memo-Android-debug-APK` artifact ID `14136712960`. The signer verified the pinned persistent certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`; no ephemeral publication fallback was used. Artifact checksum: `59dd1a6a7a7240f08cf5feb056fb8c6d9a82f9d03a839eda2950d74fa1a9083d`. Exact-head run #928 also passed isolated signing and uploaded artifact `14565824727` for versionCode `928`; APK SHA-256 `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`, artifact ZIP SHA-256 `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`.
- [ ] **STAB-05 — Physical-device acceptance:** upgrade the existing installation without clearing data; verify launch, auth, chats, stores, inference/Stop, voice, browser approval, native automation, notifications and backup/restore. Record device and evidence.
- [ ] **STAB-06 — Closeout:** update build/device/state records against the exact tested SHA; keep PR #8 draft and `main` untouched until all P0 gates pass and release is explicitly authorized.

**Rule:** tasks remain unchecked until execution evidence is attached. If the runner, quota or device remains unavailable, record a blocker and no ETA rather than claiming completion.


## 0. Mission and non-negotiable product intent

Memo is a **voice-first, local-first, checkpoint-driven personal execution environment**. A conversation is its interface, not its database. Memo captures intent; assembles the capability a task needs; routes safe work through local models, browser sessions, native apps, and human review; produces real artifacts; records evidence; and resumes work without losing context.

The full product scope is retained:

- local model discovery, downloads, inference, proper streaming chat, model selection, cancellation and truthful failure states;
- local ASR/TTS voice conversations, locked-screen entry points, wake-word work only after power/privacy/foreground-service review;
- typed, versioned domain records; deterministic workflow engine; routing and contextual retrieval; reusable capability library/compiler;
- in-Memo browser runtime, isolated per-service sessions, provider frontiers through their web/app UI where appropriate, device-app automation, selection/recording of web and native elements;
- approved actions bound to the exact content hash, explicit permissions, audit trail, outbound redaction, kill switch, bounded retries and postcondition verification;
- populated artifacts before review, live execution visibility, pause/takeover/cancel controls and resumable jobs;
- user-owned Self Model, source ingestion and evidence-backed artifact studio (CV, content and other documents); no invented personal facts;
- optional role-based Council for local and frontier participants, with evidence/claims ledger and controlled debate;
- reminders, capture from anywhere, sources/search/memory, Today/briefings, contacts and everyday utilities;
- English, Polish and Turkish as first-class experiences; accessible, polished, responsive UI with one coherent design system;
- encrypted storage and backup, key/data migration safety, local-first defaults, release automation, measurable performance and real-device acceptance.

## Current implementation increment — 2026-10-28

The strict gates above remain in force. The current source pass adds the following actual behavior, while leaving wider milestones open until their acceptance evidence exists:

- **WP-02 Context-driven execution:** deterministic, bounded retrieval across saved conversations, approved records, prior jobs and evidence with stable source IDs and source excerpts; model prompts label retrieved workspace material as untrusted and request provenance markers.
- **WP-03 Editable checkpoint plans:** draft plans can be revised before a step starts; the route and checkpoint list regenerate from the revised intent; external writes remain a separate approval checkpoint.
- **WP-04 Capability compilation:** the planner compiles selected, registered capabilities into a deterministic SHA-256 manifest, preserves runtime status (including optional permissions), calls out unresolved requirements, and does not grant permissions or execute side effects.
- **WP-05 Grounded Self Model artifacts:** confirmed facts and original source evidence feed local CV/cover-letter/profile/project drafts; generated numeric/date claims get a narrow heuristic review; draft edits create revisions and never mutate source evidence.
- **WP-06 Encrypted portable workspace backup:** passphrase-derived AES-256-GCM envelope with authenticated header, version/iteration bounds, bounded payload, tamper/wrong-password rejection, encrypted export and gated restore. Plain JSON export remains separately labelled as readable.
- **WP-07 Live, cancellable generation:** compatible remote streaming renders incremental response text and exposes active connection cancellation; cancellation retains the partial response instead of claiming completion.

These work packages are **partial source implementation**, not a claim that M0–M6 are finished. Local JVM helper smoke tests and static source verification are recorded in `IMPLEMENTATION_LOG.md`; Android integration, complete CI, and device checks remain open. Follow-up priorities remain unified encrypted storage, full media/share ingestion, generic cross-runtime DAG execution, local ASR/TTS and broader GGUF/llama.cpp support, universal action policy, and real-device acceptance.

See `IMPLEMENTATION_CONTRACT.md`, `MEMO2_ALIGNMENT_MATRIX.md`, and `IMPLEMENTATION_LOG.md` for gate rules, scope crosswalk, and evidence ledger.

## Product reset — 5 November 2026

The normative product direction has been reconciled in [`docs/MEMO_PRODUCT_CONTRACT.md`](docs/MEMO_PRODUCT_CONTRACT.md). The current UI and source are **not yet the intended product**. This reset is a correction of product scope, not a claim of completion.

The primary surfaces are now Talk / Live / Work / Studio in source and CI #99. Talk has one text/voice composer and Council entry points. Live now combines persisted activity, jobs/checkpoints, tasks, action-audit events and evidence in one newest-first timeline; the active browser session remains a separate visible panel and Apps/Approvals remain contextual routes. Studio is a separate primary destination. This is a first shell correction, not acceptance of the complete product.

The current Talk composer opens a conversation draft; it does not yet parse an intent into a dynamic workflow. `IntentPlanner` is keyword/template based and `CapabilityCompiler` maps domains to static lists. They are useful prototypes, not the final dynamic planner or workflow engine. Council is still a separate route. The browser workflow is provider-specific; native app automation uses a separate legacy store; the primary workspace and native workflow store are not unified. Do not describe these as one integrated agent.

**Next implementation order:** (1) keep physical-device install-over-existing/data-preservation acceptance and the reported APK-version/data-loss history active; do not uninstall or clear data; (2) connect safe deterministic Memo app capabilities through the shared runtime request, attempt journal and independent postcondition path, one capability at a time; (3) expand to generic DAG dispatch only after the deterministic app slice passes exact-head CI; (4) then integrate visible browser/native adapters with their existing approvals, permissions and takeover controls; (5) unify Live events and runtime panes; (6) integrate Council with Talk context; (7) complete end-to-end Brainstorm, CRM, Jobs, Content, social and arbitrary-work journeys; (8) add Today priorities and everyday voice/capture utilities. Do not polish isolated CRUD while the intent-to-outcome loop is missing.

## 5 November 2026 — Talk / Live / Work / Studio shell increment

- Product contract: `docs/MEMO_PRODUCT_CONTRACT.md` now records the recovered brainstorms, user goals, architecture decisions, non-goals, safety boundaries, acceptance criteria and honest source-to-vision audit.
- UI: primary bottom navigation is Talk / Live / Work / Studio; More moved to the top bar.
- Talk: one text/voice intent composer creates a conversation draft; Council is reachable from the conversation list and active conversation header.
- Live: first workspace view reads actual browser workflow state, saved jobs/tasks, approval ledger and activity records; Browser, Apps and Approvals are secondary routes.
- Work: Self Model/Studio removed from the Work section.
- Studio: Self Model and local artifact workflow now has a primary destination.
- Validation: CI #99 passed source invariants, remote-runner tests, Android unit tests, lint, debug APK assembly and package/signature identity checks. This is not physical-device acceptance.
- Source commit: `d11b83ad13e731e851d6e169662d0a3c91ddb2a6`
- CI run: https://github.com/ysfclc/memo/actions/runs/42081787582
- CI handoff head: `f3b1a9950241b6db051bb794e1fb7d761f7e8d13`
- Next product slice: typed workflow graph + dynamic intent planning; do not return to isolated CRUD polish.

## 8 November 2026 — Typed TaskSpec and dynamic DAG proposal boundary

- Promoted the existing WorkflowNodeSpec contract to the canonical TaskSpec name while retaining a source-compatible typealias; existing schema-v1 serialized workflow graphs keep their field layout.
- Added CapabilityCompiler.compileForTasks, which assembles only the capabilities explicitly requested by a proposed graph and resolves each capability against its exact runtime ID. A capability on another runtime is never borrowed by name.
- Added WorkflowGraphCompiler.compile(goal, tasks, manifest, budget) for arbitrary proposed task lists, including fan-out/join dependency graphs. Validation now enforces exact runtime/participant compatibility, bounded task contracts, unique capability requirements, model-call and aggregate-timeout budgets, cycle/dependency integrity and approval-isolated external writes.
- Added WorkflowTaskProposalParser as a strict untrusted-JSON boundary. It bounds payload/task/list sizes, requires typed fields, rejects unknown enum values, malformed contracts, unsupported capabilities, invalid dependencies and unapproved external writes. Idempotency keys are generated by Memo, never accepted from model output.
- Added JVM tests for dynamic fan-out/join graphs, exact-runtime capability matching, deterministic capability manifests, fresh app-owned idempotency keys, budgets, cycles, malformed proposals and approval failures. tools/verify_source.py now checks that these safeguards and tests remain present.
- Scope boundary at that checkpoint: typed proposal/compilation only. The 10 November increment now connects the parser to an optional local-model expansion action on untouched Work drafts. Talk still creates its deterministic template plan first; the optional expansion is draft-only, rejects external/approval-gated work and unavailable runtimes, and does not execute nodes. A durable DAG executor, per-node run journal, pause/resume scheduler, runtime dispatch and CI evidence remain open.

## 12 November 2026 — Content-bound runtime receipts for local Work output

- Added a typed `WorkflowRuntimeReceipt` in the pure core. A saved output receipt binds one attempt ID/number, job/checkpoint, exact runtime and capability IDs, stable idempotency key, start/completion timestamps, evidence ID and SHA-256 of the exact artifact bytes.
- Local Work now creates and attaches the receipt in the same immutable snapshot as the review-required artifact and attempt transition. The receipt is persisted inside the existing evidence metadata, so no workspace schema bump or migration is needed.
- Workspace integrity validation checks the receipt against the durable attempt and saved artifact. It requires the receipt marker in both the attempt journal and artifact metadata, rejects missing/mismatched receipts and detects later artifact-content mutation.
- Compatibility: existing schema-7 output attempts without the new receipt marker remain readable; new output attempts are receipt-backed.
- Honest completion boundary: `postconditionStatus=NOT_VERIFIED`. This proves the local runtime returned and Memo persisted those exact bytes; it does not prove the requested outcome. No external action is enabled by this increment.
- Added regression tests for receipt creation/binding, journal validation and mutated output rejection; extended `tools/verify_source.py` to require the receipt source and guard markers.
- Verification status: source and test changes are committed; Gradle/JVM tests, lint, APK and physical-device checks have not run on this head because the current GitHub runner and Vercel build paths remain blocked. Do not mark this slice verified until the exact-head test suite passes.
- Next: introduce a shared runtime-adapter contract and deterministic postcondition evidence path, then connect browser and native adapters only after their existing approval/visibility boundaries are preserved.




## 1. Rules for using Memo 2's plan

1. **Do not clone Memo 2.** Reuse requirements, engineering lessons, tested contracts, and task IDs where useful. Do not wholesale import its screens, Activity trees, persistence assumptions, UI structure, or platform coupling.
2. **Preserve Memo 1.** Never replace its only source ZIP or overwrite existing files before the actual current archive has been extracted and compared. Preserve unique features, app IDs, signing assumptions, storage names, Keystore aliases, migration behavior and user data.
3. **No hidden scope cuts.** If a task cannot be implemented yet, record the dependency, risk, and smallest unblocker. Do not silently turn a complete capability into a mock, one-browser prototype, or demo screen.
4. **No fake completion.** A button is not functionality; a unit test is not an APK; an APK is not device acceptance; model output is not proof of a side effect. Each completion claim links to concrete evidence.
5. **Deterministic boundaries.** Policy, workflow state, schemas, routing decisions, approvals, audit, validation, bounded retry, cancellation and postcondition checks must be deterministic and testable. Models can propose or enrich; they cannot bypass policy.
6. **Approval binds to content.** Any action with an external or destructive side effect must pass an explicit policy gate tied to the exact content hash and scope approved. A changed artifact invalidates approval.
7. **All automation lives within the approved Memo product boundary.** No invisible PC/ADB/remote runner as a substitute for the Android product. Any separately configured remote execution must be isolated, opt-in, authenticated, auditable, and separately approved.
8. **One implementation per capability.** Browser, model, vault and automation implementations sit behind stable replaceable interfaces. Do not add duplicate activities/services or a second competing data store because the current seam is inconvenient.
9. **Secrets stay secrets.** No tokens, prompt contents with personal data, decrypted vaults, cookies, or secrets in logs, screenshots, test fixtures or artifacts. Exports are explicit. Network traffic is visible and audited.
10. **Work only in `ysfclc/memo`.** Do not commit to, branch, merge or mutate `ysfclc/memo-2` as part of this mission.

## 2. P1-0 — Source reconciliation, before implementation

These are mandatory P1 tasks, not optional documentation:

- [ ] **P1-0.1 Fetch the live Memo 1 source.** Obtain the exact current `Memo-Android-Repository.zip` from `ysfclc/memo` default branch. Record commit SHA, blob SHA, size, SHA-256, ZIP CRC/integrity and entry count.
- [ ] **P1-0.2 Inventory before extraction changes.** List every file, Gradle module, manifest, Activity, Service, Compose entry, storage area, model/runtime, automation channel, asset, test, script, build workflow and document. Preserve the pristine archive untouched.
- [ ] **P1-0.3 Compare source snapshots.** Compare live Memo 1 against all locally available snapshots and any branch upload. Generate a path-level diff and identify source-only files, removed/renamed files, duplicates and different implementations. Do not assume smaller snapshots are newer or complete.
- [ ] **P1-0.4 Architecture mapping.** Map each Memo 1 component to the intended domain/core/platform/UI architecture. Identify code to keep, adapt, retire or migrate with evidence and rationale.
- [ ] **P1-0.5 Storage compatibility inventory.** Document app ID, persisted filenames, schemas/versions, encryption, Keystore aliases, backup/export formats, and upgrade behavior. Add migration and rollback tests before touching compatibility-sensitive code.
- [ ] **P1-0.6 Baseline.** Run existing CI against the unmodified source first. Record compiler/test/lint/package failures separately from known product gaps. Do not treat an old green run or old APK as proof of the current source.
- [ ] **P1-0.7 Product acceptance ledger.** Map every requirement and task ID to source paths, tests, CI evidence, and device acceptance where needed. A task without an objective acceptance condition stays open.
- [ ] **P1-0.8 Workflow alignment.** Define one canonical source/archive contract and one matching workflow. Validate the ZIP before extracting; fail if Gradle root, expected module, wrapper, or manifest is absent. Run pure core tests, Android tests, lint, debug/release build as supported, then publish APK, SHA-256 and reports. Trigger and inspect the real run.

**Gate P1-0 exit:** source reconciliation report exists; source identity and baseline are known; no destructive replacement or source-code implementation occurs before this gate passes.

## 3. Verification protocol

Every task follows this cycle:

1. Read its product-plan section, predecessors and relevant Memo 1 source before editing.
2. Make the smallest coherent change; avoid unrelated formatting or renames.
3. Add or update named tests before accepting behavior. Pure policy/codec/engine logic belongs in JVM tests whenever possible.
4. Run targeted tests, all required module tests, static checks, lint and relevant build tasks. Record exact commands and outcomes.
5. Inspect the diff and security-sensitive paths personally: permissions, user data, external actions, approval gate, redaction, cancellation and failure behavior.
6. Verify acceptance criteria and evidence; do not accept an agent's assertion as evidence.
7. Update task status, `FEATURES.md`, `PROGRESS.md` and the engineering log. Keep incomplete items `[~]` or `[ ]` with the blocker and next step.
8. Merge only after checks pass. The integration branch must be green and installable; record commit SHA and CI run.

### Evidence labels

- **Unit:** named core/JVM test and result.
- **Integration:** Android/runtime/storage/adapter test and result.
- **CI:** workflow run URL/ID, commit SHA, successful job summaries.
- **Artifact:** APK path/version, byte size, SHA-256 and build variant.
- **Device:** explicit phone/emulator walkthrough, OS/device, steps, outcome and any known limitation.
- **Security:** test/review evidence for permission, data-flow, secrets, destructive action, authentication and logging boundaries.

No `[x]` for a task that requires a device check until a device check is recorded.

### Work package IDs added to the evidence ledger

| WP-ID | Product area | Acceptance implemented in source | Evidence / remaining boundary |
|---|---|---|---|
| WP-02 | Context-aware local execution | Bounded deterministic retrieval returns source IDs, excerpts and per-hit relevance; prompts mark retrieved material as untrusted. | `WorkspaceContextRetriever.kt`, `WorkspaceContextRetrieverTest.kt`, Work prompt integration. Android run and evidence-quality/device review pending. |
| WP-03 | Draft plan editing | Untouched DRAFT can be edited; route/checkpoints regenerate; active/started plans cannot be rewritten in place. | `IntentPlanner.kt`, `MemoWorkspaceViewModel.reviseIntentPlan`, `IntentPlanEditDialog`. Gradle and UI test pending. |
| WP-04 | Capability composition | Registered capability selection, missing-ID reporting, permission/status retention and SHA-256 manifest. | `CapabilityCompiler.kt`, tests and plan integration. Full versioned skill/tool schemas and runtime-pinned execution manifests remain open. |
| WP-05 | Grounded Self Model artifact | Approved facts/source material, immutable original source, generated numeric/date claims flagged heuristically, drafts versioned for human review. | `SelfModelFactualityGuard.kt`, `SelfModelStudioScreen`, tests. Not a general truth detector. |
| WP-06 | Encrypted backup | Versioned AES-256-GCM envelope, passphrase KDF, AAD, payload bounds, wrong-passphrase/tamper rejection, explicit restore confirmation. | `EncryptedBackupCodec.kt`, encrypted Settings flow, codec tests/smoke. Restore preview and on-device recovery test remain open. |
| WP-07 | Live generation control | Compatible remote responses render as stream chunks; cancellation disconnects active request and retains partial output. | `OpenAiCompatibleClient.kt`, `AiRuntimeViewModel.kt`, chat UI. End-to-end lifecycle race and device checks pending. |

## 4. Milestones and task map

The following keeps Claude's task families and ordering while requiring Memo 1-specific implementation and proof. Parent task IDs remain stable for traceability. Detailed mini-task notes, test names and build instructions should be copied into the matching task as work starts; do not mark the Memo 2 status as Memo 1 status.

### M0 — Foundation, deterministic core, safety and vault

- [ ] **M0.0** Establish the adapted plan, task state, branch/commit policy and evidence ledger.
- [ ] **M0.A1** Make external workflow steps asynchronous and resumable (`Pending`/external result delivery); define timeout, cancel, stale result and process-death behavior.
- [ ] **M0.A2 (folded into M0.A1 where the source plan consolidates it)** Audit wiring: every side effect and state transition is represented in the audit trail; verify no empty/detached audit chain.
- [ ] **M0.A3** Define versioned domain entities and invariants for Project, Note, Person, Company, Job, ContentDraft, Post, MetricSample, WorkflowTemplate, Prompt, ArchitecturalPrinciple, ContentPreference, ChannelAccount, ProviderAdapter, ProviderCapability, SocialAccount, SocialItem, SocialConversation, SocialMessage, EngagementAction, Campaign and SocialMetric, plus migration compatibility.
- [ ] **M0.A4** Add schema semver, workspace versioning, registry reuse/update rules and real migration (not identity-only); test round-trip, unknown versions and failed migration rollback.
- [ ] **M0.A5** Centralize policy: channel controls, destructive-action classification, user permissions, approval requirements, redaction and kill switch.
- [ ] **M0.A6** Implement deterministic routing contracts: selected target, rationale, availability, fallback and audit; routing must not bypass user settings.
- [ ] **M0.A7** Define platform interfaces for LLM, speech, vault, browser, app automation, package discovery, scheduler and notifier; keep pure core free of Android imports.
- [ ] **M0.A8** Resolve hidden/ignored core test failures and add regression coverage for all fixes.
- [ ] **M0.B1** Stabilize Android/dependency/native infrastructure, pinned toolchain and repeatable CI build.
- [~] **M0.C1** Harden encrypted vault, key lifecycle, auth binding and secret-safe diagnostics. The primary workspace and native workflow stores now have separate canonical files; legacy payloads are authenticated before atomic copy, old aliases/formats are retained, and reads no longer create replacement keys. CI and device acceptance remain pending.
- [~] **M0.C2** Preserve Memo 1 data and key/file compatibility across the current primary/native store boundary. Store isolation and both legacy migration paths are implemented with an isolated Android instrumentation acceptance test; CI/device proof and the later single-store transaction/event boundary remain open.
- [ ] **M0.F1** Fix engine safety invariants: gates bind to content hash, retries are bounded, vetoes dominate, cancellations stop work, postconditions determine success.
- [ ] **M0.F2** Review/fix app integration and UI assumptions against core contracts.
- [ ] **M0.F3** Independently review core logic and resolve findings with tests.
- [ ] **M0.F4** Harden publish-preview / outbound action confirmation and artifact integrity.
- [ ] **M0.R1** Perform independent milestone security/architecture review and record findings.
- [ ] **M0.R2** Second independent review; resolve material issues before closing milestone.
- [ ] **M0 close (MCP)** Core checks green, vault compatibility established, no critical findings open, evidence recorded.

**M0 acceptance:** no external action can run without an explicit hash-matching gate; audit is non-empty and tamper-evident; data survives app restart and migration; pure core test suite passes; CI builds a genuine installable Memo 1 APK.

### M1 — Local AI, model hub, chat

- [ ] **M1.A1** Integrate LiteRT-LM behind the model-runtime interface; real streaming, cancellation, initialization failure handling and device capability reporting.
- [ ] **M1.A2** Integrate llama.cpp/GGUF runtime behind the same interface; pin source/API, JNI lifecycle and error/OOM behavior.
- [ ] **M1.A3** Build inference manager/service with bounded resource use, runtime selection, concurrency, model unload and truthful states.
- [ ] **M1.B1** Implement pure model catalog/search/compatibility metadata with pinned revisions and hash/size verification.
- [ ] **M1.B2** Build safe model downloads/imports, resumability, disk checks, cancellation and encrypted/approved storage policy.
- [ ] **M1.B3** Ship model hub UI and real install/ready/error/unsupported states; no fake download progress.
- [ ] **M1.C1** Implement encrypted, versioned conversation/message persistence and migration.
- [ ] **M1.C2** Implement full chat: streaming, stop/regenerate, model picker, voice messages, context visibility, actions, empty/loading/error states and accessible layout.
- [ ] **M1 close (MCP)** Demonstrate install → load → stream → cancel → restart → recover history on a supported device; document model sizes and limitations honestly.

### M2 — Voice; M2.5 — speed and profiling

- [ ] **M2.A1** Audio input/output interfaces, foreground-service policy, lifecycle, permission rationale and interruption handling.
- [ ] **M2.A2** Integrate local speech engine (ASR/TTS) with downloadable models, language capability, VAD and accurate progress/error handling.
- [ ] **M2.A3** Connect speech to conversation and orchestration: barge-in, stop, transcript correction and recoverable failures.
- [ ] **M2.B1** Voice message/live conversation UX integrated with M1 chat.
- [ ] **M2.C1** Assistant role/session behavior and lock-screen entry with Android-compliant visibility and permissions.
- [ ] **M2.C2** Wake-word / screen-off listening only after battery, privacy, permission and foreground-service design review; no hidden listening.
- [ ] **M2.C3** Quick settings tile, bubble/overlay and ongoing notification; each has explicit stop and return-to-Memo behavior.
- [ ] **M2 close (MCP)** Verify live speech on device in supported languages; confirm no unwanted system speech popup or silent always-on capture.
- [ ] **M2.5.A1** Release build variant and persistent signing design; protect signing material; never commit secrets.
- [ ] **M2.5.A2** Establish baseline profiles and reproducible macrobenchmarks.
- [ ] **M2.5.A3** Profile and optimize measured startup, chat first-token latency, memory, download and navigation performance.

### M2.B0 — Design system (parallel, dependencies enforced)

- [ ] **M2.B0a** Tokens, typography, color system and themes in one canonical design system.
- [ ] **M2.B0b** Motion tokens and haptics; honor reduced-motion settings.
- [ ] **M2.B0c** Accessible component kit with disabled/loading/error/empty states and consistent touch targets.
- [ ] **M2.B0d** Navigation shell, shared transitions, predictive back and responsive layouts. No second navigation system or disconnected mock screens.

### M3 — Orchestrator and live environment

- [~] **M3.A1** Local planner maps intent to a typed, bounded workflow dynamically; no hardcoded workflow per use case.
- [ ] **M3.A2** Context retrieval across permitted Memo records/sources with relevance, provenance, privacy scope and source freshness.
- [~] **M3.A3** Connect chat and workflow state; show plans, artifacts, checkpoints and outcome evidence in the same product flow.
- [ ] **M3.A4** Local tool-using agent engine with capability restrictions, tool schemas, bounded steps, iteration/time budgets and kill switch.
- [ ] **M3.B1** Evidence-bearing artifact renderers and approval UX; user reviews the populated artifact, not only a vague plan.
- [ ] **M3.B2** Live tab/session view; every action has state, timestamp, rationale, result and evidence.
- [ ] **M3.B3** Glance widget with truthful status and safe user control.
- [ ] **M3.C1** Profile/preferences, voice preferences, privacy defaults and explicit autonomy settings.
- [ ] **M3.C2** CRM enrichment flow: capture person, resolve/create company, research missing facts, label provenance/confidence, approve and persist.
- [ ] **M3 close** Device acceptance: “I met Ada at Web Summit, add to CRM” → plan → sourced filled record → user approval → persisted record and follow-up task.

### M3.5 — Full control

- [ ] **M3.5.A1** Standing rules and remembered approvals with narrow scopes, expiry/revocation and audit.
- [ ] **M3.5.A2** Humanizer is optional and cannot alter facts, remove caveats or bypass policy.
- [ ] **M3.5.B1** Real CRUD/editors for every supported record type, with validation, versions and provenance.
- [ ] **M3.5.B2** Workflow editor, versioning, preview, dry run, test, duplicate and rollback.
- [ ] **M3.5.C1** Complete settings suite: permissions, channels, autonomy, privacy, model/runtime and diagnostics.

### M3.6 — Languages

- [ ] **M3.6.A1** Multilingual intent/command grammar, locale-independent core semantics and regression fixtures.
- [ ] **M3.6.B1** App localisation and language picker; do not hardcode user-facing strings in screens.
- [ ] **M3.6.C1** Voice pipeline and key everyday workflows for Polish and Turkish in addition to English.

### M3.7 — Everyday product

- [ ] **M3.7.A1** Reminders, timers, alarms and reliable scheduling through platform interfaces.
- [ ] **M3.7.A2** Time-intent resolution with locale/timezone correctness and tests in EN/PL/TR.
- [ ] **M3.7.B1** Capture from anywhere with explicit Android sharing/overlay permissions and safe fallback.
- [ ] **M3.7.B2** Source ingestion, indexing, chunking, deduplication and retrieval with traceable references.
- [ ] **M3.7.B3** Memory graph with tested bounded layout, explicit relationships and user correction.
- [ ] **M3.7.C1** Today, briefings, driving mode and quiet hours; notification policy honors user settings.
- [ ] **M3.7.C2** Onboarding, contextual help, templates and fresh-install walkthrough.
- [ ] **M3.7.C3** In-app updates with signed/hash-verified artifacts, version comparison and rollback/failure path.
- [ ] **M3.7.C4** Contact import, safe merging and optional place tags; reuse-first identity resolution.

### M3.8 — Capability Compiler

- [ ] **M3.8.A1** Versioned capability/library schemas and real migrations; trust transitions are explicit.
- [~] **M3.8.A2** TaskSpec/graph/assembler/readiness analysis: core proposal parsing and exact-runtime capability validation exist; compiler UI, conflict visualization and full readiness acceptance remain open.
- [ ] **M3.8.A3** Compile an execution package from validated capabilities; golden tests by size class; inspectable prompt/context assembly.
- [~] **M3.8.A7** Durable runtime executor: the local-model Work slice is checkpoint-gated and writes a bounded per-node attempt journal in workspace schema 7. Each attempt binds job/step, exact runtime/capabilities and stable TaskSpec idempotency key; output-save/failure close the attempt with the step/job/evidence transition; process recovery marks RUNNING attempts INTERRUPTED and requires review. Typed runtime output receipts bind saved bytes and remain NOT_VERIFIED. ORCH-007 provides the CI-verified shared runtime request/resolver and deterministic postcondition contracts. The first concrete app adapter, app.entities/entity.propose, is wired through Talk's source-grounded outcome extraction and Work's durable attempt journal. A shared WorkflowRuntimeDispatcher now owns exact adapter resolution, pre/post cancellation/deadline checks and candidate-output validation; the Android host enforces the remaining persisted-attempt deadline. The separate read-only observer still verifies persisted PROPOSED entities before completion; proposals are never auto-approved. Generic DAG scheduling, shared workflow-wide budgets, cross-runtime cancellation, pause/resume/takeover, browser/native adapters and physical-device acceptance remain open.
- [~] **M3.8.A10 — Deterministic app-runtime capability:** the first concrete handler for app.entities/entity.propose is connected to Talk's existing outcome extraction. It accepts only exact source-message excerpts, binds an invocation to the saved TaskSpec/runtime/capability/attempt, emits bounded idempotent candidate output, persists PROPOSED Work entities and a content-bound receipt, then requires an independent read-only app.jobs observer and a separate WORKFLOW_VERIFICATION record before completing the checkpoint. No proposal is auto-approved and no browser/native/external side effect is enabled. Regression tests and source-verifier guards are committed. Exact-head CI #767 passed all automated gates, including API 35 in-place upgrade/data-preservation checks; physical-device acceptance remains pending.
- [~] **M3.8.A6** Optional local-model TaskSpec expansion for untouched Work drafts: strict parser, exact goal preservation, current runtime/capability revalidation, no external/approval-gated tasks, no execution. Talk-native expansion is integrated. Dependency-aware checkpoint coordination validates the saved DAG, dependency readiness, current runtime/capability availability and external-effect boundaries; it persists one active checkpoint and labels outcomes as user-reported. Legacy sequential jobs remain supported. Core/app regression suites passed on exact-head CI #678; broader dynamic execution remains open.
- [ ] **M3.8.A4** Evaluate/diagnose/repair/learn with finite budget, bounded retry and promotion only with evidence.
- [ ] **M3.8.A5** Architect/critic loop with disagreement handling, evidence comparison and hard round limit.
- [ ] **M3.8.B1** Seed capability library; every seed loads, validates and has an evaluator.
- [ ] **M3.8.C1** Compiler UI to inspect requirements, capability selection, gaps, generated plan and verification evidence.

### M3.9 — Self Model and Artifact Studio

- [ ] **M3.9.A1** Encrypted, user-owned, versioned Self Model; diffs, conflicts and approval before merge.
- [ ] **M3.9.A2** Ingest websites, LinkedIn exports, CV text and user files; every ingest produces a reviewable, sourced diff.
- [ ] **M3.9.B1** CV/export generation (DOCX/PDF/JSON Resume as appropriate) with structural and page-fit tests.
- [ ] **M3.9.B2** Job-tailored artifacts and fabrication guard: reject unsupported employer, metric, experience or skill claims; test with false-input fixtures.
- [ ] **M3.9.B3** Other artifact types with per-type factuality validation and EN/PL/TR templates.
- [ ] **M3.9.C1** Outbound profile sync only by explicit user action, preview, approval and auditable confirmation.
- [~] **M3.9 — General private-draft foundation:** Studio can create typed user-authored drafts via `WorkArtifactFactory`, persist them in the encrypted workspace, list them with generated artifacts, and reopen/edit/export them. `updateWorkArtifact` stores up to five prior revision snapshots. Factory regression tests passed in exact-head CI #678; revision compare/restore UI, autosave and richer document formats remain open.

### M4 — Channels, automation, Council and Live view

- [ ] **M4.0** Emulator execution/acceptance job in CI where feasible; stable fixtures and reports.
- [ ] **M4.A0** Pure automation contracts: closed action vocabulary, typed input/output, bounds, permission labels and postconditions.
- [ ] **M4.A0b** Fixtures for forms, lists/pagination, shadow DOM, contenteditable editors, streaming AI chat, login wall and DOM version drift.
- [ ] **M4.A1** Headless in-Memo browser runtime, isolated service sessions, safe cookie storage and human takeover.
- [ ] **M4.L1** Action narrator/session state: explain what is happening without claiming unverified success.
- [ ] **M4.A2** Provider adapters and frontier executor; use visible web/app interaction where intended, respect login/session and rate/policy constraints.
- [ ] **M4.L2** Web Live view with event stream, highlighted target, action preview, result, pause/resume/takeover/cancel.
- [ ] **M4.A3** Browser tab with HTTPS policy, capture-to-chat, login status and takeover banner.
- [ ] **M4.B1** Accessibility service with opt-in, app allowlist, sensitive-window detection and immediate disable.
- [ ] **M4.B2** Screenshot/OCR fallback only when accessible nodes cannot safely satisfy the task; never tap a password/secret region.
- [ ] **M4.B3** Native AI-app adapters with versioned capabilities and deterministic failure states.
- [ ] **M4.B4** App capability map, versioning and reuse-first matching.
- [ ] **M4.C1** Device-wide AI app discovery with signature evidence; avoid unsupported package/name assumptions.
- [ ] **M4.C2** Channel settings and router wiring; explain why a channel was chosen and what access it needs.
- [ ] **M4.C3** Social provider capability registry: versioned account/provider capability manifests, exact scopes, account eligibility, limits, policy constraints, fixtures, health and last-verified evidence. Planner must not infer support from a provider name.
- [ ] **M4.D1** Native-app sessions with visible Memo console, verified app identity, refusal when session controls are hidden, safe return path.
- [ ] **M4.L3** App Live HUD with node highlighting and immediate pause when user touches the target app.
- [ ] **M4.D2** Memo overlay console; verify safe form filling, secure-window suppression and that Memo never taps itself by accident.
- [ ] **M4.A4a** Council core as a pure state machine: seats, roles, relevance, budget, round limits, claim ledger, disagreement and anti-sycophancy.
- [ ] **M4.A4b** Council runtime for local and frontier participants; each participant receives only approved relevant context.
- [ ] **M4.A4c** Council UI inside Talk with named roles, distinct evidence, disagreements and final synthesis.
- [ ] **M4.A4d** Evidence-backed extraction; an unsupported claim is rejected unless a source quotation/reference supports it.
- [ ] **M4.A5** Optional API channel with opt-in, HTTPS, per-request context preview, redaction and outbound audit. API not required for web/app channels.
- [ ] **M4.E0** Versioned automation schemas: validation, stability scoring, transforms and migration generation.
- [ ] **M4.E1** Web element picker: unique locator, list generalization, shadow DOM, live count and no password capture.
- [ ] **M4.E2** Schema builder UI and fixtures demonstrating accurate repeat-item extraction.
- [ ] **M4.E3** Web recorder: record safe fill/submit sequence, parameterize it, verify a postcondition, and require approval for commit/destructive steps.
- [ ] **M4.E4** Native app picker/recorder tested against a controlled fixture app.
- [ ] **M4.E5** Self-healing proposes a quarantined trial patch; never silently commits changed locators or risky steps.
- [ ] **M4.E6** Workflow editor integration: build a workflow by selecting actual elements and inspect generated recipe.
- [~] **M4.L4** Live now projects persisted activity, jobs/checkpoints, tasks, action-audit events, evidence and per-node execution attempts into one newest-first feed. Attempt entries expose timing, runtime/capability references and review/failure state without model payloads. Still required: true cross-runtime event journaling, inline browser/native/overlay/background panes, and pause/resume/takeover controls backed by runtime support.
- [~] Work now exposes local-model TaskSpec expansion on untouched draft plans. Proposals are parsed against the runtime registry and applied only after exact-goal, capability, availability and no-external-write checks; every node remains PENDING and the job remains DRAFT. Tests are added but unexecuted. Talk now exposes the optional expansion directly in its Start with Memo composer, using an installed local model only. The initial plan remains deterministic; no DAG node is dispatched automatically.
- [ ] **M4 close** Demonstrate end-to-end browser and native-app automation with user approval, live view, pause/takeover, postcondition verification and audit.

### M5 — Complete use cases end to end

- [ ] **M5.A1 UC-1 Brainstorm:** voice/text intent → contextual local planning → relevant frontier consultation/Council as configured → save to Project → action items/tasks with provenance.
- [ ] **UC-2 CRM capture acceptance (implemented under M3.C2)**: correct identity linking, sourced enrichment, user approval and persistence; close only with the end-to-end acceptance evidence described under M3.
- [ ] **M5.B1 UC-3 Jobs:** source and rank jobs; approval per job; tailor factual cover letter; apply only through an approved channel; record verified status.
- [ ] **M5.C1 UC-4 Content:** source idea → preferences → text/image/carousel/video draft → review → user-approved publish → metric tracking and learning loop. No pretend publishing.
- [ ] **M5.C1a** Local image-generation capability with real model/adapter status, resource limits, cancellation and device test.
- [ ] **M5.C1b** Media asset pipeline for slides/storyboard, carousel PDF and/or MP4; validate real output files on device.

#### M5.D — Social platform (full product domain; not content-only)

- [ ] **M5.D0 Provider truth audit:** LinkedIn, Instagram, Facebook Pages, X, TikTok, YouTube, Reddit and Threads; document official APIs, scopes, account eligibility, review/pricing, rate limits, terms and unsupported operations with dated sources. Evaluate the existing separate WhatsApp/Troy workflow without silently replacing it.
- [ ] **M5.D1 Accounts and identity:** connect/disconnect/reconnect, multiple eligible profiles/pages, scoped credentials, health, sync status, capability matrix and revocation tests.
- [ ] **M5.D2 Unified feed/activity:** real provider-backed reads, followed people/pages/topics where exposed, filtering, pagination, refresh, dedupe, provenance, offline/stale states and honest coverage gaps.
- [ ] **M5.D3 Unified inbox:** supported DMs, requests, comments, mentions and replies; conversation grouping/search, participant identity resolution, Work/CRM linking and permission-aware attachments.
- [ ] **M5.D4 Engagement:** supported reactions, comments, replies, repost/share, follow/unfollow and bookmarks; exact account/target/content approval, verified receipts and NEEDS_REVIEW for uncertain sends.
- [ ] **M5.D5 Publishing operations:** channel-specific content variants, private calendar/queue, media assets, previews, per-destination approval, provider-supported scheduling, independent receipts and partial-failure recovery.
- [ ] **M5.D6 Listening and discovery:** bounded monitoring of user-selected topics/people/brands/competitors, sourced digests, opportunity discovery, alerts and follow-up tasks.
- [ ] **M5.D7 Analytics and learning:** source-linked account/post metrics, exact post/version association, historical comparisons, evidence-backed recommendations and explicit distinction between provider data/imports/estimates.
- [ ] **M5.D8 Personal brand and growth:** approved positioning/content pillars in Self Model, inbound enquiry-to-CRM workflows, customer acquisition, campaigns, partnerships and monetization experiments with factuality guardrails.
- [ ] **M5.D9 Automation:** opt-in user-authored monitoring/draft/follow-up rules with narrow scopes, limits, quiet hours, global stop, audit and anti-spam constraints.
- [ ] **M5.D10 Security and data lifecycle:** encrypted local storage, provider-secret isolation, per-account revoke, cache deletion/export, remote-context consent, rate-limit handling and account-disconnect recovery.
- [ ] **M5.D11 End-to-end acceptance:** two eligible real providers; account revoke; feed sync; supported inbox thread; approved reply with receipt; multi-destination publish; metrics ingestion; provider failure/rate-limit/token-expiry; ambiguous send and stop. Clearly label fixtures vs sandbox vs real-account evidence.

Full product contract: docs/SOCIAL_PLATFORM_PRODUCT_SPEC.md. No task closes because a feed/inbox screen renders sample data.

- [ ] **M5.C2 UC-5 Arbitrary plan:** decompose arbitrary tasks, enforce iteration/time/action budgets, stop always wins, and all risky steps checkpointed.
- [ ] **M5 close** Watch and verify UC-1, UC-3, UC-4 and UC-5 end to end on a real supported phone; record any blocked external services clearly.

### M6 — Hardening and release

- [ ] **M6.1** Offline queue, process-death/boot recovery and `NEEDS_REVIEW` state for ambiguous interrupted actions.
- [ ] **M6.2** Diagnostics and exportable redacted logs; verify tokens, cookies, vault values and personal secrets cannot leak.
- [ ] **M6.3** Encrypted backup/export/import with passphrase and tamper tests, wrong-password behavior, versioned round trips and documented recovery.
- [ ] **M6.4** App lock, biometric behavior, relock timer and secure-window policy.
- [ ] **M6.5** Permission centre; every manifest permission has purpose, rationale, requested-at-use behavior and revoke behavior.
- [ ] **M6.6** Strict lint (`abortOnError = true`) with zero errors or explicitly documented, bounded waivers.
- [ ] **M6.7** Test sweep and coverage report for engine, policy, codec, routing, vault contracts, Council and automation. Target ≥85% line coverage for pure core where practical; critical safety paths need direct tests regardless of percentage.
- [ ] **M6.8** Update README, build instructions, changelog, architecture, status and migration notes.
- [ ] **M6.9** Final acceptance ledger: every feature maps to verified evidence or an honest open limitation; no stale `[x]` marks.
- [ ] **M6.10** Accessibility audit: TalkBack, 48dp targets, 200% font, contrast, reduced motion and empty/loading/error/offline states.
- [ ] **M6.11** Release build, artifact checksum, provenance, install/launch smoke test, release notes and rollback procedure.
- [ ] **v1.0.0** Tag only after release gate passes. Publish/sign only with explicit user approval; never commit signing secrets.

## 5. Cross-cutting gates — apply to every milestone

- [ ] **Security gate:** no secret in logs, crash reports, fixtures, clipboard or network payload; secure windows honored; HTTP blocked where HTTPS is required; unsafe target and permission states fail closed.
- [ ] **Policy gate:** all external/destructive actions pass current hash-matching approval; changed content requires reapproval; cancellation is honored at every async boundary.
- [ ] **Reliability gate:** every long task has timeout, cancellation, bounded retry, idempotency strategy, explicit failure state and process-death recovery plan.
- [ ] **Truthfulness gate:** outcome status requires a verified postcondition; a model's claim alone cannot mark an action successful.
- [ ] **Data gate:** source/provenance, timestamps, confidence, schema version and migration path for persisted records.
- [ ] **UX gate:** no dead controls, placeholders masquerading as features, hidden progress, unexplained permissions, blank failures or inaccessible core path.
- [ ] **Privacy gate:** local-first default; any remote request shows target, exact context/data scope and reason before sending; user can cancel/disable each channel.
- [ ] **Language gate:** newly added strings are localized; intent and time behavior have EN/PL/TR test fixtures where applicable.
- [ ] **Build gate:** pinned versions, reproducible wrapper, tests, lint, source ZIP validation, APK checksum and archived reports.
- [ ] **Documentation gate:** task status, feature checklist, progress notes and engineering decisions updated together.

## 6. Parallel work packages

Use small work packages, not one agent per tiny task. Parallelize only when file ownership is disjoint and dependencies are already met. Keep no more than three concurrent implementers unless the lead demonstrates there is no shared-file collision. Every task gets one commit or a clearly traceable commit range and the verification gate above.

Suggested waves (confirm against live Memo 1 source and dependency state before launch):

- **Wave 0:** P1-0 source reconciliation and baseline; no parallel code changes.
- **Wave 1 / M0:** async engine + policy/audit; domain/schema/registry; vault/platform interfaces; UI-kit tokens may proceed only on disjoint files after the baseline.
- **Wave 2 / M1:** local LLM runtimes; model hub; chat/persistence after their platform contracts stabilize.
- **Wave 3 / M2:** voice I/O, ASR/TTS, conversation integration; design component/navigation work.
- **Wave 4 / M3:** planner/context/agent; artifact+Live surfaces; profile and everyday utilities.
- **Wave 5 / M3.8–M3.9:** compiler/library; Self Model/artifacts, after schemas and retrieval contracts are stable.
- **Wave 6 / M4:** automation contracts/fixtures; browser runtime; native-app/accessibility channel; element selection/recorder; Council after role/capability types are agreed.
- **Wave 7 / M5–M6:** complete user workflows, then security/performance/accessibility/release hardening.

## 7. Milestone close procedure (MCP)

A milestone cannot close until:

1. all required tasks and named tests are complete;
2. the entire required test/lint/build pipeline is green on the integration commit;
3. the diff and safety-sensitive code have been independently reviewed;
4. CI artifact, commit SHA and evidence links are recorded;
5. required device walkthroughs are recorded, or the milestone explicitly remains open;
6. FEATURES checklist and progress log agree with the code and proof;
7. every known defect is fixed or carried forward with severity, impact, workaround and owner;
8. next milestone has explicit dependencies and no hidden assumptions.

## 8. Definition of shipped

Memo is not “done” because a ZIP exists, a screen looks complete, or CI produced any APK. A release is accepted only when the product's declared scope is implemented or explicitly deferred by the user, workflows work end to end with verifiable outcomes, local-first/security claims are tested, the critical paths work on a real device, and build/recovery/release instructions are reproducible.

## 9. Living ledger

At every task update, add a record with:

- **Task ID / status:** `[ ]` not started, `[~]` in progress/blocked, `[x]` verified in Memo 1.
- **Commit:** exact Memo 1 commit SHA.
- **Files:** changed source paths.
- **Tests:** named tests and exact commands/results.
- **CI:** run URL/ID, artifact name and checksum when applicable.
- **Device/security evidence:** exact steps and result where required.
- **Decision/deviation:** why Memo 1 differs from Memo 2 where relevant.
- **Remaining work:** smallest concrete next action.

Do not copy progress claims from Memo 2 into this ledger. Start with P1-0 and keep every Product 1 completion claim independently verifiable.


## 28 October 2026 — Memo 1 implementation ledger (in progress)

- **Task family / status:** M0.A2 + M0.A5 + M0.F1, `[~]` partial browser-path implementation only. The full milestone remains open; this is not a Memo 2 completion claim.
- **Commit:** none. The connected GitHub write integration returned HTTP 403 for a contents update; no repository commit is claimed. Branch-ready archive delivered as `Memo-Android-Repository.zip`; it now bundles this complete runbook and `MEMO-PATCH1-AUDIT.md` inside `Memo-Android/`. The archive and all 90 source-manifest entries were revalidated after packaging. Use the matching `.sha256.txt` sidecar delivered with the ZIP to verify the exact final archive. No separate runbook ZIP is required.
- **Files changed in the working source:** `core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt`; `core/src/main/kotlin/app/memo/android/data/Models.kt`; `core/src/main/kotlin/app/memo/android/browser/BrowserWorkflow.kt`; `app/src/main/java/app/memo/android/browser/BrowserSession.kt`; `app/src/main/java/app/memo/android/ui/BrowserWorkflowScreen.kt`; `app/src/main/java/app/memo/android/ui/ActionApprovalInboxScreen.kt` (new); `app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt`; `app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt`; `app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt`; `app/src/main/java/app/memo/android/ui/MemoLocalization.kt`; `core/src/test/kotlin/app/memo/android/data/ActionApprovalLedgerTest.kt`; `core/src/test/kotlin/app/memo/android/data/CouncilBackupTest.kt`; `tools/verify_source.py`; `TASKS.md`; `FEATURE_AUDIT.md`; `PRODUCT_GAP_REGISTRY.md`; `README.md`; `CHANGELOG.md`; `BUILD_STATUS.md`; `SOURCE_SHA256SUMS.txt`.
- **Source behavior:** exact external prompt stored with runtime/capability/destination/payload hash; full prompt preview; human approval required for each browser provider submit; one-use approval is checked before send; destination and payload are revalidated; approval is invalidated on mismatch/import/restart; uncertain in-flight action moves to `NEEDS_REVIEW`; completion requires a saved response evidence record; audit events form a verifiable SHA-256 hash chain. Audit hashing is tamper-evident but not a cryptographic signature against a fully privileged source editor.
- **Tests/evidence:** `python3 tools/verify_source.py` passed, including a new central-inbox/stale-approval source check and a 283/283 static translation scan across six Compose files; `PYTHONPATH=. python3 -m unittest discover -s tests -v` from `remote-runner/` passed 12/12; a standalone `kotlinc` harness compiled the actual `ActionApprovalLedger.kt` against minimal snapshot/evidence stubs and passed core transition/mismatch/audit tamper checks. New Gradle JVM tests are written but **not run**.
- **Blockers:** Gradle 8.14.6 wrapper distribution download failed with DNS resolution for `services.gradle.org`; therefore full `:core:test`, Android Kotlin compilation, `:app:testDebugUnitTest`, lint, APK and WebView/device behavior are unverified.
- **Remaining work:** obtain a working Gradle CI run and fix compiler/test/lint failures; verify schema v4 codec and Compose compilation; connect the ledger to native/guest/publishing runtimes; complete the first global stop slice with a durable cross-runtime cancellation token and DAG; validate provider DOM/send behavior and process restart on a real device.


## 28 October 2026 — central action inbox implementation increment

- **Task ID/status:** M0.A2 + M0.F1 / `[~]`. This is a browser-ledger inbox, not yet a universal app-wide approvals queue.
- **Commit:** none; direct GitHub file write was rejected by the connected integration; a branch-ready ZIP will be supplied for upload.
- **Behavior added:** More → Action approval inbox exposes action queue, needs-review and history filters; shows exact payload, hash, destination, runtime, capability, expiry, evidence pointer and the latest five audit events; permits rejecting pending proposals and marking active/approved outcomes for review; routes only exact-hash/destination matches back to the live browser dialog. Saved history cannot be approved or replayed.
- **Integrity hardening:** ledger import rejects orphaned audit events, transitions require the ledger to verify, and the workspace size estimate counts event status/sequence/time.
- **Proof:** source verifier passed (283/283 eligible UI labels catalogued across six scanned Compose files); remote runner unit suite passed 12/12; standalone `kotlinc` harness passed the targeted ledger scenarios. Android Compose/Kotlin compilation, Gradle tests, lint, package and device acceptance are not verified.
- **Next:** build/compile the full tool, fix first-run and migration regressions, then implement universal action gating, a real pause/stop, durable workflow DAG and device acceptance. Do not close P0/M0.


## 28 October 2026 — top-level stop-control update

- **Task family/status:** M0 cancellation and HITL policy, `[~]` partial.
- **Behavior now present in branch-ready source:** top-level Stop active work requests cancellation of local model generation, the browser workflow and pending browser task, plus active AccessibilityService automation. Pending external proposals are rejected; already approved/in-flight external actions are moved to `NEEDS_REVIEW`. A nonterminal guest-runner receipt triggers a best-effort remote cancellation request with explicit user-facing uncertainty if not confirmed.
- **Files added/changed in this slice:** `app/src/main/java/app/memo/android/ui/MemoWorkspaceApp.kt`, `tools/verify_source.py`, `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, `TASKS.md`, `CHANGELOG.md`, `BUILD_STATUS.md`, `SOURCE_SHA256SUMS.txt`.
- **Verification:** structural source verifier and 12/12 guest-runner Python tests pass. UI stop behavior, Kotlin/Compose build, race/timeout cases and device behavior remain unverified.
- **Commit:** none; GitHub write integration is still blocked. Full M0 and product definition remain open.

## 2026-11-08 — Encrypted storage isolation slice

- **Task status:** M0.C1 / M0.C2 `[~]`. This is a data-integrity prerequisite, not completion of the vault or the product-wide persistence boundary.
- **Change:** `EncryptedVault` now uses `memo-workspace-vault.bin`; `SynthStore` now uses `memo-native-workflows.bin`. The original `synthos-vault.bin` path is treated as a read-only migration source. Existing Keystore aliases, AES-GCM parameters, the `SYV1` primary format/AAD and native JSON schema v1 are retained.
- **Safety:** existing ciphertext is authenticated with the existing key before an atomic copy; the source file is never deleted or overwritten. Unknown, corrupt or unauthenticated payloads fail closed. Reads do not generate a replacement key. The old shared file could contain only the last store to write; earlier overwritten bytes cannot be reconstructed.
- **Acceptance added:** `EncryptedStoreUpgradeTest` seeds both real encrypted stores, verifies decryption after a version-increasing signed `adb install -r`, tests both legacy-format migration directions, tests corruption/unknown-format preservation, and uses an isolated app-private test directory so it cannot mutate the actual user workspace. The CI workflow now builds and runs this test on API 35.
- **Verification state:** source edits are committed on `ysfclc-patch-1`. Android Gradle compilation, lint, instrumentation/emulator results and physical-device acceptance are not yet confirmed. Do not mark this slice complete until the CI run is inspected; do not claim phone acceptance from emulator evidence.
- **Remaining:** confirm CI; complete physical install-over-existing on the user's phone; later design and test a single authoritative transaction/event boundary without deleting or silently merging either store.


## M7 — Optional Sales Operations use-case pack (not Memo core architecture)

**Scope status:** Optional domain scenario only. It does not define Memo's core product, create a separate product direction or become an active build mandate merely because it is documented. Implement a slice only when the user explicitly selects it.  
**Product scenario:** `docs/use-cases/sales-operations/`  
**Architecture reference:** `docs/architecture/MEMO_SYSTEM_ARCHITECTURE.md`  
**Rule:** do not create another CRM. Reuse general Work entities, relationships, evidence and approved capabilities. No outbound product, cold-contact automation or channel-send workflow is authorized by this scenario; the user must separately request that exact capability. One feature slice per branch/worktree.

- [~] **M7.A1 — Optional account-research scenario (MEMO-SALES-001; partial source only, inactive until selected):** reuse `EntityRecord(type = "ORGANIZATION")`, `EntityRelationship` and `EvidenceRecord`; add a versioned account projection and deterministic validator in `:core`; retain source URL, page title, retrieval time, evidence and claim confidence; implement transparent factor-level ICP scoring with unknowns; flag possible duplicates without destructive merging; support operator accept/reject. Read-only: no contact collection and no outbound action.
- [ ] **M7.A2 — Optional qualification scenario (MEMO-SALES-002; inactive until selected):** discovery brief, confirmed-problem vs hypothesis distinction, editable qualification outcome and source-linked next action.
- [ ] **M7.A3 — Permissioned channels (MEMO-SALES-003; inactive / not authorized for current build):** preserve the scenario requirement and its safeguards, but do not implement external contact, channel sending or outbound automation unless the user separately requests that exact capability.
- [ ] **M7.A4 — Optional inbound/listening scenario (MEMO-SALES-004; inactive until selected):** connected/provider-supported or permitted public-source events only; classify and draft; no scraping or bulk unsolicited DMs.
- [ ] **M7.A5 — Optional proposal/engagement scenario (MEMO-SALES-005; inactive until selected):** prepare a scoped pilot from confirmed discovery; pricing, tax and contract terms remain human/adviser review.
- [ ] **M7.A6 — Optional delivery/reporting scenario (MEMO-SALES-006; inactive until selected):** client-authorised data boundaries, deliverables, milestones, acceptance and measured outcomes.

**P0 release gate:** CI signing isolation is documented in `docs/security/CI_CREDENTIAL_BOUNDARIES.md`. Exact-head run #678 passed isolated pinned signing and API 35 in-place upgrade; physical-device upgrade acceptance remains open.


## 2026-11-10 — Consolidated security and experience work

- [~] Native automation safety: unapproved TAP_TEXT and BACK actions fail closed; editor defaults to observation-only WAIT_TEXT; unknown action types are rejected. This is an interim guard, not the final shared one-use approval ledger.
- [~] Talk reliability: failed draft capture no longer navigates away or discards the user's draft; the user receives an explicit save error.
- [~] Signal & Graphite design foundation and unified Talk / Live / Work / Studio navigation rebuild integrated. Today priority grouping is persisted against the local calendar day and plan cards expose checkpoints, outcomes, linked entities and evidence.
- [~] Live now renders a single newest-first projection of persisted activity, job and checkpoint state, tasks, action-audit transitions and captured evidence. The core projection is deterministic, bounded to 500 entries, preserves stable record references, and avoids displaying raw audit details. Four regression tests cover ordering, legacy actions, bounded results and invalid limits; they are committed but have not run because GitHub-hosted runners still fail before job steps.
- [~] Studio now has a general private-draft path alongside Self Model generation. A core factory validates title/type/content bounds, records user-authored provenance and `requiresHumanReview`, and stores drafts as `WORK_ARTIFACT` in the existing encrypted evidence workspace. The Studio list includes generic drafts; existing edit/export behavior and revision-snapshot storage are reused. Three factory regression tests are added but unexecuted.
- [ ] Complete component migration, Gradle tests, Android lint/build and visual/device acceptance. Source integration is not proof of runtime acceptance.

## M7.A1 — Industrial account research (optional Work capability)

- [~] Domain model, evidence-backed ICP scoring, unknown-factor handling, duplicate warnings, vault-gated proposed ORGANIZATION creation and initial Work intake UI integrated.
- [ ] Add evidence capture / factor review UI, run full Gradle and ViewModel integration tests, and verify the Android experience. This is one optional Work workflow; Memo's product scope remains broader.


## 12 November 2026 — M3.8.A8 local inference timeout

- Added a 180-second deadline around serialized LiteRT inference, including engine initialization and streamed generation.
- Timeout cancels the active LiteRT conversation through `Conversation.cancelProcess()` and surfaces a dedicated failure state; it is not reported as a user cancellation or successful completion.
- Added policy regression coverage and source-verifier assertions. Deadline mapping, in-flight cancellation, success and parent-cancellation tests passed in exact-head CI #678. Native cancellation responsiveness remains a physical-device measurement.
- **Still open:** native-call cancellation responsiveness, Work-level cancel control wiring/visibility, full Gradle tests, lint, and real-device timing. The deadline is cooperative and is not a hard process kill.

- **Follow-up UI:** Work now displays a Stop generation control during an active local checkpoint, shows a bounded tail of partial output, and explains that stopping leaves the checkpoint for review rather than saving partial text as a finished artifact.


## 13 November 2026 — Backlog continuity rule

- **Owner requirement:** previously reported bugs, requested capabilities and roadmap tasks remain active when new defects are discovered. New findings add to the work; they do not defer, remove, downgrade or close earlier tasks.
- A stabilization freeze controls sequencing and release safety only. It is not a declaration that existing product work is out of scope or abandoned.
- Keep each task's implementation state separate from its verification state. Source changes, static checks, compiled builds, automated tests, emulator tests and physical-device acceptance are distinct evidence levels.
- WF-01 through WF-13 in the Memo workflow recovery plan remain active. The wider M0–M7 milestones and documented product gaps remain active in this backlog.
- Do not mark a task deferred or complete merely because a new bug has been found. Update the task with the new dependency/blocker and preserve its original acceptance criteria.


## 16 November 2026 — Fresh start and deterministic app-runtime increment

- User direction: build Memo deliberately against the agreed personal AI operating-agent vision; prioritize a useful, connected intent-to-outcome increment over disconnected screens or superficial polish.
- Baseline inspected before source changes: `fcafb582ad694926823534f768cdd509e93bd44b`; tracking PR #8 remains draft/unmerged; `main` untouched.
- **M3.8.A10 / [~]:** implemented the first concrete deterministic app capability, app.entities/entity.propose, and routed Talk's existing outcome-extraction flow through it. Each excerpt must match one original conversation message; exact validated input is stored as encrypted WORKFLOW_INPUT evidence and can be restored only after job/step/source/hash checks; output is journaled and saved as PROPOSED entities; a separate read-only Work observer verifies persisted records before the checkpoint completes. No entity is auto-approved. Exact-head CI #767 passed source verification, remote-runner, core/app tests, lint, APK identity and API 35 in-place upgrade/data-preservation gates. Physical-device acceptance remains pending.
- Added adapter/coordinator regression tests for end-to-end execution, missing source evidence, duplicate records and missing persisted proposals. Added source-verifier checks. See `docs/checkpoints/2026-11-16-deterministic-app-runtime.md`.
- Exact-head CI #767 passed source verification, remote-runner tests, core/app tests, lint, APK identity/signature and API 35 encrypted-store install-over-existing preservation. This was a pull-request validation run: the APK was assembled and emulator-tested, but the pinned signing/final APK artifact handoff is intentionally skipped for PR events. Physical-device acceptance remains pending; do not uninstall Memo or clear app data.

## 15 November 2026 — ORCH-007 shared runtime and postcondition contracts

- **Task ID/status:** M3.8.A9 / `[x]` contract package implemented and CI-verified; MEMO-ORCH-001 remains partial because concrete adapters/execution are not wired.
- **User direction:** preserve the original Memo product contract and build one coherent increment at a time. This resumes reversible branch development only; it does not authorize merge/release/deployment/data migration.
- **Commit:** `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34`.
- **Files:** `WorkflowRuntimeAdapter.kt`, `WorkflowPostcondition.kt`, their two core test files, `tools/verify_source.py`, `SOURCE_SHA256SUMS.txt`.
- **Behavior:** runtime request binds saved graph, exact TaskSpec, active attempt, capability-manifest fingerprint and current runtime/capability registry; rejects approval-gated/external-effect tasks; enforces input/output bounds, deadline and cancellation. Adapters return candidate output only.
- **Verification contract:** registered read-only deterministic observer, distinct from the producing capability; evidence binds exact attempt, TaskSpec/postcondition fingerprints, observer identity, time and separate evidence ID/hash. Only VERIFIED permits checkpoint completion.
- **Tests:** runtime manifest/attempt binding, runtime drift, adapter uniqueness, approval bypass, bounds/cancellation, saved-output receipt binding, observer registration/independence and non-verified outcomes.
- **CI:** run #678, ID `43666486553`, passed source verification, remote-runner, `:core:test`, `:app:testDebugUnitTest`, lint, APK assembly/identity, API 35 in-place upgrade and isolated pinned signing. [Run #678](https://github.com/ysfclc/memo/actions/runs/43666486553).
- **APK:** `app.memo.android.debug`, versionCode 678, versionName `4.0.0-memo-debug`; [artifact 14143185526](https://github.com/ysfclc/memo/actions/runs/43666486553/artifacts/14143185526). APK SHA-256 `026a9be7743e1e91cbc3b7807bed19504db6ea93b3d9249c91046ea15100f469`; pinned certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Not claimed:** generic dispatcher/adapter implementation, browser/native observer wiring, or physical-device acceptance. The user installed #674 but has not tested it.
- **Next:** deterministic app-runtime adapter through the checkpoint journal with shared timeout/cancellation and process-death recovery. WF-01–WF-13 and all wider milestones remain active.
## 16 November 2026 — Requested signed APK build

- User requested a fresh APK from the current stabilization source. This ledger-only commit triggers the existing push build path; it does not change application code, merge PR #8, or touch `main`.
- Required source verification, JVM tests, lint, APK assembly, emulator upgrade/data-preservation checks and isolated pinned-certificate signing must all pass before delivery.


## 17 November 2026 — Design-system consolidation and editable provider prompt

- **User outcome:** remove duplicate visual primitives and make the prompt → approval → provider journey user-editable without weakening the existing exact-content approval boundary.
- **Design-system cleanup:** commit `49fa0900fdca7ae9b28a32d13d408d4de9528ac0` migrated current screen call sites to the existing `MemoPanel`, `MemoSectionHeading` and `MemoStatusTag` primitives. Duplicate `SurfaceCard` and `SectionHeading` implementations were removed. Still-used `StatusPill`, `DotLabel` and `EmptyState` behavior was retained.
- **Prompt checkpoint:** commits `2ee5f8adce415828a9a1900e9621bcedbda64acd`, `2a7186ceec0dacdad97880866cc0921f99558251`, `ad364b3ed62a93f534cdee63f3486142cdde2b6c`, `b428bca63c09ed3ed7240532f2f5bbdf12cd61dd` and `090a2de99dacbc65cfcc0351be9333bbf00aa182` add the editable prepared-prompt checkpoint, old-approval invalidation, fresh payload hash/approval, reject-to-edit, explicit skip/cancel controls, EN/PL/TR strings, verifier coverage and the missing compile import correction.
- **Safety behavior:** edits never submit. A previous pending approval must be rejected in the existing ledger before a fresh exact-content approval is created. Editing is refused after submission or when submission is uncertain. Reject returns to editing; Skip is provider-scoped; Cancel entire run stops the full consultation. Approval/execution-gate failure after execution begins remains a stop/review path.
- **Current tested source:** `090a2de99dacbc65cfcc0351be9333bbf00aa182` on `stabilization/usable-stage-2026-11-10`.
- **CI:** [GitHub Actions run #802](https://github.com/ysfclc/memo/actions/runs/44016406556), run ID `44016406556`, completed successfully on the exact source commit. Source verification and localization coverage passed; remote-runner tests passed; `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, debug/Android-test APK assembly, APK identity/signature, API 35 install-over-existing encrypted-store preservation and isolated pinned signing all passed.
- **APK:** package `app.memo.android.debug`, versionCode `802`, versionName `4.0.0-memo-debug`; [signed artifact 14298479094](https://github.com/ysfclc/memo/actions/runs/44016406556/artifacts/14298479094). APK SHA-256: `69345f8b680feb0b7cbb59d83161561693e3f1d3f079fe1d52c288c82b224db3`. Pinned certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.
- **Upgrade proof:** API 35 emulator installed the higher-version APK over the baseline package and verified app-private data plus both encrypted Memo stores, legacy recovery and corruption handling. No uninstall or clear-data step was used.
- **Acceptance boundary:** this proves source, automated tests, emulator upgrade and signing—not physical-device behavior or real signed-in provider interaction. WF-01–WF-13 and the end-to-end prompt-to-result journey remain open until required device acceptance is recorded. PR #8 remains draft/unmerged; `main` is untouched.


## 17 November 2026 — Live browser workflow re-entry

- Live's active browser card now returns to the existing task workflow workspace instead of routing to generic browsing. The existing `BrowserSession`, prompt, approval ledger, provider page and recovery controls are reused.
- The generic top-level Browser session action remains a distinct way to start ordinary browsing.
- The active card is state-aware: “Review prompt and controls” while prompt approval is waiting; otherwise “Open workflow controls”. Polish and Turkish translations and unit coverage are included.
- The source verifier checks both the Live control and the route to `ToolSection.WORKFLOWS`.
- CI runs PR validation on open/reopen only; subsequent branch updates run the push workflow, which includes the isolated signed APK stage. This prevents duplicate unsigned PR builds from racing/cancelling signed push builds.
- Exact-head CI and device acceptance pending.


## 17 November 2026 — shared runtime dispatch boundary

- Added the first shared WorkflowRuntimeDispatcher over the existing manifest-bound request and active attempt contract.
- Talk's deterministic app.entities/entity.propose path now uses the dispatcher instead of resolving and invoking its adapter inline.
- The host timeout is calculated from the persisted attempt's original deadline, not restarted from dispatch time. The dispatcher rechecks cancellation/deadline after the adapter returns and validates the candidate output before it can be persisted.
- Approval-gated/external side effects remain rejected by the existing request contract. Persistence and independent postcondition verification remain in their existing coordinator; this increment does not claim a generic DAG executor.
- Exact-head CI and APK validation pending. Physical-device acceptance and WF-01–WF-13 remain open.

## 17 November 2026 — Four-surface navigation and route clarity

- **UIX-01 — Specialist route context:** Browser, Phone apps and Approval inbox display a shared parent-aware route strip under Live; Council returns to Talk; Models and Settings return to the originating primary surface. Android Back follows the same parent relationship.
- **UIX-02 — System destinations:** Models and Settings move into the header system menu; the separate More landing route is no longer part of the navigable shell. The four primary destinations remain Talk / Live / Work / Studio.
- **UIX-03 — Live action grouping:** Browser session, Phone apps and Approval inbox share one compact horizontally grouped action row.
- **UIX-04 — Contextual emergency stop:** the global Stop control is shown only while a model run, browser workflow, Work job or phone task reports RUNNING/active state.
- **Scope boundary:** specialist screens, workflow engine, approvals, encrypted stores and task semantics are reused unchanged. Automated and physical-device UX acceptance remain pending; do not mark these items complete without evidence.
- **Release invariant:** package app.memo.android.debug, persistent signing certificate, and encrypted data compatibility remain unchanged.

## 18 November 2026 — Work / Live navigation responsibility boundary

- **User outcome:** stop scattering execution records across the Live browser surface and the Work workspace. Work should be the place to manage workflows, browser tasks and evidence; Live should remain the place to inspect active runtimes and make live control/approval decisions.
- **UIX-05:** Work now links directly to Browser workflows, Browser tasks and Evidence. These routes reuse ToolsHubScreen and its existing runtime/data handlers.
- Live's current-browser card still opens the exact active workflow controls, but the destination is now owned by Work. Ordinary Browser session, Phone apps and Approval inbox remain Live routes.
- Browser route ownership is explicit: Browser session returns to Live; Workflows, Browser tasks and Evidence return to Work. Android Back and the visible parent route strip agree.
- Work and Live labels/descriptions are localized in English, Polish and Turkish. Shared page headers and section headings now use the language-aware text component.
- No existing capability, task, evidence record, approval, runtime, storage format, app ID or signing identity was removed or changed.
- **Status:** exact-head Actions #821 passed source verification, localization tests, remote-runner tests, 178 core tests, 43 app tests, lint (zero errors), debug APK assembly, pinned persistent signing and API 35 in-place data-preservation checks for both encrypted stores. Final artifact: Memo-Android-debug-APK, ID 14387410285, package app.memo.android.debug, versionCode 821. Physical-device visual/touch/Back acceptance remains pending; PR #8 stays draft/unmerged and main untouched.


## 19 November 2026 — AI-first intent entry correction

- Confirmed the target was already specified by the product contract and M3.A1 / M3.A3: one intent entry, with Memo deciding the needed capabilities after understanding the request.
- Removed the keyword-based split in `startTalkIntent` that created a Work plan immediately and navigated away from Talk for action-oriented prompts.
- Every new intent now opens a Talk conversation with the original prompt prefilled. The user can send it to the selected AI model before deciding whether to promote an outcome into Work.
- Removed the Talk composer checkbox that asked the user to choose a local-model workflow expansion before Memo had understood the request. Draft TaskSpec expansion remains available from Work for existing plans.
- Updated source regression guards and the product-contract source-reality row. This is an entry-point correction, **not** completion of M3.A1 / M3.A3 or the generic orchestrator. Automatic capability assembly, browser handoff, unified plan/checkpoint presentation and end-to-end execution remain open.
- Exact-head CI, signed APK and physical-device acceptance pending. PR #8 remains draft/unmerged; main untouched.

## 19 November 2026 — Talk-driven dynamic workflow planning

- Talk now treats the model response as the first step in intent understanding. After a successful reply is saved, the same explicitly selected model receives a bounded follow-up assessment and returns either `NO_WORKFLOW` or a typed `PROPOSE` graph. Ordinary conversation and one-shot answers remain conversation-only.
- The decision output is parsed by `WorkflowIntentDecisionParser`; a proposed graph is passed through `WorkflowTaskProposalParser`, then recompiled against the exact current runtime registry by `ConversationWorkflowDraftFactory`.
- Accepted proposals are restricted to the explicit read-only allowlist: local model list/generation and browser HTTPS navigation/page inspection, plus capability-free Memo scope/review nodes. Local entity/job writes and evidence-capture writes are excluded. Every target runtime must currently be available. The graph goal must match the exact original user message; the resulting Work record retains the originating conversation/message IDs, typed dependency order and checkpoint contracts.
- The Work draft is saved as `DRAFT` with every checkpoint `PENDING`, and is displayed inline in that same Talk conversation. The planner's raw JSON is suppressed from the visible model-response stream. No task is started or external effect performed by this increment.
- A remote follow-up uses only the same selected configured provider and the same explicit per-request remote-consent state as the original answer; there is no provider fallback. It receives bounded recent Talk context, not a second copy of unrelated workspace records.
- M3.A1 and M3.A3 are now partial implementation, **not complete**. A durable generic DAG dispatcher, deterministic/browser/native runtime adapters, checkpoint execution/verification, inline editing/control of plans and end-to-end journeys remain open.
- Regression tests and source guards were added. Exact-head Gradle tests, lint, APK/signing and physical-device acceptance remain pending until the new CI run completes.

## 19 November 2026 — Smart-routing product direction and working design proposal

- User-stated lifecycle: **Capture → Observe → Plan → Execute**. Do not turn classification or verification into additional mandatory user-facing screens.
- The user raised **Classify** as a possible refinement; its exact position and whether it is a distinct internal stage remain open design questions. The expanded sequence **Capture → Observe → Classify → Plan → Route → Execute → Verify → Present** is a working implementation proposal, not a fixed sequence the user explicitly approved. Validate and revise it against multiple use cases. Observation/classification/planning/routing/execution may loop and replan while preserving original intent, graph, completed work, evidence and state.
- AI and deterministic code are complementary. Working responsibility split (proposal to validate per task): AI can interpret, research, reason, plan, extract, adapt and evaluate; deterministic code can own typed schemas, capability and permission boundaries, consent, state, persistence, dispatch, budgets, checkpoints, idempotency, retries, policy and verification. The exact split is not a fixed user-approved architecture.
- Working design proposal: coordinate smart routing across suitable capabilities. A shared task/context/policy/budget/state model is one option to validate, not a user-approved requirement. Labels such as intent/task, capability, model, strategy, execution, recovery and verification are proposed internal responsibilities, not a fixed user-approved component list. Do not create separate agents or router pages solely to mirror these labels; choose the architecture based on the user's actual requirements and evidence.
- Router picks compatible tools, models and runtimes dynamically. Deterministic constraints/policy always dominate AI choices; no silent fallback, unauthorized context disclosure or unverified success.
- **Client discovery is one non-defining acceptance scenario only, not an outbound product or core architecture.** It tests whether the general orchestrator can research and evaluate relevant companies from an open-ended Talk request, preserve evidence and uncertainty, deduplicate, and present a source-linked shortlist. No outreach drafts, sending, cold-contact automation or domain-specific prospecting subsystem is implied or authorized.
- Engineering status: the four-stage lifecycle and smart-routing/AI-plus-deterministic direction are user-stated product direction. The expanded internal sequence and router decomposition are working design proposals plus acceptance scenarios, not a user-locked architecture or an implementation-complete claim. The current automatic Talk workflow triage/draft increment does not implement the full router fabric, mission-grade multi-source research, adaptive execution or verification. Add traceable implementation tasks and close them only with code + tests + CI/device evidence.

## 19 November 2026 — Permanent user-input and product-scope correction

- Added `AGENTS.md` as mandatory agent instructions and `docs/USER_INPUT_LEDGER.md` as the append-only record for user corrections, requirements, feedback, bug reports, ideas and use-case boundaries.
- One authority hierarchy now governs the repository. Memo 2 and other historical archives are comparison references only; they cannot add product requirements or reinterpret user intent.
- Client discovery is recorded as one acceptance scenario for general-purpose orchestration, not an outbound product, product pillar or default priority. Sales Operations remains an optional scenario pack; outbound/channel-send work is not authorized by the example.
- Ideas remain uncommitted until explicitly adopted. Bug reports and feedback remain active until evidence-backed closure or explicit user closure. New input must preserve original wording and receive a stable ID.
- These governance changes do not close or defer UWF-01–UWF-08, WF-01–WF-13 or any other previously reported defect/task.
- Android build/device status is not implied by these documentation changes; exact-head Android verification must be recorded separately. PR #8 remains draft/unmerged; `main` untouched.

## 19 November 2026 — current verification handoff

The exact current branch head `f62540cda08dc658309de16e037619201707f43a` passed run [#929](https://github.com/ysfclc/memo/actions/runs/44599220097). The run rebuilt and tested the committed repository source, passed source verification (434/434 static UI Text literals translated), remote-runner tests, 188 core JVM tests, 44 app unit tests, lint (0 errors; 42 warnings; 5 hints), APK/test-APK assembly, API 35 signed in-place encrypted-store upgrade, and isolated pinned signing. Signed versionCode 929 artifact: [14567058852](https://github.com/ysfclc/memo/actions/runs/44599220097/artifacts/14567058852); APK SHA-256 `9b60929f720ad8fade98dd23c03441dd0199b5c377994883d9cbe66e60635264`. This run changes no product scope and does not close physical acceptance.

**Next authorized gate remains STAB-05:** install versionCode 929 over the existing phone installation without uninstalling or clearing data, then execute the device checklist. No further broad implementation slice is authorized by this sequencing record until the real-device evidence or a concrete regression is captured. PR #8 stays draft/open; `main` remains untouched.

### New user-reported browser and control defects — 19 November 2026

These reports are part of the existing P0 workflow recovery plan. They do not replace WF-01–WF-13 and must not be lost behind the stabilization APK work.

- [ ] **BRW-14 — General-purpose browser target resolution (INPUT-2026-11-19-004):** remove the fixed Frontier catalogue as the browser runtime's architecture boundary. Keep Frontier providers as optional presets. Resolve permitted user-intended websites/targets from the actual task and registered browser capabilities; do not hardcode a domain list into intent routing. Acceptance: arbitrary supported HTTPS target can be opened visibly and inspected, with unsupported targets reported honestly.
- [ ] **BRW-15 — Selected-target navigation and execution regression (INPUT-2026-11-19-004):** reproduce why the selected-provider workflow did not open the browser despite the user reporting signed-in accounts. Trace the actual Talk/Work entry route, local-orchestrator completion/fallback, BrowserSession navigation callback, WebView load/error events, provider/session identity and workflow state. Acceptance: selecting a target visibly opens that exact target; navigation success/failure is explicit; signed-in state is checked in the actual Memo WebView session (do not assume Chrome cookies are shared); no silent no-op.
- [ ] **APR-01 — One discoverable approval control (INPUT-2026-11-19-005):** make pending external actions visible and actionable from the Action Approval Inbox and in-context execution surface, with a single consistent approval record. The inbox currently has no approve callback; the browser "Approve and send" action exists only in a conditional dialog. Preserve exact-content hash, destination, expiry, rejection, one-use consumption and audit. Acceptance: user can always locate a pending item and approve/reject it; no approval is possible for missing, stale or changed payloads.
- [ ] **CHK-01 — Separate progress, input, review and authorization (INPUT-2026-11-19-006):** audit checkpoint/status producers and all screens. Ordinary progress/read-only/local steps do not ask for authorization. Input-needed, recovery-needed and result-review states have distinct labels and next actions. Only a real external/destructive side effect creates an approval request. Acceptance: each pause states what happened, what is blocked, why user input is needed, and exactly one next action; no repeated approval for the same action.
- **Ordering:** diagnose BRW-15 and map the generic BrowserTask versus FrontierWorkflow entry points first; implement BRW-14/15 against the existing browser session/runtime seams; then APR-01; then CHK-01 and end-to-end regression coverage. Do not paper over the issue by adding more generic checkpoints, more provider hardcoding, or blanket/standing approval.
- **Scope boundary:** these are user-reported defects/feedback, not permission to turn Memo into a Frontier aggregator or remove frontier support. Memo remains general-purpose.
