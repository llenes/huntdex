package dev.huntdex.feature.pokedex.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

object PokedexTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = "Pokédex",
            icon = rememberVectorPainter(Icons.Filled.Favorite)
        )

    @Composable
    override fun Content() {
        PokemonListScreen.Content()
    }
}
