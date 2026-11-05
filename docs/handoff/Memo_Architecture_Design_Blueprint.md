# Memo
## Product Architecture & Design Blueprint

**Version:** 0.1 — review draft  
**Date:** 26 October 2026  
**Status:** Phase 1 reconnaissance complete; architecture and design proposed for approval  
**Scope:** iOS + Android mobile first; architecture prepared for a future desktop client.  

> **Decision in one sentence:** Memo is a local-first personal execution environment where conversation is the easiest way in, while evidence-backed records, relationships, tasks, and verified work live in a durable workspace behind it.

This is a design and implementation plan, not a claim that the prototype or any included Android source package is already a unified, release-ready Memo application. No product implementation has been started in this phase.

---

# 1. Executive recommendation

The archive contains useful pieces of one larger product, but not one coherent codebase. The strongest ideas are spread across a conversational HTML prototype, several Android-native products named Threadline and SynthOS, and a constrained Android runtime. They differ in UI frameworks, storage formats, model integration, entity models, runtime assumptions, and verification status. The correct move is to establish a single product contract and port the best primitives into it—not to combine their screens or copy entire apps into one binary.

Memo should be built around five invariants:

1. **Conversation is the interface, not the database.** A natural text/voice conversation stays continuous, but durable tasks, people, companies, projects, sources, decisions, and runs are real records that can be opened and resumed independently.
2. **Local data is the default source of truth.** Capturing, searching, creating internal records, reviewing evidence, and using a compatible installed local model should work without a Memo account or a cloud AI dependency.
3. **Models propose; Memo validates.** Model output is untrusted structured input. Deterministic domain code validates it. High-impact actions require approval, and success requires observable evidence—not a model's claim that an action succeeded.
4. **Everything important has provenance.** Records and relationships can point back to the source, timestamp, excerpt, model proposal, human decision, or runtime observation that supports them.
5. **Capabilities are explicit and replaceable.** Browser work, Android system integrations, local models, remote model providers, reminders, and future connectors implement contracts. None owns the canonical task/entity state or receives implicit permission to act.

## Recommended product shape

- **Primary experience:** an actual conversation UI with streaming replies, keyboard-safe composition, optional voice input/output, interruption, and inline cards for real tasks or records created from a conversation.
- **Durable workspace:** an extensible record graph for projects, tasks, people, organizations, opportunities, source material, decisions, artifacts, and activity history. “CRM” and “job search” are views/workflows over these shared records, not separate databases.
- **First-class browser:** a real, visible in-app browsing surface whose active page can be discussed, captured, linked to a task, and used as evidence. The browser does not get a privileged bridge into Memo's storage or permissions.
- **Local intelligence:** compatible on-device models help interpret intent and propose structured changes. Deterministic rules handle simple operations and data integrity. Optional remote models are separately configured and require per-request consent to send a visible, limited context packet.
- **Trustworthy execution:** persisted task state, bounded capabilities, approval records, checkpoints, observable outcomes, recovery, cancellation, and an append-only event journal.

## Initial scope decision

Ship the useful local core first. Defer general cross-app autonomous automation, a full multi-agent council, automatic social publishing, broad scraping, video/image generation, and a remote guest-device runner. These are not prerequisites for Memo's core value and would increase platform, security, and release risk before the record system is solid.

---

# 2. Phase 1 — reconnaissance and synthesis

## 2.1 Archive inventory

The supplied `all.zip` contains nine top-level entries. Two standalone HTML files are exact byte-for-byte duplicates of the HTML files inside `memo-v2-prototype.zip`; they are alternate copies, not separate implementations.

| Asset | What it contains | Reusable value | Decision for Memo |
|---|---|---|---|
| `index.html` | Responsive conversation/workspace prototype with Talk, Work, Browser, Memory/Sources and model-routing concepts | Product direction; inline task cards; visible context; evidence/approval concepts; architecture inspector | Use as a UX reference only. Rebuild in native mobile UI. It uses browser `localStorage` and simulated logic, not live models, native files, or a real browser runtime. |
| `explainer.html` | Seven-scene, self-contained architecture walkthrough with timeline and reduced-motion behavior | Explains the intended relationship between conversation, context, policy, runtimes, and evidence | Keep as a communication reference, not app code. |
| `memo-v2-prototype.zip` | The same HTML prototype plus a README describing its limits | Confirms its own prototype boundary | Do not merge it separately; the two HTML documents match the standalone copies exactly. |
| `threadline-vo.zip` | Threadline 1.6 Android project; Kotlin domain models, local LiteRT-LM, encrypted vault, encrypted passphrase backup, capture/share, OCR, reminders, floating capture, content drafts, motion system and product docs | Strong privacy baseline; deterministic task/project rules; encrypted backup; low-friction capture; private-by-default artifact drafting; reduced-motion design | Port the data and security concepts. Do not keep the large feature-owning `MainActivity.kt` as the application architecture. The package is Android-only, and the docs explicitly leave full build/device verification outstanding. |
| `threadline-app.zip` | Earlier Threadline Android version with a bundled HTML/WebView UI, assistant runtime/store, source mirroring, model catalogue worker, floating assistant and Android accessibility integration | Useful examples for source mirroring, app lifecycle, share/capture and runtime boundaries | Treat as a predecessor. Do not combine its shell or persistence with Threadline 1.6; reconcile useful functions against the new contracts instead. |
| `synthos-pocket.zip` | Compose Android capture/workspace app with share imports, attachments, local model manager, prompt composers, mission context, encrypted storage, reminders/overlay and QA documentation | Good capture pipeline, explicit context selection, model setup, attachment limits and recovery-oriented QA | Reuse the capture/source workflow and context-manifest idea. Keep floating overlay optional and later; make the main composer the primary interaction. |
| `synthos-fieldwork.zip` | Earlier SynthOS Fieldwork iteration with native browser inspection, local model/Council work, tasks, encrypted vault and optional HTTPS model endpoint | Model/provider abstraction, context-aware tasks, local/remote separation, backup and inspectable multi-model outputs | Use as historical reference, not as a second subsystem. |
| `synthos-fieldwork - 2.zip` | Later Fieldwork 3.1.0 source: saved chats, visible frontier-model website workflows, typed entities/relationships, evidence-checked model proposals, job traces, local model/Council runtime and browser tasks | Most mature entity graph, source-validated proposals, typed runtime registry, browser/provider separation, and outcome/job tracing | Port the schema ideas and evidence-validation rules. Website automation must remain bounded and recoverable; its behavior is not a guarantee that third-party website UIs remain stable. Android build and device inference are not certified by the archive. |
| `app-release.zip` | SynthOS App Runtime 0.3 Android source plus Python HTTPS guest-runner: local LiteRT-LM, versioned skills, native workflow policy, AccessibilityService workflow executor, run ledger, encrypted vault and remote-runner protocol | Strong execution contract: explicit start, allowlisted steps, checkpoints, interruption recovery, postcondition verification and separate remote runner | Use the task/run state machine and evidence rules. Do not make AccessibilityService or the guest runner a baseline cross-platform requirement. No APK is included; Android release verification is outstanding. |

