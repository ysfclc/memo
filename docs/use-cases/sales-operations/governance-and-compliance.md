# Sales Operations — Governance, Compliance and Risk Controls

This is a product safety specification, not legal advice. Legal requirements must be reviewed for the actual jurisdiction, communication channel, data, contract and user status.

## 1. Independent gates

Before any prospecting or external action, evaluate each gate separately:

1. **Source access:** Is the source lawfully and contractually accessible? Are scraping, reuse and retention permitted?
2. **Personal-data basis:** Is personal data being processed? What is the purpose, basis, transparency obligation, minimisation and retention?
3. **Direct-marketing permission:** Is this communication channel and purpose permitted? Is documented consent or another valid route required?
4. **Platform policy:** Does the platform permit the requested access/action and automation?
5. **Technical capability:** Does Memo have a verified connected capability for this exact action?
6. **Human approval:** Has Yusuf approved the exact destination, content and effect?
7. **Client authority:** If acting inside a client's systems, is this within signed scope and delegated permission?

Passing one gate does not imply passing the others.

## 2. Polish / EU considerations to expose to the operator

The system should make the following risks visible and avoid hard-coding simplistic legal advice:
- Polish Electronic Communications Law (Prawo komunikacji elektronicznej), including Article 398, regulates use of telecommunications terminal equipment / automated calling systems for transmitting commercial information, including direct marketing. Whether a particular B2B email, phone call, platform message or other channel is permitted requires a channel- and fact-specific assessment.
- GDPR applies where personal data is processed. Public availability is not a blanket legal basis for every reuse or direct-marketing purpose. Legitimate interest, transparency, objection rights, consent and suppression need case-specific assessment.
- A public business email or phone number must not automatically be interpreted as permission to send a marketing message.
- An inbound request authorises a response relevant to that request; it does not automatically authorise unrelated future marketing.
- An event badge scan, business card or referral is not automatically blanket consent for ongoing marketing. Record the actual expectation and permission.
- A referrer should establish that the recipient is open to an introduction before personal details or a sales pitch are sent.
- Cross-border rules may apply for recipients outside Poland/EU.

Memo should display a “requires legal review” state when applicability is uncertain. It must not make definitive legal determinations from a generic template.

## 3. Suppression and objection handling

A contact/account can be suppressed for a channel or globally:
- Explicit opt-out / unsubscribe.
- Objection to direct marketing.
- Complaint or request not to be contacted.
- Wrong person / misdirected message.
- Legal or platform restriction.
- Unclear permission.

Suppression should be checked before every new action. Preserve only the minimum information necessary to honour suppression, with an appropriate retention policy. Never auto-clear an objection.

## 4. Approval model

Approval must be:
- Per external action, not a blanket approval for an entire campaign.
- Bound to exact account, recipient, platform, connected account, content, attachments and payload.
- Time-limited where appropriate.
- Invalidated by any material change.
- Recorded with approver, timestamp, payload hash, outcome and evidence.
- Idempotent: retries must not duplicate an external action.
- Explicit about uncertain outcomes; do not report success until verified.

High-risk actions require stronger confirmation:
- Public posts/comments.
- Direct marketing.
- Sharing or exporting client data.
- Changing/deleting CRM data.
- Quoting prices or contractual terms.
- Signing, accepting, purchasing or paying.

## 5. Data minimisation and security

- Collect business facts before personal contact details.
- Store source URL, retrieval time and purpose.
- Avoid sensitive personal data entirely unless the user has a specific lawful need and the system supports it.
- Do not store passwords, cookies, tokens or session secrets in work records.
- Keep local vs remote model data-sharing status visible.
- Require explicit user consent before sending local workspace content to a remote model.
- Apply least privilege and revoke client access at engagement end.
- Keep an audit log of data access and external actions.
- Provide export, correction, deletion and retention controls as appropriate.

## 6. Platform capability registry

For every platform/action pair, track:
- Platform and account type.
- Official API / browser / manual route.
- Required permissions and review status.
- Supported read/write operations.
- Rate limits and terms constraints.
- Authentication status and token expiry handling.
- Whether human-in-the-loop is mandatory.
- Evidence of last successful test.
- Failure and rollback behaviour.

Allowed capability states:
- **Available:** verified and connected.
- **Needs connection:** integration exists but user must connect.
- **Needs permission:** platform/app approval required.
- **Manual step:** user performs action outside Memo.
- **Unsupported:** no safe supported route exists.
- **Blocked:** policy, consent, legal or security gate failed.

Do not label an action “Available” based only on a UI mockup or a successful login.

## 7. Failure handling

- If permission cannot be verified, stop before sending.
- If an action times out after submission, mark “Outcome uncertain”; verify destination before retrying.
- If a token expires, request reconnection; never ask user to paste secrets into chat or notes.
- If platform changes API/terms, disable affected capability until revalidated.
- If duplicate records are detected, preserve provenance and ask before merging destructive data.
- If user requests an unsafe or non-compliant workflow, explain the blocked step and provide a safe alternative.

## 8. Human authority

Memo is an assistant/operator, not the business owner. Yusuf decides whom to approach, what offer to make, what fee to charge and whether to accept work. The client decides its own customer commitments and CRM changes. Memo must not invent authority based on access to a browser or account.
