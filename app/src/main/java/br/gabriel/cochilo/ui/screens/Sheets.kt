package br.gabriel.cochilo.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Baby
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.DAY
import br.gabriel.cochilo.domain.localDate
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.Button
import br.gabriel.cochilo.ui.kit.ButtonKind
import br.gabriel.cochilo.ui.kit.DatePickerSheet
import br.gabriel.cochilo.ui.kit.Field
import br.gabriel.cochilo.ui.kit.FilterChips
import br.gabriel.cochilo.ui.kit.PickerField
import br.gabriel.cochilo.ui.kit.SwitchRow
import br.gabriel.cochilo.ui.kit.TimePickerSheet
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Spacing
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneId

/** The system's sheet: surface, radius 16 on top, a title, then content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun Sheet(onDismiss: () -> Unit, content: @Composable () -> Unit) {
    val c = CochiloTheme.colors
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.surface,
        contentColor = c.ink,
        shape = RoundedCornerShape(topStart = Radius.radiusL, topEnd = Radius.radiusL),
    ) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).imePadding().navigationBarsPadding()
                .padding(start = Spacing.space6, end = Spacing.space6, bottom = Spacing.space6)
        ) { content() }
    }
}

@Composable
private fun SheetTitle(text: String) {
    Text(text, style = Type.title, color = CochiloTheme.colors.ink, modifier = Modifier.semantics { heading() })
    Spacer(Modifier.height(Spacing.space6))
}

private fun at(date: LocalDate, time: LocalTime, zone: ZoneId) = date.atTime(time).atZone(zone).toInstant().toEpochMilli()
private fun timeOf(ms: Long, zone: ZoneId): LocalTime = Instant.ofEpochMilli(ms).atZone(zone).toLocalTime().withSecond(0).withNano(0)

/** Defaults for a new entry of [type]: timers start now; a manual sleep is the last half hour. */
fun newEvent(type: EventType, now: Long): Event = when (type) {
    EventType.SLEEP -> Event(type = type, start = now - 30 * 60_000, end = now)
    EventType.FEED, EventType.PUMP, EventType.DAYLIGHT -> Event(type = type, start = now, end = null,
        kind = Kinds.forType(type).firstOrNull())
    else -> Event(type = type, start = now, kind = Kinds.forType(type).firstOrNull())
}

@Composable
fun EventSheet(
    initial: Event,
    isNew: Boolean,
    zone: ZoneId,
    onSave: (Event) -> Unit,
    onDelete: (Event) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val f = Format(context, zone)
    var date by remember { mutableStateOf(localDate(initial.start, zone)) }
    var startTime by remember { mutableStateOf(timeOf(initial.start, zone)) }
    var endTime by remember { mutableStateOf(initial.end?.let { timeOf(it, zone) } ?: timeOf(initial.start, zone)) }
    var running by remember { mutableStateOf(initial.type.hasDuration && initial.end == null) }
    var kind by remember { mutableStateOf(initial.kind) }
    var amount by remember { mutableStateOf(initial.amountMl?.toString() ?: "") }
    var note by remember { mutableStateOf(initial.note ?: "") }
    var picking by remember { mutableStateOf<String?>(null) }
    var confirmDelete by remember { mutableStateOf(false) }

    val t = initial.type
    val ok = stringResource(R.string.ok)
    val cancel = stringResource(R.string.cancel)

    fun build(): Event {
        val start = at(date, startTime, zone)
        var end: Long? = null
        if (t.hasDuration && !running) {
            end = at(date, endTime, zone)
            if (end < start) end += DAY // crossed midnight
        }
        return initial.copy(
            start = start, end = end, kind = kind,
            amountMl = amount.filter { it.isDigit() }.toIntOrNull(),
            expressedAt = if (t == EventType.PUMP) start else initial.expressedAt,
            note = note.trim().ifEmpty { null },
        )
    }

    if (confirmDelete) {
        ConfirmSheet(
            title = stringResource(R.string.delete_confirm_title),
            line = stringResource(R.string.delete_confirm_line),
            confirm = stringResource(R.string.delete),
            onConfirm = { confirmDelete = false; onDelete(initial) },
            onDismiss = { confirmDelete = false },
        )
        return
    }

    Sheet(onDismiss) {
        SheetTitle(if (isNew) f.typeName(t) else stringResource(R.string.edit) + " · " + f.typeName(t))

        val kinds = Kinds.forType(t)
        if (kinds.isNotEmpty()) {
            Text(stringResource(R.string.field_kind), style = Type.meta, color = CochiloTheme.colors.muted)
            FilterChips(kinds, kind, { f.kindName(it) ?: it }) { kind = it }
            Spacer(Modifier.height(Spacing.space4))
        }

        PickerField(stringResource(R.string.field_date), f.day(date), { picking = "date" }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space4))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.space3)) {
            PickerField(
                stringResource(if (t.hasDuration) R.string.field_start else R.string.field_time),
                f.time(at(date, startTime, zone)), { picking = "start" }, Modifier.weight(1f),
            )
            if (t.hasDuration && !running) {
                PickerField(stringResource(R.string.field_end), f.time(at(date, endTime, zone)), { picking = "end" }, Modifier.weight(1f))
            }
        }
        if (t.hasDuration) {
            Spacer(Modifier.height(Spacing.space2))
            SwitchRow(stringResource(R.string.running_now), stringResource(R.string.running_now_meta), running) {
                running = it
                if (!it) endTime = timeOf(System.currentTimeMillis(), zone)
            }
        }
        if (t == EventType.FEED || t == EventType.PUMP) {
            Spacer(Modifier.height(Spacing.space4))
            Field(amount, { amount = it.filter { ch -> ch.isDigit() }.take(4) }, stringResource(R.string.field_amount),
                keyboard = KeyboardOptions(keyboardType = KeyboardType.Number))
        }
        Spacer(Modifier.height(Spacing.space4))
        Field(note, { note = it.take(280) }, stringResource(R.string.field_note), singleLine = false)

        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.save), { onSave(build()) }, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(cancel, onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
        if (!isNew) {
            Spacer(Modifier.height(Spacing.space6))
            Button(stringResource(R.string.delete), { confirmDelete = true }, ButtonKind.DANGER, Modifier.fillMaxWidth())
        }
    }

    when (picking) {
        "date" -> DatePickerSheet(date, ok, cancel, { date = it }, { picking = null })
        "start" -> TimePickerSheet(startTime, stringResource(R.string.field_start), ok, cancel, { startTime = it }, { picking = null })
        "end" -> TimePickerSheet(endTime, stringResource(R.string.field_end), ok, cancel, { endTime = it }, { picking = null })
    }
}

