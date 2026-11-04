package br.gabriel.cochilo.sync

import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.spec.ECGenParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.KeyAgreement
import javax.crypto.Mac
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * The crypto partner sync rests on (docs/sync.md), all from the platform (java.security), nothing
 * hand-rolled but HKDF (RFC 5869, a few lines over HMAC-SHA256). Pure JVM, tested in SyncCryptoTest.
 *
 * - Pairing: each phone makes a fresh P-256 key pair; ECDH gives both the same secret. Both show a
 *   six-digit number derived from it and from everything said so far; if the parents see the same
 *   number on both phones, nobody stood in between (numeric comparison, like Bluetooth pairing).
 *   Then both keep a 32-byte pair key, and never send it.
 * - Syncing: the phones prove they hold the pair key (HMAC over fresh nonces), derive a key for
 *   this session, and send the log sealed with AES-256-GCM.
 */
object SyncCrypto {
    private val random = SecureRandom()

    fun keyPair(): KeyPair = KeyPairGenerator.getInstance("EC").apply { initialize(ECGenParameterSpec("secp256r1"), random) }.generateKeyPair()

    fun publicBytes(kp: KeyPair): ByteArray = kp.public.encoded

    fun agree(kp: KeyPair, peerPublic: ByteArray): ByteArray {
        val peer = KeyFactory.getInstance("EC").generatePublic(X509EncodedKeySpec(peerPublic))
        return KeyAgreement.getInstance("ECDH").apply { init(kp.private); doPhase(peer, true) }.generateSecret()
    }

    fun nonce(size: Int = NONCE): ByteArray = ByteArray(size).also(random::nextBytes)

    fun sha256(vararg parts: ByteArray): ByteArray = MessageDigest.getInstance("SHA-256").run { parts.forEach(::update); digest() }

    fun hmac(key: ByteArray, vararg parts: ByteArray): ByteArray =
        Mac.getInstance("HmacSHA256").run { init(SecretKeySpec(key, "HmacSHA256")); parts.forEach(::update); doFinal() }

    /** HKDF-SHA256 (RFC 5869): extract with [salt], expand with [info] to [length] bytes. */
    fun hkdf(ikm: ByteArray, salt: ByteArray, info: String, length: Int = KEY): ByteArray {
        val prk = hmac(salt, ikm)
        val out = java.io.ByteArrayOutputStream()
        var t = ByteArray(0)
        var i = 1
        while (out.size() < length) {
            t = hmac(prk, t, info.toByteArray(), byteArrayOf(i.toByte()))
            out.write(t)
            i++
        }
        return out.toByteArray().copyOf(length)
    }

    /** The six digits both phones show at pairing. */
    fun sas(shared: ByteArray, transcript: ByteArray): String {
        val b = hkdf(shared, transcript, "cochilo-sync-sas", 4)
        val n = ((b[0].toLong() and 0xff) shl 24) or ((b[1].toLong() and 0xff) shl 16) or ((b[2].toLong() and 0xff) shl 8) or (b[3].toLong() and 0xff)
        return "%06d".format(n % 1_000_000)
    }

    fun pairKey(shared: ByteArray, transcript: ByteArray): ByteArray = hkdf(shared, transcript, "cochilo-sync-pair-key")

    /** A short public name for a pair, so each phone finds its partner on the network. Not secret. */
    fun pairId(pairKey: ByteArray): String = sha256("cochilo-pair-id".toByteArray(), pairKey).take(8).joinToString("") { "%02x".format(it) }

    /** AES-256-GCM: a fresh 12-byte nonce, then the sealed bytes. [aad] is checked, not hidden. */
    fun seal(key: ByteArray, plain: ByteArray, aad: ByteArray): ByteArray {
        val iv = nonce(IV)
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply { init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, iv)); updateAAD(aad) }
        return iv + c.doFinal(plain)
    }

    /** Throws if the bytes were changed or sealed with another key. */
    fun open(key: ByteArray, sealed: ByteArray, aad: ByteArray): ByteArray {
        require(sealed.size > IV) { "too short" }
        val c = Cipher.getInstance("AES/GCM/NoPadding").apply {
            init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(TAG_BITS, sealed.copyOfRange(0, IV))); updateAAD(aad)
        }
        return c.doFinal(sealed.copyOfRange(IV, sealed.size))
    }

    /** Compares MACs in constant time. */
    fun same(a: ByteArray, b: ByteArray): Boolean = MessageDigest.isEqual(a, b)

    const val NONCE = 32
    const val KEY = 32
    private const val IV = 12
    private const val TAG_BITS = 128
}
