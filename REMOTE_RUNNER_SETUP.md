# Optional Memo guest runner

The remote guest runner is a separately installed companion that drives a connected Android device through ADB. It is not required for local Memo workflows and does not run AI prompts. It accepts only bounded native workflow steps, requires explicit approval, and returns status/evidence. Memo itself submits only the reviewed workflow definition; it does not upload conversations, attachments, frontier-provider responses or local-model context.

## Requirements

- A host you control, Python 3.10+ and ADB installed.
- A guest Android device paired through `adb devices` and authorized by the device owner.
- A trusted TLS certificate/key and a random bearer token with at least 32 characters.
- A network boundary/firewall appropriate to the host. Public binding is blocked unless you affirm you have configured perimeter controls.

## Start

Run from `remote-runner/` (or point the variables at the same files):

```bash
export MEMO_BEARER_TOKEN="replace-with-a-random-secret-of-at-least-32-characters"
export MEMO_TLS_CERT="/secure/path/server.crt"
export MEMO_TLS_KEY="/secure/path/server.key"
python3 server.py --host 127.0.0.1 --port 8765
```

The runner supports the previous `SYNTHOS_*` environment-variable names for compatibility. Use `MEMO_*` for new deployments. If you bind to a non-loopback interface, set `MEMO_ACK_PUBLIC_BIND=YES_I_HAVE_FIREWALLED_AND_TLS_TERMINATED_THIS_HOST` only after configuring the host/firewall and TLS properly. Keep the bearer token and private key out of source control. The SQLite job database is a private operational record and should be backed up/retained according to your own policy.

## API and safety

- `GET /health` — authenticated protocol and guest connection status.
- `POST /jobs` — authenticated; requires JSON with a workflow and `approved: true`.
- `GET /jobs/{id}` — authenticated job state/evidence.
- `POST /jobs/{id}/cancel` — authenticated cancellation request.

Requests have bounded sizes and step counts. The runner rejects unknown operations, missing visible-text postconditions, sensitive or restricted text, and execution without a connected guest. After a restart, jobs previously marked running become interrupted. The runner does not automatically retry an interrupted UI action because it cannot know whether the side effect already occurred.

Run unit tests:

```bash
python3 -m unittest discover -s tests -v
```

The guest runner controls a real device through ADB and still requires physical/device-level testing. Use it only with devices you own or are authorized to administer.
