package app.memo.android.data

import android.content.Context
import android.content.ContextWrapper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import com.synthos.runtime.data.SynthStore
import com.synthos.runtime.data.Workflow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.io.File

/**
 * CI-only two-phase acceptance test:
 *   phase=seed runs before adb install -r;
 *   phase=verify runs after the newer signed APK replaces the baseline APK.
 *
 * Store files are redirected into a private test directory. The test never deletes or edits
 * Memo's actual user workspace, even when it simulates historical/corrupt legacy payloads.
 */
@RunWith(AndroidJUnit4::class)
class EncryptedStoreUpgradeTest {
    @Test
    fun encryptedStoresSurviveInstallUpgradeAndLegacyRecovery() = runBlocking {
        val context = isolatedContext()
        when (InstrumentationRegistry.getArguments().getString("phase")) {
            "seed" -> seed(context)
            "verify" -> verifyAndExerciseLegacyRecovery(context)
            else -> throw AssertionError("Pass instrumentation argument phase=seed or phase=verify.")
        }
    }

    private suspend fun seed(context: Context) {
        val files = context.filesDir
        assertFalse(files.resolve(LEGACY_FILE).exists())

        EncryptedVault(context).write(sampleSnapshot())

        val workflow = Workflow("Upgrade persistence workflow", "com.android.settings", "Settings").apply {
            id = WORKFLOW_ID
        }
        SynthStore(context).saveWorkflow(workflow)

        assertTrue(files.resolve(WORKSPACE_FILE).isFile)
        assertTrue(files.resolve(NATIVE_FILE).isFile)
        assertFalse(files.resolve(LEGACY_FILE).exists())
    }

    private suspend fun verifyAndExerciseLegacyRecovery(context: Context) {
        val files = context.filesDir
        val workspace = files.resolve(WORKSPACE_FILE)
        val native = files.resolve(NATIVE_FILE)
        val legacy = files.resolve(LEGACY_FILE)

        assertTrue("Primary encrypted workspace missing after APK update.", workspace.isFile)
        assertTrue("Native workflow store missing after APK update.", native.isFile)
        val workspaceBytes = workspace.readBytes()
        val nativeBytes = native.readBytes()
        assertFalse("The two stores must not share ciphertext.", workspaceBytes.contentEquals(nativeBytes))

        // Authentication with the existing Android Keystore aliases proves the keys remained usable.
        assertWorkspaceContent(EncryptedVault(context).read())
        assertEquals(
            WORKFLOW_NAME,
            SynthStore(context).getWorkflow(WORKFLOW_ID)?.name
        )

        // Historical primary-vault file: the native store must recognize it as foreign and leave it alone.
        assertTrue(workspace.delete())
        assertTrue(native.delete())
        legacy.writeBytes(workspaceBytes)
        val nativeStoreBeforePrimaryMigration = SynthStore(context)
        assertTrue(nativeStoreBeforePrimaryMigration.getWorkflows().isEmpty())
        assertFalse("A primary-vault payload must not create a native-store file.", native.exists())
        assertArrayEquals(workspaceBytes, legacy.readBytes())

        val migratedWorkspace = EncryptedVault(context).read()
        assertWorkspaceContent(migratedWorkspace)
        assertArrayEquals("Migration must preserve the original legacy bytes.", workspaceBytes, legacy.readBytes())
        assertArrayEquals(workspaceBytes, workspace.readBytes())

        // Historical native-store file: EncryptedVault must leave it for SynthStore to adopt.
        assertTrue(workspace.delete())
        assertTrue(native.delete())
        legacy.writeBytes(nativeBytes)
        assertNull(EncryptedVault(context).read())
        assertFalse("A native-store payload must not create a workspace file.", workspace.exists())
        assertArrayEquals(nativeBytes, legacy.readBytes())

        val migratedNativeStore = SynthStore(context)
        assertEquals(WORKFLOW_NAME, migratedNativeStore.getWorkflow(WORKFLOW_ID)?.name)
        assertTrue(native.isFile)
        assertArrayEquals("Native migration must preserve the original legacy bytes.", nativeBytes, legacy.readBytes())
        assertArrayEquals(nativeBytes, native.readBytes())

        // Both canonical stores now coexist. Writes to either must not alter the other ciphertext.
        EncryptedVault(context).write(sampleSnapshot())
        val workspaceBeforeNativeWrite = workspace.readBytes()
        migratedNativeStore.saveWorkflow(
            Workflow("Second isolated workflow", "com.android.settings", "Settings").apply {
                id = "upgrade-workflow-second"
            }
        )
        assertArrayEquals(workspaceBeforeNativeWrite, workspace.readBytes())
        assertWorkspaceContent(EncryptedVault(context).read())
        assertEquals(WORKFLOW_NAME, SynthStore(context).getWorkflow(WORKFLOW_ID)?.name)
        assertEquals("Second isolated workflow", SynthStore(context).getWorkflow("upgrade-workflow-second")?.name)
        assertTrue(workspace.isFile && native.isFile)

        // Corrupted primary data must fail closed without being overwritten by the other store.
        assertTrue(workspace.delete())
        assertTrue(native.delete())
        val corruptPrimary = workspaceBytes.copyOf().apply {
            this[this.lastIndex] = (this[this.lastIndex].toInt() xor 0x01).toByte()
        }
        legacy.writeBytes(corruptPrimary)
        assertTrue(SynthStore(context).getWorkflows().isEmpty())
        assertFalse(native.exists())
        val primaryRejectedCorruption = runCatching { EncryptedVault(context).read() }.isFailure
        assertTrue("Corrupt primary ciphertext must not be accepted.", primaryRejectedCorruption)
        assertArrayEquals(corruptPrimary, legacy.readBytes())

        // Corrupted native data is recognized as native, but authentication must reject it.
        val corruptNative = nativeBytes.copyOf().apply {
            this[this.lastIndex] = (this[this.lastIndex].toInt() xor 0x01).toByte()
        }
        legacy.writeBytes(corruptNative)
        assertNull(EncryptedVault(context).read())
        assertFalse(workspace.exists())
        val nativeRejectedCorruption = runCatching { SynthStore(context) }.isFailure
        assertTrue("Corrupt native ciphertext must not be accepted.", nativeRejectedCorruption)
        assertFalse(native.exists())
        assertArrayEquals(corruptNative, legacy.readBytes())

        // Unknown formats are rejected by both stores and remain byte-for-byte recoverable.
        val unknown = byteArrayOf(0x7f, 0x4d, 0x45, 0x4d, 0x4f, 0x01)
        legacy.writeBytes(unknown)
        assertTrue(runCatching { EncryptedVault(context).read() }.isFailure)
        assertTrue(runCatching { SynthStore(context) }.isFailure)
        assertFalse(workspace.exists())
        assertFalse(native.exists())
        assertArrayEquals(unknown, legacy.readBytes())

        // The acceptance test itself must never reach the real app files directory.
        assertTrue(files.canonicalPath.contains("memo-storage-upgrade-acceptance"))
        assertNotNull(context.applicationContext)
    }

