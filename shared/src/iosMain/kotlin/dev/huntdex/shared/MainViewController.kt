package dev.huntdex.shared

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.window.ComposeUIViewController
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.core.data.di.dataModule
import dev.huntdex.core.data.di.iosDataModule
import dev.huntdex.shared.di.iosAppModule
import dev.huntdex.shared.navigation.IosNavigatorAdapter
import dev.huntdex.shared.screens.IosHomeScreen
import org.koin.core.context.loadKoinModules
import org.koin.core.context.startKoin
import org.koin.core.context.unloadKoinModules
import platform.UIKit.UIViewController

fun MainViewController(): UIViewController {
    startKoin {
        modules(dataModule, iosDataModule)
    }
    return ComposeUIViewController(configure = { enforceStrictPlistSanityCheck = false }) {
        MaterialTheme {
            Navigator(IosHomeScreen()) { navigator ->
                val adapter = remember { IosNavigatorAdapter(navigator) }
                val module = remember(adapter) {
                    iosAppModule(adapter).also { loadKoinModules(it) }
                }
                DisposableEffect(module) {
                    onDispose { unloadKoinModules(module) }
                }
                CurrentScreen()
            }
        }
    }
}
