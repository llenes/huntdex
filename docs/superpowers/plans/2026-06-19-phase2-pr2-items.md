# Phase 2 PR 2 — feature:items Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Items encyclopedia feature — two-tab list (Objects by pocket / Berries), full detail screen for both, with offline cache.

**Architecture:** MVI pattern identical to `feature:pokedex`. Domain models in `core:domain`, repository in `core:data`, UI in `feature:items`. The `Destination` sealed class gains a new `BerryDetail(id)` entry. All 3 entry points wired at the end.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Voyager, Koin, Ktor, SQLDelight, kotlinx.serialization, Coil.

## Global Constraints

- Kotlin 2.0.21, Compose Multiplatform 1.7.0
- Koin 3.5.6, Coil 3.0.4, coroutines 1.9.0
- All new domain models must be `@Serializable`
- Target platforms: Android + Desktop + iOS (commonMain code only)
- Test runner: `./gradlew :feature:items:desktopTest`

---

### Task 1: Domain models, navigation destination, and repository interface

**Files:**
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ItemEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ItemDetail.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryDetail.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryFlavor.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/ItemRepository.kt`
- Modify: `core/navigation/src/commonMain/kotlin/dev/huntdex/core/navigation/Destination.kt`

**Interfaces:**
- Produces: `ItemEntry`, `ItemDetail`, `BerryEntry`, `BerryDetail`, `BerryFlavor`, `ItemRepository`, `Destination.BerryDetail`

- [ ] **Step 1: Create domain models**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ItemEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemEntry(val id: Int, val name: String)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BerryEntry(val id: Int, val name: String)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryFlavor.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BerryFlavor(val flavor: String, val potency: Int)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ItemDetail.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class ItemDetail(
    val id: Int,
    val name: String,
    val spriteUrl: String?,
    val category: String,
    val pocket: String,
    val effectEntry: String,
    val flavorText: String,
    val flingPower: Int?,
    val flingEffect: String?
)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/BerryDetail.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class BerryDetail(
    val id: Int,
    val name: String,
    val itemName: String,
    val spriteUrl: String?,
    val growthTime: Int,
    val maxHarvest: Int,
    val naturalGiftPower: Int,
    val size: Int,
    val smoothness: Int,
    val soilDryness: Int,
    val flavors: List<BerryFlavor>,
    val effectEntry: String,
    val flavorText: String
)
```

- [ ] **Step 2: Add `BerryDetail` destination**

In `core/navigation/src/commonMain/kotlin/dev/huntdex/core/navigation/Destination.kt`, add `BerryDetail`:
```kotlin
package dev.huntdex.core.navigation

sealed class Destination {
    data object PokemonList : Destination()
    data class PokemonDetail(val id: Int) : Destination()
    data object MoveList : Destination()
    data class MoveDetail(val id: Int) : Destination()
    data object ItemList : Destination()
    data class ItemDetail(val id: Int) : Destination()
    data class BerryDetail(val id: Int) : Destination()
    data object RegionList : Destination()
    data class LocationDetail(val id: Int) : Destination()
    data object GenerationList : Destination()
    data object HuntingList : Destination()
    data class HuntingSessionDetail(val sessionId: String) : Destination()
    data class NewHuntingSession(val pokemonId: Int? = null) : Destination()
    data object Profile : Destination()
}
```

- [ ] **Step 3: Create repository interface**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/ItemRepository.kt`:
```kotlin
package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.BerryDetail
import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.ItemDetail
import dev.huntdex.core.domain.model.ItemEntry

interface ItemRepository {
    suspend fun getItemPage(limit: Int, offset: Int): List<ItemEntry>
    suspend fun getItemsByPocket(pocketName: String): List<ItemEntry>
    suspend fun getItemDetail(id: Int): ItemDetail
    suspend fun getAllBerries(): List<BerryEntry>
    suspend fun getBerryDetail(id: Int): BerryDetail
}
```

- [ ] **Step 4: Verify `core:domain` compiles**

```bash
./gradlew :core:domain:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/ItemRepository.kt \
        core/navigation/src/commonMain/kotlin/dev/huntdex/core/navigation/Destination.kt
git commit -m "feat(items): domain models, BerryDetail destination, ItemRepository interface"
```

---

### Task 2: SQLDelight schema + desktop migration guard

**Files:**
- Modify: `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`
- Modify: `core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt`

- [ ] **Step 1: Add item and berry tables to `HuntdexDatabase.sq`**

Append to `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`:
```sql
-- ============================================================
-- Item List Cache
-- ============================================================
CREATE TABLE item_entry (
  id   INTEGER PRIMARY KEY,
  name TEXT    NOT NULL
);

insertItemEntry:
INSERT OR IGNORE INTO item_entry (id, name) VALUES (?, ?);

selectItemPage:
SELECT id, name FROM item_entry ORDER BY id LIMIT :limit OFFSET :offset;

-- ============================================================
-- Item Detail Cache
-- ============================================================
CREATE TABLE item_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL
);

