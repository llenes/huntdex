package dev.huntdex.core.data.repository

import dev.huntdex.core.common.LocaleProvider
import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.mapper.spriteUrl
import dev.huntdex.core.data.mapper.toPokemonDetail
import dev.huntdex.core.data.mapper.toPokemonEntry
import dev.huntdex.core.data.network.PokemonApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.PokemonDetail
import dev.huntdex.core.domain.model.PokemonEntry
import dev.huntdex.core.domain.repository.PokemonRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class PokemonRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: PokemonApi,
    private val localeProvider: LocaleProvider = LocaleProvider()
) : PokemonRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getPokemonPage(limit: Int, offset: Int): List<PokemonEntry> {
        val cached = queries
            .selectPokemonPage(limit.toLong(), offset.toLong())
            .executeAsList()
        if (cached.isNotEmpty()) {
            return cached.map { PokemonEntry(it.id.toInt(), it.name, it.sprite_url) }
        }

        val response = api.getPokemonList(limit, offset)
        response.results.forEach { result ->
            val id = result.url.extractPokeApiId()
            queries.insertPokemonEntry(id.toLong(), result.name, spriteUrl(id))
        }
        return queries
            .selectPokemonPage(limit.toLong(), offset.toLong())
            .executeAsList()
            .map { PokemonEntry(it.id.toInt(), it.name, it.sprite_url) }
    }

    override suspend fun getPokemonByGeneration(generationId: Int): List<PokemonEntry> {
        val generation = api.getGeneration(generationId)
        return generation.pokemonSpecies
            .map { species ->
                val id = species.url.extractPokeApiId()
                toPokemonEntry(id, species.name)
            }
            .sortedBy { it.id }
    }

    override suspend fun getPokemonDetail(id: Int): PokemonDetail {
        val cachedJson = queries.selectPokemonDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) {
            return Json.decodeFromString(cachedJson)
        }

        val detail = coroutineScope {
            val detailDeferred = async { api.getPokemonDetail(id) }
            val encountersDeferred = async { api.getPokemonEncounters(id) }

            val detailDto = detailDeferred.await()
            val speciesId = detailDto.species.url.extractPokeApiId()

            val speciesDeferred = async { api.getPokemonSpecies(speciesId) }
            val speciesDto = speciesDeferred.await()
            val chainId = speciesDto.evolutionChain.url.extractPokeApiId()

            val chainDeferred = async { api.getEvolutionChain(chainId) }
            val encounters = encountersDeferred.await()
            val chainDto = chainDeferred.await()

            toPokemonDetail(detailDto, speciesDto, chainDto, encounters, localeProvider.languageCode())
        }

        queries.insertPokemonDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }
}
