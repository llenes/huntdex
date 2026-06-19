package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail

fun toMoveDetail(dto: MoveDetailDto, contestEffectDto: ContestEffectDto?): MoveDetail {
    val effectEntry = dto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
    val flavorText = dto.flavorTextEntries
        .lastOrNull { it.language.name == "en" }
        ?.flavorText?.replace("\n", " ")?.replace("", " ") ?: ""

    val learnedBy = dto.learnedByPokemon.map { learned ->
        val pokemonId = learned.pokemon.url.extractPokeApiId()
        val methods = learned.versionDetails.map { it.moveLearnMethod.name }.distinct()
        LearnedByPokemon(pokemonId, learned.pokemon.name, methods)
    }.sortedBy { it.id }

    val contestEffect = if (dto.contestType != null && contestEffectDto != null) {
        val effectDesc = contestEffectDto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
        MoveContestEffect(
            contestType = dto.contestType.name,
            appeal = contestEffectDto.appeal,
            jam = contestEffectDto.jam,
            effectEntry = effectDesc
        )
    } else null

    return MoveDetail(
        id = dto.id,
        name = dto.name,
        type = dto.type.name,
        damageClass = dto.damageClass.name,
        power = dto.power,
        accuracy = dto.accuracy,
        pp = dto.pp,
        priority = dto.priority,
        effectEntry = effectEntry,
        flavorText = flavorText,
        learnedBy = learnedBy,
        contestEffect = contestEffect
    )
}
