# Memo 1 — Master execution runbook

**Target repository:** `ysfclc/memo` (Product 1)  
**Reference plan:** `ysfclc/memo-2/docs/TASKS.md` (Claude runbook), supported by `docs/BUILD_PLAN.md`, `docs/FEATURES.md`, and `docs/PROGRESS.md`  
**Reference TASKS blob observed:** `666fb7a030b71294ba06fe44d2f37839f4ce4430`  
**Rule:** this document is the execution contract for Memo 1. Memo 2 contributes the complete scope and lessons; Memo 1 remains its own product and codebase.

> **Important status rule:** upstream `[x]` marks from Memo 2 are not completion evidence for Memo 1. Every task below starts unverified in Memo 1 and must pass its own evidence gate before it may be marked `[x]`.

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

The primary surfaces are now Talk / Live / Work / Studio in source and CI #99. Talk has one text/voice composer and Council entry points. Live summarizes browser state, active saved work, approvals and activity, and routes to Browser and Apps. Studio is a separate primary destination. This is a first shell correction, not acceptance of the complete product.

The current Talk composer opens a conversation draft; it does not yet parse an intent into a dynamic workflow. `IntentPlanner` is keyword/template based and `CapabilityCompiler` maps domains to static lists. They are useful prototypes, not the final dynamic planner or workflow engine. Council is still a separate route. The browser workflow is provider-specific; native app automation uses a separate legacy store; the primary workspace and native workflow store are not unified. Do not describe these as one integrated agent.

**Next implementation order:** (1) physical install-over-existing and data-preservation acceptance, then safe single-store migration; (2) typed TaskSpec + dynamic versioned DAG and shared runtime/policy contracts; (3) make the Talk composer produce an inspectable workflow draft and route to the correct capability; (4) unify Live events and attach visible browser/native runtime panes; (5) integrate Council with Talk context; (6) complete end-to-end Brainstorm, CRM, Jobs, Content and arbitrary-work journeys; (7) add Today priorities and everyday voice/capture utilities. Do not spend the next increments polishing isolated entity CRUD while the central intent-to-outcome loop is missing.

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
- [ ] **M0.A3** Define versioned domain entities and invariants for Project, Note, Person, Company, Job, ContentDraft, Post, MetricSample, WorkflowTemplate, Prompt, ArchitecturalPrinciple, ContentPreference, ChannelAccount and ProviderAdapter, plus migration compatibility.
- [ ] **M0.A4** Add schema semver, workspace versioning, registry reuse/update rules and real migration (not identity-only); test round-trip, unknown versions and failed migration rollback.
- [ ] **M0.A5** Centralize policy: channel controls, destructive-action classification, user permissions, approval requirements, redaction and kill switch.
- [ ] **M0.A6** Implement deterministic routing contracts: selected target, rationale, availability, fallback and audit; routing must not bypass user settings.
- [ ] **M0.A7** Define platform interfaces for LLM, speech, vault, browser, app automation, package discovery, scheduler and notifier; keep pure core free of Android imports.
- [ ] **M0.A8** Resolve hidden/ignored core test failures and add regression coverage for all fixes.
- [ ] **M0.B1** Stabilize Android/dependency/native infrastructure, pinned toolchain and repeatable CI build.
- [ ] **M0.C1** Harden encrypted vault, key lifecycle, auth binding and secret-safe diagnostics.
- [ ] **M0.C2** Move persistence/runtime onto the vault while preserving existing Memo 1 data and key/file compatibility; test migration and recovery.
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

- [ ] **M3.A1** Local planner maps intent to a typed, bounded workflow dynamically; no hardcoded workflow per use case.
- [ ] **M3.A2** Context retrieval across permitted Memo records/sources with relevance, provenance, privacy scope and source freshness.
- [ ] **M3.A3** Connect chat and workflow state; show plans, artifacts, checkpoints and outcome evidence in the same product flow.
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
