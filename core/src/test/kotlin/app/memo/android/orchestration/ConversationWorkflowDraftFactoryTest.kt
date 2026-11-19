package app.memo.android.orchestration

import app.memo.android.data.AppSnapshot
import app.memo.android.data.ChatMessageRecord
import app.memo.android.data.ConversationRecord
import app.memo.android.data.WorkJobRecord
import app.memo.android.runtime.RuntimeRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ConversationWorkflowDraftFactoryTest {
    private val runtimes = RuntimeRegistry().listRuntimes()
    private val intent = "Research and prepare a sourced market report"
    private val conversation = ConversationRecord(
        id = "conversation-1",
        title = "Market research",
        messages = listOf(ChatMessageRecord(id = "message-1", role = "USER", content = intent))
    )
    private val graph = WorkflowGraph(
        goal = intent,
        nodes = listOf(
            TaskSpec(
                id = "scope", title = "Frame the report", kind = WorkflowNodeKind.SCOPE,
                participant = WorkflowParticipant.DETERMINISTIC, runtimeId = "app.jobs",
                requiredCapabilities = emptyList(), dependsOn = emptyList(),
                inputContract = "Original request", outputContract = "Defined research scope",
                postcondition = "Scope is explicit", timeoutMs = 90_000, maxRetries = 0,
                idempotencyKey = "a".repeat(64)
            ),
            TaskSpec(
                id = "research", title = "Gather source evidence", kind = WorkflowNodeKind.DOMAIN_WORK,
                participant = WorkflowParticipant.BROWSER, runtimeId = "runtime.browser",
                requiredCapabilities = listOf("browser.navigate_https", "browser.inspect_page", "browser.capture_evidence"),
                dependsOn = listOf("scope"), inputContract = "Defined research scope",
                outputContract = "Source-linked evidence", postcondition = "Every claim has a source",
                timeoutMs = 120_000, maxRetries = 0, idempotencyKey = "b".repeat(64)
            ),
            TaskSpec(
                id = "review", title = "Review the report", kind = WorkflowNodeKind.REVIEW,
                participant = WorkflowParticipant.USER, runtimeId = "app.jobs",
                requiredCapabilities = emptyList(), dependsOn = listOf("research"),
                inputContract = "Draft report", outputContract = "User review decision",
                postcondition = "User has reviewed the draft", timeoutMs = 90_000, maxRetries = 0,
                idempotencyKey = "c".repeat(64)
            )
        )
    )

    @Test fun createsLinkedReadOnlyDraftWithoutStartingAnyStep() {
        val job = ConversationWorkflowDraftFactory.create(
            AppSnapshot(conversations = listOf(conversation)), "conversation-1", "message-1",
            graph, runtimes, 123L
        )
        assertEquals("DRAFT", job.status)
        assertEquals("conversation-1", job.sourceConversationId)
        assertEquals("message-1", job.sourceMessageId)
        assertEquals(intent, job.goal)
        assertEquals(listOf("scope", "research", "review"), job.steps.map { it.id })
        assertTrue(job.steps.all { it.status == "PENDING" && !it.requiresApproval })
        assertTrue(job.workflowGraphJson.isNotBlank())
        assertTrue(job.resultSummary.contains("No nodes were executed"))
    }

    @Test fun rejectsMissingOrNonUserSourceMessage() {
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(AppSnapshot(), "missing", "message-1", graph, runtimes, 123L)
        }.isFailure)
        val assistantOnly = conversation.copy(messages = listOf(
            ChatMessageRecord(id = "message-1", role = "ASSISTANT", content = intent)
        ))
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(AppSnapshot(conversations = listOf(assistantOnly)), "conversation-1", "message-1", graph, runtimes, 123L)
        }.isFailure)
    }

    @Test fun rejectsChangedGoalDuplicateSourceAndOptionalRuntime() {
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(
                AppSnapshot(conversations = listOf(conversation)), "conversation-1", "message-1",
                graph.copy(goal = "A different request"), runtimes, 123L
            )
        }.isFailure)
        val duplicate = AppSnapshot(
            conversations = listOf(conversation),
            jobs = listOf(WorkJobRecord(
                title = "Existing", goal = intent, sourceConversationId = "conversation-1", sourceMessageId = "message-1"
            ))
        )
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(duplicate, "conversation-1", "message-1", graph, runtimes, 123L)
        }.isFailure)
        val optional = graph.copy(nodes = graph.nodes + graph.nodes.first().copy(
            id = "android", participant = WorkflowParticipant.ANDROID_APP,
            runtimeId = "runtime.android.accessibility", requiredCapabilities = listOf("android.launch_app"),
            dependsOn = listOf("review"), idempotencyKey = "d".repeat(64)
        ))
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(
                AppSnapshot(conversations = listOf(conversation)), "conversation-1", "message-1",
                optional, runtimes, 123L
            )
        }.isFailure)
    }

    @Test fun rejectsLocalWorkspaceMutationCapabilitiesOutsideTheReadOnlyAllowlist() {
        val mutation = graph.copy(nodes = graph.nodes.map { node ->
            if (node.id == "scope") node.copy(requiredCapabilities = listOf("job.create")) else node
        })
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(
                AppSnapshot(conversations = listOf(conversation)), "conversation-1", "message-1",
                mutation, runtimes, 123L
            )
        }.exceptionOrNull()?.message.orEmpty().contains("read-only allowlist"))
    }

    @Test fun rejectsApprovalGatedAndExternalWriteNodes() {
        val unsafe = graph.copy(nodes = graph.nodes.map { node ->
            if (node.id == "research") node.copy(externalWrite = true) else node
        })
        assertTrue(runCatching {
            ConversationWorkflowDraftFactory.create(
                AppSnapshot(conversations = listOf(conversation)), "conversation-1", "message-1",
                unsafe, runtimes, 123L
            )
        }.isFailure)
    }
}