### Verification evidence from the archive

- The latest Threadline and SynthOS docs distinguish “implemented in source” from “verified on device.” Neither source presence nor a local syntax check proves a working Android build.
- The `app-release/PRODUCT_STATUS.md` explicitly says the Android build was not completed in that environment and no APK was included.
- I ran the packaged `app-release/remote-runner/tests` Python suite during this audit: **12 tests passed**. This verifies the isolated Python protocol tests only; it does not verify Android compilation, actual ADB pairing, TLS deployment, mobile UI, or a real guest device.
- The supplied archive has no iOS project. All Kotlin/Java runtimes and Android manifest integrations need an iOS implementation or an explicit “not supported on iOS” capability declaration.

## 2.2 What the existing micro-apps are good at

| Capability cluster | Best source material | Keep / change |
|---|---|---|
| Capture and intake | Threadline 1.6, SynthOS Pocket, Threadline 2.0 | Keep text, URL, share-sheet, file and user-triggered voice capture. Standardize on one `Source` object and one ingestion pipeline. |
| Private local persistence | Threadline 1.6 secure vault; SynthOS Pocket / Fieldwork encrypted stores | Keep encryption, atomic writes, schema migration and “never overwrite the last good file after a failed decrypt.” Replace several incompatible JSON snapshots with one versioned database. |
| Tasks and projects | Threadline domain rules; SynthOS task/run records | Keep explicit states, duplicate prevention, due dates, blockers and durable IDs. Generalize the task model so a task can link to any record or source. |
| Local AI | LiteRT-LM adapters and model managers across the Android projects | Keep a provider interface and explicit model download/import. A model file being installed must not be treated as proof that it loads or fits the device. |
| Entity graph | SynthOS Fieldwork 3.1 typed entities, relations, evidence-backed proposals | Keep first-class relationships and source excerpts. Make the type registry extensible instead of freezing Memo to one closed list. |
| Browser work | Memo HTML prototype; Fieldwork BrowserSession / BrowserScripts | Keep a real visible browser and active-tab context. Start with browsing, capture and bounded page inspection; do not promise universal sign-in or stable arbitrary automation. |
| Execution and verification | App Runtime WorkflowPolicy, run records and guest-runner protocol | Keep explicit approval, strict step allowlists, idempotency, interruption recovery and final postconditions. Keep Android system automation isolated from the cross-platform core. |
| Visual language and motion | Threadline DesignSystem; SynthOS Pocket motion docs; Memo HTML prototype | Combine calm evergreen/graphite surfaces, clear semantic states, short state-driven motion and visible evidence. Replace separate screens and styles with a single token/component system. |
| Content/studio drafting | Threadline 1.6 | Treat as a later capability that creates private `Artifact` drafts from selected evidence. Do not make automatic social publishing a core screen or allow generated content to become public implicitly. |

## 2.3 Critical gaps and conflicts to resolve

1. **Several app identities and codebases:** Threadline, SynthOS, SynthOS Pocket, SynthOS Fieldwork and the Memo prototype have overlapping functionality but different contracts. Memo needs one name, one navigation model, one domain layer, one schema and one release pipeline.
2. **Separate persistence formats:** several stores serialize whole app snapshots to JSON files. This is simple for prototypes but makes search, relationships, migrations and concurrent task updates harder. Memo should use relational SQLite tables, with a typed entity/relationship layer on top.
3. **UI orchestration concentrated in large activities:** several Android `MainActivity` files own too much screen composition, state and business flow. Feature modules and domain services should have explicit interfaces.
4. **Prototypes can overstate live capabilities:** the HTML prototype has simulated chat rules, localStorage records, and a fictional browser surface. It does not connect to a live local model, signed-in browser profile, file library, remote worker or native phone runtime.
5. **Local and remote semantics are inconsistent:** some builds are local-only; another can use an OpenAI-compatible endpoint with per-run consent; another models external provider websites as visible WebView workflows. Memo needs one route policy and a transparent data-disclosure step for every remote action.
6. **Execution is platform-bound:** Android AccessibilityService, floating overlays, app listing and ADB guest control cannot be assumed on iOS. They must not be prerequisites for the shared task model or core user journey.
7. **Security and recovery differ by version:** some workspaces have encrypted backups while other exports are plaintext JSON; local model keys, endpoint credentials and task payloads are stored differently. Memo must define one security contract and make any plaintext export an explicit exception.
8. **Release status is not production proof:** architecture notes and unit tests are useful, but mobile builds, encryption startup checks, model cold start, keyboard resize, voice, browser sign-in, interrupted runs and accessibility all need device-level acceptance.

## 2.4 Product filter

### Essential to Memo

- Continuous conversation with text and integrated voice input/output.
- One canonical, searchable, local-first record graph with source provenance.
- Tasks, projects, follow-ups, decisions, notes, people, organizations, opportunities and artifacts linked to sources.
- Share-sheet and file/URL capture with user review before ingesting sensitive material.
- Local model adapter with genuine streaming/cancellation, model compatibility checks and a truthful “not installed / not supported / failed” state.
- Optional remote provider with exact context preview and per-request consent.
- Real visible browser with active page attached to conversation/task context.
- Typed internal commands, policy checks, approval records, execution journal and verifiable outcomes.
- Encrypted local data, safe backup/restore, OS-backed key protection, and no hidden telemetry.

### Defer until core quality is proven

- Universal Android cross-app automation, floating overlay and unattended microphone.
- Remote guest-device/ADB runner and multi-device execution.
- Multi-model Council/debate as a default workflow; it may later be a specialist capability.
- Full video/image understanding, media generation, OCR/PDF bulk ingestion and semantic embeddings beyond small deterministic indexes.
- Social publishing, broad social scraping, autonomous email sending, calendar writes and general-purpose arbitrary shell execution.
- Mandatory cloud accounts, cloud-only storage, automatic remote inference fallback and usage/engagement dashboards.

---

# 3. Phase 2 — master architecture

## 3.1 Architecture decisions

