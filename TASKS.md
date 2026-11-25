# Memo 1 — Master execution runbook

**Target repository:** `ysfclc/memo` (Product 1)  
**Reference plan:** `ysfclc/memo-2/docs/TASKS.md` (Claude runbook), supported by `docs/BUILD_PLAN.md`, `docs/FEATURES.md`, and `docs/PROGRESS.md`  
**Reference TASKS blob observed:** `666fb7a030b71294ba06fe44d2f37839f4ce4430`  
**Rule:** this document is the execution contract for Memo 1. Memo 2 contributes the complete scope and lessons; Memo 1 remains its own product and codebase.

> **Important status rule:** upstream `[x]` marks from Memo 2 are not completion evidence for Memo 1. Every task below starts unverified in Memo 1 and must pass its own evidence gate before it may be marked `[x]`.

**Delivery governance:** [DEVELOPMENT_GOVERNANCE.md](DEVELOPMENT_GOVERNANCE.md)  
**Stable requirement IDs and acceptance crosswalk:** [docs/REQUIREMENT_TRACEABILITY.md](docs/REQUIREMENT_TRACEABILITY.md)  
**Material decision history:** [docs/DECISION_LOG.md](docs/DECISION_LOG.md)  

Every substantial task must reference requirement IDs and close only with evidence under the governance rules.

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

## Architecture lock — 20 November 2026

**Binding contract:** [AI Execution Invariants](docs/AI_EXECUTION_INVARIANTS.md), enforced by repository [AGENTS.md](AGENTS.md) and [DEVELOPMENT_GOVERNANCE.md](DEVELOPMENT_GOVERNANCE.md).

The user gives Memo an outcome. Talk must route every tool-using request through one provider-agnostic, model-directed execution loop and one shared dispatcher. Local/frontier models are intelligence providers; browser, Android, code, files, PDF, image, video, workspace and future operations are callable capabilities. The engine discovers actual capability availability, validates proposed actions, executes, observes, verifies and continues/re-plans. Deterministic code owns permissions, approvals, audit, budgets, cancellation, idempotency and recovery.

**Hard prohibitions:** no keyword/domain planner as the primary executor; no fixed workflow, screen, template, agent or engine for each example; no model-only text response presented as action; no registry entries without callable adapters; no treating a TaskSpec, DAG, manifest or green build as proof of execution; no duplicate stores, dispatchers, journals or approval systems.

The examples in the invariants document are cross-cutting acceptance tests for the same engine. Do not prioritize isolated entity CRUD/page polish or specialist browser flows over the missing Talk-to-execution connection unless the user explicitly asks for that work.

This is a constraint lock, not an implementation-complete marker. The agent execution loop, callable capability dispatch, durable run lifecycle and universal verification remain unimplemented until proven end-to-end.

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

The primary surfaces are now Talk / Live / Work / Studio in source and CI #99. Talk has one text/voice composer and Council entry points. Live summarizes browser state, active saved work, approvals and activity, and routes to Browser and Apps. Studio is a separate primary destination. This is a first shell correction, not acceptance of the complete product.

The current Talk composer opens a conversation draft; it does not yet parse an intent into a dynamic workflow. `IntentPlanner` is keyword/template based and `CapabilityCompiler` maps domains to static lists. They are useful prototypes, not the final dynamic planner or workflow engine. Council is still a separate route. The browser workflow is provider-specific; native app automation uses a separate legacy store; the primary workspace and native workflow store are not unified. Do not describe these as one integrated agent.

