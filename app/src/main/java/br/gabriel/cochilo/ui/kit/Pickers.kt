package br.gabriel.cochilo.ui.kit

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import android.text.format.DateFormat
import br.gabriel.cochilo.ui.theme.CochiloTheme
import br.gabriel.cochilo.ui.theme.Type
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

// Material's pickers, coloured from the tokens so they read as part of the kit.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DatePickerSheet(initial: LocalDate, okLabel: String, cancelLabel: String, onPick: (LocalDate) -> Unit, onDismiss: () -> Unit) {
    val c = CochiloTheme.colors
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial.atStartOfDay().toInstant(ZoneOffset.UTC).toEpochMilli()
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(onClick = {
                state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) }
                onDismiss()
            }) { Text(okLabel, style = Type.label, color = c.ink) }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel, style = Type.label, color = c.muted) } },
        colors = DatePickerDefaults.colors(containerColor = c.surface),
    ) {
        DatePicker(
            state = state, showModeToggle = false,
            colors = DatePickerDefaults.colors(
                containerColor = c.surface,
                selectedDayContainerColor = c.mint, selectedDayContentColor = c.onMint,
                todayDateBorderColor = c.ink, todayContentColor = c.ink,
                dayContentColor = c.ink, weekdayContentColor = c.muted,
                navigationContentColor = c.ink, yearContentColor = c.ink,
                selectedYearContainerColor = c.mint, selectedYearContentColor = c.onMint,
                titleContentColor = c.muted, headlineContentColor = c.ink,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TimePickerSheet(initial: LocalTime, title: String, okLabel: String, cancelLabel: String, onPick: (LocalTime) -> Unit, onDismiss: () -> Unit) {
    val c = CochiloTheme.colors
    val is24 = DateFormat.is24HourFormat(LocalContext.current)
    val state = rememberTimePickerState(initial.hour, initial.minute, is24)
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = c.surface,
        title = { Text(title, style = Type.label, color = c.ink) },
        text = {
            TimePicker(
                state = state,
                colors = TimePickerDefaults.colors(
                    clockDialColor = c.page, clockDialSelectedContentColor = c.onMint, clockDialUnselectedContentColor = c.ink,
                    selectorColor = c.mint, containerColor = c.surface,
                    periodSelectorSelectedContainerColor = c.mint, periodSelectorSelectedContentColor = c.onMint,
                    periodSelectorUnselectedContentColor = c.ink, periodSelectorBorderColor = c.line,
                    timeSelectorSelectedContainerColor = c.mint, timeSelectorSelectedContentColor = c.onMint,
                    timeSelectorUnselectedContainerColor = c.page, timeSelectorUnselectedContentColor = c.ink,
                ),
            )
        },
        confirmButton = {
            TextButton(onClick = { onPick(LocalTime.of(state.hour, state.minute)); onDismiss() }) {
                Text(okLabel, style = Type.label, color = c.ink)
            }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text(cancelLabel, style = Type.label, color = c.muted) } },
    )
}
