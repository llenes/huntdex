package dev.huntdex.feature.moves.detail

import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveDetail

data class MoveDetailState(
    val detail: MoveDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLearnedByExpanded: Boolean = false
) {
    val learnedByVisible: List<LearnedByPokemon>
        get() = if (isLearnedByExpanded) detail?.learnedBy ?: emptyList()
                else (detail?.learnedBy ?: emptyList()).take(10)
    val hasMoreLearnedBy: Boolean
        get() = (detail?.learnedBy?.size ?: 0) > 10
}
