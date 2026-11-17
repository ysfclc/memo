# Memo — Reorientation, Recovered User Feedback & Locked Execution Baseline

**Date:** 17 November 2026  
**Repository:** `ysfclc/memo`  
**Working branch:** `stabilization/usable-stage-2026-11-10`  
**Status:** **Product direction locked; implementation and device acceptance remain in progress.**  
**Authority:** This is a dated companion to `docs/MEMO_PRODUCT_CONTRACT.md`, not a replacement. The Product Contract and `docs/SOCIAL_PLATFORM_PRODUCT_SPEC.md` remain authoritative for product scope. `TASKS.md` remains the engineering execution ledger.

## 1. Why this document exists

The user reported feeling lost about Memo's vision, architecture, progress and direction. A review of prior feedback, product decisions, source audits, workflow recovery records and the current repository found that the ambition had not disappeared. The problem is that implementation slices, technical subsystems and isolated fixes had obscured the connection between the user's intended operating model and the experience Memo actually delivers.

This document locks the recovered interpretation so future sessions do not repeatedly reinterpret the product, reduce it to the latest implementation, or mistake technical work for user-visible progress.

It records four different kinds of information separately:

1. **Product decisions:** what Memo must be, based on the user-approved Product Contract and explicit direction.
2. **Reported feedback and failures:** what the user said was frustrating or missing, especially in prompt feeding and browser workflows.
3. **Existing implementation evidence:** what source, tests and CI currently establish.
4. **Acceptance still required:** what remains unproven, incomplete or awaiting real-device use.

A source file, screen, old task checkbox or passing CI run is not proof that an entire user journey works.

## 2. Locked product definition

**Memo is an Android-first personal AI operating agent: it understands what the user wants, assembles and runs the right workflow across the user's information, local intelligence, frontier AI systems, websites and Android apps, produces useful work, and keeps the user in control.**

The operating model is:

- **Local AI is the brain** when a compatible on-device model is available: it helps understand intent, retrieve and organize context, plan, route work, prepare instructions and synthesize results.
- **Authorized applications and browser sessions are the hands:** Memo can operate through the user's permitted, visible sessions and supported Android interfaces.
- **Deterministic Memo code is the control plane:** it owns schemas, capability discovery, permissions, state transitions, budgets, hashes, idempotency, approvals, audit, recovery and verification.
- **The user remains the authority:** Memo cannot grant itself new access or approval. Consequential actions require the required human checkpoint.

Using ChatGPT, Claude, Gemini or another frontier service through its normal signed-in website/app is a distinct route from calling that provider's inference API. The preferred multi-provider workflow uses authorized, visible website/app sessions where appropriate. Direct API integrations are optional, separately configured and explicitly disclosed; they are never a silent substitute or automatic fallback.

Memo is not a chatbot with browser shortcuts, a collection of hardcoded automations, a local-model-only app, a generic browser, or a dashboard of disconnected features.

## 3. The four product surfaces remain locked

The product has four connected user-facing surfaces. Browser, models, automation, skills, providers and capability management are supporting capabilities, not additional competing product shells.

| Surface | Responsibility | Required relationship |
|---|---|---|
| **Talk** | Intent, conversation, voice, prompt preparation, questions and follow-ups | The natural-language entry point into all work; no need to choose a specialist screen first |
| **Live** | Runtime visibility and control | The execution room for browser, Android, model, connector and user checkpoints |
| **Work** | Durable user-owned context and execution records | One searchable, encrypted graph for goals, tasks, records, sources, evidence, workflows, providers, runs and relationships |
| **Studio** | Useful outputs and deliverables | Editable, versioned, evidence-grounded artifacts; publication/submission is a separate approved action |

The same task, context, run identity and evidence must remain connected across all four surfaces. Opening a browser or Android app must not create a disconnected second workflow or cause the user to lose the active task.

## 4. Recovered feedback — prompt feeding and context

The user's intended prompt workflow is not merely “send my text to an AI website.” Memo must manage the complete journey from intention to usable result.

