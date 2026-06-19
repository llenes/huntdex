package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonSpeciesDto(
    val id: Int,
    val generation: NamedApiResourceDto,
    @SerialName("evolution_chain") val evolutionChain: ApiResourceDto,
    @SerialName("flavor_text_entries") val flavorTextEntries: List<FlavorTextEntryDto>
)

@Serializable
data class FlavorTextEntryDto(
    @SerialName("flavor_text") val flavorText: String,
    val language: NamedApiResourceDto,
    val version: NamedApiResourceDto
)
