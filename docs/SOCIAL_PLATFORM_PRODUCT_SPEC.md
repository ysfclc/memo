# Memo Social Platform — Product Specification

**Status:** Product requirements restored; implementation not claimed  
**Date:** 9 November 2026  
**Parent contract:** [MEMO_PRODUCT_CONTRACT.md](MEMO_PRODUCT_CONTRACT.md)  
**Execution ledger:** [../TASKS.md](../TASKS.md)

## 1. Why this exists

Social media is a first-class Memo domain, not a synonym for content generation or a small publishing feature. The intended experience is a personal, unified social operating layer inside Memo: connect the user's permitted accounts, understand activity across them, manage conversations and relationships, create and distribute content, measure outcomes, and turn useful activity into follow-through.

The user previously described this breadth as comparable in ambition to Unipile, but integrated into Memo's local-first personal workspace rather than exposed as a disconnected integration dashboard. This is a product ambition, not a claim that Unipile or any specific provider currently supplies every capability.

This specification preserves the intended scope while requiring honest, provider-specific capability checks. It does not promise that every platform exposes every operation through an official API.

## 2. Product outcomes

Memo should let the user:

1. See and manage the social accounts and identities they choose to connect.
2. Review a useful, configurable view of activity from those accounts and relevant people/topics.
3. Handle supported conversations, comments, replies and engagement from one place.
4. Understand people and organizations across social channels without creating duplicate contacts.
5. Research topics, people, companies, competitors and audience signals from permitted sources.
6. Create, adapt, review, schedule where supported, and publish content to selected destinations.
7. Understand content and account performance using real, source-linked measurements.
8. Automate repetitive monitoring and preparation, with user-defined rules and approval boundaries.
9. Connect social activity to projects, opportunities, CRM, customer acquisition, personal brand and monetization goals.
10. Keep control of what is connected, ingested, retained, shared, automated and published.

## 3. Capability areas

### 3.1 Accounts and identity

- Connect, disconnect, reconnect and inspect multiple accounts across supported providers.
- Show account identity, platform, permissions/scopes, connection health, last sync, limitations and required reauthorization.
- Support multiple profiles/pages/organizations where a provider permits them.
- Keep provider credentials/tokens in protected platform storage; never put secrets in logs, source control, analytics or workflow artifacts.
- Make account ownership and selected publishing identity explicit before any outbound action.
- Allow per-account and per-capability revocation.

### 3.2 Unified activity and feed

- Combine supported posts and activity from connected accounts, followed people/pages, selected topics, lists and saved sources.
- Support chronological and relevance-based views, with the ranking reason inspectable.
- Filter by account, person, organization, topic, media type, date and activity type.
- Save, bookmark, annotate, share into Memo, hide, mute or mark items for follow-up where source capabilities permit.
- Preserve source URL, provider, author identity, timestamp, retrieval time and available engagement context.
- Distinguish a genuinely synchronized feed from a browser shortcut, imported snapshot or incomplete provider feed.
- Support refresh, pagination, stale/offline state, duplicates and provider-specific omissions.
- Let the user tune relevance and exclusions; never silently hide or fabricate activity.

### 3.3 Unified inbox and conversations

- Aggregate supported direct messages, message requests, comments, mentions, replies and conversation threads.
- Group by person/conversation while preserving the original provider, account and thread identity.
- Show unread/read, assigned/follow-up, priority and response state where the provider exposes them.
- Search and filter conversations; link participants to Work people/organizations and existing CRM records.
- Draft replies with context and source visibility; user reviews and approves each outbound message unless a narrowly scoped, explicitly configured rule authorizes it.
- Support attachments and media only where the provider and app permissions allow.
- Never claim an inbox is unified when a provider does not expose a given message type or private conversation.

### 3.4 Engagement and relationship actions

Where supported by the provider, Memo can prepare or perform user-approved actions such as:
- like/react, comment, reply, repost/share, follow/unfollow, save/bookmark;
- manage supported page/community interactions;
- mark conversations for follow-up and create reminders;
- open the exact source item in its official app/site when the action is not safely or legally available through Memo.

Every action must identify the account, target, exact content and destination. Consequential external actions require current approval. A failed or ambiguous action is not retried blindly.

### 3.5 Publishing and content operations

- Maintain a private content calendar/queue across selected channels.
- Create text, image, carousel, short video, long video, story/reel/short-form variants and other formats only where actual asset tooling and provider capabilities exist.
- Adapt one source idea to channel-specific constraints without changing factual meaning or user-approved positioning.
- Support drafts, revisions, reusable content pillars, campaign/grouping, tags and asset provenance.
- Provide a destination-specific preview and show unsupported formatting/feature differences.
- Schedule only where a provider or approved scheduling service genuinely supports it; otherwise state that scheduling is unavailable and offer a reminder.
- Require explicit, content-bound approval for publishing. Record provider receipt/post ID, URL, account and timestamp where available.
- Handle partial multi-channel publication accurately: each destination has its own result and evidence.

