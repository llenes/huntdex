package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationEncounter(
    val locationAreaName: String,  // humanized, e.g. "Pallet Town Area"
    val versions: List<String>     // e.g. ["red", "blue"]
)
