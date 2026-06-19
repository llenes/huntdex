package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.PokemonDetail
import dev.huntdex.core.domain.model.PokemonEntry

interface PokemonRepository {
    /** Returns a page of all Pokémon. Checks SQLDelight cache first; fetches API on miss. */
    suspend fun getPokemonPage(limit: Int, offset: Int): List<PokemonEntry>

    /** Returns all Pokémon in a generation. Network-only in Phase 1. */
    suspend fun getPokemonByGeneration(generationId: Int): List<PokemonEntry>

    /** Returns full detail. Checks SQLDelight cache first; fetches 3 parallel API calls on miss. */
    suspend fun getPokemonDetail(id: Int): PokemonDetail
}
