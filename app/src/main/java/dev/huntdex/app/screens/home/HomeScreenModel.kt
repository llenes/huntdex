package dev.huntdex.app.screens.home

import cafe.adriel.voyager.core.model.ScreenModel
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class HomeState(val count: Int = 0)

sealed interface HomeIntent {
    data object NavigateToDetail : HomeIntent
}

class HomeScreenModel(
    private val navigator: AppNavigator
) : ScreenModel {

    private val _state = MutableStateFlow(HomeState())
    val state: StateFlow<HomeState> = _state.asStateFlow()

    fun onIntent(intent: HomeIntent) {
        when (intent) {
            is HomeIntent.NavigateToDetail ->
                navigator.navigateTo(Destination.PokemonDetail(id = 1))
        }
    }
}
