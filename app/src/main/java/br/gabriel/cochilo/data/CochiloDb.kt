package br.gabriel.cochilo.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Delete
import androidx.room.Query
import androidx.room.Room
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

@Database(entities = [Event::class, Baby::class], version = 1, exportSchema = true)
@TypeConverters(Converters::class)
abstract class CochiloDb : RoomDatabase() {
    abstract fun events(): EventDao
    abstract fun baby(): BabyDao

    companion object {
        fun create(context: Context): CochiloDb =
            Room.databaseBuilder(context, CochiloDb::class.java, "cochilo.db").build()
    }
}
