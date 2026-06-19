package dev.huntdex.feature.pokedex.detail

import dev.huntdex.core.domain.model.PokemonDetail

data class PokemonDetailState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val detail: PokemonDetail? = null
)
