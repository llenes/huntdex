package dev.huntdex.core.data.di

import dev.huntdex.core.data.network.PokemonApi
import dev.huntdex.core.data.network.buildHttpClient
import dev.huntdex.core.data.repository.PokemonRepositoryImpl
import dev.huntdex.core.domain.repository.PokemonRepository
import org.koin.dsl.module

val dataModule = module {
    single { buildHttpClient() }
    single { PokemonApi(get()) }
    single<PokemonRepository> { PokemonRepositoryImpl(get(), get()) }
}
