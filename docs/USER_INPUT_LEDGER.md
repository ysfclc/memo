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
