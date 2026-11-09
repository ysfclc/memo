# Memo — Experience and Navigation Rebuild

**Status:** product/UX direction approved for implementation; source implementation not yet complete  
**Date:** 9 November 2026  
**Target:** Android app in `ysfclc/memo`  
**Supersedes:** the assumption that the current four-item bottom bar and current screen hierarchy are fixed.

## 1. Product truth

Memo is one personal operating agent, not a collection of mini-apps. The user should express an outcome once; Memo should bring the right context, capabilities, people, runtimes and evidence into one continuous experience.

The interface must serve the complete ambition already agreed for Memo:

- Talk and voice as the universal intent/capture interface.
- Real execution across local models, frontier AI sessions, websites and permitted Android apps, with visible checkpoints and user control.
- A persistent personal/work graph: goals, projects, tasks, contacts, companies, opportunities, jobs, sources, decisions, workflows and evidence.
- Social media as a major first-class domain: connected identities, unified feed, inbox for DMs/comments/replies, discovery and filtering, engagement and relationship management, trends/audience/competitor intelligence, publishing, analytics and bounded automation. Initial priority platforms include LinkedIn and Instagram; WhatsApp remains a separate communication domain. Social activity must connect to people, projects, conversations and goals.
- Studio for useful deliverables: research, plans, career documents, content drafts, images/video/carousels as supported, revisions, previews and exports.
- Privacy-first local storage, explicit remote disclosure, exact-scope approval for consequential actions, reliable recovery and honest evidence.

Social is not just a content calendar or a publish button. It must not be reduced to a Studio sub-screen. It is a first-class product domain and deserves a first-class navigation destination when its real workflows are implemented.

## 2. Proposed primary navigation

Use five destinations, in this order:

1. **Talk** — one place to think with Memo, capture anything, start/resume conversations, use voice, attach/share inputs, invoke Council and turn intent into inspectable work.
2. **Social** — unified social workspace: accounts/identities, relevant feed, inbox, discovery, engagement, relationships, publishing queue and performance intelligence. The user must not need to jump between platform apps for routine review. Actions that cannot be safely or officially integrated remain visible handoffs, never fake controls.
3. **Live** — one chronological execution timeline across all runtimes. Show queued/active/waiting/blocked/failed/cancelled/completed/uncertain states, current step, participant, target, timestamps, evidence and controls to pause, resume, cancel, take over, inspect and approve.
4. **Work** — the user's persistent world and daily priorities. Its default landing view is **Today**, organized as MUST / SHOULD / COULD / NOT TODAY, with concrete outcomes and done conditions. The same workspace provides searchable projects, goals, tasks, people, companies, jobs, opportunities, sources, decisions, workflows and evidence.
5. **Studio** — create and manage real deliverables: private drafts, content assets and platform variants, research, plans, career documents, revisions, provenance, previews and exports. Publishing is a separate Social action with an exact destination/content preview and approval.

Five is the maximum primary bottom-navigation set. Do not add Browser, Apps, Approvals, Models, Council, Settings, Evidence or individual CRUD categories as extra bottom tabs.

### Navigation behavior

- Each destination retains its own navigation/back stack and scroll position. Switching tabs must not silently discard a draft, active conversation, editor state or selected record.
- Deep links open the exact record or execution and preserve a clear way back to its owning destination.
- Android Back first closes an overlay/detail, then returns within the current destination, then returns to the last primary destination; it must not unexpectedly jump to Talk or Home.
- Opening Memo should land in Talk's useful start state: a fast intent composer plus relevant unfinished conversation/context. Work > Today remains one tap away and can be surfaced contextually when urgent priorities exist. Do not turn the Talk entry into a generic metrics dashboard.
- Android share-sheet, notification, shortcut and voice entry all resolve into the same Talk intent flow, preserving the incoming material.
- A single global search may search only sources it can actually index. Results must identify their type and open the exact record; unsupported sources must not be implied.
- No duplicate action buttons for the same task. Use one clear primary action per screen, with secondary actions in contextual menus or details.

## 3. Supporting navigation, not more primary tabs

- **Browser and Android Apps** are visible runtime panes inside Live, and may be opened contextually from a task. They are not separate products.
- **Approvals** appear at the exact checkpoint in Talk/Live/Social/Studio and in a consolidated review queue reachable from Live. Approval must bind to exact content, destination, capability and scope.
- **Council** is a Talk workflow with preserved participant responses, disagreements and synthesis—not a disconnected destination.
- **Models, providers, capabilities, permissions, diagnostics and Settings** live under a compact system/settings area. Advanced implementation diagnostics must not be the everyday navigation.
- **Evidence** is attached to the relevant conversation, record, run or artifact; a global evidence browser remains available from Work search rather than competing with user goals.
- **Today** is the default Work landing view, not another bottom tab. It prioritizes the user's day without creating a second task database.

## 4. Social workspace definition

Social must support the complete loop, not a static dashboard:

- Account and identity connections with clear provider, permissions, health and last-sync state.
- Unified feed with source attribution, relevance controls, saved items, mute/ignore/block where the provider supports it, and links to the underlying conversation/contact.
- Unified inbox for DMs, comments, mentions and replies, with unread state, assignment/follow-up and relationship context.
- Engagement actions such as reply, comment, react, follow/unfollow and ignore only where the provider officially permits them and Memo can verify the outcome.
- Discovery and research across topics, trends, audiences and competitors with source provenance and freshness.
- Content creation handoff to Studio; platform-specific variants and previews; scheduled/queued publishing where supported; exact preview and explicit approval before any external write.
- Analytics for available impressions, clicks, engagement, audience growth and downstream outcomes. Missing metrics must be labelled unavailable, never fabricated.
- Rules and bounded automation with clear triggers, scope, limits, audit history, stop and review controls.
- Social signals connect to Work's people, companies, projects, goals and conversations. The user can personalize what is important and what should be ignored.

