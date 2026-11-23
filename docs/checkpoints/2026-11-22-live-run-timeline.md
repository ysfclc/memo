# Memo Live run timeline checkpoint — 22 November 2026

## Baseline
- Repository: `ysfclc/memo` (Memo 1; Memo-2 excluded)
- Branch: `build/product-core-integration-2026-11-21`
- Parent commit: `a9de80ef22b4ab2b6939d68db32b7a36c9ee316a`
- Work package: M3.B2-S1
- Requirements: MEMO-LIVE-001, MEMO-ORCH-001, MEMO-RECOVERY-001

## Implemented slice
- Live correlates an `AgentRunRecord` to its originating `ConversationRecord`.
- Only messages with `sourceType = AGENT_EVENT` and a final metadata line whose first field is the exact `Run ID: <id>` value are included; additional metadata fields remain allowed after the exact ID separator.
- The projection sorts by the message's persisted `createdAt` timestamp; the UI shows the most recent four event records with their saved summary, status, call and evidence metadata.
- Run start/update times, final response and latest evidence reference are also visible.
- The existing Talk conversation remains the full event transcript. No second event store or journal was added.

## Compatibility and safety
- Read-only projection over the existing encrypted workspace snapshot.
- No workspace/backup schema, encrypted-store path, encryption format, Keystore alias, application ID, version code policy or signing configuration change.
- Event content is rendered as plain text; it is not interpreted as commands or links.
- Full cross-runtime Live control, pause/resume/takeover and the global stop/action gate remain incomplete.

## Tests
- Exact run-ID correlation and isolation from other runs, including combined metadata fields.
- Ignore non-`AGENT_EVENT` messages, substring matches and run IDs appearing outside the final metadata line.
- Chronological ordering and missing-conversation behavior.
- Polish and Turkish translations for new Live labels.

## Verification status
- Source/test changes are prepared for the existing integration PR branch.
- Exact-head GitHub Actions result: pending at checkpoint creation.
- Local Gradle/Android SDK build: not run in this environment.
- Physical-device acceptance: pending.
- No signed release APK is produced or claimed by this slice.

## Next
1. Inspect exact-head Actions results and repair any failures.
2. Add cross-runtime event projection only through the existing shared run/event lifecycle; do not create a competing journal.
3. Add explicit pause/resume/takeover and global stop only after shared lifecycle ownership and safety boundaries are defined and tested.
