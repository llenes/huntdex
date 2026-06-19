package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class NamedApiResourceDto(val name: String, val url: String)

@Serializable
data class ApiResourceDto(val url: String)

/** Extracts the numeric ID from a PokeAPI URL like "https://pokeapi.co/api/v2/pokemon/1/" */
fun String.extractPokeApiId(): Int =
    trimEnd('/').substringAfterLast('/').toInt()
