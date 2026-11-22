# Memo physical-device acceptance — release candidate 4.0.0-debug

## Safety before install

- [ ] Keep current installation; do not uninstall.
- [ ] Confirm package: `app.memo.android.debug`.
- [ ] Confirm Memo encrypted backup/export succeeds and backup can be located.
- [ ] Confirm the current installed version and package signature are compatible with this APK.
- [ ] Install as an in-place update only. If Android rejects it, stop and capture the error; do not clear app data or uninstall.

## Release candidate identity

APK: `Memo-Android-debug.apk`

Expected SHA-256:
`9e075f944de83615295445e01cfd7cfcbdf3fee2712326e9bf110a8e5f42b4af`

Built from source:
`d189a732416c241d1e5d94d6a20d6a7e11345034`

Expected package: `app.memo.android.debug`
Expected version: `4.0.0-memo-debug`

## Basic acceptance

- [ ] Install/update completes.
- [ ] App launches without crash or blank screen.
- [ ] Unlock / biometric or app-lock flow works.
- [ ] Existing conversations remain visible and open.
- [ ] Existing files and saved records remain accessible.
- [ ] Talk: send a simple text prompt; response completes and is saved.
- [ ] Talk: stop a response mid-generation; partial response is retained.
- [ ] Navigation: switch Talk → Live → Work → Studio → Talk; no crash and no unexpected loss.
- [ ] Live: current/recent run states are truthful; recovered runs show review status.
- [ ] Work: existing records load and open.
- [ ] Studio: existing content loads and draft editing/saving behaves.
- [ ] Voice input: permission, launch, recognition and failure path.
- [ ] Local model: list/load/respond/stop (if a compatible model is installed).
- [ ] Remote model: configure/provider selection/respond and failure states (if configured).
- [ ] Browser: open session, navigate, return, handle failure.
- [ ] Native automation: permissions and foreground/unlocked boundaries are respected; no test of real external side effects without explicit approval.
- [ ] Backup/export: create an encrypted backup and verify it is present.
- [ ] Restart app: existing conversations/files survive; unfinished runs recover as NEEDS_REVIEW and do not replay.

## Failure capture

For any failure, record:
- Screen/feature and exact action immediately before failure.
- Whether crash, hang, wrong output, missing record or permission failure.
- Approximate time.
- Screenshot or screen recording, if possible.
- App version and APK SHA-256 above.

Do not send secrets, passwords, private message contents, authentication codes or sensitive screenshots.

## Acceptance status

- CI/emulator evidence: separate and already documented.
- Physical-device acceptance: pending; cannot be inferred from CI.
- User report: pending.
