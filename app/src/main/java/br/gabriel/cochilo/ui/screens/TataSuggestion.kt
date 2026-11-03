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
/**
 * Tatá's note for the baby's age (§3.1): her words, her byline, a shop link only when the note is
 * about a product ([product] and [url]), and "Got it", which closes it for this age band.
 */
@Composable
fun TataAgeNote(band: br.gabriel.cochilo.domain.AgeNotes.Band, product: String?, url: String?, onClose: () -> Unit) {
    val context = LocalContext.current
    val text = stringResource(
        when (band) {
            br.gabriel.cochilo.domain.AgeNotes.Band.M0_1 -> R.string.age_note_m0_1
            br.gabriel.cochilo.domain.AgeNotes.Band.M1_2 -> R.string.age_note_m1_2
            br.gabriel.cochilo.domain.AgeNotes.Band.M2_3 -> R.string.age_note_m2_3
            br.gabriel.cochilo.domain.AgeNotes.Band.M3_4 -> R.string.age_note_m3_4
            br.gabriel.cochilo.domain.AgeNotes.Band.M4_6 -> R.string.age_note_m4_6
            br.gabriel.cochilo.domain.AgeNotes.Band.M6_8 -> R.string.age_note_m6_8
            br.gabriel.cochilo.domain.AgeNotes.Band.M8_10 -> R.string.age_note_m8_10
            br.gabriel.cochilo.domain.AgeNotes.Band.M10_12 -> R.string.age_note_m10_12
            br.gabriel.cochilo.domain.AgeNotes.Band.M12_15 -> R.string.age_note_m12_15
            br.gabriel.cochilo.domain.AgeNotes.Band.M15_18 -> R.string.age_note_m15_18
            br.gabriel.cochilo.domain.AgeNotes.Band.M18_21 -> R.string.age_note_m18_21
            br.gabriel.cochilo.domain.AgeNotes.Band.M21_24 -> R.string.age_note_m21_24
        }
    )
    val link = if (product != null && url != null) productLink(product) else null
    TataNote(
        text = text, byline = stringResource(R.string.tata_byline),
        linkLine = link?.let { stringResource(R.string.tata_link_line) }, linkAction = link?.first, linkSpoken = link?.second,
        onLink = if (link != null) { { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } } else null,
        dismiss = stringResource(R.string.age_note_close), onDismiss = onClose,
    )
}

/** The link words for a catalog product: the action and what TalkBack reads. */
@Composable
private fun productLink(product: String): Pair<String, String> = when (product) {
    "tata_facil_sling" -> stringResource(R.string.tata_link_sling) to stringResource(R.string.tata_link_sling_spoken)
    "tata_naninha" -> stringResource(R.string.tata_link_naninha) to stringResource(R.string.tata_link_naninha_spoken)
    "tata_to" -> stringResource(R.string.tata_link_to) to stringResource(R.string.tata_link_to_spoken)
    else -> stringResource(R.string.tata_link_shop) to stringResource(R.string.tata_link_shop_spoken)
}

@Composable
fun TataSuggestion(s: Suggestions.Suggestion, url: String?, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val text = when (s.moment) {
        Suggestions.Moment.ARMS_NAPS -> stringResource(R.string.tata_note_arms_naps, s.count ?: 0)
        Suggestions.Moment.ROUTINE_STEADY -> stringResource(R.string.tata_note_routine, s.count ?: 0)
        Suggestions.Moment.LITTLE_DAYLIGHT -> stringResource(R.string.tata_note_daylight)
        Suggestions.Moment.SIX_MONTHS -> stringResource(R.string.tata_note_six_months)
    }
    val (action, spoken) = productLink(s.product)
    TataNote(
        text = text, byline = stringResource(R.string.tata_byline),
        linkLine = stringResource(R.string.tata_link_line), linkAction = action, linkSpoken = spoken,
        onLink = url?.let { u -> { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(u)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } } },
        dismiss = stringResource(R.string.tata_dont_show), onDismiss = onDismiss,
    )
}