### 4.1 Required prompt lifecycle

1. Capture the user's original intention and preserve it unchanged.
2. Understand the intended outcome, constraints, audience, requested format and material ambiguities.
3. Retrieve permitted, relevant workspace and source context. Do not send the entire workspace by default.
4. Assemble an inspectable context manifest containing selected records, excerpts, files, timestamps, provenance, privacy labels and unresolved conflicts.
5. Prepare a task-specific prompt or instruction package. Prompt construction must be reusable, versioned and testable; it must not be a hardcoded prompt tied to one use case.
6. Show the original request and the proposed enhancement side by side. Explain material additions, assumptions and transformations.
7. Let the user edit, regenerate, accept or reject the enhancement. The original remains recoverable.
8. Identify the exact participant and route: local model, configured API provider, signed-in browser/app provider, another capability, or the user.
9. Show the exact destination, account/session where relevant, context disclosed and final payload.
10. Obtain the appropriate approval before external disclosure or a consequential action. Any material change to payload, destination, context scope or action invalidates the previous approval.
11. Feed the exact approved payload through the chosen runtime and record what actually happened.
12. Capture the result, preserve raw evidence and make it available for editing, comparison, follow-up and synthesis.

### 4.2 These records must not be conflated

- Original user intention.
- Clarifications and constraints.
- Retrieved context manifest and source references.
- Workflow/TaskSpec and its revision.
- Prompt template/skill version.
- Enhanced prompt revision(s).
- Exact approved outgoing payload and its hash.
- Participant, provider, runtime, account/session and destination.
- Approval and audit events.
- Raw captured provider response and capture method.
- Editable working copy and its revisions.
- Final synthesis/deliverable and its supporting evidence.

The raw response must remain recoverable when the user edits a working copy. A final synthesis must not overwrite the individual participant responses.

### 4.3 Multi-provider/Council behavior

Council is a Talk workflow, not a separate intelligence silo. It may coordinate local participants, configured API participants and browser-connected participants, but the runtime for each participant must be truthful and visible.

Each participant receives only its approved, relevant context and instructions. Responses, sources, disagreements, failures and follow-up rounds remain distinct. A synthesis is a separate result. Sequential website sessions must be shown as sequential; Memo must not imply hidden parallel API access or represent a browser login as API credentials.

## 5. Recovered feedback — workflows and automation

The user wants Memo to construct and operate reusable workflows, not merely execute a list of static steps selected by keywords.

### 5.1 Workflow definition versus execution

These are separate durable objects:

- **Workflow definition/template:** reusable, editable and versioned instructions, graph, capability requirements, inputs, outputs, policies and postconditions.
- **Execution run:** one actual attempt to perform a goal using a pinned workflow/capability/context version, with its own identity, state, timestamps, events, outputs, failures and evidence.
- **Standing automation/rule:** an explicitly enabled trigger and bounded policy that may initiate future runs when a defined event or condition occurs.

A template is not execution history. Opening a previous run must never silently restart it or resend an external action.

### 5.2 Dynamic workflow composition

A workflow must be composed from reusable, versioned capabilities based on the requested outcome, not selected from a hardcoded list of use cases.

The composer/planner must:

- clarify material ambiguity and identify missing information;
- construct a typed dependency graph (DAG), including branches, joins and conditional paths where needed;
- identify required inputs, outputs, preconditions, postconditions and evidence;
- discover capabilities that actually exist and are currently available;
- distinguish unavailable, incompatible, untrusted, permission-blocked and merely unverified capabilities;
- select reusable skills/procedures without treating a skill as a permission grant;
- choose the appropriate runtime for each step;
- explain capability selection and why a step is routed to a particular model, website, app or deterministic handler;
- calculate and enforce bounded step, time, iteration, model-call, resource and external-action budgets;
- pin workflow, skill, tool, provider, model, surface-schema and capability versions in the run manifest;
- present the plan and unresolved gaps before execution where meaningful.

The plan should be understandable and editable. The user should not have to author every low-level automation step manually, but can inspect and change what Memo proposes.

