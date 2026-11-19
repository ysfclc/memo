# Memo Product-Integrity Audit — 19 November 2026

**Scope:** product-direction drift, user-input loss, assistant interpretation being promoted into requirements, use-case overfitting, AI-generated plan overreach, stale implementation claims and conflicting project authority.

**Audit target:** `ysfclc/memo`, branch `stabilization/usable-stage-2026-11-10`, PR #8.  
**Authority:** This report is evidence/history only. It does not create product requirements. Current user direction and corrections in `docs/USER_INPUT_LEDGER.md`, `AGENTS.md` and `docs/MEMO_PRODUCT_CONTRACT.md` remain authoritative.

## Findings and remediations

### PI-01 — Competing sources of truth could overrule the user's latest direction
**Finding:** `DEVELOPMENT_GOVERNANCE.md`, `IMPLEMENTATION_CONTRACT.md`, `MEMO2_ALIGNMENT_MATRIX.md`, the Product Contract and the dated state records expressed overlapping authority rules. In particular, the implementation contract called Memo 2 the product blueprint and ranked itself above other engineering records. A future agent could treat a historical project as authority to infer new Memo requirements.

**Remediation:** Added mandatory `AGENTS.md`; unified the hierarchy in `DEVELOPMENT_GOVERNANCE.md`; demoted Memo 2 to historical comparison material in the implementation contract, matrix and implementation log; made user input and the accepted Product Contract the governing product authority. Added static CI guards against the old blueprint wording.

**Status:** Corrected in repository records; exact-head CI pending.

### PI-02 — No append-only record for all kinds of user input
**Finding:** The Decision Log captured material decisions, but did not provide a durable classified record for every correction, reported bug, feedback item, or uncommitted idea. Several historical feedback records were spread across baseline/recovery documents and task lists.

**Remediation:** Added `docs/USER_INPUT_LEDGER.md` with stable IDs, original user wording, separately labelled interpretations, scope boundaries, links and a template. It distinguishes CORRECTION, REQUIREMENT/DECISION, BUG_REPORT, FEEDBACK, IDEA (UNCOMMITTED), USE_CASE and CONSTRAINT/NON_GOAL. It preserves the 17 November UWF/WF feedback register by reference rather than silently closing or rewriting those records.

**Integrity control:** `tools/verify_source.py` compares the current ledger and Decision Log against their first-parent versions and fails if committed history is edited or removed. New content may be appended.

**Residual limitation:** No repository check can detect a future user correction that an agent never records. The mandatory agent rules therefore require recording new input before or in the same change as related implementation, and checking the ledger at session start and handoff.

### PI-03 — A use case could become Memo's product identity and roadmap
**Finding:** Client discovery was expanded into a detailed “mission” with outreach drafts, and Sales Operations appeared as a product/domain extension, a scheduled M7 vertical slice and a P1 product gap. This could steer general-purpose architecture toward outbound/sales automation.

**User correction preserved:** “But again, we are not building outbound. This is just a use case. Please keep track of whatever correction I add in memo. Permanent!”

**Remediation:** Reclassified client discovery as one non-defining acceptance scenario. Removed outreach drafts/contacting from that scenario. Reclassified Sales Operations as an optional scenario pack, not a core product definition, default architecture, P1 gap, roadmap priority or implementation mandate. Updated Product Contract, TASKS, README, Roadmap, Product Gap Registry, Traceability, Sales Operations spec, architecture map, Feature Audit and source verifier. Added CI guards against the old M7 vertical-slice and P1 wording.

**Status:** Corrected in current branch records; exact-head CI pending.

### PI-04 — Assistant-proposed architecture could be misattributed as a user decision
**Finding:** The user explicitly stated Capture → Observe → Plan → Execute, asked for AI plus deterministic code and smart routers, and floated “Classify” as a possible refinement. The assistant's expanded eight-stage cycle, exact router list, single-orchestrator/shared-state topology and detailed responsibility split were design proposals, not all separately approved decisions.

**Remediation:** Product Contract, AGENTS and TASKS now separate the four-stage user-stated lifecycle and smart-routing/AI-plus-deterministic direction from the candidate Classify stage, expanded sequence, router labels, shared-state model, topology and responsibility split. Those details are labelled proposals to validate, not fixed user-approved architecture. The user may later adopt, reject or revise them.

**Status:** Corrected in repository records; exact-head CI pending.

