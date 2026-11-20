# M3.8.A10 — first deterministic Memo app-runtime capability

**Date:** 16 November 2026
**Status:** Source implementation and regression tests committed; exact-head CI #767 passed all automated gates including API 35 install-over-existing encrypted-store preservation. Physical-device acceptance and final pinned APK handoff remain pending.
**Branch:** stabilization/usable-stage-2026-11-10
**Draft PR:** https://github.com/ysfclc/memo/pull/8

## Product decision

The first useful runtime increment for Memo's agreed vision is a real, deterministic capability inside Memo itself, connected to Talk and Work. The selected capability is app.entities / entity.propose. It turns source-checked Talk outcome candidates into reviewable Work records. This is intentionally narrower than a generic agent or executor and is not a new silo product.

## Execution path

1. Talk retains its existing model-based candidate extraction and OutcomeParser validation. Remote model use remains behind the existing explicit per-request consent control.
2. Before persistence, the coordinator requires every candidate excerpt to match an individual original message in the exact source conversation. Missing or cross-message excerpts fail closed.
3. Memo compiles a one-node TaskSpec for the exact registered app.entities / entity.propose capability. The invocation binds the saved graph, active attempt, capability-manifest fingerprint, idempotency identity, bounded input and deadline.
4. EntityProposalRuntimeAdapter validates and canonicalizes the bounded batch. It returns candidate output only; it does not write the workspace, approve records or claim success.
5. Before execution, the exact validated input JSON is saved as a WORKFLOW_INPUT evidence record in the encrypted workspace, bound to the job, step, source identities, content type and SHA-256. The coordinator rechecks the returned batch against that saved input, then writes source-linked EntityRecord proposals, PROPOSES relationships and a Work artifact through the existing LocalWorkArtifactCoordinator. All entity records remain PROPOSED.
6. The saved Work artifact receives the existing content-bound runtime receipt. OUTPUT_SAVED proves only that output bytes were persisted; its postcondition remains NOT_VERIFIED.
7. A separate read-only app.jobs / job.verify observer reads the saved workspace snapshot and checks each proposal's identity, PROPOSED state, source conversation, source job, original message ID and exact excerpt.
8. Only VERIFIED observer evidence is saved as a separate WORKFLOW_VERIFICATION record and may complete the checkpoint. Talk's parent extraction job then moves to the explicit Work review step. Nothing is auto-approved.

The ViewModel persists the prepared job and active attempt before running the adapter, persists output before observation, and persists verification before updating the source Talk extraction history.

## Safety boundaries

- No entity is automatically approved.
- No browser submission, Android tap, external write, frontier-provider request or other external side effect is enabled by this adapter.
- The adapter implements only entity.propose; it is not yet a general dispatcher for every registered Memo capability.
- Missing source evidence, mismatched input/output, invalid receipts, a duplicate-only batch or a non-verified observer result cannot complete the checkpoint.
- Process-death recovery continues to use the existing attempt journal and requires review for interrupted work. The exact input remains encrypted and can be reconstructed with restorePrepared; missing, ambiguous or hash-mismatched input evidence fails closed rather than regenerating a new payload. Each new workflow job gets a distinct input-evidence identity, including repeated identical batches.

## Files

- core/src/main/kotlin/app/memo/android/runtime/EntityProposalRuntimeAdapter.kt
- core/src/main/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinator.kt
- core/src/test/kotlin/app/memo/android/data/EntityProposalWorkflowCoordinatorTest.kt
- app/src/main/java/app/memo/android/data/MemoWorkspaceViewModel.kt
- app/src/main/java/app/memo/android/ui/WorkspaceScreens.kt
- tools/verify_source.py
- TASKS.md, docs/STATE.md, IMPLEMENTATION_LOG.md and BUILD_STATUS.md

## Regression coverage

- Full execution through the exact runtime and independent read-after-write verification.
- Rejection of excerpts that cannot be linked to one original conversation message.
- Duplicate detection without creating a second entity.
- Proof that a missing persisted proposal cannot pass the independent postcondition.
- Recovery of the exact encrypted input after workspace recreation, refusal when input evidence is missing, and distinct input records for repeated identical batches.

## Verification boundary and next step

Exact-head CI #767 passed source verification, remote-runner tests, core and app unit tests, lint, debug/Android-test APK assembly, APK identity/signature checks, and the API 35 encrypted-store install-over-existing test. The emulator verified that app-private data and both real encrypted Memo stores survive the update, and legacy recovery/corruption tests passed. This was a PR validation run, so pinned signing and final APK artifact handoff were skipped; no new signed installable APK is claimed. The increment remains partial pending physical-device Talk → Work review acceptance.

After physical-device acceptance, broaden Memo's deterministic app-runtime path one safe internal capability at a time. The next useful candidates should close another concrete Talk → Work outcome gap while reusing the encrypted input, durable attempt, receipt and independent observer contracts. Browser and native Android adapters remain separate later increments because they need their existing approval, permission and visible-control boundaries.
