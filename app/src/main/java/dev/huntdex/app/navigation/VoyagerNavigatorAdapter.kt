package dev.huntdex.app.navigation

import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.map

class VoyagerNavigatorAdapter(
    private val navigator: Navigator
) : AppNavigator {

    private val results = MutableSharedFlow<Pair<String, Any?>>(extraBufferCapacity = 1)

    override fun navigateTo(destination: Destination) {
        navigator.push(destination.toScreen())
    }

    override fun navigateBack() {
        navigator.pop()
    }

    override fun popTo(destination: Destination, inclusive: Boolean) {
        val targetScreen = destination.toScreen()
        navigator.popUntil { screen ->
            if (inclusive) screen::class == targetScreen::class
            else screen::class == targetScreen::class
        }
    }

    override fun <T> setResult(key: String, value: T) {
        results.tryEmit(key to value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getResult(key: String): Flow<T?> =
        results
            .filterIsInstance<Pair<String, Any?>>()
            .map { (k, v) -> if (k == key) v as? T else null }
}
