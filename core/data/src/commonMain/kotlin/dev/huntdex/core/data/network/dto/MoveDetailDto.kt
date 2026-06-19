package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoveDetailDto(
    val id: Int,
    val name: String,
    val accuracy: Int?,
    val power: Int?,
    val pp: Int,
    val priority: Int,
    val type: NamedApiResourceDto,
    @SerialName("damage_class") val damageClass: NamedApiResourceDto,
    @SerialName("effect_entries") val effectEntries: List<MoveEffectEntryDto>,
    @SerialName("flavor_text_entries") val flavorTextEntries: List<MoveFlavorTextEntryDto>,
    @SerialName("learned_by_pokemon") val learnedByPokemon: List<NamedApiResourceDto>,
    @SerialName("contest_type") val contestType: NamedApiResourceDto?,
    @SerialName("contest_effect") val contestEffect: ApiResourceDto?
)

@Serializable
data class MoveEffectEntryDto(
    val effect: String,
    val language: NamedApiResourceDto
)

@Serializable
data class MoveFlavorTextEntryDto(
    @SerialName("flavor_text") val flavorText: String,
    val language: NamedApiResourceDto,
    @SerialName("version_group") val versionGroup: NamedApiResourceDto
)
