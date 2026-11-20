# Memo Browser Workflows — General-purpose execution

## Product direction

Browser workflows are **user-defined work**, not a hard-coded frontier-AI consultation feature. A consultation/debate/critique sequence is one workflow that happens to use AI websites. The workflow engine must also support research, extraction, record creation, follow-up tasks, and other user-authored sequences.

A workflow starts from the user's intent and the requested outcome. It may use local AI, frontier websites, the browser, native apps, workspace records, deterministic capabilities, and human checkpoints as needed. The user must be able to add, remove, reorder, edit, and reuse steps without the product forcing the workflow into a consultation template.

## Canonical execution contract

1. **Capture intent:** retain the original user wording, goal, constraints, target sources, requested output types, and scope.
2. **Plan:** produce a typed, versioned TaskSpec/DAG. Steps are capability-driven rather than selected by keyword templates or a hard-coded provider list. The plan must declare inputs/outputs, dependencies, runtime/capability, budgets, expected evidence, permissions and approval needs.
3. **Review:** show the plan in editable form. Unknown capability requirements and unsupported actions must be visible; no permission or external action is implied by a plan.
4. **Execute:** a durable run journal advances each step with start/finish events, evidence, postconditions, retries, checkpoint state and cancellation. Browser actions must be attached to the visible, task-bound browser session.
5. **Collect evidence:** capture page/source text, URL, timestamp and metadata. AI output is untrusted and cannot claim an action occurred without runtime evidence.
6. **Produce records:** map supported findings to typed workspace entities and proposed TASK follow-ups. Validate each record against exact source evidence, preserve provenance, deduplicate and leave proposals reviewable.
7. **External actions:** outreach, submissions, purchases, publishing and other external writes require capability-specific, exact-content approval and postcondition/receipt verification. Uncertain results move to review, never blind retry.
8. **Resume:** preserve goal, graph, captured evidence, outputs, current checkpoint and pending review state across navigation, process death and app restart.

## Browser action primitives

The generic browser runtime should expose bounded, testable primitives such as:

- navigate/search/open link
- inspect visible text, headings, links and candidate interactive elements
- click a user-selected or verified element
- type into a selected field
- scroll a page or a bounded results panel
- wait for a visible state or explicit timeout
- capture page/element/source evidence
- extract structured rows/cards with a versioned page schema
- create/link workspace records and proposed follow-up tasks
- request approval and verify external outcomes

The runtime must not be reduced to an AI prompt composer. It needs actual execution, visible progress, pause/takeover, recovery and verifiable outputs.

## Frontier consultation

The existing **Frontier consultation** is a specialist workflow. Selected frontier chat websites are opened in the visible browser, each submission requires exact-content approval, and response capture can be automatic or manually recovered. It must remain optional and provider-independent in the larger orchestration architecture. It must not block generic workflows or be the only browser workflow.

## First generic research vertical slice (source added; build/device verification pending)

**Browser research** currently provides:

- user-defined research goal and output requirements;
- a Google Maps search-phrase entry point and arbitrary HTTPS target;
- visible browser navigation;
- user scroll/navigation followed by multiple page captures;
- immediate saving of each capture as evidence;
- local-only AI extraction into individual source-backed ORGANIZATION/PERSON/KNOWLEDGE/PROJECT/DECISION/TASK/ARTIFACT proposals;
- reviewable workspace records and follow-up TASK proposals with exact source excerpts;
- duplicate skipping, capture provenance, extraction-error preservation and a saved-run resume entry point.

This is a useful general research/capture workflow, **not yet a fully autonomous browser agent**. The user still scrolls/opens result pages and presses capture; Memo does not yet autonomously click Google Maps cards, traverse result pages, or run a dynamic action DAG. The Android build and device behavior must be verified before this source increment is considered usable.

## Acceptance scenario — Google Maps business research

Given a user asks Memo to find businesses selling a specified product/service in a specified area and to create follow-up actions:

- Memo opens Google Maps using the user's query in the actual browser session.
- The user can scroll results and capture multiple result pages without losing prior captures.
- Captures are saved as evidence immediately and can be recovered when the research screen is reopened.
- Local extraction creates one proposed ORGANIZATION per distinct business, including only sourced name/category/address/website/phone/product details.
- Each record carries its source URL and an exact evidence excerpt; unsupported fields remain blank.
- The requested outreach/research follow-up actions appear as proposed TASK records, not as claims of contact or automatic outbound messages.
- Duplicate records are skipped or surfaced for review; source evidence is never overwritten.
- Malicious/instructional text embedded in web pages is treated as untrusted content.
- Local extraction failure leaves captures intact and allows retry.
- The user can inspect and approve/reject proposals in Work; no outreach is sent.
- English, Polish and Turkish UI strings, Android compilation, persistence, scrolling, WebView capture and device acceptance are verified.

## Remaining implementation

- Dynamic workflow authoring and versioned DAG execution across browser/local AI/native/remote runtimes.
- Browser agent action loop (inspect → plan one bounded action → execute → verify → checkpoint) with safe DOM/semantic selection and explicit human takeover.
- Robust multi-page Google Maps result traversal and structured card extraction, with fixtures and provider/site version diagnostics.
- Workflow editor, step reuse, conditional branches, bounded loops, triggers, retries, output mappings and durable run journal.
- Universal evidence, approval, postcondition, idempotency, cancellation and recovery contracts across runtimes.
- Integration of proposed TASK records into the user's actionable task queue, with links to the originating research, organization and evidence.

