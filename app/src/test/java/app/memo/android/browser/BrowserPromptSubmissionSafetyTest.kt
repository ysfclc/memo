package app.memo.android.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Source-contract regression tests for the JavaScript executed in the visible WebView.
 *
 * These assertions deliberately fail if submission safety is weakened back to substring or
 * page-wide text detection. Runtime provider DOM behavior is still verified on-device.
 */
class BrowserPromptSubmissionSafetyTest {
    @Test
    fun preparationPreservesAnExistingDifferentComposerDraft() {
        val script = BrowserScripts.preparePrompt("approved prompt")

        assertTrue(script.contains("existing.length>0 && existing!==normalize(prompt)"))
        assertTrue(script.contains("left the existing draft untouched"))
    }

    @Test
    fun sendRequiresTheComposerToMatchTheApprovedPrompt() {
        val script = BrowserScripts.sendPreparedPrompt("approved prompt")

        assertTrue(script.contains("value() !== wanted"))
        assertFalse(script.contains("value().includes(wanted)"))
    }

    @Test
    fun verificationRequiresANewExactUserMessageAndAnEmptyComposer() {
        val script = BrowserScripts.verifyPromptSubmission("approved prompt")

        assertTrue(script.contains("message=>message===normalize(wanted)"))
        assertTrue(script.contains("newExactUserMessage=afterExactMatches>Number(state.beforeExactMatches||0)"))
        assertTrue(script.contains("composerCleared && newExactUserMessage"))
        assertFalse(script.contains("newUserMessage || newVisiblePrompt"))
        assertFalse(script.contains("afterOccurrences>state.beforeOccurrences"))
    }
}