| Decision | Proposed choice | Why |
|---|---|---|
| Mobile UI | **React Native + Expo prebuild/custom native builds + TypeScript** | One shared iOS/Android interface and domain layer. Expo Go is not the release target: SQLCipher and model/native integrations require a custom native build and device-level verification. |
| App navigation | Expo Router or equivalent typed route layer | Routes remain separate from business logic. Browser, chat and work surfaces can be developed and tested independently. |
| Local database | SQLite with SQLCipher encryption; repositories and migrations behind an interface | Local-first, transactional writes, searchable records, relationship joins and predictable migrations. On startup, explicitly verify `PRAGMA cipher_version`; never assume encryption is enabled merely because a key pragma ran. |
| Key management | Native platform adapter wrapping a random database key with Android Keystore / iOS Keychain-backed protection | Database keys never live in source code, logs, plain preferences or exported backups. Authentication UX is separated from raw key material. |
| Domain core | Pure TypeScript modules with schema validation (for example, Zod/JSON Schema) | Deterministic commands and policies can be unit tested without React Native, the model runtime or a server. |
| Model runtime | Provider interface; LiteRT-LM native Android and iOS adapters where supported; optional provider adapters behind policy | Keep models replaceable. Android Kotlin code is a reference implementation, not a cross-platform runtime by itself. |
| Browser | Native WebView surface wrapped by a platform-specific browser adapter, with a strict host-to-core boundary | Browser is real and visible; websites are untrusted content and cannot directly invoke privileged Memo commands. |
| App state / live UI | Repository-driven state subscriptions plus a durable local event journal | UI reflects committed work, not optimistic animation or fabricated progress. |
| Backend | **No mandatory cloud backend for v1.** Define an optional Memo Relay interface for later sync, connectors and explicitly delegated long jobs. | Preserve local utility and privacy. Do not build an always-on server before a real feature requires it. |
| Desktop path | Shared domain, schema, contracts and prompt registry in platform-independent packages | A later desktop shell can reuse the core. Desktop UI and device/runtime compatibility are outside this delivery. |

### Why not simply keep the Android Compose app?

Compose is a good Android implementation, and several Android-only features are already present. However, there is no iOS project in the archive. Keeping Compose as the only application UI would contradict the stated iOS/Android requirement. React Native means some Android UI code must be rebuilt, but it gives Memo a shared product shell while preserving hard platform-specific functions behind native adapters.

### Why no immediate cloud database?

Most of Memo's core value—capture, retrieval, record creation, local model use, tasks, evidence and internal actions—does not require a server. A cloud component creates a privacy boundary, credentials, retention rules, availability dependencies and a second state authority. Memo should not inherit those costs just to appear “full-stack.” An optional service becomes justified when a concrete use case needs OAuth token brokering, cross-device sync, push delivery or a long-running remote task.

## 3.2 Logical architecture

![Memo architecture: mobile client, domain core, policy and approval, capability runtimes, verification journal, encrypted local state, model adapters and optional Relay.](Memo_Architecture_Diagram.png)

The key direction is intentional: **models and runtimes cannot become the source of truth.** A model can return an `ActionProposal`; the core validates the shape and policy; the user approves where required; a runtime reports observations; the verifier decides whether the outcome is supported; and the journal stores what happened.

## 3.3 Module boundaries

| Module | Owns | Must not own |
|---|---|---|
| `features/talk` | Message UI, streaming state, voice controls, selected context UI | Direct database writes from a model response; hidden remote sends |
| `features/work` | Today/Inbox, projects, task detail, entities and activity views | Its own duplicate contacts/jobs/project databases |
| `features/browser` | URL/navigation, visible WebView, tabs, active-page reference, user handoff | Privileged JavaScript bridge exposed to website content; arbitrary shell/network actions |
| `features/library` | Sources, imported items, notes, search and provenance detail | Automatically trust imported text as instructions or facts |
| `core/domain` | Record types, task/run states, invariants, IDs and domain validation | Platform UI, network calls, model initialization |
| `core/commands` | Typed use cases such as `CaptureSource`, `CreateTask`, `LinkRecords`, `ApproveAction` | Unbounded side effects hidden inside parsing |
| `core/policy` | Capability checks, privacy routing, risk classification, approval and idempotency rules | Inferring permission from a model's natural-language confidence |
| `data/db` | SQLCipher initialization, migrations, repository implementations, transactions | Plaintext database fallback when encryption support is absent |
| `data/crypto` | Random key creation, platform key wrapping, backup encryption, lifecycle clearing | Hard-coded keys, key logging, credentials in JSON exports |
| `ai/providers` | Local/remote adapter contracts, streaming, cancellation and capability discovery | Own the tasks or claim external effects without proof |
| `ai/context` | Build bounded context packets with record IDs and source excerpts | Sending the full workspace by default |
| `ai/proposals` | Parse/validate model output, match excerpts to source corpus, surface unknowns | Silently turning suggestions into accepted facts |
| `runtime/registry` | Typed runtime capabilities, versions, permission requirements, supported platforms | Treating iOS and Android as feature-identical when they are not |
| `runtime/browser` | Bounded browser observation/actions and capture evidence | Website-controlled access to Memo's internal API |
| `runtime/jobs` | Persisted execution state, scheduling, checkpoints, cancellation and recovery | Automatic replay of an uncertain external side effect |
| `runtime/notifications` | User-selected reminders and important state alerts | Engagement nudges unrelated to user goals |
| `optional/relay` (later) | Explicit remote execution, connector callbacks, selected sync operations | Silent full-workspace upload or unconditional remote inference |

---

# 4. Unified data model

## 4.1 Design rule: one record graph, many views

Memo should not build one database for CRM, another for job search, another for AI chats, and another for browser research. Every durable object lives in a common record system. A record has a stable ID and an extensible type. Relationships are first-class rows with their own evidence and status. Structured claims keep facts, model suggestions and disputed information distinct.

The model should be **open-ended but typed**: ship useful standard types, then permit user-defined types and fields through a versioned type registry. Do not hardcode a giant closed ontology. Do not create or merge identities merely because two names look similar; preserve aliases and require evidence or a human decision for identity merges.

## 4.2 Core tables

