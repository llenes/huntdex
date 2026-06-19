package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class EvolutionChainDto(
    val id: Int,
    val chain: ChainLinkDto
)

@Serializable
data class ChainLinkDto(
    val species: NamedApiResourceDto,
    @SerialName("evolution_details") val evolutionDetails: List<EvolutionDetailDto>,
    @SerialName("evolves_to") val evolvesTo: List<ChainLinkDto>
)

@Serializable
data class EvolutionDetailDto(
    @SerialName("min_level") val minLevel: Int?
)
