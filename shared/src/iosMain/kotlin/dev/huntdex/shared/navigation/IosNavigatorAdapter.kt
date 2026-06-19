package dev.huntdex.shared.navigation

import cafe.adriel.voyager.navigator.Navigator
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map

class IosNavigatorAdapter(
    private val navigator: Navigator
) : AppNavigator {

    private val results = MutableSharedFlow<Pair<String, Any?>>(extraBufferCapacity = 1)

    override fun navigateTo(destination: Destination) {
        navigator.push(destination.toIosScreen())
    }

    override fun navigateBack() {
        navigator.pop()
    }

    override fun popTo(destination: Destination, inclusive: Boolean) {
        val target = destination.toIosScreen()
        navigator.popUntil { screen -> screen::class == target::class }
        // TODO: inclusive=true should pop one additional screen
    }

    override fun <T> setResult(key: String, value: T) {
        results.tryEmit(key to value)
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T> getResult(key: String): Flow<T?> =
        results
            .filter { (k, _) -> k == key }
            .map { (_, v) -> v as? T }
}
