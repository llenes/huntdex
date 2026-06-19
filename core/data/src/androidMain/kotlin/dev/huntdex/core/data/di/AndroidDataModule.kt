package dev.huntdex.core.data.di

import dev.huntdex.core.data.db.DatabaseDriverFactory
import dev.huntdex.core.data.db.HuntdexDatabase
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val androidDataModule = module {
    single { DatabaseDriverFactory(androidContext()) }
    single { HuntdexDatabase(get<DatabaseDriverFactory>().createDriver()) }
}
