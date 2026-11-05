# Memo — device acceptance checklist

Run this after GitHub Actions produces the debug APK. These tests cannot be substituted by a successful source ZIP check.

## Core workspace

- [ ] Install the APK and open Memo; the app launches without a crash.
- [ ] Authenticate with the Android device credential/biometric prompt and lock Memo again.
- [ ] Create, rename, reopen and delete a chat; force-stop/reopen and confirm persistence.
- [ ] Import a small text file and Android shared text; confirm the captured text is a source, not silently executed instructions.
- [ ] Create an entity, connect a relationship and export a workspace backup; restore a test backup only after reviewing the replace warning.
- [ ] Confirm remote inference does not happen until the per-request remote-consent control is enabled.

## Frontier browser workflow

- [ ] Open a provider website that you are signed into, create a consultation, and run one provider first.
- [ ] Confirm the browser remains visible, the exact submitted prompt is the approved one, and the captured answer is stored with its source URL/time.
- [ ] Run a two-provider comparison and confirm each provider is visited sequentially and receives only the configured prompt/context.
- [ ] Change the provider page structure or navigate away mid-run; confirm Memo pauses for takeover rather than claiming success or blindly re-sending.

## Native Android automation

- [ ] Open Apps & automation, verify app inventory labels and launch an ordinary app.
- [ ] Enable Memo's Accessibility service manually in Android settings. Confirm the UI shows permission as enabled on return.
- [ ] Enable floating controls only after Android's overlay permission flow. Confirm Capture requires a user tap.
- [ ] Save a safe workflow that opens an app, taps an unambiguous non-sensitive visible control, and waits for a final visible-text condition.
- [ ] Try to save a workflow without a final success condition and verify Memo rejects it.
- [ ] Start a workflow; verify the target app is foregrounded, steps are traceable, and the run stops/pauses if the target app leaves the foreground or the phone locks.
- [ ] Stop a running workflow and confirm it does not continue to issue actions after cancellation.
- [ ] Edit a workflow and confirm a new revision is created; open history and confirm the previous version remains visible.
- [ ] Confirm workflows proposing password entry, payment confirmation, CAPTCHA/security bypass, or ad/reward engagement are rejected.

## Optional remote guest runner

- [ ] Only configure a runner on a device/host you control, using HTTPS and a randomly generated bearer token (32+ characters).
- [ ] Verify unauthenticated health/job calls fail, a valid token succeeds, and the guest reports connected only when the paired ADB device is available.
- [ ] Preview a harmless bounded workflow, cancel at preview, and confirm no job was submitted.
- [ ] Approve the workflow intentionally; check job status, final evidence and cancellation. Kill/restart the guest runner during a test and confirm interrupted jobs are not silently replayed.

## Sign-off

Record device model, Android version, APK SHA-256, GitHub Actions run URL, test date, failures and screenshots. Do not label the build production-ready until this checklist and the Android unit tests/lint pass on the actual release candidate.

## Language, voice and notifications (added in the current integration)

- [ ] Switch the interface language to English, Polski and Türkçe. Confirm the selection survives force-stop/reopen and key navigation/settings labels change. Record untranslated labels as localization follow-ups.
- [ ] In Polish mode, tap the chat microphone, allow microphone permission, speak a short Polish message, confirm the transcript is inserted into the editable draft and is not sent automatically.
- [ ] Repeat voice input in Turkish and English. Deny microphone permission once and confirm typing remains usable and the UI explains why voice cannot start.
- [ ] Confirm Memo does not listen continuously and has no active wake-word service in this build.
- [ ] Open Settings → Notifications & reminders. Request notification permission only after a user action; deny it and confirm the permission path reports the limitation without claiming the notification was sent.
- [ ] Allow notifications and run Test notification. Check Android's app notification settings and verify the body contains no conversation, task or provider output.
- [ ] Enable the daily nudge, choose a time a few minutes ahead, and verify the local reminder; toggle it off and verify subsequent reminders are cancelled.
- [ ] Verify the reminder re-schedules after reboot/timezone/time change; test with battery saver and record any OEM delay rather than assuming exact timing.
- [ ] Complete and fail a bounded native workflow; confirm one generic workflow notification per terminal transition and no duplicate notification on ordinary refreshes.
- [ ] Tap a Memo notification while locked. Confirm Memo still requires its normal workspace unlock before private content is accessible.
