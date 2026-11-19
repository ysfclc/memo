# Memo browser visibility and approval recovery — 19 November 2026

## User-reported problems

- Browser automation felt hardcoded around Frontier providers.
- Selecting providers did not visibly open the browser, even though the user reported signed-in accounts.
- The approval button was not discoverable.
- Checkpoints and repeated approval requests were confusing.

The original reports remain in USER_INPUT_LEDGER.md as INPUT-2026-11-19-004 through -006.

## Confirmed source-level causes

1. BrowserWorkflowScreen started BrowserSession.startBrowserWorkflow(), which calls navigate(provider.startUrl), but the Android WebView was hosted only by the dedicated BrowserScreen. The workflow section did not switch to BrowserScreen after starting. Navigation could therefore be requested without a visible browser surface.
2. ActionApprovalInboxScreen showed "Open live approval" only for a matching live pending record. Its callback selected ToolSection.BROWSER, which showed the ordinary browser rather than the workflow screen containing the exact-prompt AlertDialog.
3. BrowserWorkflowScreen kept the current external action ID in transient Compose state. Moving between Browser and Workflow could recreate the screen and lose the UI-side identity needed by later response-capture callbacks.
4. Local intent structuring defaulted on whenever any local model was installed, adding an implicit local inference stage before browser navigation.
5. The broad preflight consent checkbox duplicated the exact per-provider prompt approval that already displays the destination and full outbound payload.
6. The workflow mapped every WAITING_FOR_USER state before submission to a NEEDS_REVIEW job step, including ordinary manual sign-in/page setup.

## Changes made

- Keep the WebView hosted only by the dedicated BrowserScreen. Do not embed AndroidView inside the independently scrolling workflow screen; WebView can render above Compose siblings.
- On successful workflow start, switch to the dedicated Browser screen so the provider page is visible.
- When a prompt awaits exact approval, BrowserScreen displays a prominent "Review prompt & approve" action back to workflow controls.
- The Action Approval Inbox uses the same label and returns to the workflow confirmation surface, not the ordinary browser.
- After the exact prompt is approved, return to the visible Browser screen for submission and response capture.
- Store activeExternalActionId in BrowserSession so it survives Browser/Workflow screen recreation.
- Make local intent structuring opt-in by default.
- Remove the duplicate broad preflight consent checkbox. Keep the exact-content, destination-bound, one-use approval before every provider send.
- Do not label manual sign-in/page setup as NEEDS_REVIEW approval. Reserve NEEDS_REVIEW for exact approval, sent or uncertain actions. Label paused Work jobs "Needs your input".
- Corrected the generic runtime capability policy: browser.prepare_prompt (fill only) and browser.run_multi_chat_workflow (orchestration) do not create blanket approvals; browser.submit_prompt is the external side effect and remains explicitly approved. CapabilityCompiler adds browser.submit_prompt for external browser sends, and tests guard the distinction.
- Extend tools/verify_source.py with source-level regression checks for these routes and distinctions.

## Physical-device acceptance checklist

Do not uninstall Memo or clear app data. Install the next signed APK over the existing installation.

1. Open Browser → Workflows. Select at least two provider presets and enter a test goal.
2. Leave local intent structuring off (the default). Start the workflow.
3. Verify Memo switches to the dedicated Browser screen and visibly opens the first selected provider. Verify loading progress and explicit page errors.
4. If sign-in is required, sign in inside Memo's embedded WebView. Do not assume Chrome's cookies are shared.
5. Verify navigation/read-only page inspection does not ask for external-action approval.
6. When the exact prompt is ready, verify BrowserScreen shows "Review prompt & approve". Tap it.
7. Verify the workflow dialog displays provider, destination and the complete exact outbound prompt. Verify the button says "Approve and send".
8. Approve once. Verify Memo returns to the visible browser, submits once, and captures the response. Verify no second approval for the same unchanged payload.
9. Open Action Approval Inbox while another exact prompt is waiting. Verify "Review prompt & approve" routes back to the matching workflow dialog. Reject and verify nothing is sent.
10. Verify sign-in/page-setup pauses are labelled as user input, not as external approval. Verify uncertain submission outcomes remain in Needs review and are never automatically retried.
11. Verify Chrome-only login does not silently appear as an authenticated Memo session; the embedded WebView must show its own actual state.

## Remaining work

- BRW-14: general-purpose browser target resolution; Frontier websites remain optional presets, not the architecture boundary.
- BRW-15: physical-device navigation and provider-session acceptance.
- APR-01: consider direct inbox approval only if it can use the same canonical exact-action gate; no parallel approval ledger.
- CHK-01: complete the cross-runtime audit of WAITING_FOR_USER, NEEDS_REVIEW and approval producers.
- CI, APK signing, API 35 in-place upgrade and device acceptance must pass on the exact final branch head before calling this phase verified.
- BRW-14 remains partial: generic read-only browser tasks exist for arbitrary HTTPS pages, but the universal DOM/action executor, dynamic intent-to-target resolution and independently verified action postconditions are not yet implemented.

## Verification boundary

These changes are source-committed but not yet verified by exact-head CI or a physical device. No real provider session, phone data or sign-in state has been tested.
