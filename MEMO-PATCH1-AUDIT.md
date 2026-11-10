# Memo 1 — feature checkpoint against `TASKS.md`

**Target:** `ysfclc/memo`, branch `ysfclc-patch-1`  
**Purpose:** product/source checkpoint, not an APK or release claim. Memo 2 is a requirements reference only; Memo 1's code and stored data remain authoritative.

**Canonical package:** `Memo-Android-Repository.zip` includes this audit and the full `TASKS.md` inside the `Memo-Android/` project root. The packaged archive has exactly one Android project root and one `settings.gradle`; ZIP integrity, all 90 source-manifest entries, and offline structural verification were rechecked. Use the sibling `.sha256.txt` sidecar delivered with the archive to verify its exact bytes.

## Product direction preserved

Memo's operating model is **local AI as the brain; explicitly authorized apps and browser sessions as its hands; HITL always**. The user may direct local AI to interact with services such as ChatGPT, Claude, Gemini or other AI tools through their normal authorized apps/sites. This is different from sending Memo prompts directly to a provider's inference API. Direct API use, where present, must stay a clearly disclosed optional path with request-level consent; it is not the default orchestration design. The user can pause, take over or stop; Memo cannot grant itself new authority.

## Work in this source increment

The new branch-ready source package adds a first real approval-control vertical slice:

- `ActionApprovalLedger` in `:core` binds an action to the runtime, capability, HTTPS destination/path and exact payload hash; approval expires, is one-use, and cannot be reused if the envelope changes.
- A browser provider prompt is staged before submission and shown in full for explicit human approval. The live destination and payload are checked again before sending.
- The core schema is now v4 while retaining imports for older v1–v3 workspaces. Imported or restarted approvals are invalidated; uncertain in-flight work is moved to `NEEDS_REVIEW`.
- Completion requires a saved response-evidence record for that job. Missing/uncertain evidence does not become success.
- The More menu now includes **Action approval inbox** with queue, needs-review and history views. It surfaces exact payload/hash, destination, runtime/capability, evidence and recent chained audit events. Pending actions can be rejected; active/approved outcomes can be marked for review. The inbox cannot approve stale history: only a still-matching pending action can be routed to its live browser confirmation.
- Rejecting a pending proposal from the inbox also stops the matching live browser workflow. The ledger now rejects invalid browser destinations (non-HTTPS, credentialed URLs, query strings/fragments) and orphaned audit events. A review flag warns that it cannot undo an already-started external submission.

## Verification performed in the source workspace

- `python3 tools/verify_source.py`: **passed**. This includes a central-inbox integration and stale-approval guard check, Android XML/manifest checks, one-launcher boundary, and **283/283** eligible literal `Text()` labels across six Compose source files represented in the Polish/Turkish catalog.
- `PYTHONPATH=. python3 -m unittest discover -s tests -v` from `remote-runner/`: **12/12 passed**.
- The actual `ActionApprovalLedger.kt` compiled with `kotlinc` against minimal snapshot/evidence stubs and passed targeted checks for proposal → human approval → execution → evidence-backed completion, missing-evidence review, payload mismatch invalidation, clean-HTTPS destination policy, expiry, orphan-event rejection and audit tampering.
- The new `ActionApprovalLedgerTest.kt` contains corresponding JVM regression tests, but these **have not been run through Gradle**.

## What is still open against the runbook

| `TASKS.md` area | Current status | What remains before it can be called done |
|---|---|---|
| Source reconciliation / build baseline | Partial | Compare the active branch and `main` archives path-by-path without replacing `main`; complete the Gradle baseline. |
| One canonical durable workspace | Partial | Primary Compose vault and legacy native workflow store remain separate; compatibility-preserving migration and atomic recovery are unproven. |
| Exact-content approval | Partial | Implemented for visible frontier-browser prompt submission; not universal for native, guest, publication, file or other external actions. |
| Approval inbox / history | First source slice added | Needs unified cross-runtime records, richer permission/risk summary, full filtering and device tests. |
| Global pause / emergency stop | First source slice added | Header control requests cancellation of local inference, browser workflow/task and active native accessibility execution; pending approvals are rejected and approved/in-flight outcomes become NEEDS_REVIEW. Guest cancellation is best-effort and reported honestly. Durable cross-runtime cancellation token, race tests and device verification remain open. |
| Dependency-aware workflow executor | Open/partial | No complete durable DAG spanning local model → browser/app → review → artifact, with version-pinned capabilities, checkpoints, timeouts, bounded retry and cancellation. |
| Local model hub / streaming | Partial | A local model path/catalog exists, but device compatibility/readiness, streaming behavior, cancel, resume, checksums, RAM/storage diagnostics and rollback are not verified. |
| User-authorized external AI websites | Partial | Visible sequential provider workflow exists; provider-specific selectors/scripts remain, not a generic versioned browser surface engine. |
| Native app automation | Partial | Bounded Accessibility runtime exists; device/permission/foreground behavior unverified and not under the shared approval ledger. |
| Guest/remote runner | Partial | Python suite passes 12/12, but paired Android client and any real remote device are not verified; it is optional, not a hosted service. |
| Voice and quick capture | Partial | Tap-to-talk and basic share/text capture exist; not always-on, not guaranteed offline, and full multi-file/immutable capture provenance remains open. |
| Self Model / artifact studio / publishing | Open/partial | Typed records and some source-grounded outcome proposals exist; full evidence-backed profile, CV/content studio, safe destination review, publish receipts and metrics loop are incomplete. |
| Council / multi-model workflows | Partial | Local and browser-based paths exist, but are not a single dependency-aware runtime and disagreement/evidence ledger end-to-end. |
| Encrypted backup / migration | Partial | Encrypted workspace exists and JSON import/export is present; authenticated encrypted portable backup, restore preview, rollback and full schema migration acceptance remain open. |
| EN / PL / TR | Source partial | Static literal catalog checks pass, but dynamic/system strings and manual Polish/Turkish screen review remain. |
| Android build / lint / APK | **Unverified** | Gradle distribution download fails here due network DNS; no successful current-source compile, JVM suite, Android tests, lint or APK. |
| Real-device acceptance | **Unverified** | Install and test local inference, browser submit/reject/restart, accessibility, voice, reminders, vault migration and cancel/stop on a real phone. |
| Production release/security | Open | Signing, encrypted-backup security, audit/authenticity review, permissions, performance/battery and accessibility sign-off remain. |

## Build boundary

`python3 tools/verify_source.py` and the Python runner tests do not prove Kotlin/Compose compilation. The local Gradle wrapper could not fetch Gradle 8.14.6 from `services.gradle.org` because DNS resolution failed. Therefore no current-source APK is claimed, and the tool is not described as build-verified or device-validated.

The source package below is a **development source increment**, not the finished product. It deliberately keeps unimplemented requirements marked partial/open rather than copying Memo 2's checklist state.

## Repository write status

This session did not commit directly to GitHub. The connected GitHub write integration rejected a contents update with HTTP 403. The updated source ZIP and master task ledger are supplied as downloadable files so the user can replace the canonical archive on `ysfclc-patch-1`. The `main` archive has not been replaced.


## Additional product increment — global stop

The source now contains a top-level **Stop active work** control. It asks the local AI runner, browser workflow/task and active AccessibilityService automation to cancel. Pending proposals are rejected; already approved or in-flight external side effects are flagged `NEEDS_REVIEW`, not falsely described as undone. If the last guest-runner receipt is nonterminal, Memo attempts remote cancellation but tells the user to verify the final state. This is a first control-plane slice, not yet a durable cross-runtime cancellation token.
