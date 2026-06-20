# Phase 2 PR 1 — feature:moves Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Moves encyclopedia feature — paginated list with type/damage-class filters, full detail with contest data and "learned by" Pokémon list.

**Architecture:** MVI pattern identical to `feature:pokedex`. Domain models in `core:domain`, repository implementation in `core:data`, UI in `feature:moves`. All 3 entry points (Android `app`, Desktop `desktopApp`, iOS `shared`) wired at the end.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Voyager (navigation), Koin (DI), Ktor (HTTP), SQLDelight (cache), kotlinx.serialization, Coil (images).

## Global Constraints

- Kotlin 2.0.21, Compose Multiplatform 1.7.0
- Koin 3.5.6 (not 4.x — incompatible with voyager-koin 1.1.0-beta02)
- Coil 3.0.4 (pinned — 3.1.0 breaks Kotlin 2.0.21 KLIB ABI)
- coroutines 1.9.0
- All new domain models must be `@Serializable` (cached as JSON blobs)
- Target platforms: Android + Desktop + iOS (commonMain code only)
- Test runner: `./gradlew :feature:moves:desktopTest`

---

### Task 1: Domain models and repository interface

**Files:**
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveDetail.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveContestEffect.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LearnedByPokemon.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/MoveRepository.kt`

**Interfaces:**
- Produces: `MoveEntry`, `MoveDetail`, `MoveContestEffect`, `LearnedByPokemon`, `MoveRepository` — used by all subsequent tasks

- [ ] **Step 1: Create domain models**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveEntry(val id: Int, val name: String)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LearnedByPokemon.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LearnedByPokemon(val id: Int, val name: String, val learnMethods: List<String>)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveContestEffect.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveContestEffect(
    val contestType: String,
    val appeal: Int,
    val jam: Int,
    val effectEntry: String
)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveDetail.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class MoveDetail(
    val id: Int,
    val name: String,
    val type: String,
    val damageClass: String,
    val power: Int?,
    val accuracy: Int?,
    val pp: Int,
    val priority: Int,
    val effectEntry: String,
    val flavorText: String,
    val learnedBy: List<LearnedByPokemon>,
    val contestEffect: MoveContestEffect?
)
```

- [ ] **Step 2: Create repository interface**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/MoveRepository.kt`:
```kotlin
package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry

interface MoveRepository {
    suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry>
    suspend fun getMovesByType(typeName: String): List<MoveEntry>
    suspend fun getMovesByDamageClass(className: String): List<MoveEntry>
    suspend fun getMoveDetail(id: Int): MoveDetail
}
```

- [ ] **Step 3: Verify `core:domain` compiles**

```bash
./gradlew :core:domain:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveEntry.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveDetail.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/MoveContestEffect.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LearnedByPokemon.kt \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/MoveRepository.kt
git commit -m "feat(moves): add domain models and MoveRepository interface"
```

---

### Task 2: SQLDelight schema + desktop migration guard

**Files:**
- Modify: `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`
- Modify: `core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt`

**Interfaces:**
- Consumes: nothing (schema additions only)
- Produces: `huntdexDatabaseQueries.insertMoveEntry`, `selectMovePage`, `searchMoves`, `insertMoveDetail`, `selectMoveDetail`

- [ ] **Step 1: Add move tables to `HuntdexDatabase.sq`**

Append at the end of `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`:
```sql
-- ============================================================
-- Move List Cache
-- ============================================================
CREATE TABLE move_entry (
  id   INTEGER PRIMARY KEY,
  name TEXT    NOT NULL
);

insertMoveEntry:
INSERT OR IGNORE INTO move_entry (id, name) VALUES (?, ?);

selectMovePage:
SELECT id, name FROM move_entry ORDER BY id LIMIT :limit OFFSET :offset;

searchMoves:
SELECT id, name FROM move_entry WHERE name LIKE '%' || :query || '%' ORDER BY id;

-- ============================================================
-- Move Detail Cache (full object serialized as JSON)
-- ============================================================
CREATE TABLE move_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL
);

insertMoveDetail:
INSERT OR REPLACE INTO move_detail (id, data) VALUES (?, ?);

selectMoveDetail:
SELECT data FROM move_detail WHERE id = ?;
```

- [ ] **Step 2: Add migration guard in `DatabaseDriverFactory.kt`**

In `core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt`, add a `"move_entry" !in tables` branch inside the `when` block. The full file after the edit:
```kotlin
package dev.huntdex.core.data.db

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver

