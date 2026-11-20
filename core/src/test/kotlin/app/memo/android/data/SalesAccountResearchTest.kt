package app.memo.android.data

import org.junit.Assert.*
import org.junit.Test

class SalesAccountResearchTest {
    private val evidence = EvidenceRecord(
        id = "evidence-1", title = "Company capabilities page",
        sourceUrl = "https://www.example.com/capabilities", kind = "ACCOUNT_RESEARCH_SOURCE",
        content = "CNC milling and turning services for industrial customers.",
        summary = "The company publicly describes CNC services.",
        capturedAt = 1_800_000_000_000L
    )

    private fun completeProfile() = SalesAccountProfile(
        website = "https://www.example.com", location = "Krakow, Poland",
        segment = SalesAccountSegment.CNC_METALWORKING, employeeRange = "10-50",
        accessRoute = SalesAccessRoute.WARM_INTRODUCTION,
        researchState = SalesAccountResearchState.READY_FOR_REVIEW,
        assessments = IcpFactor.entries.map { IcpFactorAssessment(it, it.weight / 2, "Supported by reviewed public evidence.", listOf(evidence.id)) }
    )

    @Test fun weightsCoverExactlyOneHundredPoints() { assertEquals(100, IcpFactor.entries.sumOf { it.weight }) }

    @Test fun unknownFactorsDoNotReceivePointsOrPretendToBeAssessed() {
        val score = SalesAccountResearch.score(SalesAccountProfile("https://example.com", "Unknown", SalesAccountSegment.OTHER))
        assertEquals(0, score.pointsOutOf100); assertEquals(0, score.assessedWeight)
        assertEquals(100, score.unassessedWeight); assertNull(score.percentOfAssessedFactors)
        assertEquals(100, score.bestPossiblePointsOutOf100)
    }

    @Test fun scoreSeparatesEarnedPointsCoverageAndKnownFactorNormalisation() {
        val profile = SalesAccountProfile("https://example.com", "Unknown", SalesAccountSegment.CNC_METALWORKING,
            assessments = listOf(IcpFactorAssessment(IcpFactor.SEGMENT_FIT, 10, "CNC supplier evidence", listOf(evidence.id))))
        val score = SalesAccountResearch.score(profile)
        assertEquals(10, score.pointsOutOf100); assertEquals(20, score.assessedWeight)
        assertEquals(80, score.unassessedWeight); assertEquals(50, score.percentOfAssessedFactors)
        assertEquals(90, score.bestPossiblePointsOutOf100)
    }

    @Test fun scoredFactorRequiresRationaleAndExistingSourcedEvidence() {
        val factor = IcpFactorAssessment(IcpFactor.SEGMENT_FIT, 10, "", listOf("missing"))
        val profile = SalesAccountProfile("https://example.com", "Krakow", SalesAccountSegment.CNC_METALWORKING, assessments = listOf(factor))
        val errors = SalesAccountResearch.validate(profile, emptyList())
        assertTrue(errors.any { it.contains("rationale") }); assertTrue(errors.any { it.contains("missing evidence") })
        assertTrue(SalesAccountResearch.validate(profile.copy(assessments = listOf(factor.copy(evidenceIds = emptyList()))), emptyList())
            .any { it.contains("at least one linked source") })
    }

    @Test fun readyForReviewCannotContainUnknownFactors() {
        val profile = SalesAccountProfile("https://example.com", "Krakow", SalesAccountSegment.CNC_METALWORKING,
            researchState = SalesAccountResearchState.READY_FOR_REVIEW)
        assertTrue(SalesAccountResearch.validate(profile, emptyList()).any { it.contains("Ready for review requires all ICP factors") })
    }

    @Test fun disqualificationNeedsReasonAndRemainsSeparateFromScore() {
        val profile = completeProfile().copy(researchState = SalesAccountResearchState.DISQUALIFIED)
        assertTrue(SalesAccountResearch.validate(profile, listOf(evidence)).any { it.contains("needs a reason") })
        assertTrue(SalesAccountResearch.score(profile.copy(disqualificationReason = "No relevant B2B quotation process.")).isDisqualified)
    }

    @Test fun profileRoundTripsThroughExistingOrganizationProperties() {
        val profile = completeProfile()
        assertEquals(profile, SalesAccountResearch.fromProperties(SalesAccountResearch.toProperties(profile)))
        assertTrue(SalesAccountResearch.toProperties(profile).size <= 40)
    }

    @Test fun futureProfileVersionIsNotMisread() {
        assertNull(SalesAccountResearch.fromProperties(mapOf(SalesAccountResearch.PROFILE_VERSION_KEY to "999")))
    }

    @Test fun duplicateDetectionUsesDomainAndNameWithoutChangingRecords() {
        val existing = EntityRecord(id = "org-1", type = "ORGANIZATION", title = "Example Manufacturing Sp. z o.o.",
            reviewStatus = "APPROVED", properties = mapOf("website" to "https://www.EXAMPLE.com/contact"))
        val matches = SalesAccountResearch.findPotentialDuplicates("Example Manufacturing", completeProfile(), listOf(existing))
        assertEquals(1, matches.size); assertTrue(matches.single().reason.startsWith("Same company website domain"))
        assertEquals("https://www.EXAMPLE.com/contact", existing.properties["website"])
    }

    @Test fun archivedDuplicatesAreNotSuggestedAsActiveCandidates() {
        val rejected = EntityRecord("org-1", "ORGANIZATION", "Example Manufacturing", reviewStatus = "REJECTED")
        val merged = EntityRecord("org-2", "ORGANIZATION", "Example Manufacturing", reviewStatus = "MERGED")
        assertTrue(SalesAccountResearch.findPotentialDuplicates("Example Manufacturing",
            completeProfile(), listOf(rejected, merged)).isEmpty())
    }

    @Test fun invalidScoringRangeFailsClosed() {
        val profile = SalesAccountProfile("https://example.com", "Krakow", SalesAccountSegment.CNC_METALWORKING,
            assessments = listOf(IcpFactorAssessment(IcpFactor.SEGMENT_FIT, 21, "Too high", listOf(evidence.id))))
        assertTrue(SalesAccountResearch.validate(profile, listOf(evidence)).any { it.contains("between 0 and 20") })
        assertTrue(runCatching { SalesAccountResearch.score(profile) }.isFailure)
    }
}
