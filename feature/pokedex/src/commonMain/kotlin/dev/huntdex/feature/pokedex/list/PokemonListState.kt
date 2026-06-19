package dev.huntdex.feature.pokedex.list

import dev.huntdex.core.domain.model.PokemonEntry

data class PokemonListState(
    val pokemon: List<PokemonEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedGeneration: Int? = null,   // null = All
    val currentOffset: Int = 0,
    val hasMore: Boolean = true
) {
    val displayedPokemon: List<PokemonEntry>
        get() = if (searchQuery.isBlank()) pokemon
                else pokemon.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
