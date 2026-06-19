package dev.huntdex.core.navigation

import kotlinx.coroutines.flow.Flow

interface AppNavigator {
    fun navigateTo(destination: Destination)
    fun navigateBack()
    fun popTo(destination: Destination, inclusive: Boolean = false)
    fun <T> setResult(key: String, value: T)
    fun <T> getResult(key: String): Flow<T?>
}