**Next implementation order under the architecture lock:** (1) finish the already-started encrypted-store emulator and physical-phone migration/install-over-existing acceptance before any store consolidation or compatibility-sensitive change; (2) define one versioned, executable capability contract and a shared dispatcher that resolves real adapters, with intelligence providers kept independent from tools; (3) connect the existing Talk composer to one model-directed execution loop that supports direct answers, one-step tool calls and dynamically composed multi-step work—the DAG may represent complex plans but is not the only execution mechanism; (4) implement the durable run journal, event lifecycle, budgets, permission checks, exact approvals, cancellation, idempotency and safe recovery; (5) connect the existing local-model, browser/frontier, Android and workspace implementations through adapters, then add file/code/document/media capabilities where real implementations are available; (6) connect Live to the shared runtime events and expose actual pause/stop/takeover state; (7) pass the cross-cutting acceptance scenarios in `docs/AI_EXECUTION_INVARIANTS.md` through the same Talk path; (8) resume secondary UI polish and broader domain journeys without creating specialist engines. Do not spend the next increments polishing isolated entity CRUD while the central intent-to-outcome loop is missing.

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
- Scope boundary: this is the typed proposal/compilation layer only. Talk still creates its current deterministic template plan; no local-model TaskSpec generation is connected yet, and no DAG executor, durable per-node run journal, pause/resume scheduler or runtime dispatch has been added. The P2 milestone remains partial until those pieces and CI evidence land.



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

### Execution safety increment — event-sink failure

- [~] **M3.A4-S1** Treat execution-event persistence/delivery failure as a hard stop. If a capability may already have started, return `NEEDS_REVIEW` and do not continue with another model decision. A regression test covers failure while recording a completed capability. Exact-head integration CI run #1149 (`45002690812`) passed on `060853bd8e2ff07cb1445980430461e048067e7f`; physical-device acceptance remains pending. This is a guardrail, not durable process-death recovery.


- [~] **M3.A4-S2** Talk transcript must render all execution event types, including approval-requested/granted/rejected. Added explicit approval lifecycle rendering and a unit test for all three event labels; exact-head integration CI run #1149 passed; physical-device acceptance remains pending.


- [~] **M3.A4-S3** Execution-event journal failure is a hard stop: if recording/delivery fails, do not issue another model decision or capability; if a side effect may have started, stop at `NEEDS_REVIEW`. Added this rule to `docs/AI_EXECUTION_INVARIANTS.md`; exact-head integration CI run #1149 passed; physical-device acceptance remains pending.

### M3 — Orchestrator and live environment

- [ ] **M3.A1** Local planner maps intent to a typed, bounded workflow dynamically; no hardcoded workflow per use case.
- [ ] **M3.A2** Context retrieval across permitted Memo records/sources with relevance, provenance, privacy scope and source freshness.
- [~] **M3.A3** Connect chat and workflow state; show plans, artifacts, checkpoints and outcome evidence in the same product flow. Talk now durably records requests, execution events and results in the existing encrypted conversation. Browser navigation/capture can surface the existing visible WebView session in Talk, but full Work/Studio/Live projection and process-death resume remain open.
- [~] **M3.A4** Local tool-using agent engine with capability restrictions, tool schemas, bounded steps, iteration/time budgets and kill switch. The provider-agnostic Talk loop uses a runtime-validated adapter registry and recalculates readiness before each model decision. Callable adapters include workspace search/private notes/proposed records plus explicitly user-supplied HTTPS navigation and visible-page evidence capture. The shared engine now requires adapter postcondition verification for success; current adapters verify bounded search results, encrypted workspace writes and settled browser state/capture. Remote browser-page text sharing is a separate per-request permission and defaults off. Approved browser prompt submission and consent-gated response capture are now integrated as M3.A4-S8/S9. Dynamic TaskSpec execution is still a proposal boundary, not an executing planner; process-death invocation reconciliation, general browser forms, Android-app/code/media capabilities, cross-runtime control and physical-device acceptance remain open.

