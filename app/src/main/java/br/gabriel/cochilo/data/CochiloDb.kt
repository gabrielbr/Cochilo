package br.gabriel.cochilo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.room.migration.Migration
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

class Converters {
    @TypeConverter fun fromType(t: EventType): String = t.name
    @TypeConverter fun toType(s: String): EventType = EventType.valueOf(s)
}

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY start DESC")
    fun observeAll(): Flow<List<Event>>

    @Query("SELECT * FROM events ORDER BY start ASC")
    suspend fun all(): List<Event>

    @Query("SELECT * FROM events WHERE id = :id")
    suspend fun byId(id: String): Event?

    @Upsert
    suspend fun upsert(event: Event)

    @Upsert
    suspend fun upsertAll(events: List<Event>)

    @Delete
    suspend fun deleteRow(event: Event)

    /**
     * Deletes [event] and remembers that it was deleted (0.47.0), so a partner's phone deletes it
     * too at the next sync, instead of bringing it back (docs/sync.md).
     */
    @androidx.room.Transaction
    suspend fun delete(event: Event) {
        deleteRow(event)
        remember(Deletion(event.id, System.currentTimeMillis()))
    }

    @Upsert
    suspend fun remember(deletion: Deletion)

    @Query("SELECT * FROM deletions")
    suspend fun deletions(): List<Deletion>

    @Upsert
    suspend fun rememberAll(deletions: List<Deletion>)

    @Query("DELETE FROM events WHERE id IN (:ids)")
    suspend fun deleteIds(ids: List<String>)

    @Query("DELETE FROM events")
    suspend fun deleteAll()
}

@Dao
interface BabyDao {
    @Query("SELECT * FROM baby WHERE id = 1")
    fun observe(): Flow<Baby?>

    @Query("SELECT * FROM baby WHERE id = 1")
    suspend fun get(): Baby?

    @Upsert
    suspend fun upsert(baby: Baby)
}

@Database(entities = [Event::class, Baby::class, Deletion::class], version = 5, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CochiloDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun baby(): BabyDao

    companion object {
        fun create(context: Context): CochiloDb =
            Room.databaseBuilder(context, CochiloDb::class.java, "cochilo.db")
                .addMigrations(MEDICINE_TYPE, BABY_LOOK, NIGHTS, DELETIONS)
                .build()

        /**
         * 1 → 2: medicine became its own type (MEDICINE). Notes that were medicine (the Napper import
         * wrote "Medicine · <name>" / "Remédio · <name>") become MEDICINE entries holding just the
         * name. The table is unchanged; only those rows move. updatedAt moves on, so a backup made
         * after this wins over an older one on import.
         */
        val MEDICINE_TYPE = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(MEDICINE_SQL)
            }
        }

        /** 2 → 3: the baby's look for the avatar (0.13.0); existing babies get the default look. */
        val BABY_LOOK = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(LOOK_SQL)
            }
        }

        const val LOOK_SQL = "ALTER TABLE baby ADD COLUMN look TEXT NOT NULL DEFAULT ''"

        /**
         * 3 → 4: a sleep knows if it's the night or a nap, and a night knows when the baby was up
         * for the day (docs/nights.md, 0.42.0). Existing rows get null: they keep the clock rule.
         */
        val NIGHTS = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                NIGHTS_SQL.forEach(db::execSQL)
            }
        }

        val NIGHTS_SQL = listOf(
            "ALTER TABLE events ADD COLUMN night INTEGER",
            "ALTER TABLE events ADD COLUMN upForDay INTEGER",
        )

        /** 4 → 5: deleted entries are remembered, so partner sync deletes them too (docs/sync.md, 0.47.0). */
        val DELETIONS = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(DELETIONS_SQL)
            }
        }

        const val DELETIONS_SQL = "CREATE TABLE IF NOT EXISTS `deletions` (`id` TEXT NOT NULL, `deletedAt` INTEGER NOT NULL, PRIMARY KEY(`id`))"

        /** Kept as plain SQL so a JVM test can run it against SQLite. */
        const val MEDICINE_SQL = """
            UPDATE events SET
                type = 'MEDICINE',
                note = CASE WHEN instr(note, ' · ') > 0
                            THEN nullif(trim(substr(note, instr(note, ' · ') + 3)), '')
                            ELSE NULL END,
                updatedAt = CAST(strftime('%s', 'now') AS INTEGER) * 1000
            WHERE type = 'NOTE' AND (
                note IN ('Medicine', 'Remédio') OR note LIKE 'Medicine · %' OR note LIKE 'Remédio · %')
        """
    }
}
