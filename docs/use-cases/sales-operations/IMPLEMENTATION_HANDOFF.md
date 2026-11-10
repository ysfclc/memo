# Implementation Handoff — Sales Operations Workflows

This file is the build-agent entry point for the Sales Operations use-case pack. Read it together with [README.md](./README.md), all six numbered use cases and [governance-and-compliance.md](./governance-and-compliance.md).

## Product request in one sentence

Turn Memo into Yusuf's controlled, evidence-backed sales-operations operator for a small industrial B2B service business: discover and qualify suitable manufacturers, identify real commercial signals, manage permissioned multichannel conversations, turn confirmed problems into paid pilots, support delivery and report outcomes.

## Repository facts and architecture constraints

The product owner asked for this as an addition to Memo, not a new standalone CRM or another app.

The expanded workflow-governance source branch inspected while preparing this specification contains:
- A product contract and social-platform product specification.
- Work orchestration and source-backed work records.
- Intent planning and capability compilation.
- A remote browser workflow / runner.
- A persistent action-approval ledger and approval inbox.
- Explicit local/remote model and context-sharing controls.

Relevant source areas inspected on that branch include:
- `core/src/main/kotlin/app/memo/android/data/ActionApprovalLedger.kt`
- `core/src/main/kotlin/app/memo/android/orchestration/IntentPlanner.kt`
- `core/src/main/kotlin/app/memo/android/orchestration/CapabilityCompiler.kt`
- `core/src/main/kotlin/app/memo/android/runtime/RuntimeContracts.kt`
- `app/src/main/java/app/memo/android/browser/BrowserWorkflow.kt`
- `app/src/main/java/app/memo/android/ui/ActionApprovalInboxScreen.kt`
- `app/src/main/java/app/memo/android/ui/AppsWorkspaceScreen.kt`
- `remote-runner/server.py`

**Important source-of-truth warning:** This use-case pack was committed on branch `feature/sales-operations-use-cases-2026-11-10`, based on `memo-workflow-governance-2026-11-09`. The repository's `main` branch was a smaller source snapshot at inspection time. Before coding, compare the active/default branch and any newer user-selected branch. Do not silently merge, reset, or replace the owner's chosen source of truth. Preserve the source archive/build workflow and existing product contracts.

The social-platform spec inspected on the governance branch describes a broad social product direction; it does not prove that Meta, LinkedIn or other integrations are currently connected or fully implemented. Audit each capability against executable code, permissions and live tests.

## Required first step for the build agent

Produce a short architecture map before implementation:
1. Identify the active source-of-truth branch and current commit.
2. Locate Work records, persistence, planner/compiler, capability registry, browser runner, approval ledger and audit history.
3. Map which of the use-case records can reuse existing models.
4. Inventory real connectors and permissions for Gmail/email, browser, LinkedIn, Meta/Facebook, Instagram, YouTube, calendar and CRM.
5. Mark every capability Available / Needs connection / Needs permission / Manual step / Unsupported / Blocked.
6. Identify any overlap with the existing social-platform specification and explain whether the new use cases extend it or need a cross-reference.
7. Present gaps and a phased plan before broad UI changes.

Do not start by creating a large dashboard or fake social inbox.

## Recommended implementation phases

### Phase 0 — Audit and traceability
- Read all files in this use-case folder.
- Read existing product contract, social spec, governance docs, feature/gap audit and task ledger.
- Create a traceability matrix: use case → existing capability → gap → implementation/test.
- Resolve the source-of-truth branch with the owner.

### Phase 1 — Account research vertical slice
- Create/reuse an Account record.
- Research a company from permitted sources.
- Store source URL, retrieval time, evidence excerpt/summary and confidence.
- Generate a structured account brief.
- Score ICP fit transparently with factor-level rationale.
- Detect duplicates without destructive merging.
- Human accepts/rejects the account.
- No external contact action in this phase.

### Phase 2 — Qualification and opportunity
- Link discovery notes and signals to an account.
- Separate confirmed facts, client statements, operator observations and hypotheses.
- Generate discovery brief/questions.
- Store qualification outcome and next action.
- Avoid fabricated budgets, deal values or close probabilities.

