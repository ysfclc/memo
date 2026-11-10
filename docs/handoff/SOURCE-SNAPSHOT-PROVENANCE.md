# Memo source snapshot provenance and current handoff state

**Prepared:** 5 November 2026  
**Repository target:** `ysfclc/memo`  
**Branch target:** `ysfclc-patch-1`  
**Canonical future upload:** `incoming/memo.zip`

## Source included in this ZIP

The Android source tree in this package was recovered from the locally available `Memo-Android-Repository(9).zip` artifact, dated 28 October 2026 at approximately 15:10 UTC. Its SHA-256 is:

`c553b1bc4d3cb815f47d827c3769b56180647e0f8c96cf424f4708d3f6aea8f2`

This is a complete Memo Android Gradle project tree with the `app`, platform-independent `core`, `remote-runner`, `tools`, Gradle build files, task ledger, feature audit, product-gap registry, architecture/implementation notes and device acceptance checklist. It is **not** represented as the exact latest Patch1 source binary.

## Live branch evidence checked on 5 November 2026

- Current Patch1 head at inspection: `05fb6b8607203b7c8269e42568249c1704cd15c5`.
- The branch still contained several legacy archives and two ZIPs under `incoming/`; the new workflow intentionally removes the need for a root-level baseline archive and accepts only `incoming/memo.zip`.
- Last recent successful Android build: Actions run #54, 29 October 2026, commit `1dc1f9436f2250ec1d7b26f146288c58efc337d3`. The retained reports show 98 JVM tests, zero failures/errors/skips, lint with zero errors, and a debug APK artifact.
- Latest inspected Android build: Actions run #68, 3 November 2026, commit `05fb6b8607203b7c8269e42568249c1704cd15c5`. ZIP import, source-manifest checks, source invariants and remote-runner tests passed; Kotlin compilation then failed with 101 diagnostics in the workflow-engine/data-model integration area.

The newest live `incoming/memo.zip` binary could not be retrieved through the available read-only GitHub connection. GitHub write/export workflow creation was denied. Therefore this handoff does not silently mix the failing 3 November source with the older recoverable project snapshot. The full ledger and CI evidence are included below `docs/handoff/` so the next source reconciliation has the concrete branch/run identifiers and diagnostics.

## Live source import and latest regression

- This consolidated project was uploaded as `incoming/memo.zip` in commit `ac6b2069ad768362ca05ecc0a83eea20e5ff1f98`, with SHA-256 `1467ee49a95840d4cd65fc82bae6cc8259f9dc89f7c7d2077dee0730c362b6f9`.
- GitHub Actions run #71 (`41964174031`) passed single-ZIP preflight, archive validation/extraction, full snapshot sync, source checksum refresh, `tools/verify_source.py`, and remote runner tests (12/12).
- Kotlin source and test compilation passed. The `:core:test` stage ran 52 tests and failed one capability-classification regression. The exact root cause and retained test report are in `docs/handoff/ci-history/run71-failure-2026-11-05/`.
- The failure was corrected in this snapshot: explicit automation verbs now outrank the generic `app` BUILD keyword, while a build request for a workflow editor remains BUILD. An isolated Kotlin/JVM smoke harness compiled the actual planner/compiler/runtime contracts and passed the three relevant behavior checks.
- Full Gradle tests, Android app unit tests, lint, and APK assembly have **not** yet passed on the corrected source. The source correction is prepared; another GitHub Actions run must verify it before it is treated as integrated/green.

## Next source-of-truth action

Upload this corrected full project snapshot as `incoming/memo.zip` on `ysfclc-patch-1`. The persistent workflow replaces the project source as an exact snapshot, refreshes `SOURCE_SHA256SUMS.txt` and `BUILD_STATUS.md`, appends a permanent `notes/imports/<upload-commit>.md` record, and commits the integrated source only after the required quality gates pass. Keep any additional ZIP out of `incoming/`; that folder must contain only `memo.zip` (plus `.gitkeep`).
