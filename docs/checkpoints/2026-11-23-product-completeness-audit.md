# Memo product-completeness audit — 23 November 2026

## Session identity

- Date/time/timezone: 2026-11-23, Europe/Warsaw
- Repository: `ysfclc/memo`
- Audit branch: `audit/product-completeness-2026-11-23`
- Base branch / head at start: `main` at `ca26752755063b3503a360881155dc186a7c7041`
- Product/requirement IDs: MEMO-CORE-001, MEMO-TALK-001, MEMO-LIVE-001, MEMO-WORK-001, MEMO-STUDIO-001, MEMO-ORCH-001, MEMO-AI-001, MEMO-CAPTURE-001, MEMO-SECURITY-001, MEMO-RELEASE-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-ARTIFACT-001

## User outcome

The user still does not have a Memo installation they regard as working. This audit prioritizes actual product completeness and the path to one useful, verifiable end-to-end outcome—not feature count, architecture documents, attractive screens or green compilation.

The product contract remains: one outcome-first Talk entry point, one shared provider-agnostic execution lifecycle, real callable capabilities, observable results, durable state, exact approvals and verified postconditions. No per-example workflow or parallel execution engine is acceptable.

## Baseline inspected

- Required governance: `AGENTS.md`, `docs/AI_EXECUTION_INVARIANTS.md`, `docs/MEMO_PRODUCT_CONTRACT.md`, `DEVELOPMENT_GOVERNANCE.md`, `IMPLEMENTATION_CONTRACT.md`, `docs/REQUIREMENT_TRACEABILITY.md`, `TASKS.md`, `docs/DECISION_LOG.md`, `docs/SESSION_CHECKPOINT_TEMPLATE.md`.
- Latest prior checkpoint: `docs/checkpoints/2026-11-22-release-baseline-audit.md`.
- Current main: `ca26752755063b3503a360881155dc186a7c7041`; source handoff parent: `9e16d18801e98049474dc9cbc282a1f52cad6a6f`.
- Mainline source inspected: `AgentExecutionEngine.kt`, `AgentCapabilityRegistry.kt`, `MemoWorkspaceAgentTools.kt`, `AiRuntimeViewModel.kt`, `MemoWorkspaceViewModel.kt`, `WorkspaceScreens.kt`, `BrowserSession.kt`, `app/build.gradle`, and related tests.
- Mainline product records inspected: `FEATURE_AUDIT.md`, `PRODUCT_GAP_REGISTRY.md`, `BUILD_STATUS.md`, `TASKS.md`, `DEVICE_ACCEPTANCE_CHECKLIST.md`, `IMPLEMENTATION_LOG.md`.
- Current-main CI: Actions run [45060946098](https://github.com/ysfclc/memo/actions/runs/45060946098), source head `9e16d18801e98049474dc9cbc282a1f52cad6a6f), completed successfully. Build, isolated signing and source-handoff jobs passed; unit tests, lint, APK assembly, APK identity/signature and emulator install-over-existing checks passed.
- Product integration candidate: draft PR [#20](https://github.com/ysfclc/memo/pull/20), branch `build/product-core-integration-2026-11-21`, head `86c52b4e07f530b5ad0b727651b8ce7a75bd2918`, based on `d80f19ae6ff956392cbe11c2b1aaead1758a7d19). Exact-head Actions run [45020438646](https://github.com/ysfclc/memo/actions/runs/45020438646) completed successfully.
- The integration branch comparison against current main reports 188 commits ahead and 2 behind, with 40 changed files. It is not a clean, current-main integration and must not be merged blindly.
- `memo-2` was inspected only to establish repository intent. `IMPLEMENTATION_CONTRACT.md` and `AGENTS.md` explicitly designate it as blueprint/reference only. No changes are permitted there.

## Work completed

This was a source, branch, product-scope and CI audit only. No application source, schema, key, signing configuration, data path or release workflow was changed.

### What is actually present on current main

1. Talk is connected to a bounded, model-directed `AgentExecutionEngine`; this is more than a static keyword planner.
2. Runtime ownership and approval policy are checked by `AgentCapabilityRegistry`.
3. The mainline Talk adapter set is limited to workspace search, private note saving, proposed-record creation, browser navigation, browser page capture and opening a link observed in the captured page.
4. Talk execution events and run summaries are written into the encrypted workspace; interrupted runs are moved to `NEEDS_REVIEW`, not replayed.
5. Browser work is constrained to a shared visible WebView and user-supplied HTTPS navigation/read/capture; current main does not provide general form automation or arbitrary website interaction through Talk.
6. Existing specialist app automation/workflow surfaces are not yet a universal native-app capability behind the same Talk dispatcher.
7. Work/Studio have useful partial data and artifact foundations; they do not yet provide the complete outcome-to-deliverable lifecycle.
8. Local/remote model and voice features exist as partial integrations, but device/runtime acceptance is not established by source presence.

### What the open product integration branch already adds

PR #20 contains substantial implementation absent from current main, including:

- adapter-level postcondition verification enforced by the shared Talk engine;
- approved browser prompt submission bound to exact destination and prompt, with a one-use action ledger;
- consent-gated browser response capture with encrypted evidence;
- Talk-created private Studio artifacts with source provenance and read-back verification;
- user-directed Markdown export with exact URI read-back and digest verification;
- improved local model download lifecycle and recovery;
- additional Talk/Live execution visibility and regression tests.

Its exact-head CI run passed. This is meaningful implementation evidence, but not physical-device acceptance or product completion. PR #20 remains a draft and is not merged. Its PR CI intentionally does not publish the pinned-certificate release APK.

### Release-delivery blocker

- `app/build.gradle` sets the normal application ID to `app.memo.android`, then adds `.debug` to the debug variant.
- The current-main workflow builds `assembleDebug` and signs that debug artifact in an isolated job.
- The resulting package identity is `app.memo.android.debug`, not `app.memo.android`.
- Therefore the current signed debug APK is not an in-place update of a normal `app.memo.android` installation. The emulator install-over-existing test proves preservation for the tested debug package; it does not prove compatibility with the user's normal package.
- A release-variant APK and exact package/certificate compatibility have not been proven for the user's physical installation. Do not ask the user to uninstall Memo or overwrite the existing installation as a workaround.

## Product-completeness assessment

| Product layer | Current evidence | Actual remaining gap |
|---|---|---|
| Talk / intent | One natural-language entry point and bounded model-directed loop | Broader arbitrary-goal handling, clarification, capability selection and reliable completion across unrelated tasks |
| Capability system | Runtime-validated callable adapter registry | Registry/dispatcher coverage for native Android, files, code, image/PDF/video and other supported operations; no phantom registrations |
| Execution lifecycle | Bounded model/tool loop, approvals, cancellation, durable run summary | Per-invocation journal, cross-runtime checkpoints, safe inspect-and-continue after restart, idempotency and universal cancellation/recovery |
| Outcome verification | Main has adapter result statuses; PR #20 adds mandatory postconditions | Port and prove the shared postcondition protocol on latest main; define observable acceptance for each real adapter |
| Browser | Visible navigation and evidence capture on main; PR #20 adds approved provider prompt send/response capture | General versioned page/surface schemas, safe element targeting, form workflows, page-change detection, robust receipts and replay fixtures |
| Native apps | Existing bounded specialist automation surface | Same shared Talk dispatcher, runtime ownership, safety, event journal and postcondition verification; real-device/OEM acceptance |
| Work / memory | Encrypted workspace, typed records, evidence and bounded retrieval | Coherent source-grounded graph, reliable cross-surface links and inspect/edit/delete/export/privacy controls |
| Studio | Private draft foundations; PR #20 adds Talk-created Markdown artifacts/export | Full editor/revisions and factuality review, file formats/media, provenance and safe publication lifecycle |
| AI / voice | Local and compatible remote model integrations; tap-to-talk path | Proven on-device model loading/inference on the target phone, model compatibility/discovery, robust voice lifecycle and transparent remote-consent handling |
| Capture | Text/share and browser evidence paths | Multi-file, image, PDF, screenshot, audio/video intake with size limits, extraction, provenance, dedupe and recovery |
| Social platform | Product specification and requirement IDs | Connected accounts, feed, inbox, engagement, listening, publishing, metrics and monetization are not an implemented provider-backed product |
| Release / trust | Current-main CI and pinned signing job pass | Correct release package variant, compatibility with existing install, physical-device acceptance and product acceptance evidence |

## Verification evidence

| Check | Environment and commit | Result | Evidence |
|---|---|---|---|
| Mainline Android CI | Actions 45060946098, source head `9e16d18801e98049474dc9cbc282a1f52cad6a6f` | passed | [Run 45060946098](https://github.com/ysfclc/memo/actions/runs/45060946098) |
| Mainline APK signing and package identity | Same run | passed for the debug variant only | Same run; package ID is `app.memo.android.debug` |
| Emulator install-over-existing | Same run | passed for the tested debug package and its app-private data | Same run; not a physical-phone test |
| Product integration branch CI | Actions 45020438646, head `86c52b4e07f530b5ad0b727651b8ce7a75bd2918` | passed | [Run 45020438646](https://github.com/ysfclc/memo/actions/runs/45020438646) |
| Physical Android device | No device access in this session | not run | Required before device_verified/release-ready |
| Existing-user package/certificate compatibility | No installed APK/package evidence available | unknown | Must inspect/verify before in-place update |
| Product end-to-end acceptance | No physical-device mission executed in this session | not verified | Must test a real Talk mission and inspect saved artifact/evidence |

## Requirement and task ledger updates

No status checkboxes or requirement statuses were changed in this audit. Several current-main task descriptions are stale relative to the actual Talk execution source; reconcile them only in the same integration change that records exact source/test evidence. Do not use PR #20 branch code as proof that main already contains it.

## Decisions and deviations

- Work only in `ysfclc/memo`; `memo-2` remains reference-only.
- Do not merge PR #20 or any other branch into main without explicit user approval.
- Do not equate PR #20's green CI with phone acceptance or full product completeness.
- Do not deliver the debug-suffixed package as an update to the normal application ID.
- Keep all original product requirements, including the complete social platform and multimodal capture, in scope; sequence them behind the central execution path rather than deleting or silently deferring them.

## Risks, blockers and unresolved questions

| Item | Impact / severity | Mitigation | Next action |
|---|---|---|---|
| Product integration PR #20 is not in main | Critical to user-visible completeness | Preserve it as a candidate; review exact file diff | Reconcile/rebase its implementation onto latest main and rerun exact-head CI |
| PR #20 base is 2 commits behind latest main and touches overlapping UI files | High regression risk | No blind merge or wholesale snapshot replacement | Port changes deliberately; preserve PR #21 run inspection and UI scrolling contract |
| Debug package ID differs from normal package ID | Release blocker | Do not tell user to uninstall | Build and verify a release-variant package with exact app ID and pinned certificate; prove emulator upgrade path |
| No physical-device acceptance | Release blocker | Keep candidate test-only/unverified | Back up existing workspace, confirm installed package/cert, then perform in-place device acceptance |
| Process-death recovery is fail-closed but not resumable | Core product gap | Never replay uncertain steps | Design user-controlled inspect-and-continue as a new linked run using verified observations only |
| Browser and native app execution remain narrow/split | Core product gap | One shared dispatcher and lifecycle | Integrate real adapters, postconditions and run journal without feature-specific engines |
| Primary workspace and native workflow stores remain separate | Data integrity/UX risk | Preserve existing keys and migration paths | Plan a tested, reversible migration to one ownership/transaction boundary |

## Recovery / rollback

No source or data migration was performed. This branch contains only this audit checkpoint. It can be deleted without affecting application data or mainline source.

## Next recommended action

**P0 — Reconcile and integrate the PR #20 product implementation onto the latest mainline, then produce a correct release-variant candidate.**

Requirements: MEMO-ORCH-001, MEMO-AGENT-001, MEMO-CAP-001, MEMO-ARTIFACT-001, MEMO-RELEASE-001.

Acceptance:
1. Start from the exact latest main commit and preserve its Talk run inspection, page-level scrolling and existing encrypted data contracts.
2. Port/rebase PR #20's postcondition engine, Talk browser prompt/response, Studio artifact/export and model lifecycle work with a file-level review; do not replace source wholesale.
3. Ensure every capability advertised in Talk is callable, policy-checked and has an observable postcondition; unsupported capabilities remain unavailable.
4. Run source invariants, remote-runner tests, all core/app unit tests, lint, debug/release assembly, package/version/signature checks and emulator install-over-existing data-preservation tests on the exact resulting commit.
5. Produce a signed release-variant APK whose package ID and signing certificate are compatible with the existing normal Memo installation; do not claim user compatibility until verified.
6. On the physical phone, preserve an encrypted backup, confirm package/certificate compatibility, install in place (never uninstall), and execute one useful mission: user-supplied public HTTPS research → saved encrypted source evidence → a private Studio artifact → verified export. Inspect the actual file and run/evidence timeline.

After this first vertical mission is proven, the next core slice is safe inspect-and-continue after process death, followed by native Android adapter unification and broader multimodal capabilities. Social-platform requirements remain in the roadmap and are not removed.

## User-facing handoff

- Current main has a real but narrow Talk execution loop; it is not yet a complete personal agent.
- PR #20 contains important, CI-passing functionality not yet present in main.
- The current-main signed APK is a debug package and is not an in-place update for the normal package ID.
- No physical-device or full end-to-end mission acceptance was performed in this session.
