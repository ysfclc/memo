package app.memo.android.ui

import androidx.compose.ui.test.assertDoesNotExist
import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.memo.android.browser.BrowserWorkflowUiState
import app.memo.android.data.AgentRunRecord
import app.memo.android.data.AppSnapshot
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class LiveContinuationUiTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun eligibleInterruptedRunShowsActionAndPassesExactParentToCallback() {
        val parent = AgentRunRecord(
            id = "parent-run",
            conversationId = "original-conversation",
            goal = "Finish the original task",
            status = "NEEDS_REVIEW",
            lastEventType = "RUN_NEEDS_REVIEW"
        )
        var continuedRunId = ""
        var continuedConversationId = ""

        composeRule.setContent {
            MemoTheme(themeMode = "LIGHT") {
                LiveWorkspaceScreen(
                    snapshot = AppSnapshot(agentRuns = listOf(parent)),
                    browserState = BrowserWorkflowUiState(),
                    onOpenBrowser = {},
                    onOpenApps = {},
                    onOpenApprovals = {},
                    onOpenWork = {},
                    onOpenTalk = {},
                    onContinueRun = { run ->
                        continuedRunId = run.id
                        continuedConversationId = run.conversationId
                    },
                    onResumeTask = {}
                )
            }
        }

        composeRule.onNodeWithText("Continue in Talk").assertExists().performClick()
        composeRule.runOnIdle {
            assertEquals("parent-run", continuedRunId)
            assertEquals("original-conversation", continuedConversationId)
        }
    }

    @Test
    fun uncertainCapabilityOutcomeNeverShowsContinuationAction() {
        val uncertain = AgentRunRecord(
            id = "uncertain-run",
            conversationId = "original-conversation",
            goal = "Inspect an uncertain operation",
            status = "NEEDS_REVIEW",
            lastEventType = "TOOL_FAILED",
            toolCalls = 1,
            successfulToolCalls = 0
        )

        composeRule.setContent {
            MemoTheme(themeMode = "LIGHT") {
                LiveWorkspaceScreen(
                    snapshot = AppSnapshot(agentRuns = listOf(uncertain)),
                    browserState = BrowserWorkflowUiState(),
                    onOpenBrowser = {},
                    onOpenApps = {},
                    onOpenApprovals = {},
                    onOpenWork = {},
                    onOpenTalk = {},
                    onContinueRun = {},
                    onResumeTask = {}
                )
            }
        }

        composeRule.onNodeWithText("Continue in Talk").assertDoesNotExist()
        composeRule.onNodeWithText(
            "A capability may have started without a verified completion. Inspect its destination before continuing."
        ).assertExists()
    }
}
