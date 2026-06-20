package dev.huntdex.app.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.app.screens.home.HomeIntent
import dev.huntdex.app.screens.home.HomeScreenModel

class HomeScreen : Screen, java.io.Serializable {
    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<HomeScreenModel>()

        Column(
            modifier = Modifier.fillMaxSize().padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Huntdex", style = MaterialTheme.typography.headlineLarge)
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { screenModel.onIntent(HomeIntent.NavigateToPokemonList) }) {
                Text("Abrir Pokédex")
            }
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { screenModel.onIntent(HomeIntent.NavigateToMoveList) }) {
                Text("Abrir Movimientos")
            }
        }
    }
}
