package app.memo.android.data

import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/** Portable passphrase-encrypted backup envelope. The passphrase is never persisted by this codec. */
object EncryptedBackupCodec {
    private val magic = byteArrayOf(0x4d, 0x45, 0x4d, 0x4f, 0x45, 0x4e, 0x43, 0x31) // MEMOENC1
    private const val VERSION = 1
    private const val ITERATIONS = 310_000
    private const val MIN_ITERATIONS = 150_000
    private const val MAX_ITERATIONS = 1_000_000
    private const val SALT_BYTES = 16
    private const val NONCE_BYTES = 12
    private const val HEADER_BYTES = 8 + 1 + 4 + SALT_BYTES + NONCE_BYTES
    private const val TAG_BITS = 128
    private const val MAX_PLAINTEXT_BYTES = 40 * 1024 * 1024
    private const val MIN_PASSPHRASE_CHARS = 12

    fun encrypt(plaintext: ByteArray, passphrase: CharArray): ByteArray {
        try {
            require(passphrase.size >= MIN_PASSPHRASE_CHARS) { "Use a backup passphrase of at least 12 characters." }
            require(plaintext.isNotEmpty() && plaintext.size <= MAX_PLAINTEXT_BYTES) { "Backup content must be between 1 byte and 40 MB." }
            val salt = ByteArray(SALT_BYTES).also(SecureRandom()::nextBytes)
            val nonce = ByteArray(NONCE_BYTES).also(SecureRandom()::nextBytes)
            val header = makeHeader(ITERATIONS, salt, nonce)
            val key = deriveKey(passphrase, salt, ITERATIONS)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
                cipher.updateAAD(header)
                val ciphertext = cipher.doFinal(plaintext)
                return ByteArrayOutputStream(header.size + ciphertext.size).apply {
                    write(header)
                    write(ciphertext)
                }.toByteArray()
            } finally {
                key.fill(0)
            }
        } finally {
            passphrase.fill('\u0000')
        }
    }

    fun decrypt(envelope: ByteArray, passphrase: CharArray): ByteArray {
        try {
            require(envelope.size >= HEADER_BYTES + (TAG_BITS / 8)) { "This file is not a complete Memo encrypted backup." }
            require(envelope.size <= MAX_PLAINTEXT_BYTES + HEADER_BYTES + 64) { "Encrypted backup exceeds the 40 MB workspace limit." }
            val buffer = ByteBuffer.wrap(envelope).order(ByteOrder.BIG_ENDIAN)
            val foundMagic = ByteArray(magic.size).also(buffer::get)
            require(foundMagic.contentEquals(magic)) { "This file is not a Memo encrypted backup (MEMOENC1 header missing)." }
            val version = buffer.get().toInt() and 0xff
            require(version == VERSION) { "Encrypted backup version $version is not supported by this Memo build." }
            val iterations = buffer.int
            require(iterations in MIN_ITERATIONS..MAX_ITERATIONS) { "Encrypted backup uses an unsupported key-derivation setting." }
            val salt = ByteArray(SALT_BYTES).also(buffer::get)
            val nonce = ByteArray(NONCE_BYTES).also(buffer::get)
            val header = envelope.copyOfRange(0, HEADER_BYTES)
            val ciphertext = envelope.copyOfRange(HEADER_BYTES, envelope.size)
            require(ciphertext.size >= TAG_BITS / 8) { "Encrypted backup is truncated." }
            val key = deriveKey(passphrase, salt, iterations)
            try {
                val cipher = Cipher.getInstance("AES/GCM/NoPadding")
                cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, nonce))
                cipher.updateAAD(header)
                return try {
                    cipher.doFinal(ciphertext).also { plaintext ->
                        if (plaintext.size !in 1..MAX_PLAINTEXT_BYTES) {
                            plaintext.fill(0)
                            throw IllegalArgumentException("Decrypted workspace exceeds the size limit.")
                        }
                    }
                } catch (badTag: AEADBadTagException) {
                    throw IllegalArgumentException("The encrypted backup could not be authenticated. The passphrase may be wrong or the file may be damaged.", badTag)
                }
            } finally {
                key.fill(0)
            }
        } finally {
            passphrase.fill('\u0000')
        }
    }

    private fun makeHeader(iterations: Int, salt: ByteArray, nonce: ByteArray): ByteArray =
        ByteBuffer.allocate(HEADER_BYTES).order(ByteOrder.BIG_ENDIAN)
            .put(magic)
            .put(VERSION.toByte())
            .putInt(iterations)
            .put(salt)
            .put(nonce)
            .array()

    private fun deriveKey(passphrase: CharArray, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(passphrase, salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }
}
