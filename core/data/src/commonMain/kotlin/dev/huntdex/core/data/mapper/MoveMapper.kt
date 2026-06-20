package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail

fun toMoveDetail(dto: MoveDetailDto, contestEffectDto: ContestEffectDto?, languageCode: String): MoveDetail {
    val effectEntry = dto.effectEntries
        .firstOrNull { it.language.name == languageCode }?.effect
        ?: dto.effectEntries.firstOrNull { it.language.name == "en" }?.effect
        ?: ""

    val flavorText = dto.flavorTextEntries
        .lastOrNull { it.language.name == languageCode }?.flavorText
        ?: dto.flavorTextEntries.lastOrNull { it.language.name == "en" }?.flavorText
        ?: ""

    val learnedBy = dto.learnedByPokemon.map { learned ->
        LearnedByPokemon(
            id = learned.url.extractPokeApiId(),
            name = learned.name,
            learnMethods = emptyList()
        )
    }.sortedBy { it.id }

    val contestEffect = if (dto.contestType != null && contestEffectDto != null) {
        val effectDesc = contestEffectDto.effectEntries
            .firstOrNull { it.language.name == languageCode }?.effect
            ?: contestEffectDto.effectEntries.firstOrNull { it.language.name == "en" }?.effect
            ?: ""
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
        flavorText = flavorText.replace("\n", " ").replace("", " "),
        learnedBy = learnedBy,
        contestEffect = contestEffect
    )
}
