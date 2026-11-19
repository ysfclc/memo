# Memo — Product Contract

**Version:** 1.0  
**Reconciled:** 19 November 2026  
**Product:** Memo for Android  
**Integration branch:** `main`  
**Authority:** This document records the user's accepted product decisions and is the product-level source of truth. The append-only [USER_INPUT_LEDGER.md](USER_INPUT_LEDGER.md) preserves each user correction, requirement, bug report, feedback item, idea and use-case boundary; the latest explicit user correction takes precedence over an assistant interpretation recorded here. `TASKS.md` is the engineering execution ledger. Domain specifications apply only within their explicitly approved scope and do not redefine Memo. The full social platform requirements are specified in [SOCIAL_PLATFORM_PRODUCT_SPEC.md](SOCIAL_PLATFORM_PRODUCT_SPEC.md) and remain a domain of Memo, not a replacement product.

**Delivery governance:** [../AGENTS.md](../AGENTS.md) and [../DEVELOPMENT_GOVERNANCE.md](../DEVELOPMENT_GOVERNANCE.md) define mandatory agent and change-control rules. Stable capability IDs are tracked in [REQUIREMENT_TRACEABILITY.md](REQUIREMENT_TRACEABILITY.md); material decisions are recorded in [DECISION_LOG.md](DECISION_LOG.md).

## 1. The product in one sentence

**Memo is a personal AI operating agent on the phone: it understands what the user wants, assembles and runs the right workflow across the user's information, models, websites and Android apps, produces useful work, and keeps the user in control.**

Memo is not a chatbot skin, generic browser, CRM clone, static task manager, app launcher, or dashboard of disconnected modules.

Conversation is the easiest interface. It is not the database. Browser sessions, local models, frontier AI websites, native apps and automation services are replaceable capabilities—not separate products and never competing sources of truth.

## Personal value filter

Memo exists to turn thoughts into real progress, not to give the user another system to maintain. It should understand active goals and obligations, remember unfinished work, surface concrete next actions, and help the user decide what matters today.

The daily execution view should classify work as **MUST / SHOULD / COULD / NOT TODAY**. Tasks need a concrete outcome and a clear done condition, not vague entries such as "work on project." Memo should protect time for urgent commitments, paid work, active opportunities and other user-defined priorities before optional creation or endless configuration. It should support a practical shutdown and weekly review without turning those rituals into another administrative burden.

## 2. The user's intended experience

The user should be able to open Memo and immediately say, type, share or attach what is on their mind. They should not need to decide in advance whether a request belongs in a chat, a planner, a browser, a CRM or an automation screen.

One intent entry point should lead into one continuous flow:

1. Capture the intent and its inputs (text, voice, URL, files, images or shared content).
2. Understand the desired outcome, constraints and missing information. Ask a useful question when a material ambiguity blocks safe progress.
3. Retrieve relevant, permission-eligible context from the user's Memo workspace.
4. Assemble a typed, inspectable workflow from available capabilities. Workflows are dynamic and versioned, not a hardcoded checklist selected by keyword.
5. Route each step to the appropriate participant: deterministic code, a compatible local model, an explicitly permitted frontier AI website/app, the visible browser, a permitted Android app runtime, or the user.
6. Show the plan and meaningful progress. The user can inspect inputs, model/provider, capability, destination, outputs, evidence and current checkpoint.
7. Pause at human checkpoints. Every external, destructive or otherwise consequential action requires explicit approval bound to the exact content, destination and scope.
8. Verify the actual result using observable evidence or a postcondition. A model saying “done” is not proof.
9. Save the deliverable, source provenance, decisions, events and follow-up work into the same workspace. Resume safely after interruption without replaying uncertain side effects.

No fake progress, silent network fallback, hidden listening, or opaque agent behavior.

## 2A. User-stated lifecycle and candidate internal operating model

The lifecycle stated by the user is: **Capture → Observe → Plan → Execute**. The user also raised “Classify” as a possible refinement. Whether classification is a distinct internal stage, and exactly where it belongs, remains a design question rather than a locked user decision or an extra required screen. For implementation discussion, one candidate internal loop is **Capture → Observe → Classify → Plan → Route → Execute → Verify → Present**. This is a working model to validate and revise, not a fixed sequence attributed to the user. Verification is independently required by Memo's existing safety contract before claiming outcomes. Observation, classification, planning, routing and execution may loop dynamically; the plan is living state, not a one-time static checklist.

