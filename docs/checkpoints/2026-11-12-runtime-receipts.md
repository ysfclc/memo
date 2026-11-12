# Checkpoint — content-bound runtime receipts

**Date:** 12 November 2026  
**Repository:** `ysfclc/memo`  
**Branch:** `stabilization/usable-stage-2026-11-10`  
**PR:** #8 (draft, open; base `main`)  
**Product area:** Work / orchestration / Live evidence integrity

## Product reason

Memo must distinguish a runtime returning output from the user's requested outcome actually being achieved. A model response is a draft, not proof of completion. Before adding more runtime adapters, each saved output needs a durable, inspectable identity that ties the bytes in Evidence to the exact execution attempt that produced them.

## Implemented in source

- Added `WorkflowRuntimeReceipt` in pure core.
- A receipt binds attempt ID and number, job/checkpoint, runtime ID, exact capability IDs, stable idempotency key, start/completion timestamps, Evidence ID and SHA-256 of the exact output content.
- The receipt is attached to the artifact metadata in the same immutable transition that stores the artifact and closes the attempt as `OUTPUT_SAVED`.
- The attempt journal and Evidence metadata must both carry the receipt marker. Workspace validation rejects a missing or mismatched receipt and recomputes the output hash to detect later artifact mutation.
- Receipt state explicitly remains `postconditionStatus=NOT_VERIFIED`. It does not mark the checkpoint complete, authorize an external effect or assert that a model's claims are true.
- Existing schema-7 output attempts without the new receipt marker remain compatible; no workspace schema migration was introduced.
- Added regression tests for exact receipt binding, workspace validation and mutated output rejection; extended the offline source verifier to require the receipt model and safety markers.

## Changed source

- `core/src/main/kotlin/app/memo/android/data/WorkflowRuntimeReceipt.kt` (new)
- `core/src/main/kotlin/app/memo/android/data/WorkflowAttemptJournal.kt`
- `core/src/main/kotlin/app/memo/android/data/LocalWorkArtifactCoordinator.kt`
- `core/src/test/kotlin/app/memo/android/data/LocalWorkArtifactCoordinatorTest.kt`
- `tools/verify_source.py`
- `SOURCE_SHA256SUMS.txt`
- `PRODUCT_GAP_REGISTRY.md`
- `docs/REQUIREMENT_TRACEABILITY.md`
- `TASKS.md`

## Verification and limits

- Source changes and regression tests are committed to the private development branch.
- The SHA-256 source manifest was refreshed for every changed source/test/verifier file it tracks, including the new receipt type.
- The test assertions and integrity paths were reviewed statically, but **Gradle/JVM tests have not been executed** on this head. No exact-head lint, APK, emulator or physical-device result is claimed.
- GitHub Actions remains blocked before runner assignment on the current account/run pattern. The next Vercel deployment was rejected with HTTP 402 due to the daily deployment quota; the APK build path cannot be used again until that quota resets.
- The existing signing identity has not been changed. No signing secrets, user data or production branch were touched.

## Next implementation slice

Add a shared runtime-adapter contract and a deterministic postcondition evidence path. Keep adapters replaceable and enforce runtime/capability identity, budgets, cancellation, approval requirements and process-death recovery centrally. Browser and native adapters must remain visible and preserve their current exact-content approval/permission boundaries. Do not mark any node complete from an unverified model response.
