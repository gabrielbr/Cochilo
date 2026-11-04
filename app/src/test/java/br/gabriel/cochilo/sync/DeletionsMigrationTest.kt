package br.gabriel.cochilo.sync

import br.gabriel.cochilo.data.CochiloDb
import org.junit.Assert.assertEquals
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
}
