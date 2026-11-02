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
    suspend fun delete(event: Event)

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

@Database(entities = [Event::class, Baby::class], version = 3, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CochiloDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun baby(): BabyDao

    companion object {
        fun create(context: Context): CochiloDb =
            Room.databaseBuilder(context, CochiloDb::class.java, "cochilo.db")
                .addMigrations(MEDICINE_TYPE, BABY_LOOK)
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