### 5.3 Durable execution and recovery

Every run needs durable state and a truthful state machine. At minimum, distinguish draft, ready, queued, running, waiting for approval, waiting for user, paused, blocked, failed, interrupted, needs review, cancelled, partially completed and verified/completed.

Each step needs its own state, attempt identity, dependencies, runtime, input/output references, timestamps, bounded retry policy, idempotency strategy, checkpoint and postcondition.

Required controls have distinct meanings:

- Pause: suspend further execution while retaining the run.
- Resume: continue only from a safe, persisted state.
- Take over: hand interaction to the user without abandoning task state.
- Retry detection/observation: inspect the current state again; do not repeat the external action.
- Retry step: start a new safe attempt, with duplicate-risk handling where relevant.
- Skip step/provider: skip only when the state makes that safe; preserve previous successful outputs.
- Stop step: stop the current step and record whether its external outcome is known or uncertain.
- Cancel run: cancel the remaining run as a distinct, explicit operation.
- Needs review: preserve uncertainty after a process death, timeout or ambiguous external side effect.

No automatic retry may blindly replay an uncertain send, payment, submission, publish or other external side effect. Completed results must survive a later step's failure. Cancellation and global veto/stop must dominate model or workflow requests to continue.

### 5.4 Standing automations

Standing automations are not just long-running linear workflows. They need explicit trigger registration, source/account scope, conditions, cooldowns, rate and resource limits, quiet hours, retention, notification rules, opt-in state, audit and a global stop. Their trigger logic should create traceable workflow runs; each run then uses the same safe execution engine.

Read-only monitoring, internal preparation and externally visible actions are separate permission classes. External sends, comments, follows, publishing, deletion and other consequential operations remain approval-gated by default. Any remembered authorization must be narrow, visible, expiring/revocable and auditable.

## 6. Recovered feedback — browser automation

The browser must be a genuine, visible runtime attached to the active Memo task, not a separate product that takes over the application and strands the user's work.

Required behavior:

- Keep provider/site sessions isolated and use normal authorized sign-in. Do not bypass login, MFA, CAPTCHA, provider restrictions or site policies.
- Keep the active task, plan, original/enhanced prompt, participant, approval, run status, captured responses and recovery controls available while the browser is open.
- Support an optional larger/full-screen page view without losing the task context or an obvious return/control path.
- Inspect accessible page structure and identify meaningful semantic elements, forms, lists, pagination, editors and repeated items.
- Let the user select and record web elements. Produce an inspectable, reusable recipe/schema with unique locators, expected conditions, inputs/outputs and postconditions.
- Generalize repeated items only when evidence supports it; show counts and ambiguity rather than inventing a locator.
- Version browser surface schemas and selectors. Detect page/layout drift, changed destinations, changed login state, hidden/ambiguous controls and unexpected page navigation.
- Use bounded fixtures for forms, pagination, shadow DOM, contenteditable and streaming AI chat, login walls and selector drift.
- Offer human takeover when the page is ambiguous, the user needs to sign in, or the automation cannot safely continue.
- Treat website content and model-generated instructions from websites as untrusted data; page content cannot change Memo policy or grant permissions.
- Make response capture resilient: automatic DOM detection is best-effort, never the only recovery path.

For a provider response, Memo must offer manual paste/select/review capture when automatic detection fails, with capture provenance and a durable link to the original run/step. Retrying detection must never resend the prompt. If a send might have happened but cannot be confirmed, record an uncertain state and require review rather than automatically submitting again.

A locator that stops working must not silently be “self-healed” into a different action. Memo may propose a quarantined replacement, explain the difference, test it against a fixture or safe observation, and ask for approval before promoting a changed recipe.

## 7. Recovered feedback — native Android app automation

Memo should interact with the actual installed Android application where the user has authorized that capability. This is not the same as embedding an arbitrary third-party app screen inside Memo.

Required behavior:

