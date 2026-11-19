# Memo — user-facing workflow and control audit

**Date:** 13 November 2026  
**Repository:** `ysfclc/memo` (Memo 1)  
**Inspected branch:** `stabilization/usable-stage-2026-11-10`  
**Inspected source baseline:** `2b92810b5a913ace1ed9e8064112f48f7b8392c2`  
**Status:** Source-level findings documented; runtime behavior still requires device reproduction.  
**Related gate:** [STOP-WORK stabilization plan](MEMO_STABILIZATION_PLAN_2026-11-13.md)

## 1. Executive finding

The user-facing problem is a broken intent-to-result journey. Memo lets a user enter a prompt, prepare and approve a provider-specific prompt, and send it through a browser session. But it does not reliably return the provider's answer to a place where the user can edit, compare, continue, or act on it. The browser workflow is isolated from Council, run history is not presented with the templates, and the user loses task context when moved into the full browser.

This is not just a navigation or styling problem. It breaks the core promise of Memo as a user-controlled workspace for getting work done.

**Primary defect statement:** sending an approved prompt to an external AI provider is not a completed task. The user must regain control of the returned answer inside Memo, even if automatic page detection fails.

This document records requirements and defects for stabilization. It does not authorize unrelated feature expansion, redesign, merge, release, or deployment.

## 2. The reported scenario

The concrete user journey under review:

1. User opens Memo and enters a natural-language prompt.
2. Memo enhances or structures the prompt.
3. User reviews and approves sending the prepared prompt.
4. Memo opens or uses a browser provider session and sends it.
5. Provider produces an answer.
6. Memo is expected to capture that answer and make it usable in the user's workspace.

Reported actual experience:
- Council appears limited to local/on-device models rather than the browser-connected frontier AI sessions.
- Browser workflow is a separate destination and takes over the screen.
- Saved templates are visible, but prior workflow runs and their results are not presented as a coherent history.
- The approved prompt is sent, but the answer is not reliably captured into Memo.
- When the flow gets stuck, the user reports having no meaningful step-level controls beyond cancelling the whole operation or saving.
- There is no obvious way to manually capture the visible provider answer and continue working with it.

The reported behavior is treated as a user-observed failure requiring reproduction on a device. The source findings below explain why the architecture can produce this experience.

## 3. Source-level findings

### UWF-01 — Council and browser participants are separate systems

**Finding:** Council participant construction uses installed local models plus a configured OpenAI-compatible remote endpoint. It does not automatically enumerate or reuse authenticated browser sessions for ChatGPT, Claude, Gemini or other web providers.

**User impact:** signing into a frontier AI provider in Memo's browser does not make that provider available as a Council participant. The user experiences two disconnected AI systems and cannot naturally combine local and browser-connected participants in one Council run.

**Expected behavior:** Council should present the capabilities actually available to the user with clear runtime labels: on-device model, configured API model, or browser-connected provider. Browser sessions must not be represented as API access; session use must respect provider UI, account state and approval constraints.

**Acceptance:**
- A provider authenticated in a supported browser session is either selectable as a browser participant or clearly marked unavailable, with the reason.
- Local, API and browser participants are visibly distinguished.
- A Council run shows which runtime actually handled each participant.
- No browser session is silently treated as an API key or bypassed through undocumented endpoints.

### UWF-02 — Browser workflow is a separate product surface

**Finding:** Council and browser workflow have separate routes, state and execution paths. Browser workflow is reached as a dedicated Live/Browser/Workflows experience rather than as a runtime panel attached to the user's current intent.

**User impact:** the user must leave the main task context to use a provider. The workflow feels like a second application rather than a capability available to the current conversation or Council run.

**Expected behavior:** the active task remains the primary context. Browser execution appears as an embedded or adjacent runtime panel within that task. The user can inspect and control the browser without losing the prompt, plan, participants, progress or captured outputs.

**Acceptance:**
- Opening the browser does not navigate away from the active task.
- Returning from or resizing the browser preserves prompt, approval, progress and result state.
- Browser errors do not discard the active task.
- The browser panel can be collapsed/reopened without losing the session or workflow context.

