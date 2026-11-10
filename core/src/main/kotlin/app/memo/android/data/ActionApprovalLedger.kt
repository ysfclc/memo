package app.memo.android.data

import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import java.net.URI
import java.security.MessageDigest

/** The action's exact outbound envelope. A material change must invalidate the approval. */
data class ActionApprovalRecord(
    val id: String = newId(),
    val jobId: String = "",
    val stepId: String = "",
    val runtimeId: String,
    val capabilityId: String,
    val destination: String,
    val title: String,
    val payload: String,
    val contentHash: String,
    val risk: String = "EXTERNAL_WRITE",
    val status: String = "PENDING",
    val createdAt: Long = nowMillis(),
    val expiresAt: Long = createdAt + 10 * 60 * 1000L,
    val approvedAt: Long = 0L,
    val approvalHash: String = "",
    val startedAt: Long = 0L,
    val completedAt: Long = 0L,
    val evidenceRef: String = "",
    val resolution: String = ""
)

data class ActionAuditEvent(
    val id: String = newId(),
    val sequence: Long,
    val at: Long,
    val actor: String,
    val eventType: String,
    val actionId: String,
    val runtimeId: String,
    val destination: String,
    val contentHash: String,
    val status: String,
    val previousHash: String,
    val eventHash: String,
    val details: String = ""
)

data class ActionLedgerTransition(
    val snapshot: AppSnapshot,
    val action: ActionApprovalRecord?,
    val accepted: Boolean,
    val message: String
)

/** Deterministic approval and evidence boundary for external actions. This object never executes a tool. */
object ActionApprovalLedger {
    const val MAX_ACTIONS = 200
    const val MAX_EVENTS = 4_000
    const val MAX_PAYLOAD_CHARS = 24_000
    private const val GENESIS = "GENESIS"

    /** Length-prefixing prevents separator collisions between destination, capability and payload fields. */
    fun contentHash(runtimeId: String, capabilityId: String, destination: String, payload: String): String {
        val output = ByteArrayOutputStream()
        DataOutputStream(output).use { stream ->
            listOf(runtimeId, capabilityId, destination, payload).forEach { field ->
                val bytes = field.toByteArray(Charsets.UTF_8)
                stream.writeInt(bytes.size)
                stream.write(bytes)
            }
        }
        return sha256(output.toByteArray())
    }

