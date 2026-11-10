# M7.A1 checkpoint — Account research core

**Date:** 10 November 2026  
**Branch:** `feature/sales-ops-account-research`  
**Base:** `1cc8802b35296e2b4a082708790a631870121785` (mainline consolidation)

## Implemented in this slice

- Added SalesAccountResearch core domain types for industrial segment, access route, research state, weighted ICP factors, factor assessments and score summary.
- Six ICP weights total exactly 100. Unknown factors earn zero and remain outside assessed coverage; score separately reports earned points, assessed weight, normalized assessed score and best possible score.
- Deterministic validation requires a valid HTTP(S) company website, location (or explicit Unknown), rationale and existing source evidence for every scored factor, retrieval timestamps and non-empty captured source content/summary.
- READY_FOR_REVIEW is blocked while any ICP factor remains unknown. Disqualification requires an explicit reason.
- Versioned account profile serializes into namespaced EntityRecord.properties; no AppSnapshot schema migration or second CRM database.
- Duplicate matching checks normalized website domain and normalized company name, returns warnings only and excludes rejected/merged records. It never mutates or merges existing entities.
- MemoWorkspaceViewModel.createSalesAccount requires an unlocked/healthy vault, validates against captured workspace evidence, blocks creation when potential duplicates exist, and creates a proposed ORGANIZATION through existing Work entity persistence.

## Verification

- Focused isolated JVM harness: **11/11 scenarios passed** for weights, unknowns, score coverage, source validation, review gating, disqualification, profile round-trip, future schema rejection, duplicate warnings, archived candidate exclusion and invalid score bounds.
- This is not yet the real Gradle test task result. Run ./gradlew :core:test on this branch before merge.
- Full Android compile/lint/APK and device acceptance remain open. The existing 8 November APK is historical evidence for its own source commit only, not this branch.

## Remaining M7.A1 work

1. Run real :core:test and full project verification; fix any discrepancies.
2. Add a focused Work account create/review UI with evidence selectors and factor-level unknown/score controls.
3. Add integration tests for the ViewModel save/duplicate/vault gates.
4. Build a new APK from the exact accepted integration commit and verify package/signature/checksum.
5. Physical-device install-over-existing acceptance.

No browser research automation, contact harvesting, email, DMs or external actions are included in M7.A1.
