package dev.huntdex.shared.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination
import dev.huntdex.shared.screens.IosHomeScreen
import dev.huntdex.shared.screens.IosPlaceholderScreen

fun Destination.toIosScreen(): Screen = when (this) {
    is Destination.PokemonList -> IosHomeScreen()
    else -> IosPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
