# Memo Engineering Log

Append-only session and implementation record. Do not rewrite historical entries. Detailed requirements remain in `TASKS.md`; user intent remains in `docs/USER_INPUT_LEDGER.md`.

## 2026-11-19 — Responsible stabilization continuation

- **Base:** `stabilization/usable-stage-2026-11-10` at `2fde1f0793d969192f38fc09465b14cee5310676`.
- **User instruction:** “Ok. Build responsibly”.
- **Scope:** one narrow blocker only: missing Polish/Turkish catalog entries for five already-present Compose UI strings. No product expansion, redesign, storage migration, signing change or app identity change.
- **Observed failure:** GitHub Actions run #923 / `44489842476` fails in `tools/verify_source.py` before Gradle because five strings are missing from `MemoLocalization.kt`: `Back to workflow`, `Open Work`, `Open browser`, `Plan only · no action has been executed`, `Workflow browser`.
- **Evidence rule:** do not count #821 or another PR's passing run as validation of this head. Only a completed run for the exact resulting commit can establish CI status.
- **Safety:** preserve package ID, pinned signing identity, encrypted data formats and existing installation. No uninstall, clear-data or destructive operation.
- **Next:** add accurate PL/TR translations and a focused regression test; then observe exact-head CI through signing/artifact checks. If blocked, record the actual failing stage and stop rather than claiming a build.
- **Start status:** no source change made yet.


## 2026-11-19 — Exact-head build, upgrade acceptance and pinned APK signing passed

