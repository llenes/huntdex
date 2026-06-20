package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry

interface MoveRepository {
    suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry>
    suspend fun getMovesByType(typeName: String): List<MoveEntry>
    suspend fun getMovesByDamageClass(className: String): List<MoveEntry>
    suspend fun getMoveDetail(id: Int): MoveDetail
}