insertItemDetail:
INSERT OR REPLACE INTO item_detail (id, data) VALUES (?, ?);

selectItemDetail:
SELECT data FROM item_detail WHERE id = ?;

-- ============================================================
-- Berry Detail Cache
-- ============================================================
CREATE TABLE berry_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL
);

insertBerryDetail:
INSERT OR REPLACE INTO berry_detail (id, data) VALUES (?, ?);

selectBerryDetail:
SELECT data FROM berry_detail WHERE id = ?;
```

- [ ] **Step 2: Add migration guard in `DatabaseDriverFactory.kt`**

Add `"item_entry" !in tables` branch to the `when` block. Full file:
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
                driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            "move_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE move_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE move_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            "item_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            // else: all tables present, nothing to do
        }

        return driver
    }
}
```

- [ ] **Step 3: Verify schema generation**

```bash
./gradlew :core:data:generateSqlDelightInterface
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq \
        core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt
git commit -m "feat(items): add item_entry, item_detail, berry_detail SQLDelight tables"
```

---

### Task 3: Network layer — DTOs, API, mapper, repository, DI

**Files:**
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemDetailDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/BerryDetailDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemPocketDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/ItemApi.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/ItemMapper.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/ItemRepositoryImpl.kt`
- Modify: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt`

- [ ] **Step 1: Create DTOs**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemDetailDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ItemListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)

@Serializable
data class ItemDetailDto(
    val id: Int,
    val name: String,
    val sprites: ItemSpritesDto,
    val category: ItemCategoryDto,
    @SerialName("effect_entries") val effectEntries: List<ItemEffectEntryDto>,
    @SerialName("flavor_text_entries") val flavorTextEntries: List<ItemFlavorTextDto>,
    @SerialName("fling_power") val flingPower: Int?,
    @SerialName("fling_effect") val flingEffect: NamedApiResourceDto?
)

@Serializable
data class ItemSpritesDto(@SerialName("default") val default: String?)

@Serializable
data class ItemCategoryDto(val name: String, val url: String)

@Serializable
data class ItemEffectEntryDto(val effect: String, val language: NamedApiResourceDto)

@Serializable
data class ItemFlavorTextDto(
    @SerialName("text") val text: String,
    val language: NamedApiResourceDto,
    @SerialName("version_group") val versionGroup: NamedApiResourceDto
)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/BerryDetailDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class BerryListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)

@Serializable
data class BerryDetailDto(
    val id: Int,
    val name: String,
    val item: NamedApiResourceDto,
    @SerialName("growth_time") val growthTime: Int,
    @SerialName("max_harvest") val maxHarvest: Int,
    @SerialName("natural_gift_power") val naturalGiftPower: Int,
    val size: Int,
    val smoothness: Int,
    @SerialName("soil_dryness") val soilDryness: Int,
    val flavors: List<BerryFlavorDto>
)

@Serializable
data class BerryFlavorDto(
    val potency: Int,
    val flavor: NamedApiResourceDto
)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemPocketDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class ItemPocketDto(
    val id: Int,
    val name: String,
    val categories: List<NamedApiResourceDto>
)

@Serializable
data class ItemCategoryDetailDto(
    val id: Int,
    val name: String,
    val items: List<NamedApiResourceDto>,
    val pocket: NamedApiResourceDto
)
```

- [ ] **Step 2: Create `ItemApi.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/ItemApi.kt`:
```kotlin
package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.BerryDetailDto
import dev.huntdex.core.data.network.dto.BerryListResponseDto
import dev.huntdex.core.data.network.dto.ItemCategoryDetailDto
import dev.huntdex.core.data.network.dto.ItemDetailDto
import dev.huntdex.core.data.network.dto.ItemListResponseDto
import dev.huntdex.core.data.network.dto.ItemPocketDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val BASE_URL = "https://pokeapi.co/api/v2"

class ItemApi(private val client: HttpClient) {
    suspend fun getItemList(limit: Int, offset: Int): ItemListResponseDto =
        client.get("$BASE_URL/item?limit=$limit&offset=$offset").body()

    suspend fun getItemDetail(id: Int): ItemDetailDto =
        client.get("$BASE_URL/item/$id").body()

    suspend fun getItemPocket(pocketName: String): ItemPocketDto =
        client.get("$BASE_URL/item-pocket/$pocketName").body()

    suspend fun getItemCategory(categoryName: String): ItemCategoryDetailDto =
        client.get("$BASE_URL/item-category/$categoryName").body()

    suspend fun getBerryList(): BerryListResponseDto =
        client.get("$BASE_URL/berry?limit=100").body()

    suspend fun getBerryDetail(id: Int): BerryDetailDto =
        client.get("$BASE_URL/berry/$id").body()

    suspend fun getItemDetailByName(name: String): ItemDetailDto =
        client.get("$BASE_URL/item/$name").body()
}
```

- [ ] **Step 3: Create `ItemMapper.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/ItemMapper.kt`:
```kotlin
package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.BerryDetailDto
import dev.huntdex.core.data.network.dto.ItemDetailDto
import dev.huntdex.core.domain.model.BerryDetail
import dev.huntdex.core.domain.model.BerryFlavor
import dev.huntdex.core.domain.model.ItemDetail

