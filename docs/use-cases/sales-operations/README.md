# Memo Sales Operations — Product Use Cases

**Status:** Product-planning specification / proposed workflows  
**Owner context:** Yusuf's independent B2B sales-operations practice  
**Target implementation:** Memo Android app + its existing Work orchestration, capability planning, browser/remote-runner and action-approval architecture  
**Created:** 2026-11-10  
**Important:** This document defines intended operator workflows. It does not claim that social-platform integrations, account connections, browser actions or CRM connectors are currently implemented. The implementing agent must inspect the current source and capability registry before marking any step executable.

## 1. Product intent

Memo should act as Yusuf's personal sales-operations co-pilot and controlled operator. It should help him find suitable companies, understand their business, identify plausible commercial problems, prepare compliant approaches, manage conversations, scope and deliver paid engagements, and learn from results.

Memo is not intended to be:
- A generic CRM clone.
- A bulk cold-email / auto-DM / robocalling tool.
- A lead-list scraper or a system that evades platform restrictions.
- An autonomous agent that invents facts, sends messages, makes commitments or signs contracts without approval.
- A replacement for a lawyer, accountant, immigration adviser or the client's own sales judgement.

The intended outcome is a reliable small-business operating system that lets one person run a focused B2B service with less administrative overhead.

## 2. Operator and business context

The operator is based in Kraków, Poland and can potentially travel around the Kraków–Silesia industrial corridor; travel radius and weekly capacity must be confirmed in the product onboarding. Turkish and Kurdish are native languages, English is strong, Polish is developing (A2). Initial customer conversations should therefore prioritise English-capable decision-makers, warm introductions, international/export-oriented firms, or meetings where a trusted Polish-speaking introducer can help. Memo must not assume fluent Polish.

Relevant experience:
- Approximately six years in CNC / injection-moulding machining, design and programming, with Fanuc/Mitsubishi controls and Autodesk PowerShape.
- B2B partner management / account work at SALESmanago, including DACH partners and MEDDPICC exposure.
- Business development and outbound-system building at Printbox / Masterpiece AI.
- Built operational prospecting systems using Airtable, n8n/Make, Apollo, Unipile and scripts; experienced with high-volume outbound, but the new Poland-based acquisition system must be permission-led and legally reviewed.
- Strong practical interest in CRM, workflow automation, AI-assisted research and sales process design.

Current financial situation makes near-term paid work important. Memo should favour short sales cycles, small fixed-scope pilots and existing businesses with actual customers and enquiries. Do not recommend long speculative product builds or unpaid consulting as the default.

## 3. Initial service hypothesis

**Working offer:** Sales Pipeline & Quotation Recovery Sprint for small B2B industrial businesses.

The service improves the process around enquiries, RFQs, quotations and follow-up. It may include:
1. Mapping the current enquiry-to-quote-to-order workflow.
2. Reviewing a client-approved sample of open / dormant quotations.
3. Defining pipeline stages, required fields, ownership and next actions.
4. Setting up or cleaning the client's existing CRM or a lightweight spreadsheet workflow.
5. Creating follow-up task queues, reminders and management visibility.
6. Producing a short handover guide and team walkthrough.

The service sells defined work and operational improvement, not guaranteed sales, a guaranteed number of leads, or a guaranteed conversion uplift. Any commercial target must be framed as a hypothesis and measured against a baseline.

### Pilot hypothesis (not a fixed price list)

- Duration: 8–12 hours of work over approximately 10 working days.
- Scope: one team / business unit, one agreed pipeline, a limited sample of records.
- Indicative test price: PLN 900–1,500 gross, subject to scope, tax treatment, legal contract form and buyer validation.
- Payment: proposed 50% start / 50% handover or another agreed milestone structure.
- Acceptance: deliverables are supplied and reviewed against written criteria. No guarantee that a quotation converts into an order.
- Longer engagement only after pilot review.

Memo must present these as draft commercial assumptions, never as verified market rates or commitments.

## 4. Initial ideal customer profile (ICP)

Test, do not assume, these segments in this order:

### Segment A — CNC / metalworking subcontractors
Examples: CNC milling and turning, precision machining, laser cutting, sheet metal, welding, custom metal components.

### Segment B — Injection moulding / tooling / technical plastics
Examples: injection moulders, mould makers, toolrooms, technical plastic components and contract manufacturing.

### Segment C — Industrial distributors / technical wholesalers
Examples: machine components, tooling, automation equipment, industrial supplies and import/export distributors.

Initial company-size hypothesis: approximately 10–100 employees. This is a research filter, not a hard rule. A better qualification signal is an existing B2B enquiry/quotation process, recurring customers, technical sales cycles and a small or owner-led commercial team.

