package br.gabriel.cochilo.music

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import br.gabriel.cochilo.domain.Music

/**
 * Opens a routine link in its music app (0.58.0): Spotify, YouTube Music or YouTube, which plays
 * it whole, in the background, with the screen off. Without the app, whatever opens links does.
 * Nothing goes through Cochilo, and neither build needs a permission for this (the manifest only
 * declares the three apps in `<queries>`, so Android lets Cochilo see whether they're installed).
 */
object MusicOpener {
    /** The intent for [link]: to its own app when installed, else to whatever opens links. */
    fun intent(context: Context, link: Music.Link): Intent {
        val view = Intent(Intent.ACTION_VIEW, Uri.parse(link.url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val inApp = Intent(view).setPackage(link.service.packageName)
        return if (inApp.resolveActivity(context.packageManager) != null) inApp else view
    }

    fun open(context: Context, link: Music.Link): Boolean =
        runCatching { context.startActivity(intent(context, link)) }.isSuccess

    /** A notification button's tap (0.60.0): opens [link] straight in its app. */
    fun pending(context: Context, link: Music.Link): PendingIntent =
        PendingIntent.getActivity(context, REQUEST, intent(context, link), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)

    private const val REQUEST = 7
}
