# Session checkpoint — mainline consolidation

**Date:** 10 November 2026  
**Target:** `ysfclc/memo`  
**Integration target:** `main`

## Baseline found

- `main` was at `4699600f5e599d53417677309d941ffe961e772f` and contained only 14 tracked paths: README, one workflow, one UI source file and a repository ZIP.
- The full Android source baseline is preserved in `ysfclc-patch-1` at `9edf9876010371a6619d0f7a43fd5024a90c904b`.
- The credential-isolation branch is `security/isolate-ci-signing-2026-11-09` at `9c6eab03007597e80334e24425f75cbb03bf3345`. Its workflow statically separates read-only preflight/build, artifact-only signing and write-only source handoff.
- The Sales Operations use-case pack and governance/social product scope are at `feature/sales-operations-use-cases-2026-11-10` head `6c5407357bb834ef5024e3a82bae01bf497072a6`.

## Changes brought into the mainline candidate

- Full Android Gradle project source lives at the repository root; the redundant root source ZIP is removed from the current tree.
- The newest `main` WorkspaceScreens page-header change is retained.
- CI workflow branch restrictions target `main`: read-only build/test, artifact-only signing and separate write-only handoff.
- Development governance, decision history, requirement traceability, social platform scope and all six Sales Operations use cases are included.
- Architecture and isolated worktree policy are documented in `docs/architecture/`.
- New product requirements MEMO-SALES-001 through MEMO-SALES-006 are specified, not implemented.

## APK evidence and limits

- GitHub Actions run [#223 / 42530596381](https://github.com/ysfclc/memo/actions/runs/42530596381) passed on 8 November for commit `c4cfba812362fd5f69cc72026a935fe77a4d6dfc`.
- The APK is `app.memo.android.debug`, version code 223, version name `4.0.0-memo-debug`, signing certificate `a15b8b21e4d9ef43cbba01e8230491796c75c6ee41b3f5c674558aced5fb134a`, SHA-256 `670acd9192f4b02e7aea1c23e456036b2ad3a98f5d48d57ef7c895a72e8d22b2`.
- The inspected source baseline `9edf9876010371a6619d0f7a43fd5024a90c904b` is one commit after that build; that change touched only build-status/checksum/import-history files, not Android source.
- The newer PR3 workflow attempts failed before runner startup (zero executed steps; no test result). This is not an app test failure, but it is not a successful CI run.
- No build is claimed for this consolidation candidate until CI runs against its exact commit. Physical-device install-over-existing acceptance remains outstanding.
- UX/navigation PR #5 and native-approval/reliability PR #4 remain separate open workstreams and are not included as accepted behavior.

## Next feature work package

Create `feature/sales-ops-account-research` from the integrated mainline. Implement only M7.A1 / MEMO-SALES-001: organization projection, source-evidence linkage/validation, transparent ICP scoring with unknowns, duplicate-safe matching and operator review status. Do not implement outbound contact or social connectors in this slice.
