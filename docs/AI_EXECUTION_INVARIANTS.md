# Memo AI Execution Invariants

**Status:** Binding product and engineering constraints  
**Established:** 20 November 2026  
**Authority:** User-approved architecture direction. This document constrains implementation; it does not claim the architecture is already implemented.  
**Applies to:** Every Memo feature, model integration, provider, runtime, tool, workflow, UI surface and future contributor.

These rules exist to prevent Memo from drifting into disconnected chat modes, one-off workflows, model-specific integrations or screens that describe work without doing it. They are hard constraints, not suggestions.

## 1. The product interaction is outcome-first

The user states an outcome in ordinary language, voice, or supplied material. Memo determines how to accomplish it using the capabilities that are actually available.

The user must not have to choose in advance between Chat, Research, Browser, Council, CRM, PDF, Image, Code, Automation or another implementation category. Settings may expose preferences and permissions; they must not become a mandatory workflow selector.

Examples such as researching companies, generating an image, creating a PDF, changing code, consulting frontier models or operating an Android app are **acceptance scenarios**, not instructions to build a dedicated screen or hardcoded workflow for each example.

Simple conversational requests may receive a direct answer. A request requiring one action may invoke one capability. A compound request may require a dynamically selected sequence of capabilities. All three belong to the same intent and execution architecture.

## 2. One canonical intent-to-outcome path

There must be one canonical path from Talk to completed work:

1. Capture the user's request and supplied inputs.
2. Understand the intended outcome, constraints, relevant context and definition of done.
3. Discover the capabilities and intelligence providers actually available and permitted for this request.
4. Select an appropriate model/provider when reasoning is useful; select zero, one or several tools based on the outcome.
5. Decide the next action. A plan may be produced for complex work, but a plan is not a prerequisite for trivial actions and is never itself execution.
6. Validate the proposed action against deterministic policy, schemas, permissions, budgets and approvals.
7. Invoke the real capability implementation through the shared dispatcher.
8. Observe its actual result, errors, artifacts and state changes.
9. Verify the result against an observable postcondition.
10. Continue, replan, request a necessary human decision, or finish with the actual result.

The loop is dynamic: `understand → decide → validate → invoke → observe → verify → continue/replan/finish`.

Every tool-using request from Talk must use this same execution control plane. Do not create an alternate route for a new domain, modality, model or provider.

## 3. Intelligence providers and action capabilities are separate

A model is an intelligence provider, not an execution runtime.

Intelligence providers may include:
- local/on-device models;
- explicitly configured frontier model APIs;
- visible, user-authorized frontier websites controlled through the browser;
- future compatible providers.

Action capabilities may include:
- code and project-file operations;
- image generation and image processing;
- document, PDF and other file creation;
- video and other media creation where a real provider is available;
- browser navigation, inspection and evidence capture;
- Android app discovery and permitted app actions;
- Memo workspace records, relationships, tasks and artifacts;
- other providers and tools added through the capability contract.

Do not confuse these categories. A browser is not a model. An Android app is not an intelligence provider. A chat completion endpoint is not automatically a coding agent or a tool executor. A model that can produce text is not thereby capable of changing files, creating a PDF, generating an image or controlling an app.

The orchestrator may choose a local model, a permitted frontier provider, multiple models, or no model for a deterministic task. Model choice must not determine which tools exist. Tool availability must not depend on a particular model vendor.

Local-first is the default where the task and installed device capabilities support it. Remote inference or frontier website use must follow explicit, understandable per-request permissions and context disclosure. Never silently send private workspace content to a remote provider.

## 4. A registered capability must be real and callable

The capability catalog is a registry of executable interfaces, not a list of aspirations.

Every advertised capability must have:
- a stable identifier and version;
- a human- and model-readable purpose;
- a validated input schema and defined output contract;
- a concrete adapter/implementation that can be invoked;
- its owning runtime/provider and truthful availability/readiness state;
- required permissions, resource scopes and preconditions;
- side-effect/risk classification and approval policy;
- timeout, cancellation, bounded retry and idempotency behavior where applicable;
- an observable postcondition and evidence/result format.

The dispatcher must resolve a capability to its actual implementation and return a typed result or a truthful failure. An unimplemented, unavailable, unsupported or permission-blocked capability must never be reported as available or successful.

Capability discovery is data-driven from installed/registered adapters and their current readiness. A model may request only registered capabilities; Memo validates the request and supplies trusted policy fields. Model output cannot grant permissions, invent capability availability, choose its own risk classification or bypass approval.

Adding a new modality or integration means adding a genuine adapter that conforms to this contract. It does **not** mean adding another hardcoded intent domain, separate agent, parallel workflow engine or primary navigation destination.

## 5. One shared execution engine; adapters remain replaceable

There must be one shared execution lifecycle and dispatch/policy boundary across local-model, frontier, browser, Android, file, code and workspace capabilities.

Provider-specific implementation details belong inside isolated adapters. They must not leak into the product's canonical workflow model or require users to start a provider-specific workflow.

Do not introduce:
- separate orchestrators for research, Council, browser, code, image, PDF or Android automation;
- keyword/domain classification as the primary way to decide how a request is executed;
- a fixed checklist or workflow selected because the request contains words such as "research", "create", "browser" or "automation";
- a specialist screen that becomes the only way to use a capability;
- duplicate run journals, approval ledgers, workspace stores or competing sources of truth;
- model-vendor-specific branches in the common action lifecycle.

Existing specialist flows may be retained during migration, but new work must not deepen their isolation. Reuse their valuable implementations as adapters behind the shared contracts, then migrate them into the canonical execution path with regression coverage.