### UWF-03 — Template list is not execution history

**Finding:** the workflow screen loads saved `BROWSER_WORKFLOW` entities as reusable definitions. Execution state and outputs are stored through other records (jobs, conversations, evidence and response entities). The inspected workflow surface does not provide a joined run-history view.

**User impact:** users can see reusable templates but cannot easily inspect what they ran previously, which provider received which prompt, what was approved, what response was captured, or where a run failed.

**Expected behavior:** a run history is distinct from the template library and links each execution to its template (if any), prompt revision, provider/runtime, approval event, timestamps, status, captured output and evidence.

**Acceptance:**
- Template library and execution history are separately labelled.
- A run can be reopened with its original prompt, provider, approval and output.
- Failed, cancelled, waiting-for-user and completed runs remain inspectable.
- History does not imply success when the response was not captured or verified.

### UWF-04 — Browser takes over the screen and hides task controls

**Finding:** the browser workflow has a full-browser route/action separate from the workflow screen. This can move the user away from the task-specific controls.

**User impact:** the browser can cover the whole experience; prompt, workflow state, template context and recovery actions are no longer in the same view.

**Expected behavior:** browser interaction must be contextual. A full-screen browser may be an optional viewing mode, but it must preserve an obvious return path and persistent task status/control affordances.

**Acceptance:**
- User can inspect the page and return to the workflow without losing state.
- Active task status and Stop/Take over controls remain reachable.
- No navigation path silently abandons an approval or pending capture.

### UWF-05 — Prompt enhancement lacks a complete editable checkpoint

**Finding:** the source has prompt preparation and an exact provider-specific approval dialog. The inspected path does not provide a complete prompt-enhancement editor with a clear original/enhanced comparison, revision history and regenerate/edit/accept controls before approval.

**User impact:** the user is asked to approve sending, but does not have sufficiently clear control over how Memo transformed their original request.

**Expected behavior:** before any external send, show:
- original prompt, preserved unchanged;
- enhanced prompt, fully editable;
- concise explanation of material changes or assumptions;
- regenerate/rewrite option;
- accept, edit, or reject choices;
- exact final provider, destination and payload bound to approval.

**Acceptance:**
- Editing the enhanced prompt changes the payload hash and invalidates any prior approval.
- Original text remains recoverable.
- No external send occurs before explicit approval of the final exact payload and destination.
- Rejection returns to editing rather than cancelling the entire task.

### UWF-06 — Automatic response capture is brittle and has no adequate manual fallback

**Finding:** browser response capture depends on site-specific DOM detection, selectors and polling. The capture path waits for a detected new/stable assistant response and then persists it into conversation/evidence/response records. If detection does not identify a stable answer, the workflow can reach a waiting-for-user state without capturing the answer. The inspected UI does not expose a clear manual **Capture response** action.

**User impact:** a provider can visibly answer while Memo records no usable result. The user is left with a running or stalled workflow and cannot simply select/capture the visible answer to continue.

**Expected behavior:** automatic capture is best-effort assistance, not a single point of failure. A user must always be able to:
- capture the visible answer manually;
- select the relevant answer region or paste the response;
- edit the captured text before saving;
- save it as a durable result linked to the run;
- retry detection, skip the provider, or continue with already captured results.

**Acceptance:**
- If automatic detection times out or fails, the browser remains available and a manual capture path is offered.
- Manual capture stores the text, provider/session, source run, timestamp and capture method.
- The user can review/edit before committing it as a result.
- A capture failure never silently discards the provider's visible answer.
- A response is not labelled complete merely because the Send action was clicked.
- A failed detector cannot force the user to cancel the entire task to preserve useful work.

### UWF-07 — No meaningful step-level recovery and continuation controls

**Finding:** the reported current UI offers little after a browser step becomes stuck beyond cancelling the workflow or saving. The inspected waiting-for-user path is oriented around polling/continuing detection or stopping, rather than a full user-controlled recovery surface.

**User impact:** the user cannot recover a partially successful multi-provider task. One provider's capture failure can block the whole operation.

