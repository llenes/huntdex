package dev.huntdex.shared.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.moves.detail.MoveDetailScreen
import dev.huntdex.feature.moves.list.MoveListScreen
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreen
import dev.huntdex.feature.pokedex.list.PokemonListScreen
import dev.huntdex.shared.screens.IosPlaceholderScreen

fun Destination.toIosScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    is Destination.MoveList -> MoveListScreen
    is Destination.MoveDetail -> MoveDetailScreen(id)
    else -> IosPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