Prioritise businesses that:
- Already have a product/service and paying customers.
- Receive RFQs, enquiries or requests for quotations.
- Prepare quotations but lack clear ownership, follow-up or pipeline visibility.
- Have a small commercial team or owner-led sales process.
- Can benefit from better organisation without a long IT procurement.
- Have an English-capable contact, export activity or a warm introduction route.

Deprioritise initially:
- Large OEMs / Tier 1 procurement systems with long vendor onboarding.
- One-person workshops with little enquiry volume or budget.
- Startups without customers or a proven offer.
- Firms expecting guaranteed revenue, free strategy work or commission-only engagement.
- Firms whose core problem is product quality, capacity or pricing rather than sales process.

## 5. Operating model: one person, one command centre

Memo should organise work into a continuous loop:

**Discover → Research → Qualify → Find a lawful/appropriate route → Prepare approach → Obtain approval → Contact or assist operator → Capture response → Diagnose → Scope → Contract → Deliver → Report → Learn.**

Every company is an account. Every individual is a contact only when necessary and with appropriate data governance. Every commercial interaction is an activity linked to an account and, where appropriate, a contact. Every claim has source evidence and a retrieval date. Every external side effect is subject to capability checks and approval.

### Core records

- **Account:** legal/trading name, website, location, industry, size estimate with source, capabilities, target segments, languages, export evidence, fit score, evidence links, status, next action.
- **Contact:** name, role, business contact details only where lawfully obtained and necessary, source, purpose, lawful/consent status, channel permission, last verified date, suppression/do-not-contact state. Avoid collecting personal details irrelevant to the business purpose.
- **Signal:** public or permissioned evidence such as RFQ page, expansion announcement, new equipment, hiring, trade-show attendance, new export market, customer-service complaint, or an explicit request for help. Record source, date, exact observation and confidence. Do not infer sensitive personal data.
- **Opportunity:** problem hypothesis, discovery status, estimated value (clearly marked estimate), buying roles, next step, objections, timing, probability only if grounded.
- **Activity:** research, introduction requested, consent granted, conversation, meeting, proposal, follow-up, delivery.
- **Consent / communication permission:** channel, scope, wording/evidence, date, source, expiry/withdrawal, applicable jurisdiction and reviewer.
- **Approval item:** exact action, exact destination/account, exact content or payload, evidence, risk, expiry, approver and immutable outcome.
- **Engagement:** signed scope, contract type (selected by human/adviser), deliverables, milestones, fee, payment state, client access and data-processing obligations.
- **Delivery task / result:** owner, due date, evidence, status, acceptance and client feedback.

Do not build a second database if Memo's current Work records can represent these entities. Extend existing records and repositories where appropriate.

## 6. Channel strategy — capabilities must be verified

Memo should treat channels as different surfaces with different permissions, APIs and limits. Never assume a single “social integration” can read and act everywhere.

| Channel | Intended role | Safe initial operating mode | Automation status |
|---|---|---|---|
| Website / contact pages | Research, capability evidence, inbound forms, public RFQ routes | Browser-assisted research; use published forms only under their stated purpose and rules | Verify per site |
| Email | Business correspondence and proposals | Draft, classify replies, manage approved threads; marketing outreach only where prior consent / another valid route is established and reviewed | Verify connected email and legal basis |
| LinkedIn | Professional research, warm introductions, inbound interest, content | User-led browsing; draft posts/messages; no scraping, bulk connection requests or automated DMs | API and automation limits must be checked |
| Facebook Pages / Groups | Local business discovery, industry groups, public posts, page enquiries and inbound comments | Discover public business signals; participate only under group/page rules; respond to inbound enquiries; no unsolicited mass DMs | Meta permissions/API coverage must be verified |
| Instagram | Business profile research, visual capability signals, inbound DMs/comments | Monitor only where account/API permissions allow; draft content and replies; handle inbound conversations with approval | Meta permissions/API coverage must be verified |
| YouTube | Technical content, company capabilities, public product demonstrations | Research videos/transcripts where lawfully accessible; identify relevant business signals | Verify data source |
| Trade directories / maps | Account discovery and location/industry filtering | Use approved sources and respect terms; avoid bulk extraction and prohibited reuse | Verify source terms |
| Events / trade fairs | Warm introductions and face-to-face discovery | Prepare target list, questions and follow-up drafts; record consent to follow up | Human-led |
| Referrals / partners | Highest-priority route to early conversations | Ask referrer to confirm recipient is open to an introduction; track intro permission | Human-led |
| Phone | Discovery after permission / appropriate lawful basis | Prepare call brief and script; user makes call; record outcome | No automated calling |

Memo must distinguish (a) researching a publicly visible business fact, (b) processing personal data, (c) using a channel for direct marketing, and (d) performing a platform action. They are separate checks.

## 7. Prioritisation and scoring

A transparent score can rank accounts but must not pretend to be objective truth. Store individual factors and evidence, not only a single opaque score.