**2026-11-20 implementation slices:** AgentExecutionEngine.kt is connected to Talk via AiRuntimeViewModel.generateAgentTurn(). AgentCapabilityRegistry checks every adapter against RuntimeRegistry ownership and approval/side-effect policy, and refreshes live readiness on every model turn. Workspace operations use the encrypted vault. The browser slice uses the same visible BrowserSession: only exact public HTTPS URLs included in the user's request may be opened; visible page text is captured without activating controls and the full source is durably saved as an encrypted EvidenceRecord. A separate opt-in is required before page text may be sent to a remote model. Talk can also open a page-derived link only after that exact HTTPS anchor appears in the latest capture, belongs to the capture's exact origin, and is re-verified against the still-visible DOM immediately before navigation. This remains navigation-only: it does not click controls, fill forms or submit prompts. Automated CI #1003 passed the foundation baseline; CI #1018 passed source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, APK identity/signature checks and emulator install-over-existing validation including preservation of both encrypted Memo stores and app-private data. The milestone remains **in progress**: browser form actions and provider prompt submission are not yet in the unified Talk loop; native Android, file/code, image/PDF/video capabilities, durable process-death invocation recovery, complete postconditions and physical-device acceptance remain open.
- [~] **M3.B1** Evidence-bearing artifact renderers and approval UX. First Talk-to-Studio slice now creates a private Markdown draft through the shared dispatcher, stores it in the encrypted workspace with source provenance and content digest, verifies read-back, and exposes it for editing/export in Studio. Remaining: general editor/revision compare/restore, PDF/media renderers and destination-bound publish approval.
- [~] **M3.A4 browser navigation extension** Talk can follow a captured same-origin HTTPS anchor after live DOM verification. Provider prompt submission is implemented under M3.A4-S8 with exact one-use approval and visible postcondition verification; consent-gated stable response capture is M3.A4-S9. General form interaction, file upload and arbitrary controls remain gated.
- [~] **M3.B2** Live tab/session view; every action has state, timestamp, rationale, result and evidence. Recent Talk runs now project a per-run timeline from the originating encrypted conversation's `AGENT_EVENT` messages, matched by the exact persisted Run ID. Live shows event timestamps and recorded state/rationale/result/evidence, plus the final response and latest evidence reference. The existing conversation remains the full trace; no second event store was introduced. Cross-runtime event projection/control, pause/resume/takeover and physical-device acceptance remain open.
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
- [ ] **M3.8.A2** TaskSpec/graph/assembler/readiness analysis: missing capability, conflicts, untrusted component and expansion limits.
- [ ] **M3.8.A3** Compile an execution package from validated capabilities; golden tests by size class; inspectable prompt/context assembly.
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
- [ ] **M4.L4** Live previews for browser, native apps, overlays and background runs; maintain one coherent session timeline.
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


## M7 — Sales Operations domain (extends Memo Work)

**Product specification:** `docs/use-cases/sales-operations/`  
**Architecture:** `docs/architecture/MEMO_SYSTEM_ARCHITECTURE.md`  
**Rule:** do not create another CRM. Reuse Work entities, relationships, evidence, planner and approval services. One feature slice per branch/worktree.

- [~] **M7.A1 — Account research foundation (MEMO-SALES-001):** reuse `EntityRecord(type = "ORGANIZATION")`, `EntityRelationship` and `EvidenceRecord`; add a versioned account projection and deterministic validator in `:core`; retain source URL, page title, retrieval time, evidence and claim confidence; implement transparent factor-level ICP scoring with unknowns; flag possible duplicates without destructive merging; support operator accept/reject. Read-only: no contact collection and no outbound action.
- [ ] **M7.A2 — Qualification (MEMO-SALES-002):** discovery brief, confirmed-problem vs hypothesis distinction, editable qualification outcome and source-linked next action.
- [ ] **M7.A3 — Permissioned channels (MEMO-SALES-003):** channel readiness, suppression and independent policy gates; public contact details never imply consent; exact payload/destination approval before any supported send.
- [ ] **M7.A4 — Inbound/listening (MEMO-SALES-004):** connected/provider-supported or permitted public-source events only; classify and draft; no scraping or bulk unsolicited DMs.
- [ ] **M7.A5 — Proposal/engagement (MEMO-SALES-005):** prepare a scoped pilot from confirmed discovery; pricing, tax and contract terms remain human/adviser review.
- [ ] **M7.A6 — Delivery/reporting (MEMO-SALES-006):** client-authorised data boundaries, deliverables, milestones, acceptance and measured outcomes.

