package dev.huntdex.desktopapp.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination
import dev.huntdex.desktopapp.screens.DesktopPlaceholderScreen
import dev.huntdex.feature.pokedex.detail.PokemonDetailScreen
import dev.huntdex.feature.pokedex.list.PokemonListScreen

fun Destination.toDesktopScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    else -> DesktopPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
