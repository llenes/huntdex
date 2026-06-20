package dev.huntdex.feature.moves.list

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.moves.FakeMoveRepository
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
class MoveListScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading with empty list`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.moves.isEmpty())
    }

    @Test
    fun `after init first page is loaded`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(20, model.state.value.moves.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `search filters displayed list by name`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(MoveListIntent.Search("move-1"))
        val names = model.state.value.displayedMoves.map { it.name }
        assertTrue(names.all { it.contains("move-1") })
    }

    @Test
    fun `selecting move navigates to detail`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = MoveListScreenModel(FakeMoveRepository(), navigator, this)
        model.onIntent(MoveListIntent.SelectMove(42))
        assertEquals(Destination.MoveDetail(42), navigator.destinations.last())
    }

    @Test
    fun `load next page appends to list`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(MoveListIntent.LoadNextPage)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(30, model.state.value.moves.size)
    }

    @Test
    fun `filter by type replaces list`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(MoveListIntent.FilterByType("fire"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, model.state.value.moves.size)
        assertEquals("fire", model.state.value.selectedType)
    }

    @Test
    fun `filter by damage class replaces list`() = testScope.runTest {
        val model = MoveListScreenModel(FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(MoveListIntent.FilterByDamageClass("physical"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(3, model.state.value.moves.size)
        assertEquals("physical", model.state.value.selectedDamageClass)
    }
}
