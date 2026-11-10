# Use Case 03 — Permissioned Multichannel Engagement

**Actor:** Yusuf (approver and sender)  
**Outcome:** Prepare and manage relevant, channel-appropriate conversations without spam, deceptive automation or platform-policy evasion.

## Core principle

Memo must not equate “publicly visible contact detail” with consent to direct marketing. Channel permission, privacy/data-processing basis, platform rules and the technical capability to act are separate gates. If any gate is unknown, the state is “Needs review” or “Manual step”, not “Ready to send”.

## Channel workflows

### A. Warm introductions
1. Identify the mutual connection from operator-provided context or an authorised contact source.
2. Draft a short request to the referrer explaining why the company may be relevant.
3. Ask the referrer to confirm the recipient is open to an introduction.
4. Do not message the recipient until the introduction is made or the recipient agrees.
5. Record who introduced whom, permission scope/date and next action.
6. Draft a concise follow-up for Yusuf to review.

This is the preferred early acquisition route.

### B. LinkedIn
1. Research company and role using permitted, user-operated access or supported official API features.
2. Do not scrape profiles at scale, automate connection requests, evade limits or use fake accounts.
3. Prefer public content, inbound interest, mutual introductions and conversations the user has already initiated.
4. Draft a context-specific note only after a suitable route exists.
5. Show exact profile/account, text and context before any action.
6. If no supported compliant sending capability exists, provide a manual handoff.

### C. Facebook Pages and Groups
1. Discover relevant industrial companies, local business communities, trade groups and public business discussions using permitted access.
2. Respect each group's membership, promotional and self-promotion rules.
3. Do not mass-post, scrape member lists, auto-comment, join groups deceptively or send unsolicited bulk Messenger messages.
4. For an inbound comment, Page message or explicit request, prepare a helpful reply that addresses the question.
5. Before posting/replying, show the exact Page/group/thread, audience visibility and text for approval.
6. Track whether the channel is a Page, group or personal profile; permissions differ.

### D. Instagram
1. Research business profiles and visual evidence of products, machinery, work and trade activity where publicly accessible.
2. Do not infer that a business profile permits unsolicited sales DMs.
3. Use official Meta API capabilities only for connected professional accounts and approved permissions.
4. Prioritise inbound DMs, comments, story replies or explicit enquiries where the connected account is authorised to access them.
5. Draft a relevant response; require approval before sending.
6. If API or account permissions do not support a requested action, show “Unsupported / Manual step” rather than simulate it.

### E. Email
1. Classify inbound correspondence and identify requests, questions, objections and meeting intent.
2. Draft replies and proposals for approval.
3. For outbound marketing, check documented prior consent or another specifically reviewed lawful route before preparing a send action.
4. A public email address alone is not sufficient evidence of marketing permission.
5. Maintain suppression/opt-out status; never re-add a person after opt-out without a new valid basis.
6. Do not use purchased lists or automated sequences as a shortcut around consent requirements.

### F. Website forms
1. Check the form's stated purpose and terms.
2. Use it only for a relevant enquiry where the form invites that type of request.
3. Never disguise a sales pitch as a customer enquiry, job application, support request or quote request.
4. Show exact fields and text before submission.
5. A form submission is an external side effect and requires approval.

### G. Phone and events
1. Prepare call notes, qualification questions and a clear reason for contact.
2. Use calls after permission or a reviewed lawful route; no robocalls or automated dialling.
3. User makes the call unless a separately verified, lawful capability is approved.
4. At events, prepare target questions and log whether the person agreed to follow-up.

## Consent and permission record

Store:
- Person/business or account identifier.
- Channel and account identity.
- Exact permission or consent scope.
- Source and evidence (e.g. recipient's explicit request, event opt-in, referrer confirmation).
- Timestamp and jurisdiction.
- Whether permission covers marketing, service replies or a single requested response.
- Withdrawal/opt-out date and suppression state.
- Reviewer and any legal interpretation required.

Do not infer consent from a like, follow, group membership, public profile, published phone number or generic email listing.

## Approval contract

Before any external message/post/comment/invitation/form submission, show:
- Channel, connected account and destination.
- Exact recipient, thread, Page or group.
- Exact text and attachments.
- Purpose and relevant source context.
- Permission evidence and status.
- Whether this is a marketing message or response to an inbound request.
- Data to be disclosed.
- Expected external effect.
- Any platform/API limitation.

Approval is bound to the exact content, destination and action. Any change invalidates approval. Record execution result and verify whether it actually occurred.

## Legal and platform caveat

This specification is operationally conservative, not legal advice. The builder must not encode legal conclusions as universal rules. Polish electronic-marketing rules, GDPR, cross-border rules and platform terms may change. Provide configurable policy gates and require qualified review where applicability is uncertain.

## Acceptance criteria

- No mass-send or bulk-DM feature is introduced.
- No contact becomes “outreach-ready” without a documented route and permission review.
- A public contact detail is not treated as consent.
- Exact action approval is required for every external side effect.
- Unsupported platform operations are clearly marked and not faked.
- Opt-outs and objections suppress future marketing actions across channels.
