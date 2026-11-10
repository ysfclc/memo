package app.memo.android.context

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WorkspaceContextRetrieverTest {
    @Test fun ranksRelevantTitleAndBodyAndPreservesSourceAttribution() {
        val result = WorkspaceContextRetriever.select(
            "Memo local model runtime",
            listOf(
                ContextCandidate("1", "PROJECT", "Memo local model runtime", "LiteRT local inference and model downloads", 10, "Project notes"),
                ContextCandidate("2", "KNOWLEDGE", "Holiday planning", "Beaches and hotels in Italy", 20, "Imported chat"),
                ContextCandidate("3", "EVIDENCE", "Runtime note", "The local model runner streams partial tokens from LiteRT", 30, "Saved evidence")
            )
        )
        assertEquals("1", result.hits.first().id)
        assertTrue(result.hits.any { it.id == "3" })
        assertTrue(result.asPromptContext().contains("[PROJECT:1]"))
        assertTrue(result.asPromptContext().contains("Project notes"))
    }

    @Test fun emptyOrGenericQueriesDoNotLeakWholeWorkspace() {
        assertTrue(WorkspaceContextRetriever.select("", listOf(ContextCandidate("1", "X", "private", "secret"))).isEmpty)
        assertTrue(WorkspaceContextRetriever.select("the and help me", listOf(ContextCandidate("1", "X", "private", "secret"))).isEmpty)
    }

    @Test fun retrievalIsBoundedAndDeterministic() {
        val candidates = (1..30).map { ContextCandidate("$it", "EVIDENCE", "CRM outbound project $it", "Apollo outreach and partner follow-up", it.toLong(), "Evidence $it") }
        val first = WorkspaceContextRetriever.select("CRM outbound outreach", candidates, maxHits = 3, maxCharacters = 1_200)
        val second = WorkspaceContextRetriever.select("CRM outbound outreach", candidates, maxHits = 3, maxCharacters = 1_200)
        assertEquals(first.hits.map { it.id }, second.hits.map { it.id })
        assertTrue(first.hits.size <= 3)
        assertTrue(first.estimatedCharacters <= 1_200)
        assertFalse(first.asPromptContext().isBlank())
    }

    @Test fun handlesPolishTurkishAccentsAndUsesSnippetsRatherThanFullDocuments() {
        val result = WorkspaceContextRetriever.select(
            "kariyer özgeçmiş Türkçe",
            listOf(ContextCandidate("cv", "ARTIFACT", "Özgeçmiş kariyer", "Long profile document. Türkçe career history, achievements, and verified skills.", 1, "CV draft"))
        )
        assertTrue(result.hits.isNotEmpty())
        assertTrue(result.hits.first().excerpt.length <= 1_000)
    }
}
