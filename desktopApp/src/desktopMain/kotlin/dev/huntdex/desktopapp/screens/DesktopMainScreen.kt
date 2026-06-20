package dev.huntdex.desktopapp.screens

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuOpen
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
        var expanded by remember { mutableStateOf(true) }
        TabNavigator(tab = PokedexTab) {
            Row(modifier = Modifier.fillMaxSize()) {
                DesktopNavigationRail(expanded = expanded, onToggle = { expanded = !expanded })
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(start = 32.dp, top = 16.dp, end = 32.dp, bottom = 16.dp)
                ) {
                    CurrentTab()
                }
            }
        }
    }
}

@Composable
private fun DesktopNavigationRail(expanded: Boolean, onToggle: () -> Unit) {
    val railWidth by animateDpAsState(
        targetValue = if (expanded) 160.dp else 80.dp,
        label = "railWidth"
    )
    val tabNavigator = LocalTabNavigator.current
    NavigationRail(
        modifier = Modifier.width(railWidth),
        header = {
            IconButton(onClick = onToggle) {
                Icon(
                    imageVector = if (expanded) Icons.AutoMirrored.Filled.MenuOpen else Icons.Default.Menu,
                    contentDescription = if (expanded) "Collapse navigation" else "Expand navigation"
                )
            }
        }
    ) {
        Spacer(Modifier.height(8.dp))
        listOf<Tab>(PokedexTab, MovesTab).forEach { tab ->
            val options = tab.options
            NavigationRailItem(
                selected = tabNavigator.current.key == tab.key,
                onClick = { tabNavigator.current = tab },
                icon = {
                    options.icon?.let {
                        Icon(painter = it, contentDescription = options.title)
                    }
                },
                label = if (expanded) ({ Text(options.title) }) else null
            )
        }
    }
}
