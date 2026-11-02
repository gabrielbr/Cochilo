package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import br.gabriel.cochilo.ui.theme.CTokens
import br.gabriel.cochilo.ui.theme.CTokens.Radius
import br.gabriel.cochilo.ui.theme.CTokens.Size
import br.gabriel.cochilo.ui.theme.CochiloTheme
import java.time.ZoneId
import br.gabriel.cochilo.domain.isNightSleep

// One line icon per kind of entry, drawn in the kind's fabric colour on a soft round (KindIcon) (the design system's line set: 22dp, stroke 1.75), so an entry
// is known at a glance: a drop for a breastfeed, a bottle, a spoon, a pump, a diaper, the moon for
// sleep, the sun for daylight, a bath for a routine, a capsule for medicine, a pencil for a note.
// Always next to the entry's name: an icon helps recognition, it never replaces the word.

@DrawableRes
fun typeIcon(t: EventType): Int = when (t) {
    EventType.SLEEP -> R.drawable.gg_ic_moon
    EventType.FEED -> R.drawable.gg_ic_breast
    EventType.PUMP -> R.drawable.gg_ic_pump
    EventType.DIAPER -> R.drawable.gg_ic_diaper
    EventType.DAYLIGHT -> R.drawable.gg_ic_sun
    EventType.ROUTINE -> R.drawable.gg_ic_routine
    EventType.NOTE -> R.drawable.gg_ic_note
    EventType.MEDICINE -> R.drawable.gg_ic_medicine
}

/** The entry's own icon: a feed shows how it was given (breast, bottle, solids). */
@DrawableRes
fun entryIcon(e: Event): Int = when {
    e.type == EventType.FEED && e.kind == Kinds.BOTTLE -> R.drawable.gg_ic_bottle
    e.type == EventType.FEED && e.kind == Kinds.SOLIDS -> R.drawable.gg_ic_spoon
    else -> typeIcon(e.type)
}

/** A kind's data colour: its fabric (Lavanda sleep, Rosa Chá feed, Trevo diaper…). Sleep is the night's unless [night] is false. */
fun CTokens.Palette.kindColor(t: EventType, night: Boolean = true): Color = when (t) {
    EventType.SLEEP -> if (night) eventNight else eventNap
    EventType.FEED -> eventFeed
    EventType.PUMP -> eventPump
    EventType.DIAPER -> eventDiaper
    EventType.DAYLIGHT -> eventDaylight
    EventType.ROUTINE -> eventRoutine
    EventType.NOTE -> eventNote
    EventType.MEDICINE -> eventMedicine
}

/** The soft round behind a kind's icon. */
fun CTokens.Palette.kindSoft(t: EventType, night: Boolean = true): Color = when (t) {
    EventType.SLEEP -> if (night) eventNightSoft else eventNapSoft
    EventType.FEED -> eventFeedSoft
    EventType.PUMP -> eventPumpSoft
    EventType.DIAPER -> eventDiaperSoft
    EventType.DAYLIGHT -> eventDaylightSoft
    EventType.ROUTINE -> eventRoutineSoft
    EventType.NOTE -> eventNoteSoft
    EventType.MEDICINE -> eventMedicineSoft
}

/**
 * Kind icon (design system, components.md): the kind's line icon in its fabric colour, on a soft
 * round of the same colour, so each kind of entry is known by colour and shape. Always beside the
 * kind's name; the icon itself says nothing to TalkBack.
 */
@Composable
fun KindIcon(@DrawableRes icon: Int, color: Color, soft: Color, modifier: Modifier = Modifier) {
    Box(
        modifier.size(Size.kindBadge).clip(RoundedCornerShape(Radius.radiusRound)).background(soft),
        contentAlignment = Alignment.Center,
    ) { GIcon(icon, null, color) }
}

/** A kind of entry's icon in its round (a menu, a log row). */
@Composable
fun KindIcon(t: EventType, modifier: Modifier = Modifier) {
    val c = CochiloTheme.colors
    KindIcon(typeIcon(t), c.kindColor(t), c.kindSoft(t), modifier)
}

/** An entry's own icon in its round: a bottle for a bottle feed, the nap colour for a nap. */
@Composable
fun KindIcon(e: Event, zone: ZoneId, modifier: Modifier = Modifier) {
    val c = CochiloTheme.colors
    val night = e.type != EventType.SLEEP || e.isNightSleep(zone)
    KindIcon(entryIcon(e), c.kindColor(e.type, night), c.kindSoft(e.type, night), modifier)
}
