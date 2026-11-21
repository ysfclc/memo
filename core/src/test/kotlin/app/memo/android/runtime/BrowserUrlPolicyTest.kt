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
    fun observedLinksCannotEscapeCapturedOrigin() {
        val source = "https://example.com/research/current"
        assertTrue(BrowserUrlPolicy.isSameOriginHttpsLink(source, "https://example.com/pricing"))
        assertTrue(BrowserUrlPolicy.isSameOriginHttpsLink(source, "https://EXAMPLE.com/docs?q=Memo"))
        listOf(
            "https://other.example.com/pricing",
            "https://example.org/pricing",
            "http://example.com/pricing",
            "https://user@example.com/pricing",
            "https://example.com:8443/pricing",
            "https://192.168.1.1/pricing"
        ).forEach { assertFalse(BrowserUrlPolicy.isSameOriginHttpsLink(source, it)) }
        assertFalse(BrowserUrlPolicy.isSameOriginHttpsLink("https://example.com:8443/", "https://example.com/"))
    }

    @Test
    fun modelMayNavigateOnlyToAnExactUrlInTheUsersMessage() {
        val request = "Please inspect https://example.com/research?source=me."
        assertTrue(BrowserUrlPolicy.wasExplicitlySupplied("https://example.com/research?source=me", request))
        assertFalse(BrowserUrlPolicy.wasExplicitlySupplied("https://other.com/research", request))
        assertFalse(BrowserUrlPolicy.wasExplicitlySupplied("https://example.com/other", request))
    }
}
