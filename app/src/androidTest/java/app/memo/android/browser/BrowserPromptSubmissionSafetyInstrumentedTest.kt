package app.memo.android.browser

import android.view.ViewTreeObserver
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.memo.android.MainActivity
import org.json.JSONObject
import org.json.JSONTokener
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/**
 * Executes the real BrowserScripts in Android WebView against a controlled local page.
 *
 * These tests cover browser-engine behavior that source-string assertions cannot prove:
 * existing draft preservation, exact composer binding, and exact new-message postconditions.
 * They do not replace acceptance against authenticated provider websites on a physical device.
 */
@RunWith(AndroidJUnit4::class)
class BrowserPromptSubmissionSafetyInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    private lateinit var webView: WebView

    @After
    fun tearDown() {
        if (::webView.isInitialized) {
            composeRule.runOnUiThread {
                webView.stopLoading()
                webView.destroy()
            }
        }
    }

    private fun loadFixture(
        initialDraft: String = "",
        existingMessage: String = "",
        sendScript: String = ""
    ) {
        val pageLoaded = CountDownLatch(1)
        val viewLaidOut = CountDownLatch(1)
        val escapedDraft = escapeHtml(initialDraft)
        val escapedMessage = escapeHtml(existingMessage)
        val priorMessage = if (existingMessage.isNotEmpty()) {
            """<div data-message-author-role="user">$escapedMessage</div>"""
        } else {
            ""
        }
        val html = """
            <!doctype html>
            <html>
            <head>
              <meta name="viewport" content="width=device-width, initial-scale=1">
              <style>
                html, body { margin: 0; padding: 0; }
                form { padding: 16px; }
                textarea { display: block; width: 320px; height: 120px; }
                button { display: block; width: 120px; height: 48px; }
                [data-message-author-role="user"] { display: block; min-height: 24px; }
              </style>
            </head>
            <body>
              <main>
                $priorMessage
                <form id="prompt-form">
                  <textarea id="composer" aria-label="Prompt">$escapedDraft</textarea>
                  <button id="send" type="button" aria-label="Send">Send</button>
                </form>
                <div id="messages"></div>
              </main>
              <script>
                window.sendCount = 0;
                document.getElementById('send').addEventListener('click', function () {
                  window.sendCount += 1;
                  $sendScript
                });
              </script>
            </body>
            </html>
        """.trimIndent()

        composeRule.runOnUiThread {
            webView = WebView(composeRule.activity).apply {
                settings.javaScriptEnabled = true
                webViewClient = object : WebViewClient() {
                    override fun onPageFinished(view: WebView, url: String) {
                        pageLoaded.countDown()
                    }
                }
                viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                    override fun onGlobalLayout() {
                        if (width > 0 && height > 0) {
                            viewTreeObserver.removeOnGlobalLayoutListener(this)
                            viewLaidOut.countDown()
                        }
                    }
                })
            }
            composeRule.activity.setContentView(webView)
            webView.loadDataWithBaseURL(
                "https://provider.example/conversation",
                html,
                "text/html",
                "UTF-8",
                null
            )
        }

        assertTrue("Fixture page did not finish loading.", pageLoaded.await(10, TimeUnit.SECONDS))
        assertTrue("Fixture WebView was not laid out.", viewLaidOut.await(10, TimeUnit.SECONDS))
        composeRule.waitForIdle()
    }

    private fun escapeHtml(value: String): String = value
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&#39;")

    private fun evaluateValue(script: String): String {
        val completed = CountDownLatch(1)
        var encodedResult: String? = null
        composeRule.runOnUiThread {
            webView.evaluateJavascript(script) { result ->
                encodedResult = result
                completed.countDown()
            }
        }
        assertTrue("WebView JavaScript evaluation timed out.", completed.await(10, TimeUnit.SECONDS))
        return JSONTokener(encodedResult ?: "null").nextValue() as? String
            ?: error("WebView JavaScript did not return a string.")
    }

    private fun evaluateJson(script: String): JSONObject = JSONObject(evaluateValue(script))

    private fun composerValue(): String =
        evaluateValue("document.getElementById('composer').value")

    private fun sendCount(): Int = evaluateValue("String(window.sendCount)").toInt()

    private fun runJavaScript(statement: String) {
        evaluateValue("(function(){ $statement; return 'ok'; })()")
    }

    @Test
    fun preparationPreservesAnExistingDifferentComposerDraft() {
        loadFixture(initialDraft = "my private draft")

        val result = evaluateJson(BrowserScripts.preparePrompt("approved prompt"))

        assertFalse(result.optBoolean("ok"))
        assertEquals("Existing draft preserved", result.optString("title"))
        assertEquals("my private draft", composerValue())
        assertEquals(0, sendCount())
    }

    @Test
    fun sendRefusesAComposerChangedAfterApproval() {
        loadFixture()
        assertTrue(evaluateJson(BrowserScripts.preparePrompt("approved prompt")).optBoolean("ok"))

        runJavaScript("document.getElementById('composer').value = 'changed after approval'")

        val result = evaluateJson(BrowserScripts.sendPreparedPrompt("approved prompt"))

        assertFalse(result.optBoolean("clicked"))
        assertEquals("changed after approval", composerValue())
        assertEquals(0, sendCount())
    }

    @Test
    fun exactNewUserMessageAndClearedComposerVerifySubmission() {
        loadFixture(
            sendScript = """
                const composer = document.getElementById('composer');
                const message = document.createElement('div');
                message.setAttribute('data-message-author-role', 'user');
                message.textContent = composer.value;
                document.getElementById('messages').appendChild(message);
                composer.value = '';
            """.trimIndent()
        )
        assertTrue(evaluateJson(BrowserScripts.preparePrompt("approved prompt")).optBoolean("ok"))
        assertTrue(evaluateJson(BrowserScripts.sendPreparedPrompt("approved prompt")).optBoolean("clicked"))

        val result = evaluateJson(BrowserScripts.verifyPromptSubmission("approved prompt"))

        assertTrue(result.optBoolean("verified"))
        assertTrue(result.optBoolean("newExactUserMessage"))
        assertTrue(result.optBoolean("composerCleared"))
        assertEquals("", composerValue())
    }

    @Test
    fun prefixOrModifiedMessageDoesNotVerifySubmission() {
        loadFixture(
            sendScript = """
                const composer = document.getElementById('composer');
                const message = document.createElement('div');
                message.setAttribute('data-message-author-role', 'user');
                message.textContent = composer.value + ' with an unapproved suffix';
                document.getElementById('messages').appendChild(message);
                composer.value = '';
            """.trimIndent()
        )
        assertTrue(evaluateJson(BrowserScripts.preparePrompt("approved prompt")).optBoolean("ok"))
        assertTrue(evaluateJson(BrowserScripts.sendPreparedPrompt("approved prompt")).optBoolean("clicked"))

        val result = evaluateJson(BrowserScripts.verifyPromptSubmission("approved prompt"))

        assertFalse(result.optBoolean("verified"))
        assertFalse(result.optBoolean("newExactUserMessage"))
        assertTrue(result.optBoolean("composerCleared"))
    }

    @Test
    fun preExistingExactMessageDoesNotCountAsANewSubmission() {
        loadFixture(
            existingMessage = "approved prompt",
            sendScript = "document.getElementById('composer').value = '';"
        )
        assertTrue(evaluateJson(BrowserScripts.preparePrompt("approved prompt")).optBoolean("ok"))
        assertTrue(evaluateJson(BrowserScripts.sendPreparedPrompt("approved prompt")).optBoolean("clicked"))

        val result = evaluateJson(BrowserScripts.verifyPromptSubmission("approved prompt"))

        assertFalse(result.optBoolean("verified"))
        assertFalse(result.optBoolean("newExactUserMessage"))
        assertTrue(result.optBoolean("composerCleared"))
    }
}