actual class DatabaseDriverFactory {
    actual fun createDriver(): SqlDriver {
        val driver = JdbcSqliteDriver("jdbc:sqlite:huntdex.db")

        val tables = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type='table' AND name NOT LIKE 'sqlite_%'",
            { cursor ->
                val list = mutableListOf<String>()
                while (cursor.next().value) { cursor.getString(0)?.let { list.add(it) } }
                QueryResult.Value(list)
            },
            0
        ).value

        when {
            tables.isEmpty() ->
                HuntdexDatabase.Schema.create(driver)
            "pokemon_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE pokemon_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, sprite_url TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE pokemon_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE move_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE move_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            "move_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE move_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE move_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            // else: all tables present, nothing to do
        }

        return driver
    }
}
```

- [ ] **Step 3: Verify `core:data` compiles**

```bash
./gradlew :core:data:generateSqlDelightInterface
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq \
        core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt
git commit -m "feat(moves): add move_entry + move_detail SQLDelight tables"
```

---

### Task 3: Network layer — DTOs, API, mapper, repository, DI

**Files:**
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveDetailDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveListResponseDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ContestEffectDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/TypeDetailDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/MoveApi.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/MoveMapper.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/MoveRepositoryImpl.kt`
- Modify: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt`

**Interfaces:**
- Consumes: `MoveRepository` (Task 1), SQLDelight queries (Task 2), `NamedApiResourceDto`, `ApiResourceDto`, `extractPokeApiId` (existing)
- Produces: `MoveRepositoryImpl` registered as `MoveRepository` in Koin

- [ ] **Step 1: Create DTOs**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveListResponseDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class MoveListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveDetailDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class MoveDetailDto(
    val id: Int,
    val name: String,
    val accuracy: Int?,
    val power: Int?,
    val pp: Int,
    val priority: Int,
    val type: NamedApiResourceDto,
    @SerialName("damage_class") val damageClass: NamedApiResourceDto,
    @SerialName("effect_entries") val effectEntries: List<MoveEffectEntryDto>,
    @SerialName("flavor_text_entries") val flavorTextEntries: List<MoveFlavorTextEntryDto>,
    @SerialName("learned_by_pokemon") val learnedByPokemon: List<MoveLearnedByDto>,
    @SerialName("contest_type") val contestType: NamedApiResourceDto?,
    @SerialName("contest_effect") val contestEffect: ApiResourceDto?,
    @SerialName("super_contest_effect") val superContestEffect: ApiResourceDto?
)

@Serializable
data class MoveEffectEntryDto(
    val effect: String,
    val language: NamedApiResourceDto
)

@Serializable
data class MoveFlavorTextEntryDto(
    @SerialName("flavor_text") val flavorText: String,
    val language: NamedApiResourceDto,
    @SerialName("version_group") val versionGroup: NamedApiResourceDto
)

@Serializable
data class MoveLearnedByDto(
    val pokemon: NamedApiResourceDto,
    @SerialName("version_details") val versionDetails: List<MoveVersionDetailDto>
)

@Serializable
data class MoveVersionDetailDto(
    @SerialName("level_learned_at") val levelLearnedAt: Int,
    @SerialName("move_learn_method") val moveLearnMethod: NamedApiResourceDto,
    @SerialName("version_group") val versionGroup: NamedApiResourceDto
)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ContestEffectDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ContestEffectDto(
    val id: Int,
    val appeal: Int,
    val jam: Int,
    @SerialName("effect_entries") val effectEntries: List<ContestEffectEntryDto>
)

@Serializable
data class ContestEffectEntryDto(val effect: String, val language: NamedApiResourceDto)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/TypeDetailDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class TypeDetailDto(val id: Int, val name: String, val moves: List<NamedApiResourceDto>)

@Serializable
data class MoveDamageClassDto(val id: Int, val name: String, val moves: List<NamedApiResourceDto>)
```

