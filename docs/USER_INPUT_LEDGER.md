# Memo User Input & Product Intent Ledger

**Status:** Append-only; active.  
**Purpose:** Durable record of what the user actually said about Memo, what kind of input it was, and how it may or may not affect the product.  
**Authority:** The user's original wording is authoritative. Any normalized interpretation below is explicitly an interpretation and must never silently replace the original.

## Operating rules

1. Record every material Memo correction, requirement, decision, bug report, feedback item, idea and use-case boundary here before or in the same commit as implementation.
2. Preserve original wording verbatim where available. Never attribute assistant-written architecture, assumptions or wording to the user.
3. Give each input a stable ID. Link it to Decision Log, Requirement Traceability, TASKS, a defect/feedback record or a domain scenario as appropriate.
4. Keep these categories distinct:
   - **CORRECTION:** corrects an existing assistant interpretation or project record. Apply it immediately; preserve the old record as history.
   - **REQUIREMENT / DECISION:** a clear user request or approval. It can enter accepted scope.
   - **BUG_REPORT:** an observed defect. It stays active until fixed and verified against its reproduction/acceptance criteria, or explicitly closed by the user.
   - **FEEDBACK:** a concern or evaluation. Preserve it and trace the response; do not drop it when priorities change.
   - **IDEA (UNCOMMITTED):** retain permanently, but do not treat as accepted scope or start implementation until the user adopts it.
   - **USE_CASE:** an example scenario used to validate the general platform. It does not define Memo's identity, architecture or priorities.
   - **CONSTRAINT / NON_GOAL:** a boundary that all proposals and implementation must respect.
5. When one input contains several kinds, split it into linked records rather than flattening it into one vague summary.
6. Do not delete records. Add a later entry to change status or record supersession, with the reason and affected IDs.
7. A user correction outranks earlier assistant interpretations. If it appears to conflict with an earlier explicit user decision, record the conflict and ask the user rather than silently choosing.
8. Never promote an assistant-proposed architecture detail into a user-approved requirement without explicit user adoption.

## Active input records

### INPUT-2026-11-19-001 — General operating lifecycle and smart routing

- **Date:** 2026-11-19
- **Type:** REQUIREMENT / product-direction input
- **User wording:**
  > This should be the main steps before it reaches me. Both deterministic code and ai Enhancements, correct me if I'm wrong, or ebhance. Capture, observe, plan, execute. Next , I wanna find clients for myself, from maps, google, LinkedIn or whatever. I simply go to AI, to do sth. And system does the rest. With Smart routers.
- **User wording (follow-up):**
  > Idk maybe sth like classify!?
- **Preserved meaning:** The visible experience should be simple and outcome-oriented: Capture → Observe → Plan → Execute. The system should combine deterministic code and AI and use smart routing so the user does not manually orchestrate capabilities. Classification is a candidate internal stage, not an extra required user screen.
- **Scope boundary:** The client-finding sentence is an example scenario within this request. It is not authority to make client acquisition, prospecting or outbound activity the product's defining purpose.
- **Status:** Active; represented in Product Contract §2A and TASKS.md's 19 November checkpoint.

### INPUT-2026-11-19-002 — Client finding is a use case, not an outbound product

- **Date:** 2026-11-19
- **Type:** CORRECTION / CONSTRAINT / NON_GOAL
- **User wording:**
  > But again, we are not building outbound. This is just a use case. Please keep track of whatever correction I add in memo. Permanent!
- **Correction:** Client finding is one use case only. Memo is not an outbound product. Do not make outbound execution, cold outreach, lead generation or sales automation the product identity, global architecture, default classifier, core roadmap or assumed next task.
- **Implementation rule:** Keep this scenario only as one optional acceptance test of general-purpose orchestration. It does not authorize building an outbound module or messaging workflow. A verified public contact route may be evidence in a scenario; it does not imply permission to contact anyone.
- **Related records:** Product Contract §2A and optional UC-7; TASKS.md 19 November smart-router checkpoint; optional Sales Operations scenario pack.
- **Status:** Active; latest user correction. It supersedes any assistant interpretation that treated the example as a product direction.

### INPUT-2026-11-19-003 — Preserve all future corrections, feedback, bug reports and ideas

- **Date:** 2026-11-19
- **Type:** REQUIREMENT / GOVERNANCE DECISION
- **User wording:**
  > I want you to please do some reading, deep dive in the repo, find vulnerabilities that would cause you hallucinate and get out of product direction, not invent things or reshape the product with different interpretation, my feedback, bug reports or any additional Idea I add.. fix it once and for all. I don't wanna find myself repeating.
- **Requirement:** Preserve all future product corrections, feedback, reported bugs and additional ideas in the repository. Do not make the user repeat them. Do not invent requirements or reshape Memo through assistant interpretation.
- **Implementation rule:** Every future material user input must be classified, retained with original wording, and linked to its decision/requirement/task/defect or use-case record. Ideas remain uncommitted until explicitly adopted. Bug reports stay active until evidence-backed closure or explicit user closure.
- **Status:** Active; enforced by AGENTS.md, DEVELOPMENT_GOVERNANCE.md, session checkpoints and source-verifier checks.

## Historical feedback continuity

### INPUT-HIST-2026-11-17-001 — Recovered workflow feedback and defect register

