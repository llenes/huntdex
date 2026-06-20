# Phase 2 PR 4 — feature:games + GlobalFilterRepository Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Introduce `GlobalFilterRepository` in `:core:domain`, implement it in `:core:data`, build `GenerationListScreen` that sets the active filter, and surgically refactor moves/items/locations ScreenModels to read from the global repository instead of local state.

**Architecture:** The filter is a single in-memory `MutableStateFlow<Generation?>` owned by `:core:data`. ScreenModels that previously held local `selectedGeneration` state now inject `GlobalFilterRepository` and observe its flow. The UI layer is untouched — filter chips still call `onIntent(FilterByGeneration(id))`, but intents now delegate to the repository. No persistence: the filter resets on app restart.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Voyager, Koin, Ktor, SQLDelight, kotlinx.serialization.

## Global Constraints

- Kotlin 2.0.21, Compose Multiplatform 1.7.0
- Koin 3.5.6, Coil 3.0.4, coroutines 1.9.0
- All new domain models must be `@Serializable`
- Target platforms: Android + Desktop + iOS (commonMain code only)
- Test runner: `./gradlew :feature:games:desktopTest`
- PRs 1–3 must be merged before this PR is opened

---

### Task 1: GlobalFilterRepository in core:domain + GenerationEntry model

**Files:**
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/GenerationEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GlobalFilterRepository.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GenerationRepository.kt`

- [ ] **Step 1: Create `GenerationEntry.kt`**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/GenerationEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class GenerationEntry(
    val id: Int,
    val name: String,
    val versions: List<String>
)
```

- [ ] **Step 2: Create `GlobalFilterRepository.kt`**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GlobalFilterRepository.kt`:
```kotlin
package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.GenerationEntry
import kotlinx.coroutines.flow.StateFlow

interface GlobalFilterRepository {
    val selectedGeneration: StateFlow<GenerationEntry?>
    fun setGeneration(generation: GenerationEntry?)
}
```

- [ ] **Step 3: Create `GenerationRepository.kt`**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GenerationRepository.kt`:
```kotlin
package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.GenerationEntry

interface GenerationRepository {
    suspend fun getAllGenerations(): List<GenerationEntry>
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :core:domain:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/GenerationEntry.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GlobalFilterRepository.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/GenerationRepository.kt
git commit -m "feat(games): GlobalFilterRepository interface and GenerationEntry domain model"
```

---

### Task 2: SQLDelight schema + GlobalFilterRepository implementation + GenerationRepositoryImpl

**Files:**
- Modify: `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`
- Modify: `core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GlobalFilterRepositoryImpl.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/GenerationDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/GenerationApi.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GenerationRepositoryImpl.kt`
- Modify: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt`

- [ ] **Step 1: Add `generation_entry` table to `HuntdexDatabase.sq`**

Append to the file:
```sql
-- ============================================================
-- Generation Cache (static data, versions as JSON array)
-- ============================================================
CREATE TABLE generation_entry (
  id       INTEGER PRIMARY KEY,
  name     TEXT    NOT NULL,
  versions TEXT    NOT NULL
);

insertGenerationEntry:
INSERT OR IGNORE INTO generation_entry (id, name, versions) VALUES (?, ?, ?);

selectAllGenerations:
SELECT id, name, versions FROM generation_entry ORDER BY id;
```

- [ ] **Step 2: Update migration guard in `DatabaseDriverFactory.kt`**

Add `"generation_entry" !in tables` branch. The full updated `when` block:
```kotlin
when {
    tables.isEmpty() ->
        HuntdexDatabase.Schema.create(driver)
    "pokemon_entry" !in tables -> {
        // same DDL as task 2 of PR3, plus generation_entry
        driver.execute(null, "CREATE TABLE pokemon_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, sprite_url TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE pokemon_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE move_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE move_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE generation_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, versions TEXT NOT NULL)", 0)
    }
    "move_entry" !in tables -> {
        driver.execute(null, "CREATE TABLE move_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE move_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE generation_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, versions TEXT NOT NULL)", 0)
    }
    "item_entry" !in tables -> {
        driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE generation_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, versions TEXT NOT NULL)", 0)
    }
    "region_entry" !in tables -> {
        driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
        driver.execute(null, "CREATE TABLE generation_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, versions TEXT NOT NULL)", 0)
    }
    "generation_entry" !in tables -> {
        driver.execute(null, "CREATE TABLE generation_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, versions TEXT NOT NULL)", 0)
    }
    // else: all tables present, nothing to do
}
```

- [ ] **Step 3: Create `GlobalFilterRepositoryImpl.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GlobalFilterRepositoryImpl.kt`:
```kotlin
package dev.huntdex.core.data.repository

