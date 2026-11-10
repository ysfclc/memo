# Memo persistent APK signing setup

This one-time setup gives all future Memo sideload builds the same signing certificate. It is required because GitHub-hosted runners generated a different debug certificate for different runs, which Android correctly refuses to treat as an in-place update.

## Recommended: one PowerShell command

1. Extract this kit to a private folder on your Windows PC. Do **not** put it inside the Memo source repository.
2. Install GitHub CLI and sign in with an account that has repository admin access:
   - Install: https://cli.github.com/
   - Run `gh auth login`
3. Open PowerShell in the extracted folder and run:

```powershell
Set-ExecutionPolicy -Scope Process Bypass
.\Update-MemoReleaseSigning.ps1
```

The script sets four GitHub Actions secrets on `ysfclc/memo` and updates the active workflow on `ysfclc-patch-1` so it can detect root-level ZIP uploads, use the persistent key, and verify the APK identity before publishing.

## Manual alternative

In GitHub, open `ysfclc/memo` → Settings → Secrets and variables → Actions. Create these repository secrets:

- `MEMO_UPLOAD_KEYSTORE_BASE64`: base64 of `memo-upload-signing.jks` (PowerShell: `[Convert]::ToBase64String([IO.File]::ReadAllBytes('.\memo-upload-signing.jks'))`)
- `MEMO_KEYSTORE_PASSWORD`: value from `Memo-Signing-Secrets.txt`
- `MEMO_KEY_ALIAS`: value from `Memo-Signing-Secrets.txt`
- `MEMO_KEY_PASSWORD`: value from `Memo-Signing-Secrets.txt`

Then apply the workflow changes from `Update-MemoReleaseSigning.ps1` or ask me to provide the exact manual patch. Do not paste any password or base64 key into a public issue, commit, workflow YAML, or source ZIP.

## Security and migration notes

- `memo-upload-signing.jks` is a private signing credential. Keep a secure backup. If it is lost, future APKs cannot update installations signed with it.
- The key is intentionally excluded from `memo.zip` and the public repository.
- The first build using this stable key may require a one-time migration if the currently installed APK was signed by an old ephemeral CI key. Android cannot change an installed app's signing certificate in place. Export an encrypted Memo backup before that migration and restore it afterward.
- After the migration, future builds must keep `applicationId` = `app.memo.android.debug`, reuse this exact key, and increment `versionCode`. Routine updates must not require uninstalling or deleting app data.