**Expected behavior:** expose explicit, state-aware actions for the current step:
- capture answer manually;
- retry detection without resending;
- resend only after clear duplicate-risk warning and fresh approval;
- skip this provider;
- edit prompt and retry this participant;
- continue with captured results;
- pause/take over browser;
- stop the current step;
- cancel the entire run as a separate, clearly destructive scope.

**Acceptance:**
- Step-level actions do not cancel unrelated completed steps.
- Retrying capture never resends the prompt.
- Resending a prompt is a distinct action, shows duplicate/submission uncertainty, and requires fresh approval.
- Captured outputs survive stopping or failing later steps.
- UI labels distinguish Stop step, Pause, Skip and Cancel run.

### UWF-08 — Results are not a usable workspace

**Finding:** response persistence exists in the source callback, but the end-to-end user journey does not provide a reliable result surface with comparison, editing and follow-up controls.

**User impact:** even a successfully captured response is not necessarily something the user can work with naturally. The product experience ends too close to "send and save".

**Expected behavior:** captured answers should become first-class editable results attached to the active task/conversation. The user can compare participants, quote or combine results, ask follow-ups, export/copy, and continue the task without starting over.

**Acceptance:**
- Each participant result has its own editable view and provenance.
- User can compare results side by side without losing originals.
- User can ask a follow-up against one result or selected results.
- Edits create revisions; they do not overwrite raw captured evidence.
- Final synthesis is a separate artifact and never silently replaces participant answers.

## 4. User-control inventory for this journey

| Control | Required behavior | Current source/UI assessment |
|---|---|---|
| Edit original prompt | Always preserve and allow editing | Original intent exists; full edit/revision checkpoint not established |
| Edit enhanced prompt | Edit before approval; approval invalidates on change | Complete editor/revision flow not found in inspected path |
| Regenerate enhancement | Offer alternatives without sending | Not established in inspected path |
| Choose participant runtime | Local/API/browser clearly distinguished | Council does not automatically include browser sessions |
| Approve exact send | Bind approval to exact content and destination | Exact-content approval path exists |
| Reject/edit | Return to prompt editing | Current rejection path is not a complete edit loop |
| Keep task context | Browser must not replace active task | Separate browser route/full-browser action exists |
| See template library | Reuse saved definitions | Exists |
| See run history | Review every attempt and outcome | Not joined into workflow surface |
| Capture response manually | Save visible/pasted answer without resend | No clear manual capture control found |
| Retry capture | Re-run detector only | Detection retry exists as a limited waiting-state action |
| Retry send | Separate from capture retry; require new approval | Not established as a safe, explicit recovery path |
| Skip provider/step | Preserve other results and continue | Not established in inspected path |
| Continue with partial results | Keep completed work and proceed | Not established as a complete user path |
| Edit captured result | Create revision while preserving raw evidence | Not established in browser result surface |
| Compare/follow up | Work across provider results | Not established in the inspected workflow surface |
| Cancel whole run | Explicitly cancel all remaining work | Stop/cancel exists, but scope must be made unambiguous |

"Not established" means the inspected path did not demonstrate the complete user-facing behavior; it is not proof that no lower-level helper or alternate screen exists.

## 5. Stabilization priorities

These are acceptance requirements for the already-present prompt → approval → browser → result journey, not permission to broaden product scope.

### P0 — recover the user's result and preserve task context
1. Reproduce the reported send-without-capture failure on a real device/provider.
2. Ensure the workflow never treats clicking Send as a completed response.
3. Add/restore a dependable manual capture/review path when DOM detection fails.
4. Keep browser, task, prompt and recovery controls in one persistent context.
5. Ensure capture retry cannot accidentally resend a prompt.
6. Preserve already captured results when a later participant or step fails.

### P1 — make the existing journey controllable
1. Provide a proper editable enhancement checkpoint before approval.
2. Separate template definitions from execution history and link them.
3. Add step-level skip/retry/pause/takeover/continue controls with clear semantics.
4. Present captured results as editable, provenance-preserving task outputs.
5. Make Council runtime availability truthful and explain why a browser provider is or is not selectable.

