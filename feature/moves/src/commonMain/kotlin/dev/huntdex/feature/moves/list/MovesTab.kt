package dev.huntdex.feature.moves.list

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabOptions

object MovesTab : Tab {
    override val options: TabOptions
        @Composable get() = TabOptions(
            index = 1u,
            title = "Movimientos",
            icon = rememberVectorPainter(Icons.Filled.Star)
        )

    @Composable
    override fun Content() {
        MoveListScreen.Content()
    }
}
