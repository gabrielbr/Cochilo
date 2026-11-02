package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.Ritual
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.kit.Hairline
import br.gabriel.cochilo.ui.kit.ListRow
import br.gabriel.cochilo.ui.kit.RowGroup
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type

/** The steps of a routine [kind]: the parent's own, or the defaults in the app's language. */
@Composable
fun ritualFor(kind: String?, rituals: Map<String, String?>): List<String> {
    val saved = rituals[kind]
    if (saved != null) return Ritual.decode(saved)
    return when (kind) {
        Kinds.BEDTIME_ROUTINE -> stringArrayResource(R.array.ritual_bedtime).toList()
        Kinds.NAP_ROUTINE -> stringArrayResource(R.array.ritual_nap).toList()
        else -> emptyList()
    }
}

/**
 * Settings › Routine steps (docs/rituals.md): write the nap and bedtime steps once; each is ticked
 * in the routine's sheet. Add with the field, remove with the row's Remove; Save keeps them.
 */
@Composable
fun RitualSheet(rituals: Map<String, String?>, onSave: (String, List<String>) -> Unit, onDismiss: () -> Unit) {
    val f = Format(LocalContext.current)
    var kind by remember { mutableStateOf(Kinds.BEDTIME_ROUTINE) }
    val bedtime = ritualFor(Kinds.BEDTIME_ROUTINE, rituals)
    val nap = ritualFor(Kinds.NAP_ROUTINE, rituals)
    val lists = remember { mapOf(Kinds.BEDTIME_ROUTINE to mutableStateListOf(*bedtime.toTypedArray()), Kinds.NAP_ROUTINE to mutableStateListOf(*nap.toTypedArray())) }
    var adding by remember { mutableStateOf("") }
    val steps = lists.getValue(kind)
    Sheet(onDismiss) {
        SheetTitle(stringResource(R.string.ritual_title))
        Text(stringResource(R.string.ritual_line), style = Type.body, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space4))
        FilterChips(listOf(Kinds.BEDTIME_ROUTINE, Kinds.NAP_ROUTINE), kind, { f.kindName(it) ?: it }) { kind = it }
        Spacer(Modifier.height(Spacing.space3))
        if (steps.isNotEmpty()) {
            RowGroup {
                steps.forEachIndexed { i, step ->
                    if (i > 0) Hairline()
                    ListRow(step) {
                        Button(stringResource(R.string.ritual_remove), { steps.removeAt(i) }, ButtonKind.SECONDARY)
                    }
                }
            }
            Spacer(Modifier.height(Spacing.space3))
        }
        if (steps.size < Ritual.MAX_STEPS) {
            Row(verticalAlignment = Alignment.Bottom) {
                androidx.compose.foundation.layout.Box(Modifier.weight(1f)) {
                    Field(adding, { adding = it.take(Ritual.MAX_LENGTH) }, stringResource(R.string.ritual_new_step))
                }
                Spacer(Modifier.width(Spacing.space2))
                Button(stringResource(R.string.ritual_add), { if (adding.isNotBlank()) { steps.add(adding.trim()); adding = "" } }, ButtonKind.SECONDARY, enabled = adding.isNotBlank())
            }
        }
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.save), {
            lists.forEach { (k, v) -> onSave(k, v.toList()) }
            onDismiss()
        }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}
