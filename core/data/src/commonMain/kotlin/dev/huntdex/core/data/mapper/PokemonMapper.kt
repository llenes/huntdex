package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.ChainLinkDto
import dev.huntdex.core.data.network.dto.EvolutionChainDto
import dev.huntdex.core.data.network.dto.LocationEncounterDto
import dev.huntdex.core.data.network.dto.PokemonDetailDto
import dev.huntdex.core.data.network.dto.PokemonSpeciesDto
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.EvolutionStep
import dev.huntdex.core.domain.model.LocationEncounter
import dev.huntdex.core.domain.model.PokemonAbility
import dev.huntdex.core.domain.model.PokemonDetail
import dev.huntdex.core.domain.model.PokemonEntry
import dev.huntdex.core.domain.model.PokemonStat
import dev.huntdex.core.domain.model.PokemonType

fun spriteUrl(id: Int): String =
    "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/$id.png"

fun toPokemonEntry(id: Int, name: String): PokemonEntry =
    PokemonEntry(id = id, name = name, spriteUrl = spriteUrl(id))

fun toPokemonDetail(
    detail: PokemonDetailDto,
    species: PokemonSpeciesDto,
    chain: EvolutionChainDto,
    encounters: List<LocationEncounterDto>
): PokemonDetail {
    val flavorText = species.flavorTextEntries
        .firstOrNull { it.language.name == "en" }
        ?.flavorText
        ?.replace("\n", " ")
        ?.replace("\u000C", " ")
        ?: ""

    return PokemonDetail(
        id = detail.id,
        name = detail.name,
        height = detail.height,
        weight = detail.weight,
        spriteUrl = detail.sprites.frontDefault ?: spriteUrl(detail.id),
        types = detail.types
            .sortedBy { it.slot }
            .map { PokemonType(it.type.name) },
        stats = detail.stats.map { PokemonStat(it.stat.name, it.baseStat) },
        abilities = detail.abilities.map { PokemonAbility(it.ability.name, it.isHidden) },
        flavorText = flavorText,
        generation = species.generation.name,
        evolutionSteps = flattenEvolutionChain(chain.chain),
        locationEncounters = encounters.map { enc ->
            LocationEncounter(
                locationAreaName = humanizeName(enc.locationArea.name),
                versions = enc.versionDetails.map { it.version.name }
            )
        }
    )
}

private fun flattenEvolutionChain(link: ChainLinkDto): List<EvolutionStep> {
    val steps = mutableListOf<EvolutionStep>()
    for (evolution in link.evolvesTo) {
        val minLevel = evolution.evolutionDetails.firstOrNull()?.minLevel
        steps += EvolutionStep(
            fromId = link.species.url.extractPokeApiId(),
            fromName = link.species.name,
            toId = evolution.species.url.extractPokeApiId(),
            toName = evolution.species.name,
            minLevel = minLevel
        )
        steps += flattenEvolutionChain(evolution)
    }
    return steps
}

private fun humanizeName(name: String): String =
    name.split("-").joinToString(" ") { it.replaceFirstChar { c -> c.uppercase() } }
