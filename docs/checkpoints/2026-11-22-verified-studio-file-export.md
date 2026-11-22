# 2026-11-22 — M3.B1-S2 verified Studio file export

## Baseline
- Repository: `ysfclc/memo`
- Integration base: `build/product-core-integration-2026-11-21`
- Base commit: `2a5a7a76ea845f000792233bf0a8fb060d165687`
- Working branch: `feature/verified-studio-file-export-2026-11-22`
- Prior integration CI: Actions run `44998474952` (run #1147), exact base head passed source verification, remote-runner tests, core/app unit tests, lint, debug APK assembly, APK identity/signature checks and emulator install-over-existing data preservation.

## User outcome
A private Markdown artifact created through Talk can be exported to a real file at a location selected by the user, and Memo reports success only after verifying the bytes actually stored at that destination.

## Implementation
- Added `ArtifactExportPolicy` in the pure core module.
- MIME types are allowlisted to text/markdown, text/plain, text/csv and application/json; the current Studio UI uses Markdown only.
- Filenames are sanitized and receive the extension dictated by the selected MIME type.
- UTF-8 content is preserved exactly, with a 1 MiB bound.
- The export payload receives a SHA-256 digest before writing.
- The Studio export flow writes to the Android CreateDocument URI selected by the user, reopens the same URI, streams a bounded read-back and compares SHA-256 in constant time.
- Any write/read failure, oversized destination content or digest mismatch is reported as unverified. No false success message is shown.

## Boundaries
- No arbitrary filesystem path access, background export, app-private plaintext artifact store, new database/schema, or remote upload.
- No claim of general filesystem read/write, code execution, PDF/image/video generation, or publishing.
- Existing encrypted Studio draft storage, source provenance, editing and approval boundaries remain unchanged.

## Tests
- Added pure-core tests for filename sanitization, MIME allowlist, UTF-8 preservation, stable digest, exact read-back, changed/truncated content, invalid digest and size limits.
- Exact-head CI for this feature branch: PENDING.
- Physical-device export through at least one document provider: PENDING.

## Next
1. Open/update the existing integration PR with this branch head and inspect exact-head Actions.
2. Fix any compilation, source-verifier or test failures without weakening the read-back guarantee.
3. Test export through real Android document providers and verify the saved file's bytes/hash.
4. Continue toward general file/code adapters only after this safe artifact boundary is accepted.
