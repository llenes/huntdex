package dev.huntdex.feature.pokedex.list

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.pokedex.FakePokemonRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FakeAppNavigator : AppNavigator {
    val destinations = mutableListOf<Destination>()
    override fun navigateTo(destination: Destination) { destinations += destination }
    override fun navigateBack() {}
    override fun popTo(destination: Destination, inclusive: Boolean) {}
    override fun <T> setResult(key: String, value: T) {}
    override fun <T> getResult(key: String): Flow<T?> = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonListScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading with empty list`() = testScope.runTest {
        val model = PokemonListScreenModel(FakePokemonRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.pokemon.isEmpty())
    }

    @Test
    fun `after init first page is loaded`() = testScope.runTest {
        val model = PokemonListScreenModel(FakePokemonRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(20, model.state.value.pokemon.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `search filters displayed list by name`() = testScope.runTest {
        val model = PokemonListScreenModel(FakePokemonRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(PokemonListIntent.Search("pokemon-1"))
        assertEquals("pokemon-1", model.state.value.searchQuery)
        val names = model.state.value.displayedPokemon.map { it.name }
        assertTrue(names.all { it.contains("pokemon-1") })
    }

    @Test
    fun `selecting pokemon navigates to detail`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = PokemonListScreenModel(FakePokemonRepository(), navigator, this)
        model.onIntent(PokemonListIntent.SelectPokemon(42))
        assertEquals(Destination.PokemonDetail(42), navigator.destinations.last())
    }

    @Test
    fun `load next page appends to list`() = testScope.runTest {
        val model = PokemonListScreenModel(FakePokemonRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(20, model.state.value.pokemon.size)
        model.onIntent(PokemonListIntent.LoadNextPage)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(30, model.state.value.pokemon.size)
    }
}
