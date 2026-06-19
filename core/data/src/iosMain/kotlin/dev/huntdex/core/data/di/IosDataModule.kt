package dev.huntdex.core.data.di

import dev.huntdex.core.data.db.DatabaseDriverFactory
import dev.huntdex.core.data.db.HuntdexDatabase
import org.koin.dsl.module

val iosDataModule = module {
    single { DatabaseDriverFactory() }
    single { HuntdexDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
