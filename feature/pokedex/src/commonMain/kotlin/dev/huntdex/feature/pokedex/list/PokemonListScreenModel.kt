package dev.huntdex.feature.pokedex.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.PokemonRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class PokemonListScreenModel(
    private val repository: PokemonRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope

    private val _state = MutableStateFlow(PokemonListState())
    val state: StateFlow<PokemonListState> = _state.asStateFlow()

    init {
        loadFirstPage()
    }

    fun onIntent(intent: PokemonListIntent) {
        when (intent) {
            is PokemonListIntent.LoadNextPage -> loadNextPage()
            is PokemonListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is PokemonListIntent.FilterByGeneration -> applyGenerationFilter(intent.generationId)
            is PokemonListIntent.SelectPokemon -> navigator.navigateTo(Destination.PokemonDetail(intent.id))
            is PokemonListIntent.Retry -> loadFirstPage()
        }
    }

    private fun loadFirstPage() {
        _state.update { PokemonListState(isLoading = true) }
        scope.launch {
            runCatching { repository.getPokemonPage(PAGE_SIZE, 0) }
                .onSuccess { entries ->
                    _state.update {
                        it.copy(
                            pokemon = entries,
                            isLoading = false,
                            currentOffset = entries.size,
                            hasMore = entries.size == PAGE_SIZE
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.selectedGeneration != null) return
        _state.update { it.copy(isLoadingMore = true) }
        scope.launch {
            runCatching { repository.getPokemonPage(PAGE_SIZE, current.currentOffset) }
                .onSuccess { entries ->
                    _state.update {
                        it.copy(
                            pokemon = it.pokemon + entries,
                            isLoadingMore = false,
                            currentOffset = it.currentOffset + entries.size,
                            hasMore = entries.size == PAGE_SIZE
                        )
                    }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoadingMore = false, error = e.message) }
                }
        }
    }

    private fun applyGenerationFilter(generationId: Int?) {
        _state.update { it.copy(selectedGeneration = generationId, isLoading = true, pokemon = emptyList()) }
        if (generationId == null) { loadFirstPage(); return }
        scope.launch {
            runCatching { repository.getPokemonByGeneration(generationId) }
                .onSuccess { entries ->
                    _state.update { it.copy(pokemon = entries, isLoading = false, hasMore = false) }
                }
                .onFailure { e ->
                    _state.update { it.copy(isLoading = false, error = e.message) }
                }
        }
    }
}
