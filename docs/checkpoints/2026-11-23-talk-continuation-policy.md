# Talk run continuation policy — 23 November 2026

## Scope

This increment adds a pure, deterministic eligibility policy for continuing an interrupted Talk run as a new run. It does not resume or replay the old run and does not yet expose a user-facing Continue action.

## Policy

A run is eligible only when:

- its durable status is `NEEDS_REVIEW`;
- its originating conversation and goal still exist in the record;
- it has no pending approval-call reference;
- every capability start recorded in the run has a corresponding successful completion.

A run is blocked when a capability may have started without a verified completion. This is intentionally conservative: the current run summary does not contain enough per-invocation detail to prove whether an unsuccessful tool was read-only or externally consequential.

The policy's positive result is not an execution grant. A future continuation must still:

1. show the saved run, transcript, evidence references and recovery reason;
2. require an explicit user action;
3. create a new run ID linked to the prior run;
4. pass only persisted observations as untrusted context;
5. rediscover current capabilities and re-evaluate permissions;
6. obtain fresh approval for any external side effect;
7. retain both run histories and never mutate the old terminal record.

No previous call ID, idempotency key, browser scope or approval grant may be reused.

## Verification

- Added JVM policy tests for safe eligibility, active/terminal states, uncertain tool outcomes, stale approval references and missing source identity.
- Full Gradle/CI verification is pending for this branch.
- No UI integration, ViewModel continuation launcher, process-death emulator test or physical-device acceptance is claimed.

## Next implementation

Integrate the policy into Live's durable run inspection, add explicit user review and a new-run continuation launcher in the ViewModel, persist the parent-run relationship additively, and test that old run history and approval records remain unchanged.