- Discover installed/launchable apps and build a capability inventory based on observed and verified facts, not package-name assumptions.
- Start or resume the actual target app and verify its identity before actions.
- Observe the app's exposed UI semantics and let the user select/record relevant elements.
- Build reusable, versioned app capability maps/recipes with preconditions, input/output schemas and observable postconditions.
- Provide a native-app picker/recorder, tested first against a controlled fixture app.
- Show the current target, selected element, proposed action, execution trace and verification state in a visible Memo console/HUD.
- Keep the user able to take over. Direct user interaction with the target should pause automation promptly.
- Provide a safe return path to Memo with the task and run intact.
- Fail closed when the app identity, surface, selector or action is ambiguous or hidden.
- Respect Android permission boundaries and the limitations of Accessibility, overlays, protected windows, custom-drawn controls and background execution. Do not imply arbitrary app control, locked-screen control or access to another app's private data.
- Use screenshot/OCR as a bounded fallback only when semantic/accessibility information is insufficient and the action can still be performed safely.
- Never automate password/secret capture, MFA/CAPTCHA/security bypass, payment confirmation, ad/reward engagement or other prohibited sensitive actions.
- If the app leaves the foreground, the device locks, permission is revoked or the user takes over, pause/stop safely and record the actual outcome.

Accessibility is one Android-specific adapter. It must not become the entire Memo automation architecture. Official APIs, supported deep links/intents, app-provided interfaces and other documented system capabilities should be evaluated where appropriate, while preserving the same capability, permission, run and verification contracts.

## 8. Common automation architecture

Browser and native-app automation should share the same higher-level contracts while retaining different runtime adapters and surface-specific evidence.

A capability should declare:

- stable ID and version;
- purpose and supported runtime/surface;
- typed inputs and outputs;
- preconditions and postconditions;
- required permissions/scopes and risk class;
- read-only versus internal-write versus external-side-effect classification;
- resource/step/time limits;
- idempotency and retry semantics;
- cancellation and interruption behavior;
- observability/evidence contract;
- fixtures/evaluators and last-verified compatibility information.

A skill/procedure describes reusable know-how and constraints; it is not a tool implementation, runtime, role or permission. A tool performs a concrete operation. A capability describes an allowed operation independently of which compatible tool/runtime provides it. An agent/role coordinates responsibilities but does not receive implicit authority. A model is replaceable and cannot override deterministic policy.

The runtime adapter returns bounded candidate output. Memo's coordinator validates and persists it. A separate, deterministic, read-only observer verifies the declared postcondition. Only independently **VERIFIED** evidence may complete the step. A model's text, a successful button click or a saved-output receipt is not proof that the user's goal was achieved.

## 9. Recovered prompt-to-result failure register

The following feedback must remain active until the complete user journey is accepted. The detailed source audit and recovery plan remain the acceptance source for these IDs.

| ID | Recovered issue / required outcome | Current closure rule |
|---|---|---|
| UWF-01 | Council and browser-connected frontier participants are disconnected; runtime types and availability must be truthful | Selectable only through a supported, visible route; runtime identity shown |
| UWF-02 | Browser is a separate destination that loses active task context | Browser is a task-attached runtime panel; opening/collapsing/reopening preserves state |
| UWF-03 | Workflow templates are confused with run history | Separate definition library and linked execution history |
| UWF-04 | Full-browser takeover hides task controls | Status, stop/takeover and return path stay reachable |
| UWF-05 | Prompt enhancement lacks a complete editable checkpoint | Preserve original; edit/regenerate enhanced prompt; exact approval after final edit |
| UWF-06 | Automatic response capture is brittle with no dependable manual recovery | Manual capture/review always available; raw response and provenance preserved |
| UWF-07 | A stuck step offers inadequate recovery controls | Separate capture retry, step retry, skip, stop, pause/takeover and cancel-run semantics |
| UWF-08 | Captured responses are not a usable workspace | Editable working copies, comparison, follow-up and synthesis without overwriting raw evidence |

The associated WF-01 through WF-13 recovery tasks remain active unless individually closed with the required source, automated and device evidence. Discovering or fixing a new defect must not delete, downgrade or defer an older requirement.