- **Date:** 2026-11-17 baseline
- **Type:** FEEDBACK / BUG_REPORT continuity pointer
- **Preserved records:** UWF-01 through UWF-08 and WF-01 through WF-13 remain active according to their own acceptance evidence and status in `docs/MEMO_REORIENTATION_AND_FEEDBACK_BASELINE_2026-11-17.md`, `docs/handoff/MEMO_USER_WORKFLOW_CONTROL_AUDIT_2026-11-13.md`, `docs/handoff/MEMO_WORKFLOW_RECOVERY_EXECUTION_PLAN_2026-11-13.md`, and `TASKS.md`.
- **Rule:** The presence of this new governance work or a newly reported defect does not close, replace, downgrade or defer any of those issues.
- **Status:** Active continuity pointer; use the original IDs and records, do not duplicate or erase them.

## Append template

For each new material input, append a new entry:

- **ID:** `INPUT-YYYY-MM-DD-NNN`
- **Date:**
- **Type:** CORRECTION / REQUIREMENT / BUG_REPORT / FEEDBACK / IDEA (UNCOMMITTED) / USE_CASE / CONSTRAINT / NON_GOAL
- **User wording:** exact quote where available
- **Preserved meaning:** clearly labelled interpretation, with uncertainty retained
- **Scope boundary:** what the input does not authorize or imply
- **Related requirement / decision / task / defect IDs:**
- **Status:** active / uncommitted / in progress / verified / explicitly closed by user



### INPUT-2026-11-19-004 — Browser automation is Frontier-provider hardcoded and did not open the selected browser target

- **Date:** 2026-11-19
- **Type:** BUG_REPORT / CORRECTION to the current implementation boundary
- **User wording:**
  > Also, i tried browser automation,1st. İt's hardcoded around frontiers. 2nd even i tried Frontiers with Selected providers and all already logged in, o browser happened.
- **Preserved meaning:** The user expects browser automation to serve general website tasks, not be architecturally limited to a fixed list of frontier AI chat sites. The current implementation's provider-specific consultation feature is not an acceptable substitute for general browser automation. The user also reports that selecting providers and having accounts already signed in did not result in the browser opening/performing the expected action.
- **Source observation (not user wording):** BrowserWorkflow.kt defines a fixed BrowserModelProviders catalogue; BrowserWorkflowScreen.kt binds its provider picker to that list and labels the feature "Frontier workflows"; BrowserSession.startBrowserWorkflow accepts only those provider IDs and navigates to each configured startUrl. The generic browser task path is separate. The selected-provider navigation path exists in source, but the reported real-device failure has not yet been reproduced, so its exact runtime cause remains unknown.
- **Scope boundary:** Keep frontier websites as optional browser destinations/participants. Do not remove them, but do not make them the browser runtime's provider universe or define Memo's browser automation product.
- **Acceptance required:** A general user goal can select/resolve an arbitrary permitted HTTPS website or app/web target, navigate in the visible browser, inspect the page, prepare a task-specific action, request approval only when a real external side effect is proposed, execute after approval, and return captured evidence. A regression must prove selected targets actually open from the workflow entry point, with explicit state/errors when navigation fails. Existing frontier workflows remain a supported scenario, not the architecture.
- **Related:** MEMO-BROWSER-001; new BRW-14 and BRW-15; WF-05, WF-07, WF-09, WF-11, WF-12.

### INPUT-2026-11-19-005 — Approval action is not discoverable / no visible approval button

- **Date:** 2026-11-19
- **Type:** BUG_REPORT / usability feedback
- **User wording:**
  > And where is the approval butron? İ can't see anywhere.
- **Preserved meaning:** The user cannot find a clear, visible, actionable approval control in the actual flow. Having approval logic in source is not sufficient; the user must be able to identify the exact pending action and approve or reject it from an obvious place.
- **Source observation (not user wording):** The current browser prompt's "Approve and send" button is inside an AlertDialog shown only when workflowState.awaitingPromptApproval is true. The Action Approval Inbox lists pending actions but its current interaction contract does not provide an approve callback; its description directs the user back to the live browser workflow for confirmation. This creates a hidden/context-dependent approval affordance.
- **Acceptance required:** Every pending action has one obvious, reachable approval surface with a plain-language action label, target/destination, exact content or action summary, consequence and Approve / Reject controls. The same pending record is not duplicated across multiple competing approval surfaces. Approval remains bound to exact payload, destination, expiry and one-use execution. If the action cannot be approved safely, the UI says why and offers a safe exit.
- **Related:** MEMO-BROWSER-001; new APR-01; WF-06, WF-07, WF-11, WF-12.

### INPUT-2026-11-19-006 — Checkpoints are confusing and approvals are requested too often

- **Date:** 2026-11-19
- **Type:** FEEDBACK / BUG_REPORT
- **User wording:**
  > Checkpoints confusing, nothing to approve but keeps asking approvals everywhere.
  > So many confusion.
- **Preserved meaning:** The current interaction conflates ordinary workflow checkpoints, user review, recovery prompts and authorization for external side effects. The user experiences repeated approval requests even when no meaningful action is presented for approval. Reduce unnecessary interruptions and make each pause explain exactly what is waiting and why.
- **Acceptance required:** Distinguish (1) progress/informational checkpoint, (2) user input or recovery needed, (3) review of a produced artifact/result, and (4) explicit authorization for a specific external/destructive side effect. Read-only navigation, observation, local inference, planning, draft generation and persistence must not trigger an external-action approval. One external side effect produces one pending approval bound to the exact action; no repeated prompt for the same unchanged action. Recovery/uncertainty is labelled "Needs review", not disguised as an approval request. Keep fail-closed security; do not remove legitimate approvals to make the UI quieter.
- **Related:** MEMO-LIVE-001, MEMO-ORCH-001, MEMO-BROWSER-001; new CHK-01 and APR-01; WF-03, WF-06, WF-07, WF-11, WF-12.
- **Status:** Active; not yet reproduced/closed on a physical device.
