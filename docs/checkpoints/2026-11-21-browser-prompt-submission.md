# Talk browser prompt submission checkpoint — 21 November 2026

## User outcome
Talk can send a prompt to the visible browser page only after the user approves the exact HTTPS destination and full prompt. The visible submission is verified, stored as encrypted evidence, and bound to the durable action ledger. When the user separately enables browser-page sharing, Talk can wait for a new stable response and capture it as untrusted source material.

## Implementation
- browser.submit_prompt is an EXTERNAL_WRITE capability with explicit one-use approval.
- The action grant binds runtime, capability, normalized HTTPS destination and exact prompt payload. The adapter rechecks the grant hash and current page before invoking the visible composer.
- BrowserSession.submitPromptForAgent reuses the existing visible-composer prepare/send/verify scripts. It verifies the exact prompt appears as a new submitted message, the composer is cleared, and the page remains same-origin.
- The adapter persists BROWSER_PROMPT_SUBMISSION evidence with exact prompt, prompt/page digests, source conversation/message provenance, action ID/hash and run/call IDs.
- The adapter postcondition verifies the evidence and in-progress action ledger, then completes the ledger against the exact evidence ID. Any uncertain result, cancellation or persistence mismatch becomes NEEDS_REVIEW; the prompt is never automatically replayed.
- browser.capture_response is added only when browser-page sharing consent is enabled. It requires the completed submission evidence ID, verifies the original baseline, waits for a new stable response using the shared bounded provider-selector script, stores BROWSER_PROMPT_RESPONSE evidence and labels the returned text untrusted.
- Studio exposes both prompt and response records in a separate Browser research evidence section.

## Tests and guards
- Runtime registry requires explicit approval and external-side-effect classification for browser.submit_prompt; added direct regression coverage.
- Added a test that browser.capture_response is not an external side effect and does not require external-action approval.
- Source verifier checks adapter counts, exact approval/hash/action-ledger lifecycle, visible composer submission, response baseline, consent gating, idempotency and Studio evidence visibility.
- Physical-device checklist covers approve/reject, stale page, interrupted send, uncertain outcome, consent on/off and stable response capture.

## Verification
- Prompt-submission exact-head CI: PASS, run 44954196829, commit 23073011b9c411945c12531ce8483ac0a6d2b4b5. Source verification, remote runner, core/app unit tests, lint, APK assembly, identity/signature and emulator install-over-existing data preservation all passed.
- Response-capable latest head: exact-head CI pending.
- Physical-device acceptance: PENDING.
- Release-signing job skipped; no canonical release APK claimed.

## Scope boundary
General browser controls, form filling, file uploads, arbitrary page-event subscriptions, native Android actions and universal cross-runtime action support remain open.