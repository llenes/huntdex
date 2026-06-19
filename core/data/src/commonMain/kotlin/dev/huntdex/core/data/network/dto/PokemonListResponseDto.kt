package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class PokemonListResponseDto(
    val count: Int,
    val results: List<PokemonResultDto>
)

@Serializable
data class PokemonResultDto(val name: String, val url: String)