**P0 release gate:** CI signing isolation is documented in `docs/security/CI_CREDENTIAL_BOUNDARIES.md`. The workflow's job boundaries are statically inspected; a fresh runner execution, normal signing run and physical-device upgrade acceptance remain open.


## 2026-11-10 — Consolidated security and experience work

- [~] Native automation safety: unapproved TAP_TEXT and BACK actions fail closed; editor defaults to observation-only WAIT_TEXT; unknown action types are rejected. This is an interim guard, not the final shared one-use approval ledger.
- [~] Talk reliability: failed draft capture no longer navigates away or discards the user's draft; the user receives an explicit save error.
- [~] Signal & Graphite design foundation and unified Talk / Live / Work / Studio navigation rebuild integrated. Today priority grouping is persisted against the local calendar day and plan cards expose checkpoints, outcomes, linked entities and evidence.
- [ ] Complete component migration, Gradle tests, Android lint/build and visual/device acceptance. Source integration is not proof of runtime acceptance.

## M7.A1 — Industrial account research (optional Work capability)

- [~] Domain model, evidence-backed ICP scoring, unknown-factor handling, duplicate warnings, vault-gated proposed ORGANIZATION creation and initial Work intake UI integrated.
- [ ] Add evidence capture / factor review UI, run full Gradle and ViewModel integration tests, and verify the Android experience. This is one optional Work workflow; Memo's product scope remains broader.

 
## 2026-11-21 — M3.A4-S4 — ViewModel-owned Talk execution and exact approval broker

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-APPROVAL-001, MEMO-RECOVERY-001.
- **Change:** moved the Talk execution job out of the disposable Compose destination scope into MemoWorkspaceViewModel; navigation from Talk to Live/Approvals no longer cancels the active run. Run state and stage are exposed as a StateFlow, and the global Stop control cancels the ViewModel-owned job.
- **Approval path:** the engine's onApprovalRequired callback now stages the exact runtime/capability/destination/title/payload in the existing encrypted one-use action ledger and suspends the same run. The action ID is registered with its waiter before the first suspension, avoiding a fast-approval race. Approval is accepted only for the active conversation's live Talk request and only after exact-hash approval is persisted and read back. Rejection resumes the callback without a grant; no adapter invocation follows.
- **UI:** the existing Action Approval Inbox now exposes Approve exact action and Reject and stop this action only for a pending action bound to an existing, currently executing Talk conversation and the agent-run: call marker. The app-root coroutine owns durable decisions, so switching tabs cannot cancel the read-back. Existing browser-live approval behavior remains separate.
- **Termination:** the ViewModel settles leftover pending actions as rejected and unused approved actions as NEEDS_REVIEW; an uncertain/in-flight outcome is never replayed. Process-restart behavior remains invalidation/review, not automatic run resumption.
- **Validation:** CI #1034 and #1036 stopped before Android compilation on source-verifier defects; CI #1035 and #1037 also exposed four missing PL/TR strings in the approval UI. Follow-up commits corrected the verifier, callback wiring and translations. CI #1038 passed on exact head `07f100c6`: source invariants, remote-runner tests, Android unit tests, lint, debug APK assembly, identity/signature checks and install-over-existing data-preservation. PR workflow published only `Memo-Android-Build-Reports`; APK preparation and isolated signing/publication were skipped, so no installable APK artifact is claimed. At this point the first external-write adapter, browser.submit_prompt, has been added: it binds exact destination and prompt to one-use approval, verifies the visible submission, and records encrypted evidence. Broader external controls and forms remain outside this slice; see M3.A4-S8.
- **Status:** [~] this integration slice is CI-verified; M3.A4 and overall Talk execution remain open.

### M3.A4-S5 — Exact approval review acknowledgement and durable decision ownership

