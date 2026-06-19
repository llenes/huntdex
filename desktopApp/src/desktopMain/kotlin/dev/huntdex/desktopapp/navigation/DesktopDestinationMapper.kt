package dev.huntdex.desktopapp.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination
import dev.huntdex.desktopapp.screens.DesktopHomeScreen
import dev.huntdex.desktopapp.screens.DesktopPlaceholderScreen

fun Destination.toDesktopScreen(): Screen = when (this) {
    is Destination.PokemonList -> DesktopHomeScreen()
    else -> DesktopPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
