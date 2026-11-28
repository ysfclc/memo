# 2026-11-28 — Durable, capability-driven Work DAG execution

## Purpose

Turn a validated, model-proposed `WorkflowGraph` into real, inspectable work. This is a general execution substrate: it must not recognise task domains, example phrases, keywords, or preselected workflows. A graph may contain unfamiliar combinations of registered runtimes and capabilities.

This checkpoint is an architecture and acceptance contract. It does not claim that DAG execution exists yet.

## Source audit: current boundary

- `WorkflowTaskProposalParser` and `WorkflowGraphValidator` already accept bounded, dependency-validated `TaskSpec` graphs.
- `WorkflowGraphValidator.topologicalOrder` provides a deterministic topological ordering, but there is no persisted per-node execution lifecycle or general scheduler.
- `WorkJobRecord` stores `workflowGraphJson` and a separate sequential `steps` list. The latter is not a faithful representation of DAG dependencies and must not be treated as the graph's execution journal.
- `AgentExecutionEngine` already provides the shared model/tool loop, registered capability adapters, budgets, approval callbacks, durable event integration at the host, cancellation handling and postcondition hooks. Reuse it for eligible model-directed nodes; do not create a second agent engine.
- `AgentRunRecord` is conversation/run scoped, not a durable per-Work-node invocation ledger.
- Current restart recovery moves an interrupted Work checkpoint to review and avoids automatic replay. Preserve that fail-closed behavior.

## Required architecture

### 1. One persisted graph-run record

Persist execution state alongside the owning Work job in the encrypted workspace. The record must bind:

- Work job ID and immutable graph digest
- exact source conversation and source USER message IDs
- graph schema version and runtime capability-manifest digest
- run ID, created/updated timestamps, cancellation state and aggregate budget counters
- ordered node-run records and an append-only transition history (or an equivalently tamper-evident event journal)

Do not infer node state from UI checkpoint titles, list order, or a reconstructed planner template. Existing `JobStepRecord` remains a human-facing checkpoint surface, not the authoritative DAG journal.

### 2. Per-node durable invocation lifecycle

Each node record binds the graph node ID, its validated capability set, a Memo-generated invocation ID, the graph idempotency key, attempt number, state, timestamps, bounded input/output references, evidence references, postcondition result, and safe failure/recovery reason.

Allowed states:

`BLOCKED` → `READY` → `PREPARED` → `RUNNING` → one of `SUCCEEDED`, `FAILED`, `WAITING_FOR_APPROVAL`, `NEEDS_REVIEW`, `CANCELLED`, `SKIPPED`.

Transitions are validated by a pure reducer. Terminal states cannot be overwritten by late callbacks. A node is READY only when every dependency is SUCCEEDED and the graph/run is not cancelled or blocked by a failed/uncertain dependency.

Persist PREPARED (including invocation ID, exact capability scope, input reference and idempotency envelope) before dispatch. Persist RUNNING before invoking any adapter. Persist the verified output/evidence and SUCCEEDED only after its postcondition passes and encrypted read-back confirms the transition.

### 3. Generic runtime dispatch

Resolve a node through a registry of installed executors/adapters using its exact participant, runtime ID and required capability IDs. Do not switch on task title, goal text, domain, or example phrases.

- A capability name on a different runtime is never interchangeable.
- Missing, unavailable, version-mismatched or unverified capabilities stop that node before invocation.
- A model-directed node reuses `AgentExecutionEngine` with only the exact adapters allowed by that node and its declared budget.
- A deterministic/runtime node uses its registered adapter contract; it must not be silently routed through a model.
- User/approval nodes remain user-controlled. An approval node is not an executable write capability.
- Unknown participant/runtime combinations fail closed and remain inspectable.

### 4. Approval and side-effect boundaries

No executor may broaden a TaskSpec's capability set or infer approval from graph structure alone. Bind any approval to the exact runtime, capability, destination, payload hash, node ID and invocation ID. Any payload or destination change invalidates the approval. External writes are never automatically retried.

