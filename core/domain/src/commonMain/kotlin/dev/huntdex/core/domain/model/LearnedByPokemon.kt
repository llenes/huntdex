package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LearnedByPokemon(val id: Int, val name: String, val learnMethods: List<String>)