### 3.6 Listening, discovery and research

- Monitor selected keywords, topics, people, brands, competitors and relevant public conversations where permitted.
- Discover potential contacts, collaborators, customers, communities, opportunities and questions worth answering.
- Summarize trends and recurring audience questions with links and dates.
- Compare competitor positioning, content themes and visible performance without inventing private analytics.
- Save findings as sourced research and link them to Work entities, projects and decisions.
- Provide configurable alert thresholds and quiet hours; no hidden or unbounded monitoring.

### 3.7 Analytics and learning

- Capture account and post metrics actually exposed by each provider or imported by the user.
- Link every metric to provider, account, post/version, measurement time, metric definition and source.
- Show trends, comparisons, campaign outcomes, audience growth and conversion signals only when the data supports them.
- Distinguish provider-reported values, imported exports, estimates and Memo-derived calculations.
- Learn which topics, formats, timing and calls-to-action perform for the user's goals; present recommendations with evidence and uncertainty.
- Do not infer reach, impressions, demographics, revenue or attribution when those fields are unavailable.

### 3.8 Personal brand, business and monetization

- Maintain approved positioning, biographies, expertise, offers, content pillars, voice and factual claims in the Self Model.
- Connect inbound enquiries and social relationships to CRM, opportunities, customer conversations and follow-up tasks.
- Track campaigns and funnel outcomes when the user supplies or connects the required evidence.
- Identify possible services, products, partnerships, audience needs and monetization experiments.
- Draft proposals, outreach and follow-ups grounded in approved facts and relationship context.
- Never fabricate testimonials, credentials, metrics, audience size, endorsements or customer results.

### 3.9 Rules and automation

- Let the user define bounded rules: monitor a source, classify activity, summarize, create a draft, queue a follow-up or notify.
- Separate read-only collection, internal preparation and external side effects.
- Require explicit opt-in, scope, limits, rate controls, quiet hours, audit trail and a global stop.
- Require per-action approval by default for sending messages, commenting, following, publishing, deleting or other externally visible actions.
- Any standing authorization must be narrow, understandable, revocable and visible in the account's permissions centre.
- No spam, unsolicited mass engagement, artificial engagement, deceptive impersonation, platform-limit evasion or unauthorized scraping.

## 4. Unified Memo experience

Social is not a fifth primary navigation silo. It is a connected domain surfaced through the existing four product surfaces:

- **Talk:** ask questions, capture social ideas, research people/topics, ask Memo to prepare replies or campaigns, and hand work into a social workflow.
- **Live:** see syncs, monitoring jobs, drafts awaiting approval, publishing runs, messages being sent and uncertain outcomes in the same chronological execution timeline as all other Memo work.
- **Work:** store accounts as connected capabilities and store people, organizations, conversations/references, campaigns, content plans, opportunities, follow-ups, sources and metrics in the canonical graph.
- **Studio:** create and edit content, media, campaign assets and channel-specific variants; compare revisions and inspect provenance before publication.

A unified view must not conceal provider boundaries. Every item/action shows which account and platform it belongs to.

## 5. Provider and capability architecture

Implement provider adapters against a common typed capability contract. Capabilities are independently discoverable and versioned, for example:

- account/profile read and connection health;
- feed/list/search read;
- post/thread read;
- inbox/message read;
- message send/reply;
- comment/reaction/follow/share write;
- media upload and post create;
- scheduling;
- account/post analytics;
- webhooks/events and incremental sync.

Each provider adapter must publish:
- supported capability IDs and unsupported capability IDs;
- API/permission scopes and review requirements;
- account types supported;
- pagination, rate limits, retention and webhook behavior;
- media/format constraints;
- provider terms/policy constraints;
- version, test fixtures, health and last verified date.

The planner must never infer a capability from a platform name. A workflow is compiled only from capabilities the selected account/provider actually exposes and the user has granted.

### Provider coverage register

This is a discovery and verification register, not a statement of current integration availability.

| Provider | Intended evaluation | Current Memo status |
|---|---|---|
| LinkedIn | Profiles, feed/activity, connections, messaging, comments, publishing, analytics, organization/page support | Not implemented / capability and policy audit required |
| Instagram | Profiles, feed/media, comments, messaging, publishing, insights, professional-account constraints | Not implemented / capability and policy audit required |
| Facebook Pages | Page feed, comments/messages, publishing, insights and page roles | Not implemented / capability and policy audit required |
| X | Timeline/search, posts/replies, messaging, publishing and analytics | Not implemented / API access and policy audit required |
| TikTok | Video publishing, comments, inbox and analytics where available | Not implemented / API and account eligibility audit required |
| YouTube | Channels, videos, comments, community activity and analytics | Not implemented / API and permission audit required |
| Reddit | Subreddit/feed discovery, posts, comments, messaging and moderation boundaries | Not implemented / API and policy audit required |
| Threads | Feed, replies, publishing and insights where exposed | Not implemented / API and eligibility audit required |
| WhatsApp | Existing separate Troy/Evolution-related workflow; evaluate supported business/personal boundaries and avoid duplicating or silently taking over that integration | Not part of Memo's verified implementation |