/** A confirm for anything destructive: title, one line, the danger action and Cancel. */
@Composable
fun ConfirmSheet(title: String, line: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    Sheet(onDismiss) {
        Text(title, style = Type.title, color = CochiloTheme.colors.ink, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Spacing.space2))
        Text(line, style = Type.body, color = CochiloTheme.colors.muted)
        Spacer(Modifier.height(Spacing.space6))
        Button(confirm, onConfirm, ButtonKind.DANGER, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}

@Composable
fun BabySheet(initial: Baby?, zone: ZoneId, onSave: (Baby) -> Unit, onDismiss: () -> Unit) {
    val f = Format(LocalContext.current, zone)
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var birth by remember { mutableStateOf(initial?.birthDate?.let { localDate(it, zone) } ?: LocalDate.now(zone)) }
    var due by remember { mutableStateOf(initial?.dueDate?.let { localDate(it, zone) }) }
    var picking by remember { mutableStateOf<String?>(null) }
    val ok = stringResource(R.string.ok)
    val cancel = stringResource(R.string.cancel)
    Sheet(onDismiss) {
        SheetTitle(stringResource(if (initial == null) R.string.add_baby else R.string.caption_baby))
        Field(name, { name = it.take(40) }, stringResource(R.string.baby_name))
        Spacer(Modifier.height(Spacing.space4))
        PickerField(stringResource(R.string.birth_date), f.day(birth), { picking = "birth" }, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space4))
        PickerField(stringResource(R.string.due_date), due?.let { f.day(it) } ?: stringResource(R.string.not_set), { picking = "due" }, Modifier.fillMaxWidth())
        Text(stringResource(R.string.due_date_meta), style = Type.meta, color = CochiloTheme.colors.muted,
            modifier = Modifier.padding(top = Spacing.space1))
        if (due != null) {
            Spacer(Modifier.height(Spacing.space2))
            Button(stringResource(R.string.clear), { due = null }, ButtonKind.SECONDARY)
        }
        Spacer(Modifier.height(Spacing.space6))
        Button(
            stringResource(R.string.save),
            {
                onSave(
                    Baby(
                        name = name.trim(),
                        birthDate = birth.atStartOfDay(zone).toInstant().toEpochMilli(),
                        dueDate = due?.atStartOfDay(zone)?.toInstant()?.toEpochMilli(),
                    )
                )
            },
            ButtonKind.PRIMARY, Modifier.fillMaxWidth(), enabled = name.isNotBlank(),
        )
        Spacer(Modifier.height(Spacing.space2))
        Button(cancel, onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
    when (picking) {
        "birth" -> DatePickerSheet(birth, ok, cancel, { birth = it }, { picking = null })
        "due" -> DatePickerSheet(due ?: birth, ok, cancel, { due = it }, { picking = null })
    }
}

/** What a shared or opened file holds, before anything is saved: Import or Cancel. */
@Composable
fun ImportSheet(pending: br.gabriel.cochilo.ui.PendingImport, zone: ZoneId, onImport: () -> Unit, onDismiss: () -> Unit) {
    val f = Format(LocalContext.current, zone)
    val c = CochiloTheme.colors
    Sheet(onDismiss) {
        SheetTitle(stringResource(
            if (pending.source == br.gabriel.cochilo.ui.ImportSource.NAPPER) R.string.import_napper_title else R.string.import_backup_title
        ))
        Text(
            stringResource(R.string.import_range, f.day(localDate(pending.from, zone)), f.day(localDate(pending.to, zone))),
            style = Type.body, color = c.ink,
        )
        Spacer(Modifier.height(Spacing.space2))
        Text(
            stringResource(R.string.import_counts, pending.sleeps, pending.feeds, pending.diapers, pending.medicine, pending.other),
            style = Type.bodySmall, color = c.ink,
        )
        if (pending.skipped > 0) {
            Spacer(Modifier.height(Spacing.space2))
            Text(stringResource(R.string.import_skipped, pending.skipped), style = Type.meta, color = c.muted)
        }
        Spacer(Modifier.height(Spacing.space2))
        Text(stringResource(R.string.import_json_meta), style = Type.meta, color = c.muted)
        Spacer(Modifier.height(Spacing.space6))
        Button(stringResource(R.string.import_action), onImport, ButtonKind.PRIMARY, Modifier.fillMaxWidth())
        Spacer(Modifier.height(Spacing.space2))
        Button(stringResource(R.string.cancel), onDismiss, ButtonKind.SECONDARY, Modifier.fillMaxWidth())
    }
}
