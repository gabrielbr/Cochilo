package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.domain.Details
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.kit.kindColor
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/**
 * Details after (docs/flows.md, 0.28.0): once an entry ends, its one open question in a small
 * sheet: "Where did Bebê sleep?" with the places, the last one marked; one tap answers and closes.
 * Amounts get a field filled with the last amount and Save. "Not now" leaves it as it is.
 */
@Composable
fun DetailsSheet(e: Event, log: List<Event>, babyName: String, onKind: (String) -> Unit, onAmount: (Int) -> Unit, onSkip: () -> Unit) {
    Sheet(onSkip) { DetailsBody(e, log, babyName, onKind, onAmount, onSkip) }
}

@Composable
fun DetailsBody(e: Event, log: List<Event>, babyName: String, onKind: (String) -> Unit, onAmount: (Int) -> Unit, onSkip: () -> Unit) {
    val f = Format(LocalContext.current)
    val ask = Details.askAfter(e) ?: return
    Column(Modifier.fillMaxWidth()) {
        val title = when {
            ask == Details.Ask.PLACE -> stringResource(R.string.details_where, babyName)
            e.type == EventType.FEED && ask == Details.Ask.KIND -> stringResource(R.string.details_feed_kind)
            e.type == EventType.FEED -> stringResource(R.string.details_bottle_amount)
            e.type == EventType.DAYLIGHT -> stringResource(R.string.details_daylight)
            else -> stringResource(R.string.details_pumped)
        }
        SheetTitle(title)
        // What it was, so the question has its context: "Nap · 1:15 PM–2:30 PM · 1h 15m".
        Text(listOf(eventLabel(e, f, java.time.ZoneId.systemDefault()), eventMeta(e, e.end ?: e.start, f)).joinToString(" · "), style = Type.meta, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space4))
        if (ask == Details.Ask.AMOUNT) {
            var amount by remember { mutableStateOf(Details.lastAmount(e, log)?.toString() ?: "") }
            Field(amount, { amount = it.filter { ch -> ch.isDigit() }.take(4) }, stringResource(R.string.field_amount), keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
            Spacer(Modifier.height(Spacing.space6))
            val ml = amount.toIntOrNull()
            Button(stringResource(R.string.save), { ml?.let(onAmount) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = ml != null && ml > 0)
        } else {
            val last = Details.lastKind(e, log)
            FilterChips(
                Details.options(e), null, { f.kindName(it) ?: it },
                last = last, lastColor = CochiloTheme.colors.kindColor(e.type), lastSpoken = stringResource(R.string.last_used),
            ) { onKind(it) }
            Spacer(Modifier.height(Spacing.space4))
        }
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.details_skip), onSkip, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}
