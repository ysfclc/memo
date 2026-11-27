# 2026-11-27 — MEMO-ORCH-001 — Strict persisted Work-graph decoding

## Session identity
- Repository: `ysfclc/memo` (Memo 1, not Memo-2)
- Mainline base: `1602e50f2e2d23b5265362a7641f649aaf06e3c4`
- Working branch: `fix/strict-work-graph-mainline-2026-11-27`
- Scope: persisted WorkflowGraph decoding, TaskSpec proposal envelope and regression coverage

## User outcome
A saved Work graph or imported encrypted workspace backup cannot silently reinterpret malformed fields as a different, apparently valid workflow. Previously stored schema-v1 graphs remain readable under the exact legacy field shape.

## Implementation
- Replaced permissive `optString`, `optBoolean`, `optLong` and silent `.take(...)` truncation with required, typed readers for bounded strings, arrays, enums, booleans and exact integers.
- Require the schema version, goal, full budget object and node array, then run `WorkflowGraphValidator.validate` on the decoded graph.
- Reject mixed presence of `externalSideEffect`. Current-format graphs must carry it for every node.
- Preserve legacy schema-v1 graphs only when every node omits `externalSideEffect`; infer legacy side-effect classification solely from a matching explicit, non-retrying user approval checkpoint.
- Require an exact integer schema version and a text goal in `WorkflowTaskProposalParser`.
- Parse numeric values through `BigDecimal.longValueExact()`, rejecting fractions and values outside the signed 64-bit range rather than using floating-point boundary comparisons.

## Regression coverage
- Overlong fields and arrays, non-string capability entries, wrong boolean types, fractional timeouts, missing budget data and mixed side-effect representation.
- Legacy external-write and consent-only graph round trips.
- Proposal schema version as string, fraction, boolean or missing field; non-text goal; out-of-range timeout values.
- Persisted graph budget overflow.

## Compatibility and limits
- No workspace schema or backup-format migration.
- No encryption envelope, Android Keystore alias, package ID, signing certificate, or external provider behavior changes.
- This change validates the persisted graph format; it does not add DAG execution. Work still executes only the exact local-model checkpoint type. Browser, native-app, remote-model and external-write checkpoints remain blocked/review-only in Work.
- Physical-device acceptance remains pending.

## Verification
- Previous PR #28 head passed run #1293, but it was based on stale main and is not acceptance evidence for this transplant.
- Current mainline branch `fix/strict-work-graph-mainline-2026-11-27`, based on `1602e50f2e2d23b5265362a7641f649aaf06e3c4`, is awaiting exact-head Actions validation.
