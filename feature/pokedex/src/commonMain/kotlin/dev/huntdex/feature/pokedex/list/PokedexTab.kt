package dev.huntdex.feature.pokedex.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions
import huntdex.feature.pokedex.generated.resources.Res
import huntdex.feature.pokedex.generated.resources.pokedex_tab_title
import org.jetbrains.compose.resources.stringResource

object PokedexTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 0u,
            title = stringResource(Res.string.pokedex_tab_title),
            icon = rememberVectorPainter(Icons.Filled.Favorite)
        )

    @Composable
    override fun Content() {
        PokemonListScreen.Content()
    }
}