The descriptions below are candidate implementation responsibilities for evaluating the expanded model; they do not require separate modules, agents or user-facing screens.

- **Capture:** preserve the user's original intent and receive text, voice, URLs, screenshots, files, shared content and relevant context.
- **Observe:** inspect permission-eligible workspace context, sources, websites, apps and available environment state; gather evidence before assuming.
- **Classify:** determine task type, outcome, complexity, constraints, ambiguity, risk, required capabilities, and done conditions. Do not expose classification as a mandatory user selection.
- **Plan:** compose a bounded, typed, inspectable DAG/workflow dynamically. Resolve dependencies, sequence, parallelizable work, budgets, checkpoints and verification criteria.
- **Route:** apply smart routing to select compatible model(s), deterministic functions, capability/runtime adapters and strategy. One coordinated orchestrator is a possible design, but the topology (single orchestrator, modular routers or cooperating agents) is an engineering choice to validate against real requirements. Routes can combine AI and deterministic code; AI reasoning never overrides policy or capability boundaries.
- **Execute:** deterministic orchestration owns state, permission enforcement, dispatch, persistence, idempotency, timeouts, budgets, checkpoints, retries and side-effect controls. AI can reason, extract, assess, propose, and replan within its assigned scope.
- **Verify:** inspect observable outputs/evidence and postconditions. Distinguish proposed, attempted, completed, verified, failed and uncertain states; model-generated claims are not execution evidence.
- **Present:** return the deliverable, evidence/provenance, outcome, unresolved blockers and next useful action. Keep the user informed without requiring supervision.

### Smart routing (working architecture vocabulary)

The user's requirement is smart routing across suitable AI and deterministic capabilities. Labels such as intent/task, capability, model, execution, strategy, recovery and verification describe useful internal responsibilities. They are not a fixed module map, a mandate to create separate agents/services/screens, or a user-approved architectural decomposition. Treat this breakdown as working design vocabulary that may evolve; do not attribute each named router or module boundary to the user unless explicitly approved.

One possible working design is to share a typed task, context manifest, policy, budget, graph and state across routing decisions. This is a design proposal to validate, not a user-locked architecture. A suitable system may apply deterministic constraints first, use AI judgment where ambiguity or strategic choice warrants it, and re-evaluate as observations or execution results change. A fallback route must be explicit, compatible, permission-eligible and observable; never silently switch provider or disclose context beyond consent.

### AI + deterministic engineering contract (working responsibility split)

The user's requirement is to combine AI and deterministic code. The following division is an engineering proposal to validate per task and capability, not a fixed user-approved module boundary. AI can supply flexible understanding, research, reasoning, planning, extraction, analysis, adaptation and evaluation. Deterministic code can supply typed interfaces, capability registry, policy/consent checks, schemas, state transitions, storage, routing constraints, execution, idempotency, bounded retries, verification and audit evidence. Known operations should use reliable deterministic paths; AI is not required for every step. Use both where the task benefits. No workflow may claim execution or success without verified runtime evidence.

### Non-defining acceptance scenario: client discovery (not an outbound product)

**Scope boundary:** This is one example for testing Memo's general-purpose orchestration. It is not a product pillar, domain-specific architecture, outbound product requirement or roadmap priority. It does not authorize cold outreach, bulk messaging, auto-DMs or a dedicated prospecting subsystem. Do not treat this example as more authoritative than other materially different use cases.

A request such as “Find potential clients for my automation/software skills; use Maps, Google, LinkedIn or other useful sources” can test whether Memo starts from one Talk intent and independently selects appropriate available capabilities. Memo may discover relevant companies, observe actual websites/listings/public signals, classify evidence and fit, investigate and deduplicate candidates, distinguish confirmed facts from hypotheses, and present a source-linked shortlist with verified findings, confidence, unknowns and public contact routes where available.

The scenario is not complete merely because search results were collected. Candidate existence, evidence quality, relevance and provenance must be checked. No invented contacts, pain points or false personalization. A public contact route is not consent. Contacting, messaging, submitting or otherwise acting externally is outside this scenario and requires a separate explicit user request plus the applicable approval boundary. This scenario validates the general capture/observe/classify/plan/route/execute/verify/present system; it must not dictate that system's architecture.

