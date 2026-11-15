# ORCH-007 — Shared runtime adapter and postcondition evidence contracts

**Date:** 15 November 2026  
**Status:** Source implemented and exact-head CI verified. Concrete runtime adapters and observer implementations are still open.  
**Branch:** `stabilization/usable-stage-2026-11-10`  
**Source commit:** `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34`

## User direction

The user explicitly asked to resume Memo development one coherent increment at a time while preserving the original product brainstorming, all requested capabilities and all reported bugs. This increment implements only the shared contract layer. It does not authorize a merge, release, production deployment, signing change or destructive migration.

## Source changes

- `core/src/main/kotlin/app/memo/android/runtime/WorkflowRuntimeAdapter.kt`
- `core/src/main/kotlin/app/memo/android/runtime/WorkflowPostcondition.kt`
- `core/src/test/kotlin/app/memo/android/runtime/WorkflowRuntimeAdapterTest.kt`
- `core/src/test/kotlin/app/memo/android/runtime/WorkflowPostconditionTest.kt`
- `tools/verify_source.py`
- `SOURCE_SHA256SUMS.txt`

### Runtime request and adapter boundary

- The request is tied to the saved workflow graph, exact TaskSpec, active attempt, capability-manifest fingerprint, current registered runtime/capabilities and stable idempotency key.
- It rejects runtime/capability drift, unresolved or modified manifests, approval-gated tasks, external writes and external side effects.
- Input/output size limits, task deadline and cooperative cancellation are explicit.
- The adapter returns typed candidate output. It cannot persist workspace state, grant approvals or declare a checkpoint complete.

### Independent postcondition evidence

- Only registered deterministic observer methods are supported: Memo record lookup, browser-visible state, Android Accessibility state and file-artifact inspection.
- The observer must use an available, registered, read-only capability distinct from the capability that produced the output.
- Evidence is bound to the exact attempt, task fingerprint, postcondition fingerprint, observer identity, observation time and a separate evidence ID/hash.
- Only `VERIFIED` evidence permits checkpoint completion. `NOT_VERIFIED`, `CONTRADICTED` and `NEEDS_REVIEW` do not.
- A runtime output receipt remains proof of saved bytes only, not proof of the user's requested outcome.

## Tests and exact-head CI

- Runtime tests cover exact TaskSpec/attempt/runtime/capability binding, manifest tampering, runtime availability/policy drift, adapter uniqueness, approval bypass, deadlines/cancellation and output bounds.
- Postcondition tests cover saved-output receipt validation, registered observer requirements, independent/read-only observer enforcement, exact attempt/task/observer binding and non-verified outcomes.
- GitHub Actions run #678, ID `43666486553`, passed: https://github.com/ysfclc/memo/actions/runs/43666486553.
- Passed source invariants/checksum verification, remote-runner tests, `:core:test`, `:app:testDebugUnitTest`, `:app:lintDebug`, `:app:assembleDebug`, `:app:assembleDebugAndroidTest`, APK identity/signature checks, API 35 in-place encrypted-store upgrade instrumentation and isolated pinned signing.
- Build: `BUILD SUCCESSFUL in 4m 49s`.
- APK package `app.memo.android.debug`, versionCode `678`, versionName `4.0.0-memo-debug`.
- APK SHA-256: `026a9be7743e1e91cbc3b7807bed19504db6ea93b3d9249c91046ea15100f469`.
- Artifact: [Memo-Android-debug-APK, ID 14143185526](https://github.com/ysfclc/memo/actions/runs/43666486553/artifacts/14143185526).
- Pinned certificate SHA-256: `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`.

## Not implemented by this increment

- No generic dispatcher, concrete deterministic app adapter, browser adapter or native Android adapter is connected.
- No workflow-wide budget ledger or full cross-runtime cancellation/pause/resume/takeover path is added.
- No independent browser/native postcondition observer is wired to the DAG.
- No physical-device acceptance is claimed. The user installed the earlier #674 APK but has not checked it; emulator upgrade success is not a substitute.

## Next increment

Implement the deterministic Memo app-runtime adapter through the durable checkpoint journal. Enforce shared timeout/cancellation and process-death recovery, prove with pure-core tests, then enable browser/native adapters only in later increments.