### P2 — verify coherence and usability
1. Test multi-provider runs with one provider succeeding and another timing out.
2. Test browser navigation, page reload, provider UI updates and response selector drift.
3. Test app backgrounding/process recreation while approval or capture is pending.
4. Test accessibility, small screens, keyboard/IME and long prompts/results.
5. Test English, Polish and Turkish labels and error/recovery messages.

## 6. Required end-to-end acceptance scenarios

| Scenario | Action | Required outcome |
|---|---|---|
| Happy path | Enter prompt → edit enhancement → approve → send → provider answers | Answer captured, reviewed and available in task results |
| Detector failure | Provider answers but DOM detector times out | Manual capture offered; user saves answer without resending |
| Delayed answer | Provider is still generating at initial timeout | User can wait, take over or capture later; no false completion |
| Duplicate-send risk | Send may have succeeded but confirmation is missing | Memo reports uncertain state; does not auto-resend |
| Prompt correction | User edits enhanced prompt after preview | Previous approval invalidated; new exact payload requires approval |
| Multi-provider partial success | One provider returns; another fails | First answer preserved; failed provider can be skipped/retried independently |
| Browser takeover | User needs to interact with provider page | Browser interaction does not abandon task state or controls |
| App interruption | Android kills/recreates Memo during browser work | Run reopens with truthful state; no blind duplicate send |
| History review | User opens a previous run | Original prompt, revisions, participants, approvals, outputs and failures are visible |
| Result editing | User edits a captured answer | New revision saved; raw captured response remains available |

## 7. Evidence boundaries and unknowns

- The architecture and code paths described above were inspected on the stated branch baseline.
- The user's report that the provider answer is not captured and that only cancel/save are available is a reported real-world symptom; a fresh device reproduction and logs are still required.
- The source contains an automatic response persistence callback. This does not prove that provider output is reliably detected or that the user can recover when detection fails.
- The source contains exact-content approval. This does not prove the complete prompt-editing experience is usable.
- The inspected browser workflow has separate route/state from Council. This does not imply browser-backed Council participation is impossible in principle; it is not integrated in the current inspected path.
- This audit did not establish a new app crash. Crashes must be logged separately with reproduction steps, Android version, device, stack trace/logcat and exact source SHA.
- No runtime acceptance, emulator execution or physical-device verification is claimed by this document.

## 8. Definition of done

This journey is not accepted until a user can, without losing context:

1. enter and edit a prompt;
2. inspect, edit or regenerate its enhancement;
3. choose a clearly identified participant/runtime;
4. approve the exact final content and destination;
5. watch and control execution without leaving the task;
6. recover the answer even if automatic browser detection fails;
7. edit, compare and continue with captured results;
8. inspect the full run history later;
9. stop one step or the entire run deliberately, without losing already captured work.

The critical invariant is:

**Once an external provider has produced an answer, Memo must provide a user-controlled way to capture and preserve it, independent of whether automatic DOM detection succeeds.**

Until the end-to-end scenarios pass on a physical Android device, mark this journey **OPEN / NOT ACCEPTED**.

## 9. Additional user reproduction feedback — 19 November 2026

New reports (INPUT-2026-11-19-004 through -006) extend, rather than replace, the original acceptance matrix:

- The current browser workflow is Frontier-provider-specific. General browser automation must resolve user-intended permitted websites/targets; Frontier providers remain optional presets.
- The user selected Frontier providers and reports that no browser opened despite accounts already being logged in. The source has a navigation call, so the precise failure must be reproduced by tracing the actual entry route, local-model stage, WebView callbacks/errors and Memo's own browser session. Do not assume external Chrome login state is shared with the embedded WebView.
- The approval action is not discoverable. Source inspection confirms "Approve and send" lives in a conditional dialog, while the Action Approval Inbox does not expose approval itself. The approval inbox and in-context control must converge on one actionable record.
- Checkpoints and approvals are experienced as repetitive/confusing. Read-only observation, navigation, local AI and planning must not be treated as external-action authorization. Distinguish progress, input-needed, result-review, recovery/uncertainty and explicit authorization.

Add BRW-14/15, APR-01 and CHK-01 to the existing WF-01–WF-13 acceptance run. Do not close earlier items or weaken exact-content/one-use approval to reduce friction.
