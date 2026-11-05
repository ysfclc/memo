package app.memo.android.automation

import app.memo.android.data.TaskKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class TaskRulesTest {
    @Test fun addsHttpsToBareHost() {
        assertEquals("https://example.com/path", TaskRules.normalizeHttpsUrl("example.com/path"))
    }

    @Test fun rejectsInsecureHttp() {
        val error = runCatching { TaskRules.normalizeHttpsUrl("http://example.com") }.exceptionOrNull()
        assertNotNull(error)
    }

    @Test fun rejectsCredentialsInUrl() {
        val error = runCatching { TaskRules.normalizeHttpsUrl("https://user:password@example.com") }.exceptionOrNull()
        assertNotNull(error)
    }

    @Test fun textSearchRequiresQuery() {
        assertNotNull(TaskRules.validate("Find a phrase", "", TaskKind.FIND_TEXT, "  "))
    }

    @Test fun captureTaskCanUseCurrentPage() {
        assertNull(TaskRules.validate("Capture this page", "", TaskKind.CAPTURE_PAGE, ""))
    }

    @Test fun promptCannotBeEmpty() {
        assertNotNull(TaskRules.validate("Prepare a prompt", "", TaskKind.PREPARE_PROMPT, ""))
    }
}
