package dev.huntdex.feature.moves.detail

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
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

private class FakeAppNavigator : AppNavigator {
    var backCalled = false
    override fun navigateTo(destination: Destination) {}
    override fun navigateBack() { backCalled = true }
    override fun popTo(destination: Destination, inclusive: Boolean) {}
    override fun <T> setResult(key: String, value: T) {}
    override fun <T> getResult(key: String): Flow<T?> = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
class MoveDetailScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertNull(model.state.value.detail)
    }

    @Test
    fun `after init detail is loaded`() = testScope.runTest {
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertNotNull(model.state.value.detail)
        assertEquals(1, model.state.value.detail?.id)
    }

    @Test
    fun `learned by shows first 10 by default`() = testScope.runTest {
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(10, model.state.value.learnedByVisible.size)
        assertTrue(model.state.value.hasMoreLearnedBy)
    }

    @Test
    fun `expand learned by shows all`() = testScope.runTest {
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(MoveDetailIntent.ExpandLearnedBy)
        assertEquals(15, model.state.value.learnedByVisible.size)
        assertTrue(model.state.value.showAllLearnedBy)
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), navigator, this)
        model.onIntent(MoveDetailIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }

    @Test
    fun `contest effect is present when move has it`() = testScope.runTest {
        val model = MoveDetailScreenModel(1, FakeMoveRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertNotNull(model.state.value.detail?.contestEffect)
        assertEquals("tough", model.state.value.detail?.contestEffect?.contestType)
    }
}
