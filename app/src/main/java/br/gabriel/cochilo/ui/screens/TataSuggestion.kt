package br.gabriel.cochilo.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.domain.Suggestions
import br.gabriel.cochilo.ui.kit.TataNote

/**
 * One of Tatá's suggestions as a Tatá note (docs/tata-slings/in-app-marketing.md §3.2): what the
 * parent logged first, then the product, her byline, one quiet link to the shop page in [url],
 * and "Don't show again". The words are drafts for Tatá to review before any moment is approved.
 */
@Composable
fun TataSuggestion(s: Suggestions.Suggestion, url: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = when (s.moment) {
        Suggestions.Moment.ARMS_NAPS -> stringResource(R.string.tata_note_arms_naps, s.count ?: 0)
        Suggestions.Moment.ROUTINE_STEADY -> stringResource(R.string.tata_note_routine, s.count ?: 0)
        Suggestions.Moment.LITTLE_DAYLIGHT -> stringResource(R.string.tata_note_daylight)
        Suggestions.Moment.SIX_MONTHS -> stringResource(R.string.tata_note_six_months)
    }
    val (action, spoken) = when (s.product) {
        "tata_facil_sling" -> stringResource(R.string.tata_link_sling) to stringResource(R.string.tata_link_sling_spoken)
        "tata_naninha" -> stringResource(R.string.tata_link_naninha) to stringResource(R.string.tata_link_naninha_spoken)
        "tata_to" -> stringResource(R.string.tata_link_to) to stringResource(R.string.tata_link_to_spoken)
        else -> stringResource(R.string.tata_link_shop) to stringResource(R.string.tata_link_shop_spoken)
    }
    TataNote(
        text = text, byline = stringResource(R.string.tata_byline),
        linkLine = stringResource(R.string.tata_link_line), linkAction = action, linkSpoken = spoken,
        onLink = url?.let { u -> { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
        dismiss = stringResource(R.string.tata_dont_show), onDismiss = onDismiss,
    )
}