- [ ] **Step 2: Create `MoveApi.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/MoveApi.kt`:
```kotlin
package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.MoveListResponseDto
import dev.huntdex.core.data.network.dto.MoveDamageClassDto
import dev.huntdex.core.data.network.dto.TypeDetailDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val BASE_URL = "https://pokeapi.co/api/v2"

class MoveApi(private val client: HttpClient) {
    suspend fun getMoveList(limit: Int, offset: Int): MoveListResponseDto =
        client.get("$BASE_URL/move?limit=$limit&offset=$offset").body()

    suspend fun getMoveDetail(id: Int): MoveDetailDto =
        client.get("$BASE_URL/move/$id").body()

    suspend fun getContestEffect(id: Int): ContestEffectDto =
        client.get("$BASE_URL/contest-effect/$id").body()

    suspend fun getTypeDetail(typeName: String): TypeDetailDto =
        client.get("$BASE_URL/type/$typeName").body()

    suspend fun getDamageClassDetail(className: String): MoveDamageClassDto =
        client.get("$BASE_URL/move-damage-class/$className").body()
}
```

- [ ] **Step 3: Create `MoveMapper.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/MoveMapper.kt`:
```kotlin
package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.ContestEffectDto
import dev.huntdex.core.data.network.dto.MoveDetailDto
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail

fun toMoveDetail(dto: MoveDetailDto, contestEffectDto: ContestEffectDto?): MoveDetail {
    val effectEntry = dto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
    val flavorText = dto.flavorTextEntries
        .lastOrNull { it.language.name == "en" }
        ?.flavorText?.replace("\n", " ")?.replace("", " ") ?: ""

    val learnedBy = dto.learnedByPokemon.map { learned ->
        val pokemonId = learned.pokemon.url.extractPokeApiId()
        val methods = learned.versionDetails.map { it.moveLearnMethod.name }.distinct()
        LearnedByPokemon(pokemonId, learned.pokemon.name, methods)
    }.sortedBy { it.id }

    val contestEffect = if (dto.contestType != null && contestEffectDto != null) {
        val effectDesc = contestEffectDto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
        MoveContestEffect(
            contestType = dto.contestType.name,
            appeal = contestEffectDto.appeal,
            jam = contestEffectDto.jam,
            effectEntry = effectDesc
        )
    } else null

    return MoveDetail(
        id = dto.id,
        name = dto.name,
        type = dto.type.name,
        damageClass = dto.damageClass.name,
        power = dto.power,
        accuracy = dto.accuracy,
        pp = dto.pp,
        priority = dto.priority,
        effectEntry = effectEntry,
        flavorText = flavorText,
        learnedBy = learnedBy,
        contestEffect = contestEffect
    )
}
```

- [ ] **Step 4: Create `MoveRepositoryImpl.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/MoveRepositoryImpl.kt`:
```kotlin
package dev.huntdex.core.data.repository

import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.mapper.toMoveDetail
import dev.huntdex.core.data.network.MoveApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry
import dev.huntdex.core.domain.repository.MoveRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class MoveRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: MoveApi
) : MoveRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry> {
        val cached = queries.selectMovePage(limit.toLong(), offset.toLong()).executeAsList()
        if (cached.isNotEmpty()) return cached.map { MoveEntry(it.id.toInt(), it.name) }

        val response = api.getMoveList(limit, offset)
        response.results.forEach { result ->
            val id = result.url.extractPokeApiId()
            queries.insertMoveEntry(id.toLong(), result.name)
        }
        return queries.selectMovePage(limit.toLong(), offset.toLong()).executeAsList()
            .map { MoveEntry(it.id.toInt(), it.name) }
    }

    override suspend fun getMovesByType(typeName: String): List<MoveEntry> {
        val typeDetail = api.getTypeDetail(typeName)
        return typeDetail.moves
            .map { MoveEntry(it.url.extractPokeApiId(), it.name) }
            .sortedBy { it.id }
    }

    override suspend fun getMovesByDamageClass(className: String): List<MoveEntry> {
        val classDetail = api.getDamageClassDetail(className)
        return classDetail.moves
            .map { MoveEntry(it.url.extractPokeApiId(), it.name) }
            .sortedBy { it.id }
    }

    override suspend fun getMoveDetail(id: Int): MoveDetail {
        val cachedJson = queries.selectMoveDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) return Json.decodeFromString(cachedJson)

        val detail = coroutineScope {
            val moveDto = api.getMoveDetail(id)
            val contestEffectDeferred = moveDto.contestEffect?.url?.extractPokeApiId()
                ?.let { async { api.getContestEffect(it) } }
            val contestEffectDto = contestEffectDeferred?.await()
            toMoveDetail(moveDto, contestEffectDto)
        }

        queries.insertMoveDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }
}
```

- [ ] **Step 5: Register in `DataModule.kt`**

