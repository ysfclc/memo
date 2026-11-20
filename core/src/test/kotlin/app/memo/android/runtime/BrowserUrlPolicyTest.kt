package app.memo.android.runtime

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserUrlPolicyTest {
    @Test
    fun normalizesHttpsUrlWithoutChangingPathAndQuery() {
        assertEquals(
            "https://example.com/research?q=Memo",
            BrowserUrlPolicy.normalizeHttpsUrl("HTTPS://Example.COM/research?q=Memo")
        )
    }

    @Test
    fun rejectsHttpCredentialsLocalIpAndNonstandardPortUrls() {
        listOf(
            "http://example.com",
            "https://user:password@example.com",
            "https://localhost",
            "https://router.local",
            "https://192.168.1.1",
            "https://example.com:8443"
        ).forEach { assertEquals(null, BrowserUrlPolicy.normalizeHttpsUrl(it)) }
    }

    @Test
    fun modelMayNavigateOnlyToAnExactUrlInTheUsersMessage() {
        val request = "Please inspect https://example.com/research?source=me."
        assertTrue(BrowserUrlPolicy.wasExplicitlySupplied("https://example.com/research?source=me", request))
        assertFalse(BrowserUrlPolicy.wasExplicitlySupplied("https://other.com/research", request))
        assertFalse(BrowserUrlPolicy.wasExplicitlySupplied("https://example.com/other", request))
    }
}