| Table | Purpose / important fields |
|---|---|
| `records` | Canonical entities/pages: `id`, `type_key`, `title`, `properties_json`, `lifecycle_state`, `privacy_label`, `created_at`, `updated_at`, `deleted_at`, `schema_version`. Indexed common fields stay relational; flexible type-specific fields live in validated JSON. |
| `record_types` | Versioned open type registry: `type_key`, label, schema, allowed relations and display configuration. Built-in types are defaults, not a closed universe. |
| `relationships` | Explicit edges: `id`, `from_record_id`, `relation_type`, `to_record_id`, `properties_json`, `status`, `created_at`, `updated_at`. |
| `relationship_evidence` | Many-to-many links from a relationship to source segments, user decisions or verifier observations. A link should be explainable. |
| `sources` | Provenance root: `id`, source kind, URI/provider/path reference, title, capture time, content hash, permission scope, privacy label, ingestion state, encrypted blob reference. |
| `source_segments` | Bounded text/image/OCR/document/browser excerpts with source ID, offsets/page/time, segment hash, extraction method and confidence. The content remains inside encrypted local storage. |
| `claims` | Subject/predicate/object or value, evidence reference, state (`observed`, `proposed`, `confirmed`, `disputed`, `superseded`), confidence, valid time and review metadata. |
| `aliases` | Alternate names, spelling, handles and phrases with provenance and identity-resolution status. Similar strings are candidates, not proof of sameness. |
| `conversations` / `messages` | Thread identity, message role/content, timestamps, provider/model ID, prompt version, generation status and selected context manifest. |
| `context_manifests` | Exact references/excerpts selected for a model/browser handoff, destination route, privacy labels, creation time and approval/consent record. Avoid silently reconstituting a broader context later. |
| `tasks` | Task-specific state associated with a `record`: status, due date, priority/reason, next action, blocked-by links, parent task/record, reminder settings and completion evidence. |
| `action_proposals` / `action_steps` | Structured model/runtime proposal, capability ID, payload, preconditions, postconditions, policy result, risk class and immutable payload hash. |
| `approvals` | User, timestamp, exact payload hash, destination/resource, scope, expiry and revoked/stale state. Any material payload change invalidates approval. |
| `execution_runs` | Durable run state, task/proposal link, capability and version, idempotency key, checkpoint, attempt count, started/updated timestamps, cancellation and result classification. |
| `observations` / `proofs` | What a runtime actually observed: URL, visible text, response ID, file hash, test result or human-confirmed outcome; reference linked to source and run. |
| `event_journal` | Append-only state and action history: request, plan, approval, start, progress, wait, observation, verification, failure, cancel and recovery events. |
| `model_profiles` / `model_runs` | Installed model metadata, platform/runtime compatibility, model checksum, roles, prompt version, duration/status, local/remote route and context manifest ID. No API secret is stored here. |
| `skills` / `skill_versions` | Reusable bounded workflows with typed inputs/outputs, preconditions, permissions, step limits, examples, tests and version history. |
| `integration_grants` | Provider, scopes, granted/revoked timestamps, expiry and consent metadata. Actual refresh tokens/API keys stay in OS-protected storage outside workspace exports. |
| `blobs` | Encrypted attachment/media metadata, MIME type, size, hash, encryption version, storage reference and retention status. |

## 4.3 Standard record types for the first release

Start with `Person`, `Organization`, `Project`, `Task`, `Decision`, `Note`, `Conversation`, `Source`, `Opportunity`, `Application`, `Artifact`, `BrowserSession`, `Workflow`, `ExecutionRun`, and `Knowledge`. Add types through `record_types`, not code branches scattered across the UI.

Type-specific fields can be schema-validated. For example:

- `Opportunity`: source URL, title, location/remote mode, compensation as supplied, employment type, extracted requirements, evidence per requirement, unknown fields, review state and last checked time.
- `Application`: opportunity ID, organization ID, stage, submitted-material references, approval status, confirmation evidence and follow-up date.
- `Person`: name/handle aliases, organization links, interaction summaries, source-backed attributes, privacy label and unresolved identity questions.
- `Organization`: canonical name, aliases/domains, sources and relationships to people/opportunities/projects.
- `Task`: desired outcome, next action, state, blockers, due date, context manifest and proof of completion.

## 4.4 CRM and job-search example

A job found on a website becomes a `Source`, then a `JobOpportunity`-style record (using the extensible type registry). Its employer resolves to an `Organization`; a recruiter becomes a `Person` only when there is adequate evidence. `APPLIES_TO`, `POSTED_BY`, `CONTACT_AT`, `FOUND_IN` and `FOLLOW_UP_FOR` are relationships. A selected opportunity can generate an `Application` and linked tasks. A follow-up reminder points to the same person, application and original source.

There is no duplicate contact database. CRM is a view over people, organizations, interactions and opportunities. Job search is a workflow over sources and opportunity records. Both use the same search, provenance, identity resolution, privacy rules and event history.

## 4.5 Evidence and model-proposal lifecycle

1. **Capture:** save the original source reference, timestamp, content hash, permission scope and encrypted content/excerpt.
2. **Extract deterministically:** text, URL, title, dates, links, table fields, offsets, repeated terms and known format metadata.
3. **Propose meaning:** a local/remote model may propose a typed record, claim, alias or relationship with an exact supporting excerpt and a confidence/reason field.
4. **Validate:** verify JSON schema, size limits, allowed record types/actions, source-excerpt match and existing identity candidates. Invalid or unsupported claims stay rejected/reviewable.
5. **Review:** the user approves, edits, rejects or defers uncertain identity/category changes. Simple explicit user commands can create internal records without an LLM.
6. **Commit atomically:** write the record/relationship and journal event in one transaction. Never show “saved” if persistence failed.
7. **Retrieve later:** include only the small relevant context bundle and show why records were linked.

---

# 5. Local AI and deterministic coordination

## 5.1 Model contract

All model implementations conform to one interface:

- `getCapabilities()` — supported input/output modes, structured output, streaming, cancellation, platform, memory/storage notes.
- `prepare()` — load or initialize the model and report actual readiness.
- `generate()` / `stream()` — return a response and structured status; no direct database mutation.
- `cancel()` and `dispose()` — release resources and stop active generation when the runtime supports it.

On Android and iOS, LiteRT-LM is a candidate native runtime because the official AI Edge stack documents both Android and iOS APIs. React Native still needs explicit native adapters; a Kotlin class from an Android archive is not an iOS implementation. Exact model artifacts and supported task capabilities must be tested per device/runtime before being offered in the catalog.

## 5.2 Routing rules

- Deterministic code handles clear commands such as creating a task, editing a title, setting a reminder or linking a record.
- A local model can classify ambiguous intent, extract structured candidates, suggest context links, produce a draft or generate a bounded action plan.
- The policy layer validates the resulting proposal using the same rules regardless of model provider.
- A remote model is **never an automatic fallback** when the local model is missing, slow or failing.
- Remote sending requires an enabled provider, explicit user approval for that request, a visible recipient/route and a preview of the exact context packet. The app logs the context-manifest reference and outcome, not secrets.
- A model claiming “sent,” “saved,” “submitted,” “deleted” or “completed” is not evidence that the action happened.

## 5.3 Prompt and context registry

Prompts/skills have IDs and versions, typed input/output schemas, evidence requirements, maximum input/output lengths, permission declarations and example tests. The context builder returns a bounded, inspectable bundle: selected record summaries, source excerpts, timestamps, unresolved conflicts and provenance references. The full workspace is never sent by default.

Language-intelligence proposals should preserve the original phrase, canonical candidate, aliases, context, source offsets, frequency and confidence. Repeated expressions can be clustered deterministically. A model definition that merely repeats a term without adding grounded meaning must fail the validation quality gate or remain an unresolved proposal.

