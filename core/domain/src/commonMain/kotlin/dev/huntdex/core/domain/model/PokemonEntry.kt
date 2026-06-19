package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonEntry(
    val id: Int,
    val name: String,
    val spriteUrl: String
)
