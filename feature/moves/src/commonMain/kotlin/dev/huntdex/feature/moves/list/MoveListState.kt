package dev.huntdex.feature.moves.list

import dev.huntdex.core.domain.model.MoveEntry

data class MoveListState(
    val moves: List<MoveEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedType: String? = null,
    val selectedDamageClass: String? = null,
    val selectedGeneration: Int? = null,
    val currentOffset: Int = 0,
    val hasMore: Boolean = true
) {
    val displayedMoves: List<MoveEntry>
        get() = if (searchQuery.isBlank()) moves
                else moves.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
