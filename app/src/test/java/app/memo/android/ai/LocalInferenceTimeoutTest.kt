package app.memo.android.ai

import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test

class LocalInferenceTimeoutTest {
    @Test fun timeoutIsReportedAsAnInferenceFailureAndCancelsTheWork() = runBlocking {
        var cancelled = false
        try {
            withLocalInferenceTimeout(20L) {
                try {
                    delay(5_000L)
                    "unexpected completion"
                } finally {
                    cancelled = true
                }
            }
            fail("Expected a local inference timeout.")
        } catch (error: LocalInferenceTimeoutException) {
            assertEquals("Local model inference timed out after 0 seconds. The partial response was not saved as a completed result.", error.message)
        }
        assertEquals(true, cancelled)
    }

    @Test fun successfulInferenceReturnsItsResult() = runBlocking {
        assertEquals("complete", withLocalInferenceTimeout(1_000L) {
            delay(1L)
            "complete"
        })
    }
}
