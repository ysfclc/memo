# Memo Branch and Worktree Strategy

**Integration branch:** `main`  
**Rule:** one meaningful feature per isolated branch and local Git worktree. Never combine unrelated product slices merely because they touch the same app.

## Branch roles

- `main` — integrated source of truth. Merge through pull requests; no feature work directly on main.
- `feature/<domain>-<slice>` — one feature work package, one focused PR, one named acceptance gate.
- `fix/<area>-<issue>` — isolated repair with a regression test.
- `security/<area>` — security-sensitive work; keep separate until policy and tests are reviewed.
- `integration/<topic>` — temporary consolidation branch only; retire after merge.

Examples:
- `feature/sales-ops-account-research`
- `feature/sales-ops-qualification`
- `feature/social-provider-audit`
- `feature/ux-navigation-rebuild`
- `security/native-action-approval`

## Local worktree commands

After cloning the private repository once:

```bash
git fetch origin
git worktree add ../memo-sales-account -b feature/sales-ops-account-research origin/main
git worktree add ../memo-ux-navigation -b feature/ux-navigation-rebuild origin/main
git worktree add ../memo-social-audit -b feature/social-provider-audit origin/main
```

Each worktree has its own branch and working directory while sharing one Git object store. Do not run two agents in the same working directory. Do not let parallel worktrees edit the same shared files without an explicit file-ownership contract.

## Work-package contract

Every feature branch must state:
1. Requirement IDs and user outcome.
2. Source-of-truth base commit and dependency branches.
3. Files/modules owned and explicitly excluded.
4. Domain/data contracts and migration impact.
5. Security/privacy/approval requirements.
6. Named unit, integration, lint and device tests.
7. APK/build evidence required before merge.
8. Rollback and data-compatibility plan.

## Merge and release

- Feature PRs target `main`; CI runs read-only build/test validation on the PR merge candidate.
- Signing secrets are never available to pull-request build code. The pinned APK signer runs only after a trusted main-branch build and consumes the APK artifact, not repository source.
- Merge only after required checks and security review pass. A draft PR is not a release.
- Main-branch build artifacts are tied to the exact commit SHA and include a checksum and package/certificate identity receipt.
- Rebase or merge the latest main into long-running branches deliberately. Never force-push a branch containing another work package's commits.

## Current parallel work carried forward

The 9 November UX/navigation rebuild and native-automation safety work remain separate workstreams. They are not discarded by mainline consolidation and must be reconciled with main only after their Kotlin/Gradle/lint/CI/device gates are satisfied.
