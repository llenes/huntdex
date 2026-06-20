package dev.huntdex.feature.pokedex.list

sealed interface PokemonListIntent {
    data object LoadNextPage : PokemonListIntent
    data class Search(val query: String) : PokemonListIntent
    data class FilterByGeneration(val generationId: Int?) : PokemonListIntent
    data class SelectPokemon(val id: Int) : PokemonListIntent
    data object Retry : PokemonListIntent
    data object NavigateBack : PokemonListIntent
}
