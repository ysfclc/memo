# Session checkpoint — 20 November 2026 — execution architecture constraints

**Repository:** `ysfclc/memo`  
**Branch:** `governance/agent-execution-invariants-2026-11-20`  
**Baseline:** `main` at `c307f94f5d296622e9e3b2fa92b02fe9c58361f2`  
**Status:** Governance constraints established; product implementation not started by this checkpoint.

## User-approved direction

The user gives Memo an outcome in ordinary language. Memo chooses the appropriate intelligence provider and real capabilities, executes through one shared lifecycle, observes the results, verifies them and continues/replans as needed. Local and frontier models are intelligence providers; browser, Android, code, file, image, PDF, video and workspace operations are separate capabilities. Examples are tests of the common engine, never blueprints for hardcoded features.

## Binding repository records

- `AGENTS.md` — contributor hard stops and mandatory reading order.
- `docs/AI_EXECUTION_INVARIANTS.md` — complete binding architecture rules and cross-cutting acceptance suite.
- `docs/MEMO_PRODUCT_CONTRACT.md` — product-level architecture lock.
- `DEVELOPMENT_GOVERNANCE.md` and `IMPLEMENTATION_CONTRACT.md` — Fresh Start, work-package and quality gates.
- `TASKS.md` — architecture lock placed before feature work; explicitly not marked implemented.
- `docs/REQUIREMENT_TRACEABILITY.md` — MEMO-AGENT-001, MEMO-CAP-001 and MEMO-ARTIFACT-001.
- `docs/DECISION_LOG.md` — ARCH-001.
- `IMPLEMENTATION_LOG.md` — GOV-ARCH-001.

## Known source baseline — not yet fixed

- Talk sends ordinary messages directly through `AiRuntimeViewModel.runPrompt()`.
- `IntentPlanner` remains keyword/template based.
- `CapabilityCompiler` builds a manifest but does not dispatch callable implementations.
- `WorkflowGraph` and `WorkflowTaskProposalParser` are proposal/validation foundations, not an executor.
- There is no demonstrated universal tool invocation, observe/replan loop or durable cross-runtime run journal.

Do not build new feature-specific workflows to work around these gaps. The first implementation work package must connect Talk to the shared execution engine and actual capability dispatcher while preserving existing data, permissions and runtime behavior.

## Filesystem, build and device status

- This checkpoint records documentation/governance changes only; no application source or schema was changed.
- No Gradle build, CI run or device acceptance is claimed for this documentation-only work.
- The existing draft browser research PR is not merged by this work.
- Do not merge this branch to `main` without the user's explicit approval.
