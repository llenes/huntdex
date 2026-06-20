package dev.huntdex.feature.moves.list

sealed interface MoveListIntent {
    data object LoadNextPage : MoveListIntent
    data class Search(val query: String) : MoveListIntent
    data class FilterByType(val typeName: String?) : MoveListIntent
    data class FilterByDamageClass(val className: String?) : MoveListIntent
    data class FilterByGeneration(val generationId: Int?) : MoveListIntent
    data class SelectMove(val id: Int) : MoveListIntent
    data object Retry : MoveListIntent
    data object NavigateBack : MoveListIntent
}
