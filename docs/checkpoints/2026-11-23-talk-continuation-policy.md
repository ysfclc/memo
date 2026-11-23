# Talk run continuation policy — 23 November 2026

## Scope

This increment adds a pure, deterministic eligibility policy and a user-controlled Live → Talk continuation path. The user reviews the interrupted run in Live, opens the originating conversation with a prefilled editable draft, and explicitly sends it to start a new run. The new run stores an additive `parentRunId`; the prior terminal record is not modified.

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

No previous call ID, idempotency key or approval grant may be reused. A fresh adapter is built for the new run against the current browser session; any consequential action still requires fresh approval.

## Verification

- Added JVM policy tests for safe eligibility, active/terminal states, uncertain tool outcomes, stale approval references and missing source identity.
- Added additive `parentRunId` serialization with backward compatibility for schema-7 backups that predate the field.
- Live exposes Continue in Talk only when the policy allows it; unsafe cases show the blocking reason.
- Talk prepopulates an editable draft, labels it as a continuation, supplies prior run metadata as untrusted context, and requests current capability discovery/fresh approval.
- The ViewModel rechecks eligibility at durable run creation and persists the new run before execution.
- Exact-head Gradle/CI verification is pending. No physical-device acceptance is claimed.

## Next implementation

Add ViewModel-level continuation tests, verify linked-run import/export and process-death behavior on the emulator, and inspect the exact-head CI result. Physical-device acceptance remains separate.
