package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TypeDetailDto(val id: Int, val name: String, val moves: List<NamedApiResourceDto>)

@Serializable
data class MoveDamageClassDto(val id: Int, val name: String, val moves: List<NamedApiResourceDto>)