## 5.4 Model manager UX

- No weights bundled in the application package.
- Models downloaded/imported only after a user action.
- Verify published checksums when available; distinguish publisher integrity from a hash calculated on an arbitrary user-imported file.
- Show size, compatible runtime, supported capabilities and device-test status.
- A download does not imply the model can initialize or fit memory. Include a real inference smoke test; preserve the last usable model if a replacement fails.
- Only load as many models concurrently as measured device RAM/thermal limits allow; one model at a time is the safe default.
- If no model is usable, capture/search/tasks and other deterministic features remain available.

---

# 6. Security, privacy and authentication

## 6.1 Trust boundaries

Memo has five distinct zones: (1) private local records, (2) imported/website content, (3) model providers, (4) capability runtimes, and (5) optional remote services. Data can cross a boundary only through a typed, logged operation with a stated purpose. An imported page or model response is untrusted evidence; it is never executable authority.

### Local at-rest protection

- Use SQLCipher for the workspace database and authenticated encryption for externally stored blobs. The app must verify the actual native database reports a SQLCipher version before using it; silently falling back to plaintext SQLite is a release-blocking failure.
- Generate a random database data key. Protect/wrap it through an OS-backed key-management adapter. Never hardcode, log or include it in backup JSON, diagnostics or source control.
- Clear sensitive in-memory keys/captures on lock, logout, explicit vault reset and appropriate background transitions. Make the re-lock policy configurable but privacy-first by default.
- Use atomic transactions/migrations. A failed decrypt or migration must not overwrite the last readable database.
- Disable unintended OS/cloud backup of internal key/database files. A user-controlled portable backup is a separate export format.

### Backup / restore

- Default to an encrypted, versioned backup with a passphrase-derived key (current cryptographic parameters to be selected and reviewed at implementation) and an authenticated encryption format.
- Validate file size, version, schema and integrity before replacing current data. Restore to a temporary database first; swap only after successful decrypt and validation.
- If a user explicitly requests plaintext JSON, show a separate warning that content and attachments are readable outside Memo. Do not quietly include API keys or refresh tokens in workspace exports.

### In transit / provider privacy

- HTTPS only for remote API requests and remote runtime calls; normal certificate validation, bounded payloads and timeouts.
- Local inference stays on device. A remote model request includes only the selected context manifest after per-request consent.
- Store API keys and OAuth refresh tokens separately in Keychain/Keystore-protected storage, with narrow scopes and revocation controls.
- No third-party analytics, session replay, prompt logging or remote crash uploads by default in v1. If diagnostics are added later, make them opt-in and redact them.

## 6.2 Authentication flow

**Local mode (default):** install/open Memo → create an encrypted local workspace → configure device passcode/biometric unlock → start using Talk/Work/Library. No account or internet connection is required for this core path.

**Optional Memo sync account (later):** user explicitly enables sync → authenticate using a system-supported OAuth/PKCE flow → store tokens in OS-protected storage → show which records/services can sync → allow per-collection pause/revocation. Sync is not assumed to be end-to-end encrypted until that protocol and key recovery design are implemented and tested.

**Third-party connector:** user picks a provider → Memo explains requested scopes and purpose → use the provider's supported OAuth/app authorization → store credentials securely → show active grant and a disconnect/revoke control. Do not ask users to paste passwords into Memo or work around provider sign-in restrictions.

**Website sign-in in Browser:** keep the user in a visible browser surface when that provider allows it. Some identity providers restrict authentication inside embedded WebViews; provide a legitimate system-browser/auth-session or share/capture fallback. Never bypass MFA, CAPTCHA, login barriers or account policy.

## 6.3 Privacy labels and disclosure

Each source/record/claim/context bundle has one or more explicit labels:

- `PRIVATE_LOCAL` — stored locally; not eligible for remote sending.
- `LOCAL_MODEL_ALLOWED` — may be supplied to a compatible local model on this device.
- `REMOTE_CONSENT_REQUIRED` — may be included only after request-specific review and approval.
- `PUBLIC_SAFE_REVIEWED` — human has reviewed this particular artifact for external use; publishing/sending still requires a separate action.
- `RESTRICTED_SECRET` — credentials, tokens or sensitive fields; never included in a normal prompt/context bundle.

A “public-safe” label is not a publication permission. Every external send or consequential action shows destination, account where relevant, payload preview, selected sources and exact scope. Any content/destination change invalidates its earlier approval.

## 6.4 Browser and prompt-injection controls

- No privileged JavaScript bridge exposed to arbitrary web page code.
- Keep website-origin browsing state isolated from privileged local APIs; DOM extraction returns bounded, labelled evidence only.
- Disallow `file://` access and mixed content by default; normalize and validate navigation targets; external launches require deliberate user intent.
- Web text, snippets, OCR and model output are labelled untrusted inputs. They cannot change policies, permissions or the user's approved task.
- Browser actions are allowlisted and tied to an active task. A page asking the assistant to reveal secrets or perform unrelated actions is treated as content, not instructions.

## 6.5 Platform-specific automation boundary

Android's AccessibilityService and floating overlays are powerful, sensitive integrations and are not equivalent to ordinary app permissions. Google Play policy places restrictions on accessibility-based automation and disallows general-purpose autonomous planning/execution under the ordinary automation route. Therefore Memo's first release must not depend on AccessibilityService to complete core workflows. If a narrow Android-only, user-defined deterministic workflow module is ever included, it needs a separate prominent disclosure, explicit opt-in, a policy review, a supported step allowlist and visible execution logs. It must not be presented as cross-platform automation.

On iOS, do not design the product around arbitrary control of other installed apps. Use supported share extensions, the app's own browser, native system authorization, files, notifications and the platform's permitted background APIs. Feature availability is declared in a capability registry per OS and version.

---

# 7. The “Live” environment: durable work, not fake autonomy

## 7.1 Task/run state machine

Use one canonical execution lifecycle:

`DRAFT → READY → AWAITING_APPROVAL → QUEUED → RUNNING → WAITING_FOR_USER → VERIFYING → SUCCEEDED`

Terminal/exception states include `FAILED`, `BLOCKED`, `CANCELLED`, `INTERRUPTED`, `OUTCOME_UNKNOWN` and `REVIEW_REQUIRED`. Not every task visits every state. State transitions are validated in domain code and persisted before side effects.

Each external-capable step specifies:

- capability ID and version;
- typed input and the selected source/record references;
- preconditions and expected postconditions;
- risk class and required user approval;
- timeout and retry/cancellation policy;
- an idempotency key where the target supports it;
- checkpoint and observations/proof references.

## 7.2 Execution protocol

