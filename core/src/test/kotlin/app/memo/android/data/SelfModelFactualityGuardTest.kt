package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SelfModelFactualityGuardTest {
    @Test fun flagsUnsupportedMetricsButKeepsNumbersGroundedInApprovedMaterial() {
        val review = SelfModelFactualityGuard.review(
            "Managed 15 partners, improved reply rate to 2.3%, and worked from 2021 to 2024.",
            listOf("Managed 15 DACH partners. Reply rate was 1.5%. Employment dates: 2021–2024.")
        )
        assertTrue(review.needsReview)
        assertTrue(review.findings.any { it.claim.contains("2.3") })
        assertFalse(review.findings.any { it.claim.contains("15") })
        assertFalse(review.findings.any { it.claim.contains("2021") })
        assertFalse(review.findings.any { it.claim.contains("2024") })
    }

    @Test fun noMetricsMeansNoNumericFlags() {
        val review = SelfModelFactualityGuard.review("Wrote an evidence-based project brief.", listOf("Project brief"))
        assertFalse(review.needsReview)
        assertEquals(0, review.checkedClaims)
    }

    @Test fun reportsRemainBoundedAndNarrowlyLabelledAsHeuristic() {
        val draft = (1..50).joinToString(" ") { "Delivered $it projects." }
        val review = SelfModelFactualityGuard.review(draft, listOf("No metrics approved"))
        assertTrue(review.findings.size <= 30)
        assertTrue(review.findings.all { it.code == "UNSUPPORTED_NUMERIC_CLAIM" })
        assertTrue(review.findings.all { it.explanation.contains("heuristic") })
    }
}