fun toItemDetail(dto: ItemDetailDto): ItemDetail {
    val effectEntry = dto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
    val flavorText = dto.flavorTextEntries
        .lastOrNull { it.language.name == "en" }
        ?.text?.replace("\n", " ") ?: ""
    val pocket = dto.category.name.substringAfterLast('-').let {
        // category names like "medicine", "battle-items" — pocket is resolved from the category detail
        dto.category.name
    }

    return ItemDetail(
        id = dto.id,
        name = dto.name,
        spriteUrl = dto.sprites.default,
        category = dto.category.name,
        pocket = pocket,
        effectEntry = effectEntry,
        flavorText = flavorText,
        flingPower = dto.flingPower,
        flingEffect = dto.flingEffect?.name
    )
}

fun toBerryDetail(berryDto: BerryDetailDto, itemDto: ItemDetailDto): BerryDetail {
    val effectEntry = itemDto.effectEntries.firstOrNull { it.language.name == "en" }?.effect ?: ""
    val flavorText = itemDto.flavorTextEntries
        .lastOrNull { it.language.name == "en" }
        ?.text?.replace("\n", " ") ?: ""

    return BerryDetail(
        id = berryDto.id,
        name = berryDto.name,
        itemName = berryDto.item.name,
        spriteUrl = itemDto.sprites.default,
        growthTime = berryDto.growthTime,
        maxHarvest = berryDto.maxHarvest,
        naturalGiftPower = berryDto.naturalGiftPower,
        size = berryDto.size,
        smoothness = berryDto.smoothness,
        soilDryness = berryDto.soilDryness,
        flavors = berryDto.flavors
            .filter { it.potency > 0 }
            .map { BerryFlavor(it.flavor.name, it.potency) },
        effectEntry = effectEntry,
        flavorText = flavorText
    )
}
```

- [ ] **Step 4: Create `ItemRepositoryImpl.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/ItemRepositoryImpl.kt`:
```kotlin
package dev.huntdex.core.data.repository

import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.mapper.toBerryDetail
import dev.huntdex.core.data.mapper.toItemDetail
import dev.huntdex.core.data.network.ItemApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.BerryDetail
import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.ItemDetail
import dev.huntdex.core.domain.model.ItemEntry
import dev.huntdex.core.domain.repository.ItemRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class ItemRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: ItemApi
) : ItemRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getItemPage(limit: Int, offset: Int): List<ItemEntry> {
        val cached = queries.selectItemPage(limit.toLong(), offset.toLong()).executeAsList()
        if (cached.isNotEmpty()) return cached.map { ItemEntry(it.id.toInt(), it.name) }

        val response = api.getItemList(limit, offset)
        response.results.forEach { result ->
            val id = result.url.extractPokeApiId()
            queries.insertItemEntry(id.toLong(), result.name)
        }
        return queries.selectItemPage(limit.toLong(), offset.toLong()).executeAsList()
            .map { ItemEntry(it.id.toInt(), it.name) }
    }

    override suspend fun getItemsByPocket(pocketName: String): List<ItemEntry> = coroutineScope {
        val pocket = api.getItemPocket(pocketName)
        pocket.categories
            .map { async { api.getItemCategory(it.name) } }
            .awaitAll()
            .flatMap { category -> category.items.map { ItemEntry(it.url.extractPokeApiId(), it.name) } }
            .sortedBy { it.id }
    }

    override suspend fun getItemDetail(id: Int): ItemDetail {
        val cachedJson = queries.selectItemDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) return Json.decodeFromString(cachedJson)

        val detail = toItemDetail(api.getItemDetail(id))
        queries.insertItemDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }

    override suspend fun getAllBerries(): List<BerryEntry> {
        val response = api.getBerryList()
        return response.results.map { BerryEntry(it.url.extractPokeApiId(), it.name) }.sortedBy { it.id }
    }

    override suspend fun getBerryDetail(id: Int): BerryDetail {
        val cachedJson = queries.selectBerryDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) return Json.decodeFromString(cachedJson)

        val detail = coroutineScope {
            val berryDeferred = async { api.getBerryDetail(id) }
            val berryDto = berryDeferred.await()
            val itemDto = api.getItemDetailByName(berryDto.item.name)
            toBerryDetail(berryDto, itemDto)
        }

        queries.insertBerryDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }
}
```

- [ ] **Step 5: Register in `DataModule.kt`**

Add to the existing `dataModule`:
```kotlin
import dev.huntdex.core.data.network.ItemApi
import dev.huntdex.core.data.repository.ItemRepositoryImpl
import dev.huntdex.core.domain.repository.ItemRepository