- **Requirements:** MEMO-APPROVAL-001, MEMO-RECOVERY-001.
- **Change:** the approval card now exposes the complete destination and exact payload before approval is offered. The user must expand the card and tick an explicit review acknowledgement; collapsing the card resets that acknowledgement. Reject remains available without expansion.
- **Lifecycle:** approval and rejection persistence jobs now run in `MemoWorkspaceViewModel.viewModelScope`, surviving tab changes and Activity recreation. The inbox composable owns no decision job.
- **Tests:** added PL/TR acknowledgement translations and localization assertions; source verifier guards the review checkbox, full destination display and ViewModel-owned decision scope.
- **Validation:** S5 exact head a6ed188c passed CI #1039: source verifier, remote-runner tests, Android core/app unit tests, lint, debug APK assembly, identity/signature and install-over-existing preservation. The PR workflow still skipped APK packaging/re-signing and published diagnostics only.
- **Status:** [~] source implementation committed; validate exact-head CI before marking complete.

 
### M3.A4-S6 — Encrypted durable Talk run record and restart recovery

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-RECOVERY-001.
- **Change:** added AgentRunRecord to the existing encrypted AppSnapshot and bumped workspace backup schema to 7 with additive schema-1–6 import compatibility. Each record retains run/conversation identity, goal, model, lifecycle status, last event/stage, capability counts, pending approval call, evidence reference and final/recovery message.
- **Execution identity:** Memo creates the run ID before engine startup and supplies that exact ID to AgentExecutionEngine.executeWithRunId; approval requests and every event therefore point to the same durable record.
- **Journal boundary:** the run record transition and its TOOL transcript message are written together to one encrypted snapshot, reopened and verified before the engine continues. Engine terminal events enter `FINALIZING`; the final response and terminal run status are then committed together. If that second write is interrupted, the still-active `FINALIZING` record is recovered to `NEEDS_REVIEW` rather than appearing completed without its final answer.
- **Restart behavior:** unlock converts RUNNING, WAITING_FOR_APPROVAL, TOOL_RUNNING and FINALIZING records to NEEDS_REVIEW, clears the approval call reference and records that no step was replayed. Existing action ledger recovery still invalidates/flags pending or in-flight external actions. Completed history remains unchanged.
- **Live visibility:** Live now shows recent Talk runs, their actual persisted status, last recorded stage, call counts, recovery reason, and links back to the originating conversation and approval inbox.
- **Tests:** deterministic-clock event-reducer tests, finalization crash-window/recovery tests, schema-v7 round-trip/schema-v6 import compatibility, Council backup schema-version assertion, caller-supplied run-ID engine test, PL/TR Live labels and source-verifier guards. The obsolete non-durable Talk launcher is removed.
- **Validation:** CI run `44901392061` on source commit `adf58109af909358940e7361251669377e2a84d5` subsequently completed with all observed jobs successful, including source/runner checks, core and app unit tests, lint, debug/instrumentation assembly, APK identity/signature and install-over-existing data preservation. This supersedes the earlier exact-head pending note for S6; physical-device acceptance remains open. PR-event APK publication/re-signing was skipped, so no release APK is claimed.
- **Known gap:** Talk now exposes browser.submit_prompt as its first approved external-write adapter, but general page controls, form filling, provider response polling and native-app actions remain unavailable. This milestone makes the execution lifecycle durable/visible; it does not imply all external actions are executable. Physical-device behavior remains unverified.
- **Status:** [~] source implementation pending exact-head CI and device acceptance.


### M3.A4-S7 — Shared capability postcondition verification

