package br.gabriel.cochilo.sync

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream

/**
 * The two conversations partner sync has over a connection (docs/sync.md). They run over any pair
 * of streams, a socket on the family's Wi-Fi in the app, pipes in SyncProtocolTest. Pure JVM,
 * blocking: call them off the main thread.
 */

/** Length-prefixed frames, at most [MAX] bytes each. */
open class Wire(input: InputStream, output: OutputStream) : java.io.Closeable {
    private val inp = DataInputStream(input)
    private val out = DataOutputStream(output)

    /** Ends the conversation: the other side's next read fails at once instead of waiting. */
    override fun close() { runCatching { out.close() }; runCatching { inp.close() } }

    fun send(bytes: ByteArray) { out.writeInt(bytes.size); out.write(bytes); out.flush() }

    fun receive(): ByteArray {
        val n = inp.readInt()
        if (n < 0 || n > MAX) throw IOException("frame too big")
        return ByteArray(n).also(inp::readFully)
    }

    fun sendText(s: String) = send(s.toByteArray())
    fun receiveText(): String = String(receive())

    companion object {
        /** 64 MB: years of a log, with room to spare. */
        const val MAX = 64 shl 20
    }
}

internal val syncJson = Json { ignoreUnknownKeys = true; encodeDefaults = true }

@Serializable
internal data class PairHello(val v: Int, val name: String, val pub: String, val nonce: String)

@Serializable
internal data class SyncHello(val v: Int, val pair: String, val nonce: String)

@Serializable
internal data class SyncReply(val nonce: String, val mac: String)

private fun ByteArray.hex() = joinToString("") { "%02x".format(it) }
private fun String.unhex(): ByteArray = chunked(2).map { it.toInt(16).toByte() }.toByteArray()

/** A finished pairing: the key both phones keep, and the partner's name. */
data class Paired(val key: ByteArray, val partnerName: String) {
    val pairId get() = SyncCrypto.pairId(key)
}

/** Thrown when the other phone isn't the partner, or answered with something else. */
class SyncRefused(reason: String) : IOException(reason)

object Pairing {
    const val VERSION = 1

    /**
     * Pairs over [wire]. The [guest] is the phone that picked the other from the list. Both
     * compute the same six digits; [confirm] shows them with the partner's name and returns the
     * parent's answer (it may block until then). Returns null if either parent said no.
     */
    fun run(wire: Wire, myName: String, guest: Boolean, confirm: (sas: String, partnerName: String) -> Boolean): Paired? {
        val kp = SyncCrypto.keyPair()
        val mine = PairHello(VERSION, myName, SyncCrypto.publicBytes(kp).hex(), SyncCrypto.nonce().hex())
        val theirs: PairHello
        if (guest) {
            wire.sendText(syncJson.encodeToString(PairHello.serializer(), mine))
            theirs = syncJson.decodeFromString(PairHello.serializer(), wire.receiveText())
        } else {
            theirs = syncJson.decodeFromString(PairHello.serializer(), wire.receiveText())
            wire.sendText(syncJson.encodeToString(PairHello.serializer(), mine))
        }
        if (theirs.v != VERSION) throw SyncRefused("version")
        val (g, h) = if (guest) mine to theirs else theirs to mine
        val transcript = SyncCrypto.sha256(
            "cochilo-pair-v1".toByteArray(), g.pub.unhex(), h.pub.unhex(), g.nonce.unhex(), h.nonce.unhex(),
            g.name.toByteArray(), byteArrayOf(0), h.name.toByteArray(),
        )
        val shared = SyncCrypto.agree(kp, theirs.pub.unhex())
        val sas = SyncCrypto.sas(shared, transcript)
        val key = SyncCrypto.pairKey(shared, transcript)
        if (!confirm(sas, theirs.name)) return null
        // Each phone proves the parent said yes, with a MAC only the same key can make.
        val me = if (guest) "guest" else "host"
        val them = if (guest) "host" else "guest"
        wire.send(SyncCrypto.hmac(key, "confirm".toByteArray(), me.toByteArray()))
        val got = wire.receive()
        if (!SyncCrypto.same(got, SyncCrypto.hmac(key, "confirm".toByteArray(), them.toByteArray()))) throw SyncRefused("not confirmed")
        return Paired(key, theirs.name)
    }
}

object SyncSession {
    /**
     * The phone that reached out. Proves it holds [key], checks the partner does too, sends
     * [local] sealed and returns the partner's data.
     */
    fun asClient(wire: Wire, key: ByteArray, local: () -> SyncData): SyncData {
        val nc = SyncCrypto.nonce()
        wire.sendText(syncJson.encodeToString(SyncHello.serializer(), SyncHello(SyncData.VERSION, SyncCrypto.pairId(key), nc.hex())))
        val reply = syncJson.decodeFromString(SyncReply.serializer(), wire.receiveText())
        val ns = reply.nonce.unhex()
        if (!SyncCrypto.same(reply.mac.unhex(), SyncCrypto.hmac(key, "S".toByteArray(), nc, ns))) throw SyncRefused("not the partner")
        wire.send(SyncCrypto.hmac(key, "C".toByteArray(), nc, ns))
        val session = SyncCrypto.hkdf(key, nc + ns, "cochilo-sync-session")
        wire.send(SyncCrypto.seal(session, encode(local()), "C".toByteArray()))
        return decode(SyncCrypto.open(session, wire.receive(), "S".toByteArray()))
    }

    /** The phone that was found. The same, the other way round; refuses anyone without [key]. */
    fun asServer(wire: Wire, key: ByteArray, local: () -> SyncData): SyncData {
        val hello = syncJson.decodeFromString(SyncHello.serializer(), wire.receiveText())
        if (hello.pair != SyncCrypto.pairId(key)) throw SyncRefused("another pair")
        if (hello.v > SyncData.VERSION) throw SyncRefused("newer")
        val nc = hello.nonce.unhex()
        val ns = SyncCrypto.nonce()
        wire.sendText(syncJson.encodeToString(SyncReply.serializer(), SyncReply(ns.hex(), SyncCrypto.hmac(key, "S".toByteArray(), nc, ns).hex())))
        if (!SyncCrypto.same(wire.receive(), SyncCrypto.hmac(key, "C".toByteArray(), nc, ns))) throw SyncRefused("not the partner")
        val session = SyncCrypto.hkdf(key, nc + ns, "cochilo-sync-session")
        val remote = decode(SyncCrypto.open(session, wire.receive(), "C".toByteArray()))
        wire.send(SyncCrypto.seal(session, encode(local()), "S".toByteArray()))
        return remote
    }

    private fun encode(d: SyncData) = syncJson.encodeToString(SyncData.serializer(), d).toByteArray()
    private fun decode(b: ByteArray): SyncData {
        val d = syncJson.decodeFromString(SyncData.serializer(), String(b))
        if (d.version > SyncData.VERSION) throw SyncRefused("newer")
        return d
    }
}
