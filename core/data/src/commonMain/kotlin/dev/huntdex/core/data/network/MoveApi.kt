package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.MoveListResponseDto
import dev.huntdex.core.data.network.dto.MoveDamageClassDto
import dev.huntdex.core.data.network.dto.TypeDetailDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

class MoveApi(private val client: HttpClient) {
    suspend fun getMoveList(limit: Int, offset: Int): MoveListResponseDto =
        client.get("$BASE_URL/move?limit=$limit&offset=$offset").body()

    suspend fun getMoveDetail(id: Int): MoveDetailDto =
        client.get("$BASE_URL/move/$id").body()

    suspend fun getContestEffect(id: Int): ContestEffectDto =
        client.get("$BASE_URL/contest-effect/$id").body()

    suspend fun getTypeDetail(typeName: String): TypeDetailDto =
        client.get("$BASE_URL/type/$typeName").body()

    suspend fun getDamageClassDetail(className: String): MoveDamageClassDto =
        client.get("$BASE_URL/move-damage-class/$className").body()
}