1. Save the request and its source/context references.
2. Create and validate a plan. Mark uncertainty instead of inventing missing inputs.
3. Show the user the planned effects, destination and disclosure boundary.
4. Bind approval to the immutable action payload hash and its destination/scope.
5. Persist the run and checkpoint before invoking the runtime.
6. Execute only the approved action through a registered capability.
7. Record runtime observations, including errors, partial results, page state and timing.
8. Verify the expected postcondition. If there is no trustworthy observation, record `OUTCOME_UNKNOWN` or `UNVERIFIED` rather than success.
9. On cancellation, interruption or app restart, retain state and never replay an uncertain external side effect automatically.

## 7.3 Real-time behavior and background work

- **Foreground:** live model token streaming, browser progress, step state, cancel control, and source/evidence arrival update from actual engine events.
- **Background:** use platform schedulers only for tasks the operating system permits. Android can use WorkManager for deferrable jobs; iOS background tasks are scheduled/limited by the system. Do not promise indefinite execution while the phone is locked or Memo is suspended.
- **Reminders:** schedule locally for user-authored follow-ups and due dates, with quiet hours and per-project mute. A notification means “something needs attention,” not proof that the underlying action succeeded.
- **Remote work (later):** only when an explicitly configured Relay is needed. The payload is task-scoped, authentication is bounded, status is polled or streamed, cancellation is available, and the server returns observations—not authoritative success text.
- **Recovery:** process death marks active work interrupted. Reconcile the most recent persisted checkpoint and present Review/Resume/Discard; never silently resume a possible payment, message, submission or deletion.

## 7.4 Capability registry

Each capability describes its `id`, version, supported OSes, input/output schema, permissions, risk class, whether it reads external data, whether it creates side effects, cancel support, postcondition strategy and tests. The coordinator can request a capability; it cannot call arbitrary code or select undeclared permissions.

---

# 8. Mobile UX and visual system

## 8.1 Navigation and screen model

Use four primary destinations. Settings and model management remain reachable but do not compete with everyday work.

| Destination | Purpose | Core interactions |
|---|---|---|
| **Talk** | The primary working surface: natural conversation and voice with real context | Stream a response; interrupt; switch text/voice; pin selected context; create or open a linked task/record inline; inspect which sources were used |
| **Work** | Resume progress without rereading a chat | Today/Inbox, active projects, next actions, blocked/review tasks, upcoming follow-ups, records and entity search |
| **Browser** | A usable, visible browsing workspace tied to tasks and sources | Full-size web viewport; address/search; tabs; back/forward/reload; manual sign-in; capture current page; add page to context; ask about the current page; pause for user intervention |
| **Library** | The evidence and memory surface | Captures, files, excerpts, notes, conversations, imported transcripts and source history; full-text search; inspect provenance and privacy labels |

A small profile/settings entry exposes model setup, privacy controls, integrations, appearance, backup/restore and diagnostics. “CRM,” “Job search,” “Studio” and “Council” are not mandatory top-level tabs in v1. They are filtered views or workflows built over the same records.

## 8.2 Talk screen contract

- It must feel like a real, continuous conversation—not a separate “listen” utility or a row of tiny action buttons.
- One prominent composer supports multiline text, an integrated microphone, send, stop/cancel while generating, and accessible attachment/share capture.
- Voice mode remains in the conversation: show recording/listening/speaking states, allow interruption, and keep a readable transcript. Microphone access begins only after explicit user action unless the user later enables a clearly disclosed alternative.
- Stream only actual tokens. Model-loading/progress UI appears only while the engine is truly preparing or generating.
- When a message leads to an internal action, show a typed result card in the thread (“Task created,” “Record proposed,” “Needs review”) with a link to the persisted object. Do not force the user through a separate form for every simple command.
- Show a small, inspectable context strip (“Using: Project X · 2 sources”) and a control to manage selected context. The app does not add the entire memory automatically.

## 8.3 Mobile usability contract

- The input composer stays visible above the keyboard. Safe areas, Android IME resize/pan behavior, iOS keyboard insets, nested scroll regions and browser viewport height must be handled deliberately.
- The browser remains a full-size working surface on a phone. Address bar, page viewport and essential controls must not be trapped in a fixed tiny panel. Scrolling must be possible when the keyboard is open and when closed.
- Lists preserve scroll position on state updates; generated tokens must not yank the user away from older messages when they scroll up.
- Every visible control has a real action or is visibly disabled with a reason. Empty, loading, offline, permission-denied, failed and unverified states are first-class UI states.
- Use platform-safe touch targets: at least 48 dp on Android and approximately 44 pt on iOS. Content reflows at large text sizes. No status depends on color alone.

## 8.4 Design language

**Direction:** calm, precise, tactile, private. A personal workspace, not a neon AI dashboard. Use contrast, typography, and hierarchy rather than constant motion or heavy shadows.

### Color tokens (initial proposal)

| Token | Light | Dark | Meaning |
|---|---|---|---|
| `canvas` | `#F3F6F2` | `#101714` | Main background |
| `surface` | `#FFFFFF` | `#18211D` | Main panels/cards |
| `surfaceRaised` | `#F8FAF8` | `#202A25` | Inputs, selected panels |
| `textPrimary` | `#19242C` | `#EDF5F0` | Main content |
| `textSecondary` | `#65736F` | `#A5B7AE` | Supporting information |
| `accent` | `#276B56` | `#83D8B0` | Primary action / active state |
| `caution` | `#8B551D` | `#E6B56F` | Needs review / disclosure |
| `success` | `#176C4B` | `#70D5A3` | Verified outcome |
| `danger` | `#B02D3C` | `#FF7F88` | Failure / destructive action |
| `info` | `#245F99` | `#83BDF3` | Neutral information |

Contrast must be tested for every text/surface pair, including disabled and high-contrast states. Color must be accompanied by icon/label/text. Respect system light/dark mode and add a user override.

### Type, spacing and shape

- Start with system typefaces for native readability (San Francisco on iOS, system sans on Android) rather than shipping a decorative font prematurely.
- Suggested scale: display 30–32; screen title 24–28; card title 16–18; body 14–16; supporting text 12–14; metadata no smaller than 11–12 unless truly secondary.
- Use a 4-point spacing rhythm: 4, 8, 12, 16, 20, 24, 32. Main horizontal content gutters 20; card padding 16–20.
- Buttons and inputs use 14–16 radius; cards 18–22; larger hero surfaces 24–28. Borders are subtle and surfaces provide elevation; avoid a card-within-card everywhere.
- Icons are consistent stroke icons with labels where meaning is not universal. Use one primary action per screen/sheet, with secondary actions as text or outlined controls.

## 8.5 Motion and interaction specification

Motion explains state and hierarchy; it must never simulate work that did not happen.

