package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonStat(val name: String, val baseStat: Int)