## 3. Four connected product surfaces

These are the primary user-facing surfaces. They are not separate applications or separate data silos.

### Talk — think and communicate with Memo

- Continuous conversations with multiple named, resumable chats.
- One fast composer supporting text, voice, URLs, files, images and Android share intake as capabilities are implemented.
- Conversation context can include selected projects, people, jobs, sources, prior decisions and artifacts, with a visible context manifest.
- Local model assistance is available without cloud dependence where the device/runtime supports it.
- Frontier models can participate through their official signed-in websites/apps in a visible, sequential workflow. Optional API-compatible providers are separately configured and require explicit per-request consent and context disclosure.
- Council is a Talk workflow: selectable participants and roles, distinct responses, critique, evidence/claim comparison, preserved disagreement and a synthesis. It must not pretend that sequential website sessions are hidden parallel calls.
- Results can be turned into durable projects, decisions, tasks and artifacts without leaving the conversation.

### Live — see and control work

- One chronological execution/session view across workflow steps and runtimes.
- Show what is active, queued, waiting, blocked, failed, cancelled, completed or uncertain; show timestamps, participant/runtime, action, target, result and evidence.
- Browser and native-app sessions are visible runtime panes within Live, not disconnected primary products.
- The user can pause, resume, cancel, take over, inspect and approve at the correct checkpoint.
- Interrupted or uncertain external work becomes `NEEDS_REVIEW`; Memo never blindly replays it.
- A status is complete only after its defined postcondition is observed.

### Work — the user's persistent world

One local-first, searchable, typed and versioned graph for:
- projects, goals, tasks, plans and follow-ups;
- people, companies, contacts, relationships and opportunities;
- jobs, applications, interviews and career decisions;
- notes, sources, evidence, research, decisions and preferences;
- workflows, skills, capabilities, prompts, providers and execution history;
- artifacts and their revisions.

Job search and CRM are workflows/views over this shared graph, not separate databases. Records retain stable IDs, timestamps, provenance, source excerpts, confidence/review state and relationships. Duplicate resolution and destructive changes must be reviewable and reversible where possible. Work also owns a practical Today view, reminders, timers, alarms, camera/share capture, contacts and driving mode. These are everyday entry points into the same context and workflow graph, not disconnected utility screens. Today uses MUST / SHOULD / COULD / NOT TODAY and outcome-based tasks.

### Studio — create real deliverables

- Private, editable, versioned artifacts generated from selected context and evidence.
- Career documents, CVs, cover letters and job-specific applications; research reports; plans and proposals; notes and other documents; content drafts and media as capabilities mature.
- Self Model is a user-owned factual profile used as an evidence-controlled source—not a generic profile dashboard.
- Every factual claim about the user must be supported by an approved fact or explicitly marked for confirmation.
- Editing, revision comparison, export and artifact provenance are first-class.
- Publishing, submitting or syncing externally is a separate action with a complete preview, destination and explicit content-bound approval. Track a verified receipt and later metrics where supported.

Settings, model management, capability management, permissions and diagnostics are supporting controls. They must not displace these four surfaces.

## 4. Core workflows that define Memo

### UC-1 — Brainstorm into a real project

Voice/text idea → retrieve relevant project and personal context → clarify the goal → optionally consult selected frontier AIs/Council → compare claims, arguments and disagreements → produce a grounded synthesis → save project/decision → create linked tasks and next actions → show the result and provenance.

### UC-2 — Capture a person into CRM

Natural-language capture or shared contact/source → resolve possible existing person/company matches → research missing details only through permitted sources → label each field with source and confidence → show the completed record and relationship → user approves → save record and a useful follow-up task.

### UC-3 — Find and apply for jobs

Discover jobs through permitted sources → deduplicate and rank against the approved Self Model and user preferences → explain fit and missing requirements → user selects an opportunity → generate factual tailored documents in Studio → review exact application and destination → explicit approval before submission → verify and save the receipt/status.

### UC-4 — Create, publish and learn from content

Capture idea/source → use the user's approved positioning and content preferences → create editable private draft/assets → review factuality and presentation → preview exact channel/destination → user approves publication → verify receipt → ingest supported performance metrics as evidence for later improvement.

### UC-5 — Arbitrary user goal