- **Verified commit:** `7ad0eb0426f953a8946741c59247b081a258f94a`; branch `stabilization/usable-stage-2026-11-10`.
- **Actions:** [run #928, attempt 2](https://github.com/ysfclc/memo/actions/runs/44596413135), final conclusion `success`. Attempt 1 hit a transient corrupted Android system-image ZIP during SDK installation; rerunning the failed job succeeded.
- **Source repairs:** added PL/TR translations for `Back to workflow`, `Open Work`, `Open browser`, `Plan only · no action has been executed`, `Workflow browser`; added a PL/TR localization regression test. Added the registered read-only `browser.capture_evidence` capability to `ConversationWorkflowDraftFactory`'s allowlist, fixing the existing 188-test suite's draft-factory failure. Removed the premature composable-closing brace in `BrowserWorkflowScreen.kt`, keeping its approval dialog within the composable and fixing Kotlin top-level syntax errors.
- **Verification:** source verifier PASS (78 integration files; 434/434 static UI Text literals translated); remote-runner PASS; `:core:test` 188/188; `:app:testDebugUnitTest` 44/44; lint 0 errors, 42 warnings, 5 hints; `:app:assembleDebug` and `:app:assembleDebugAndroidTest` PASS; APK identity/signature PASS.
- **Upgrade safety:** API 35 emulator installed a lower-version baseline, seeded private and encrypted stores, then installed the newer signed APK in place. App-private data and both real encrypted Memo stores survived; legacy recovery and corruption checks passed. No uninstall or clear-data operation was used.
- **Signing:** isolated job signed and verified using pinned persistent certificate SHA-256 `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`; no ephemeral key fallback.
- **Artifact:** [Memo-Android-debug-APK, ID 14565824727](https://github.com/ysfclc/memo/actions/runs/44596413135/artifacts/14565824727); package `app.memo.android.debug`, versionCode `928`, versionName `4.0.0-memo-debug`; APK SHA-256 `6323c78ad428d6e92bb58d7822c546e3031994e7d5bce7b09fee19a1cda72340`; artifact ZIP SHA-256 `749829d23c2c5e824903136263d0b975432083b8069e52d60a218f96ef34b6fe`; source fingerprint `47f5a33c243cc6cf08eeea17369fa25daf6b02a236b7506cf95526891eb4942b`.
- **Open boundary:** lint warnings remain; physical-device install/UX/provider-session acceptance remains pending. PR #8 remains draft/open, `main` untouched, and no release/merge is authorized.


## 2026-11-19 — Exact current branch-head validation (run #929)

- **Current tested head:** `f62540cda08dc658309de16e037619201707f43a` (documentation-only commit after source commit `7ad0eb0426f953a8946741c59247b081a258f94a`).
- **Actions:** [run #929](https://github.com/ysfclc/memo/actions/runs/44599220097) — all required build and isolated signing jobs succeeded on the exact branch head.
- **Results:** source verification 434/434 UI strings; remote-runner PASS; core 188/188; app 44/44; lint 0 errors, 42 warnings, 5 hints; debug/test APK assembly PASS; API 35 signed in-place upgrade preserved app-private data and both encrypted Memo stores, with legacy recovery/corruption checks passing.
- **Artifact:** versionCode 929, package `app.memo.android.debug`; [signed APK artifact 14567058852](https://github.com/ysfclc/memo/actions/runs/44599220097/artifacts/14567058852); APK SHA-256 `9b60929f720ad8fade98dd23c03441dd0199b5c377994883d9cbe66e60635264`; ZIP SHA-256 `8c166e2aae7576a5ee969fae5c7f3295c48aeb2a5a992bf5b411e827f20e77ad`. Certificate SHA-256 unchanged and pinned.
- **Not tested:** actual user phone, physical-device UX and real signed-in provider journey. No phone data was accessed. STAB-05 remains the next gate; don't uninstall or clear Memo data.
- **Governance:** PR #8 draft/open, `main` untouched; no merge/release/destructive change.

## 2026-11-19 — Browser automation and approval UX defect intake

- **User reports:** browser automation feels hardcoded around Frontier providers; selected providers did not open the browser despite the user's report that accounts were already logged in; the approval button is not visible/discoverable; checkpoints are confusing and approval requests appear when no concrete action is shown.
- **Ledger:** appended INPUT-2026-11-19-004 through -006, preserving the user's wording and separating browser behavior bugs from approval discoverability and checkpoint-friction feedback.
- **Source inspected:** BrowserWorkflow.kt, BrowserSession.kt, BrowserWorkflowScreen.kt, ActionApprovalInboxScreen.kt, ActionApprovalLedger.kt, WorkflowCheckpointCoordinator.kt, current workflow-control audit and recovery execution plan.
- **Confirmed source facts:** the current browser workflow screen is Frontier-only by design and binds to a fixed provider list; general browser tasks are a separate path. The selected-provider path calls visible WebView navigation, but the reported no-browser behavior has not been reproduced. The approval CTA exists only in a conditional browser AlertDialog; the inbox does not offer an approve callback. The shared ledger has exact-content/one-use checks. Checkpoint reservation explicitly states that it does not invoke a runtime or perform an external action.
- **Next plan:** BRW-15 trace/reproduce the no-browser path; BRW-14 separate general-purpose browser runtime from optional Frontier presets; APR-01 make approval discoverable without weakening exact-content security; CHK-01 distinguish progress/input/recovery/review/authorization and stop unnecessary approval prompts. Existing WF-01–WF-13 remain active.
- **Verification boundary:** source inspection only for these new defects; no source behavior changed, no device reproduction or new acceptance claimed. Latest tested build remains run #930 on parent b1a949bd5128f770a6125ff4d16b9dd590736724; this documentation commit requires its own exact-head CI.


## 2026-11-19 — Browser visibility, approval re-entry and checkpoint-friction fixes

- **Root cause confirmed in source:** BrowserWorkflowScreen started BrowserSession.startBrowserWorkflow() while the Android WebView was mounted only by the separate BrowserScreen. The workflow route did not switch to that screen, so navigate(provider.startUrl) could run without a visible browser surface. The inbox's old "Open live approval" route also selected the ordinary Browser section instead of the Workflow controls, hiding the conditional exact-prompt approval dialog.
- **Navigation fix:** successful workflow start now opens the dedicated Browser screen; after the user approves the exact prompt, Memo returns to the visible Browser screen for submission and response capture. When an approval is waiting, BrowserScreen exposes a prominent "Review prompt & approve" control back to the workflow dialog. The inbox uses the same label and routes to Workflow controls.
- **State continuity:** the active external action ID moved from transient composable state into BrowserSession, so switching between Browser and Workflow does not lose the action identity used by response capture and audit completion. The WebView remains hosted only by BrowserScreen; it is intentionally not embedded inside the independently scrolling workflow page because Android WebView can render above Compose siblings.
- **Checkpoint friction:** local intent structuring is now opt-in rather than automatically delaying browser navigation whenever a local model is installed. Removed the duplicate broad preflight consent checkbox; each external send still requires the exact provider, destination and full prompt to be reviewed and approved through the one-use ledger. Manual sign-in/page-setup waits no longer mark the job step as NEEDS_REVIEW; exact approval, sent and uncertain actions still do. Paused jobs are labelled "Needs your input".
- **Regression coverage:** source verifier now checks the Browser/Workflow navigation loop, shared approval identity, exact-prompt route, no duplicate preflight consent, and separation of manual input waits from approval checkpoints. Source SHA-256 manifest refreshed.
- **Verification boundary:** code changes are committed to stabilization/usable-stage-2026-11-10; exact-head CI is pending. No APK, Android emulator, physical phone or real provider-session test is claimed yet. BRW-14 remains open; BRW-15, APR-01 and CHK-01 are partial pending device and broader cross-runtime acceptance.


## 2026-11-19 — Remove redundant browser capability approval gates

- A second source-level approval defect was found in the generic capability compiler: it treated browser.prepare_prompt (fill a visible composer, do not submit) as approval-gated and used it as the approval capability for external actions. The multi-chat orchestration capability was also marked as a blanket external side effect, even though each browser.submit_prompt is already independently approved.
- Corrected the runtime contract: browser.prepare_prompt and browser.run_multi_chat_workflow are orchestration/preparation capabilities without a blanket approval; browser.submit_prompt remains the approval-gated external side effect.
- CapabilityCompiler now adds browser.submit_prompt—not browser.prepare_prompt—when an external browser send is required. RuntimeRegistryTest and CapabilityCompilerTest cover the distinction. The source verifier now guards against reintroducing these duplicate checkpoints.
- This reduces unnecessary approvals without weakening the exact per-send gate. Other runtimes and WAITING_FOR_USER producers remain under CHK-01 audit.
