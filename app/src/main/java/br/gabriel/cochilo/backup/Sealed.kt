package br.gabriel.cochilo.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.AEADBadTagException
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

/**
 * A passphrase-protected backup (docs/backup.md): the same backup JSON, sealed with AES-256-GCM
 * under a key stretched from the parent's passphrase (PBKDF2-HMAC-SHA256). Only the file that
 * leaves the phone is sealed; the app's own data is unchanged. Pure JVM crypto, so it's unit-tested
 * like the rest. Without the passphrase the file can't be read, and nobody, Cochilo included, can
 * recover it.
 */
@Serializable
data class SealedFile(
    val format: String = FORMAT,
    val version: Int = VERSION,
    val kdf: String = KDF,
    val iterations: Int = ITERATIONS,
    val salt: String,
    val iv: String,
    val data: String,
) {
    companion object {
        const val FORMAT = "cochilo-backup-sealed"
        const val VERSION = 1
        const val KDF = "PBKDF2WithHmacSHA256"
        /** OWASP 2023's floor for PBKDF2-HMAC-SHA256; about a second on a mid-range phone. */
        const val ITERATIONS = 600_000
    }
}

/** The passphrase didn't open the file (wrong, or the file was changed). */
class WrongPassphrase : Exception("wrong passphrase")

object Sealed {
    private const val KEY_BITS = 256
    private const val SALT_BYTES = 16
    private const val IV_BYTES = 12
    private const val TAG_BITS = 128
    /** Short passphrases are allowed (it's the parent's choice) but not empty ones. */
    const val MIN_LENGTH = 4

    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true; encodeDefaults = true }
    private val random = SecureRandom()
    private val b64 = Base64.getEncoder()
    private val unb64 = Base64.getDecoder()

    /** Whether [text] is a sealed backup, without opening it. */
    fun isSealed(text: String): Boolean = text.contains("\"${SealedFile.FORMAT}\"")

    fun seal(plain: String, passphrase: String, iterations: Int = SealedFile.ITERATIONS): String {
        require(passphrase.length >= MIN_LENGTH) { "passphrase too short" }
        val salt = ByteArray(SALT_BYTES).also(random::nextBytes)
        val iv = ByteArray(IV_BYTES).also(random::nextBytes)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, key(passphrase, salt, iterations), GCMParameterSpec(TAG_BITS, iv))
        cipher.updateAAD(SealedFile.FORMAT.toByteArray())
        val data = cipher.doFinal(plain.toByteArray())
        val file = SealedFile(iterations = iterations, salt = b64.encodeToString(salt), iv = b64.encodeToString(iv), data = b64.encodeToString(data))
        return json.encodeToString(SealedFile.serializer(), file)
    }

    /** Throws [WrongPassphrase] when [passphrase] doesn't open it, IllegalArgumentException when it isn't a sealed file. */
    fun open(text: String, passphrase: String): String {
        val file = try {
            json.decodeFromString(SealedFile.serializer(), text)
        } catch (e: Exception) {
            throw IllegalArgumentException("not a sealed Cochilo backup", e)
        }
        require(file.format == SealedFile.FORMAT) { "not a sealed Cochilo backup" }
        require(file.version <= SealedFile.VERSION) { "made by a newer Cochilo" }
        require(file.kdf == SealedFile.KDF) { "unknown key derivation" }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, key(passphrase, unb64.decode(file.salt), file.iterations), GCMParameterSpec(TAG_BITS, unb64.decode(file.iv)))
        cipher.updateAAD(SealedFile.FORMAT.toByteArray())
        return try {
            cipher.doFinal(unb64.decode(file.data)).decodeToString()
        } catch (e: AEADBadTagException) {
            throw WrongPassphrase()
        }
    }

    private fun key(passphrase: String, salt: ByteArray, iterations: Int): SecretKeySpec {
        val spec = PBEKeySpec(passphrase.toCharArray(), salt, iterations, KEY_BITS)
        try {
            val bytes = SecretKeyFactory.getInstance(SealedFile.KDF).generateSecret(spec).encoded
            return SecretKeySpec(bytes, "AES")
        } finally {
            spec.clearPassword()
        }
    }
}
