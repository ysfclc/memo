package app.memo.android.browser

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserCaptureSharePayloadTest {
    @Test
    fun formatsVersionedSourceLinkedPageText() {
        val payload = BrowserCaptureSharePayload.format(
            title = "  Acme Supplier  ",
            sourceUrl = "https://example.com/company",
            summary = "Industrial manufacturer",
            content = "Acme manufactures CNC components and supplies industrial customers."
        )

        assertTrue(payload!!.startsWith(BrowserCaptureSharePayload.HEADER))
        assertTrue(payload.contains("Title: Acme Supplier"))
        assertTrue(payload.contains("Source URL: https://example.com/company"))
        assertTrue(payload.contains("Summary: Industrial manufacturer"))
        assertTrue(payload.contains("Captured page text:\nAcme manufactures"))
        assertTrue(payload.length <= BrowserCaptureSharePayload.MAX_PAYLOAD)
    }

    @Test
    fun rejectsUnsafeUrlsAndShortOrEmptyContent() {
        assertNull(BrowserCaptureSharePayload.format("x", "javascript:alert(1)", "", "This is enough text to pass the minimum length."))
        assertNull(BrowserCaptureSharePayload.format("x", "file:///etc/passwd", "", "This is enough text to pass the minimum length."))
        assertNull(BrowserCaptureSharePayload.format("x", "https://user:pass@example.com/", "", "This is enough text to pass the minimum length."))
        assertNull(BrowserCaptureSharePayload.format("x", "https://example.com", "", "short"))
    }

    @Test
    fun boundsContentAndKeepsAuthenticationDataOutOfThePayload() {
        val pageText = "Public information ".repeat(1_000)
        val payload = BrowserCaptureSharePayload.format(
            "Page", "https://example.com", "Public data", pageText
        )!!

        assertTrue(payload.length <= BrowserCaptureSharePayload.MAX_PAYLOAD)
        assertEquals(BrowserCaptureSharePayload.MAX_PAGE_TEXT, pageText.trim().take(BrowserCaptureSharePayload.MAX_PAGE_TEXT).length)
        assertTrue(payload.contains("Source URL: https://example.com"))
        assertTrue(!payload.contains("document.cookie"))
        assertTrue(!payload.contains("localStorage"))
    }
    @Test
    fun stripsQueryAndFragmentThatCouldContainAuthenticationMaterial() {
        val payload = BrowserCaptureSharePayload.format(
            "Account page",
            "https://example.com/profile?code=oauth-code&access_token=secret#session=private",
            "Account details",
            "Visible profile information that the user chose to share with another app."
        )!!

        assertTrue(payload.contains("Source URL: https://example.com/profile"))
        assertTrue(!payload.contains("oauth-code"))
        assertTrue(!payload.contains("access_token"))
        assertTrue(!payload.contains("session=private"))
    }

}
