package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.EvolutionChainDto
import dev.huntdex.core.data.network.dto.GenerationDto
import dev.huntdex.core.data.network.dto.LocationEncounterDto
import dev.huntdex.core.data.network.dto.PokemonDetailDto
import dev.huntdex.core.data.network.dto.PokemonListResponseDto
import dev.huntdex.core.data.network.dto.PokemonSpeciesDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class PokemonApi(private val client: HttpClient) {

    suspend fun getPokemonList(limit: Int, offset: Int): PokemonListResponseDto =
        client.get("$BASE_URL/pokemon?limit=$limit&offset=$offset").body()

    suspend fun getPokemonDetail(id: Int): PokemonDetailDto =
        client.get("$BASE_URL/pokemon/$id").body()

    suspend fun getPokemonSpecies(id: Int): PokemonSpeciesDto =
        client.get("$BASE_URL/pokemon-species/$id").body()

    suspend fun getEvolutionChain(id: Int): EvolutionChainDto =
        client.get("$BASE_URL/evolution-chain/$id").body()

    suspend fun getPokemonEncounters(id: Int): List<LocationEncounterDto> =
        client.get("$BASE_URL/pokemon/$id/encounters").body()

    suspend fun getGeneration(generationId: Int): GenerationDto =
        client.get("$BASE_URL/generation/$generationId").body()
}