## 6. Plans are dynamic proposals, not the product

TaskSpec, dependency graphs, capability manifests and plans are useful representations. They do not constitute an agent or prove that work happened.

For complex work, the engine may ask a model to propose a bounded, typed plan. Memo validates it, compiles actual registered capabilities, and executes it. The engine must also be able to choose its next action from the latest observation, revise a plan, take an alternative route, branch or stop when conditions change.

Do not require every request to be forced into a fixed domain or DAG. Do not treat a static graph as the only execution semantics. Small tasks should not pay the cost of unnecessary planning.

Multi-model consultation, specialist roles, parallel analysis and debate are optional strategies selected when useful. They must use the same execution lifecycle and preserve each participant's identity, outputs, failures and evidence. They are not separate products or mandatory steps.

## 7. Deterministic safety; model-directed choice

Models may interpret intent and propose actions. Trusted application code owns:
- schema validation and capability resolution;
- authorization, permission scopes and privacy boundaries;
- action risk classification and approval requirements;
- exact destination/content binding for consequential actions;
- budgets, timeouts, retry limits, cancellation and idempotency;
- durable state transitions, audit events and postcondition evaluation.

External, destructive, financial, publishing, sending, submitting, deleting or otherwise consequential actions require explicit approval bound to the exact action, target, payload and scope. Any material change invalidates that approval. Ambiguous external outcomes become `NEEDS_REVIEW`; they must not be blindly replayed.

Browser login, CAPTCHA, OTP, passwords, payment confirmation, security challenges, revoked permissions and unexpected app/site states must stop for user intervention when the action cannot safely proceed.

Model output, web content, imported files and tool output are untrusted data. None may alter trusted policy or issue instructions that bypass the user's intent and system safeguards.

## 8. Durable execution and truthful results

A run must have durable, inspectable state sufficient to recover after process death or interruption. Record the goal, selected providers/capabilities, decisions, validated invocations, approvals, observations, outputs, evidence, checkpoints, failures and unresolved uncertainty without logging secrets or unnecessary private content.

Every action has explicit lifecycle state, bounded execution and cancellation. Reopening Memo must not silently replay a completed or uncertain side effect. Resume from recorded evidence and idempotency state.

**Journal failure is a hard execution stop.** If Memo cannot durably record a lifecycle event or observation, it must not continue to another model decision or capability. If any side effect may have started, mark the run `NEEDS_REVIEW`, explain that the outcome is uncertain, and require inspection before retry. Approval-requested, approved and rejected states must be visible in the same Talk execution transcript; a transcript rendering omission is a correctness defect.

"Complete" means the required postcondition was observed and its evidence was saved. A model saying that it succeeded is not evidence. A tool returning success is not enough when the result can be independently checked.

Created outputs must be real, accessible artifacts with truthful type, location/reference, provenance and integrity information where applicable. A PDF request must produce a readable PDF file; an image request must produce an actual image artifact; a code request must produce inspectable changes and relevant test evidence. If the required provider or capability is missing, say so and offer a real alternative. Never substitute explanatory text while implying that an artifact was created.

Live must show the actual shared run state and allow the user to inspect, approve, pause, resume, cancel or take over where supported. Work remains the durable user-owned record; artifacts and evidence must not disappear into provider-specific chat history.

## 9. User experience constraints

- Talk remains the single natural-language entry point for arbitrary requests and inputs.
- Live is the control surface for actual execution, not a dashboard of simulated progress.
- Work contains durable records and relationships, not a substitute for the agent.
- Studio exposes actual created/edited artifacts and their revisions; it is not limited to one document category.
- Browser and Android sessions appear as runtime views within a shared execution, not as competing products.
- No new primary navigation destination or specialist wizard is created merely to accommodate a new model, provider, modality or example.
- Ask the user only when a material ambiguity, permission, approval or human-only step prevents safe progress. Do not ask them to design the workflow the agent is responsible for determining.

## 10. Required acceptance tests

The same Talk entry point and shared execution engine must be tested against unrelated outcomes, including:

1. Generate an actual image using an available image capability.
2. Create and return a valid PDF from supplied material.
3. Modify code in a selected project and report actual file changes and test results.
4. Research a subject using the browser, retain source evidence, then create related workspace records and follow-up tasks when requested.
5. Consult a local model and a permitted frontier provider, compare their actual outputs, preserve disagreements, and save the result.
6. Operate a permitted Android app through the registered runtime, respecting visibility, permission, takeover and verification.
7. Handle a goal with no explicit workflow: discover available capabilities and decide the sequence without a use-case-specific template.
8. Recover from a failed tool, changed website, unavailable provider, missing permission, timeout or partial result without fabricating success.
9. Interrupt and restart a run without losing completed evidence or repeating uncertain external side effects.
10. Report honestly when no suitable capability exists.

These are regression tests of the common engine. Passing them must not be implemented by adding one special workflow per test.

## 11. Change-control hard stop

Before accepting any architecture, code or UX change, ask:

- Does this preserve the single intent-to-outcome path?
- Does it use the shared dispatcher and real capability contracts?
- Is intelligence-provider choice independent from action capability choice?
- Does it produce and verify an actual result?
- Does it preserve permissions, audit, recovery and user control?
- Does it avoid a new hardcoded domain, specialist workflow or parallel source of truth?

If any answer is no, do not implement the change as proposed. Redesign it as an adapter or a change to the shared engine, or record the concrete blocker and ask for a product decision. A passing build or attractive screen cannot waive these constraints.
