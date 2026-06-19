package dev.huntdex.desktopapp.di

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.desktopapp.navigation.DesktopNavigatorAdapter
import dev.huntdex.desktopapp.screens.DesktopDetailScreenModel
import dev.huntdex.desktopapp.screens.DesktopHomeScreenModel
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreenModel
import dev.huntdex.feature.pokedex.list.PokemonListScreenModel
import org.koin.dsl.module

fun desktopAppModule(adapter: DesktopNavigatorAdapter) = module {
    single<AppNavigator> { adapter }
    factory { DesktopHomeScreenModel(get()) }
    factory { DesktopDetailScreenModel(get()) }
    factory { PokemonListScreenModel(get(), get()) }
    factory { params -> PokemonDetailScreenModel(params.get(), get(), get()) }
}
