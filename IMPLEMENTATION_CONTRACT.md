# Memo 1 — Product implementation contract

**Canonical implementation target:** `ysfclc/memo` (Memo 1)  
**Blueprint only:** `ysfclc/memo-2` — especially `docs/BUILD_PLAN.md`, `docs/TASKS.md`, `docs/FEATURES.md`, `docs/ARCHITECTURE.md`, and `docs/PROGRESS.md`.  
**Canonical package name:** `Memo-Android-Repository.zip`  
**Contract status:** active; no product milestone is declared complete by this document alone.

## 1. The assignment

Implement the same ambitious product defined by Memo 2 in the Memo 1 codebase, using improved knowledge of Yusuf's projects, preferences, constraints, and working context to make the product more useful. Memo 2 is the product blueprint; Memo 1 remains the sole implementation target. This is an implementation and reconciliation job, not a request to copy Memo 2's repository, downgrade scope, or make a demo that merely builds.

Memo is a personal execution environment: it captures intent, maintains durable and source-grounded context, composes the capabilities needed for a task, executes work through controlled tools, presents real artifacts and live state, requests approval for consequential actions, and records evidence so work can safely resume.

## 2. Source-of-truth hierarchy

When documents disagree, apply this order and record the resolution in `IMPLEMENTATION_LOG.md`:

1. The user's explicit product intent and safety constraints.
2. This contract (`IMPLEMENTATION_CONTRACT.md`).
3. `TASKS.md` — ordered task-level execution plan and acceptance gates.
4. `MEMO2_ALIGNMENT_MATRIX.md` — scope crosswalk to the blueprint.
5. `PRODUCT_GAP_REGISTRY.md`, `FEATURE_AUDIT.md`, and `MEMO-PATCH1-AUDIT.md` — Memo 1-specific baseline evidence and gap hypotheses.
6. `README.md`, `BUILD_STATUS.md`, and old run summaries — supporting information, never proof that current code meets requirements.

Memo 2 checkboxes, test results, commits, and green builds are **not** evidence of Memo 1 completion.

## 3. Non-negotiable operating rules

- Work only in `ysfclc/memo`. Never commit to, merge, or modify `ysfclc/memo-2` for this assignment.
- Reconcile the live canonical ZIP and repository before changing implementation. Keep an untouched baseline and compare paths/content before replacing files.
- Preserve Memo 1's identity, application ID, current useful behavior, user data, vault keys/aliases, storage paths, schema/migration compatibility, and signing assumptions unless a reviewed migration explicitly changes them.
- No silent scope reduction. Missing or infeasible work remains open with a concrete blocker and next action; no fake buttons, placeholder success, simulated progress, or undocumented substitutes.
- Deterministic code owns policy, workflow state, validation, permissions, approval binding, routing constraints, retry limits, cancellation, audit and postcondition checks. Models may propose; they cannot bypass these boundaries.
- Every consequential external/destructive action is gated by approval bound to the exact artifact/payload, destination and action envelope. Any change invalidates approval. Uncertain outcomes go to review; never blindly replay a commit.
- Automation stays within the declared product boundary. Any optional remote execution is separately configured, opt-in, authenticated, visible, audited and approved; it is not a substitute for core Android capability.
- No secrets, cookies, decrypted vault data, private prompt contents or personal data in logs, screenshots, test fixtures or build artifacts.
- Keep one implementation per capability behind stable interfaces. No duplicate competing stores, runtimes, activities or services to work around architecture.
- Keep user context evidence-based and editable. Never fabricate personal facts, infer consent, or silently send private context to external models.
- Optimize for the user's real workflow: low-budget/local-first operation where feasible, practical project follow-through, clear state, low-friction capture, and no unnecessary manual orchestration. External inference and remote execution require explicit, understandable controls.

## 4. Implementation order and hard gates

### Gate G0 — Source identity and baseline (must pass before feature work)

- Fetch the current Memo 1 root ZIP from the canonical branch; record repository commit, ZIP blob SHA, file size, SHA-256, entry count and ZIP integrity result.
- Inventory all files, modules, manifests, entry points, services, persistence locations, model runtimes, automation channels, tests, workflows and docs.
- Compare the live archive against the candidate ZIP and any relevant source snapshot. Explain every material difference.
- Record application ID, vault format/key aliases, persisted paths, schemas, migrations, backup formats and upgrade behavior. Add migration/rollback tests before compatibility-sensitive edits.
- Run CI against the exact untouched baseline; record failures separately from missing product capabilities.
- Produce a requirement-to-source/test/evidence ledger.

**G0 exit evidence:** source identity and baseline report; inventory/diff; storage compatibility note; baseline CI URL/commit; open-gap ledger. No implementation rewrite before this gate.

### Gate G1 — Foundation and safety (M0)

Establish deterministic core contracts, versioned domain data, workflow lifecycle/recovery, policy, routing, hash-bound approvals, outbound redaction/audit, kill switch, encrypted persistence and migration safety. Side effects must be refused if the gate, audit durability, target or policy cannot be verified.

