package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ActionApprovalLedgerTest {
    private val runtime = "runtime.browser"
    private val capability = "browser.submit_prompt"
    private val destination = "https://chatgpt.com/"
    private val payload = "Exact prompt\nDo not modify this text."

    private fun proposed(): ActionLedgerTransition = ActionApprovalLedger.propose(
        snapshot = AppSnapshot(), jobId = "job-1", stepId = "step-1", runtimeId = runtime,
        capabilityId = capability, destination = destination, title = "Send prompt", payload = payload, now = 1_000L
    )

    @Test fun browserEnvelopeRequiresCleanHttpsDestination() {
        fun attempt(url: String) = ActionApprovalLedger.propose(
            snapshot = AppSnapshot(), jobId = "job-1", stepId = "step-1", runtimeId = runtime,
            capabilityId = capability, destination = url, title = "Send prompt", payload = payload, now = 1_000L
        )
        assertTrue(attempt(destination).accepted)
        assertFalse(attempt("http://chatgpt.com/").accepted)
        assertFalse(attempt("https://user:pass@chatgpt.com/").accepted)
        assertFalse(attempt("https://chatgpt.com/?session=secret").accepted)
        assertFalse(attempt("https://chatgpt.com/#fragment").accepted)
    }

    @Test fun contentHashBindsRuntimeCapabilityDestinationAndExactPayload() {
        val original = ActionApprovalLedger.contentHash(runtime, capability, destination, payload)
        assertTrue(original != ActionApprovalLedger.contentHash(runtime, capability, "https://claude.ai/", payload))
        assertTrue(original != ActionApprovalLedger.contentHash(runtime, "browser.navigate", destination, payload))
        assertTrue(original != ActionApprovalLedger.contentHash(runtime, capability, destination, payload + " "))
        assertTrue(original != ActionApprovalLedger.contentHash("runtime.native", capability, destination, payload))
    }

    @Test fun happyPathRequiresHumanApprovalAndEvidenceBackedCompletion() {
        val proposal = proposed()
        assertTrue(proposal.accepted)
        val record = assertNotNullAndReturn(proposal.action)
        assertEquals("PENDING", record.status)

        val approved = ActionApprovalLedger.approve(proposal.snapshot, record.id, record.contentHash, now = 1_100L)
        assertTrue(approved.accepted)
        assertEquals("APPROVED", approved.action?.status)

        val started = ActionApprovalLedger.beginExecution(
            approved.snapshot, record.id, runtime, capability, destination, payload, now = 1_200L
        )
        assertTrue(started.accepted)
        assertEquals("IN_PROGRESS", started.action?.status)

        val withoutEvidence = ActionApprovalLedger.completeVerified(started.snapshot, record.id, "evidence:missing", now = 1_300L)
        assertTrue(withoutEvidence.accepted) // accepted transition to NEEDS_REVIEW, not a false completion
        assertEquals("NEEDS_REVIEW", withoutEvidence.action?.status)

        val nextProposal = proposed()
        val nextAction = assertNotNullAndReturn(nextProposal.action)
        val nextApproval = ActionApprovalLedger.approve(nextProposal.snapshot, nextAction.id, nextAction.contentHash, now = 1_100L)
        val nextStarted = ActionApprovalLedger.beginExecution(nextApproval.snapshot, nextAction.id, runtime, capability, destination, payload, now = 1_200L)
        val withEvidence = nextStarted.snapshot.copy(evidence = listOf(EvidenceRecord(
            id = "response-1", taskId = "job-1", title = "Observed provider response", sourceUrl = destination,
            kind = "FRONTIER_BROWSER_RESPONSE", content = "A stable response is visible.",
            summary = "Observed in the browser workflow."
        )))
        val verified = ActionApprovalLedger.completeVerified(withEvidence, nextAction.id, "evidence:response-1", now = 1_400L)
        assertTrue(verified.accepted)
        assertEquals("COMPLETED", verified.action?.status)
        assertEquals("evidence:response-1", verified.action?.evidenceRef)
        assertTrue(ActionApprovalLedger.verifyAuditChain(verified.snapshot.actionAuditEvents))
    }

    @Test fun changedPayloadOrDestinationCannotReuseApproval() {
        val proposal = proposed()
        val action = assertNotNullAndReturn(proposal.action)
        val approved = ActionApprovalLedger.approve(proposal.snapshot, action.id, action.contentHash, now = 1_100L)
        val changedPayload = ActionApprovalLedger.beginExecution(
            approved.snapshot, action.id, runtime, capability, destination, payload + " changed", now = 1_200L
        )
        assertFalse(changedPayload.accepted)
        assertEquals("INVALIDATED", changedPayload.action?.status)

        val second = proposed()
        val secondAction = assertNotNullAndReturn(second.action)
        val secondApproved = ActionApprovalLedger.approve(second.snapshot, secondAction.id, secondAction.contentHash, now = 1_100L)
        val changedDestination = ActionApprovalLedger.beginExecution(
            secondApproved.snapshot, secondAction.id, runtime, capability, "https://chatgpt.com/c/another", payload, now = 1_200L
        )
        assertFalse(changedDestination.accepted)
        assertEquals("INVALIDATED", changedDestination.action?.status)
    }

    @Test fun tamperedStoredPayloadAndTamperedAuditEventFailClosed() {
        val proposal = proposed()
        val action = assertNotNullAndReturn(proposal.action)
        val approved = ActionApprovalLedger.approve(proposal.snapshot, action.id, action.contentHash, now = 1_100L)
        val mutated = approved.snapshot.copy(actionApprovals = approved.snapshot.actionApprovals.map {
            if (it.id == action.id) it.copy(payload = "silent payload replacement") else it
        })
        val refused = ActionApprovalLedger.beginExecution(mutated, action.id, runtime, capability, destination, payload, now = 1_200L)
        assertFalse(refused.accepted)
        assertEquals("APPROVED", refused.action?.status) // corrupted state is blocked, never dispatched
        assertFalse(ActionApprovalLedger.verifyLedger(mutated))

        val clean = proposed().snapshot
        assertTrue(ActionApprovalLedger.verifyAuditChain(clean.actionAuditEvents))
        val tampered = clean.copy(actionAuditEvents = clean.actionAuditEvents.mapIndexed { index, event ->
            if (index == 0) event.copy(details = "edited audit history") else event
        })
        assertFalse(ActionApprovalLedger.verifyAuditChain(tampered.actionAuditEvents))
        val refusedApproval = ActionApprovalLedger.approve(tampered, action.id, action.contentHash, now = 1_100L)
        assertFalse(refusedApproval.accepted)
    }

    @Test fun restartAndImportInvalidatePortableApprovalsAndFlagInFlightAction() {
        val proposal = proposed()
        val action = assertNotNullAndReturn(proposal.action)
        val imported = ActionApprovalLedger.invalidateImportedApprovals(proposal.snapshot, now = 1_050L)
        assertEquals("INVALIDATED", imported.actionApprovals.single().status)
        assertEquals("", imported.actionApprovals.single().approvalHash)
        assertTrue(ActionApprovalLedger.verifyAuditChain(imported.actionAuditEvents))

        val approved = ActionApprovalLedger.approve(proposal.snapshot, action.id, action.contentHash, now = 1_100L)
        val started = ActionApprovalLedger.beginExecution(approved.snapshot, action.id, runtime, capability, destination, payload, now = 1_200L)
        val recovered = ActionApprovalLedger.recoverAfterRestart(started.snapshot, now = 1_300L)
        assertEquals("NEEDS_REVIEW", recovered.actionApprovals.single().status)
        assertFalse(recovered.actionApprovals.single().approvalHash.isNotBlank())
        assertTrue(ActionApprovalLedger.verifyAuditChain(recovered.actionAuditEvents))
    }

    @Test fun schemaFourRoundTripsApprovalAndRejectsTamperedPayload() {
        val proposal = proposed()
        val record = assertNotNullAndReturn(proposal.action)
        val approved = ActionApprovalLedger.approve(proposal.snapshot, record.id, record.contentHash, now = 1_100L)
        val restored = AppSnapshot.fromJson(approved.snapshot.toJson())
        assertEquals(6, org.json.JSONObject(approved.snapshot.toJson()).optInt("schemaVersion"))
        assertEquals("APPROVED", restored.actionApprovals.single().status)
        assertEquals(record.contentHash, restored.actionApprovals.single().contentHash)
        assertTrue(ActionApprovalLedger.verifyAuditChain(restored.actionAuditEvents))

        val json = org.json.JSONObject(approved.snapshot.toJson())
        json.getJSONArray("actionApprovals").getJSONObject(0).put("payload", "tampered")
        var rejected = false
        try {
            AppSnapshot.fromJson(json.toString())
        } catch (_: IllegalArgumentException) {
            rejected = true
        } catch (_: org.json.JSONException) {
            rejected = true
        }
        assertTrue("Import must reject a payload that does not match the action content hash", rejected)

        val forgedStatus = org.json.JSONObject(approved.snapshot.toJson())
        forgedStatus.getJSONArray("actionApprovals").getJSONObject(0).put("status", "COMPLETED")
        var statusRejected = false
        try {
            AppSnapshot.fromJson(forgedStatus.toString())
        } catch (_: IllegalArgumentException) {
            statusRejected = true
        } catch (_: org.json.JSONException) {
            statusRejected = true
        }
        assertTrue("Import must not trust a completion status that has no matching audit transition", statusRejected)
    }

    @Test fun ledgerRejectsOrphanedAuditEventsAndExpiredApprovals() {
        val proposal = proposed()
        val action = assertNotNullAndReturn(proposal.action)
        val orphaned = proposal.snapshot.copy(actionApprovals = emptyList())
        assertFalse(ActionApprovalLedger.verifyLedger(orphaned))

        val expired = ActionApprovalLedger.approve(proposal.snapshot, action.id, action.contentHash, now = action.expiresAt + 1)
        assertFalse(expired.accepted)
        assertEquals("EXPIRED", expired.action?.status)
        assertTrue(ActionApprovalLedger.verifyLedger(expired.snapshot))

        val rejectedProposal = proposed()
        val rejectedAction = assertNotNullAndReturn(rejectedProposal.action)
        val rejected = ActionApprovalLedger.reject(rejectedProposal.snapshot, rejectedAction.id, "User declined", now = 1_050L)
        assertTrue(rejected.accepted)
        assertEquals("REJECTED", rejected.action?.status)
        assertFalse(ActionApprovalLedger.beginExecution(rejected.snapshot, rejectedAction.id, runtime, capability, destination, payload, now = 1_060L).accepted)
    }

    @Test fun oldSchemaStillImportsWithoutApprovalRecords() {
        val restored = AppSnapshot.fromJson("""{"schemaVersion":3,"themeMode":"DARK","tasks":[],"evidence":[],"activity":[],"councilRuns":[],"conversations":[],"entities":[],"relationships":[],"jobs":[]}""")
        assertTrue(restored.actionApprovals.isEmpty())
        assertTrue(restored.actionAuditEvents.isEmpty())
    }

    private fun assertNotNullAndReturn(record: ActionApprovalRecord?): ActionApprovalRecord {
        assertNotNull(record)
        return record!!
    }
}