Interpret the desired outcome and constraints → compose a bounded workflow from reusable capabilities → show dependencies and unresolved requirements → execute safe internal steps → checkpoint consequential actions → verify outputs → save artifacts and learnings. This is the general product capability, not a fifth hardcoded scenario.

### UC-6 — Operate the user's social world

Connect the user's permitted social accounts → inspect actual provider capabilities and permissions → unify supported feeds, conversations and engagement in one provider-aware experience → link people and activity to the canonical Work graph → research topics/audiences/opportunities → create channel-specific content and campaigns in Studio → preview and approve each external action → verify receipts → ingest real metrics and improve future decisions. This includes account management, feed/activity, inbox/DMs, comments/replies/reactions, relationship management, discovery/listening, publishing, analytics, personal brand, customer acquisition and monetization workflows. It is not limited to content generation or publishing. See [SOCIAL_PLATFORM_PRODUCT_SPEC.md](SOCIAL_PLATFORM_PRODUCT_SPEC.md) for capability and acceptance detail.

## 5. Intelligence and execution architecture

- **Local-first source of truth:** user data, records, workflow state, approvals and evidence remain in the encrypted on-device workspace by default. Memo must remain useful for local capture, search, organization and supported local inference without a cloud account.
- **Local model as coordinator:** when available and appropriate, a compatible on-device model helps interpret intent, plan, route, structure data and synthesize. Deterministic code owns validation, permissions, state transitions, budgets, hashes, retries and verification. If no compatible local model exists, Memo must state the limitation and use a safe deterministic fallback or ask the user; it must not claim that a small model has frontier reasoning quality.
- **Frontier intelligence:** the user wants to use real frontier AI systems. Prefer visible, user-signed-in official app/site sessions for the multi-provider Council and website workflows. Optional API providers are an explicit, separately configured route, never a hidden fallback. Show the destination and exact context/data scope before remote disclosure.
- **Capability registry:** versioned schemas for tools, runtimes, skills, providers, browser/native surfaces and outputs. Reuse matching capabilities; record versions in every execution manifest. Missing, conflicting or untrusted capabilities block or narrow the plan.
- **Workflow engine:** typed dependency graph (DAG), bounded step/time/iteration budgets, durable checkpoints, pause/resume/cancel, idempotency strategy, process-death recovery and postcondition verification. The current keyword planner/static capability route is not this engine.
- **Visible runtimes:** browser and Android-app interaction must be observable, bounded and user-controllable. Browser selectors/surface schemas require versioning, drift detection, fixtures and safe takeover. Android Accessibility requires explicit permission and foreground/unlocked constraints; Memo must not imply arbitrary locked-screen app control.
- **One policy boundary:** approvals bind to exact artifact/content hash, destination, capability and scope. Changed payloads require new approval. A global stop/veto wins. Uncertain side effects enter review. Every important transition is audited.
- **One canonical data boundary:** conversations, work graph, workflow runs, approvals, evidence and artifacts must converge on a versioned encrypted workspace and migration path. Separate legacy stores are migration debt, not the target architecture.

## 6. Security, privacy and user control

- No external write or destructive side effect without explicit, current, content-bound approval.
- No secrets, private context or provider cookies in logs, source control or build artifacts.
- No hidden always-on microphone. Screen-off voice entry must obey Android foreground-service and permission rules, expose clear microphone status and a real stop control. Quick entry must degrade honestly if the OS blocks an action.
- No arbitrary app control, security bypass, password capture, CAPTCHA bypass or invisible operation. Sensitive windows and user takeover stop automation.
- Remote requests show the provider, destination, exact context scope and reason; deny/cancel is always available.
- Encrypt local workspace and credentials with OS-backed keys; backups are explicit, authenticated and recoverable. Migrations preserve existing data.
- No false success. Persist actual outputs and evidence, including failure and uncertainty.

## 7. Product and design constraints

- Android-only product target, with English, Polish and Turkish interface support.
- One coherent, polished mobile design system; responsive layouts, keyboard-safe composer, accessible touch targets, useful motion/haptics and reduced-motion support.
- Fast entry, not a maze of tabs and specialist tools. Primary surfaces are Talk, Live, Work and Studio.
- Browser, model catalogue, app automation and settings are capabilities that appear when the current task needs them.
- Every visible control must do what it says. No placeholder screens, toy browser, fake downloads/progress, invented data or “AI agent” labels around a static checklist.
- Preserve existing app identity, signing, encrypted user data and migration compatibility. Do not ask the user to reinstall for ordinary feature updates.

