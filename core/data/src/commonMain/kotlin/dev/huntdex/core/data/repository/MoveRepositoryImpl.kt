package dev.huntdex.core.data.repository

import dev.huntdex.core.common.LocaleProvider
import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.mapper.toMoveDetail
import dev.huntdex.core.data.network.MoveApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry
import dev.huntdex.core.domain.repository.MoveRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MoveRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: MoveApi,
    private val localeProvider: LocaleProvider = LocaleProvider()
) : MoveRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry> {
        val cached = queries.selectMovePage(limit.toLong(), offset.toLong()).executeAsList()
        if (cached.isNotEmpty()) return cached.map { MoveEntry(it.id.toInt(), it.name) }

        val response = api.getMoveList(limit, offset)
        response.results.forEach { result ->
            val id = result.url.extractPokeApiId()
            queries.insertMoveEntry(id.toLong(), result.name)
        }
        return queries.selectMovePage(limit.toLong(), offset.toLong()).executeAsList()
            .map { MoveEntry(it.id.toInt(), it.name) }
    }

    override suspend fun getMovesByType(typeName: String): List<MoveEntry> {
        val typeDetail = api.getTypeDetail(typeName)
        return typeDetail.moves
            .map { MoveEntry(it.url.extractPokeApiId(), it.name) }
            .sortedBy { it.id }
    }

    override suspend fun getMovesByDamageClass(className: String): List<MoveEntry> {
        val classDetail = api.getDamageClassDetail(className)
        return classDetail.moves
            .map { MoveEntry(it.url.extractPokeApiId(), it.name) }
            .sortedBy { it.id }
    }

    override suspend fun getMoveDetail(id: Int): MoveDetail {
        val cachedJson = queries.selectMoveDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) return Json.decodeFromString(cachedJson)

        val detail = coroutineScope {
            val moveDto = api.getMoveDetail(id)
            val contestEffectDeferred = moveDto.contestEffect?.url?.extractPokeApiId()
                ?.let { async { api.getContestEffect(it) } }
            val contestEffectDto = contestEffectDeferred?.await()
            toMoveDetail(moveDto, contestEffectDto, localeProvider.languageCode())
        }

        queries.insertMoveDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }
}
