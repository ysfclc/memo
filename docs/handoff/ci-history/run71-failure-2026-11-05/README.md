# CI run 71 — root cause and source correction

- Repository: `ysfclc/memo`
- Branch: `ysfclc-patch-1`
- Workflow run: 71 (`41964174031`)
- Run URL: https://github.com/ysfclc/memo/actions/runs/41964174031
- Upload commit: `ac6b2069ad768362ca05ecc0a83eea20e5ff1f98`
- Run checkout SHA: `758475305ee77506b9ae2138b0b8d593bb47b912`
- Uploaded `incoming/memo.zip` SHA-256: `1467ee49a95840d4cd65fc82bae6cc8259f9dc89f7c7d2077dee0730c362b6f9`

## Passed before the failure

- Single-ZIP preflight: passed. The old second ZIP had been removed after run 70.
- ZIP validation and safe extraction: passed.
- Exact source-tree replacement: passed.
- Source checksum refresh: passed.
- `tools/verify_source.py`: passed.
- Remote runner tests: 12/12 passed.
- Kotlin compilation for `:core` and `:core:compileTestKotlin`: passed.

## Failure

Gradle task `:core:test` reported 52 tests, 1 failure:

`CapabilityCompilerTest.optionalRuntimeIsSelectedButNeverPretendedToBeEnabled`

At the assertion checking that the Android accessibility runtime was selected, the list was empty. The intent text was `Automate a sequence in my Android app`.

## Root cause

`IntentPlanner.classify()` evaluated the generic `app` build keyword before the explicit `automate` automation keyword. The goal was therefore routed to `BUILD`, so `CapabilityCompiler` selected the build route rather than `runtime.android.accessibility`.

This is an intent-classification precedence bug, not a Gradle, ZIP, source-integrity, runner, or Kotlin compilation error.

## Correction included in this source snapshot

- Explicit automation verbs/action language now run before generic build/app keyword matching.
- Broad `browser` and `workflow` markers stay after the BUILD rule, so requests to implement a browser/workflow editor remain BUILD tasks.
- The existing failing test now asserts the expected `AUTOMATION` domain, the registered `android.launch_app` capability, and `OPTIONAL_PERMISSION` runtime status.
- A second regression test verifies that `Build the Android app's workflow editor` remains `BUILD`.

## Local verification of the correction

An isolated Kotlin/JVM smoke harness compiled the actual `IntentPlanner.kt`, `CapabilityCompiler.kt` and `RuntimeContracts.kt` with Kotlin 1.9 and passed all three assertions:

1. Explicit automation request routes to `AUTOMATION`.
2. Android accessibility capabilities are selected but remain `OPTIONAL_PERMISSION`.
3. A build request for a workflow editor remains `BUILD`.

The full Gradle test suite, app unit tests, lint and APK assembly have not yet been rerun after this correction. CI run 72 or later must confirm those gates before the fix is considered integrated/verified.
