# Talk-to-Studio artifact slice checkpoint — 21 November 2026

## User outcome
A user can ask Memo in Talk to create a private Markdown deliverable, have the shared execution engine save it into the encrypted workspace, then open it from Studio to edit, inspect revision history, copy or export it.

## Implementation
- Capability: `workspace.save_artifact`, registered under `app.workspace` and exposed through `MemoWorkspaceAgentTools`.
- Contract: `StudioArtifactPolicy` validates a bounded title, descriptive artifact type and Markdown body; artifact type is metadata, not a workflow selector.
- Persistence: `EvidenceRecord(kind = "STUDIO_ARTIFACT")` with source conversation/message IDs, source excerpt digest, content digest, idempotency/run/call metadata, version and DRAFT review state.
- Verification: the adapter's postcondition reads back the exact artifact, content, digest, type, version and source provenance. The shared engine will not count the write as successful without this verifier.
- UI: Studio lists both Self Model artifacts and Talk-created Studio drafts. The existing Evidence dialog opens the artifact for edit/revision history, copy and Markdown export. User edits recompute the digest and increment the version; source captures remain immutable.

## Tests and guards
- `StudioArtifactPolicyTest`: descriptive-type normalization, stable digest, digest binding to title/type/body, field bounds and generic types.
- `AgentCapabilityRegistryTest`: the capability is registered as a reversible local workspace write, not an external side effect.
- `tools/verify_source.py`: checks the adapter, registered runtime, postcondition, Studio listing, edit allowlist and digest/version refresh.

## Scope boundary
- Implemented in this slice: private Markdown text artifact only.
- Not implemented: filesystem code execution, PDF/image/video generation, multi-modal rendering, public-safe review, channel connectors, external publishing/submission or universal artifact revision compare/restore.

## Verification
- Upstream postcondition integration exact-head CI: PASS, run `44907881554`.
- PR #20 integration CI: run `44952865406` was still in progress at the start of this slice; the updated head requires a fresh exact-head CI run.
- Physical-device acceptance: PENDING.
- No release APK is claimed.

## CI follow-up — 21 November 2026

The artifact-capable source head `ae6ef31976d7f35339715a16933ec73e21210af5` passed exact-head CI run `44953421742`: source verification, remote runner, core/app unit tests, lint, debug APK assembly, package/version identity, signature verification and emulator install-over-existing data preservation. Release signing was skipped and physical-device acceptance remains pending. This run predates the later browser prompt/response adapters.
