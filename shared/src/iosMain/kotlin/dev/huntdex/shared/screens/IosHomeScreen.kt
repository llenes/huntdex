package dev.huntdex.shared.screens

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
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination

class IosHomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<IosHomeScreenModel>()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Huntdex iOS", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(24.dp))
            Button(onClick = { screenModel.navigateToPokemonList() }) {
                Text("Abrir Pokédex")
            }
        }
    }
}

class IosHomeScreenModel(private val navigator: AppNavigator) : ScreenModel {
    fun navigateToPokemonList() = navigator.navigateTo(Destination.PokemonList)
}