- **Requirements:** MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-LIVE-001.
- **Change:** the provider-agnostic engine now requires an explicit adapter verifier before accepting `SUCCEEDED`. Verification is bounded by the existing tool timeout. Only verified calls increment successful-call counts and emit `TOOL_FINISHED`; verified summaries enter the model observation.
- **Fail-closed behavior:** a missing, failed, timed-out or evidence-mismatched read-only postcondition is recorded as `FAILED` and permits a new model decision. For local/external/destructive writes, the same uncertainty stops at `NEEDS_REVIEW`. Write verification must return the exact evidence reference produced by the invocation.
- **Current adapters:** local search deterministically recomputes its bounded output/source IDs; note, Studio artifact and proposed-record adapters read back exact encrypted workspace records; browser navigation verifies the settled visible URL/title; page capture reads back the encrypted record and matches source/content/link digests; observed-link navigation verifies the settled current page; approved browser prompt submission binds the exact destination/prompt to the one-use action ledger, verifies the visible submission and completes the ledger only after its evidence postcondition; consent-gated stable response capture binds to that completed submission and persists untrusted response evidence. Stale browser capture state is cleared before a new navigation attempt.
- **Regression tests:** missing read-only verification (failed/re-planned), missing local-write verification, mismatched write evidence, cancellation during write invocation and postcondition verification, and write-adapter `FAILED` (all review-stop/no-replay cases); existing sequential, approved-write and event-journal-failure tests supply verifiers.
- **Compatibility:** no AppSnapshot/backup schema, encryption, signing, app identity or external-action policy changes.
- **Validation:** PR exact-head CI run `44907881554` passed on `bfe40a182b1e283699a8c4c36bf9c612f83981c7`: source verifier, remote-runner tests, core/app unit tests, lint, debug/instrumentation assembly, APK identity/signature and install-over-existing data preservation. No local Gradle/Android SDK checkout; release signing skipped, so no canonical release APK is claimed.
- **Status:** `[~]` source implementation and exact-head CI verified; physical-device acceptance pending.


### M3.B1-S1 — Talk-created private Markdown artifacts

- [~] Added `workspace.save_artifact` to the same provider-agnostic Talk capability registry and dispatcher. It accepts a bounded title, descriptive artifact type and Markdown content; artifact type is metadata only and does not select a specialized workflow.
- Drafts are saved as `STUDIO_ARTIFACT` in the encrypted workspace with source conversation/message IDs, source excerpt digest, content digest, idempotency/run/call metadata, version 1 and `DRAFT` review status. Exact artifact, digest and provenance are read back before the engine can report success.
- Studio now lists these drafts. The existing Evidence dialog opens them for editing with revision history, copying and Markdown export. The source capture/evidence immutability boundary remains intact.
- Supported output in this slice: private Markdown text only. No filesystem code execution, PDF/image/video generation, external publishing or submission is claimed.
- Validation: `StudioArtifactPolicyTest` covers normalization, bounds and digest binding; `AgentCapabilityRegistryTest` covers the local reversible runtime policy; source verifier guards adapter, postcondition, editability and Studio listing. Exact-head CI run `44953421742` on commit `ae6ef31976d7f35339715a16933ec73e21210af5` passed source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, package/signature checks and emulator install-over-existing data preservation. Physical-device acceptance remains pending. The later integrated exact-head run #1149 (`45002690812`) also passed this artifact path alongside the browser prompt/response additions.


### M3.B1-S2 — Verified user-directed Studio file export

