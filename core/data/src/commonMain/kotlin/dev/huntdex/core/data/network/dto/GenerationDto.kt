package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerationDto(
    val id: Int,
    val name: String,
    @SerialName("pokemon_species") val pokemonSpecies: List<NamedApiResourceDto>
)