### PI-05 — Legacy fixed keyword routing could be mistaken for Memo's canonical smart router
**Finding:** `IntentPlanner` contains a finite keyword/domain taxonomy; `CapabilityCompiler` retains static per-domain routes. This is not a general-purpose, dynamically routed architecture. The old planner is no longer the Talk entry gate, but remains in legacy Work-editor helpers.

**Remediation:** Marked the planner, domain labels and static route map as legacy/prototype compatibility code. Removed the obsolete planner import from the Talk entry shell. Added comments around the Work-specific keyword hint and local-model draft expander that distinguish them from the canonical intent router and preserve independent parser/runtime/allowlist checks. Added source-verifier guards.

**Remaining technical debt:** The legacy Work editor still uses the keyword planner for a UI hint and draft-expansion pre-check. It is not the system's canonical intent router, and those heuristics must not be expanded into Memo's general routing architecture. Replacing/removing the compatibility path requires a separate tested implementation task.

### PI-06 — AI-generated plans could import assumptions or unrelated domain templates
**Finding:** A model-generated workflow proposal can be syntactically valid while its task descriptions still over-interpret the user's goal. The current validator enforces the original goal, typed graph and runtime/capability allowlists, but cannot fully prove semantic relevance of every model-generated task.

**Remediation:** Tightened the legacy Work draft-expansion prompt to state that it is not Memo's general router or product authority, require every node to trace to the exact goal or a necessary step, preserve unknown facts, and prohibit importing unrelated examples/domain templates. The generated graph remains an unexecuted draft and is revalidated against registered runtimes and strict read-only capability allowlists. Talk workflow assessment likewise produces proposals only; it does not execute them.

**Remaining technical debt:** Semantic proposal evaluation is not yet a complete deterministic guarantee. Do not connect these draft-only proposals to unrestricted execution. A general orchestrator needs its own traceability, policy, evidence and verification tests before it can execute work.

### PI-07 — Stale dated records could be read as current implementation truth
**Finding:** State, roadmap, feature audit, architecture map, build status and PR text mixed historical source snapshots with current implementation statements. This risks hallucinated build/device acceptance and coding against stale architecture.

**Remediation:** Marked historical audit/build/architecture snapshots as historical, updated the roadmap's Talk status, and mapped run #821 to its exact tested source SHA in State and Build Status. The source verifier checks key status/authority markers. The current branch must be validated by exact-head CI; a passing earlier commit does not prove a later head.

**Current verification boundary:** GitHub Actions run #821 passed on exact source commit `028a5cda60ac0a4761fc23eb9f4537f879cbd6d0`. The governance/source changes made after that commit are not covered by run #821. Physical-device UX acceptance remains pending.

## Permanent operating rules

1. Read `AGENTS.md` and `docs/USER_INPUT_LEDGER.md` before substantial Memo work.
2. Append every material user correction, requirement, feedback item, bug report, idea and use-case boundary with original wording and a stable ID. Keep user wording separate from assistant interpretation.
3. Never promote an assistant suggestion, old project, existing class, use case or model proposal into accepted product scope without user adoption.
4. A correction overrides assistant interpretation. If two explicit user decisions genuinely conflict, preserve both and ask rather than silently choosing.
5. Keep ideas uncommitted until the user adopts them. Keep bugs and feedback active until evidence-backed closure or explicit user closure.
6. Preserve historical records; append status events rather than rewriting prior user input.
7. Update requirement/task/state/checkpoint records as appropriate and distinguish designed, implemented, source-verified, CI-verified, provider-verified and device-accepted.
8. Never claim that a later commit is tested by an earlier Actions run.

## Files central to this safeguard

- `AGENTS.md`
- `docs/USER_INPUT_LEDGER.md`
- `docs/MEMO_PRODUCT_CONTRACT.md`
- `DEVELOPMENT_GOVERNANCE.md`
- `IMPLEMENTATION_CONTRACT.md`
- `docs/DECISION_LOG.md`
- `docs/REQUIREMENT_TRACEABILITY.md`
- `TASKS.md`, `ROADMAP.md`, `PRODUCT_GAP_REGISTRY.md`
- `tools/verify_source.py`
- `.github/workflows/build-apk.yml`

## Change control

This audit and its remediations are on `stabilization/usable-stage-2026-11-10` / PR #8 only. No merge, release, deployment, destructive migration, signing change or modification to `main` is authorized by this work.

**Verification status:** governance edits are committed to the branch. Static content/marker presence has been manually checked through repository reads. The updated verifier has not yet been run in GitHub Actions on the final head. No Android build, signed artifact or physical-device acceptance is claimed for these newer commits.
