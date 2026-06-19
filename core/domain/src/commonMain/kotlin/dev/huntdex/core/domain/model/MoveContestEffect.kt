package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveContestEffect(
    val contestType: String,
    val appeal: Int,
    val jam: Int,
    val effectEntry: String
)
