package dev.huntdex.feature.moves

import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry
import dev.huntdex.core.domain.repository.MoveRepository

class FakeMoveRepository : MoveRepository {
    val entries = (1..30).map { id -> MoveEntry(id, "move-$id") }

    override suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry> =
        entries.drop(offset).take(limit)

    override suspend fun getMovesByType(typeName: String): List<MoveEntry> = entries.take(5)

    override suspend fun getMovesByDamageClass(className: String): List<MoveEntry> = entries.take(3)

    override suspend fun getMoveDetail(id: Int): MoveDetail = MoveDetail(
        id = id, name = "move-$id", type = "normal", damageClass = "physical",
        power = 40, accuracy = 100, pp = 35, priority = 0,
        effectEntry = "Does damage.", flavorText = "A test move.",
        learnedBy = (1..15).map { LearnedByPokemon(it, "pokemon-$it", listOf("level-up")) },
        contestEffect = MoveContestEffect("tough", 2, 1, "Startles the foe.")
    )
}