## 8. What Memo is not

- Not a browser-first product with a chat tab.
- Not a generic chatbot with a few CRUD screens.
- Not a dashboard whose cards merely link to disconnected tools.
- Not a static, keyword-selected checklist marketed as dynamic planning.
- Not a local-model-only product that ignores the user's explicit desire to consult frontier AIs.
- Not an unrestricted background agent that hides actions or bypasses Android controls.
- Not a SaaS requiring account signup or cloud storage to hold the user's own knowledge.

## 9. Source reality snapshot — 15 November 2026 (historical)

This is a historical source audit, not a current-branch status or device-acceptance claim. Use `docs/STATE.md` and exact-head CI evidence for current implementation and verification status; this snapshot must not be treated as the latest build result.

| Intended capability | Current Memo 1 source reality |
|---|---|
| Four primary surfaces | The active bottom navigation is Talk / Live / Work / Studio. Talk combines a text/voice intent composer, conversation history and active chat. Live has a persisted chronological timeline plus the active browser-session panel; Browser and Apps remain contextual runtime routes. Studio supports private user-authored drafts alongside Self Model and local generation. The old Home composable remains legacy source, not a primary destination. |
| One intent entry point | Every text/voice “Start with Memo” intent opens Talk first. After a successful model reply is saved, the same selected model performs a bounded follow-up triage and returns either NO_WORKFLOW or a typed TaskSpec graph. A proposed graph is parsed, recompiled against exact registered capabilities, and accepted only when every runtime is currently available and every capability belongs to the strict planning allowlist (local model list/generation or browser HTTPS navigation/page inspection; no local workspace-write capabilities). The graph must preserve the exact original user request and is stored as a DRAFT linked to its source conversation/message; its checkpoints are shown inline in Talk. The raw planner JSON is hidden from the conversation response. This is planning only: no generic DAG dispatcher, checkpoint runner, browser/native execution adapter or postcondition observer is wired. Manual response promotion remains as a fallback. The old keyword split no longer routes a new request away from Talk before understanding it. |
| Dynamic workflow engine | Work jobs persist schema-v1 typed DAG proposals. Core compiles bounded fan-out/join graphs and strictly parses untrusted proposals. Schema-7 per-node attempts and content-bound runtime output receipts exist. The shared runtime adapter and deterministic postcondition evidence contracts are CI-verified, but no generic DAG dispatcher or concrete deterministic/browser/native adapters are wired. |
| Live control | Live renders a newest-first projection of persisted activity, jobs/checkpoints, tasks, action-audit transitions, evidence and attempt/receipt state. Receipts explicitly remain NOT_VERIFIED. Cross-runtime panes, runtime-backed pause/resume/takeover and independent postcondition observers remain incomplete. |
| Frontier Council | Talk now exposes Council from the conversation list and conversation header, but Council still opens a separate route and does not yet share the Talk conversation's context and unified Live timeline end to end. |
| Browser/app runtime | A provider-specific visible WebView workflow exists with exact-content approval, capture/retry controls and a durable response-save acknowledgement before advancing. Native Accessibility automation is bounded and permission-gated. Neither is yet a generic DAG runtime adapter; live provider/device acceptance remains open. |
| Work graph | Conversations, entities, jobs, tasks, evidence, schema-7 attempts and typed runtime receipts exist. Full project/contact/opportunity/application CRUD and cross-runtime provenance remain incomplete. |
| Studio | Studio supports user-authored private drafts stored in the encrypted workspace, plus Self Model fact extraction and local-model CV-style generation. Existing artifact editing/export and revision snapshots are reused; revision comparison/restore UI, autosave, broad templates, rich export formats and publishing/metrics remain incomplete. |
| Social platform | The current contract/backlog only describe a narrow content-to-publishing/metrics journey and generic ChannelAccount/ProviderAdapter data concepts. No complete feed, inbox, engagement, listening, account management or social analytics implementation is evidenced. Treat the entire social platform spec as unimplemented until source paths and provider-backed acceptance tests prove otherwise. |
| Voice | Chat tap-to-talk delegates to Android speech recognition. Full local ASR/TTS, always-on voice and dependable lock-screen command execution are not implemented. |
| Model platform | Curated LiteRT-LM catalogue and optional API-compatible provider exist. GGUF/llama.cpp and device-wide model discovery are not integrated. |
| Data layer | Encrypted workspace and native workflow stores remain separate. API 35 emulator in-place upgrade tests pass for both stores; single-store migration and physical-device acceptance remain open. |

