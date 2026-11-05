# CI run #72 — ZIP metadata repair

**Repository:** `ysfclc/memo`  
**Branch:** `ysfclc-patch-1`  
**Run:** [#72](https://github.com/ysfclc/memo/actions/runs/41965700176)  
**Upload commit:** `176aae88a0a6f8e09d6e8ac45b6ecc48b00150f1`  
**Failed archive SHA-256:** `0797edb71f38f5d92338444900d5bc29927cab87f0a58b80daab329652526043`

## Failure

The importer rejected `Memo-Android/.github/workflows/build-apk.yml` with:

```text
ZIP validation failed: symlink or special file is forbidden:
'Memo-Android/.github/workflows/build-apk.yml'
```

The entry was not a symlink. The ZIP producer had populated only Unix permission bits (such as `0644`) and omitted the Unix file-type bits. The validator uses file-type checks (`S_ISREG`, `S_ISDIR`, `S_ISLNK`) to reject special files; permission-only metadata does not identify a regular file, so the strict validator rejected it.

## Repair

Repacked the same project contents with Unix ZIP metadata explicitly set to regular files (`S_IFREG | 0644`) and directories (`S_IFDIR | 0755`). No extraction-safety checks were weakened and no project source code was changed for this repair.

## Local checks after repair

- ZIP CRC: passed.
- ZIP file-type scan: 0 symlinks/special files.
- Memo source verifier: passed.
- Remote runner tests: 12/12 passed.
- Full Gradle tests, Android unit tests, lint and APK assembly: pending the next GitHub Actions run.