import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class GlobalFilterRepositoryImpl : GlobalFilterRepository {
    private val _selectedGeneration = MutableStateFlow<GenerationEntry?>(null)
    override val selectedGeneration: StateFlow<GenerationEntry?> = _selectedGeneration.asStateFlow()

    override fun setGeneration(generation: GenerationEntry?) {
        _selectedGeneration.value = generation
    }
}
```

- [ ] **Step 4: Create `GenerationDto.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/GenerationDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class GenerationListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)

@Serializable
data class GenerationDetailDto(
    val id: Int,
    val name: String,
    @SerialName("version_groups") val versionGroups: List<NamedApiResourceDto>
)

@Serializable
data class VersionGroupDetailDto(
    val id: Int,
    val name: String,
    val versions: List<NamedApiResourceDto>
)
```

- [ ] **Step 5: Create `GenerationApi.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/GenerationApi.kt`:
```kotlin
package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.GenerationDetailDto
import dev.huntdex.core.data.network.dto.GenerationListResponseDto
import dev.huntdex.core.data.network.dto.VersionGroupDetailDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val BASE_URL = "https://pokeapi.co/api/v2"

class GenerationApi(private val client: HttpClient) {
    suspend fun getGenerationList(): GenerationListResponseDto =
        client.get("$BASE_URL/generation").body()

    suspend fun getGenerationDetail(id: Int): GenerationDetailDto =
        client.get("$BASE_URL/generation/$id").body()

    suspend fun getVersionGroup(id: Int): VersionGroupDetailDto =
        client.get("$BASE_URL/version-group/$id").body()
}
```

- [ ] **Step 6: Create `GenerationRepositoryImpl.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GenerationRepositoryImpl.kt`:
```kotlin
package dev.huntdex.core.data.repository

import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.network.GenerationApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.repository.GenerationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class GenerationRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: GenerationApi
) : GenerationRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getAllGenerations(): List<GenerationEntry> {
        val cached = queries.selectAllGenerations().executeAsList()
        if (cached.isNotEmpty()) return cached.map { row ->
            GenerationEntry(row.id.toInt(), row.name, Json.decodeFromString(row.versions))
        }

        val listResponse = api.getGenerationList()
        val generations = coroutineScope {
            listResponse.results.map { async { api.getGenerationDetail(it.url.extractPokeApiId()) } }.awaitAll()
        }

        val entries = generations.map { dto ->
            val versionNames = coroutineScope {
                dto.versionGroups.map { vg ->
                    async { api.getVersionGroup(vg.url.extractPokeApiId()).versions.map { it.name } }
                }.awaitAll().flatten()
            }
            GenerationEntry(dto.id, dto.name, versionNames)
        }

        entries.forEach { entry ->
            queries.insertGenerationEntry(entry.id.toLong(), entry.name, Json.encodeToString(entry.versions))
        }

        return entries
    }
}
```

- [ ] **Step 7: Register in `DataModule.kt`**

Add to `dataModule`:
```kotlin
import dev.huntdex.core.data.network.GenerationApi
import dev.huntdex.core.data.repository.GenerationRepositoryImpl
import dev.huntdex.core.data.repository.GlobalFilterRepositoryImpl
import dev.huntdex.core.domain.repository.GenerationRepository
import dev.huntdex.core.domain.repository.GlobalFilterRepository

// Inside module { }, register as single (shared instance):
single<GlobalFilterRepository> { GlobalFilterRepositoryImpl() }
single { GenerationApi(get()) }
single<GenerationRepository> { GenerationRepositoryImpl(get(), get()) }
```

- [ ] **Step 8: Verify**

```bash
./gradlew :core:data:generateSqlDelightInterface :core:data:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit**

```bash
git add core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq \
        core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GlobalFilterRepositoryImpl.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/GenerationDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/GenerationApi.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/GenerationRepositoryImpl.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt
git commit -m "feat(games): GlobalFilterRepositoryImpl, GenerationRepositoryImpl, generation_entry table"
```

---

### Task 3: GenerationListScreenModel + tests

**Files:**
- Modify: `feature/games/build.gradle.kts`
- Delete: `feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/Placeholder.kt`
- Delete: `feature/games/src/desktopMain/kotlin/dev/huntdex/feature/games/DesktopStub.kt`
- Delete: `feature/games/src/iosMain/kotlin/dev/huntdex/feature/games/IosStub.kt`
- Create: `feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListState.kt`
- Create: `feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListIntent.kt`
- Create: `feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListScreenModel.kt`
- Create: `feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/FakeGenerationRepository.kt`
- Create: `feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/FakeGlobalFilterRepository.kt`
- Create: `feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/GenerationListScreenModelTest.kt`