Suggested 100-point model:
- Segment fit: 20
- Evidence of active B2B quotation / enquiry flow: 20
- Observable process-improvement signal: 20
- Likely ability and willingness to pay: 15
- Access route / warm introduction / explicit inbound interest: 15
- Language and geography fit: 10

Hard disqualifiers override score:
- No lawful/appropriate route to contact.
- Explicit objection, opt-out or suppression record.
- No relevant B2B offer or no credible fit.
- Unverifiable company identity or suspicious request.
- Work would require prohibited scraping, deception or platform evasion.

An account without a verified contact route can remain in a research queue, but must not be treated as outreach-ready.

## 8. Success measures

Early validation:
- 30 researched accounts with evidence.
- 10–15 fully qualified accounts.
- 5–8 conversations obtained through permissioned / warm / inbound routes.
- 2–3 discovery meetings.
- One paid pilot.
These are operating targets, not forecasts or guarantees.

Business measures:
- Time spent researching each qualified account.
- Share of records with source evidence.
- Permission / channel status completeness.
- Warm introduction acceptance rate.
- Discovery-to-proposal rate.
- Proposal-to-paid-pilot rate.
- Time from first conversation to payment.
- Pilot delivery hours vs estimated hours.
- Client acceptance and repeat engagement.

Do not optimise for raw messages sent, scraped leads or vanity engagement.

## 9. Human control and trust

Memo may research, summarise, score, draft, organise, remind, prepare and execute low-risk internal actions within granted scope.

Memo must require explicit, per-action approval before:
- Sending a commercial email or message.
- Posting or commenting publicly.
- Sending an invitation or connection request.
- Publishing social content.
- Submitting a form that creates an external commitment.
- Sharing client data with a third party or remote model.
- Changing/deleting CRM or customer records in a material way.
- Issuing a quote, accepting terms, signing, or making a financial commitment.

Approval must show exact destination, exact content, attachments/data, platform/account identity, reason, evidence, permission status, and irreversible consequences. A changed payload invalidates the prior approval. “Approve this campaign” must not silently approve later modified messages or new recipients.

See [governance and safety](./governance-and-compliance.md) and the individual flows.

## 10. Implementation principles

1. Read current Memo architecture and capability registry before implementation.
2. Reuse the existing Work orchestration, source-backed records, capability planner/compiler, remote browser worker and action-approval ledger where present.
3. Do not duplicate the CRM, Work engine, browser runner, model router or approval inbox.
4. A UI button must not imply a capability exists. Show capability state: Available, Needs connection, Needs permission, Manual step, Unsupported, or Blocked.
5. No fabricated success. Verify postconditions from the platform or destination.
6. Idempotency and safe retry are required for any external side effect.
7. Preserve an audit trail: source evidence, plan, approvals, execution events, result and errors.
8. Build the smallest useful vertical slice first: account research → qualification → evidence-backed brief → approved warm introduction / inbound reply handling → opportunity tracking.
9. Social integrations should be added one platform and one permitted capability at a time, subject to API access, app review, terms and tests.
10. Never store passwords, session cookies or access tokens in ordinary work records or logs.

## 11. Open questions to resolve before implementation

- Which Memo build/branch is the implementation target? Compare the active branch with the expanded workflow-governance branch and preserve the user's selected source of truth.
- Which email provider and calendar are actually connected and authorised?
- Does the remote browser worker support persistent authenticated sessions, and what are its current security boundaries?
- Which Meta APIs / app permissions are approved? Do not assume access to personal inboxes, Groups or arbitrary Instagram accounts.
- What LinkedIn capabilities are genuinely available through official APIs or user-operated browser flow?
- What is the user's realistic travel radius and weekly delivery capacity?
- What current Polish residence/work basis permits the user to perform paid services for additional clients?
- Which Polish contract/tax/ZUS model has been confirmed by a qualified accountant or authority?
- What data-protection roles and agreements apply when handling client CRM and contact data?

These questions are gates, not reasons to fake functionality.

## 12. Definition of done for this product slice

The first useful release is complete when Yusuf can:
1. Create or import a target account without scraping prohibited data.
2. Ask Memo to research it and receive an evidence-linked account brief with confidence and unknowns.
3. See why it fits or does not fit the ICP.
4. See whether there is a valid route to contact it, including the source of any permission.
5. Generate a channel-appropriate, truthful draft.
6. Review and approve the exact action before it leaves Memo.
7. Execute only through a verified supported capability or receive a clear manual handoff.
8. Capture response and meeting outcome into the same account/opportunity.
9. Generate a proposal and delivery checklist from an approved scope without sending or committing automatically.
10. Report results and learn which segment, problem and acquisition route produce paid pilots.

A beautiful dashboard without this end-to-end working loop is not done.
