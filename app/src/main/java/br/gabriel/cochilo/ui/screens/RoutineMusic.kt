package br.gabriel.cochilo.ui.screens

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.domain.Music
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * Opens a routine link in its music app (0.58.0): Spotify, YouTube Music or YouTube, which plays
 * it whole, in the background, with the screen off. Without the app, whatever opens links does.
 * Nothing goes through Cochilo, and neither build needs a permission for this.
 */
object MusicOpener {
    fun open(context: Context, link: Music.Link): Boolean {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(link.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return runCatching { context.startActivity(Intent(view).setPackage(link.service.packageName)) }.isSuccess ||
            runCatching { context.startActivity(view) }.isSuccess
    }
}

/** "Spotify · Playlist", what a link is, when the parent gave it no name. */
@Composable
fun musicMeta(l: Music.Link): String {
    val service = when (l.service) {
        Music.Service.SPOTIFY -> "Spotify"
        Music.Service.YOUTUBE_MUSIC -> "YouTube Music"
        Music.Service.YOUTUBE -> "YouTube"
    }
    val kind = stringResource(when (l.kind) {
        Music.Kind.TRACK -> R.string.music_kind_track
        Music.Kind.PLAYLIST -> R.string.music_kind_playlist
        Music.Kind.ALBUM -> R.string.music_kind_album
        Music.Kind.ARTIST -> R.string.music_kind_artist
        Music.Kind.OTHER -> R.string.music_kind_other
    })
    return "$service · $kind"
}

@Composable
fun musicName(l: Music.Link): String = l.name ?: musicMeta(l)

/**
 * Settings › Routine music (0.58.0, docs/rituals.md): paste a Spotify or YouTube Music link (a
 * song or a playlist) and, if you like, a name. Each row plays it; Remove takes it off; Save
 * keeps the list.
 */
@Composable
fun RoutineMusicSheet(saved: List<Music.Link>, onSave: (List<Music.Link>) -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) { RoutineMusicBody(saved, onSave, onDismiss) }
}

/** The sheet's content, also drawn by the screenshot test. */
@Composable
fun RoutineMusicBody(saved: List<Music.Link>, onSave: (List<Music.Link>) -> Unit, onDismiss: () -> Unit, startUrl: String = "", startWrong: Boolean = false) {
    val context = LocalContext.current
    val links = remember { mutableStateListOf(*saved.toTypedArray()) }
    var url by remember { mutableStateOf(startUrl) }
    var name by remember { mutableStateOf("") }
    var wrong by remember { mutableStateOf(startWrong) }
    run {
        SheetTitle(stringResource(R.string.music_title))
        Text(stringResource(R.string.music_line), style = Type.body, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space4))
        if (links.isNotEmpty()) {
            RowGroup {
                links.forEachIndexed { i, l ->
                    if (i > 0) Hairline()
                    ListRow(musicName(l), meta = if (l.name != null) musicMeta(l) else null, onClick = { MusicOpener.open(context, l) }) {
                        Button(stringResource(R.string.ritual_remove), { links.removeAt(i) }, ButtonKind.SECONDARY)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space4))
        }
        if (links.size < Music.MAX_LINKS) {
            Field(url, { url = it; wrong = false }, stringResource(R.string.music_link), placeholder = stringResource(R.string.music_link_hint))
            if (wrong) {
                Spacer(Modifier.height(Spacing.space1))
                Text(stringResource(R.string.music_wrong), style = Type.meta, color = CochiloTheme.colors.ink)
            }
            Spacer(Modifier.height(Spacing.space3))
            Field(name, { name = it.take(Music.MAX_NAME) }, stringResource(R.string.music_name))
            Spacer(Modifier.height(Spacing.space3))
            Button(stringResource(R.string.music_add), {
                val l = Music.parse(url, name)
                if (l == null) wrong = true else { links.add(l); url = ""; name = "" }
            }, ButtonKind.SECONDARY, Modifier.fillMaxWidth(), enabled = url.isNotBlank())
        }
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.save), { onSave(links.toList()); onDismiss() }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

/** In a routine's sheet: the saved music, one tap each to play it (0.58.0). */
@Composable
fun RoutineMusicButtons(links: List<Music.Link>) {
    if (links.isEmpty()) return
    val context = LocalContext.current
    Text(stringResource(R.string.music_in_routine), style = Type.meta, color = CochiloTheme.colors.muted)
    Spacer(Modifier.height(Spacing.space2))
    links.forEach { l ->
        Button(stringResource(R.string.music_play, musicName(l)), { MusicOpener.open(context, l) }, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
    }
}
