package br.gabriel.cochilo.data

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.util.UUID

/** Everything we log is one row: a sleep, a feed, a diaper, a walk in the sun… */
@Serializable
enum class EventType { SLEEP, FEED, PUMP, DIAPER, DAYLIGHT, ROUTINE, NOTE,
    /** A dose: the note holds what was given (since 0.5.0; older backups carry these as NOTE). */
    MEDICINE;

    /** Types that last a while and can run as a timer (end = null while running). */
    val hasDuration: Boolean get() = this == SLEEP || this == FEED || this == PUMP || this == DAYLIGHT
}

/** Sub-kinds, stored as text so new ones don't need a migration. */
object Kinds {
    // FEED
    const val BREAST_LEFT = "BREAST_LEFT"
    const val BREAST_RIGHT = "BREAST_RIGHT"
    const val BOTTLE = "BOTTLE"
    const val SOLIDS = "SOLIDS"
    // DIAPER
    const val WET = "WET"
    const val DIRTY = "DIRTY"
    const val BOTH = "BOTH"
    // ROUTINE
    const val NAP_ROUTINE = "NAP"
    const val BEDTIME_ROUTINE = "BEDTIME"
    // DAYLIGHT
    const val OUTDOORS = "OUTDOORS"
    const val WINDOW = "WINDOW"

    fun forType(type: EventType): List<String> = when (type) {
        EventType.FEED -> listOf(BREAST_LEFT, BREAST_RIGHT, BOTTLE, SOLIDS)
        EventType.DIAPER -> listOf(WET, DIRTY, BOTH)
        EventType.ROUTINE -> listOf(NAP_ROUTINE, BEDTIME_ROUTINE)
        EventType.DAYLIGHT -> listOf(OUTDOORS, WINDOW)
        else -> emptyList()
    }
}

@Serializable
@Entity(tableName = "events", indices = [Index("start"), Index("type")])
data class Event(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val type: EventType,
    /** Epoch millis. */
    val start: Long,
    /** Epoch millis; null while a timer runs, and for instant events (diaper, note, routine). */
    val end: Long? = null,
    val kind: String? = null,
    val amountMl: Int? = null,
    /** For pumped milk: when it was expressed, so day milk and night milk can be told apart. */
    val expressedAt: Long? = null,
    val note: String? = null,
    val updatedAt: Long = System.currentTimeMillis(),
) {
    val running: Boolean get() = type.hasDuration && end == null
    fun durationMs(now: Long): Long = ((end ?: now) - start).coerceAtLeast(0)
}

@Serializable
@Entity(tableName = "baby")
data class Baby(
    @PrimaryKey val id: Int = 1,
    val name: String,
    /** Epoch millis at local midnight of the birth date. */
    val birthDate: Long,
    /** Due date, for corrected age if the baby was born early. Null = born at term. */
    val dueDate: Long? = null,
    /** How the baby looks, for the avatar (domain.BabyLook, encoded). Empty = the default look. */
    @androidx.room.ColumnInfo(defaultValue = "")
    val look: String = "",
)
