package dev.huntdex.core.data.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import dev.huntdex.core.data.db.HuntdexDatabase

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver =
        JdbcSqliteDriver("jdbc:sqlite:huntdex.db").also {
            HuntdexDatabase.Schema.create(it)
        }
}
