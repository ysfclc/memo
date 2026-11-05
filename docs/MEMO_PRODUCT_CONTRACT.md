# Memo — Product Contract

**Version:** 1.0  
**Reconciled:** 5 November 2026  
**Product:** Memo for Android  
**Branch:** `ysfclc-patch-1`  
**Authority:** This document records the user's product decisions and is the product-level source of truth for future implementation. `TASKS.md` is the engineering execution ledger. Older architecture drafts and inherited app implementations are subordinate where they conflict with this contract.

## 1. The product in one sentence

**Memo is a personal AI operating agent on the phone: it understands what the user wants, assembles and runs the right workflow across the user's information, models, websites and Android apps, produces useful work, and keeps the user in control.**

Memo is not a chatbot skin, generic browser, CRM clone, static task manager, app launcher, or dashboard of disconnected modules.

Conversation is the easiest interface. It is not the database. Browser sessions, local models, frontier AI websites, native apps and automation services are replaceable capabilities—not separate products and never competing sources of truth.

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

Job search and CRM are workflows/views over this shared graph, not separate databases. Records retain stable IDs, timestamps, provenance, source excerpts, confidence/review state and relationships. Duplicate resolution and destructive changes must be reviewable and reversible where possible.

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

## 9. Current source reality — 5 November 2026

This is a source audit, not a device-acceptance claim.

| Intended capability | Current Memo 1 source reality |
|---|---|
| Four primary surfaces | Current bottom navigation is Home / Chats / Work / Browser / More. Talk, Live and Studio are not yet coherent primary surfaces. |
| One intent entry point | Home offers two separate actions: “Plan & start” and “Ask in chat.” The first saves a keyword-classified, mostly static plan; the second sends a normal model prompt. They are not one orchestration flow. |
| Dynamic workflow engine | `IntentPlanner` uses phrase matching and predefined step templates. `CapabilityCompiler` maps domains to static capability lists. No general cross-runtime DAG executor is integrated. |
| Live control | Header Stop and browser/native run traces exist in separate areas. There is no single chronological Live surface joining agents, browser/app actions, checkpoints, evidence and takeovers. |
| Frontier Council | Visible sequential provider website workflows and a separate Council/model route exist. They are not yet one integrated Talk Council workflow with a shared execution timeline. |
| Browser/app runtime | A provider-specific WebView browser workflow exists. Native Accessibility automation is bounded and permission-gated, with separate legacy persistence. Neither is a universal DOM/native capability system. |
| Work graph | Conversations, entities, jobs, tasks and evidence exist. Full project/contact/opportunity/application CRUD and cross-runtime provenance are incomplete. |
| Studio | Self Model fact extraction and local-model CV-style artifact generation exist. A general artifact editor/versioning/export and publishing/metrics workflow is incomplete. |
| Voice | Chat tap-to-talk delegates to Android speech recognition. Full local ASR/TTS, always-on voice and dependable lock-screen command execution are not implemented. |
| Model platform | Curated LiteRT-LM catalogue and optional API-compatible provider exist. GGUF/llama.cpp and device-wide model discovery are not integrated. |
| Data layer | Primary Compose vault and native workflow `SynthStore` remain separate. This violates the target single-store boundary and requires a safe migration. |

The current build passing CI does not mean the product is complete. The source baseline is buildable; the product gaps above remain open and must be addressed in implementation.

## 10. Build order and acceptance

1. **Protect the foundation:** keep install/update identity stable; test install-over-existing and data preservation on a physical phone; consolidate persistence with migration tests.
2. **Make Talk the real entry point:** unify capture and conversation, context selection, local/frontier routing, multi-chat and Council handoffs. A single intent must not force the user to choose “chat” versus “plan.”
3. **Build the actual orchestrator:** typed TaskSpec and versioned workflow graph, dynamic capability assembly, runtime routing, budgets, checkpoints, cancellation, audit and postconditions. Replace keyword-only static planning as the execution brain.
4. **Build Live:** one cross-runtime event timeline and visible browser/native sessions with pause, takeover, approval, resume and verification.
5. **Complete Work:** canonical graph and useful end-to-end CRM, job/opportunity and project workflows.
6. **Complete Studio:** evidence-grounded artifacts, editor/revisions/export and approved publishing flow.
7. **Make voice a first-class entry path:** foreground live voice, quick entry, local ASR/TTS and an Android-compliant lock-screen/notification/shortcut experience.
8. **Harden and verify:** unit/integration tests, CI, physical-device acceptance, privacy/security, performance and accessibility.

### End-to-end acceptance gates

- A brainstorm can become a saved project with decisions and linked tasks after a traceable multi-AI consultation.
- A contact can be captured, deduplicated, enriched with sourced fields and approved into Work.
- A job can be discovered, ranked, turned into a factual application in Studio, submitted only after approval and tracked with a verified receipt.
- A content idea can become a reviewed private artifact, be published only after approval and have real metrics captured.
- An arbitrary task can compose and execute a bounded workflow with visible Live state and safe recovery.

No feature is complete because a screen or button exists. Require source changes, named tests, green CI, and physical-device evidence for Android-specific behavior.
