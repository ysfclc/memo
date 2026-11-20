# Memo UI scroll and layout repair — 18 November 2026

## Scope
Repair the actual screen layout defects in Talk and Work without changing the Signal & Graphite visual identity, product navigation, data model, runtime behavior, approval policy, or persistence.

## Findings from source inspection
- Talk's conversation-list state uses a fixed Column for its page header, Council/Import actions, prompt panel and search field, then gives only the conversation cards a separate LazyColumn. The empty state is not scrollable. This makes the page's upper sections immovable and can leave the content area unusably small on short displays or with larger text.
- Talk's open-conversation state separately pins a potentially tall request-controls panel above the message list. Keep the composer and message history usable; avoid nested vertical scrolling.
- Work places its page header, section chips and execution tools outside the content list. Today, Entities and Plans then create independent LazyColumns, while empty states are fixed. The result is inconsistent page-level scrolling and a cramped content viewport.
- Studio and Live already use page-level lazy lists, so Talk and Work should follow the same basic scrolling contract.

## Implementation plan
1. Make the Talk conversation index one page-level lazy list, including its header, actions, prompt, search, empty state and conversation cards.
2. Make Work one page-level lazy list, including its header, section navigation, execution shortcuts, running status, search/review state and section content. Use LazyListScope items rather than nesting scroll containers.
3. Preserve the selected-conversation message timeline and bottom composer as dedicated interaction regions; ensure tall request controls can be reached without covering the timeline.
4. Add/adjust source-level regression checks for the single-scroll-container contract and run the existing CI verification/build.
5. Record the exact commit, CI result and any remaining device-only acceptance requirement below.

## Acceptance criteria
- Talk's conversation index scrolls from the page heading through the final conversation/empty state.
- Work's Today, Entities, Archive and Plans sections scroll from the page heading through the final content item.
- No nested vertical scroll containers are introduced in either page.
- Horizontal chip rows remain horizontally scrollable.
- The bottom navigation, top app bar, actions, callbacks, record keys and data behavior remain intact.
- Existing tests, source verification, lint and APK build pass.
- Physical-device visual/touch verification is still required; source/CI checks alone do not prove pixel-level behavior.

## Status
- [x] Source-level root causes identified.
- [ ] Talk page-level list implemented.
- [ ] Work page-level list implemented.
- [ ] Regression checks pass.
- [ ] CI build and APK identity pass.
- [ ] Physical-device scroll/overlap acceptance.
