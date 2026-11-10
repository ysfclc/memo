# Use Case 01 — Account Discovery and Evidence-Backed Research

**Actor:** Yusuf (operator)  
**Primary system:** Memo Work  
**Outcome:** A verified, prioritised list of industrial companies worth investigating — not a scraped list of people to message.

## Trigger

Yusuf says:
- “Find 20 CNC subcontractors around Kraków and Silesia that fit my offer.”
- “Research this company before I ask for an introduction.”
- “Find injection-moulding companies with export activity.”
- “Compare these five industrial suppliers.”

## Preconditions

- Search/source capabilities are available, or Memo clearly reports that research must be manual.
- The geographic area and segment are specified or Memo asks one concise clarification.
- Sources and retrieval dates can be recorded.
- The operator's travel and language constraints are available or marked unknown.

## Main flow

1. **Clarify scope.** Memo confirms segment, geography, approximate company size, language/access preference and target count. It should use known context but must not invent travel availability.
2. **Discover accounts.** Use permitted search engines, company directories, trade associations, exhibitor lists, company websites and user-supplied sources. Respect source terms and robots/API restrictions. Do not scrape behind login or bypass rate limits.
3. **Verify identity.** Confirm company name, domain, location and that it is an operating B2B company. Distinguish legal entity from trading brand and similarly named businesses.
4. **Understand offering.** Extract only relevant public facts: manufacturing capabilities, materials, processes, markets, certifications if stated, capacity claims if stated, customer sectors, export evidence, languages and RFQ route.
5. **Find commercial-process signals.** Examples: online RFQ/quote request, new machinery, hiring sales/export roles, new plant, new market, product catalogue, trade fair participation, repeated project work. A signal is not proof of a broken sales process.
6. **Map likely buyer role.** Prefer role/title such as owner, managing director, commercial director, sales manager or export manager. Do not harvest personal contact details unnecessarily.
7. **Assess offer fit.** Apply the transparent ICP score. Each factor must cite evidence or be marked unknown. Do not assign high score merely because the company is in manufacturing.
8. **Check access route.** Mark one: warm introduction available; explicit inbound/opt-in route; event/face-to-face route; published contact route with permitted purpose; no outreach route established. Do not infer consent from a public email or phone number.
9. **Produce account brief.** Summarise why it may fit, what Memo knows, what it does not know, likely problem hypotheses, suggested discovery questions and next safe action.
10. **Queue for human review.** Yusuf accepts, rejects, or asks for more research. Only accepted accounts enter the active pipeline.

## Output: account brief

- Company name, website, location, segment.
- Evidence-backed business description.
- Capabilities / products / customer industries.
- Size estimate and source/date (if available).
- Export / language evidence.
- Observed enquiry / quotation process evidence.
- Potential process-improvement hypotheses, explicitly labelled hypotheses.
- ICP factor scores and rationale.
- Decision-maker role hypothesis (not guessed personal identity).
- Contact route and permission status.
- Unknowns / confidence.
- Sources with URLs, page title and retrieval timestamp.
- Recommended next action.

## Alternate flows

- **Insufficient evidence:** Keep as “Research needed”; do not invent missing facts.
- **No fit:** Mark “Disqualified” with reason; do not continue to contact prep.
- **Duplicate company:** Merge or link to existing account; preserve evidence and history.
- **Outdated website:** Flag date uncertainty and request verification.
- **Source blocks access:** Stop; use another lawful source or ask operator.
- **Personal data appears:** Minimise, avoid sensitive data, record only what is necessary and permitted.

## Acceptance criteria

- Every material factual claim has a source or is explicitly marked as an estimate/hypothesis.
- A user can inspect why the company was selected.
- No personal contact data is required to create an account.
- No message, invitation, form submission or public action occurs in this use case.
- Duplicate detection does not destroy existing notes or audit history.