// Inside module { }:
single { ItemApi(get()) }
single<ItemRepository> { ItemRepositoryImpl(get(), get()) }
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :core:data:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemDetailDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/BerryDetailDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/ItemPocketDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/ItemApi.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/ItemMapper.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/ItemRepositoryImpl.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt
git commit -m "feat(items): network layer — DTOs, ItemApi, mapper, repository"
```

---

### Task 4: ItemListScreenModel + tests

**Files:**
- Modify: `feature/items/build.gradle.kts`
- Delete: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/Placeholder.kt`
- Delete: `feature/items/src/desktopMain/kotlin/dev/huntdex/feature/items/DesktopStub.kt`
- Delete: `feature/items/src/iosMain/kotlin/dev/huntdex/feature/items/IosStub.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemTab.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListState.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListIntent.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListScreenModel.kt`
- Create: `feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/FakeItemRepository.kt`
- Create: `feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/list/ItemListScreenModelTest.kt`

- [ ] **Step 1: Upgrade `feature/items/build.gradle.kts`**

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
    namespace = "dev.huntdex.feature.items"
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
rm feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/Placeholder.kt
rm feature/items/src/desktopMain/kotlin/dev/huntdex/feature/items/DesktopStub.kt
rm feature/items/src/iosMain/kotlin/dev/huntdex/feature/items/IosStub.kt
```

- [ ] **Step 3: Create `ItemTab.kt`, `ItemListState.kt`, `ItemListIntent.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemTab.kt`:
```kotlin
package dev.huntdex.feature.items.list

enum class ItemTab { Objects, Berries }
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListState.kt`:
```kotlin
package dev.huntdex.feature.items.list

import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.ItemEntry

