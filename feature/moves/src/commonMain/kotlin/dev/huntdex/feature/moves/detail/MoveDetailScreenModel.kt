package dev.huntdex.feature.moves.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoveDetailScreenModel(
    private val moveId: Int,
    private val repository: MoveRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(MoveDetailState())
    val state: StateFlow<MoveDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onIntent(intent: MoveDetailIntent) {
        when (intent) {
            is MoveDetailIntent.NavigateBack -> navigator.navigateBack()
            is MoveDetailIntent.ExpandLearnedBy -> _state.update { it.copy(isLearnedByExpanded = true) }
            is MoveDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getMoveDetail(moveId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
