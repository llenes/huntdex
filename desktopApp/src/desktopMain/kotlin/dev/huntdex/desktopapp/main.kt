package dev.huntdex.desktopapp

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.core.data.di.dataModule
import dev.huntdex.core.data.di.desktopDataModule
import dev.huntdex.desktopapp.di.desktopAppModule
import dev.huntdex.desktopapp.navigation.DesktopNavigatorAdapter
import dev.huntdex.desktopapp.screens.DesktopHomeScreen
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules

fun main() {
    startKoin {
        modules(dataModule, desktopDataModule)
    }
    application {
        Window(onCloseRequest = ::exitApplication, title = "Huntdex") {
            MaterialTheme {
                Navigator(DesktopHomeScreen()) { navigator ->
                    val adapter = remember { DesktopNavigatorAdapter(navigator) }
                    val module = remember(adapter) {
                        desktopAppModule(adapter).also { loadKoinModules(it) }
                    }
                    DisposableEffect(module) {
                        onDispose { unloadKoinModules(module) }
                    }
                    CurrentScreen()
                }
            }
        }
    }
}
