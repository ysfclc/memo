# Memo Fresh Start — 16 November 2026

**Repository:** `ysfclc/memo`  
**Working branch:** `stabilization/usable-stage-2026-11-10`  
**Baseline source:** `fcafb582ad694926823534f768cdd509e93bd44b`  
**Tracking PR:** [#8](https://github.com/ysfclc/memo/pull/8), draft/unmerged  
**Product authority:** `docs/MEMO_PRODUCT_CONTRACT.md`  
**Execution ledger:** `TASKS.md`

## User direction

The user asked to build Memo correctly, with deliberate product judgement: every increment must materially serve the agreed vision. Preserve the complete product contract, prior defects, recovery requirements and data. Do not build disconnected modules, generic dashboards, demo-only flows or superficial UI.

## Baseline verified before changes

- Memo is the Android-first personal AI operating agent described in `docs/MEMO_PRODUCT_CONTRACT.md`.
- The intended continuous loop remains capture intent → retrieve permitted context → compile a typed workflow → route to the right runtime → show/control execution in Live → independently verify the outcome → persist useful work in Work/Studio.
- Talk, Live, Work and Studio remain connected surfaces over one local-first workspace; conversation is not the database.
- ORCH-007 shared runtime request/resolver and postcondition evidence contracts are implemented and exact-head CI verified.
- Run #678 passed source verification, remote-runner tests, core/app JVM tests, lint, APK assembly/identity, API 35 in-place encrypted-store upgrade instrumentation and isolated pinned signing.
- Run #678 APK is versionCode 678, package `app.memo.android.debug`; artifact ID `14143185526`.
- Physical-device install-over-existing and product-flow acceptance remain pending. Do not claim phone acceptance or ask the user to uninstall/clear data.
- No merge, production release/deployment, signing-identity change or destructive migration is authorized.

## Last completed engineering increment

**ORCH-007 — shared runtime adapter and postcondition evidence contracts** (15 Nov 2026; source commit `cef15adb9b6f7b4fd991ef7404e12f8b4bc9da34`).

It establishes exact TaskSpec/attempt/runtime/capability binding, manifest integrity, bounded inputs/outputs, deadlines/cancellation, and independently observed postconditions. Adapters cannot persist workspace state, grant approval or self-declare success.

## This increment — deterministic Memo app-runtime adapter

**Why this is the next useful step:** Memo's typed workflow graph and durable attempt journal currently stop at reservation; there is no connected deterministic app runtime. Without this bridge, Talk can propose work but cannot reliably turn safe internal Memo tasks into saved outcomes. This is the central product loop, not a standalone feature.

**Scope boundary:**
1. Implement only deterministic, local Memo-owned capabilities with explicit registered handlers.
2. Dispatch through the existing validated runtime request and durable per-node attempt journal.
3. Keep persistence/state transitions outside the adapter; return typed candidate output for the existing receipt/review path.
4. Enforce exact runtime/capability identity, input/output bounds, deadline and cooperative cancellation.
5. Recover an interrupted RUNNING attempt as INTERRUPTED/NEEDS_REVIEW; never blindly replay it.
6. Reject approval-gated, external-write, browser and Android-app tasks. No network, browser submission, Android accessibility action or external side effect in this increment.
7. Add focused tests for successful deterministic dispatch, missing/duplicate handler, manifest drift, cancellation, timeout, invalid output and process-death recovery.
8. Update source verifier/checksums and the status ledger; run exact-head CI.

**Explicitly out of scope:** generic browser/native adapters, external action execution, full workflow-wide scheduler, pause/resume/takeover UI, redesign, new product modules, merge/deployment, signing changes and data migration.

## Acceptance gates

- Pure-core tests prove the adapter cannot bypass the validated request, mutate persistence, approve actions or mark a checkpoint complete.
- Safe internal capability execution is deterministic and idempotency-aware.
- Output receipt is not treated as proof of the user's goal; only the independent postcondition path can verify completion.
- Interrupted attempts remain review-required and preserve prior evidence.
- Source verifier, checksum verification, core/app tests, lint, APK identity and emulator encrypted-store upgrade pass on the exact commit.
- Physical-device acceptance remains a separate gate and must be reported honestly.

## Stop conditions

If the available app data model cannot safely support a capability without crossing the encrypted-store transaction boundary, do not invent a shortcut. Record the blocker and implement only the safe contract/test foundation. Do not merge PR #8 or modify `main`.
