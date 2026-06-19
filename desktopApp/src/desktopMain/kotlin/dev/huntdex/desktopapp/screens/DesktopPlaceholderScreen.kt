package dev.huntdex.desktopapp.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.core.screen.ScreenKey
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.navigation.AppNavigator

data class DesktopPlaceholderScreen(val label: String) : Screen {
    override val key: ScreenKey = label

    @Composable
    override fun Content() {
        val screenModel = koinScreenModel<DesktopDetailScreenModel>()

        Column(
            modifier = Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(label, style = MaterialTheme.typography.headlineMedium)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { screenModel.goBack() }) {
                Text("Volver")
            }
        }
    }
}

class DesktopDetailScreenModel(
    private val navigator: AppNavigator
) : ScreenModel {
    fun goBack() = navigator.navigateBack()
}
