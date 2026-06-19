package dev.huntdex.core.data.di

import dev.huntdex.core.data.network.buildHttpClient
import org.koin.dsl.module

val dataModule = module {
    single { buildHttpClient() }
    // DatabaseDriverFactory is platform-specific, provided in the platform module
}
