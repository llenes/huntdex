package dev.huntdex.feature.moves.detail

sealed interface MoveDetailIntent {
    data object NavigateBack : MoveDetailIntent
    data object ExpandLearnedBy : MoveDetailIntent
    data object Retry : MoveDetailIntent
}
