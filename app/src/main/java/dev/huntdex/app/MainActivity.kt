package dev.huntdex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.app.di.appModule
import dev.huntdex.app.navigation.VoyagerNavigatorAdapter
import dev.huntdex.app.screens.HomeScreen
import org.koin.core.context.loadKoinModules
import org.koin.core.context.unloadKoinModules

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Navigator(HomeScreen()) { navigator ->
                    val adapter = remember { VoyagerNavigatorAdapter(navigator) }
                    DisposableEffect(Unit) {
                        val module = appModule(adapter)
                        loadKoinModules(module)
                        onDispose { unloadKoinModules(module) }
                    }
                    navigator.lastItem.Content()
                }
            }
        }
    }
}
