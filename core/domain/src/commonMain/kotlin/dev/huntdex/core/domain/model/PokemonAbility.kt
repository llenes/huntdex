package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonAbility(val name: String, val isHidden: Boolean)
