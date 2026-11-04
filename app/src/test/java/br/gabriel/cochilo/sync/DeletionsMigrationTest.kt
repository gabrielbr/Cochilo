package br.gabriel.cochilo.sync

import br.gabriel.cochilo.data.CochiloDb
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/** The 4 → 5 migration makes exactly the table Room expects, or the app would crash on update. */
class DeletionsMigrationTest {
    @Test fun migration_sql_matches_the_exported_schema() {
        val schema = File("schemas").walk().first { it.name == "5.json" }.readText()
        val create = Regex("\"tableName\": \"deletions\",\\s*\"createSql\": \"([^\"]+)\"").find(schema)!!.groupValues[1]
            .replace("\\u0060", "`").replace("\${TABLE_NAME}", "deletions")
        assertEquals(create, CochiloDb.DELETIONS_SQL)
    }

    /** 5 → 6 adds exactly the column Room expects (0.52.0). */
    @Test fun logged_by_column_matches_the_exported_schema() {
        val schema = File("schemas").walk().first { it.name == "6.json" }.readText()
        assertTrue(Regex("\"fieldPath\": \"loggedBy\",\\s*\"columnName\": \"loggedBy\",\\s*\"affinity\": \"TEXT\"").containsMatchIn(schema))
        assertEquals("ALTER TABLE events ADD COLUMN loggedBy TEXT", CochiloDb.LOGGED_BY_SQL)
    }
}