| Interaction | Motion | Duration | Rule |
|---|---|---:|---|
| Press feedback | Scale to ~0.98, then spring to 1.0; optional system haptic | 90–160 ms | Only on real interactive controls; respect reduced motion |
| Destination change | Light cross-fade + 4–8 px vertical settle | 180–220 ms | Preserve scroll and keyboard state when appropriate |
| Message arrives | Natural streaming from real model events; subtle end-of-stream settle | Engine-driven | Never fabricate a typing stream if the model has not responded |
| Record/task card appears | Fade + short vertical settle after transaction commits | 180–220 ms | Show success only after persistence confirms it |
| Detail expansion | Animated size/layout transition | 180–240 ms | Preserve reading position and avoid large content jumps |
| Approval/review sheet | Bottom-sheet rise with dimmed background | 220–280 ms | Destination, scope, payload and risk remain readable before approval |
| Browser capture | Capture button state → saved confirmation; source detail sheet | 160–220 ms | Only confirm after source and provenance are persisted |
| Verified task | Semantic state change and restrained success feedback | 220–280 ms | Only after a real postcondition is observed |
| Model setup | True progress, status steps and optional skeleton | As needed | No artificial percentage; display errors and preserve the last working model |

Ambient idle animation is unnecessary for v1. A recording indicator/waveform runs only when the microphone is active. Honor OS Reduce Motion/animator duration scale. Prefer opacity and transforms, avoid heavy per-frame layout and bitmap allocation, and profile real devices before claiming frame-rate targets.

## 8.6 Key interaction patterns

- **Context chip:** selected sources/records are countable and can be inspected/removed before a model handoff.
- **Proposed change card:** clearly labels a suggestion; shows supporting excerpt; actions are Accept, Edit, Reject or Ask—no silent mutation.
- **Approval sheet:** names the exact external target/account, operation, payload, selected source context, side effect, and postcondition plan.
- **Evidence drawer:** shows where a fact came from, when captured, whether it is confirmed or inferred, and how to unlink/correct/delete it.
- **Task timeline:** chronological requests, approvals, actions, observations, pauses, retries and final verification, with retry/resume only where safe.
- **Capture intake:** preview filename/URL, MIME type, size and source app before saving; unsupported/oversized content fails cleanly without partial records.

---

# 9. End-to-end user journey: find an opportunity and add it to the workspace

This journey validates that Talk, local intelligence, evidence, job search, people/organizations, tasks and execution all work as one product.

1. **Ask naturally in Talk.** “Find suitable operations roles and keep the useful ones.” The user may add constraints in ordinary conversation. The composer supports voice or typing without leaving the chat.
2. **Interpret and clarify only what matters.** Deterministic parsing handles clear values; the selected model proposes missing structured criteria. Any critical ambiguity is asked in the same conversation. No search begins under invented assumptions.
3. **Create a durable search task.** Memo saves the goal, constraints, source references and next step before doing external work. The task is visible under Work and linked to the conversation.
4. **Gather sources.** Use user-approved search/browser integrations or a visible browser session. Each role page becomes a source with URL, timestamp, text excerpt and content hash. Memo records whether evidence was captured manually, by a connector or by a model.
5. **Create opportunity proposals.** The extractor identifies role, company, location, work mode, compensation and requirements only where the source contains evidence. Missing salary/requirements remain “unknown”; they are not filled by inference. Duplicates are candidates, not automatically merged identities.
6. **Review evidence and fit.** A result card shows matches, risks, unknowns and quote/source support. The user accepts, edits or rejects the opportunity. Accepted records connect to a shared Organization and can link a Person/contact when evidence supports it.
7. **Prepare a next action.** The user can ask for role-specific application material. The context builder selects the opportunity source and approved career/proof records. Local model generation is default where supported. A remote provider requires a fresh context preview and approval.
8. **Open Browser and prepare the application.** The visible browser is tied to the task. Memo may assist with supported, allowlisted form steps, but it pauses for sign-in, CAPTCHA, ambiguous controls and consequential actions. It never claims an application was sent merely because a submit button was tapped.
9. **Approve and verify.** Before final submission, show the exact destination and submitted payload. On approval, persist the action and approval hash, perform the bounded step, then capture a confirmation page or other postcondition. If confirmation is ambiguous, record `OUTCOME_UNKNOWN` and ask for review.
10. **Close the loop.** Link the application, company, source, materials and follow-up task. Record what happened and why. The same people/company records are available in a CRM-style view without re-entering them.

**Success condition:** the user can open the opportunity later and see the source, facts, uncertainty, application state, evidence, linked people/company, and next action. Nothing important exists only in a chat transcript.

---

# 10. Proposed codebase structure

The following is the proposed organization to implement after approval; it is not an existing structure in the archive.

```text
memo/
  apps/
    mobile/
      app/                         # route shell / navigation
      src/
        design/                    # tokens, typography, motion, primitives
        features/
          talk/
          work/
          browser/
          library/
          settings/
        core/
          domain/                  # entities, states, validation
          commands/                # explicit use cases
          policy/                  # privacy, risk, approval rules
          context/                 # retrieval and context manifests
          identity/                # aliases, dedupe candidates, merges
          journal/                 # append-only events and verification
        data/
          db/                      # SQLCipher, migrations, repositories
          crypto/                  # key and backup adapters
          sources/                 # ingestion, extraction, content hashes
        ai/
          providers/               # local/remote interfaces
          schemas/                 # proposal/response validation
          prompts/                 # versioned prompt/skill registry
          routing/                 # deterministic routing decisions
        runtime/
          registry/                # capability contracts and platform matrix
          browser/
          jobs/                    # state machine, checkpoints, cancellation
          notifications/
          share/
        platform/
          android/                 # native modules only
          ios/                     # native modules only
      tests/
        unit/
        integration/
        ui/
    relay/                         # optional future service; not v1
  packages/
    domain/                        # platform-independent domain contracts
    schemas/                       # shared validated request/event schemas
    test-fixtures/                 # source cases, prompts, migration fixtures
  docs/
    product-architecture.md
    threat-model.md
    data-dictionary.md
    runtime-capabilities.md
    release-gates.md
```

Keep domain logic outside React component trees. Put platform APIs behind narrow interfaces. Prefer feature-level view models/hooks over another monolithic Activity. Keep prompts, schemas and skill definitions versioned alongside tests.

---

# 11. Implementation plan and acceptance gates

## Phase A — foundation and design system

**Build:** Expo/native project skeleton; design tokens/components; four navigation destinations; keyboard-safe Talk composer; theme/reduced-motion/accessibility baseline; platform capability registry; CI build for Android and iOS.

**Accept when:** builds succeed on CI for both platforms; all routes render on narrow devices; keyboard never covers composer or primary browser controls; large text and screen readers are usable; no dead buttons or fake states.

