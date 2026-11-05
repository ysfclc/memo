package app.memo.android.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EntityEditPolicyTest {
    @Test
    fun acceptsDistinctStructuredProperties() {
        val properties = linkedMapOf("Company" to "Acme", "Role" to "Growth Operations")
        assertNull(EntityEditPolicy.propertyValidationError(properties))
    }

    @Test
    fun rejectsDuplicateKeysIgnoringCase() {
        val error = EntityEditPolicy.propertyValidationError(linkedMapOf("Role" to "Ops", "role" to "Sales"))
        assertTrue(error!!.contains("unique"))
    }

    @Test
    fun rejectsEmptyKeysAndOversizedValues() {
        assertTrue(EntityEditPolicy.propertyValidationError(mapOf(" " to "value"))!!.contains("empty"))
        assertTrue(EntityEditPolicy.propertyValidationError(mapOf("notes" to "x".repeat(2_001)))!!.contains("2,000"))
    }

    @Test
    fun enforcesPropertyCountAndKeyLength() {
        val tooMany = (1..41).associate { "field$it" to "value" }
        assertEquals("An entity can have at most 40 properties.", EntityEditPolicy.propertyValidationError(tooMany))
        assertTrue(EntityEditPolicy.propertyValidationError(mapOf("k".repeat(81) to "value"))!!.contains("80"))
    }
}