Add to `core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt`:
```kotlin
package dev.huntdex.core.data.di

import dev.huntdex.core.data.network.MoveApi
import dev.huntdex.core.data.network.PokemonApi
import dev.huntdex.core.data.network.buildHttpClient
import dev.huntdex.core.data.repository.MoveRepositoryImpl
import dev.huntdex.core.data.repository.PokemonRepositoryImpl
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.domain.repository.PokemonRepository
import org.koin.dsl.module

val dataModule = module {
    single { buildHttpClient() }
    single { PokemonApi(get()) }
    single { MoveApi(get()) }
    single<PokemonRepository> { PokemonRepositoryImpl(get(), get()) }
    single<MoveRepository> { MoveRepositoryImpl(get(), get()) }
}
```

- [ ] **Step 6: Verify `core:data` compiles**

```bash
./gradlew :core:data:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveListResponseDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/MoveDetailDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ContestEffectDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/TypeDetailDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/MoveApi.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/MoveMapper.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/MoveRepositoryImpl.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt
git commit -m "feat(moves): network layer — DTOs, MoveApi, mapper, repository"
```

---

### Task 4: MoveListScreenModel + tests

**Files:**
- Modify: `feature/moves/build.gradle.kts`
- Delete: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/Placeholder.kt`
- Delete: `feature/moves/src/desktopMain/kotlin/dev/huntdex/feature/moves/DesktopStub.kt`
- Delete: `feature/moves/src/iosMain/kotlin/dev/huntdex/feature/moves/IosStub.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListState.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListIntent.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModel.kt`
- Create: `feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/FakeMoveRepository.kt`
- Create: `feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModelTest.kt`

**Interfaces:**
- Consumes: `MoveEntry`, `MoveRepository` (Task 1)
- Produces: `MoveListScreenModel(repository, navigator, externalScope?)`, `MoveListState`, `MoveListIntent`

- [ ] **Step 1: Upgrade `feature/moves/build.gradle.kts`**

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
            implementation(libs.coil.compose)
            implementation(libs.coil.network.ktor)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.coroutines.test)
        }
    }
}

android {
    namespace = "dev.huntdex.feature.moves"
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
rm feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/Placeholder.kt
rm feature/moves/src/desktopMain/kotlin/dev/huntdex/feature/moves/DesktopStub.kt
rm feature/moves/src/iosMain/kotlin/dev/huntdex/feature/moves/IosStub.kt
```

- [ ] **Step 3: Create `MoveListState.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListState.kt`:
```kotlin
package dev.huntdex.feature.moves.list

import dev.huntdex.core.domain.model.MoveEntry

data class MoveListState(
    val moves: List<MoveEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedType: String? = null,
    val selectedDamageClass: String? = null,
    val selectedGeneration: Int? = null,
    val currentOffset: Int = 0,
    val hasMore: Boolean = true
) {
    val displayedMoves: List<MoveEntry>
        get() = if (searchQuery.isBlank()) moves
                else moves.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

- [ ] **Step 4: Create `MoveListIntent.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListIntent.kt`:
```kotlin
package dev.huntdex.feature.moves.list

sealed interface MoveListIntent {
    data object LoadNextPage : MoveListIntent
    data class Search(val query: String) : MoveListIntent
    data class FilterByType(val typeName: String?) : MoveListIntent
    data class FilterByDamageClass(val className: String?) : MoveListIntent
    data class FilterByGeneration(val generationId: Int?) : MoveListIntent
    data class SelectMove(val id: Int) : MoveListIntent
    data object Retry : MoveListIntent
}
```

- [ ] **Step 5: Write failing tests**

`feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/FakeMoveRepository.kt`:
```kotlin
package dev.huntdex.feature.moves

import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail
import dev.huntdex.core.domain.model.MoveEntry
import dev.huntdex.core.domain.repository.MoveRepository

class FakeMoveRepository : MoveRepository {
    val entries = (1..30).map { id -> MoveEntry(id, "move-$id") }

    override suspend fun getMovePage(limit: Int, offset: Int): List<MoveEntry> =
        entries.drop(offset).take(limit)

    override suspend fun getMovesByType(typeName: String): List<MoveEntry> = entries.take(5)

    override suspend fun getMovesByDamageClass(className: String): List<MoveEntry> = entries.take(3)

