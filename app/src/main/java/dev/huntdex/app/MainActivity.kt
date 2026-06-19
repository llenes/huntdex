package dev.huntdex.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.material3.MaterialTheme
import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.app.di.appModule
import dev.huntdex.app.navigation.VoyagerNavigatorAdapter
import dev.huntdex.app.screens.HomeScreen
import org.koin.core.context.loadKoinModules

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Navigator(HomeScreen()) { navigator ->
                    val adapter = VoyagerNavigatorAdapter(navigator)
                    loadKoinModules(appModule(adapter))
                    navigator.lastItem.Content()
                }
            }
        }
    }
}