Exact-head GitHub Actions run #678 passed source verification, remote-runner tests, core/app unit tests, lint, APK assembly/identity, API 35 in-place upgrade instrumentation and isolated pinned signing. This does not mean the product is complete or that a physical phone has been accepted.

## 10. Build order and acceptance

1. **Protect the foundation:** keep install/update identity stable; add automated emulator install-over-existing and app-private-data preservation tests, then confirm on a physical phone; consolidate persistence with migration tests.
2. **Make Talk the real entry point:** unify capture and conversation, context selection, local/frontier routing, multi-chat and Council handoffs. A single intent must not force the user to choose “chat” versus “plan.”
3. **Build the actual orchestrator:** typed TaskSpec and versioned workflow graph, dynamic capability assembly, runtime routing, budgets, checkpoints, cancellation, audit and postconditions. Replace keyword-only static planning as the execution brain.
4. **Build Live:** one cross-runtime event timeline and visible browser/native sessions with pause, takeover, approval, resume and verification.
5. **Complete Work:** canonical graph and useful end-to-end CRM, job/opportunity and project workflows.
6. **Complete Studio:** evidence-grounded artifacts, editor/revisions/export and approved publishing flow.
7. **Build the social platform:** provider capability audit, account connections, unified supported feed/inbox, engagement, relationship graph, listening/discovery, channel-specific publishing, analytics and bounded automation. Follow the staged delivery sequence in SOCIAL_PLATFORM_PRODUCT_SPEC.md; do not ship a decorative feed ahead of real provider/data/security contracts.
8. **Make voice a first-class entry path:** foreground live voice, quick entry, local ASR/TTS and an Android-compliant lock-screen/notification/shortcut experience.
9. **Harden and verify:** unit/integration tests, CI, physical-device acceptance, privacy/security, performance and accessibility.

### End-to-end acceptance gates

- A brainstorm can become a saved project with decisions and linked tasks after a traceable multi-AI consultation.
- A contact can be captured, deduplicated, enriched with sourced fields and approved into Work.
- A job can be discovered, ranked, turned into a factual application in Studio, submitted only after approval and tracked with a verified receipt.
- A content idea can become a reviewed private artifact, be published only after approval and have real metrics captured.
- Two eligible social accounts can provide real, provenance-preserving feed/inbox data; supported replies and engagement are approved and verified; unsupported provider capabilities are explicitly disclosed.
- Social activity can link to Work contacts/opportunities; channel-specific content can be approved/published with independent receipts and evidence-backed metrics.
- An arbitrary task can compose and execute a bounded workflow with visible Live state and safe recovery.

No feature is complete because a screen or button exists. Require source changes, named tests, green CI, and physical-device evidence for Android-specific behavior.


## UC-7 — Optional domain scenario: Sales Operations and industrial account research

This is an optional, user-specific scenario pack that can exercise Memo's general Talk / Work / Live / Studio capabilities. It does not define Memo's core identity, architecture, priority order or required build sequence. The detailed scenarios and their constraints live in [docs/use-cases/sales-operations/README.md](use-cases/sales-operations/README.md).

This scenario does not authorize building an outbound product or a cold-contact automation system. The initial account-research slice is read-only and evidence-backed; any channel engagement or external contact action is outside current scope unless the user separately requests it. Preserve the scenario and its history without promoting it into the general platform architecture.


## 10 November 2026 — Unified experience implementation

The Android experience rebuild implements the first integrated foundation for the broader product direction in [MEMO_EXPERIENCE_NAVIGATION_REBUILD.md](product/MEMO_EXPERIENCE_NAVIGATION_REBUILD.md), using the [Signal & Graphite design system](product/MEMO_DESIGN_SYSTEM.md). This does not narrow Memo to sales or make social/business workflows separate products. They remain capabilities connected through Memo's shared personal/work graph, intent interface, execution timeline, approvals and studio.