    override suspend fun getMoveDetail(id: Int): MoveDetail = MoveDetail(
        id = id, name = "move-$id", type = "normal", damageClass = "physical",
        power = 40, accuracy = 100, pp = 35, priority = 0,
        effectEntry = "Does damage.", flavorText = "A test move.",
        learnedBy = (1..15).map { LearnedByPokemon(it, "pokemon-$it", listOf("level-up")) },
        contestEffect = MoveContestEffect("tough", 2, 1, "Startles the foe.")
    )
}
```

`feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModelTest.kt`:
```kotlin
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
```

- [ ] **Step 6: Run tests to verify they fail**

```bash
./gradlew :feature:moves:desktopTest
```
Expected: FAIL — `MoveListScreenModel` not defined yet

- [ ] **Step 7: Create `MoveListScreenModel.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreenModel.kt`:
```kotlin
package dev.huntdex.feature.moves.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class MoveListScreenModel(
    private val repository: MoveRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(MoveListState())
    val state: StateFlow<MoveListState> = _state.asStateFlow()

    init { loadFirstPage() }

    fun onIntent(intent: MoveListIntent) {
        when (intent) {
            is MoveListIntent.LoadNextPage -> loadNextPage()
            is MoveListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is MoveListIntent.FilterByType -> applyTypeFilter(intent.typeName)
            is MoveListIntent.FilterByDamageClass -> applyDamageClassFilter(intent.className)
            is MoveListIntent.FilterByGeneration -> _state.update { it.copy(selectedGeneration = intent.generationId) }
            is MoveListIntent.SelectMove -> navigator.navigateTo(Destination.MoveDetail(intent.id))
            is MoveListIntent.Retry -> when {
                _state.value.selectedType != null -> applyTypeFilter(_state.value.selectedType)
                _state.value.selectedDamageClass != null -> applyDamageClassFilter(_state.value.selectedDamageClass)
                else -> loadFirstPage()
            }
        }
    }

    private fun loadFirstPage() {
        val query = _state.value.searchQuery
        _state.update { MoveListState(isLoading = true, searchQuery = query) }
        scope.launch {
            runCatching { repository.getMovePage(PAGE_SIZE, 0) }
                .onSuccess { entries ->
                    _state.update { it.copy(moves = entries, isLoading = false, currentOffset = entries.size, hasMore = entries.size == PAGE_SIZE) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.selectedType != null || current.selectedDamageClass != null) return
        _state.update { it.copy(isLoadingMore = true) }
        scope.launch {
            runCatching { repository.getMovePage(PAGE_SIZE, current.currentOffset) }
                .onSuccess { entries ->
                    _state.update { it.copy(moves = it.moves + entries, isLoadingMore = false, currentOffset = it.currentOffset + entries.size, hasMore = entries.size == PAGE_SIZE) }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.message) } }
        }
    }

    private fun applyTypeFilter(typeName: String?) {
        _state.update { it.copy(selectedType = typeName, selectedDamageClass = null, isLoading = true, moves = emptyList()) }
        if (typeName == null) { loadFirstPage(); return }
        scope.launch {
            runCatching { repository.getMovesByType(typeName) }
                .onSuccess { entries -> _state.update { it.copy(moves = entries, isLoading = false, hasMore = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun applyDamageClassFilter(className: String?) {
        _state.update { it.copy(selectedDamageClass = className, selectedType = null, isLoading = true, moves = emptyList()) }
        if (className == null) { loadFirstPage(); return }
        scope.launch {
            runCatching { repository.getMovesByDamageClass(className) }
                .onSuccess { entries -> _state.update { it.copy(moves = entries, isLoading = false, hasMore = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 8: Run tests to verify they pass**

```bash
./gradlew :feature:moves:desktopTest
```
Expected: All 7 tests PASS

- [ ] **Step 9: Commit**

```bash
git add feature/moves/build.gradle.kts \
        feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/ \
        feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/
git commit -m "feat(moves): MoveListScreenModel with search, type/damage-class filters"
```

---

### Task 5: MoveDetailScreenModel + tests

**Files:**
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailState.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailIntent.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreenModel.kt`
- Create: `feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreenModelTest.kt`

**Interfaces:**
- Consumes: `MoveDetail`, `MoveRepository` (Task 1), `FakeMoveRepository` (Task 4)
- Produces: `MoveDetailScreenModel(moveId, repository, navigator, externalScope?)`, `MoveDetailState`

- [ ] **Step 1: Create `MoveDetailState.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailState.kt`:
```kotlin
package dev.huntdex.feature.moves.detail

import dev.huntdex.core.domain.model.LearnedByPokemon
import dev.huntdex.core.domain.model.MoveDetail

data class MoveDetailState(
    val detail: MoveDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val isLearnedByExpanded: Boolean = false
) {
    val learnedByVisible: List<LearnedByPokemon>
        get() = if (isLearnedByExpanded) detail?.learnedBy ?: emptyList()
                else (detail?.learnedBy ?: emptyList()).take(10)
    val hasMoreLearnedBy: Boolean
        get() = (detail?.learnedBy?.size ?: 0) > 10
}
```

- [ ] **Step 2: Create `MoveDetailIntent.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailIntent.kt`:
```kotlin
package dev.huntdex.feature.moves.detail

sealed interface MoveDetailIntent {
    data object NavigateBack : MoveDetailIntent
    data object ExpandLearnedBy : MoveDetailIntent
    data object Retry : MoveDetailIntent
}
```

- [ ] **Step 3: Write failing tests**

`feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreenModelTest.kt`:
```kotlin
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
        assertTrue(model.state.value.isLearnedByExpanded)
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
```

- [ ] **Step 4: Run tests to verify they fail**

```bash
./gradlew :feature:moves:desktopTest
```
Expected: FAIL — `MoveDetailScreenModel` not defined yet

- [ ] **Step 5: Create `MoveDetailScreenModel.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreenModel.kt`:
```kotlin
package dev.huntdex.feature.moves.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.MoveRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MoveDetailScreenModel(
    private val moveId: Int,
    private val repository: MoveRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(MoveDetailState())
    val state: StateFlow<MoveDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onIntent(intent: MoveDetailIntent) {
        when (intent) {
            is MoveDetailIntent.NavigateBack -> navigator.navigateBack()
            is MoveDetailIntent.ExpandLearnedBy -> _state.update { it.copy(isLearnedByExpanded = true) }
            is MoveDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getMoveDetail(moveId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 6: Run all tests**

```bash
./gradlew :feature:moves:desktopTest
```
Expected: All 13 tests PASS

- [ ] **Step 7: Commit**

```bash
git add feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/ \
        feature/moves/src/commonTest/kotlin/dev/huntdex/feature/moves/detail/
git commit -m "feat(moves): MoveDetailScreenModel with expand learned-by and contest data"
```

---

### Task 6: UI screens

**Files:**
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreen.kt`
- Create: `feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreen.kt`

**Interfaces:**
- Consumes: `MoveListState`, `MoveListIntent`, `MoveListScreenModel` (Task 4); `MoveDetailState`, `MoveDetailIntent`, `MoveDetailScreenModel` (Task 5)
- Produces: `MoveListScreen` (data object), `MoveDetailScreen(id: Int)` (data class)

- [ ] **Step 1: Create `MoveListScreen.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreen.kt`:
```kotlin
package dev.huntdex.feature.moves.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.domain.model.MoveEntry

data object MoveListScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<MoveListScreenModel>()
        val state by model.state.collectAsState()
        MoveListContent(state = state, onIntent = model::onIntent)
    }
}

@Composable
private fun MoveListContent(state: MoveListState, onIntent: (MoveListIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onIntent(MoveListIntent.Search(it)) },
            label = { Text("Search moves") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        val types = listOf("normal","fire","water","grass","electric","ice","fighting","poison",
            "ground","flying","psychic","bug","rock","ghost","dragon","dark","steel","fairy")
        val damageClasses = listOf("physical", "special", "status")

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(types) { type ->
                FilterChip(
                    selected = state.selectedType == type,
                    onClick = { onIntent(MoveListIntent.FilterByType(if (state.selectedType == type) null else type)) },
                    label = { Text(type.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        Spacer(Modifier.height(4.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(damageClasses) { cls ->
                FilterChip(
                    selected = state.selectedDamageClass == cls,
                    onClick = { onIntent(MoveListIntent.FilterByDamageClass(if (state.selectedDamageClass == cls) null else cls)) },
                    label = { Text(cls.replaceFirstChar { it.uppercase() }) }
                )
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onIntent(MoveListIntent.Retry) }) { Text("Retry") }
                }
            }
            else -> MoveList(state = state, onIntent = onIntent)
        }
    }
}

@Composable
private fun MoveList(state: MoveListState, onIntent: (MoveListIntent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(state.displayedMoves) { index, move ->
            if (index == state.displayedMoves.size - 1 && state.hasMore) {
                onIntent(MoveListIntent.LoadNextPage)
            }
            MoveRow(move = move, onClick = { onIntent(MoveListIntent.SelectMove(move.id)) })
            HorizontalDivider()
        }
        if (state.isLoadingMore) {
            item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
    }
}

@Composable
private fun MoveRow(move: MoveEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(move.name.replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Create `MoveDetailScreen.kt`**

`feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreen.kt`:
```kotlin
package dev.huntdex.feature.moves.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.MoveContestEffect
import dev.huntdex.core.domain.model.MoveDetail
import org.koin.core.parameter.parametersOf

data class MoveDetailScreen(val id: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<MoveDetailScreenModel> { parametersOf(id) }
        val state by model.state.collectAsState()
        MoveDetailContent(state = state, onIntent = model::onIntent)
    }
}

@Composable
private fun MoveDetailContent(state: MoveDetailState, onIntent: (MoveDetailIntent) -> Unit) {
    when {
        state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
        state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(state.error!!)
                Spacer(Modifier.height(8.dp))
                Button(onClick = { onIntent(MoveDetailIntent.Retry) }) { Text("Retry") }
            }
        }
        state.detail != null -> MoveDetailLoaded(state = state, detail = state.detail, onIntent = onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MoveDetailLoaded(state: MoveDetailState, detail: MoveDetail, onIntent: (MoveDetailIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.name.replaceFirstChar { it.uppercase() }) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(MoveDetailIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Stats section
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Stats", style = MaterialTheme.typography.titleMedium)
                        StatRow("Type", detail.type.replaceFirstChar { it.uppercase() })
                        StatRow("Category", detail.damageClass.replaceFirstChar { it.uppercase() })
                        StatRow("Power", detail.power?.toString() ?: "—")
                        StatRow("Accuracy", detail.accuracy?.let { "$it%" } ?: "—")
                        StatRow("PP", detail.pp.toString())
                        StatRow("Priority", detail.priority.toString())
                        if (detail.effectEntry.isNotBlank()) {
                            Spacer(Modifier.height(4.dp))
                            Text(detail.effectEntry, style = MaterialTheme.typography.bodyMedium)
                        }
                        if (detail.flavorText.isNotBlank()) {
                            Text(detail.flavorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // Learned by section
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp)) {
                        Text("Learned by Pokémon", style = MaterialTheme.typography.titleMedium)
                        Spacer(Modifier.height(8.dp))
                        state.learnedByVisible.forEach { pokemon ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${pokemon.id}.png",
                                    contentDescription = pokemon.name,
                                    modifier = Modifier.size(40.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(pokemon.name.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
                                    Text(pokemon.learnMethods.joinToString(", "), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                        if (state.hasMoreLearnedBy && !state.isLearnedByExpanded) {
                            Spacer(Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { onIntent(MoveDetailIntent.ExpandLearnedBy) },
                                modifier = Modifier.fillMaxWidth()
                            ) { Text("Ver más (${detail.learnedBy.size - 10} more)") }
                        }
                    }
                }
            }

            // Contest section (only shown if move has contest data)
            if (detail.contestEffect != null) {
                item { ContestSection(contestEffect = detail.contestEffect) }
            }
        }
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun ContestSection(contestEffect: MoveContestEffect) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Contest", style = MaterialTheme.typography.titleMedium)
            StatRow("Type", contestEffect.contestType.replaceFirstChar { it.uppercase() })
            StatRow("Appeal", contestEffect.appeal.toString())
            StatRow("Jam", contestEffect.jam.toString())
            if (contestEffect.effectEntry.isNotBlank()) {
                Text(contestEffect.effectEntry, style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
```

- [ ] **Step 3: Verify compilation**

```bash
./gradlew :feature:moves:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreen.kt \
        feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreen.kt
git commit -m "feat(moves): MoveListScreen and MoveDetailScreen UI"
```

---

### Task 7: Wire all 3 entry points

**Files:**
- Modify: `app/build.gradle.kts`
- Modify: `app/src/main/java/dev/huntdex/app/di/AppModule.kt`
- Modify: `app/src/main/java/dev/huntdex/app/navigation/DestinationMapper.kt`
- Modify: `desktopApp/build.gradle.kts`
- Modify: `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/di/DesktopAppModule.kt`
- Modify: `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/navigation/DesktopDestinationMapper.kt`
- Modify: `shared/build.gradle.kts`
- Modify: `shared/src/iosMain/kotlin/dev/huntdex/shared/di/IosAppModule.kt`
- Modify: `shared/src/iosMain/kotlin/dev/huntdex/shared/navigation/IosDestinationMapper.kt`

**Interfaces:**
- Consumes: `MoveListScreen`, `MoveDetailScreen` (Task 6); `MoveListScreenModel`, `MoveDetailScreenModel` (Tasks 4-5)
- Produces: app builds and runs with Moves feature navigable

- [ ] **Step 1: Add `feature:moves` dependency to all 3 entry points**

In `app/build.gradle.kts`, add inside `dependencies {}`:
```kotlin
implementation(projects.feature.moves)
```

In `desktopApp/build.gradle.kts`, add inside `desktopMain.dependencies {}`:
```kotlin
implementation(projects.feature.moves)
```

In `shared/build.gradle.kts`, add inside `commonMain.dependencies {}` (or `iosMain.dependencies {}`):
```kotlin
implementation(projects.feature.moves)
```

- [ ] **Step 2: Register ScreenModels in Android `AppModule.kt`**

Add to `app/src/main/java/dev/huntdex/app/di/AppModule.kt`:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.moves.detail.MoveDetailScreenModel

// Inside the module { } block:
factory { MoveListScreenModel(get(), get()) }
factory { params -> MoveDetailScreenModel(params.get(), get(), get()) }
```

- [ ] **Step 3: Register ScreenModels in Desktop `DesktopAppModule.kt`**

Add to `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/di/DesktopAppModule.kt`:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.moves.detail.MoveDetailScreenModel

// Inside the module { } block:
factory { MoveListScreenModel(get(), get()) }
factory { params -> MoveDetailScreenModel(params.get(), get(), get()) }
```

- [ ] **Step 4: Register ScreenModels in iOS `IosAppModule.kt`**

Add to `shared/src/iosMain/kotlin/dev/huntdex/shared/di/IosAppModule.kt`:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreenModel
import dev.huntdex.feature.moves.detail.MoveDetailScreenModel

// Inside the module { } block:
factory { MoveListScreenModel(get(), get()) }
factory { params -> MoveDetailScreenModel(params.get(), get(), get()) }
```

- [ ] **Step 5: Update Android `DestinationMapper.kt`**

In `app/src/main/java/dev/huntdex/app/navigation/DestinationMapper.kt`, add cases before the `else` branch:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreen
import dev.huntdex.feature.moves.detail.MoveDetailScreen

fun Destination.toScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    is Destination.MoveList -> MoveListScreen
    is Destination.MoveDetail -> MoveDetailScreen(id)
    else -> PlaceholderScreen(this::class.simpleName ?: "Unknown")
}
```

- [ ] **Step 6: Update Desktop `DesktopDestinationMapper.kt`**

In `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/navigation/DesktopDestinationMapper.kt`:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreen
import dev.huntdex.feature.moves.detail.MoveDetailScreen

fun Destination.toDesktopScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    is Destination.MoveList -> MoveListScreen
    is Destination.MoveDetail -> MoveDetailScreen(id)
    else -> DesktopPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
```

- [ ] **Step 7: Update iOS `IosDestinationMapper.kt`**

In `shared/src/iosMain/kotlin/dev/huntdex/shared/navigation/IosDestinationMapper.kt`:
```kotlin
import dev.huntdex.feature.moves.list.MoveListScreen
import dev.huntdex.feature.moves.detail.MoveDetailScreen

fun Destination.toIosScreen(): Screen = when (this) {
    is Destination.PokemonList -> PokemonListScreen
    is Destination.PokemonDetail -> PokemonDetailScreen(id)
    is Destination.MoveList -> MoveListScreen
    is Destination.MoveDetail -> MoveDetailScreen(id)
    else -> IosPlaceholderScreen(this::class.simpleName ?: "Unknown")
}
```

- [ ] **Step 8: Build all targets**

```bash
./gradlew :app:assembleDebug :desktopApp:jar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 9: Commit and push**

```bash
git add app/build.gradle.kts app/src/main/java/dev/huntdex/app/ \
        desktopApp/build.gradle.kts desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/ \
        shared/build.gradle.kts shared/src/iosMain/kotlin/dev/huntdex/shared/
git commit -m "feat(moves): wire feature into Android, Desktop, and iOS entry points"
git push origin phase-2/moves
```
