package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class PokemonDetailDto(
    val id: Int,
    val name: String,
    val height: Int,
    val weight: Int,
    val sprites: SpritesDto,
    val types: List<PokemonTypeSlotDto>,
    val stats: List<PokemonStatSlotDto>,
    val abilities: List<PokemonAbilitySlotDto>,
    val species: NamedApiResourceDto
)

@Serializable
data class SpritesDto(@SerialName("front_default") val frontDefault: String?)

@Serializable
data class PokemonTypeSlotDto(val slot: Int, val type: NamedApiResourceDto)

@Serializable
data class PokemonStatSlotDto(
    @SerialName("base_stat") val baseStat: Int,
    val stat: NamedApiResourceDto
)

@Serializable
data class PokemonAbilitySlotDto(
    val ability: NamedApiResourceDto,
    @SerialName("is_hidden") val isHidden: Boolean
)
