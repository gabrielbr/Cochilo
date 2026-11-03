package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.domain.AgeNotes
import br.gabriel.cochilo.domain.Suggestions
import br.gabriel.cochilo.ui.kit.Caption
import br.gabriel.cochilo.ui.kit.GIcon
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.kit.ScreenTitle
import br.gabriel.cochilo.ui.kit.SwapAction
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * From Tatá (0.44.0, docs/tata-slings/in-app-marketing.md §13): a tab of its own for her notes,
 * which used to appear only now and then at the end of Patterns.
 *
 * What it shows:
 * - her byline once, under the title;
 * - this week's suggestion, if one is due;
 * - the note for the baby's age;
 * - the next band's note;
 * - the earlier notes, one tap away.
 *
 * Shop links appear only when [shopLinks] is on. That is off at night and while the baby sleeps,
 * like every Tatá suggestion.
 */
@Composable
fun TataScreen(
    babyName: String,
    ageWeeks: Int,
    noteProduct: (AgeNotes.Band) -> String?,
    noteUrl: (AgeNotes.Band) -> String?,
    shopLinks: Boolean,
    suggestion: Suggestions.Suggestion? = null,
    suggestionUrl: String? = null,
    onDismissSuggestion: () -> Unit = {},
    onShop: () -> Unit = {},
) {
    val c = CochiloTheme.colors
    val bands = AgeNotes.Band.entries
    val now = AgeNotes.bandFor(ageWeeks) ?: if (ageWeeks < 0) bands.first() else bands.last()
    val i = bands.indexOf(now)
    @Composable
    fun note(b: AgeNotes.Band) = TataAgeNote(
        b, noteProduct(b), if (shopLinks) noteUrl(b) else null, signed = false,
    )
    ScrollPage {
        ScreenTitle(stringResource(R.string.title_tata))
        Spacer(Modifier.height(Spacing.space2))
        Text(stringResource(R.string.tata_byline), style = Type.meta, color = c.muted)

        if (suggestion != null) {
            Caption(stringResource(R.string.tata_this_week), top = Spacing.space8)
            TataSuggestion(suggestion, suggestionUrl, onDismissSuggestion, signed = false)
        }

        val forAge = if (babyName.isBlank()) stringResource(R.string.tata_for_age_plain) else stringResource(R.string.tata_for_age, babyName)
        Caption(forAge, top = if (suggestion == null) Spacing.space8 else Spacing.space10)
        note(now)

        bands.getOrNull(i + 1)?.let { next ->
            Caption(stringResource(R.string.tata_coming_up))
            note(next)
        }

        val earlier = bands.take(i).reversed()
        if (earlier.isNotEmpty()) {
            var open by rememberSaveable { mutableStateOf(false) }
            Spacer(Modifier.height(Spacing.space6))
            SwapAction(
                stringResource(if (open) R.string.tata_earlier_hide else R.string.tata_earlier_show, earlier.size), c.muted,
            ) { open = !open }
            if (open) earlier.forEach { b ->
                Spacer(Modifier.height(Spacing.space3))
                note(b)
            }
        }

        // Her shop, always here and never pushed (§3.4); the same row as Settings › About.
        Caption(stringResource(R.string.tata_shop_caption))
        RowGroup {
            ListRow(
                stringResource(R.string.brand_line), meta = stringResource(R.string.brand_meta), onClick = onShop,
                trailing = { GIcon(R.drawable.gg_ic_chevron, null, c.muted) },
            )
        }
    }
}
