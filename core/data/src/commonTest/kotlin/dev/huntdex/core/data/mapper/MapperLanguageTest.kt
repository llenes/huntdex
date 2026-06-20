package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.ApiResourceDto
import dev.huntdex.core.data.network.dto.ChainLinkDto
import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.ContestEffectEntryDto
import dev.huntdex.core.data.network.dto.EvolutionChainDto
import dev.huntdex.core.data.network.dto.FlavorTextEntryDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.MoveEffectEntryDto
import dev.huntdex.core.data.network.dto.NamedApiResourceDto
import dev.huntdex.core.data.network.dto.PokemonAbilitySlotDto
import dev.huntdex.core.data.network.dto.PokemonDetailDto
import dev.huntdex.core.data.network.dto.PokemonSpeciesDto
import dev.huntdex.core.data.network.dto.PokemonStatSlotDto
import dev.huntdex.core.data.network.dto.PokemonTypeSlotDto
import dev.huntdex.core.data.network.dto.SpritesDto
import kotlin.test.Test
import kotlin.test.assertEquals

class MapperLanguageTest {

    // ── PokemonMapper ──────────────────────────────────────────────────────

    private val minimalDetail = PokemonDetailDto(
        id = 1, name = "bulbasaur", height = 7, weight = 69,
        sprites = SpritesDto(frontDefault = null),
        types = emptyList(), stats = emptyList(), abilities = emptyList(),
        species = NamedApiResourceDto("bulbasaur", "https://pokeapi.co/api/v2/pokemon-species/1/")
    )

    private val minimalChain = EvolutionChainDto(
        id = 1,
        chain = ChainLinkDto(
            species = NamedApiResourceDto("bulbasaur", "https://pokeapi.co/api/v2/pokemon-species/1/"),
            evolutionDetails = emptyList(),
            evolvesTo = emptyList()
        )
    )

    private fun speciesWith(vararg entries: Pair<String, String>): PokemonSpeciesDto =
        PokemonSpeciesDto(
            id = 1,
            generation = NamedApiResourceDto("generation-i", "https://pokeapi.co/api/v2/generation/1/"),
            evolutionChain = ApiResourceDto("https://pokeapi.co/api/v2/evolution-chain/1/"),
            flavorTextEntries = entries.map { (lang, text) ->
                FlavorTextEntryDto(text, NamedApiResourceDto(lang, ""), NamedApiResourceDto("red", ""))
            }
        )

    @Test
    fun `toPokemonDetail uses requested language when available`() {
        val species = speciesWith("en" to "English text.", "es" to "Texto en español.")
        val result = toPokemonDetail(minimalDetail, species, minimalChain, emptyList(), "es")
        assertEquals("Texto en español.", result.flavorText)
    }

    @Test
    fun `toPokemonDetail falls back to en when language not available`() {
        val species = speciesWith("en" to "English text.")
        val result = toPokemonDetail(minimalDetail, species, minimalChain, emptyList(), "es")
        assertEquals("English text.", result.flavorText)
    }

    @Test
    fun `toPokemonDetail returns empty string when no entries`() {
        val species = speciesWith()
        val result = toPokemonDetail(minimalDetail, species, minimalChain, emptyList(), "es")
        assertEquals("", result.flavorText)
    }

    // ── MoveMapper ─────────────────────────────────────────────────────────

    @Test
    fun `toMoveDetail uses requested language for effectEntry`() {
        val dto = MoveDetailDto(
            id = 1, name = "tackle", accuracy = 100, power = 40, pp = 35, priority = 0,
            type = NamedApiResourceDto("normal", ""),
            damageClass = NamedApiResourceDto("physical", ""),
            effectEntries = listOf(
                MoveEffectEntryDto("Español efecto.", NamedApiResourceDto("es", "")),
                MoveEffectEntryDto("English effect.", NamedApiResourceDto("en", ""))
            ),
            flavorTextEntries = emptyList(),
            learnedByPokemon = emptyList(),
            contestType = null,
            contestEffect = null
        )
        val result = toMoveDetail(dto, null, "es")
        assertEquals("Español efecto.", result.effectEntry)
    }

    @Test
    fun `toMoveDetail falls back to en for effectEntry`() {
        val dto = MoveDetailDto(
            id = 1, name = "tackle", accuracy = 100, power = 40, pp = 35, priority = 0,
            type = NamedApiResourceDto("normal", ""),
            damageClass = NamedApiResourceDto("physical", ""),
            effectEntries = listOf(MoveEffectEntryDto("English effect.", NamedApiResourceDto("en", ""))),
            flavorTextEntries = emptyList(),
            learnedByPokemon = emptyList(),
            contestType = null,
            contestEffect = null
        )
        val result = toMoveDetail(dto, null, "es")
        assertEquals("English effect.", result.effectEntry)
    }

    @Test
    fun `toMoveDetail uses requested language for contestEffect effectEntry`() {
        val dto = MoveDetailDto(
            id = 1, name = "tackle", accuracy = 100, power = 40, pp = 35, priority = 0,
            type = NamedApiResourceDto("normal", ""),
            damageClass = NamedApiResourceDto("physical", ""),
            effectEntries = emptyList(),
            flavorTextEntries = emptyList(),
            learnedByPokemon = emptyList(),
            contestType = NamedApiResourceDto("cool", ""),
            contestEffect = null
        )
        val contest = ContestEffectDto(
            id = 1, appeal = 2, jam = 0,
            effectEntries = listOf(
                ContestEffectEntryDto("Efecto concurso ES.", NamedApiResourceDto("es", "")),
                ContestEffectEntryDto("Contest effect EN.", NamedApiResourceDto("en", ""))
            )
        )
        val result = toMoveDetail(dto, contest, "es")
        assertEquals("Efecto concurso ES.", result.contestEffect?.effectEntry)
    }
}
