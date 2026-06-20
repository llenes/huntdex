package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveDetail(
    val id: Int,
    val name: String,
    val type: String,
    val damageClass: String,
    val power: Int?,
    val accuracy: Int?,
    val pp: Int,
    val priority: Int,
    val effectEntry: String,
    val flavorText: String,
    val learnedBy: List<LearnedByPokemon>,
    val contestEffect: MoveContestEffect?
)
