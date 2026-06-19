package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MoveListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)
