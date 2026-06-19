package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class PokemonDetail(
    val id: Int,
    val name: String,
    val height: Int,        // decimeters (divide by 10 for meters)
    val weight: Int,        // hectograms (divide by 10 for kg)
    val spriteUrl: String,
    val types: List<PokemonType>,
    val stats: List<PokemonStat>,
    val abilities: List<PokemonAbility>,
    val flavorText: String,
    val generation: String,             // e.g. "generation-i"
    val evolutionSteps: List<EvolutionStep>,
    val locationEncounters: List<LocationEncounter>
)