data class ItemListState(
    val activeTab: ItemTab = ItemTab.Objects,
    val items: List<ItemEntry> = emptyList(),
    val berries: List<BerryEntry> = emptyList(),
    val isLoading: Boolean = true,
    val isLoadingMore: Boolean = false,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedPocket: String? = null,
    val selectedGeneration: Int? = null,
    val currentOffset: Int = 0,
    val hasMore: Boolean = true
) {
    val displayedItems: List<ItemEntry>
        get() = if (searchQuery.isBlank()) items
                else items.filter { it.name.contains(searchQuery, ignoreCase = true) }
    val displayedBerries: List<BerryEntry>
        get() = if (searchQuery.isBlank()) berries
                else berries.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListIntent.kt`:
```kotlin
package dev.huntdex.feature.items.list

sealed interface ItemListIntent {
    data class SelectTab(val tab: ItemTab) : ItemListIntent
    data object LoadNextPage : ItemListIntent
    data class Search(val query: String) : ItemListIntent
    data class FilterByPocket(val pocketName: String?) : ItemListIntent
    data class FilterByGeneration(val generationId: Int?) : ItemListIntent
    data class SelectItem(val id: Int) : ItemListIntent
    data class SelectBerry(val id: Int) : ItemListIntent
    data object Retry : ItemListIntent
}
```

- [ ] **Step 4: Write failing tests**

`feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/FakeItemRepository.kt`:
```kotlin
package dev.huntdex.feature.items

import dev.huntdex.core.domain.model.BerryDetail
import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.BerryFlavor
import dev.huntdex.core.domain.model.ItemDetail
import dev.huntdex.core.domain.model.ItemEntry
import dev.huntdex.core.domain.repository.ItemRepository

class FakeItemRepository : ItemRepository {
    val itemEntries = (1..30).map { id -> ItemEntry(id, "item-$id") }
    val berryEntries = (1..10).map { id -> BerryEntry(id, "berry-$id") }

    override suspend fun getItemPage(limit: Int, offset: Int): List<ItemEntry> =
        itemEntries.drop(offset).take(limit)

    override suspend fun getItemsByPocket(pocketName: String): List<ItemEntry> =
        itemEntries.take(5)

    override suspend fun getItemDetail(id: Int): ItemDetail = ItemDetail(
        id = id, name = "item-$id", spriteUrl = null, category = "medicine",
        pocket = "medicine", effectEntry = "Restores HP.", flavorText = "A test item.",
        flingPower = null, flingEffect = null
    )

    override suspend fun getAllBerries(): List<BerryEntry> = berryEntries

    override suspend fun getBerryDetail(id: Int): BerryDetail = BerryDetail(
        id = id, name = "berry-$id", itemName = "berry-$id", spriteUrl = null,
        growthTime = 3, maxHarvest = 5, naturalGiftPower = 60, size = 20,
        smoothness = 25, soilDryness = 15,
        flavors = listOf(BerryFlavor("spicy", 10)),
        effectEntry = "Restores HP.", flavorText = "A test berry."
    )
}
```

`feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/list/ItemListScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.items.list

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.items.FakeItemRepository
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

private class FakeAppNavigator : AppNavigator {
    val destinations = mutableListOf<Destination>()
    override fun navigateTo(destination: Destination) { destinations += destination }
    override fun navigateBack() {}
    override fun popTo(destination: Destination, inclusive: Boolean) {}
    override fun <T> setResult(key: String, value: T) {}
    override fun <T> getResult(key: String): Flow<T?> = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
class ItemListScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading objects tab`() = testScope.runTest {
        val model = ItemListScreenModel(FakeItemRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertEquals(ItemTab.Objects, model.state.value.activeTab)
    }

    @Test
    fun `after init items are loaded`() = testScope.runTest {
        val model = ItemListScreenModel(FakeItemRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(20, model.state.value.items.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `switching to berries tab loads berries`() = testScope.runTest {
        val model = ItemListScreenModel(FakeItemRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(ItemListIntent.SelectTab(ItemTab.Berries))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(ItemTab.Berries, model.state.value.activeTab)
        assertEquals(10, model.state.value.berries.size)
    }

    @Test
    fun `search filters items by name`() = testScope.runTest {
        val model = ItemListScreenModel(FakeItemRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(ItemListIntent.Search("item-1"))
        val names = model.state.value.displayedItems.map { it.name }
        assertTrue(names.all { it.contains("item-1") })
    }

    @Test
    fun `selecting item navigates to item detail`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = ItemListScreenModel(FakeItemRepository(), navigator, this)
        model.onIntent(ItemListIntent.SelectItem(5))
        assertEquals(Destination.ItemDetail(5), navigator.destinations.last())
    }

    @Test
    fun `selecting berry navigates to berry detail`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = ItemListScreenModel(FakeItemRepository(), navigator, this)
        model.onIntent(ItemListIntent.SelectBerry(3))
        assertEquals(Destination.BerryDetail(3), navigator.destinations.last())
    }

    @Test
    fun `pocket filter replaces item list`() = testScope.runTest {
        val model = ItemListScreenModel(FakeItemRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(ItemListIntent.FilterByPocket("medicine"))
        dispatcher.scheduler.advanceUntilIdle()
        assertEquals(5, model.state.value.items.size)
        assertEquals("medicine", model.state.value.selectedPocket)
    }
}
```

- [ ] **Step 5: Run tests to verify they fail**

```bash
./gradlew :feature:items:desktopTest
```
Expected: FAIL — `ItemListScreenModel` not defined yet

- [ ] **Step 6: Create `ItemListScreenModel.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListScreenModel.kt`:
```kotlin
package dev.huntdex.feature.items.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.ItemRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val PAGE_SIZE = 20

class ItemListScreenModel(
    private val repository: ItemRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(ItemListState())
    val state: StateFlow<ItemListState> = _state.asStateFlow()

    init { loadItems() }

    fun onIntent(intent: ItemListIntent) {
        when (intent) {
            is ItemListIntent.SelectTab -> switchTab(intent.tab)
            is ItemListIntent.LoadNextPage -> loadNextPage()
            is ItemListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is ItemListIntent.FilterByPocket -> applyPocketFilter(intent.pocketName)
            is ItemListIntent.FilterByGeneration -> _state.update { it.copy(selectedGeneration = intent.generationId) }
            is ItemListIntent.SelectItem -> navigator.navigateTo(Destination.ItemDetail(intent.id))
            is ItemListIntent.SelectBerry -> navigator.navigateTo(Destination.BerryDetail(intent.id))
            is ItemListIntent.Retry -> when (_state.value.activeTab) {
                ItemTab.Objects -> if (_state.value.selectedPocket != null) applyPocketFilter(_state.value.selectedPocket) else loadItems()
                ItemTab.Berries -> loadBerries()
            }
        }
    }

    private fun switchTab(tab: ItemTab) {
        _state.update { it.copy(activeTab = tab, searchQuery = "") }
        if (tab == ItemTab.Berries && _state.value.berries.isEmpty()) loadBerries()
    }

    private fun loadItems() {
        val query = _state.value.searchQuery
        _state.update { ItemListState(isLoading = true, searchQuery = query) }
        scope.launch {
            runCatching { repository.getItemPage(PAGE_SIZE, 0) }
                .onSuccess { entries ->
                    _state.update { it.copy(items = entries, isLoading = false, currentOffset = entries.size, hasMore = entries.size == PAGE_SIZE) }
                }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadNextPage() {
        val current = _state.value
        if (current.isLoadingMore || !current.hasMore || current.selectedPocket != null) return
        _state.update { it.copy(isLoadingMore = true) }
        scope.launch {
            runCatching { repository.getItemPage(PAGE_SIZE, current.currentOffset) }
                .onSuccess { entries ->
                    _state.update { it.copy(items = it.items + entries, isLoadingMore = false, currentOffset = it.currentOffset + entries.size, hasMore = entries.size == PAGE_SIZE) }
                }
                .onFailure { e -> _state.update { it.copy(isLoadingMore = false, error = e.message) } }
        }
    }

    private fun applyPocketFilter(pocketName: String?) {
        _state.update { it.copy(selectedPocket = pocketName, isLoading = true, items = emptyList()) }
        if (pocketName == null) { loadItems(); return }
        scope.launch {
            runCatching { repository.getItemsByPocket(pocketName) }
                .onSuccess { entries -> _state.update { it.copy(items = entries, isLoading = false, hasMore = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }

    private fun loadBerries() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getAllBerries() }
                .onSuccess { entries -> _state.update { it.copy(berries = entries, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 7: Run all tests**

```bash
./gradlew :feature:items:desktopTest
```
Expected: All 7 tests PASS

- [ ] **Step 8: Commit**

```bash
git add feature/items/build.gradle.kts \
        feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ \
        feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/
git commit -m "feat(items): ItemListScreenModel with tabs, pocket filter, and berry navigation"
```

---

### Task 5: Detail ScreenModels + tests

**Files:**
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailState.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailIntent.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreenModel.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailState.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailIntent.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreenModel.kt`
- Create: `feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreenModelTest.kt`
- Create: `feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreenModelTest.kt`

- [ ] **Step 1: Create item detail files**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailState.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import dev.huntdex.core.domain.model.ItemDetail

data class ItemDetailState(
    val detail: ItemDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailIntent.kt`:
```kotlin
package dev.huntdex.feature.items.detail

sealed interface ItemDetailIntent {
    data object NavigateBack : ItemDetailIntent
    data object Retry : ItemDetailIntent
}
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailState.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import dev.huntdex.core.domain.model.BerryDetail

data class BerryDetailState(
    val detail: BerryDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null
)
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailIntent.kt`:
```kotlin
package dev.huntdex.feature.items.detail

sealed interface BerryDetailIntent {
    data object NavigateBack : BerryDetailIntent
    data object Retry : BerryDetailIntent
}
```

- [ ] **Step 2: Write failing tests**

`feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.items.FakeItemRepository
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
class ItemDetailScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = ItemDetailScreenModel(1, FakeItemRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertNull(model.state.value.detail)
    }

    @Test
    fun `after init detail is loaded`() = testScope.runTest {
        val model = ItemDetailScreenModel(1, FakeItemRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertNotNull(model.state.value.detail)
        assertEquals(1, model.state.value.detail?.id)
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = ItemDetailScreenModel(1, FakeItemRepository(), navigator, this)
        model.onIntent(ItemDetailIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }
}
```

`feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.items.FakeItemRepository
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

private class FakeNavForBerry : AppNavigator {
    var backCalled = false
    override fun navigateTo(destination: Destination) {}
    override fun navigateBack() { backCalled = true }
    override fun popTo(destination: Destination, inclusive: Boolean) {}
    override fun <T> setResult(key: String, value: T) {}
    override fun <T> getResult(key: String): Flow<T?> = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
class BerryDetailScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = BerryDetailScreenModel(1, FakeItemRepository(), FakeNavForBerry(), this)
        assertTrue(model.state.value.isLoading)
        assertNull(model.state.value.detail)
    }

    @Test
    fun `after init berry detail is loaded`() = testScope.runTest {
        val model = BerryDetailScreenModel(1, FakeItemRepository(), FakeNavForBerry(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertNotNull(model.state.value.detail)
        assertEquals("spicy", model.state.value.detail?.flavors?.firstOrNull()?.flavor)
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeNavForBerry()
        val model = BerryDetailScreenModel(1, FakeItemRepository(), navigator, this)
        model.onIntent(BerryDetailIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :feature:items:desktopTest
```
Expected: FAIL — ScreenModels not defined yet

- [ ] **Step 4: Create `ItemDetailScreenModel.kt` and `BerryDetailScreenModel.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreenModel.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.ItemRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class ItemDetailScreenModel(
    private val itemId: Int,
    private val repository: ItemRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(ItemDetailState())
    val state: StateFlow<ItemDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onIntent(intent: ItemDetailIntent) {
        when (intent) {
            is ItemDetailIntent.NavigateBack -> navigator.navigateBack()
            is ItemDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getItemDetail(itemId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreenModel.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.ItemRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class BerryDetailScreenModel(
    private val berryId: Int,
    private val repository: ItemRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(BerryDetailState())
    val state: StateFlow<BerryDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onIntent(intent: BerryDetailIntent) {
        when (intent) {
            is BerryDetailIntent.NavigateBack -> navigator.navigateBack()
            is BerryDetailIntent.Retry -> loadDetail()
        }
    }

    private fun loadDetail() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getBerryDetail(berryId) }
                .onSuccess { detail -> _state.update { it.copy(detail = detail, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 5: Run all tests**

```bash
./gradlew :feature:items:desktopTest
```
Expected: All 13 tests PASS

- [ ] **Step 6: Commit**

```bash
git add feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ \
        feature/items/src/commonTest/kotlin/dev/huntdex/feature/items/detail/
git commit -m "feat(items): ItemDetailScreenModel and BerryDetailScreenModel"
```

---

### Task 6: UI screens

**Files:**
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListScreen.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreen.kt`
- Create: `feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreen.kt`

- [ ] **Step 1: Create `ItemListScreen.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/list/ItemListScreen.kt`:
```kotlin
package dev.huntdex.feature.items.list

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.BerryEntry
import dev.huntdex.core.domain.model.ItemEntry

data object ItemListScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<ItemListScreenModel>()
        val state by model.state.collectAsState()
        ItemListContent(state = state, onIntent = model::onIntent)
    }
}

@Composable
private fun ItemListContent(state: ItemListState, onIntent: (ItemListIntent) -> Unit) {
    Column(Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = state.activeTab.ordinal) {
            Tab(
                selected = state.activeTab == ItemTab.Objects,
                onClick = { onIntent(ItemListIntent.SelectTab(ItemTab.Objects)) },
                text = { Text("Objects") }
            )
            Tab(
                selected = state.activeTab == ItemTab.Berries,
                onClick = { onIntent(ItemListIntent.SelectTab(ItemTab.Berries)) },
                text = { Text("Berries") }
            )
        }

        OutlinedTextField(
            value = state.searchQuery,
            onValueChange = { onIntent(ItemListIntent.Search(it)) },
            label = { Text(if (state.activeTab == ItemTab.Objects) "Search items" else "Search berries") },
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
        )

        if (state.activeTab == ItemTab.Objects) {
            val pockets = listOf("medicine", "battle", "berries", "machines", "stat-boosts", "in-a-pinch", "picky-healing", "type-protection", "baking-only", "collectibles", "held-items", "key-items", "all-machines", "plates", "species-specific", "vitamins")
            LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(pockets) { pocket ->
                    FilterChip(
                        selected = state.selectedPocket == pocket,
                        onClick = { onIntent(ItemListIntent.FilterByPocket(if (state.selectedPocket == pocket) null else pocket)) },
                        label = { Text(pocket.replaceFirstChar { it.uppercase() }) }
                    )
                }
            }
        }

        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Spacer(Modifier.height(8.dp))
                    Button(onClick = { onIntent(ItemListIntent.Retry) }) { Text("Retry") }
                }
            }
            state.activeTab == ItemTab.Objects -> ItemsList(state, onIntent)
            state.activeTab == ItemTab.Berries -> BerriesList(state, onIntent)
        }
    }
}

@Composable
private fun ItemsList(state: ItemListState, onIntent: (ItemListIntent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        itemsIndexed(state.displayedItems) { index, item ->
            if (index == state.displayedItems.size - 1 && state.hasMore) onIntent(ItemListIntent.LoadNextPage)
            ItemRow(item = item, onClick = { onIntent(ItemListIntent.SelectItem(item.id)) })
            HorizontalDivider()
        }
        if (state.isLoadingMore) {
            item { Box(Modifier.fillMaxWidth().padding(16.dp), contentAlignment = Alignment.Center) { CircularProgressIndicator() } }
        }
    }
}

@Composable
private fun BerriesList(state: ItemListState, onIntent: (ItemListIntent) -> Unit) {
    LazyColumn(Modifier.fillMaxSize()) {
        items(state.displayedBerries) { berry ->
            BerryRow(berry = berry, onClick = { onIntent(ItemListIntent.SelectBerry(berry.id)) })
            HorizontalDivider()
        }
    }
}

@Composable
private fun ItemRow(item: ItemEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(item.name.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun BerryRow(berry: BerryEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(berry.name.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Create `ItemDetailScreen.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/ItemDetailScreen.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.ItemDetail
import org.koin.core.parameter.parametersOf

data class ItemDetailScreen(val id: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<ItemDetailScreenModel> { parametersOf(id) }
        val state by model.state.collectAsState()
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Button(onClick = { model.onIntent(ItemDetailIntent.Retry) }) { Text("Retry") }
                }
            }
            state.detail != null -> ItemDetailLoaded(detail = state.detail!!, onBack = { model.onIntent(ItemDetailIntent.NavigateBack) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ItemDetailLoaded(detail: ItemDetail, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.name.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    if (detail.spriteUrl != null) {
                        AsyncImage(model = detail.spriteUrl, contentDescription = detail.name, modifier = Modifier.size(80.dp))
                        Spacer(Modifier.width(16.dp))
                    }
                    Column {
                        Text(detail.name.replace('-', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.headlineSmall)
                        Text(detail.category.replace('-', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detail.effectEntry.isNotBlank()) Text(detail.effectEntry, style = MaterialTheme.typography.bodyMedium)
                        if (detail.flavorText.isNotBlank()) Text(detail.flavorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        if (detail.flingPower != null) {
                            HorizontalDivider()
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fling Power", style = MaterialTheme.typography.bodyMedium)
                                Text(detail.flingPower.toString())
                            }
                        }
                        if (detail.flingEffect != null) {
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("Fling Effect", style = MaterialTheme.typography.bodyMedium)
                                Text(detail.flingEffect.replace('-', ' '))
                            }
                        }
                    }
                }
            }
        }
    }
}
```

- [ ] **Step 3: Create `BerryDetailScreen.kt`**

`feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/detail/BerryDetailScreen.kt`:
```kotlin
package dev.huntdex.feature.items.detail

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.BerryDetail
import org.koin.core.parameter.parametersOf

data class BerryDetailScreen(val id: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<BerryDetailScreenModel> { parametersOf(id) }
        val state by model.state.collectAsState()
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Button(onClick = { model.onIntent(BerryDetailIntent.Retry) }) { Text("Retry") }
                }
            }
            state.detail != null -> BerryDetailLoaded(detail = state.detail!!, onBack = { model.onIntent(BerryDetailIntent.NavigateBack) })
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun BerryDetailLoaded(detail: BerryDetail, onBack: () -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(detail.name.replace('-', ' ').replaceFirstChar { it.uppercase() } + " Berry") },
                navigationIcon = { IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back") } }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                if (detail.spriteUrl != null) {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        AsyncImage(model = detail.spriteUrl, contentDescription = detail.name, modifier = Modifier.size(96.dp))
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Growth", style = MaterialTheme.typography.titleMedium)
                        StatRow("Growth Time", "${detail.growthTime}h")
                        StatRow("Max Harvest", detail.maxHarvest.toString())
                        StatRow("Size", "${detail.size}mm")
                        StatRow("Smoothness", detail.smoothness.toString())
                        StatRow("Soil Dryness", detail.soilDryness.toString())
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("Flavors", style = MaterialTheme.typography.titleMedium)
                        detail.flavors.forEach { flavor ->
                            StatRow(flavor.flavor.replaceFirstChar { it.uppercase() }, flavor.potency.toString())
                        }
                    }
                }
            }
            item {
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (detail.effectEntry.isNotBlank()) Text(detail.effectEntry, style = MaterialTheme.typography.bodyMedium)
                        if (detail.flavorText.isNotBlank()) Text(detail.flavorText, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
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
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :feature:items:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add feature/items/src/commonMain/kotlin/dev/huntdex/feature/items/
git commit -m "feat(items): ItemListScreen, ItemDetailScreen, BerryDetailScreen UI"
```

---

### Task 7: Wire all 3 entry points

**Files:**
- Modify: `app/build.gradle.kts`, `app/src/main/java/dev/huntdex/app/di/AppModule.kt`, `app/src/main/java/dev/huntdex/app/navigation/DestinationMapper.kt`
- Modify: `desktopApp/build.gradle.kts`, `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/di/DesktopAppModule.kt`, `desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/navigation/DesktopDestinationMapper.kt`
- Modify: `shared/build.gradle.kts`, `shared/src/iosMain/kotlin/dev/huntdex/shared/di/IosAppModule.kt`, `shared/src/iosMain/kotlin/dev/huntdex/shared/navigation/IosDestinationMapper.kt`

- [ ] **Step 1: Add `feature:items` dependency to all 3 entry points**

In `app/build.gradle.kts` → `dependencies {}`: `implementation(projects.feature.items)`
In `desktopApp/build.gradle.kts` → `desktopMain.dependencies {}`: `implementation(projects.feature.items)`
In `shared/build.gradle.kts` → iosMain dependencies: `implementation(projects.feature.items)`

- [ ] **Step 2: Register ScreenModels in all 3 DI modules**

Add to each of `AppModule.kt`, `DesktopAppModule.kt`, `IosAppModule.kt`:
```kotlin
import dev.huntdex.feature.items.list.ItemListScreenModel
import dev.huntdex.feature.items.detail.ItemDetailScreenModel
import dev.huntdex.feature.items.detail.BerryDetailScreenModel

// Inside module { }:
factory { ItemListScreenModel(get(), get()) }
factory { params -> ItemDetailScreenModel(params.get(), get(), get()) }
factory { params -> BerryDetailScreenModel(params.get(), get(), get()) }
```

- [ ] **Step 3: Update all 3 DestinationMappers**

Add to Android `DestinationMapper.kt`, Desktop `DesktopDestinationMapper.kt`, iOS `IosDestinationMapper.kt`:
```kotlin
import dev.huntdex.feature.items.list.ItemListScreen
import dev.huntdex.feature.items.detail.ItemDetailScreen
import dev.huntdex.feature.items.detail.BerryDetailScreen

// Add cases (before the else branch):
is Destination.ItemList -> ItemListScreen
is Destination.ItemDetail -> ItemDetailScreen(id)
is Destination.BerryDetail -> BerryDetailScreen(id)
```

- [ ] **Step 4: Build all targets**

```bash
./gradlew :app:assembleDebug :desktopApp:jar
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit and push**

```bash
git add app/build.gradle.kts app/src/main/java/dev/huntdex/app/ \
        desktopApp/build.gradle.kts desktopApp/src/desktopMain/kotlin/dev/huntdex/desktopapp/ \
        shared/build.gradle.kts shared/src/iosMain/kotlin/dev/huntdex/shared/
git commit -m "feat(items): wire feature into Android, Desktop, and iOS entry points"
git push origin phase-2/items
```
