package app.memo.android.ui

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class MemoDesignSystemSmokeTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun pageHeaderRendersItsHierarchyAndDescription() {
        compose.setContent {
            MemoTheme(themeMode = "DARK") {
                MemoPageHeader(
                    title = "Talk",
                    eyebrow = "MEMO / TALK",
                    description = "Capture an intent and continue where you left off."
                )
            }
        }

        compose.onNodeWithText("Talk").assertIsDisplayed()
        compose.onNodeWithText("MEMO / TALK").assertIsDisplayed()
        compose.onNodeWithText("Capture an intent and continue where you left off.").assertIsDisplayed()
    }

    @Test
    fun statusTagExposesMeaningBeyondColor() {
        compose.setContent {
            MemoTheme(themeMode = "LIGHT") {
                MemoStatusTag(label = "Needs review", tone = MemoStatusTone.WARNING)
            }
        }

        compose.onNodeWithText("Needs review").assertIsDisplayed()
        compose.onNodeWithContentDescription("Needs review status").assertIsDisplayed()
    }

    @Test
    fun messageContentAndMetadataRemainVisible() {
        compose.setContent {
            MemoTheme(themeMode = "DARK") {
                MemoMessageBubble(
                    isUser = false,
                    isSource = false,
                    label = "Memo",
                    timestamp = "Now",
                    content = "The captured information is preserved."
                )
            }
        }

        compose.onNodeWithText("Memo").assertIsDisplayed()
        compose.onNodeWithText("Now").assertIsDisplayed()
        compose.onNodeWithText("The captured information is preserved.").assertIsDisplayed()
    }
}