### 5. Recovery, retry and cancellation

On process restart, reconcile each nonterminal invocation using an explicit adapter recovery contract:

- Confirmed completion: verify the exact invocation and postcondition, then settle it.
- Confirmed not-started: it may return to READY only if the adapter proves no side effect occurred.
- Unknown/ambiguous outcome: NEEDS_REVIEW; never replay blindly.
- A started write or destructive action with unknown outcome: NEEDS_REVIEW, no retry.
- Retries are bounded by graph/node budgets and adapter-declared idempotency/recovery guarantees. A model-proposed `maxRetries` alone is not permission to retry.
- Cancellation prevents new dispatch immediately; in-flight work is cancelled cooperatively and settled as known-complete, known-cancelled, or NEEDS_REVIEW.

### 6. Scheduler behavior

The scheduler repeatedly:
1. validates graph digest and current capability readiness;
2. derives READY nodes from persisted dependency states;
3. chooses only nodes within concurrency, time, model-call and external-write budgets;
4. persists PREPARED/RUNNING before dispatch;
5. collects verified results and commits state transitions;
6. pauses on approval, uncertainty, missing capability, budget exhaustion or cancellation;
7. finishes only when every node is terminal and all required nodes succeeded or were explicitly reviewed.

Concurrency starts at one. Parallel fan-out may be enabled only after journal compare-and-set semantics and cancellation/recovery are proven. No duplicate invocation may be started by concurrent UI taps, process recreation, or repeated scheduler wakeups.

## Acceptance criteria (must be generic, not task-demo driven)

- Pure reducer tests cover every legal/illegal transition, terminal immutability, stale callbacks and invalid event ordering.
- Graph scheduler tests use generated graphs with varied node counts, fan-out/fan-in, dependency depth and capability/runtime combinations. They do not contain special-case task phrases or domain labels.
- A node cannot start until all dependencies are SUCCEEDED; a failed/uncertain dependency blocks all descendants.
- Every dispatch has a previously persisted invocation ID and exact capability allowlist.
- A capability mismatch, unavailable adapter, malformed node output, failed postcondition, budget overrun or vault read-back failure cannot produce SUCCEEDED.
- Duplicate start requests produce at most one invocation.
- Process-death tests cover interruption before dispatch, after PREPARED, after RUNNING, during read-only work, during a local write, during approval, and after output persistence but before node settlement.
- Recovery never blindly repeats an invocation with an uncertain side-effect outcome.
- Cancellation, pause/resume and approval survive process death and navigation.
- Export/import round-trips the journal; legacy schema imports remain backward-compatible; tampered or unsupported journal records fail closed without destroying the user's existing data.
- Integration tests prove the same shared `AgentExecutionEngine` is used for model nodes and that no parallel Work-specific agent loop is introduced.
- Exact-head CI includes JVM/core tests, Android unit tests, lint, debug/release assembly, APK identity/signature checks and install-over-existing/data-preservation. Physical-device acceptance remains a separate gate.

## Implementation sequence

1. **Persistence contract:** introduce versioned per-node invocation records and a pure transition reducer; add encrypted backup/import round-trip and interrupted-run recovery tests. Do not dispatch nodes yet.
2. **Scheduler core:** implement dependency readiness, budget accounting, single-flight start and cancellation as a pure/testable coordinator over a persistence interface.
3. **Runtime adapter bridge:** register exact-runtime executors and run only one low-risk capability node through its existing adapter contract; reuse the shared agent engine for model nodes.
4. **Durable orchestration:** persist before/after each invocation, verify postconditions, recover without blind replay, and expose exact progress/evidence in Work.
5. **Expand capability coverage:** add runtime types only when their adapters, safety policies, evidence and recovery contracts are genuinely implemented.

A phase is not complete because a plan can be displayed or a unit test can simulate a successful result. Real adapter execution and durable evidence are required before claiming that Memo performed the work.
