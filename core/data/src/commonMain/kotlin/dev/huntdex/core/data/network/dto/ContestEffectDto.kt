package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContestEffectDto(
    val id: Int,
    val appeal: Int,
    val jam: Int,
    @SerialName("effect_entries") val effectEntries: List<ContestEffectEntryDto>
)

@Serializable
data class ContestEffectEntryDto(val effect: String, val language: NamedApiResourceDto)