- [ ] **Step 1: Upgrade `feature/games/build.gradle.kts`**

Replace the entire file:
```kotlin
plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.library)
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.kotlin.compose)
}

kotlin {
    jvmToolchain(17)
    androidTarget()
    jvm("desktop")
    iosX64()
    iosArm64()
    iosSimulatorArm64()

    sourceSets {
        commonMain.dependencies {
            implementation(projects.core.domain)
            implementation(projects.core.navigation)
            implementation(projects.core.ui)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.ui)
            implementation(libs.voyager.navigator)
            implementation(libs.voyager.koin)
            implementation(libs.koin.core)
            implementation(libs.coroutines.core)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}

android {
    namespace = "dev.huntdex.feature.games"
    compileSdk = 35
    defaultConfig { minSdk = 26 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}
```

- [ ] **Step 2: Delete stub files**

```bash
rm feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/Placeholder.kt
rm feature/games/src/desktopMain/kotlin/dev/huntdex/feature/games/DesktopStub.kt
rm feature/games/src/iosMain/kotlin/dev/huntdex/feature/games/IosStub.kt
```

- [ ] **Step 3: Create MVI files**

`feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListState.kt`:
```kotlin
package dev.huntdex.feature.games

import dev.huntdex.core.domain.model.GenerationEntry

data class GenerationListState(
    val generations: List<GenerationEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val activeGeneration: GenerationEntry? = null
)
```

`feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListIntent.kt`:
```kotlin
package dev.huntdex.feature.games

import dev.huntdex.core.domain.model.GenerationEntry

sealed interface GenerationListIntent {
    data class SelectGeneration(val generation: GenerationEntry) : GenerationListIntent
    data object ClearFilter : GenerationListIntent
    data object Retry : GenerationListIntent
}
```

- [ ] **Step 4: Write failing tests**

`feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/FakeGenerationRepository.kt`:
```kotlin
package dev.huntdex.feature.games

import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.repository.GenerationRepository

class FakeGenerationRepository : GenerationRepository {
    val generations = listOf(
        GenerationEntry(1, "generation-i", listOf("red", "blue", "yellow")),
        GenerationEntry(2, "generation-ii", listOf("gold", "silver", "crystal")),
        GenerationEntry(3, "generation-iii", listOf("ruby", "sapphire", "emerald"))
    )

    override suspend fun getAllGenerations(): List<GenerationEntry> = generations
}
```

`feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/FakeGlobalFilterRepository.kt`:
```kotlin
package dev.huntdex.feature.games

import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeGlobalFilterRepository : GlobalFilterRepository {
    private val _selectedGeneration = MutableStateFlow<GenerationEntry?>(null)
    override val selectedGeneration: StateFlow<GenerationEntry?> = _selectedGeneration.asStateFlow()

    override fun setGeneration(generation: GenerationEntry?) {
        _selectedGeneration.value = generation
    }
}
```

`feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/GenerationListScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.games

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
class GenerationListScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = GenerationListScreenModel(FakeGenerationRepository(), FakeGlobalFilterRepository(), this)
        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.generations.isEmpty())
    }

    @Test
    fun `after init generations are loaded`() = testScope.runTest {
        val model = GenerationListScreenModel(FakeGenerationRepository(), FakeGlobalFilterRepository(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(3, model.state.value.generations.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `initial active generation reflects global filter`() = testScope.runTest {
        val filter = FakeGlobalFilterRepository()
        val gen1 = FakeGenerationRepository().generations.first()
        filter.setGeneration(gen1)
        val model = GenerationListScreenModel(FakeGenerationRepository(), filter, this)
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(gen1, model.state.value.activeGeneration)
    }

    @Test
    fun `selecting generation updates global filter`() = testScope.runTest {
        val filter = FakeGlobalFilterRepository()
        val model = GenerationListScreenModel(FakeGenerationRepository(), filter, this)
        dispatcher.scheduler.advanceUntilIdle()
        val gen2 = model.state.value.generations[1]
        model.onIntent(GenerationListIntent.SelectGeneration(gen2))
        assertEquals(gen2, filter.selectedGeneration.value)
        assertEquals(gen2, model.state.value.activeGeneration)
    }

    @Test
    fun `clearing filter sets global filter to null`() = testScope.runTest {
        val filter = FakeGlobalFilterRepository()
        filter.setGeneration(FakeGenerationRepository().generations.first())
        val model = GenerationListScreenModel(FakeGenerationRepository(), filter, this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(GenerationListIntent.ClearFilter)
        assertNull(filter.selectedGeneration.value)
        assertNull(model.state.value.activeGeneration)
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

```bash
./gradlew :feature:games:desktopTest
```
Expected: FAIL — `GenerationListScreenModel` not defined yet

- [ ] **Step 6: Create `GenerationListScreenModel.kt`**

`feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListScreenModel.kt`:
```kotlin
package dev.huntdex.feature.games

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.repository.GenerationRepository
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class GenerationListScreenModel(
    private val generationRepository: GenerationRepository,
    private val filterRepository: GlobalFilterRepository,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(GenerationListState())
    val state: StateFlow<GenerationListState> = _state.asStateFlow()

    init {
        filterRepository.selectedGeneration
            .onEach { gen -> _state.update { it.copy(activeGeneration = gen) } }
            .launchIn(scope)
        loadGenerations()
    }

    fun onIntent(intent: GenerationListIntent) {
        when (intent) {
            is GenerationListIntent.SelectGeneration -> {
                val newGen = if (filterRepository.selectedGeneration.value == intent.generation) null else intent.generation
                filterRepository.setGeneration(newGen)
            }
            is GenerationListIntent.ClearFilter -> filterRepository.setGeneration(null)
            is GenerationListIntent.Retry -> loadGenerations()
        }
    }

    private fun loadGenerations() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { generationRepository.getAllGenerations() }
                .onSuccess { gens -> _state.update { it.copy(generations = gens, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 7: Run all tests**

```bash
./gradlew :feature:games:desktopTest
```
Expected: All 5 tests PASS

- [ ] **Step 8: Commit**

```bash
git add feature/games/build.gradle.kts \
        feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/ \
        feature/games/src/commonTest/kotlin/dev/huntdex/feature/games/
git commit -m "feat(games): GenerationListScreenModel reads/writes GlobalFilterRepository"
```

---

### Task 4: GenerationListScreen UI

**Files:**
- Create: `feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListScreen.kt`

- [ ] **Step 1: Create `GenerationListScreen.kt`**

`feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListScreen.kt`:
```kotlin
package dev.huntdex.feature.games

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.domain.model.GenerationEntry

data object GenerationListScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<GenerationListScreenModel>()
        val state by model.state.collectAsState()
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Button(onClick = { model.onIntent(GenerationListIntent.Retry) }) { Text("Retry") }
                }
            }
            else -> GenerationList(state = state, onIntent = model::onIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GenerationList(state: GenerationListState, onIntent: (GenerationListIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        if (state.activeGeneration != null) {
            Surface(
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Filtering: ${state.activeGeneration.name.replace('-', ' ').replaceFirstChar { it.uppercase() }}",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                    TextButton(onClick = { onIntent(GenerationListIntent.ClearFilter) }) {
                        Text("Clear")
                    }
                }
            }
        }
        LazyColumn(Modifier.fillMaxSize()) {
            items(state.generations) { generation ->
                GenerationRow(
                    generation = generation,
                    isActive = generation == state.activeGeneration,
                    onClick = { onIntent(GenerationListIntent.SelectGeneration(generation)) }
                )
                HorizontalDivider()
            }
        }
    }
}

@Composable
private fun GenerationRow(generation: GenerationEntry, isActive: Boolean, onClick: () -> Unit) {
    ListItem(
        headlineContent = {
            Text(
                generation.name.replace('-', ' ').replaceFirstChar { it.uppercase() },
                color = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
            )
        },
        supportingContent = {
            Text(
                generation.versions.joinToString(" · ") { it.replaceFirstChar { c -> c.uppercase() } },
                style = MaterialTheme.typography.bodySmall
            )
        },
        trailingContent = if (isActive) {
            { Icon(Icons.Default.Check, contentDescription = "Active", tint = MaterialTheme.colorScheme.primary) }
        } else null,
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Verify compilation**

```bash
./gradlew :feature:games:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 3: Commit**

```bash
git add feature/games/src/commonMain/kotlin/dev/huntdex/feature/games/GenerationListScreen.kt
git commit -m "feat(games): GenerationListScreen with active filter indicator"
```

---

### Task 5: Surgical refactor — moves ScreenModel

**Files:**
- Modify: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModel.kt`
- Modify: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListState.kt`
- Modify: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListIntent.kt`
- Modify: `feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModelTest.kt`

The refactor: remove local `selectedGeneration` storage from state/intent/screenmodel. Inject `GlobalFilterRepository` and observe its flow instead.

- [ ] **Step 1: Update `MoveListState.kt`**

Remove `selectedGeneration: Int?` field and `FilterByGeneration` from state. The generation is now global, not local.

```kotlin
package dev.huntdex.feature.moves.list

import dev.huntdex.core.domain.model.MoveEntry
import dev.huntdex.core.domain.model.GenerationEntry

data class MoveListState(
    val moves: List<MoveEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedType: String? = null,
    val selectedDamageClass: String? = null,
    val activeGeneration: GenerationEntry? = null,
    val canLoadMore: Boolean = true
) {
    val displayedMoves: List<MoveEntry>
        get() {
            var result = moves
            if (searchQuery.isNotBlank()) result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (selectedType != null) result = result.filter { it.type == selectedType }
            if (selectedDamageClass != null) result = result.filter { it.damageClass == selectedDamageClass }
            return result
        }
}
```

- [ ] **Step 2: Update `MoveListIntent.kt`**

Remove `FilterByGeneration` intent — generation is set via `GenerationListScreen`, not `MoveListScreen`.

```kotlin
package dev.huntdex.feature.moves.list

sealed interface MoveListIntent {
    data class Search(val query: String) : MoveListIntent
    data class FilterByType(val type: String?) : MoveListIntent
    data class FilterByDamageClass(val damageClass: String?) : MoveListIntent
    data class SelectMove(val id: Int) : MoveListIntent
    data object LoadNextPage : MoveListIntent
    data object Retry : MoveListIntent
}
```

- [ ] **Step 3: Update `MoveListScreenModel.kt`**

Inject `GlobalFilterRepository`, observe `selectedGeneration` flow, remove local `selectedGeneration` mutation:

```kotlin
package dev.huntdex.feature.moves.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoveListScreenModel(
    private val repository: MoveRepository,
    private val filterRepository: GlobalFilterRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(MoveListState())
    val state: StateFlow<MoveListState> = _state.asStateFlow()

    init {
        filterRepository.selectedGeneration
            .onEach { gen -> _state.update { it.copy(activeGeneration = gen) } }
            .launchIn(scope)
        loadFirstPage()
    }

    fun onIntent(intent: MoveListIntent) {
        when (intent) {
            is MoveListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is MoveListIntent.FilterByType -> _state.update { it.copy(selectedType = intent.type) }
            is MoveListIntent.FilterByDamageClass -> _state.update { it.copy(selectedDamageClass = intent.damageClass) }
            is MoveListIntent.SelectMove -> navigator.navigateTo(Destination.MoveDetail(intent.id))
            is MoveListIntent.LoadNextPage -> loadNextPage()
            is MoveListIntent.Retry -> loadFirstPage()
        }
    }

    private fun loadFirstPage() {
        _state.update { it.copy(isLoading = true, error = null, moves = emptyList()) }
        scope.launch {
            runCatching { repository.getMoveList(offset = 0, limit = 20) }
                .onSuccess { result ->
                    _state.update { it.copy(moves = result.entries, isLoading = false, canLoadMore = result.hasMore) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadNextPage() {
        if (_state.value.isLoadingMore || !_state.value.canLoadMore) return
        _state.update { it.copy(isLoadingMore = true) }
        val offset = _state.value.moves.size
        scope.launch {
            runCatching { repository.getMoveList(offset = offset, limit = 20) }
                .onSuccess { result ->
                    _state.update { it.copy(moves = it.moves + result.entries, isLoadingMore = false, canLoadMore = result.hasMore) }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 4: Update tests to use `FakeGlobalFilterRepository`**

In `MoveListScreenModelTest.kt`, add a `FakeGlobalFilterRepository` (same as in Task 3 tests) and pass it to `MoveListScreenModel(repo, fakeFilter, navigator, scope)`. Remove any test for `FilterByGeneration` intent (it no longer exists). Add a test that generation state comes from the filter repository:

```kotlin
@Test
fun `active generation reflects global filter`() = testScope.runTest {
    val filter = FakeGlobalFilterRepository()
    val model = MoveListScreenModel(FakeMoveRepository(), filter, FakeAppNavigator(), this)
    dispatcher.scheduler.advanceUntilIdle()
    val gen = GenerationEntry(1, "generation-i", listOf("red"))
    filter.setGeneration(gen)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(gen, model.state.value.activeGeneration)
}
```

- [ ] **Step 5: Run moves tests**

```bash
./gradlew :feature:moves:desktopTest
```
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/ \
        feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/list/
git commit -m "refactor(moves): inject GlobalFilterRepository, remove local generation filter state"
```

---

### Task 6: Surgical refactor — items ScreenModel

**Files:**
- Modify: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListScreenModel.kt`
- Modify: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListState.kt`
- Modify: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListIntent.kt`
- Modify: `feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/list/ItemListScreenModelTest.kt`

Same pattern as Task 5, applied to items. Generation filter for items affects which versions of pocket descriptions are shown (future enhancement); for now it's stored in state to match the global value.

- [ ] **Step 1: Update `ItemListState.kt`**

Add `activeGeneration: GenerationEntry? = null` field. Remove any local `selectedGeneration` field that previously existed.

```kotlin
package dev.huntdex.feature.items.list

import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.model.ItemEntry

enum class ItemTab { Objects, Berries }

data class ItemListState(
    val items: List<ItemEntry> = emptyList(),
    val berries: List<BerryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedPocket: String? = null,
    val activeTab: ItemTab = ItemTab.Objects,
    val activeGeneration: GenerationEntry? = null,
    val canLoadMore: Boolean = true
) {
    val displayedItems: List<ItemEntry>
        get() {
            var result = items
            if (searchQuery.isNotBlank()) result = result.filter { it.name.contains(searchQuery, ignoreCase = true) }
            if (selectedPocket != null) result = result.filter { it.pocket == selectedPocket }
            return result
        }

    val displayedBerries: List<BerryEntry>
        get() = if (searchQuery.isBlank()) berries
                else berries.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

- [ ] **Step 2: Update `ItemListIntent.kt`**

Remove `FilterByGeneration` intent:

```kotlin
package dev.huntdex.feature.items.list

sealed interface ItemListIntent {
    data class Search(val query: String) : ItemListIntent
    data class FilterByPocket(val pocket: String?) : ItemListIntent
    data class SwitchTab(val tab: ItemTab) : ItemListIntent
    data class SelectItem(val id: Int) : ItemListIntent
    data class SelectBerry(val id: Int) : ItemListIntent
    data object LoadNextPage : ItemListIntent
    data object Retry : ItemListIntent
}
```

- [ ] **Step 3: Update `ItemListScreenModel.kt`**

Inject `GlobalFilterRepository`, observe its flow, remove local generation mutation:

```kotlin
package dev.huntdex.feature.items.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import dev.huntdex.core.domain.repository.ItemRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemListScreenModel(
    private val repository: ItemRepository,
    private val filterRepository: GlobalFilterRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(ItemListState())
    val state: StateFlow<ItemListState> = _state.asStateFlow()

    init {
        filterRepository.selectedGeneration
            .onEach { gen -> _state.update { it.copy(activeGeneration = gen) } }
            .launchIn(scope)
        loadItems()
        loadBerries()
    }

    fun onIntent(intent: ItemListIntent) {
        when (intent) {
            is ItemListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is ItemListIntent.FilterByPocket -> _state.update { it.copy(selectedPocket = intent.pocket) }
            is ItemListIntent.SwitchTab -> _state.update { it.copy(activeTab = intent.tab, searchQuery = "") }
            is ItemListIntent.SelectItem -> navigator.navigateTo(Destination.ItemDetail(intent.id))
            is ItemListIntent.SelectBerry -> navigator.navigateTo(Destination.BerryDetail(intent.id))
            is ItemListIntent.LoadNextPage -> loadNextPage()
            is ItemListIntent.Retry -> { loadItems(); loadBerries() }
        }
    }

    private fun loadItems() {
        scope.launch {
            runCatching { repository.getItemList(offset = 0, limit = 20) }
                .onSuccess { result ->
                    _state.update { it.copy(items = result.entries, isLoading = false, canLoadMore = result.hasMore) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadBerries() {
        scope.launch {
            runCatching { repository.getAllBerries() }
                .onSuccess { berries -> _state.update { it.copy(berries = berries) } }
                .onFailure { /* berries are supplemental */ }
        }
    }

    private fun loadNextPage() {
        if (_state.value.isLoadingMore || !_state.value.canLoadMore) return
        _state.update { it.copy(isLoadingMore = true) }
        val offset = _state.value.items.size
        scope.launch {
            runCatching { repository.getItemList(offset = offset, limit = 20) }
                .onSuccess { result ->
                    _state.update { it.copy(items = it.items + result.entries, isLoadingMore = false, canLoadMore = result.hasMore) }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 4: Update tests**

In `ItemListScreenModelTest.kt`, replace `FakeGlobalFilterRepository` (same implementation) and add:
```kotlin
@Test
fun `active generation reflects global filter`() = testScope.runTest {
    val filter = FakeGlobalFilterRepository()
    val model = ItemListScreenModel(FakeItemRepository(), filter, FakeAppNavigator(), this)
    dispatcher.scheduler.advanceUntilIdle()
    val gen = GenerationEntry(1, "generation-i", listOf("red"))
    filter.setGeneration(gen)
    dispatcher.scheduler.advanceUntilIdle()
    assertEquals(gen, model.state.value.activeGeneration)
}
```

- [ ] **Step 5: Run items tests**

```bash
./gradlew :feature:items:desktopTest
```
Expected: All tests PASS

- [ ] **Step 6: Commit**

```bash
git add feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ \
        feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/list/
git commit -m "refactor(items): inject GlobalFilterRepository, remove local generation filter state"
```

---

### Task 7: Surgical refactor — locations ScreenModels

**Files:**
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModel.kt`
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListState.kt`
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListIntent.kt`
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModel.kt`
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailState.kt`
- Modify: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailIntent.kt`
- Modify: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModelTest.kt`
- Modify: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModelTest.kt`

- [ ] **Step 1: Update `LocationListState.kt`**

Replace `selectedGeneration: Int?` with `activeGeneration: GenerationEntry?`:

```kotlin
package dev.huntdex.feature.locations.list

import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.model.LocationEntry

data class LocationListState(
    val locations: List<LocationEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val activeGeneration: GenerationEntry? = null
) {
    val displayedLocations: List<LocationEntry>
        get() = if (searchQuery.isBlank()) locations
                else locations.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

- [ ] **Step 2: Update `LocationListIntent.kt`**

Remove `FilterByGeneration` intent:

```kotlin
package dev.huntdex.feature.locations.list

sealed interface LocationListIntent {
    data class Search(val query: String) : LocationListIntent
    data class SelectLocation(val id: Int) : LocationListIntent
    data object Retry : LocationListIntent
    data object NavigateBack : LocationListIntent
}
```

- [ ] **Step 3: Update `LocationListScreenModel.kt`**

Inject `GlobalFilterRepository`:

```kotlin
package dev.huntdex.feature.locations.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import dev.huntdex.core.domain.repository.LocationRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationListScreenModel(
    private val regionId: Int,
    private val repository: LocationRepository,
    private val filterRepository: GlobalFilterRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(LocationListState())
    val state: StateFlow<LocationListState> = _state.asStateFlow()

    init {
        filterRepository.selectedGeneration
            .onEach { gen -> _state.update { it.copy(activeGeneration = gen) } }
            .launchIn(scope)
        loadLocations()
    }

    fun onIntent(intent: LocationListIntent) {
        when (intent) {
            is LocationListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is LocationListIntent.SelectLocation -> navigator.navigateTo(Destination.LocationDetail(intent.id))
            is LocationListIntent.NavigateBack -> navigator.navigateBack()
            is LocationListIntent.Retry -> loadLocations()
        }
    }

    private fun loadLocations() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getLocationsByRegion(regionId) }
                .onSuccess { locations -> _state.update { it.copy(locations = locations, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 4: Update `LocationDetailState.kt`**

Replace `selectedGeneration: Int?` with `activeGeneration: GenerationEntry?`:

```kotlin
package dev.huntdex.feature.locations.detail

import dev.huntdex.core.domain.model.AreaEncounter
import dev.huntdex.core.domain.model.GenerationEntry
import dev.huntdex.core.domain.model.LocationDetail

data class LocationDetailState(
    val detail: LocationDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val expandedAreaIds: Set<Int> = emptySet(),
    val activeGeneration: GenerationEntry? = null
) {
    fun encountersForArea(areaId: Int): List<AreaEncounter> {
        val area = detail?.areas?.firstOrNull { it.id == areaId } ?: return emptyList()
        val gen = activeGeneration ?: return area.encounters
        return area.encounters.filter { encounter -> gen.versions.any { v -> encounter.version.contains(v) } }
    }
}
```

- [ ] **Step 5: Update `LocationDetailIntent.kt`**

Remove `FilterByGeneration`:

```kotlin
package dev.huntdex.feature.locations.detail

sealed interface LocationDetailIntent {
    data object NavigateBack : LocationDetailIntent
    data class ToggleArea(val areaId: Int) : LocationDetailIntent
    data object Retry : LocationDetailIntent
}
```

- [ ] **Step 6: Update `LocationDetailScreenModel.kt`**

Inject `GlobalFilterRepository`:

```kotlin
package dev.huntdex.feature.locations.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.GlobalFilterRepository
import dev.huntdex.core.domain.repository.LocationRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationDetailScreenModel(
    private val locationId: Int,
    private val repository: LocationRepository,
    private val filterRepository: GlobalFilterRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(LocationDetailState())
    val state: StateFlow<LocationDetailState> = _state.asStateFlow()

    init {
        filterRepository.selectedGeneration
            .onEach { gen -> _state.update { it.copy(activeGeneration = gen) } }
            .launchIn(scope)
        loadDetail()
    }

    fun onIntent(intent: LocationDetailIntent) {
        when (intent) {
            is LocationDetailIntent.NavigateBack -> navigator.navigateBack()
            is LocationDetailIntent.ToggleArea -> {
                val current = _state.value.expandedAreaIds
                val updated = if (intent.areaId in current) current - intent.areaId else current + intent.areaId
                _state.update { it.copy(expandedAreaIds = updated) }
            }
            is LocationDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getLocationDetail(locationId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 7: Update locations tests**

In both `LocationListScreenModelTest.kt` and `LocationDetailScreenModelTest.kt`:
- Add `FakeGlobalFilterRepository` (same class from games tests)
- Pass it to `LocationListScreenModel(regionId, repo, fakeFilter, navigator, scope)` and `LocationDetailScreenModel(id, repo, fakeFilter, navigator, scope)`
- Remove tests for `FilterByGeneration` intent (removed)
- Add test that active generation reflects filter repository

For `LocationDetailScreenModelTest.kt`, add:
```kotlin
@Test
fun `active generation filters encounters`() = testScope.runTest {
    val filter = FakeGlobalFilterRepository()
    val model = LocationDetailScreenModel(1, FakeLocationRepository(), filter, FakeAppNavigator(), this)
    dispatcher.scheduler.advanceUntilIdle()
    model.onIntent(LocationDetailIntent.ToggleArea(1))
    val gen = GenerationEntry(1, "generation-i", listOf("red", "blue"))
    filter.setGeneration(gen)
    dispatcher.scheduler.advanceUntilIdle()
    // FakeRepository returns encounter with version "red" which is in gen.versions
    val encounters = model.state.value.encountersForArea(1)
    assertTrue(encounters.isNotEmpty())
}
```

- [ ] **Step 8: Run locations tests**

```bash
./gradlew :feature:locations:desktopTest
```
Expected: All tests PASS

- [ ] **Step 9: Commit**

```bash
git add feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/ \
        feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/ \
        feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/list/ \
        feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/detail/
git commit -m "refactor(locations): inject GlobalFilterRepository, remove local generation filter state"
```

---

### Task 8: Wire all 3 entry points + DI for GlobalFilterRepository

- [ ] **Step 1: Add `feature:games` dependency to all 3 entry points**

In `app/build.gradle.kts` → `dependencies {}`: `implementation(projects.feature.games)`
In `desktopApp/build.gradle.kts` → `desktopMain.dependencies {}`: `implementation(projects.feature.games)`
In `shared/build.gradle.kts` → iosMain dependencies: `implementation(projects.feature.games)`

- [ ] **Step 2: Update DI modules — register `GenerationListScreenModel` + update existing factories**

In each of `AppModule.kt`, `DesktopAppModule.kt`, `IosAppModule.kt`:

```kotlin
import dev.huntdex.feature.games.GenerationListScreenModel
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.items.list.ItemListScreenModel
import dev.huntdex.feature.locations.list.LocationListScreenModel
import dev.huntdex.feature.locations.detail.LocationDetailScreenModel

// Inside module { }:

// Games
factory { GenerationListScreenModel(get(), get()) }

// Updated factory signatures — now include GlobalFilterRepository (get()):
factory { MoveListScreenModel(get(), get(), get()) }
factory { ItemListScreenModel(get(), get(), get()) }
factory { params -> LocationListScreenModel(params.get(), get(), get(), get()) }
factory { params -> LocationDetailScreenModel(params.get(), get(), get(), get()) }
```

- [ ] **Step 3: Update DestinationMappers**

Add to Android `DestinationMapper.kt`, Desktop `DesktopDestinationMapper.kt`, iOS `IosDestinationMapper.kt`:

```kotlin
import dev.huntdex.feature.games.GenerationListScreen

// Add case (before the else branch):
is Destination.GenerationList -> GenerationListScreen
```

- [ ] **Step 4: Build all targets**

```bash
./gradlew :app:assembleDebug :desktopApp:jar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Run full test suite**

```bash
./gradlew allTests
```
Expected: All tests PASS

- [ ] **Step 6: Commit and push**

```bash
git add app/build.gradle.kts app/src/main/java/dev/huntdex/app/ \
        desktopApp/build.gradle.kts desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/ \
        shared/build.gradle.kts shared/src/iosMain/kotlin/dev/huntdex/shared/
git commit -m "feat(games): wire GenerationListScreen and GlobalFilterRepository into all entry points"
git push origin phase-2/games
```
