package br.gabriel.cochilo.domain

import java.net.URI

/**
 * Routine music (0.58.0, docs/rituals.md): the songs or playlists a family uses to put the baby
 * to sleep, saved as links and opened in the music app with one tap. Pure, so it's unit-tested.
 *
 * Why a link and not a player inside Cochilo: Spotify's embedded player plays only 30-second
 * previews inside an Android app, YouTube Music has no embed, an embedded player stops when the
 * screen goes off, and the play build has no internet at all. The music app plays the whole
 * playlist with the screen off, and Cochilo sends nothing.
 */
object Music {
    enum class Service(val packageName: String) {
        SPOTIFY("com.spotify.music"),
        YOUTUBE_MUSIC("com.google.android.apps.youtube.music"),
        YOUTUBE("com.google.android.youtube"),
    }

    enum class Kind { TRACK, PLAYLIST, ALBUM, ARTIST, OTHER }

    /** A saved link: [url] cleaned of tracking, and the [name] the parent gave it (or none). */
    data class Link(val url: String, val service: Service, val kind: Kind, val name: String? = null)

    const val MAX_LINKS = 5
    const val MAX_NAME = 40

    /**
     * The music link in [text] (a pasted "Listen on Spotify: https://…" works too), or null when
     * it isn't one from Spotify, YouTube Music or YouTube.
     */
    fun parse(text: String, name: String? = null): Link? {
        val raw = text.trim()
        spotifyUri(raw)?.let { return it.copy(name = clean(name)) }
        val found = Regex("""https?://\S+""").find(raw)?.value ?: return null
        val uri = runCatching { URI(found.trimEnd('.', ',', ')', '"', '\'')) }.getOrNull() ?: return null
        val host = uri.host?.lowercase()?.removePrefix("www.")?.removePrefix("m.") ?: return null
        val path = uri.path.orEmpty().trimEnd('/')
        val query = params(uri.rawQuery)
        val link = when (host) {
            "open.spotify.com", "play.spotify.com" -> {
                // Spotify adds a language step: /intl-pt/playlist/…
                val parts = path.split('/').filter { it.isNotEmpty() && !it.startsWith("intl-") }
                val type = parts.getOrNull(0) ?: return null
                val id = parts.getOrNull(1)?.takeIf { it.matches(ID) } ?: return null
                Link("https://open.spotify.com/$type/$id", Service.SPOTIFY, spotifyKind(type))
            }
            "music.youtube.com" -> when {
                path == "/watch" && query["v"] != null ->
                    Link("https://music.youtube.com/watch?v=${query["v"]}" + (query["list"]?.let { "&list=$it" } ?: ""), Service.YOUTUBE_MUSIC, if (query["list"] != null) Kind.PLAYLIST else Kind.TRACK)
                path == "/playlist" && query["list"] != null ->
                    Link("https://music.youtube.com/playlist?list=${query["list"]}", Service.YOUTUBE_MUSIC, Kind.PLAYLIST)
                path.startsWith("/browse/") -> Link("https://music.youtube.com$path", Service.YOUTUBE_MUSIC, Kind.ALBUM)
                path.startsWith("/channel/") -> Link("https://music.youtube.com$path", Service.YOUTUBE_MUSIC, Kind.ARTIST)
                else -> null
            }
            "youtube.com" -> when {
                path == "/watch" && query["v"] != null ->
                    Link("https://www.youtube.com/watch?v=${query["v"]}" + (query["list"]?.let { "&list=$it" } ?: ""), Service.YOUTUBE, if (query["list"] != null) Kind.PLAYLIST else Kind.TRACK)
                path == "/playlist" && query["list"] != null ->
                    Link("https://www.youtube.com/playlist?list=${query["list"]}", Service.YOUTUBE, Kind.PLAYLIST)
                else -> null
            }
            "youtu.be" -> path.removePrefix("/").takeIf { it.isNotEmpty() && it.matches(ID) }?.let {
                Link("https://www.youtube.com/watch?v=$it" + (query["list"]?.let { l -> "&list=$l" } ?: ""), Service.YOUTUBE, if (query["list"] != null) Kind.PLAYLIST else Kind.TRACK)
            }
            else -> null
        } ?: return null
        return link.copy(name = clean(name))
    }

    /** One link per line, stored as "name<TAB>url". */
    fun encode(links: List<Link>): String = links.take(MAX_LINKS).joinToString("\n") { "${it.name.orEmpty()}\t${it.url}" }

    fun decode(text: String?): List<Link> = text.orEmpty().lines().filter { it.isNotBlank() }.mapNotNull { line ->
        val name = line.substringBefore('\t', "")
        parse(line.substringAfter('\t'), name)
    }.take(MAX_LINKS)

    private fun spotifyUri(raw: String): Link? {
        val m = Regex("""^spotify:(track|playlist|album|artist|episode|show):([A-Za-z0-9]+)$""").find(raw) ?: return null
        val (type, id) = m.destructured
        return Link("https://open.spotify.com/$type/$id", Service.SPOTIFY, spotifyKind(type))
    }

    private fun spotifyKind(type: String) = when (type) {
        "track" -> Kind.TRACK
        "playlist" -> Kind.PLAYLIST
        "album" -> Kind.ALBUM
        "artist" -> Kind.ARTIST
        else -> Kind.OTHER
    }

    private fun params(q: String?): Map<String, String> = q.orEmpty().split('&').mapNotNull {
        val k = it.substringBefore('=', "")
        val v = it.substringAfter('=', "")
        if (k.isEmpty() || v.isEmpty() || !v.matches(ID)) null else k to v
    }.toMap()

    private fun clean(name: String?) = name?.trim()?.take(MAX_NAME)?.ifEmpty { null }

    /** What an id in these links can hold: no spaces, no markup. */
    private val ID = Regex("""[A-Za-z0-9_-]+""")
}
