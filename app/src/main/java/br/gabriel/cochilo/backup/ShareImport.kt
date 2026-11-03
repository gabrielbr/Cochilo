package br.gabriel.cochilo.backup

import android.content.Intent
import android.net.Uri
import android.os.Build

/**
 * Share to Cochilo (docs/backup.md, 0.41.0): what another app sent. Napper's "export" opens the
 * share sheet with a CSV; a file manager's "open with" sends a content Uri; some apps share the
 * CSV's text itself. Either way it goes through the same import as Settings › Import (a locked
 * backup, a Napper CSV or a Cochilo backup), which only adds and updates, never deletes.
 */
object ShareImport {
    sealed interface Shared {
        data class File(val uri: Uri) : Shared
        data class Text(val text: String) : Shared
    }

    fun from(intent: Intent?): Shared? = when (intent?.action) {
        Intent.ACTION_SEND -> stream(intent)?.let { Shared.File(it) }
            ?: intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { Shared.Text(it) }
        Intent.ACTION_VIEW -> intent.data?.takeIf { it.scheme == "content" }?.let { Shared.File(it) }
        else -> null
    }

    private fun stream(intent: Intent): Uri? {
        val extra = if (Build.VERSION.SDK_INT >= 33) intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
        else @Suppress("DEPRECATION") intent.getParcelableExtra(Intent.EXTRA_STREAM) as? Uri
        return extra ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    }
}
