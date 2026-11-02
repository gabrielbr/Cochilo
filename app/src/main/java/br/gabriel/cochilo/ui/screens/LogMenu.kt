package br.gabriel.cochilo.ui.screens

import android.content.Context
import br.gabriel.cochilo.R
import br.gabriel.cochilo.data.Event
import br.gabriel.cochilo.data.EventType
import br.gabriel.cochilo.data.Kinds
import br.gabriel.cochilo.domain.lastKindAmong
import br.gabriel.cochilo.domain.lastOf
import br.gabriel.cochilo.ui.Format
import br.gabriel.cochilo.ui.kit.LogMenuItem
import br.gabriel.cochilo.ui.kit.SlideOption
import br.gabriel.cochilo.ui.kit.typeIcon

/** Special option keys the Log button hands back (not entry kinds). */
object LogKeys {
    const val START_SLEEP = "START_SLEEP"
    const val WOKE_UP = "WOKE_UP"
    const val START = "START"
    const val REPEAT_MEDICINE = "REPEAT_MEDICINE"
}

/**
 * The Log button's menu, most frequent nearest the thumb (docs/flows.md). Each kind's choices are
 * what a slide can log at once; the one used last is marked (remember actions), and the remembered
 * bottle amount and medicine are offered as choices of their own.
 */
fun logMenuItems(context: Context, f: Format, log: List<Event>, sleeping: Boolean): List<LogMenuItem> {
    fun a11y(t: EventType, what: String?) = context.getString(R.string.a11y_log, listOfNotNull(f.typeName(t), what).joinToString(" · "))
    fun kinds(t: EventType, keys: List<String>, label: (String) -> String): List<SlideOption> {
        val last = lastKindAmong(log, t, keys.toSet())
        // The Last mark is seen (outline and dot); TalkBack says "used last".
        return keys.map { k ->
            val spoken = a11y(t, f.kindName(k))
            SlideOption(k, label(k), if (k == last) context.getString(R.string.slide_last_spoken, spoken) else spoken, last = k == last)
        }
    }
    val lastBottle = log.filter { it.type == EventType.FEED && it.kind == Kinds.BOTTLE && it.amountMl != null }.maxByOrNull { it.start }
    val lastMedicine = lastOf(log, EventType.MEDICINE)?.note?.takeIf { it.isNotBlank() }
    fun item(t: EventType, options: List<SlideOption>) = LogMenuItem(t.name, f.typeName(t), typeIcon(t), options, t)
    return listOf(
        item(EventType.FEED, kinds(EventType.FEED, listOf(Kinds.BREAST_LEFT, Kinds.BREAST_RIGHT)) { k ->
            context.getString(if (k == Kinds.BREAST_LEFT) R.string.slide_left else R.string.slide_right)
        } + listOfNotNull(lastBottle?.let { b ->
            val ml = context.getString(R.string.ml, b.amountMl!!)
            SlideOption(Kinds.BOTTLE, ml, a11y(EventType.FEED, (f.kindName(Kinds.BOTTLE) ?: "") + " " + ml), icon = R.drawable.gg_ic_bottle)
        })),
        // "Both" is short here: the menu's choices sit beside each other.
        item(EventType.DIAPER, kinds(EventType.DIAPER, listOf(Kinds.WET, Kinds.BOTH, Kinds.DIRTY)) { k ->
            if (k == Kinds.BOTH) context.getString(R.string.diaper_both_short) else f.kindName(k) ?: k
        }),
        item(EventType.SLEEP, listOf(
            if (sleeping) SlideOption(LogKeys.WOKE_UP, context.getString(R.string.action_woke_up), context.getString(R.string.action_woke_up))
            else SlideOption(LogKeys.START_SLEEP, context.getString(R.string.action_start_sleep), context.getString(R.string.action_start_sleep))
        )),
        item(EventType.MEDICINE, listOfNotNull(lastMedicine?.let { SlideOption(LogKeys.REPEAT_MEDICINE, it, context.getString(R.string.slide_last_spoken, a11y(EventType.MEDICINE, it)), last = true) })),
        item(EventType.PUMP, listOf(SlideOption(LogKeys.START, context.getString(R.string.slide_start), a11y(EventType.PUMP, null)))),
        item(EventType.DAYLIGHT, kinds(EventType.DAYLIGHT, listOf(Kinds.OUTDOORS, Kinds.WINDOW)) { f.kindName(it) ?: it }),
        item(EventType.ROUTINE, kinds(EventType.ROUTINE, listOf(Kinds.NAP_ROUTINE, Kinds.BEDTIME_ROUTINE)) { f.kindName(it) ?: it }),
        item(EventType.NOTE, emptyList()),
    )
}