Provider API and policy limits must be represented honestly. A visible browser/app handoff is preferable to a fake universal integration. Never scrape, bypass access controls or claim a provider action succeeded without observable evidence.

## 5. Screen design principles

- One dominant purpose per screen; one obvious next action.
- Fast capture first, complexity on demand. Progressive disclosure, not a maze of specialist screens.
- Clear hierarchy, generous touch targets, readable typography, keyboard/IME-safe layouts and responsive phone/tablet behavior.
- Consistent components, spacing, color semantics, iconography and motion. Useful state transitions and haptics; honor reduced-motion and accessibility settings.
- Strong empty, loading, offline, permission-denied, partial, error and recovery states. Explain what is missing and what the user can do next.
- Drafts and unsent messages are durable. Clear only after persistence succeeds. Warn before destructive changes and provide undo/recovery where possible.
- Status language must match the actual system state. No invented metrics, fake progress, placeholder buttons, simulated connected accounts or unverified completion.
- Security is part of the experience: private by default, remote disclosure visible, exact-action approvals, safe takeover, stop control and clear uncertainty.

## 6. Rebuild sequence

### UX-0 — Information architecture and state ownership
- Map every existing route, composable, callback, state owner and deep link to the five destinations or a supporting destination.
- Define a typed route model and per-destination back stack. Preserve current route IDs/deep links during migration.
- Separate navigation orchestration from screen rendering; the current giant `MemoWorkspaceApp.kt` must not grow into the new navigation framework.
- Add navigation contract tests for tab selection, nested back, deep links, draft retention and process recreation.
- Do not delete old routes until all incoming links and user data paths have a tested replacement.

### UX-1 — Talk as the universal entry
- Remove the legacy Home/dashboard detour from the primary path.
- Combine new intent capture, conversation history and active conversation in one coherent Talk destination.
- Preserve text/voice/share/file input and Council context; no forced choice between “chat” and “plan”.
- Keep a draft until durable persistence succeeds.

### UX-2 — Social as a real destination
- First audit existing provider/session/data capabilities and legal/API constraints.
- Define provider-neutral account, feed item, thread/message, engagement action, content publication and metric contracts.
- Build real account health, feed and inbox states before adding a Social tab. Never ship a dead or decorative destination.
- Integrate Social events with Work relationships and Live execution.

### UX-3 — Live as the execution control room
- Replace disconnected summary links with a unified event timeline and contextual browser/native runtime panes.
- Add pause/resume/takeover/cancel/approval and uncertain-outcome recovery only where the underlying runtime supports them.
- Keep all claims evidence-backed.

### UX-4 — Work as Today plus the persistent graph
- Make Today the useful default Work view with MUST / SHOULD / COULD / NOT TODAY.
- Add exact-record search, provenance, relationships, revisions, archive/undo and outcome-based tasks.
- Connect CRM, job search, projects and follow-ups to one graph rather than separate stores.

### UX-5 — Studio as the deliverable workspace
- Generalize beyond the current Self Model/CV flow.
- Add private drafts, source/context manifest, revision history, comparison, editing, preview and export.
- Route channel publication through Social and its approval policy.

### UX-6 — System surfaces and polish
- Move Browser/Apps/Approvals/Models/Council/Settings to contextual or supporting routes.
- Accessibility, localization (English/Polish/Turkish), performance, keyboard safety, reduced motion and recovery.
- Remove dead routes and duplicate controls only after route/data compatibility tests pass.

## 7. Acceptance journeys

1. **Open and act:** launch Memo, see one clear Talk entry, capture a voice/text/share intent, preserve it, clarify only material ambiguity, and receive an inspectable plan or answer.
2. **Return to work:** resume the exact conversation or task, inspect today's MUST/SHOULD/COULD/NOT TODAY priorities, and open the linked record without losing state.
3. **Social morning review:** see connected account health, a relevant unified feed and inbox; reply or create a follow-up with relationship context; no fake data or unsupported actions.
4. **Publish safely:** create a content asset in Studio, preview platform-specific variants in Social, approve the exact payload and destination, publish through a supported provider path, and verify a receipt.
5. **Watch execution:** move between Talk and Live while a job runs; inspect browser/native activity, pause or take over, cancel safely, and resolve uncertain outcomes without blind replay.
6. **Create and retrieve:** produce an evidence-grounded artifact, revise it, compare versions, export it, and find it later through Work.
7. **Recover:** rotate between tabs, background the app, lose network or encounter a runtime failure without losing drafts, active context or audit history.

## 8. Hard guardrails

- Preserve package/application ID, signing identity, Keystore aliases, encrypted workspace data, schemas, backup compatibility and migrations.
- Do not replace storage, rewrite app identity or delete old navigation routes as part of a visual pass.
- Do not make external actions more permissive to simplify the UI.
- No social connector or action may be presented as working until its provider path, permission, policy and result verification are real.
- Every interactive control must be wired and tested. A navigation tab must never open a placeholder.
- No broad visual rewrite before navigation/state ownership and the critical user journeys are testable.
- Keep source, test, CI, artifact and physical-device evidence distinct. No completion claim without the relevant evidence.
