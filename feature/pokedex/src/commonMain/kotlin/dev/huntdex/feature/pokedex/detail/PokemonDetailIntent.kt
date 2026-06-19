package dev.huntdex.feature.pokedex.detail

sealed interface PokemonDetailIntent {
    data object NavigateBack : PokemonDetailIntent
    data object Retry : PokemonDetailIntent
}
