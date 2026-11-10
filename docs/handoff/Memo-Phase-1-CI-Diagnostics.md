# Memo — Phase 1 CI Failure Diagnosis

**Date:** 2026-10-30  
**Repository:** `ysfclc/memo`  
**Branch:** `ysfclc-patch-1`  
**Scope:** Diagnose the latest upload/build failures only. No repository code or workflow was changed in this phase.

## Findings

### 1. Latest upload-trigger failures — runs #58 and #59

- **Run #58:** `41055116191`, commit `144d568074187450b18e9a29a149b99ebc72e0ce`.
- **Run #59:** `41059174591`, commit `9950886b6c05abe57f1ca750580035f075b53f94`.
- Both fail in the first job, `Resolve upload revision and prevent duplicate imports`, step `Resolve the uploaded ZIP commit`.
- Exact failure: `This run was not triggered by a supported Memo ZIP upload. Upload incoming/memo.zip.`
- The next job, `Import ZIP, validate, test, lint, build, and publish`, is skipped in both runs. Therefore neither run extracted the archive, ran source validation, ran Gradle, produced an APK, or committed an integrated source update.

**Root cause:** the active root workflow at `.github/workflows/build-apk.yml` resolves push uploads only by parsing `github.event_path` → `commits[]` → `added/modified/removed`. When that event payload does not provide the expected path list, `ARCHIVE_PATH` stays empty. The current workflow has no Git-history fallback, despite checking out the exact triggering SHA with full history.

**Required workflow repair:** retain the event-payload lookup as the first choice, then fall back to the actual Git diff for the triggering revision (and verify the selected path exists in the checked-out tree). Preserve the current `git log` upload-commit resolution and duplicate-import guard. Keep `workflow_dispatch` able to process the existing `incoming/memo.zip` without requiring another upload.

### 2. Last run that actually reached Android Gradle — run #57

- **Run:** `41052805467`, commit `4692ce74bb6520f2d30d0aac7a9070b3f72825a8`.
- Preflight passed and the ZIP was imported and validated.
- Source manifest verification passed; the static verifier reported 32 required integration files; the remote-runner suite passed **12/12** tests.
- Gradle reached `:core:test`, where **52 tests ran and 1 failed**.
- Failing test: `CapabilityCompilerTest.optionalRuntimeIsSelectedButNeverPretendedToBeEnabled`.
- The failure was the automation-intent classification assertion: the request `Automate a sequence in my Android app` was being classified as `BUILD`, because the generic `app` keyword was evaluated before the explicit `automate` keyword.
- The debug APK, signed release step, final artifacts, and integration commit were consequently skipped.

### 3. Current uploaded source archive

- GitHub reports the blob SHA for `incoming/memo.zip` at run #59's branch head as `691ff3111c40b9787726f18883d731bf05ae6c48`.
- The local `/mnt/data/memo.zip` has the same Git blob SHA, confirming it is byte-for-byte the archive currently uploaded in the repository.
- Its `IntentPlanner.kt` now checks explicit automation terms before generic build/app terms.
- `tools/verify_source.py` passes on this archive.
- A local Android Gradle test was not run in this environment (no installed Gradle distribution). CI has also not tested this exact archive yet because runs #58 and #59 stopped in preflight.

## Phase 1 conclusion

There are two distinct blockers:

1. **Workflow blocker:** repair upload-path resolution so runs #58/#59 do not stop before import.
2. **App regression gate:** confirm the newer archive's automation-classification fix by getting the core Gradle tests to run; then continue through lint and APK assembly.

Do not treat either failed run as a failed APK compilation. Neither #58 nor #59 reached Gradle; #57 did reach Gradle and failed one core unit test.

## Next phase

**Phase 2 — workflow repair:** make the smallest safe change to the active root workflow, preserve its importer, ZIP security checks, notes, test/lint/build gates, artifact publication and commit-safety behavior, then validate the YAML and inspect the exact diff before triggering another run.