    fun propose(
        snapshot: AppSnapshot,
        jobId: String,
        stepId: String,
        runtimeId: String,
        capabilityId: String,
        destination: String,
        title: String,
        payload: String,
        risk: String = "EXTERNAL_WRITE",
        now: Long = nowMillis()
    ): ActionLedgerTransition {
        val validation = validateEnvelope(runtimeId, capabilityId, destination, title, payload)
        if (validation != null) return ActionLedgerTransition(snapshot, null, false, validation)
        if (snapshot.actionApprovals.size >= MAX_ACTIONS) return ActionLedgerTransition(snapshot, null, false, "Action approval limit reached. Export the workspace before clearing old action history.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, null, false, "The action audit history is full. Export before continuing.")
        if (!verifyLedger(snapshot)) return ActionLedgerTransition(snapshot, null, false, "The action ledger or audit chain failed verification. External execution is blocked.")
        val contentHash = contentHash(runtimeId, capabilityId, destination, payload)
        val record = ActionApprovalRecord(
            jobId = jobId.take(128), stepId = stepId.take(128), runtimeId = runtimeId,
            capabilityId = capabilityId, destination = destination, title = title.trim().take(240),
            payload = payload, contentHash = contentHash, risk = risk.take(40), createdAt = now,
            expiresAt = now + 10 * 60 * 1000L
        )
        val withRecord = snapshot.copy(actionApprovals = (listOf(record) + snapshot.actionApprovals).take(MAX_ACTIONS))
        return ActionLedgerTransition(appendEvent(withRecord, record, "ACTION_PROPOSED", "SYSTEM", "Exact outbound payload staged for review.", now), record, true, "Exact action proposal saved for review.")
    }

    fun approve(snapshot: AppSnapshot, actionId: String, displayedHash: String, now: Long = nowMillis()): ActionLedgerTransition {
        val record = snapshot.actionApprovals.firstOrNull { it.id == actionId }
            ?: return ActionLedgerTransition(snapshot, null, false, "This action no longer exists.")
        if (!verifyLedger(snapshot)) return blocked(snapshot, record, "The action ledger or audit chain failed verification. External execution is blocked.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, record, false, "The action audit history is full. Export before continuing.")
        if (record.status != "PENDING") return ActionLedgerTransition(snapshot, record, false, "This action is ${record.status.lowercase().replace('_', ' ')} and cannot be approved again.")
        if (now > record.expiresAt) {
            val expired = record.copy(status = "EXPIRED", resolution = "Approval window expired.")
            val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) expired else it })
            return ActionLedgerTransition(appendEvent(updated, expired, "APPROVAL_EXPIRED", "SYSTEM", "Approval was not granted before expiry.", now), expired, false, "The approval expired. Prepare the action again to review its current contents.")
        }
        val computed = contentHash(record.runtimeId, record.capabilityId, record.destination, record.payload)
        if (!constantTimeEquals(record.contentHash, computed) || !constantTimeEquals(record.contentHash, displayedHash)) {
            val invalid = record.copy(status = "INVALIDATED", resolution = "Displayed or stored content did not match the proposed content hash.")
            val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) invalid else it })
            return ActionLedgerTransition(appendEvent(updated, invalid, "APPROVAL_INVALIDATED", "SYSTEM", "Content hash mismatch; no action was authorized.", now), invalid, false, "The action changed or the reviewed content did not match. It was invalidated and must be prepared again.")
        }
        val approved = record.copy(status = "APPROVED", approvedAt = now, approvalHash = displayedHash)
        val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) approved else it })
        return ActionLedgerTransition(appendEvent(updated, approved, "ACTION_APPROVED", "HUMAN", "Human approval bound to the exact content hash.", now), approved, true, "Approved. The approval is bound to this exact destination and payload.")
    }

    fun reject(snapshot: AppSnapshot, actionId: String, reason: String = "Rejected by user.", now: Long = nowMillis()): ActionLedgerTransition {
        val record = snapshot.actionApprovals.firstOrNull { it.id == actionId }
            ?: return ActionLedgerTransition(snapshot, null, false, "This action no longer exists.")
        if (!verifyLedger(snapshot)) return blocked(snapshot, record, "The action ledger or audit chain failed verification. External execution is blocked.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, record, false, "The action audit history is full. Export before continuing.")
        if (record.status != "PENDING") return ActionLedgerTransition(snapshot, record, false, "Only a pending action can be rejected.")
        val rejected = record.copy(status = "REJECTED", resolution = reason.trim().take(400).ifBlank { "Rejected by user." })
        val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) rejected else it })
        return ActionLedgerTransition(appendEvent(updated, rejected, "ACTION_REJECTED", "HUMAN", rejected.resolution, now), rejected, true, "Action rejected. Nothing was sent.")
    }

    /** Call immediately before the external side effect. Approval is one-use and bound to runtime, capability, destination and payload. */
    fun beginExecution(
        snapshot: AppSnapshot,
        actionId: String,
        runtimeId: String,
        capabilityId: String,
        destination: String,
        payload: String,
        now: Long = nowMillis()
    ): ActionLedgerTransition {
        val record = snapshot.actionApprovals.firstOrNull { it.id == actionId }
            ?: return ActionLedgerTransition(snapshot, null, false, "No approval exists for this action.")
        if (!verifyLedger(snapshot)) return blocked(snapshot, record, "The action ledger or audit chain failed verification. External execution is blocked.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, record, false, "The action audit history is full. Export before continuing.")
        if (record.status != "APPROVED") return ActionLedgerTransition(snapshot, record, false, "This action has no current unused human approval.")
        val actualHash = contentHash(runtimeId, capabilityId, destination, payload)
        val storedEnvelopeHash = contentHash(record.runtimeId, record.capabilityId, record.destination, record.payload)
        val valid = now <= record.expiresAt && record.runtimeId == runtimeId && record.capabilityId == capabilityId &&
            record.destination == destination && record.payload == payload &&
            constantTimeEquals(storedEnvelopeHash, record.contentHash) && constantTimeEquals(actualHash, record.contentHash) &&
            constantTimeEquals(record.approvalHash, record.contentHash)
        if (!valid) {
            val invalid = record.copy(status = "INVALIDATED", resolution = "Execution envelope differed from the approved envelope or expired.")
            val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) invalid else it })
            return ActionLedgerTransition(appendEvent(updated, invalid, "APPROVAL_INVALIDATED", "SYSTEM", "Execution refused because the approved envelope no longer matches.", now), invalid, false, "Execution blocked: the destination, payload or approval window changed. Review a fresh proposal.")
        }
        val started = record.copy(status = "IN_PROGRESS", startedAt = now)
        val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) started else it })
        return ActionLedgerTransition(appendEvent(updated, started, "ACTION_STARTED", "RUNTIME", "Approved action entered execution; success is not yet established.", now), started, true, "Approved action entered execution.")
    }

    /** A model's own statement is not evidence. Completion requires a concrete observed postcondition reference. */
    fun completeVerified(snapshot: AppSnapshot, actionId: String, evidenceRef: String, now: Long = nowMillis()): ActionLedgerTransition {
        val record = snapshot.actionApprovals.firstOrNull { it.id == actionId }
            ?: return ActionLedgerTransition(snapshot, null, false, "This action no longer exists.")
        if (!verifyLedger(snapshot)) return blocked(snapshot, record, "The action ledger or audit chain failed verification. External execution is blocked.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, record, false, "The action audit history is full. Export before continuing.")
        if (record.status != "IN_PROGRESS") return ActionLedgerTransition(snapshot, record, false, "Only an in-progress action can be verified.")
        val storedEnvelopeHash = contentHash(record.runtimeId, record.capabilityId, record.destination, record.payload)
        if (!constantTimeEquals(storedEnvelopeHash, record.contentHash)) {
            return ActionLedgerTransition(snapshot, record, false, "The stored action payload failed integrity verification. External execution is blocked.")
        }
        val reference = evidenceRef.trim()
        val evidenceId = reference.removePrefix("evidence:")
        val evidence = snapshot.evidence.firstOrNull { it.id == evidenceId }
        if (!reference.startsWith("evidence:") || evidenceId.isBlank() || evidence == null || evidence.taskId != record.jobId) {
            return markNeedsReview(snapshot, actionId, "No matching saved evidence record for this job proves the postcondition.", now)
        }
        val completed = record.copy(status = "COMPLETED", completedAt = now, evidenceRef = reference.take(2_048), resolution = "Verified postcondition recorded.")
        val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) completed else it })
        return ActionLedgerTransition(appendEvent(updated, completed, "ACTION_VERIFIED", "RUNTIME", "Postcondition evidence: ${completed.evidenceRef}", now), completed, true, "Action marked complete from recorded evidence.")
    }

    /** Unknown external outcome is never automatically replayed. */
    fun markNeedsReview(snapshot: AppSnapshot, actionId: String, reason: String, now: Long = nowMillis()): ActionLedgerTransition {
        val record = snapshot.actionApprovals.firstOrNull { it.id == actionId }
            ?: return ActionLedgerTransition(snapshot, null, false, "This action no longer exists.")
        if (!verifyLedger(snapshot)) return blocked(snapshot, record, "The action ledger or audit chain failed verification. External execution is blocked.")
        if (snapshot.actionAuditEvents.size >= MAX_EVENTS) return ActionLedgerTransition(snapshot, record, false, "The action audit history is full. Export before continuing.")
        if (record.status !in setOf("IN_PROGRESS", "APPROVED")) return ActionLedgerTransition(snapshot, record, false, "This action cannot transition to review from ${record.status}.")
        val review = record.copy(status = "NEEDS_REVIEW", resolution = reason.trim().take(400).ifBlank { "Outcome is uncertain; review before retrying." })
        val updated = snapshot.copy(actionApprovals = snapshot.actionApprovals.map { if (it.id == actionId) review else it })
        return ActionLedgerTransition(appendEvent(updated, review, "ACTION_NEEDS_REVIEW", "SYSTEM", review.resolution, now), review, true, "Outcome is uncertain. Memo will not replay this action automatically.")
    }

    fun invalidateImportedApprovals(snapshot: AppSnapshot, now: Long = nowMillis()): AppSnapshot {
        var next = snapshot
        snapshot.actionApprovals.filter { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS") }.forEach { old ->
            val invalid = old.copy(
                status = if (old.status == "IN_PROGRESS") "NEEDS_REVIEW" else "INVALIDATED",
                approvalHash = "",
                resolution = "Imported action approvals are not portable; rebuild and review the action on this device."
            )
            next = next.copy(actionApprovals = next.actionApprovals.map { if (it.id == old.id) invalid else it })
            next = appendEvent(next, invalid, "IMPORTED_APPROVAL_INVALIDATED", "SYSTEM", invalid.resolution, now)
        }
        return next
    }

    /** Pending or approved actions cannot survive process death because the live browser/app context is gone. */
    fun recoverAfterRestart(snapshot: AppSnapshot, now: Long = nowMillis()): AppSnapshot {
        var next = snapshot
        snapshot.actionApprovals.filter { it.status in setOf("PENDING", "APPROVED", "IN_PROGRESS") }.forEach { old ->
            val wasRunning = old.status == "IN_PROGRESS"
            val recovered = old.copy(
                status = if (wasRunning) "NEEDS_REVIEW" else "INVALIDATED",
                approvalHash = "",
                resolution = if (wasRunning) "Memo restarted during an external action. Verify the destination before deciding whether to retry." else "Memo restarted and the browser session is no longer bound to this approval. Review a newly prepared action."
            )
            next = next.copy(actionApprovals = next.actionApprovals.map { if (it.id == old.id) recovered else it })
            next = appendEvent(next, recovered, if (wasRunning) "ACTION_INTERRUPTED_ON_RESTART" else "ACTION_APPROVAL_INVALIDATED_ON_RESTART", "SYSTEM", recovered.resolution, now)
        }
        return next
    }

    fun verifyAuditChain(events: List<ActionAuditEvent>): Boolean {
        if (events.size > MAX_EVENTS) return false
        var previous = GENESIS
        var expectedSequence = 1L
        for (event in events) {
            if (event.sequence != expectedSequence || event.previousHash != previous) return false
            val computed = eventHash(event.id, event.sequence, event.at, event.actor, event.eventType, event.actionId, event.runtimeId, event.destination, event.contentHash, event.status, event.previousHash, event.details)
            if (!constantTimeEquals(computed, event.eventHash)) return false
            previous = event.eventHash
            expectedSequence++
        }
        return true
    }

    /** Confirms each retained action's current status and envelope agree with its latest chained event. */
    fun verifyLedger(snapshot: AppSnapshot): Boolean {
        if (snapshot.actionApprovals.size > MAX_ACTIONS || !verifyAuditChain(snapshot.actionAuditEvents)) return false
        val actionIds = snapshot.actionApprovals.map { it.id }
        if (actionIds.toSet().size != actionIds.size) return false
        val byId = snapshot.actionApprovals.associateBy { it.id }
        // No orphaned audit event is accepted: a malformed record must not be silently dropped on import
        // while its events continue to look like a complete ledger.
        if (snapshot.actionAuditEvents.any { it.actionId !in byId }) return false
        for (action in snapshot.actionApprovals) {
            if (!constantTimeEquals(action.contentHash, contentHash(action.runtimeId, action.capabilityId, action.destination, action.payload))) return false
            val events = snapshot.actionAuditEvents.filter { it.actionId == action.id }
            if (events.isEmpty() || events.first().eventType != "ACTION_PROPOSED") return false
            if (events.any { it.runtimeId != action.runtimeId || it.destination != action.destination || it.contentHash != action.contentHash }) return false
            val last = events.last()
            if (last.status != action.status || last.runtimeId != action.runtimeId || last.destination != action.destination || last.contentHash != action.contentHash) return false
        }
        return true
    }

    private fun blocked(snapshot: AppSnapshot, record: ActionApprovalRecord, message: String): ActionLedgerTransition =
        ActionLedgerTransition(snapshot, record, false, message)

    private fun appendEvent(snapshot: AppSnapshot, action: ActionApprovalRecord, type: String, actor: String, details: String, now: Long): AppSnapshot {
        require(snapshot.actionAuditEvents.size < MAX_EVENTS) { "Action audit history is full. Export the workspace before running more external actions." }
        require(verifyAuditChain(snapshot.actionAuditEvents)) { "The action audit chain failed verification. External execution is blocked." }
        val previous = snapshot.actionAuditEvents.lastOrNull()?.eventHash ?: GENESIS
        val sequence = (snapshot.actionAuditEvents.lastOrNull()?.sequence ?: 0L) + 1L
        val safeDetails = details.trim().take(1_000)
        val eventId = newId()
        val hash = eventHash(eventId, sequence, now, actor, type, action.id, action.runtimeId, action.destination, action.contentHash, action.status, previous, safeDetails)
        val event = ActionAuditEvent(
            id = eventId, sequence = sequence, at = now, actor = actor, eventType = type, actionId = action.id,
            runtimeId = action.runtimeId, destination = action.destination, contentHash = action.contentHash, status = action.status,
            previousHash = previous, eventHash = hash, details = safeDetails
        )
        return snapshot.copy(actionAuditEvents = snapshot.actionAuditEvents + event)
    }

    private fun validateEnvelope(runtimeId: String, capabilityId: String, destination: String, title: String, payload: String): String? {
        if (runtimeId.isBlank()) return "Choose an authorized runtime."
        if (capabilityId.isBlank()) return "The action capability is missing."
        if (destination.isBlank() || destination.length > 2_048) return "The action destination is missing or too long."
        if (runtimeId == "runtime.browser") {
            val uri = runCatching { URI(destination) }.getOrNull()
            if (uri == null || !uri.isAbsolute || !uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
                uri.rawUserInfo != null || uri.rawQuery != null || uri.rawFragment != null) {
                return "Browser actions require a clean HTTPS origin/path without credentials, query or fragment."
            }
        }
        if (title.isBlank()) return "Give the pending action a clear title."
        if (payload.isBlank()) return "The exact outbound content is empty."
        if (payload.length > MAX_PAYLOAD_CHARS) return "The outbound content exceeds the 24,000-character action limit."
        return null
    }

    private fun eventHash(id: String, sequence: Long, at: Long, actor: String, type: String, actionId: String, runtimeId: String, destination: String, payloadHash: String, status: String, previousHash: String, details: String): String {
        val bytes = ByteArrayOutputStream()
        DataOutputStream(bytes).use { stream ->
            listOf(id, sequence.toString(), at.toString(), actor, type, actionId, runtimeId, destination, payloadHash, status, previousHash, details).forEach { value ->
                val data = value.toByteArray(Charsets.UTF_8)
                stream.writeInt(data.size)
                stream.write(data)
            }
        }
        return sha256(bytes.toByteArray())
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") {
        "%02x".format(it.toInt() and 0xff)
    }

    private fun constantTimeEquals(left: String, right: String): Boolean {
        val a = left.toByteArray(Charsets.UTF_8)
        val b = right.toByteArray(Charsets.UTF_8)
        return MessageDigest.isEqual(a, b)
    }
}
