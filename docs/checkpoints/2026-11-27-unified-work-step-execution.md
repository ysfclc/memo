# 2026-11-27 — Unify Work-step execution with the shared Talk engine

## Product outcome

A model-composed Work plan must not have a second execution engine. Running an eligible local-model checkpoint from Work must use the same provider-agnostic `AgentExecutionEngine`, runtime-validated capability adapters, durable run index, encrypted conversation event journal, bounded model/tool budgets, cancellation handling and postcondition gate already used by Talk.

The user still starts and reviews work from Work. Work remains the durable plan and artifact surface; it does not own a parallel model execution lifecycle.

## Current defect

The first executable Work node is narrowly classified as `LOCAL_MODEL` / `app.models` / exactly `model.generate`. Its current UI callback calls `AiRuntimeViewModel.runPrompt()` directly, saves an `EvidenceRecord`, and changes the checkpoint through non-durable `updateJobStep()`. This bypasses the shared engine's durable run record, event transcript, capability registry, idempotency envelope, cancellation semantics and adapter postcondition verification.

This is not a reason to create a DAG scheduler in this increment. It is a reason to route the existing first node through the canonical engine.

## Design constraints

1. Reuse `AgentExecutionEngine`; do not add a Work-specific orchestrator, scheduler or event store.
2. Keep the executable node allowlist exact: participant `LOCAL_MODEL`, runtime `app.models`, and required capabilities exactly `[model.generate]`. All other graph nodes remain non-executable by this increment.
3. Reuse the existing registered workspace artifact adapter and encrypted Evidence store. The model must create a real private draft artifact; a text answer alone is not checkpoint completion.
4. Bind the execution to the Work plan's exact source conversation and source USER message. If either is missing, refuse to start.
5. Persist the run record before model execution. Append every engine event to the originating conversation and update the run index using `appendAgentEventDurably`; stop if journal persistence fails.
6. Verify the artifact through the adapter's existing encrypted read-back postcondition. Then persist the Work checkpoint's `NEEDS_REVIEW` state and exact evidence reference with a vault read-back. Do not mark the checkpoint complete automatically.
7. On cancellation, timeout, journal failure, uncertain artifact persistence or process interruption, do not replay the model write. Leave the checkpoint failed/review-only with a clear recovery explanation; any existing artifact must remain discoverable by its run/source provenance.
8. Keep remote model execution, browser, Android app, external-write and destructive nodes unavailable. No approval policy may be weakened.
9. Preserve package identity, signing, encrypted workspace schema, backup compatibility and existing user data.

## Acceptance

- Work launches the same engine used by Talk and persists a durable run before the first model call.
- The exact source conversation receives the run's lifecycle transcript; the run index and transcript are read back before the engine continues.
- The runtime registry exposes only actually callable adapters. The local Work step can save a private artifact through the registered `workspace.save_artifact` adapter.
- A successful artifact is read back with its exact evidence ID and source provenance; the corresponding Work checkpoint becomes `NEEDS_REVIEW`, never `COMPLETED`.
- A final model response without a saved artifact does not count as a successful Work step.
- Missing source message, unavailable local model, another active run, invalid checkpoint type, or missing capability fails closed.
- Cancellation and journal/persistence uncertainty never trigger automatic replay.
- Tests cover success, no-artifact final response, invalid node classification, missing source, persistence failure, cancellation, and duplicate/retry behavior without domain-specific intent strings.
- Exact-head CI passes source verification, core/app unit tests, lint, debug/release assembly, APK identity/signature checks and emulator install-over-existing/data-preservation gates. Physical-device acceptance remains separately labelled.

## Explicit non-goals

- No general DAG scheduler or cross-runtime pause/resume.
- No new executable node types.
- No change to TaskSpec schema, encrypted store schema, package ID or release signing.
- No new primary navigation surface or workflow-specific prompt/keyword routing.

## Verification record

Implementation is committed on PR #32 (latest code commit `8b61879fb139472646393459e737087c6c16f05a`). Exact-head Actions validation is pending; no merge, installable APK or physical-device acceptance is claimed.
