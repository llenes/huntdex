package dev.huntdex.app.di

import dev.huntdex.app.navigation.VoyagerNavigatorAdapter
import dev.huntdex.app.screens.detail.DetailScreenModel
import dev.huntdex.app.screens.home.HomeScreenModel
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.feature.moves.detail.MoveDetailScreenModel
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreenModel
import dev.huntdex.feature.pokedex.list.PokemonListScreenModel
import org.koin.dsl.module

fun appModule(navigatorAdapter: VoyagerNavigatorAdapter) = module {
    single<AppNavigator> { navigatorAdapter }
    factory { HomeScreenModel(get()) }
    factory { DetailScreenModel(get()) }
    factory { PokemonListScreenModel(get(), get()) }
    factory { params -> PokemonDetailScreenModel(params.get(), get(), get()) }
    factory { MoveListScreenModel(get(), get()) }
    factory { params -> MoveDetailScreenModel(params.get(), get(), get()) }
}
