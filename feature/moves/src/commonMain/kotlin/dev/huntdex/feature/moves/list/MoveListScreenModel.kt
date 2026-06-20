package dev.huntdex.feature.moves.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class MoveListScreenModel(
    private val repository: MoveRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(MoveListState())
    val state: StateFlow<MoveListState> = _state.asStateFlow()

    init { loadFirstPage() }

    fun onIntent(intent: MoveListIntent) {
        when (intent) {
            is MoveListIntent.LoadNextPage -> loadNextPage()
            is MoveListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is MoveListIntent.FilterByType -> applyTypeFilter(intent.typeName)
            is MoveListIntent.FilterByDamageClass -> applyDamageClassFilter(intent.className)
            is MoveListIntent.FilterByGeneration -> _state.update { it.copy(selectedGeneration = intent.generationId) }
            is MoveListIntent.SelectMove -> navigator.navigateTo(Destination.MoveDetail(intent.id))
            is MoveListIntent.Retry -> when {
                _state.value.selectedType != null -> applyTypeFilter(_state.value.selectedType)
                _state.value.selectedDamageClass != null -> applyDamageClassFilter(_state.value.selectedDamageClass)
                else -> loadFirstPage()
            }
        }
    }

    private fun loadFirstPage() {
        val query = _state.value.searchQuery
        _state.update { MoveListState(isLoading = true, searchQuery = query) }
        scope.launch {
            runCatching { repository.getMovePage(PAGE_SIZE, 0) }
                .onSuccess { entries ->
                    _state.update {
                        it.copy(
                            moves = entries,
                            isLoading = false,
                            currentOffset = entries.size,
                            hasMore = entries.size == PAGE_SIZE
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.selectedType != null || current.selectedDamageClass != null) return
        _state.update { it.copy(isLoadingMore = true) }
        scope.launch {
            runCatching { repository.getMovePage(PAGE_SIZE, current.currentOffset) }
                .onSuccess { entries ->
                    _state.update {
                        it.copy(
                            moves = it.moves + entries,
                            isLoadingMore = false,
                            currentOffset = it.currentOffset + entries.size,
                            hasMore = entries.size == PAGE_SIZE
                        )
                    }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.message) } }
        }
    }

    private fun applyTypeFilter(typeName: String?) {
        _state.update { it.copy(selectedType = typeName, selectedDamageClass = null, isLoading = true, error = null, moves = emptyList()) }
        if (typeName == null) { loadFirstPage(); return }
        scope.launch {
            runCatching { repository.getMovesByType(typeName) }
                .onSuccess { entries -> _state.update { it.copy(moves = entries, isLoading = false, hasMore = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun applyDamageClassFilter(className: String?) {
        _state.update { it.copy(selectedDamageClass = className, selectedType = null, isLoading = true, error = null, moves = emptyList()) }
        if (className == null) { loadFirstPage(); return }
        scope.launch {
            runCatching { repository.getMovesByDamageClass(className) }
                .onSuccess { entries -> _state.update { it.copy(moves = entries, isLoading = false, hasMore = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
