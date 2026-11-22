# Memo product build integration checkpoint — 21 November 2026

## Baseline
- Repository: `ysfclc/memo`
- Base branch: `main`
- Base commit: `d80f19ae6ff956392cbe11c2b1aaead1758a7d19`
- Integration branch: `build/product-core-integration-2026-11-21`
- Current main includes the Talk/Work page-level scrolling repair from `d189a732416c241d1e5d94d6a20d6a7e11345034`.

## Integrated work
1. M3.A4-S7 shared capability postcondition verification:
   - Exact source head: `bfe40a182b1e283699a8c4c36bf9c612f83981c7`
   - Exact-head Actions run: `44907881554`, completed successfully.
   - Every successful Talk capability needs an adapter verifier. Read-only verification failures are observed as failures; uncertain local/external/destructive writes become `NEEDS_REVIEW`. Write verifiers must match the exact evidence reference returned by the invocation.
   - Current adapter coverage: bounded workspace search, encrypted note/proposal read-back, visible browser navigation and encrypted browser capture evidence.
2. UI regression/release gate:
   - Binding page-level Talk/Work scroll contract.
   - Real-device acceptance checklist for Talk and Work, including keyboard, language, navigation, long/empty content and horizontal chip rows.
   - Release candidate integrity rule: exact source commit and APK hash plus physical-device acceptance; unverified candidates are test-only, not user-ready updates.

## Compatibility boundaries
- No app/package identity, signing key, encryption format, Keystore alias, workspace schema or backup schema changes were introduced by this integration.
- Existing encrypted workspace and native workflow stores remain separate; this integration does not claim to unify them.
- No physical-device acceptance is claimed.

## Verification
- Upstream postcondition branch exact-head CI: PASS (run `44907881554`).
- Latest main source snapshot CI: PASS (run `44927379252`, as recorded in `BUILD_STATUS.md`).
- Integration branch CI: PASS on exact head `2a5a7a76ea845f000792233bf0a8fb060d165687` (Actions run `44998474952`, run #1147). Source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, package/version identity, CI signature verification and emulator install-over-existing app-private data preservation all passed. The run used the checked-out integration-branch source; isolated final signing and release artifact publication were skipped.
- Local Android build: NOT RUN; exact-head GitHub Actions build/test gates passed instead.
- Device acceptance: PENDING.

## Next implementation priorities
1. **Completed 22 November:** exact integration head `2a5a7a76ea845f000792233bf0a8fb060d165687` passed Actions run `44998474952` (run #1147); source, runner, unit, lint, APK assembly/identity and emulator install-over-existing gates passed. Isolated release signing and artifact publication were skipped.
2. **Completed source + CI validation:** verified Studio file export on integration head `060853bd8e2ff07cb1445980430461e048067e7f`; Actions run #1149 passed source checks, remote-runner tests, Android unit tests, lint, debug APK assembly, signature/identity checks and emulator install-over-existing data preservation. Physical-device provider acceptance remains pending.
3. Connect the durable run lifecycle to cross-runtime Live state and interruption reconciliation.
4. Add generic browser surface actions only after the shared approval, evidence and postcondition boundaries are satisfied.
5. Preserve the full Work/Studio/voice/local-model/social requirements; do not mark the product complete from these foundations.
