package app.memo.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * Local AES-256-GCM workspace vault.
 *
 * The canonical file is intentionally separate from SynthStore's native workflow store. Both
 * formats historically used synthos-vault.bin, so read() recognizes the other format and leaves
 * it alone. A matching legacy workspace is copied and authenticated before it is adopted; the
 * legacy file is never deleted or overwritten.
 */
class EncryptedVault(context: Context) {
    private val atomicFile = AtomicFile(File(context.filesDir, WORKSPACE_FILE_NAME))
    private val legacySharedFile = File(context.filesDir, LEGACY_SHARED_FILE_NAME)
    private val mutex = Mutex()
    private val alias = "synthos_fieldwork_vault_v1"
    private val aad = "Memo-Android:v1".toByteArray(Charsets.UTF_8)

    suspend fun read(): AppSnapshot? = mutex.withLock {
        if (atomicFile.baseFile.exists()) return@withLock readCanonicalFile()

        if (!legacySharedFile.exists()) return@withLock null
        require(legacySharedFile.length() <= MAX_WORKSPACE_BACKUP_BYTES + 64 * 1024L) {
            "Legacy workspace exceeds the supported size limit. The original file was preserved."
        }
        val legacyBytes = legacySharedFile.readBytes()

        when {
            hasWorkspaceHeader(legacyBytes) -> {
                // Authenticate and parse before copying anything into the canonical location.
                AppSnapshot.fromJson(decrypt(legacyBytes).toString(Charsets.UTF_8))
                copyLegacyWorkspaceAtomically(legacyBytes)
                // Re-open the destination so a successful migration proves it is independently readable.
                readCanonicalFile()
            }
            isNativeStoreEnvelope(legacyBytes) -> {
                // This is SynthStore's legacy file. It belongs to the other store; do not claim it.
                null
            }
            else -> throw IllegalStateException(
                "The legacy Memo vault has an unknown format. It was not changed; recovery is required."
            )
        }
    }

    suspend fun write(snapshot: AppSnapshot) = mutex.withLock {
        val bytes = encrypt(snapshot.toJson().toByteArray(Charsets.UTF_8))
        writeAtomically(bytes)
    }

    private fun readCanonicalFile(): AppSnapshot {
        val file = atomicFile.baseFile
        require(file.length() <= MAX_WORKSPACE_BACKUP_BYTES + 64 * 1024L) {
            "Encrypted workspace exceeds the supported size limit."
        }
        val bytes = atomicFile.openRead().use { input -> input.readBytes() }
        return AppSnapshot.fromJson(decrypt(bytes).toString(Charsets.UTF_8))
    }

    private fun copyLegacyWorkspaceAtomically(payload: ByteArray) {
        var output: FileOutputStream? = null
        try {
            output = atomicFile.startWrite()
            output.write(payload)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            output?.let { atomicFile.failWrite(it) }
            throw error
        }
    }

    private fun writeAtomically(payload: ByteArray) {
        var output: FileOutputStream? = null
        try {
            output = atomicFile.startWrite()
            output.write(payload)
            output.fd.sync()
            atomicFile.finishWrite(output)
        } catch (error: Throwable) {
            output?.let { atomicFile.failWrite(it) }
            throw error
        }
    }

    private fun encrypt(plain: ByteArray): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(plain)
        val iv = cipher.iv
        require(iv.size == 12) { "Unexpected AES-GCM IV length." }
        return byteArrayOf('S'.code.toByte(), 'Y'.code.toByte(), 'V'.code.toByte(), '1'.code.toByte(), iv.size.toByte()) + iv + encrypted
    }

    private fun decrypt(payload: ByteArray): ByteArray {
        require(payload.size >= 5 + 12 + 16) { "Vault data is incomplete." }
        require(hasWorkspaceHeader(payload)) {
            "Vault format is not recognized. Restore from a Memo JSON export if available."
        }
        val ivLength = payload[4].toInt() and 0xff
        require(ivLength == 12 && payload.size > 5 + ivLength + 16) { "Vault encryption metadata is invalid." }
        val iv = payload.copyOfRange(5, 5 + ivLength)
        val encrypted = payload.copyOfRange(5 + ivLength, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        // Reading must never create a replacement key for existing ciphertext.
        cipher.init(Cipher.DECRYPT_MODE, getExistingKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(encrypted)
    }

    private fun getExistingKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
            ?: throw IllegalStateException("The existing workspace encryption key is unavailable. The encrypted file was preserved.")
    }

    private fun getOrCreateKey(): SecretKey {
        getExistingKeyOrNull()?.let { return it }
        val generator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, "AndroidKeyStore")
        generator.init(
            KeyGenParameterSpec.Builder(alias, KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT)
                .setKeySize(256)
                .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build()
        )
        return generator.generateKey()
    }

    private fun getExistingKeyOrNull(): SecretKey? {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        return keyStore.getKey(alias, null) as? SecretKey
    }

    private fun hasWorkspaceHeader(payload: ByteArray): Boolean =
        payload.size >= 4 &&
            payload[0] == 'S'.code.toByte() &&
            payload[1] == 'Y'.code.toByte() &&
            payload[2] == 'V'.code.toByte() &&
            payload[3] == '1'.code.toByte()

    private fun isNativeStoreEnvelope(payload: ByteArray): Boolean {
        if (payload.isEmpty()) return false
        val ivLength = payload[0].toInt() and 0xff
        return ivLength in 12..16 && payload.size > 1 + ivLength + 16
    }

    companion object {
        internal const val WORKSPACE_FILE_NAME = "memo-workspace-vault.bin"
        internal const val LEGACY_SHARED_FILE_NAME = "synthos-vault.bin"
    }
}