    private fun assertWorkspaceContent(snapshot: AppSnapshot?) {
        assertNotNull("Encrypted workspace did not decrypt.", snapshot)
        val conversation = snapshot!!.conversations.single { it.id == CONVERSATION_ID }
        assertEquals(MESSAGE_CONTENT, conversation.messages.single().content)
    }

    private fun sampleSnapshot(): AppSnapshot = AppSnapshot(
        conversations = listOf(
            ConversationRecord(
                id = CONVERSATION_ID,
                title = "Upgrade persistence acceptance",
                messages = listOf(
                    ChatMessageRecord(
                        id = MESSAGE_ID,
                        role = "user",
                        content = MESSAGE_CONTENT
                    )
                )
            )
        )
    )

    private fun isolatedContext(): Context {
        val target = InstrumentationRegistry.getInstrumentation().targetContext
        val directory = File(target.cacheDir, TEST_DIRECTORY)
        if (InstrumentationRegistry.getArguments().getString("phase") == "seed") {
            check(directory.deleteRecursively() || !directory.exists()) {
                "Could not reset the isolated Memo storage-test directory."
            }
        }
        check(directory.exists() || directory.mkdirs()) {
            "Could not create the isolated Memo storage-test directory."
        }
        return IsolatedFilesContext(target, directory)
    }

    private class IsolatedFilesContext(
        base: Context,
        private val isolatedFilesDir: File
    ) : ContextWrapper(base) {
        override fun getFilesDir(): File = isolatedFilesDir
        override fun getApplicationContext(): Context = this
    }

    private companion object {
        const val TEST_DIRECTORY = "memo-storage-upgrade-acceptance"
        const val WORKSPACE_FILE = "memo-workspace-vault.bin"
        const val NATIVE_FILE = "memo-native-workflows.bin"
        const val LEGACY_FILE = "synthos-vault.bin"
        const val CONVERSATION_ID = "upgrade-conversation"
        const val MESSAGE_ID = "upgrade-message"
        const val MESSAGE_CONTENT = "memo-encrypted-workspace-survived-the-update"
        const val WORKFLOW_ID = "upgrade-workflow"
        const val WORKFLOW_NAME = "Upgrade persistence workflow"
    }
}
