# Product gap registry — prioritized, with acceptance criteria

Memo should feel like a capable, safe execution partner, not a collection of buttons. These are product gaps inferred by comparing every supplied build with the intended end state; the user does not need to discover each gap one by one.

Priority meanings: **P0** release blockers; **P1** core product capability; **P2** expansion that depends on stable core execution.

## P0 — make the current build trustworthy

- [ ] **Android CI is green.** The exact source ZIP builds with Android unit tests and lint; APK is present, hashed and downloadable as the workflow artifact.
- [ ] **Real-device acceptance.** First-run/unlock, keyboard resize, share-in, local inference, remote-consent rejection, notifications and Accessibility pause/stop behaviors are exercised on at least one Android device.
- [ ] **Data-store migration strategy.** Document which encrypted vault owns each data class; add schema version, backup-before-migration and rollback; eventually converge on one transaction/event boundary without discarding older data.
- [~] **Reliable action ledger — partial source slice.** The new core ledger persists browser prompt proposals/approval/start/completion/review status, binds exact runtime/capability/destination/payload, hash-chains audit events, requires a saved evidence reference, and invalidates approvals on import/restart. A central queue/review/history inbox is now present for this ledger and exposes exact content and recent audit events. Still missing: universal adoption across native/guest/provider actions, timeout/error taxonomies, integrity-authenticated export, and a product-wide emergency stop. Gradle and device verification are pending.
- [~] **External side-effect safety — browser path implemented, product-wide partial.** Provider prompts now use a full exact-content preview, hash-bound one-use approval and pre-send destination/payload recheck. Unknown outcomes are routed to review. Universal receipt/idempotency support and cross-runtime enforcement remain open; build/device verification is pending.
- [~] **Encrypted backup — first integrated flow added.** Passphrase-derived AES-256-GCM export/import, authenticated header, version and iteration checks, bounded payload, tamper/wrong-password rejection, encrypted export UI and explicit restore confirmation are in source. Still missing: restore preview, backup-before-migration/rollback orchestration, device verification, and tested long-term recovery UX. Plain JSON remains clearly marked readable.
- [ ] **Permission & privacy audit.** Explain all permissions at point of use; never read screen content/clipboard/microphone silently; keep lock-screen notification bodies generic.

## P1 — cohesive day-to-day product

- [ ] **Unified workflow graph.** DAG with named steps, dependencies, typed inputs/outputs, retries/timeouts, checkpoints, cancellation, conditional branches, and durable state. A workflow can combine local AI → visible frontier website → native app → review → measurement.
- [~] **Capability compilation — deterministic first slice added.** A pure-core compiler selects known capability IDs for each intent domain, preserves runtime state and approval requirements, lists unresolved capabilities, and records a deterministic hashed manifest in draft-plan metadata. Still missing: editable/versioned capability, prompt, agent, tool and surface-schema registry; manifests pinned to executed runtime revisions; integration across every executor.
- [ ] **Generic browser surface engine.** Per-site versioned DOM/semantic selectors, state signatures, page-change alert, action budget, evidence screenshot/text, postconditions and replay fixtures. Provider scripts remain adapters, not the whole automation model.
- [ ] **Workflow gallery/editor.** Searchable reusable blueprints, inspect-before-run plan, permission summary, inputs, generated artifacts, per-step log, recovery/redo and revision diff; no buttons that pretend to work without a connected executor.
- [ ] **Reminders and task scheduling.** Local task due-date reminders, snooze/complete actions, quiet hours, daily nudge, missed/overdue review, timezone/reboot handling. Current daily nudge is only the first layer.
- [ ] **Voice entry.** Keep tap-to-talk with editable transcript; add a one-tap notification/launcher/Quick Settings entry into voice capture and a spoken-confirmation step before risky actions.
- [ ] **Optional wake word, only after a proper privacy design.** User opt-in, visible foreground service/notification, on-device wake-word model, mic indicator, hardware/battery controls, immediate off switch and documented OS limitations. Do not promise a magic always-on assistant across locked devices; Android restricts background microphone service starts.
- [ ] **Model discovery and install.** One-tap catalogue for supported formats with exact compatibility, hash, size, storage/RAM checks, download resume/cancel/rollback and clear statement that installed third-party app models may not expose APIs Memo can call.
- [ ] **Pocket-style source handling.** `ACTION_SEND_MULTIPLE`/URI imports, file size/type limits, review inbox, immutable original capture, derived working copy, source hash/URL/timestamp, per-mission/context selection and versioned backup.
- [ ] **Capture that produces traceable records.** Quick text, share sheet, voice, image/screenshot and selected text; every capture has original source, extraction results, provenance and a review step before it changes the graph.
- [ ] **Private draft → public-safe studio.** Drafts, claims/evidence check, media/carousel storyboard, generated asset provenance, channel-specific preview, destination-bound human approval, post receipts and later engagement measurements.

## P2 — the powerful “arms and hands” expansion

