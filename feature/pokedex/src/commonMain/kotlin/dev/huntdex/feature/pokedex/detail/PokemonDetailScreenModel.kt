package dev.huntdex.feature.pokedex.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.PokemonRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PokemonDetailScreenModel(
    private val pokemonId: Int,
    private val repository: PokemonRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope

    private val _state = MutableStateFlow(PokemonDetailState())
    val state: StateFlow<PokemonDetailState> = _state.asStateFlow()

    init {
        loadDetail()
    }

    fun onIntent(intent: PokemonDetailIntent) {
        when (intent) {
            is PokemonDetailIntent.NavigateBack -> navigator.navigateBack()
            is PokemonDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { PokemonDetailState(isLoading = true) }
        scope.launch {
            runCatching { repository.getPokemonDetail(pokemonId) }
                .onSuccess { detail ->
                    _state.update { it.copy(isLoading = false, detail = detail) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
