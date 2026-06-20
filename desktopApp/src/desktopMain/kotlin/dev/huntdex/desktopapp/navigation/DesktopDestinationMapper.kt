package dev.huntdex.desktopapp.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination
import dev.huntdex.desktopapp.screens.DesktopPlaceholderScreen
import dev.huntdex.feature.moves.detail.MoveDetailScreen
import dev.huntdex.feature.moves.list.MoveListScreen
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreen
import dev.huntdex.feature.pokedex.list.PokemonListScreen

fun Destination.toDesktopScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    is Destination.MoveList -> MoveListScreen
    is Destination.MoveDetail -> MoveDetailScreen(id)
    else -> DesktopPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