- [ ] **CRM and relationship flows.** From voice or text, create person/org/meeting, link entities, deduplicate, gather evidence-backed enrichment and queue follow-up tasks. Proposed mutations remain reviewable.
- [ ] **Job discovery/application workflow.** Search configured sources, create job entities, explain match criteria, deduplicate and approve a shortlist, then fill applications with a complete preview and explicit submit approval. Never claim an application was submitted without receipt/evidence.
- [ ] **Content performance loop.** After user-approved publication, collect permitted channel metrics, link data to the exact post/version, compare outcomes, preserve evidence and use measurements to refine future drafts.
- [ ] **Multimodal source pipeline.** Text, PDF, documents, images, OCR, screenshots, video frames, audio transcripts and generated assets, with file limits and local/cloud privacy routing made explicit.
- [ ] **Measurement and optimization loop.** Every goal defines observable success metrics, data source, sampling schedule, limits and review interval. Optimizations remain proposals until authorized.
- [ ] **Cross-device execution and secure pairing.** Mutually authenticated pairing, device scopes, expiring capabilities, revocation, signed run manifests, heartbeat, audit and remote cancellation. Do not launch a hosted service without explicit deployment/consent.
- [ ] **Accessibility and power-user controls.** VoiceOver/TalkBack semantics, keyboard support, scalable text, offline states, reduce-motion preference, permission diagnostics, battery/thermal notice and visible system state.

## Notification policy

- Current implementation: local Android notifications and daily reminder only, user-controlled, generic/private body. This requires notification permission on Android 13+. No push server exists in this build.
- Optional future remote push: only when paired remote execution or a hosted service is actually deployed. Use a minimal opaque event ID; fetch details after the device is unlocked; no private prompt or model output in push payloads; opt-in, revoke, delete-token and auditable delivery.

## Voice policy

- Current implementation: user taps mic, platform recognizer fills an editable chat draft; it is not guaranteed offline and does not run continuously.
- Future wake-word: implement only if on-device detection, OS-compliant foreground operation, visible mic state, configurable sensitivity and true battery/privacy tests are available. Android 12+ background foreground-service starts are restricted; newer versions also constrain microphone while-in-use access. Tap-to-talk is the reliable baseline.


## Additional product features worth planning now

These additions are derived from the combined source inventory and the design blueprint. They are roadmap items, not claims about the current implementation.

- [~] **Approval inbox and action queue — first slice added.** One screen now shows the browser-action ledger queue, needs-review records and history, including exact content, destination, payload hash, runtime, capability, evidence and recent audit events. It rejects a pending proposal and can mark an active/approved outcome for review. It only opens the live browser dialog for an exact matching active prompt; it cannot approve history. Still missing: native/guest/publication actions in the same queue, richer risk/permission summaries, global pause/stop and device validation.
- [~] **Global pause / emergency stop — first top-level stop control added.** Header stop requests cancellation of local model generation, browser workflow/task and active AccessibilityService execution; pending proposals are rejected and approved/in-flight external actions move to `NEEDS_REVIEW`. The optional guest-runner cancel is best-effort and reports uncertainty. Still missing: a centralized cancellation token in a durable cross-runtime DAG, cancellation tests for each runtime, background/process-death delivery, and device verification.
- [ ] **Notification deep links and safe actions.** Notifications should open the exact run/review screen and can later offer Pause, Review, Snooze or Mark done where semantics are safe. Keep private content out of notification text; re-authenticate before showing protected details.
- [ ] **Quick capture entry points.** Add launcher shortcuts, Android Quick Settings Tile and a notification action for explicit voice/text capture. These are better first steps than always-listening wake detection and work within normal user-triggered permission boundaries.
- [ ] **Privacy profiles and context allowlists.** Define per-workflow/per-project what can leave the device (none, selected excerpts, selected files, explicit full handoff), and make the payload visible before transmission. Remote-provider consent should not be remembered globally by accident.
- [ ] **Model readiness diagnostics.** Before installing or running a local model, check format compatibility, free storage, approximate memory requirements, accelerator availability, checksum, license/source, warm-up result, cancellation and rollback. A downloaded model is not a working model.
- [ ] **Record integrity tools.** Search duplicate candidates, preview reversible merge/relink actions, preserve the archived source record and provenance, and let the user undo accidental links. Add export/restore fixture tests before changing the schema.
- [ ] **Workflow test fixtures.** Allow developers/power users to replay captured, sanitized page/app states against a versioned schema without performing external side effects. Fixture success must be distinguished from a live-device success.
- [ ] **Offline queue with explicit routing.** Save work while offline, show which steps can be completed locally, and ask before remote providers are used when connectivity returns. Never silently substitute remote inference for a failed local model.
- [ ] **Goal measurement contract.** For each measurable workflow, define the metric, source, sample period, threshold and stopping rule. Preserve the observed numbers and their source; present optimizations as proposals until accepted.
