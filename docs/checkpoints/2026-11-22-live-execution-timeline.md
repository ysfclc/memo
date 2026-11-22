# Memo M3.B2-S1 — Talk execution event journal and Live timeline

Date: 2026-11-22

## Branch and baseline
- Feature branch: `feature/live-execution-timeline-2026-11-22`
- Baseline: integration PR #20 head `25decfbebc97461a0e990e5b97b696f085e17995`
- Product requirement: `MEMO-LIVE-001` — one observable execution control room.

## Scope
Implement the first vertical slice of M3.B2: durable, structured Talk-run events and an expandable chronological timeline in Live. This is not yet the complete cross-runtime control room.

## Implementation
- `AgentRunRecord` stores a bounded event journal (up to 48 events per run) in the same encrypted workspace snapshot.
- Events preserve type/status, sequence, timestamp, runtime ID, capability ID/version, call ID, display target, declared capability purpose, safe result summary and evidence reference.
- The engine emits runtime/target/purpose/result context for capability lifecycle events. Targets are display titles or capability IDs; raw tool arguments and secret payloads are not copied into the timeline.
- The ViewModel commits event journal updates with the run reducer and transcript record, then verifies the stored event read-back before continuing execution.
- Run creation, final-result persistence, interrupted closeout and app-restart recovery are represented explicitly. Startup and both backup-import paths read back the encrypted recovery snapshot before unlocking/confirming restore. Restart recovery records `RUN_RECOVERED_NEEDS_REVIEW`; no interrupted action is replayed.
- Live exposes an expandable per-run timeline with state, timestamp, runtime, action, target, purpose, result and evidence.
- Backup schema is now v8. Schema-v7 run records remain importable with an empty event list; Memo does not fabricate historical events for old runs.
- The displayed purpose is the capability's declared user-safe description, not hidden model chain-of-thought.

## Verification added
- Core lifecycle tests cover event ordering, event caps and run lifecycle transitions.
- Schema tests cover event-journal round-trip, schema-v7 import compatibility and restart recovery without duplicate recovery events.
- Engine tests cover runtime, target, purpose and result metadata for normal and approved actions.
- Transcript tests cover structured metadata and Polish labels.
- Offline source verification guards the schema-v8 journal, ViewModel persistence path, Live timeline and localization.

## Acceptance still required
- Exact-head Android CI on this feature branch.
- Physical-device check: expand a completed run and verify chronological event state/time/runtime/action/purpose/result/evidence.
- Physical-device interruption check: force-stop during an active run, reopen Memo, verify a review event is added and no action is replayed.
- The full M3.B2 scope remains open: independent browser/app session streams, unified cross-runtime chronology, pause/resume/takeover, universal action gate and global stop acceptance.

## Next
1. Run exact-head CI and fix any failures without weakening the approval, postcondition, encrypted persistence or no-replay guarantees.
2. Complete the physical-device timeline and restart checks.
3. Extend the same event contract to independent runtime sessions and user-controlled pause/resume/takeover.
