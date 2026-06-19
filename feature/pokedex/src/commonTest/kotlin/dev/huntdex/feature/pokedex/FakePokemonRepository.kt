package dev.huntdex.feature.pokedex

import dev.huntdex.core.domain.model.PokemonAbility
import dev.huntdex.core.domain.model.PokemonDetail
import dev.huntdex.core.domain.model.PokemonEntry
import dev.huntdex.core.domain.model.PokemonStat
import dev.huntdex.core.domain.model.PokemonType
import dev.huntdex.core.domain.repository.PokemonRepository

class FakePokemonRepository : PokemonRepository {
    val entries = (1..30).map { id ->
        PokemonEntry(id, "pokemon-$id", "https://example.com/$id.png")
    }

    override suspend fun getPokemonPage(limit: Int, offset: Int): List<PokemonEntry> =
        entries.drop(offset).take(limit)

    override suspend fun getPokemonByGeneration(generationId: Int): List<PokemonEntry> =
        entries.take(3)

    override suspend fun getPokemonDetail(id: Int): PokemonDetail = PokemonDetail(
        id = id, name = "pokemon-$id", height = 7, weight = 69,
        spriteUrl = "https://example.com/$id.png",
        types = listOf(PokemonType("grass")),
        stats = listOf(PokemonStat("hp", 45)),
        abilities = listOf(PokemonAbility("overgrow", false)),
        flavorText = "A test Pokémon.",
        generation = "generation-i",
        evolutionSteps = emptyList(),
        locationEncounters = emptyList()
    )
}
