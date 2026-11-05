# Memo roadmap — product rebaseline

**Product:** Memo for Android  
**Branch:** `ysfclc-patch-1` (never modify `main`)  
**Product contract:** [docs/MEMO_PRODUCT_CONTRACT.md](docs/MEMO_PRODUCT_CONTRACT.md)  
**Execution ledger:** [TASKS.md](TASKS.md)

## Product promise

Memo is a personal AI operating agent. One intent entry point connects Talk, Live, Work and Studio. It plans and executes bounded workflows across the user's encrypted workspace, compatible local models, explicitly permitted frontier AI websites/apps, browser sessions and Android runtimes. It creates useful deliverables, records provenance and keeps consequential actions under the user's control.

This is not a browser-first app, chatbot skin, static planner or disconnected dashboard.

## Verified source/build baseline — 5 November 2026

- Source snapshot: `382c65c1bf6dbc37d87fd766bf4796a2e76a400d`
- Integrated branch head after CI handoff: `6d0e20dca47350d7b53ce8b82dca866bac1f45a9`
- GitHub Actions run #93: [success](https://github.com/ysfclc/memo/actions/runs/42048273575)
- `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, source invariants, remote-runner tests and APK package/signature identity gates passed.
- APK identity: `app.memo.android.debug`; CI verifies the configured persistent signing certificate and increasing version code.
- Artifact generation succeeded. This is CI evidence only. Install-over-existing, user-data preservation, audio, WebView sign-in, Accessibility and real workflow behavior have **not** been accepted on a physical device.

## Product reality check

The current app still exposes Home / Chats / Work / Browser / More, splits the main entry into “Plan & start” versus “Ask in chat,” uses keyword-selected static plan templates, keeps browser and app automation in specialist areas, and has no universal cross-runtime DAG or single Live timeline. Studio is currently mostly Self Model fact extraction and local artifact generation. See the full source-to-vision matrix in the product contract.

## Ordered delivery plan

### P0 — Protect user data and installability
- [x] Repeatable branch-only GitHub Actions build pipeline.
- [x] Persistent APK signing identity and fail-closed certificate check.
- [ ] Physical-device install-over-existing test, including confirmation that existing encrypted workspace data survives.
- [ ] Migration design and tests to unify primary encrypted workspace and legacy native workflow/run store without data loss.
- [ ] APK size/native dependency budget and device resource baseline.

### P1 — Restore the intended product shell
- [ ] Replace Home / Chats / Work / Browser / More as primary navigation with **Talk / Live / Work / Studio**.
- [ ] Put one universal composer/intent entry point at the centre; do not ask users to choose chat versus plan.
- [ ] Move browser and native-app sessions into Live as task-bound runtime panes; keep their identity/session controls visible.
- [ ] Integrate Council into Talk conversations, with visible sequential frontier participants, role/turn history, disagreements and synthesis.
- [ ] Give Studio its own artifact workspace; Self Model is a source of approved facts inside Studio, not the whole Studio.
- [ ] Keep model, capability, permission and diagnostics management in supporting settings, not primary navigation.

### P2 — Implement the real orchestrator
- [ ] Typed TaskSpec and versioned workflow DAG; no keyword-only planning as the execution brain.
- [ ] Dynamic capability assembly from a versioned registry; inspectable manifest, gaps, trust, runtime readiness and budgets.
- [ ] Unified runtime routing for deterministic tools, local model, frontier website/app sessions, browser, native app and human checkpoints.
- [ ] Shared durable run state/event journal with pause, resume, cancellation, process-death recovery and idempotency.
- [ ] One content/destination/scope-bound approval policy across every external/destructive runtime.
- [ ] Postcondition and evidence protocol; no model-asserted completion.

### P3 — Make Talk intelligent and useful
- [ ] Continuous multi-chat UX with a single multimodal composer, selected context and visible context manifest.
- [ ] Local model coordination with true streaming, cancellation and truthful hardware/runtime capability diagnostics.
- [ ] Frontier Council as a selectable workflow, not a separate toy screen or hidden parallelism.
- [ ] Voice input/output and fast capture from share sheet, notification and launcher entry points.
- [ ] GGUF/llama.cpp backend and broader compatible model discovery after runtime interfaces are stable.

### P4 — Make Live the control room
- [ ] Unified chronological event stream across all workflow steps and runtimes.
- [ ] Visible browser/app view attached to the active workflow; inspect, pause, take over and return safely.
- [ ] Step state, current actor, target, payload preview, evidence, postcondition and timestamps.
- [ ] Global Stop, review of uncertain actions, resumable checkpoints and truthful failure states.
- [ ] DOM/semantic element picker, versioned surface schemas, recorder and replay fixtures.

### P5 — Make Work the user's operating graph
- [ ] Canonical versioned records for projects, tasks, contacts, people, organizations, opportunities, jobs, applications, decisions, sources, evidence and relationships.
- [ ] Full create/read/update/archive/delete with provenance, revisions, undo and dependency-aware destructive previews.
- [ ] End-to-end CRM capture and sourced enrichment.
- [ ] End-to-end job discovery, ranking, factual tailoring, approval-backed apply and verified application tracking.
- [ ] Search, deduplication, safe merge and graph navigation.

### P6 — Make Studio produce deliverables
- [ ] General artifact types, editor, autosave, revision history, diff/restore and export.
- [ ] Self Model ingestion with exact-source fact proposals and human approval.
- [ ] CV, cover letter, job-tailored application and research/document outputs.
- [ ] Content drafts and media pipeline with private-by-default workflow.
- [ ] Channel publishing preview, exact approval, verified receipt and metrics ingestion.

### P7 — Voice and everyday use
- [ ] Reliable foreground voice session with interruption/barge-in and local ASR/TTS.
- [ ] Android-compliant lock-screen/notification/Quick Settings/launcher entry; honest degraded behavior when locked.
- [ ] English, Polish and Turkish voice and intent fixtures.
- [ ] Today view, reminders, briefings and driving mode backed by actual graph/run data.

### P8 — Release hardening
- [ ] Physical-device acceptance for every Android-dependent critical path.
- [ ] Security/privacy review, redacted diagnostics, permissions centre and recovery drills.
- [ ] Accessibility, reduced motion, performance and battery/resource budgets.
- [ ] Final feature-to-evidence ledger; no stale completion marks or production claims without proof.

## Delivery rule

A task closes only with a real implementation, named tests, green CI and device/security evidence where applicable. Documentation is the contract and ledger—not a substitute for the product.
