package dev.huntdex.core.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.huntdex.core.data.db.HuntdexDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:huntdex.db")

        val tables = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            { cursor ->
                val list = mutableListOf<String>()
                while (cursor.next().value) { cursor.getString(0)?.let { list.add(it) } }
                QueryResult.Value(list)
            },
            0
        ).value

        when {
            tables.isEmpty() ->
                // Fresh database — create everything
                HuntdexDatabase.Schema.create(driver)
            "pokemon_entry" !in tables -> {
                // Phase 0 database — add the Phase 1 Pokémon cache tables
                driver.execute(null, """
                    CREATE TABLE pokemon_entry (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL,
                        sprite_url TEXT NOT NULL
                    )
                """.trimIndent(), 0)
                driver.execute(null, """
                    CREATE TABLE pokemon_detail (
                        id INTEGER PRIMARY KEY,
                        data TEXT NOT NULL
                    )
                """.trimIndent(), 0)
                driver.execute(null, """
                    CREATE TABLE move_entry (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """.trimIndent(), 0)
                driver.execute(null, """
                    CREATE TABLE move_detail (
                        id INTEGER PRIMARY KEY,
                        data TEXT NOT NULL
                    )
                """.trimIndent(), 0)
            }
            "move_entry" !in tables -> {
                // Phase 1 database — add the Phase 2 Move cache tables
                driver.execute(null, """
                    CREATE TABLE move_entry (
                        id INTEGER PRIMARY KEY,
                        name TEXT NOT NULL
                    )
                """.trimIndent(), 0)
                driver.execute(null, """
                    CREATE TABLE move_detail (
                        id INTEGER PRIMARY KEY,
                        data TEXT NOT NULL
                    )
                """.trimIndent(), 0)
            }
            // else: all tables present, nothing to do
        }

        return driver
    }
}