Provider coverage must be researched and revalidated before implementation because API access, scopes, pricing, review, account eligibility and terms change. Official APIs are preferred. User-authorized exports/imports may supplement gaps. A visible official browser/app session may be used only for permitted, observable interactions; it is not a license to scrape, bypass restrictions or claim full API parity. If a capability is unavailable, Memo must say so and provide a safe alternative such as opening the official destination or creating a reminder.

## 6. Data, privacy and security

- Social data is personal data. Local-first encrypted storage is the default for records, cached content, relationship links, drafts, metrics and workflow state.
- Minimize collection and retention; keep raw provider payloads only when necessary and within provider terms.
- Separate provider secrets from workspace content; use OS-backed protected storage and scoped tokens.
- Show exactly which account/data/context is being accessed or sent to a remote model/provider.
- Provide account-level disconnect, permission revocation, cache deletion and data export/delete controls.
- Keep immutable provenance for imported items and metrics; allow user correction without erasing original source evidence.
- Do not upload private messages, contact lists or account data to an AI provider without explicit, per-workflow context disclosure and consent.
- Apply the universal approval ledger, exact-content hash, destination binding, audit and NEEDS_REVIEW recovery to every outbound action.
- Enforce provider rate limits, user-configured limits and anti-abuse protections.
- No credentials/cookies in logs, screenshots, crash reports, source control or analytics.

## 7. UX acceptance principles

- One account connection centre with clear per-provider status and capabilities.
- One useful cross-provider activity view when data sources actually support it.
- One inbox view with provider-aware conversation details and honest coverage gaps.
- No dead buttons, fake counts, placeholder feeds, fabricated metrics or mock messages presented as live data.
- Empty, loading, offline, permission-denied, expired-token, rate-limited and partial-provider states are explicit.
- The user can always identify the active account before replying or publishing.
- EN/PL/TR strings and accessibility behavior are part of acceptance.

## 8. End-to-end acceptance scenarios

1. Connect two eligible accounts, inspect actual scopes and revoke one; verify the other remains connected.
2. Sync available feed items from both providers, preserve provenance, paginate, deduplicate and display a provider limitation without fabricating missing items.
3. Open a supported message/comment thread, link its participant to an existing Work contact, draft a reply, approve the exact payload and verify the provider receipt.
4. Create one content idea, generate channel-specific variants, edit/revise, preview both destinations, approve and publish; report each destination's independent result.
5. Ingest supported post/account metrics, link them to the exact post/version and generate an evidence-backed performance comparison.
6. Monitor a user-selected topic under explicit limits, produce a sourced digest and create a follow-up task.
7. Disconnect an account and prove its credentials are unusable, scheduled work is safely stopped/reviewed, and the user can delete cached data.
8. Demonstrate denial paths: expired token, missing scope, provider rate limit, unsupported capability, changed approval payload, ambiguous send outcome and global stop.

A screen mock, local fixture or browser shortcut is not end-to-end acceptance. Every test report must label fixture, sandbox, API integration and real-account/device evidence separately.

## 9. Delivery sequence

1. **Inventory and policy:** audit providers, APIs, scopes, terms, account eligibility, cost and capability coverage; make a provider matrix with source links and last-checked dates.
2. **Core contracts:** define account, provider capability, social item, conversation, message, engagement action, content post, campaign and metric schemas; migrations and encrypted storage.
3. **Read-only foundation:** connect/revoke accounts, health, incremental sync, feed, search, provenance, deduplication and privacy controls.
4. **Inbox and relationships:** supported threads, contact linking, reply drafts, approval and verified receipts.
5. **Studio and publishing:** asset pipeline, variants, calendar/queue, previews, per-destination approvals, publish receipts and recovery.
6. **Listening and analytics:** monitoring, alerts, metrics ingestion, evidence-backed comparisons and learning loop.
7. **Automation and growth:** bounded user-authored rules, CRM/opportunity workflows, personal-brand operations and monetization experiments.
8. **Hardening:** provider contract tests, rate-limit/permission failure tests, security review, account deletion, localization, accessibility and real-account acceptance.

Do not start with a decorative feed UI. First establish the provider capability truth, domain contracts, security model and data ownership.