## Phase B — encrypted local core

**Build:** SQLCipher boot verification; versioned migrations; encrypted key wrapper; repositories for records, relationships, sources, claims, tasks, conversations and events; atomic transactions; encrypted backup/restore; local full-text search.

**Accept when:** encryption is confirmed in built binaries; wrong/missing key and corrupted DB never destroy the last good workspace; migration and restore fixtures pass; exported secrets are absent; deleting/unlinking data has predictable semantics.

## Phase C — conversation + source graph

**Build:** persistent chat; deterministic direct commands; context builder/manifests; import/share intake; source detail/provenance; proposed record cards; user approval/edit/reject flow; basic entity search and link editor.

**Accept when:** a conversation can create a task/record and return a persisted object ID; the new object can be found outside the chat; provenance is visible; model proposals with nonmatching/missing excerpts are rejected or flagged, not committed silently.

## Phase D — local model adapters

**Build:** model catalogue/import, native LiteRT-LM adapters for supported devices, streaming/cancel, model readiness and memory errors, prompt versioning, schema-validated proposals.

**Accept when:** cold-start, warm latency, memory, thermal and cancellation are measured on real Android and iOS devices; unsupported artifacts fail safely; failure does not delete the last usable model; no remote fallback occurs automatically.

## Phase E — visible browser + controlled work

**Build:** full-size browser, address/search/navigation, tabs, manual login, active-page context, bounded inspection/capture, link-to-task, source evidence, approval sheet, event journal, final postcondition verifier.

**Accept when:** keyboard/scroll/rotation and login fallback are tested; website content cannot call privileged Memo APIs; prompt injection tests do not bypass policy; missing postcondition produces unverified/unknown, not success; app restart never replays a risky action automatically.

## Phase F — reminders, connectors and optional Relay

Only after the local core is reliable, add selected connectors one at a time, starting read-only. Add approved external writes only with a concrete user need, narrow scopes and end-to-end tests. Implement Relay and sync only after the key-sharing, conflict resolution, deletion, retention and recovery contracts are documented.

**Accept when:** connector scopes and disclosures are reviewable; credentials remain outside normal exports; remote payload preview matches the actual send; revoke disconnects the provider; retry/cancel/recovery behavior is documented and tested.

## Mandatory release gates

- `typecheck`, lint, domain unit tests, schema/migration tests and UI tests are green.
- Android and iOS release builds are produced by CI from the exact checked-in source.
- At least one real device per platform passes capture, lock/unlock, backup/restore, model smoke test (where runtime/model supported), Talk/keyboard behavior, browser evidence capture, interruption recovery, and accessibility checks.
- Every external action is approval-bound and observable; every “success” has evidence or a clearly defined deterministic local postcondition.
- Threat model and privacy copy match actual behavior; no secrets in logs/export/telemetry; all enabled permissions are disclosed in context.
- Product documentation differentiates implemented, platform-supported, device-tested and future capabilities.

---

# 12. Risks and explicit non-goals

| Risk | Response |
|---|---|
| Cross-platform UI hides different OS abilities | Capability registry reports support at runtime; no fake parity for accessibility automation, floating windows, background tasks or speech services. |
| Local model file is installed but cannot run | Compatibility metadata, checksum validation, startup/inference smoke test, preserved previous model, visible reason and graceful deterministic fallback. |
| Model hallucination becomes persisted truth | Typed proposals, evidence excerpt matching, clear review states, deterministic validation and conflict retention. |
| Browser page manipulates an agent through prompt injection | Website content is evidence only; no privileged bridge; allowlisted capability invocation; policy and approval operate outside the page. |
| Duplicate people or company records | Preserve aliases and candidate matches; evidence-backed identity resolution; reversible merges with archived source record and reassigned links. |
| External side effect is repeated after crash | Persist-before-execute, idempotency keys where possible, `INTERRUPTED`/`OUTCOME_UNKNOWN`, no automatic replay for ambiguous outcomes. |
| Encryption silently disabled by build configuration | Startup SQLCipher probe and release test; fail closed if the encrypted database implementation is absent. |
| Browser authentication differs by provider | Use supported system authentication/session or visible manual fallback; do not bypass MFA, CAPTCHA or embedded-browser restrictions. |
| Scope expands into a feature collection | Every feature must use the same record/source/task/policy contracts and demonstrate an end-to-end outcome before being promoted into primary navigation. |
| Build success is confused with production readiness | Separate CI compilation, model compatibility, device acceptance, store-policy review and security review gates. |

## Non-goals for the first release

Memo is not a generic chatbot skin, an all-purpose CRM, a job-board scraper, an autonomous web agent, a social media manager, a second-brain dumping ground, a multi-agent orchestration showcase, a desktop app, or a server-dependent product. Those are possible capability areas only if they strengthen the core loop and can meet the same evidence, privacy and execution contract.

---

# 13. Approval checkpoint

The blueprint is ready for review. Approval should cover these foundational decisions:

1. **Product identity:** one conversation-led personal execution environment with a durable, source-linked record graph behind it.
2. **Stack:** React Native + Expo prebuild/custom native builds, TypeScript domain core, SQLCipher-backed local SQLite, and narrow native model/security adapters.
3. **Privacy:** local-first with no mandatory account/backend; remote model and future sync are explicit opt-ins.
4. **Execution:** typed capability contracts, approval-bound side effects, durable runs and evidence-backed verification.
5. **Scope:** browser, source graph, tasks and local model are core; universal cross-app automation and the guest runner are deferred.

After approval, the next phase is to create the codebase structure and implement the foundation and end-to-end slice in the order above. The new project should not import multiple legacy screens wholesale, and it should not claim an unsupported capability merely because a corresponding class exists in one archive.

## Current technical references (checked 26 October 2026)

- [Expo documentation — SDK and native builds](https://docs.expo.dev/versions/latest/): React Native projects can use Expo modules; platform SDK compatibility must be pinned at implementation.
- [Expo SQLite — SQLCipher](https://docs.expo.dev/versions/latest/sdk/sqlite/): supports SQLCipher on native Android/iOS builds; explicitly verify cipher support because a missing SQLCipher build can cause the key pragma to be ignored.
- [Google AI Edge — LiteRT-LM](https://developers.google.com/edge/litert-lm): documents native Android and iOS APIs. Model artifact/runtime compatibility still needs validation per device.
- [Google Play — AccessibilityService policy](https://support.google.com/googleplay/android-developer/answer/10964491): restricts use of AccessibilityService for general automation and autonomous planning/execution.
- [Apple — BackgroundTasks](https://developer.apple.com/documentation/BackgroundTasks): background execution is governed by system scheduling and lifecycle constraints; the product must be interruptible and recoverable.
