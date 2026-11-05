package app.memo.android.data

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EncryptedBackupCodecTest {
    @Test fun roundTripsPortableEnvelopeAndDoesNotExposePlaintext() {
        val original = "{\"schemaVersion\":5,\"private\":\"career profile\"}".toByteArray()
        val envelope = EncryptedBackupCodec.encrypt(original, "correct horse battery".toCharArray())
        assertFalse(String(envelope, Charsets.ISO_8859_1).contains("career profile"))
        val restored = EncryptedBackupCodec.decrypt(envelope, "correct horse battery".toCharArray())
        assertArrayEquals(original, restored)
    }

    @Test fun wrongPassphraseCannotAuthenticateBackup() {
        val envelope = EncryptedBackupCodec.encrypt("memo workspace".toByteArray(), "correct horse battery".toCharArray())
        val error = runCatching { EncryptedBackupCodec.decrypt(envelope, "incorrect passphrase".toCharArray()) }.exceptionOrNull()
        assertTrue(error is IllegalArgumentException)
        assertTrue(error?.message.orEmpty().contains("could not be authenticated"))
    }

    @Test fun tamperingAndTruncationAreRejected() {
        val envelope = EncryptedBackupCodec.encrypt("memo workspace".toByteArray(), "correct horse battery".toCharArray())
        val tampered = envelope.clone().also { it[it.lastIndex] = (it.last().toInt() xor 1).toByte() }
        assertTrue(runCatching { EncryptedBackupCodec.decrypt(tampered, "correct horse battery".toCharArray()) }.isFailure)
        assertTrue(runCatching { EncryptedBackupCodec.decrypt(envelope.copyOf(20), "correct horse battery".toCharArray()) }.isFailure)
    }

    @Test fun requiresStrongPassphraseAndRecognizedHeader() {
        assertTrue(runCatching { EncryptedBackupCodec.encrypt(byteArrayOf(1), "short".toCharArray()) }.isFailure)
        assertTrue(runCatching { EncryptedBackupCodec.decrypt(byteArrayOf(1, 2, 3), "correct horse battery".toCharArray()) }.isFailure)
    }
}
