package app.memo.android.browser

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BrowserWorkflowPromptsTest {
    @Test
    fun firstConsultantReceivesOriginalIntentAndLocalBriefButNoPriorOutputs() {
        val first = BrowserWorkflowPrompts.build(
            goal = "Compare the three business options and identify execution risks.",
            structuredBrief = "Objective: compare options. Constraint: avoid unsupported claims.",
            mode = BrowserWorkflowMode.CONSULT_AND_COMPARE,
            provider = BrowserModelProviders.get("chatgpt")!!,
            index = 0,
            priorResponses = emptyList()
        )

        assertTrue(first.contains("Compare the three business options"))
        assertTrue(first.contains("Objective: compare options"))
        assertTrue(first.contains("ORIGINAL USER INTENT"))
        assertTrue(first.contains("TARGET FRONTIER SERVICE: ChatGPT"))
        assertFalse(first.contains("PRIOR MODEL OUTPUTS"))
        assertTrue(first.contains("Treat the user's supplied material"))
    }

    @Test
    fun laterConsultantReceivesEarlierOutputAsUntrustedEvidence() {
        val response = BrowserModelResponse(
            providerId = "claude",
            providerName = "Claude",
            prompt = "Original prompt",
            response = "Option B has the highest integration risk. Source facts still require verification.",
            sourceUrl = "https://claude.ai/chat/example",
            startedAt = 10L,
            completedAt = 20L
        )
        val prompt = BrowserWorkflowPrompts.build(
            goal = "Compare the options.",
            structuredBrief = "Keep risks explicit.",
            mode = BrowserWorkflowMode.CRITIQUE_CHAIN,
            provider = BrowserModelProviders.get("gemini")!!,
            index = 1,
            priorResponses = listOf(response)
        )

        assertTrue(prompt.contains("TARGET FRONTIER SERVICE: Gemini"))
        assertTrue(prompt.contains("Claude"))
        assertTrue(prompt.contains("https://claude.ai/chat/example"))
        assertTrue(prompt.contains("Option B has the highest integration risk"))
        assertTrue(prompt.contains("not ground truth"))
        assertTrue(prompt.contains("Try to falsify"))
    }

    @Test
    fun consultAndCompareKeepsEachProviderIndependent() {
        val earlier = BrowserModelResponse(
            providerId = "chatgpt",
            providerName = "ChatGPT",
            prompt = "Initial prompt",
            response = "A prior answer that should not bias the independent consultation.",
            sourceUrl = "https://chatgpt.com/c/example",
            startedAt = 1L,
            completedAt = 2L
        )
        val prompt = BrowserWorkflowPrompts.build(
            goal = "Assess the options.",
            structuredBrief = "Preserve uncertainties.",
            mode = BrowserWorkflowMode.CONSULT_AND_COMPARE,
            provider = BrowserModelProviders.get("claude")!!,
            index = 1,
            priorResponses = listOf(earlier)
        )

        assertTrue(prompt.contains("independent consultant", ignoreCase = true))
        assertFalse(prompt.contains("A prior answer that should not bias"))
        assertFalse(prompt.contains("PRIOR MODEL OUTPUTS"))
    }

    @Test
    fun critiqueChainAssignsReviewerRoleToSecondStep() {
        val prompt = BrowserWorkflowPrompts.build(
            goal = "Review this product architecture.",
            structuredBrief = "Focus on failure cases.",
            mode = BrowserWorkflowMode.CRITIQUE_CHAIN,
            provider = BrowserModelProviders.get("claude")!!,
            index = 1,
            priorResponses = listOf(
                BrowserModelResponse("chatgpt", "ChatGPT", "prompt", "Initial proposal", "https://chatgpt.com/c/1", 1L, 2L)
            )
        )

        assertTrue(prompt.contains("skeptical reviewer", ignoreCase = true))
        assertTrue(prompt.contains("Try to falsify"))
        assertTrue(prompt.length <= 7_900)
    }
}