**Exit:** relevant core tests pass; storage/migration/recovery tests pass; independent review has no unresolved critical/high findings; current Memo 1 CI builds an installable APK.

### Gate G2 — Local AI, model hub, chat and voice (M1–M2)

Deliver real model discovery/install/load/inference, streaming and cancellation; robust chat history; local speech input/output; model/runtime capability checks; honest unsupported/error states; voice entry points that comply with Android lifecycle/privacy limits. A model downloading is not the same as a model running.

**Exit:** on a supported device, install or import a compatible model, load it, stream a response, stop generation, restart the app and recover history. Voice acceptance separately proves real ASR/TTS operation and interruption behavior.

### Gate G3 — Orchestration, context, artifacts and daily use (M3)

Dynamically translate intent into typed bounded plans; retrieve only relevant, permitted context with provenance; render populated artifacts before approval; provide live state, CRUD, workflow editing, reminders, capture, sources/search/memory, language support, user preferences and everyday workflows. The UI must reflect real runtime state.

**Exit:** at least one end-to-end personal workflow is exercised with source evidence, durable state, real artifact, explicit approval where required, and recoverable execution.

### Gate G4 — Capability compiler and Self Model (M3.8–M3.9)

Build reusable versioned capabilities from task requirements rather than fixed per-use-case scripts. Maintain a user-controlled, source-grounded Self Model and Artifact Studio; tailored outputs must distinguish known facts, inferences and missing data. Never fabricate CV or profile facts.

**Exit:** capability selection/versioning and a source-grounded personalized artifact pass tests and device acceptance.

### Gate G5 — Browser/app automation, Council and live execution (M4)

Implement in-Memo browser sessions, controlled frontier consultations, optional native-app automation, target discovery, element selection/recording, evidence-based verification, live session view, pause/stop/takeover, and role-based Council with claim/evidence tracking. The real runtime—not a mock—must execute the visible workflow. Login, CAPTCHA, OTP, password, payment and unexpected UI changes hand control back to the user.

**Exit:** fixture-based tests prove both successful and refused actions; instrumented/device checks demonstrate live state, approval binding, stop/takeover and postcondition verification.

### Gate G6 — End-to-end product use cases (M5)

Complete brainstorm/project creation, CRM capture/enrichment, job discovery/application with exact submission approval, content draft/publish/measure, and generic bounded plans. Each flow produces durable records and evidence; every publish/send/submit action has a final approval gate.

**Exit:** all five use cases have named end-to-end tests or explicitly documented device-only acceptance with evidence. No flow is marked done merely because its screen exists.

### Gate G7 — Hardening and release (M6)

Close data recovery, encrypted backup/restore, app lock, permission centre, diagnostics, accessibility, test coverage, strict lint, release signing and performance. Verify provenance of the artifact and run a full independent review.

**Exit:** green CI for exact release commit, downloadable APK artifact with SHA-256, all P0/high findings closed, release checklist complete, and honest device-acceptance report. A device-only item must not be labelled device-verified until the user or recorded emulator test confirms it.

## 5. Mandatory task protocol

For each task in `TASKS.md`:

1. State task ID, requirement, dependencies, files owned and acceptance criteria before editing.
2. Read the relevant Memo 1 implementation and blueprint section.
3. Make the smallest coherent implementation; add regression tests and fixtures.
4. Run targeted tests, core tests, Android unit tests, lint and build checks appropriate to the change.
5. Review the full diff, especially data loss, policy bypass, privacy, concurrency, cancellation, audit and error handling.
6. Run the exact GitHub Actions workflow on the exact commit. Record URL/run ID, commit SHA, job outcomes and artifact hash.
7. Update `TASKS.md`, `FEATURE_AUDIT.md`, `PRODUCT_GAP_REGISTRY.md`, `BUILD_STATUS.md` and `IMPLEMENTATION_LOG.md` as applicable.
8. Mark `[x]` only with required evidence; otherwise use `[~]` or `[ ]` and name the blocker/next action.
9. At each milestone, run an independent architecture/security review and re-run integration CI after fixes.

Work may be bundled by coherent subsystem; do not spawn an agent per tiny task. Dependencies and acceptance criteria remain per task. Do not merge unverified work just to keep the branch green.

## 6. Definition of done

A feature is done only when all applicable conditions hold:

- Real code path is wired into the product and reachable from its actual UI/runtime.
- No fake data, placeholder behavior or swallowed failure is mistaken for success.
- Tests cover expected success, failure, cancellation, stale state and security-sensitive refusal paths.
- Relevant migration/data-preservation checks pass.
- The exact source commit passes the required CI and build; artifacts are identified by hash.
- Device behavior is separately labelled as verified, pending or not applicable.
- Documentation and the requirement ledger agree with observed code and test evidence.

The product is shipped only when G7 passes. A green compile is a build checkpoint, not product completion.
