package dev.huntdex.desktopapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination

class DesktopHomeScreen : Screen {

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<DesktopHomeScreenModel>()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Huntdex Desktop", style = MaterialTheme.typography.headlineLarge)
            Spacer(Modifier.height(16.dp))
            Text("Phase 0 — Desktop scaffold")
            Spacer(Modifier.height(24.dp))
            Button(onClick = { screenModel.navigateToDetail() }) {
                Text("Ver detalle de prueba")
            }
        }
    }
}

class DesktopHomeScreenModel(
    private val navigator: AppNavigator
) : ScreenModel {
    fun navigateToDetail() {
        navigator.navigateTo(Destination.PokemonDetail(id = 1))
    }
}
