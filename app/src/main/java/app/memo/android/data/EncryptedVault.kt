package app.memo.android.data

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.AtomicFile
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** Local AES-256-GCM vault. The encryption key is kept in Android Keystore and never exported. */
class EncryptedVault(context: Context) {
    private val atomicFile = AtomicFile(File(context.filesDir, "synthos-vault.bin"))
    private val mutex = Mutex()
    private val alias = "synthos_fieldwork_vault_v1"
    private val aad = "Memo-Android:v1".toByteArray(Charsets.UTF_8)

    suspend fun read(): AppSnapshot? = mutex.withLock {
        val file = atomicFile.baseFile
        if (!file.exists()) return@withLock null
        require(file.length() <= MAX_WORKSPACE_BACKUP_BYTES + 64 * 1024L) { "Encrypted workspace exceeds the supported size limit." }
        val bytes = atomicFile.openRead().use { input -> input.readBytes() }
        AppSnapshot.fromJson(decrypt(bytes).toString(Charsets.UTF_8))
    }

    suspend fun write(snapshot: AppSnapshot) = mutex.withLock {
        val bytes = encrypt(snapshot.toJson().toByteArray(Charsets.UTF_8))
        var output: FileOutputStream? = null
        try {
            output = atomicFile.startWrite()
            output.write(bytes)
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
        require(payload[0] == 'S'.code.toByte() && payload[1] == 'Y'.code.toByte() && payload[2] == 'V'.code.toByte() && payload[3] == '1'.code.toByte()) {
            "Vault format is not recognized. Restore from a Memo JSON export if available."
        }
        val ivLength = payload[4].toInt() and 0xff
        require(ivLength == 12 && payload.size > 5 + ivLength) { "Vault encryption metadata is invalid." }
        val iv = payload.copyOfRange(5, 5 + ivLength)
        val encrypted = payload.copyOfRange(5 + ivLength, payload.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        return cipher.doFinal(encrypted)
    }

    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
        val existing = keyStore.getKey(alias, null) as? SecretKey
        if (existing != null) return existing
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
}