## 10. Current implementation reality — 17 November 2026

This section is a snapshot, not an assertion of device acceptance.

### Implemented in source / automated evidence exists

- Four-surface Talk / Live / Work / Studio navigation foundation.
- Persistent conversations and encrypted workspace foundation.
- Visible, sequential provider website workflow with exact-content/destination approval.
- Browser action approval ledger and audit history.
- Source-level manual response capture and capture-only detection retry recovery paths.
- Source-level step recovery states/controls, including separate handling of uncertain submission.
- Durable response-save acknowledgement before a browser workflow advances.
- Bounded native Android Accessibility workflow steps and an optional visible floating control path.
- Versioned native workflow revisions/run history foundations.
- Typed DAG proposal parsing, dependency-aware checkpoint reservation and schema-7 per-node attempt journal.
- Shared runtime request/resolver and deterministic postcondition evidence contracts.
- First deterministic app capability: source-grounded `app.entities/entity.propose`, connected to Talk and Work. Proposals remain review-only; an independent read-only observer verifies persisted records.

### Still partial, unimplemented or not device-accepted

- Generic DAG dispatcher across deterministic, browser and native Android runtimes.
- Broad deterministic app capability library and reusable skill/tool/provider/surface-schema registries with pinned versions.
- Fully inspectable prompt/context compiler and complete original → enhancement → revision → exact approval → provider handoff → response workspace.
- Council integrated into the same Talk context and Live execution timeline, including real browser-session participants.
- Fully joined template library and durable run history.
- Universal Live controls and event journal across runtimes; runtime-backed pause/resume/takeover and global cancellation.
- Robust browser element picker/recorder, schema builder, repeat-item extraction, drift fixtures and quarantined repair/promotion loop.
- Native app element picker/recorder, tested capability maps, app HUD and end-to-end execution adapter.
- Standing automation trigger engine and user-authored policy-bounded automation management.
- Full social operating platform and provider-backed account/feed/inbox/engagement/listening/publishing/analytics/growth workflows.
- Unified encrypted storage boundary; existing primary workspace and native workflow stores remain separate migration debt.
- Physical-device acceptance of the complete critical workflows.

### Verification snapshot