- **Requirements:** MEMO-STUDIO-001, MEMO-ARTIFACT-001, MEMO-CAP-001.
- **Outcome:** A Talk-created private Studio draft can become a real Markdown file at a destination explicitly selected by the user, and Memo only reports a verified export after reading the destination back.
- **Implementation:** Add a reusable core export policy with a small text MIME allowlist, safe filename generation, UTF-8 byte preservation, a 1 MiB bound and SHA-256 preparation. The Studio export flow writes to the URI returned by Android's CreateDocument picker, reopens that same URI and verifies the exact bytes against the expected digest.
- **Safety:** No arbitrary filesystem paths, background export, overwrite of user-selected existing files outside the picker contract, remote transfer or new plaintext app storage. Export remains a user-initiated action. A write/read-back error or digest mismatch is reported as unverified; Memo must not claim success.
- **Tests:** MIME allowlist, filename sanitization, UTF-8 preservation, size bounds, exact digest match, truncation/modification rejection and fail-closed oversized/unreadable verification.
- **Status:** Source implemented; exact-head GitHub Actions run `45002690812` (run #1149) passed on commit `060853bd8e2ff07cb1445980430461e048067e7f`, including Android unit tests, lint, debug APK assembly, signature/identity checks and emulator install-over-existing data preservation. Physical-device acceptance remains pending. The documentation-updated PR head `a7eb5e5787b70f7517dbbe5acf490e71da52ac52` also passed Actions run #1150 (`45003595826`). This is a verified Markdown export slice, not general filesystem access or PDF/binary generation.


### M3.A4-S8 — Approved browser prompt submission

- [~] Added `browser.submit_prompt` as the first external-write adapter in the shared Talk dispatcher. It is registered as `EXTERNAL_WRITE` and cannot execute without explicit approval of the exact HTTPS destination and exact prompt payload.
- After approval, the adapter recomputes the content hash, durably consumes the one-use action grant, rechecks the visible page, fills the visible text composer, clicks the send control, and verifies that the exact prompt appears as a submitted message and the composer is cleared.
- The adapter saves an encrypted `BROWSER_PROMPT_SUBMISSION` evidence record containing the exact prompt, prompt digest, page snapshot digest, source conversation/message provenance and action ID/hash. The shared postcondition verifies the evidence and completes the action ledger only after read-back.
- Any page change, uncertain send, cancellation, evidence persistence failure or action-ledger mismatch stops at `NEEDS_REVIEW`; the prompt is never automatically replayed.
- Scope boundary: no arbitrary page controls, form filling, file upload or native Android actions in this slice. Stable response capture for the submitted prompt is available only when the user separately enables browser-page sharing consent.
- Validation: source verifier and runtime approval regression coverage added. Historical CI run 44954196829 on commit 23073011b9c411945c12531ce8483ac0a6d2b4b5 passed. The later integrated exact-head run #1149 (`45002690812`) on `060853bd8e2ff07cb1445980430461e048067e7f` also passed source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, package/signature checks and emulator install-over-existing data preservation. Physical-device acceptance remains pending.


### M3.A4-S9 — Consent-gated stable browser response capture

- [~] Added `browser.capture_response` to the Talk tool set only when page content is permitted for the selected model (local-model processing stays on-device; remote providers require separate per-request consent). It requires the exact evidence ID from the previously approved `browser.submit_prompt` call and verifies that the submission action is already COMPLETED.
- Uses the browser's submission baseline to detect a new assistant response, not an older answer. The response must be new, stable across repeated polls and associated with the same-origin page; lost baseline, unsupported page structure or timeout stops for review and never resends the prompt.
- Saves the response as encrypted `BROWSER_PROMPT_RESPONSE` evidence with prompt-submission reference, prompt/response/page digests, source conversation/message provenance and run/call IDs. Studio exposes both the approved prompt and response under Browser research evidence. The postcondition reads back the exact response and source binding before returning it to Talk as explicitly labelled untrusted external content.
- This adapter is a local evidence write, not a new external side effect; runtime policy does not require external-action approval. It is not advertised when page-sharing consent is disabled.
- Scope boundary: provider-specific response detection is bounded to the existing stable-response script; general page controls/forms, uploads, native apps and arbitrary page-event subscriptions remain open.
- Validation: runtime policy regression and source-verifier guards added. Historical CI run 44954878877 on commit bac787645ee85958132d56e8524c53325b536c06 passed. The later integrated exact-head run #1149 (`45002690812`) on `060853bd8e2ff07cb1445980430461e048067e7f` also passed source verification, remote-runner tests, Android unit tests, lint, debug APK assembly, package/signature checks and emulator install-over-existing data preservation. Physical-device acceptance remains pending.

### 2026-11-22 — M3.B2-S1 — Per-run Live execution timeline

- **Requirements:** MEMO-LIVE-001, MEMO-ORCH-001, MEMO-RECOVERY-001.
- **Files changed:** `app/src/main/java/app/memo/android/ui/LiveWorkspaceScreen.kt`, `app/src/main/java/app/memo/android/ui/MemoLocalization.kt`, `app/src/test/java/app/memo/android/ui/LiveAgentRunTimelineTest.kt`, `docs/REQUIREMENT_TRACEABILITY.md`, `IMPLEMENTATION_LOG.md`, `TASKS.md`.
- **Implementation:** Live correlates durable `AGENT_EVENT` transcript messages to one `AgentRunRecord` using the exact Run ID field in the final metadata line and the originating conversation ID. It sorts by persisted message timestamp and shows the latest four events with their saved rationale/status/evidence text, plus the run's final response, latest evidence reference, start time and update time. The originating Talk conversation remains the full trace.
- **Compatibility:** read-only projection over the existing encrypted workspace snapshot. No new event store, workspace schema, backup schema, encryption/key alias, package identity or signing configuration.
- **Tests:** exact run-ID isolation (including other metadata on the same line), source-type filtering, chronological ordering, missing-conversation behavior and Polish/Turkish timeline-label translations.
- **Verification:** GitHub Actions exact-head validation pending; no local Android SDK/Gradle checkout or physical device was available in this run. This is a partial M3.B2 slice, not cross-runtime Live completion or release acceptance.
- **Remaining:** cross-runtime session/event projection, pause/resume/takeover and global stop/action gate; real-device Talk/Live acceptance remains open.

### 2026-11-25 — M3.A4-S10 — Preserve user drafts and prove exact browser submission

- **Product outcome:** an approved Talk action must never overwrite an unrelated draft, send text other than the approved prompt, or report success because some unrelated page text changed.
- **Preparation:** the visible WebView composer is inspected before mutation. If it contains different text, Memo leaves it untouched and stops for user review. An already-present exact prompt may proceed without rewriting the composer.
- **Send boundary:** the composer must equal the normalized approved prompt before the send control can be clicked; substring matches are rejected. The Talk adapter now surfaces the script's actual failure explanation, so a preserved draft is clearly reported rather than hidden behind a generic composer error.
- **Verification:** success now requires both an empty visible composer and a newly observed user-message node whose complete normalized text exactly equals the approved prompt. Page-wide prefix occurrences and a generic increase in user-message count are not sufficient. Unsupported provider DOM structures fail closed for manual review.
- **Regression coverage:** added BrowserPromptSubmissionSafetyTest and source-verifier assertions for draft preservation, exact composer equality, new exact-message detection and the empty-composer postcondition.
- **Compatibility:** no encrypted workspace schema, backup schema, package ID, signing key, approval-ledger format or persisted user data changes.
- **Verification state:** source edits are on memo/mission-1-safe-browser-submission; exact-head GitHub Actions unit tests, lint, source verification and emulator upgrade checks are pending. Physical-device acceptance remains pending. Do not mark this slice complete until CI is green and the actual phone flow is checked.
- **Remaining boundary:** this hardens the existing approved browser prompt path only. It does not claim general browser controls, forms, uploads, native-app actions or universal workflow execution.

### 2026-11-25 — M3.RECOVERY-S1 — Fail-closed continuation eligibility and durable lineage

- **Requirement:** MEMO-RECOVERY-001, supporting MEMO-ORCH-001 and MEMO-AGENT-001.
- **Change:** added a pure core policy that permits continuation only for NEEDS_REVIEW runs with a valid conversation/goal, no pending approval reference, consistent capability counters, and no capability invocation without verified completion.
- **Durability:** added optional parentRunId lineage to AgentRunRecord and the existing schema-7 encrypted workspace snapshot. The field is included in backup validation, size estimation, JSON serialization and deserialization. Schema version remains 7; older schema-7 backups without the optional field import with an empty parent ID.
- **Safety:** eligibility is not authorization. Prior tool-call IDs, idempotency keys and approvals are never reused. A new run must be created only after the ViewModel rechecks the parent record. No automatic replay is introduced.
- **Tests added:** AgentRunContinuationPolicyTest covers allowed/blocked states, uncertain invocations, approval references and invalid counters. WorkspaceSchemaV3Test covers lineage round-trip, old schema-7 import and self-parent rejection.
- **Verification:** exact-head CI pending; no local Gradle/Android SDK checkout or physical-device test was available in this session. No APK is claimed.
- **Status:** [~] core policy and persistence slice implemented in source; ViewModel/UI continuation and exact-head/device acceptance remain open.

