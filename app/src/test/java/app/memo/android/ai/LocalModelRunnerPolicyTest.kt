package app.memo.android.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelRunnerPolicyTest {
    @Test
    fun localInferenceHasABoundedThreeMinuteDeadline() {
        assertEquals(180_000L, LocalModelRunner.INFERENCE_TIMEOUT_MS)
    }

    @Test
    fun timeoutIsReportedAsAnInferenceFailureNotAUserCancellation() {
        val timeout = LocalInferenceTimeoutException()
        assertTrue(timeout.message.orEmpty().contains("timed out"))
        assertTrue(timeout.message.orEmpty().contains("partial response"))
    }
}
