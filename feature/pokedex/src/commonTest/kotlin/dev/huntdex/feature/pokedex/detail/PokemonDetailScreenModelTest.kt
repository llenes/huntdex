package dev.huntdex.feature.pokedex.detail

import dev.huntdex.feature.pokedex.FakePokemonRepository
import dev.huntdex.feature.pokedex.list.FakeAppNavigator
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class PokemonDetailScreenModelTest {

    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = PokemonDetailScreenModel(1, FakePokemonRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertNull(model.state.value.detail)
    }

    @Test
    fun `after init detail is loaded`() = testScope.runTest {
        val model = PokemonDetailScreenModel(1, FakePokemonRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(1, model.state.value.detail?.id)
        assertEquals("pokemon-1", model.state.value.detail?.name)
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = PokemonDetailScreenModel(1, FakePokemonRepository(), navigator, this)
        model.onIntent(PokemonDetailIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }
}
