package dev.huntdex.core.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals

class PokemonEntryTest {

    @Test
    fun `entries with same id are equal`() {
        val a = PokemonEntry(id = 1, name = "bulbasaur", spriteUrl = "https://example.com/1.png")
        val b = PokemonEntry(id = 1, name = "bulbasaur", spriteUrl = "https://example.com/1.png")
        assertEquals(a, b)
    }

    @Test
    fun `entries with different id are not equal`() {
        val a = PokemonEntry(id = 1, name = "bulbasaur", spriteUrl = "https://example.com/1.png")
        val b = PokemonEntry(id = 2, name = "ivysaur", spriteUrl = "https://example.com/2.png")
        assertNotEquals(a, b)
    }

    @Test
    fun `copy produces independent instance`() {
        val original = PokemonEntry(id = 1, name = "bulbasaur", spriteUrl = "https://example.com/1.png")
        val copy = original.copy(name = "ivysaur")
        assertEquals(1, copy.id)
        assertEquals("ivysaur", copy.name)
        assertEquals("bulbasaur", original.name)
    }
}
