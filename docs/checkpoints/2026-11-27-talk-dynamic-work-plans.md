# 2026-11-27 — Talk dynamic Work plan creation

## Product outcome
A user can describe an outcome in Talk and Memo can turn it into a model-composed, typed, dependency-aware Work plan saved in the encrypted workspace. The plan is editable/reviewable in Work and retains its source conversation and exact user message.

## Implementation
- Routed every new home intent into Talk; removed the keyword-based `IntentPlanner.shouldCreateWorkflow` shortcut so action-oriented requests are not diverted into a deterministic template planner before the model can decide what to do.
- Added `workspace.create_work_plan` to the registered Memo workspace runtime and Talk capability catalogue.
- The model supplies a bounded TaskSpec proposal; `WorkflowTaskProposalParser` validates the graph against exact registered runtimes/capabilities, dependency structure, budgets, approval gates and external-side-effect policy.
- `AgentWorkPlanPolicy` converts the validated graph into a durable `WorkJobRecord` with topologically ordered, pending checkpoints and explicit participant, runtime, capability, input, output and postcondition details.
- The ViewModel requires the source conversation and exact USER message to exist, persists the plan to the encrypted workspace and reads back the graph, source, steps and DRAFT status.
- The Talk adapter returns the Work item ID as its evidence reference and implements the shared postcondition verifier. The engine cannot count the tool call as successful unless the exact saved graph and DRAFT state are read back.
- No task node is executed by this capability. External actions remain review-only and retain their approval boundaries.

## Tests and guards
- Added pure-core policy tests for dependency order, source binding, pending-only state, DRAFT state and approval/external-action checkpoint preservation.
- Extended `tools/verify_source.py` to require the runtime registration, Talk adapter, exact encrypted-workspace read-back and policy tests.
- Existing `WorkflowTaskProposalParserTest` continues to cover dynamic DAG validation, exact-runtime capability matching, approval gates, budgets, cycles and malformed proposals.

## Compatibility
- No workspace schema, backup format, encryption/key alias, package identity, signing configuration or external provider behavior changed.
- This is additive use of the existing `WorkJobRecord.workflowGraphJson` field and existing encrypted workspace store.

## Verification status
- Source changes are on `feature/talk-dynamic-work-plans-2026-11-27`, based on current `main`.
- Exact-head GitHub Actions validation is pending.
- No local Gradle/Android SDK execution or physical-device acceptance is claimed.
- This increment creates a dynamic Work plan; it does not yet execute the DAG. Existing Talk's iterative tool loop remains the execution path for capabilities already exposed there.
