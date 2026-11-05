package app.memo.android.ai

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import org.json.JSONObject
import java.io.File
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/** API credentials are stored separately from workspace exports, encrypted by Android Keystore. */
data class ProviderConfig(
    val baseUrl: String = "",
    val model: String = "",
    val apiKey: String = ""
) {
    val isConfigured: Boolean get() = baseUrl.startsWith("https://") && model.isNotBlank()
}

class ProviderSettingsStore(context: Context) {
    private val file = File(context.noBackupFilesDir, "synthos-provider.bin")
    private val alias = "synthos_provider_settings_v1"
    private val aad = "synthos-provider-settings:v1".toByteArray(Charsets.UTF_8)

    fun read(): ProviderConfig {
        if (!file.exists()) return ProviderConfig()
        val bytes = file.readBytes()
        require(bytes.size in 17..128_000) { "Provider settings file has an invalid size." }
        require(bytes[0] == 'S'.code.toByte() && bytes[1] == 'P'.code.toByte() && bytes[2] == 'R'.code.toByte() && bytes[3] == '1'.code.toByte()) {
            "Provider settings file is not recognized. Save the provider again."
        }
        val ivLength = bytes[4].toInt() and 0xff
        require(ivLength == 12 && bytes.size > 5 + ivLength) { "Provider encryption metadata is invalid." }
        val iv = bytes.copyOfRange(5, 5 + ivLength)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(), GCMParameterSpec(128, iv))
        cipher.updateAAD(aad)
        val root = JSONObject(cipher.doFinal(bytes.copyOfRange(5 + ivLength, bytes.size)).toString(Charsets.UTF_8))
        return ProviderConfig(
            baseUrl = root.optString("baseUrl", "").take(500),
            model = root.optString("model", "").take(200),
            apiKey = root.optString("apiKey", "").take(4_000)
        )
    }

    fun write(config: ProviderConfig) {
        val plain = JSONObject().apply {
            put("baseUrl", config.baseUrl.take(500))
            put("model", config.model.take(200))
            put("apiKey", config.apiKey.take(4_000))
        }.toString().toByteArray(Charsets.UTF_8)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key())
        cipher.updateAAD(aad)
        val encrypted = cipher.doFinal(plain)
        file.parentFile?.mkdirs()
        file.writeBytes(byteArrayOf('S'.code.toByte(), 'P'.code.toByte(), 'R'.code.toByte(), '1'.code.toByte(), cipher.iv.size.toByte()) + cipher.iv + encrypted)
    }

    private fun key(): SecretKey {
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
