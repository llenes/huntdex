package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class EvolutionStep(
    val fromId: Int,
    val fromName: String,
    val toId: Int,
    val toName: String,
    val minLevel: Int?   // null = no level requirement (e.g., stone evolution)
)
