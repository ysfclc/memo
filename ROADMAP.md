# Memo roadmap

**Product:** Memo Android — one personal execution workspace, not a chatbot skin.  
**Working branch:** `ysfclc-patch-1`. `main` is not part of this workflow.

## Product promise

Capture intent by text, voice or share sheet; turn it into editable plans and user-owned records; connect local models, approved provider sessions, browser capabilities and phone runtimes; keep evidence and outcomes in one encrypted workspace. No external side effect without explicit, content-bound approval.

## Current verified baseline

The 5 November 2026 GitHub Actions run #76 completed successfully on the prior uploaded source snapshot:
- source invariant verifier and remote-runner tests passed;
- `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug` and `:app:assembleDebug` passed;
- APK package/version/signature verification passed;
- APK was signed with the configured persistent upload key.

This is CI evidence only. It is not a real-device acceptance claim. The new model-catalogue changes in the current branch require a fresh run before they are considered integrated.

## Milestones

### P0 — Reliable foundation
- [x] Repeatable source-import/build pipeline with source checksum manifest.
- [x] Persistent debug signing and package identity verification in CI.
- [ ] Verify install-over-existing-app and preserved user data on a physical Android device.
- [ ] Add APK size budget and dependency/native-library report; investigate oversized artifacts.

### P1 — One coherent, user-owned workspace
- [ ] Consolidate Home, Chats, Work, Browser and More into one consistent navigation and visual system.
- [x] Add edit/delete controls for user and assistant chat messages; imported/system messages stay read-only.
- [x] Protect messages cited by saved entities from edits/deletion so source provenance cannot silently change.
- [x] Add conversation deletion with dependency checks for linked entities, jobs and relationships; block orphaning rather than cascade-delete.
- [ ] Complete CRUD for entities, relationships, tasks, plans, evidence and provider profiles.
- [ ] Add undo/recovery and richer impact previews for destructive actions; no silent cascade deletion.
- [ ] Make every visible action real, stateful, accessible and covered by UI/domain tests.
- [ ] Add revision history and compare/restore for user-edited artifacts.
- [ ] Add encrypted backup/restore validation and a clear storage/retention panel.

### P2 — Model and provider platform
- [x] Expand the curated LiteRT-LM catalogue to three distinct size/capability tiers.
- [x] Pin catalogue revisions and SHA-256 hashes; reject corrupted or substituted downloads.
- [ ] Verify catalogue hashes against upstream publisher metadata and exercise actual downloads.
- [ ] Add model import/discovery, runtime compatibility diagnostics and per-device benchmarks.
- [ ] Add GGUF/llama.cpp as a separate backend behind the common model-runtime interface.
- [ ] Support multiple encrypted OpenAI-compatible provider profiles with explicit per-task selection.

### P3 — Execution and automation
- [ ] General versioned workflow graph with pause/resume/retry and durable checkpoints.
- [ ] Shared approval ledger across browser, remote runner and native app actions.
- [ ] Live execution timeline with pause, cancel, takeover and verified postconditions.
- [ ] Provider-specific browser sessions and robust authentication/session recovery.

### P4 — Voice, capture and daily use
- [ ] Reliable foreground voice capture and local ASR/TTS lifecycle.
- [ ] Fast capture from lock screen, share sheet and notification entry points.
- [ ] Today view, reminders and briefings backed by real workspace data.

## Delivery rule

A task moves to done only with source changes, automated test evidence, successful CI, and device evidence where the behavior depends on Android hardware, lifecycle, accessibility, audio or installation. A screen or button alone is not completion.
