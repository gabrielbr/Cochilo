package br.gabriel.cochilo.ui.kit

import androidx.annotation.DrawableRes
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds

// One line icon per kind of entry (the design system's line set: 22dp, stroke 1.75), so an entry
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
