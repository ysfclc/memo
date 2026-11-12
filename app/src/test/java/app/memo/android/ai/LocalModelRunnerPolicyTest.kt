package app.memo.android.ai

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LocalModelRunnerPolicyTest {
    @Test
    fun localInferenceHasABoundedThreeMinuteDeadline() {
        assertEquals(180_000L, LocalModelRunner.INFERENCE_TIMEOUT_MS)
    }

    @Test
    fun timeoutIsReportedAsAnInferenceFailureNotAUserCancellation() {
        val timeout = LocalInferenceTimeoutException(LocalModelRunner.INFERENCE_TIMEOUT_MS)
        assertTrue(timeout.message.orEmpty().contains("180 seconds"))
        assertTrue(timeout.message.orEmpty().contains("partial response"))
    }

    @Test
    fun deadlineCancelsInFlightWorkAndReturnsTheDedicatedTimeoutFailure() = runBlocking {
        var workCancelled = false
        val failure = runCatching {
            withLocalInferenceTimeout(50L) {
                try {
                    delay(5_000L)
                    "unexpected completion"
                } finally {
                    workCancelled = true
                }
            }
        }.exceptionOrNull()

        assertTrue("Expected LocalInferenceTimeoutException, got $failure", failure is LocalInferenceTimeoutException)
        assertTrue(failure?.message.orEmpty().contains("timed out"))
        assertTrue(failure?.message.orEmpty().contains("partial response"))
        assertTrue("Timed-out inference work must be cancelled.", workCancelled)
    }

    @Test
    fun successfulInferenceReturnsItsCompleteResult() = runBlocking {
        val result = withLocalInferenceTimeout(1_000L) {
            delay(1L)
            "complete"
        }
        assertEquals("complete", result)
    }

    @Test
    fun parentCancellationIsNotConvertedIntoAnInferenceTimeout() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val observedCancellation = CompletableDeferred<Throwable>()
        val inference = launch {
            entered.complete(Unit)
            try {
                withLocalInferenceTimeout(30_000L) {
                    awaitCancellation()
                }
            } catch (cancelled: CancellationException) {
                observedCancellation.complete(cancelled)
            }
        }

        entered.await()
        inference.cancel()
        val cancellation = observedCancellation.await()
        inference.join()

        assertFalse(cancellation is LocalInferenceTimeoutException)
    }
}