The latest recorded successful automated validation is GitHub Actions run [#767](https://github.com/ysfclc/memo/actions/runs/43816799963), which tested app source commit `989ac28fb6b0da3e5fc66525b1f7b3aff36329df`. It passed source verification, remote-runner tests, core/app unit tests, lint, APK assembly and API 35 in-place encrypted-store preservation tests. It was a pull-request validation: the pinned signing/final APK handoff was skipped. The latest branch HEAD at the time of this baseline is a documentation-only follow-up commit; run #767 is not a fresh build of that documentation commit. Physical-device acceptance remains open.

Tracking PR #8 remains draft and unmerged. `main` has not been changed. No merge, production release/deployment, signing identity change or destructive migration is authorized by this baseline.

## 11. Ordered execution plan

This is a sequencing plan, not a reduction in product scope. All existing requirements remain in the backlog. Work proceeds in bounded increments; every increment must leave a truthful state/checkpoint record and pass its applicable verification before the next begins.

### Phase 0 — Lock the baseline and reconcile source truth

- Preserve this document, Product Contract, Social Platform Specification, Requirement Traceability, Decision Log, TASKS and current source identity.
- Confirm current branch/head/PR and exact test/CI evidence before each substantial work package.
- Reconcile the implementation and acceptance status of WF-01 through WF-13; never copy a status from an older branch or snapshot.
- Map feedback IDs and stable requirement IDs to current source paths, tests, run evidence and device acceptance.
- Maintain the existing package, signing identity, encrypted data and both current storage formats.

**Exit:** a contributor can identify the user's intended outcome, current code, open feedback and next task without reconstructing the chat.

### Phase 1 — Make the existing prompt-to-provider-to-result journey dependable

Prioritize the user's reported failure over unrelated feature expansion.

- Finish and verify editable prompt enhancement/revision and exact content/destination/context approval.
- Preserve the active task while a visible provider session runs.
- Ensure the browser can be inspected without losing task progress, approval or recovery controls.
- Complete manual response capture, capture-only retry, safe skip/stop and separate cancel-run behavior.
- Make response persistence acknowledgement mandatory before completion.
- Separate templates from runs; make prior runs reopenable without replaying actions.
- Present raw provider responses as provenance-bearing results with editable working copies, comparison, follow-up and separate synthesis.
- Make runtime/participant labels truthful across local, API and browser routes.

**Acceptance:** pass every applicable happy path, detector failure, delayed response, duplicate-send uncertainty, prompt edit, partial multi-provider success, browser takeover, app interruption, history reopen and result-editing scenario. The whole journey remains **OPEN / NOT ACCEPTED** until exercised on a physical device with real authorized provider sessions.

### Phase 2 — Turn the workflow/compiler foundation into a real internal executor

- Complete versioned capability, tool, skill and workflow schemas.
- Finish task readiness analysis and inspectable prompt/context compilation.
- Implement the generic dispatcher over the durable attempt journal.
- Start with additional Memo-owned deterministic capabilities and independent observers.
- Add shared budgets, timeout, cancellation, idempotency, checkpoint, process-death recovery and safe partial completion.
- Make compiler/plan UI show selected capabilities, gaps, conflicts, permissions, versions and verification evidence.
- Keep model output as an untrusted proposal; deterministic code validates and owns all state changes.

**Acceptance:** a user goal produces an editable, versioned graph; each step invokes a registered capability; the run survives interruption; verified outcomes are evidence-backed; uncertain external actions are never silently replayed.

### Phase 3 — Generalize browser automation

- Build semantic page/surface inspection, user element selection and recording.
- Generate reusable, versioned browser recipes with typed inputs/outputs and postconditions.
- Add repeat-item/schema construction, dynamic page fixtures and provider/session capability manifests.
- Implement target verification, selector drift detection, safe human takeover and quarantined repair trials.
- Connect the browser adapter to the shared workflow dispatcher and Live event/control model.
- Retain manual recovery for answer capture and other important visible results.

**Acceptance:** demonstrate reusable workflows across more than one representative site pattern, including changed DOM/layout, login wall, pagination, streaming AI response and ambiguous controls, without claiming universal website compatibility.

### Phase 4 — Generalize native Android app automation

- Build device/app inventory with verified app identity and capability availability.
- Add user-controlled element picker/recorder and versioned native recipes.
- Implement visible Memo console/HUD, target highlighting, safe pause/takeover and return-to-task behavior.
- Connect the bounded Android runtime adapter to the common dispatcher, policy, journal and independent verification contracts.
- Test permission grant/denial/revocation, protected or inaccessible views, custom UI, backgrounding, locking, cancellation and app-version drift.
- Evaluate official APIs, intents and supported integration paths alongside Accessibility; no single mechanism is assumed to work in every app.

**Acceptance:** demonstrate safe end-to-end workflows in controlled fixture apps and selected real apps the user is authorized to operate. Explicitly document unsupported app surfaces.

### Phase 5 — Reusable workflows and standing automations

- Provide workflow library, authoring/editor, versions, dry-run/test, duplication, rollback and linked run history.
- Let the user compose workflows from recorded/selectable browser and Android capabilities as well as deterministic/model/integration steps.
- Add opt-in triggers, conditions, quiet hours, rate/resource limits, notifications, scoped permissions and a global stop.
- Keep monitoring, classification, internal draft creation and external side effects separately permissioned.
- Add evaluation and evidence-based improvement without allowing an agent/model to silently promote its own changes.

**Acceptance:** users can understand, edit, test, pause, resume, inspect and revoke automations; each triggered run has a durable history and honest outcome.

### Phase 6 — Complete the wider operating platform

Continue the full Product Contract and social platform scope: personal and business workflows, CRM/customer discovery, jobs/career, research, content, social accounts/feed/inbox/engagement/listening/publishing/analytics, personal brand and monetization, everyday assistance, voice and local model capabilities.

These remain Memo capabilities and workflows on the shared graph and execution system, not new disconnected products. Provider integrations must be verified individually; unsupported provider operations must remain explicit.

### Phase 7 — Device, data and release acceptance

- Unify persistence only through an explicit tested migration that preserves the current primary and native stores.
- Complete privacy/security and permission review; validate backup/restore and failure recovery.
- Exercise the critical user journeys on the user's actual supported Android device.
- Verify existing installation upgrade, package/signature continuity and saved data before release.
- Merge/release/deploy only after all required gates pass and the user explicitly authorizes that step.

## 12. Non-negotiable acceptance rules

1. No prompt or context is sent externally without the required disclosure and consent/approval.
2. No external/destructive side effect without current approval bound to the exact payload, destination, capability and scope.
3. Editing a prompt or changing its destination/context invalidates the old approval.
4. A retry of detection/observation never resends an external action.
5. An uncertain send remains uncertain and requires review; no blind replay.
6. A send click or model assertion is not completion.
7. Raw source/provider evidence is immutable; user edits create separate revisions.
8. One failed step must not erase completed results from other steps.
9. Browser and native app runtimes remain visible, interruptible and subordinate to the same task/run identity.
10. Browser/app content is untrusted input and cannot grant permissions or change Memo policy.
11. Skills, agents, tools, capabilities, models and permissions are distinct concepts.
12. A skill or workflow does not grant access; permissions are explicit, narrow, revocable and audited.
13. Every run pins versions and records inputs, state transitions, outputs, errors and evidence.
14. Only an independent postcondition observer can establish a verified outcome.
15. Source implementation, unit tests, CI, emulator, real provider and physical-device acceptance are separate evidence levels.
16. New issues add to the backlog; they do not silently remove previously reported bugs or requirements.
17. Do not replace the product's ambition with a safe demo, a fixed list of automations or a dashboard of nonfunctional controls.

## 13. Working rule for future sessions

Before each substantial change:

1. Read this baseline, the Product Contract, relevant domain specification, Decision Log, current state and latest checkpoint.
2. Inspect the exact branch/head, source, tests, recent changes and latest CI.
3. Name the user feedback and stable requirement IDs being addressed.
4. Choose one coherent user-visible outcome, dependencies, non-goals, acceptance and recovery plan.
5. Implement the smallest complete vertical increment.
6. Verify against the exact commit and the right evidence level.
7. Update TASKS, State, implementation/feature audit and a dated checkpoint.
8. Do not silently begin an unrelated increment or claim device acceptance without evidence.

**Locked north star:** Memo turns human intentions into real, reusable, controlled and verifiable work across the user's own information and authorized digital environments.

## 14. Source records behind this baseline

- [User-facing workflow and control audit](handoff/MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md) — the reported prompt → enhancement → approval → browser → captured-result failure and UWF-01 through UWF-08.
- [Workflow recovery execution plan](handoff/MEMO_WORKFLOW_RECOVERY_EXECUTION_PLAN_2026-11-13.md) — WF-01 through WF-13, recovery semantics, dependencies and acceptance invariants.
- [Workflow stabilization progress](handoff/MEMO_STABILIZATION_PROGRESS_2026-11-13.md) — implementation history and the distinction between source changes and unverified runtime behavior.
- [Feature audit](../FEATURE_AUDIT.md) — what source paths exist, what is partial, and what is not implemented or device-verified.
- [System architecture map](architecture/MEMO_SYSTEM_ARCHITECTURE.md) — current module/runtime boundaries, data ownership and known migration debt.
- [Product Contract](MEMO_PRODUCT_CONTRACT.md), [Social Platform Product Spec](SOCIAL_PLATFORM_PRODUCT_SPEC.md), [Requirement Traceability](REQUIREMENT_TRACEABILITY.md) and [TASKS](../TASKS.md) — normative scope, stable requirement IDs and engineering sequence.
