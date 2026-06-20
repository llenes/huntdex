package dev.huntdex.desktopapp.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.tab.CurrentTab
import cafe.adriel.voyager.navigator.tab.LocalTabNavigator
import cafe.adriel.voyager.navigator.tab.Tab
import cafe.adriel.voyager.navigator.tab.TabNavigator
import dev.huntdex.feature.moves.list.MovesTab
import dev.huntdex.feature.pokedex.list.PokedexTab

data object DesktopMainScreen : Screen {
    @Composable
    override fun Content() {
        TabNavigator(tab = PokedexTab) {
            Scaffold(
                bottomBar = { DesktopFloatingBottomNavBar() }
            ) { padding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(padding)
                ) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun DesktopFloatingBottomNavBar() {
    val tabNavigator = LocalTabNavigator.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 8.dp,
            shadowElevation = 12.dp
        ) {
            NavigationBar(
                windowInsets = WindowInsets(0, 0, 0, 0),
                tonalElevation = 0.dp
            ) {
                listOf<Tab>(PokedexTab, MovesTab).forEach { tab ->
                    val options = tab.options
                    NavigationBarItem(
                        selected = tabNavigator.current.key == tab.key,
                        onClick = { tabNavigator.current = tab },
                        icon = {
                            options.icon?.let {
                                Icon(painter = it, contentDescription = options.title)
                            }
                        },
                        label = { Text(options.title) }
                    )
                }
            }
        }
    }
}