### Phase 3 — Permission and channel readiness
- Implement a channel/permission record and suppression checks.
- Distinguish public-source research from personal-data processing and direct marketing.
- Provide a per-channel readiness explanation.
- Draft content but do not send unless a verified capability and all policy gates pass.
- Add exact-action approval binding to destination, content and payload.
- Manual handoff is a valid product outcome.

### Phase 4 — Inbound and social listening
- Start with connected, permissioned accounts and public sources whose access terms allow use.
- Ingest only platform-supported events and content.
- Classify inbound intent and draft replies.
- Require approval before reply/post/comment.
- Never implement mass unsolicited DM, automated connection requests, group-member scraping or platform evasion.

### Phase 5 — Proposal and delivery
- Generate a pilot proposal from confirmed discovery notes.
- Mark price, tax and contract-form language as draft pending human/accountant review.
- Create a delivery checklist and agreed-scope record.
- Add client data-access boundaries and least-privilege controls.
- Support quotation audit and follow-up queue using client-authorised records only.

### Phase 6 — Reporting and learning
- Report researched accounts, qualified conversations, meetings, proposals, paid pilots, delivery time and client outcomes.
- Do not optimise for messages sent or scraped records.
- Learn which segments and routes produce actual paid work, while preserving source evidence and uncertainty.

## Required UX behaviour

The operator should be able to ask in natural language:
- “Research 10 CNC subcontractors near Kraków.”
- “Show me the strongest five and explain the evidence.”
- “Prepare a brief for this company.”
- “Find a warm-introduction route; do not contact anyone yet.”
- “Summarise new inbound messages from my connected business accounts.”
- “Draft a reply to this enquiry.”
- “Prepare a two-week quotation-recovery pilot proposal.”
- “Turn this signed scope into a delivery checklist.”
- “Show me which opportunities need my attention today.”

Memo should present a compact work queue, not a vanity dashboard:
- Research to verify.
- Accounts awaiting operator decision.
- Introductions requested / pending.
- Inbound conversations needing reply.
- Opportunities awaiting discovery.
- Proposals awaiting approval.
- Client delivery tasks.
- Follow-ups due.
- Blocked actions with the reason.

Every item should show its source, status, next action and whether it needs the operator.

## Non-negotiable behaviour

- Never invent company facts, contacts, consent, platform access, message delivery or outcomes.
- Never infer marketing consent from a public email, phone number, social follow, like, group membership or public profile.
- Never send, post, comment, invite, submit, commit, sign, quote or modify important client data without the exact approval described in the use cases.
- A modified payload requires new approval.
- A timeout after submission is “Outcome uncertain” until verified; do not blindly retry.
- Never store passwords, cookies, access tokens or secrets in ordinary work records/logs.
- Never send client CRM data to a remote model without explicit permission and appropriate contractual/data safeguards.
- Keep legal, tax, ZUS, residence/work-rights and contract classification as human/adviser review gates.
- No commission-only / guaranteed-revenue promise by default.
- Do not build a mass cold-email or auto-DM engine.

## Required tests

At minimum, add tests for:
- Source evidence and retrieval timestamp required for material company claims.
- ICP factor scores include rationale and unknowns.
- Duplicate account detection preserves provenance.
- Public contact detail does not create consent.
- Inbound request scope does not silently authorise unrelated marketing.
- Opt-out/objection suppresses future actions across relevant channels.
- Unsupported or unconnected platform action cannot be executed.
- Approval is invalidated by changed destination, content, attachment or payload.
- Duplicate retry does not send twice.
- Uncertain external result is not reported as success.
- Client data access is scoped to engagement permissions.
- Remote model sharing requires explicit user choice.
- Proposal and reporting distinguish measured facts, client statements and hypotheses.

## Definition of a successful first release

Yusuf can research and qualify a real industrial company, inspect why Memo recommends it, identify a legitimate route to a conversation, prepare an evidence-based draft, approve the exact action, record the response, prepare a fixed-scope pilot proposal and track delivery — without Memo pretending to have unavailable integrations or bypassing platform/legal safeguards.

If this complete narrow loop is not working end-to-end, do not declare the product slice complete.
