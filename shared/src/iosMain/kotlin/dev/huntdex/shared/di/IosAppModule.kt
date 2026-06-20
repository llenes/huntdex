package dev.huntdex.shared.di

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.feature.moves.detail.MoveDetailScreenModel
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreenModel
import dev.huntdex.feature.pokedex.list.PokemonListScreenModel
import dev.huntdex.shared.navigation.IosNavigatorAdapter
import dev.huntdex.shared.screens.IosDetailScreenModel
import dev.huntdex.shared.screens.IosHomeScreenModel
import org.koin.dsl.module

fun iosAppModule(adapter: IosNavigatorAdapter) = module {
    single<AppNavigator> { adapter }
    factory { IosHomeScreenModel(get()) }
    factory { IosDetailScreenModel(get()) }
    factory { PokemonListScreenModel(get(), get()) }
    factory { params -> PokemonDetailScreenModel(params.get(), get(), get()) }
    factory { MoveListScreenModel(get(), get()) }
    factory { params -> MoveDetailScreenModel(params.get(), get(), get()) }
}
