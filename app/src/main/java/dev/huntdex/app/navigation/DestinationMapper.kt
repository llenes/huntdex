package dev.huntdex.app.navigation

import cafe.adriel.voyager.core.screen.Screen
import dev.huntdex.core.navigation.Destination

fun Destination.toScreen(): Screen = PlaceholderScreen(this::class.simpleName ?: "Unknown")

// Temporary stub — will be replaced in Task 4 with real screen implementations
data class PlaceholderScreen(val label: String) : Screen {
    @androidx.compose.runtime.Composable
    override fun Content() {
        // Stub: real content comes in Task 4
    }
}
