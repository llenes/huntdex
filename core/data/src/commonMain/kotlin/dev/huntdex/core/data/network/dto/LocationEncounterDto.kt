package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationEncounterDto(
    @SerialName("location_area") val locationArea: NamedApiResourceDto,
    @SerialName("version_details") val versionDetails: List<VersionDetailDto>
)

@Serializable
data class VersionDetailDto(val version: NamedApiResourceDto)
