package dev.huntdex.core.navigation

sealed class Destination {
    data object PokemonList : Destination()
    data class PokemonDetail(val id: Int) : Destination()
    data object MoveList : Destination()
    data class MoveDetail(val id: Int) : Destination()
    data object ItemList : Destination()
    data class ItemDetail(val id: Int) : Destination()
    data object RegionList : Destination()
    data class LocationDetail(val id: Int) : Destination()
    data object GenerationList : Destination()
    data object HuntingList : Destination()
    data class HuntingSessionDetail(val sessionId: String) : Destination()
    data class NewHuntingSession(val pokemonId: Int? = null) : Destination()
    data object Profile : Destination()
}
