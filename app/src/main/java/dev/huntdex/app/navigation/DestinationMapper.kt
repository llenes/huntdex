package dev.huntdex.app.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.app.screens.PlaceholderScreen
import dev.huntdex.core.navigation.Destination

fun Destination.toScreen(): Screen = PlaceholderScreen(this::class.simpleName ?: "Unknown")
