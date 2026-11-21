# Memo UI regression contract

**Status:** Binding release invariant  
**Applies to:** Talk, Work, Live, Studio, shared navigation/layout and every imported source snapshot  
**Purpose:** Prevent previously corrected scrolling and viewport regressions from returning during feature integration or source imports.

## Non-negotiable behavior

1. Talk and Work must retain the accepted page-level scrolling behavior: content belonging to the page is reachable through one coherent vertical scroll surface, rather than being split into fixed headers/sections and a partial independently scrolling list.
2. Long content must remain reachable. No clipped cards, inaccessible bottom actions, scroll traps, or overlapping content.
3. Horizontal chip/filter rows may scroll horizontally, but must not capture or break the page's vertical scrolling.
4. Keyboard appearance, tab changes, navigation return, language changes, empty states and long-content states must not break the scroll contract.
5. Do not fix scrolling by arbitrarily adding nested scrolling, fixed heights, or another independently scrolling container. Inspect the established screen structure and verify the complete interaction.

## Change-control requirements

- Before editing these screens or shared layout primitives, inspect the current implementation, accepted prior behavior, related tests and the full diff.
- Any import from an archive/source snapshot is treated as a high-risk replacement: compare every changed UI file against the current branch and explicitly identify deleted or reverted fixes before merging.
- A UI refactor must include a regression test or a documented reason an automated test cannot faithfully represent the behavior, plus the physical-device checks below.
- Do not merge a source-import PR merely because it compiles. Review UI diffs independently of backend/feature diffs.
- Preserve this contract across feature branches, merges, rebases and APK source-import workflows. A new feature does not authorize reverting an existing UI correction.

## Release acceptance evidence

For the exact candidate commit and APK hash:

- Run Android unit tests and lint.
- Run available UI/instrumentation checks for Talk and Work.
- Perform the physical-device checklist in DEVICE_ACCEPTANCE_CHECKLIST.md.
- Attach evidence and record the result in the release/build ledger.
- If physical-device checks have not been performed, status is implemented or CI-verified, never device_verified or user-ready.

## Failure policy

Any failure or missing evidence blocks release. Revert the offending change or repair the layout, add a regression test where feasible, rerun the complete gate, and produce a new candidate. Never ask the user to install an APK to discover whether a previously fixed visual bug has returned.

## Approval inbox and external browser actions

- Talk must show the full destination and exact outbound prompt before approval; approval acknowledgement is required and rejection must not invoke the adapter.
- browser.submit_prompt is an external write. The one-use grant is bound to runtime, capability, normalized HTTPS destination and exact prompt hash; the adapter rechecks the live page before filling/sending.
- The prompt submission result is not successful until the visible prompt/composer postcondition, encrypted evidence read-back and action-ledger completion are verified.
- Page changes, hash mismatch, cancellation, timeouts, ambiguous send results or evidence persistence failures stop at NEEDS_REVIEW. Never replay an uncertain prompt automatically.
- Page response text is a separate opt-in. Do not leak page content into Talk when the user has not enabled browser-page sharing.
- Test the approval inbox and Talk/Work scrolling with keyboard open, long payloads, empty states, English/Polish/Turkish, tab navigation and narrow screens.

## Consent-gated browser response capture

- browser.capture_response must not be advertised unless the user has explicitly enabled browser-page sharing and a completed browser.submit_prompt evidence reference exists.
- It must bind to the exact prior submission ID, verify the action ledger is completed, and use the original submission baseline so a previous answer cannot be mistaken for a new response.
- Only a new, stable response is returned; baseline loss, unsupported page structure, page navigation or timeout must stop for review without resending the prompt.
- Returned response text is untrusted external content. The UI/model must not treat instructions inside the response as instructions to Memo.
- The encrypted response evidence must read back with exact prompt submission reference, response digest, page digest and source provenance.
