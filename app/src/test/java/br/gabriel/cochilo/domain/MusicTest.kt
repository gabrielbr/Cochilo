package br.gabriel.cochilo.domain

import br.gabriel.cochilo.domain.Music.Kind
import br.gabriel.cochilo.domain.Music.Service
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MusicTest {
    @Test fun a_spotify_playlist_link_is_cleaned_of_tracking() {
        val l = Music.parse("https://open.spotify.com/playlist/37i9dQZF1DWZd79rJ6a7lp?si=abc123&utm_source=copy-link")!!
        assertEquals("https://open.spotify.com/playlist/37i9dQZF1DWZd79rJ6a7lp", l.url)
        assertEquals(Service.SPOTIFY, l.service)
        assertEquals(Kind.PLAYLIST, l.kind)
    }

    @Test fun spotify_share_text_language_paths_and_uris_work() {
        assertEquals("https://open.spotify.com/track/4uLU6hMCjMI75M1A2tKUQC",
            Music.parse("Ouça no Spotify: https://open.spotify.com/intl-pt/track/4uLU6hMCjMI75M1A2tKUQC?si=x")!!.url)
        assertEquals(Kind.ALBUM, Music.parse("spotify:album:1ATL5GLyefJaxhQzSPVrLX")!!.kind)
    }

    @Test fun youtube_music_and_youtube_links_work() {
        val p = Music.parse("https://music.youtube.com/playlist?list=PLxyz_123-AB&si=track")!!
        assertEquals("https://music.youtube.com/playlist?list=PLxyz_123-AB", p.url)
        assertEquals(Service.YOUTUBE_MUSIC, p.service)
        assertEquals(Kind.TRACK, Music.parse("https://music.youtube.com/watch?v=dQw4w9WgXcQ&feature=share")!!.kind)
        assertEquals("https://www.youtube.com/watch?v=dQw4w9WgXcQ", Music.parse("https://youtu.be/dQw4w9WgXcQ?si=abc")!!.url)
        assertEquals(Service.YOUTUBE, Music.parse("https://m.youtube.com/watch?v=dQw4w9WgXcQ")!!.service)
    }

    @Test fun anything_else_is_refused() {
        assertNull(Music.parse("a lullaby"))
        assertNull(Music.parse("https://example.com/playlist/abc"))
        assertNull(Music.parse("https://open.spotify.com/"))
        assertNull(Music.parse("https://open.spotify.com/playlist/<script>"))
        assertNull(Music.parse("https://music.youtube.com/"))
    }

    @Test fun saved_links_round_trip_with_their_names_and_are_capped() {
        val a = Music.parse("https://open.spotify.com/playlist/abc", "Ninar")!!
        val b = Music.parse("https://music.youtube.com/watch?v=xyz")!!
        assertEquals(listOf(a, b), Music.decode(Music.encode(listOf(a, b))))
        val many = (1..8).map { Music.parse("https://open.spotify.com/track/t$it")!! }
        assertEquals(Music.MAX_LINKS, Music.decode(Music.encode(many)).size)
        assertEquals(Music.MAX_NAME, Music.parse("https://youtu.be/abc", "x".repeat(99))!!.name!!.length)
    }
}
