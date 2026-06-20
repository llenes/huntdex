# Phase 2 PR 3 — feature:locations Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Implement the Locations encyclopedia — 2-level navigation: region list → location list (for a region) → location detail with expandable area sections showing Pokémon encounters.

**Architecture:** MVI pattern identical to `feature:pokedex`. The `Destination` sealed class gains `LocationList(regionId: Int)`. Locations detail resolves area encounter calls in parallel. Area sections are collapsed by default, expanded on tap.

**Tech Stack:** Kotlin Multiplatform, Compose Multiplatform, Voyager, Koin, Ktor, SQLDelight, kotlinx.serialization.

## Global Constraints

- Kotlin 2.0.21, Compose Multiplatform 1.7.0
- Koin 3.5.6, Coil 3.0.4, coroutines 1.9.0
- All new domain models must be `@Serializable`
- Target platforms: Android + Desktop + iOS (commonMain code only)
- Test runner: `./gradlew :feature:locations:desktopTest`

---

### Task 1: Domain models, navigation destination, and repository interface

**Files:**
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/RegionEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationEntry.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/AreaEncounter.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationArea.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationDetail.kt`
- Create: `core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/LocationRepository.kt`
- Modify: `core/navigation/src/commonMain/kotlin/dev/huntdex/core/navigation/Destination.kt`

- [ ] **Step 1: Create domain models**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/RegionEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class RegionEntry(val id: Int, val name: String, val generation: String)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationEntry.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationEntry(val id: Int, val name: String, val regionId: Int)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/AreaEncounter.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class AreaEncounter(
    val pokemonId: Int,
    val pokemonName: String,
    val version: String,
    val method: String,
    val chance: Int,
    val minLevel: Int,
    val maxLevel: Int
)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationArea.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationArea(
    val id: Int,
    val name: String,
    val encounters: List<AreaEncounter>
)
```

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/LocationDetail.kt`:
```kotlin
package dev.huntdex.core.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class LocationDetail(
    val id: Int,
    val name: String,
    val regionId: Int,
    val areas: List<LocationArea>
)
```

- [ ] **Step 2: Add `LocationList` destination**

Replace full `Destination.kt`:
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
    data class LocationList(val regionId: Int) : Destination()
    data class LocationDetail(val id: Int) : Destination()
    data object GenerationList : Destination()
    data object HuntingList : Destination()
    data class HuntingSessionDetail(val sessionId: String) : Destination()
    data class NewHuntingSession(val pokemonId: Int? = null) : Destination()
    data object Profile : Destination()
}
```

- [ ] **Step 3: Create repository interface**

`core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/LocationRepository.kt`:
```kotlin
package dev.huntdex.core.domain.repository

import dev.huntdex.core.domain.model.LocationDetail
import dev.huntdex.core.domain.model.LocationEntry
import dev.huntdex.core.domain.model.RegionEntry

interface LocationRepository {
    suspend fun getAllRegions(): List<RegionEntry>
    suspend fun getLocationsByRegion(regionId: Int): List<LocationEntry>
    suspend fun getLocationDetail(id: Int): LocationDetail
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :core:domain:desktopMainKlibrary :core:navigation:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/model/ \
        core/domain/src/commonMain/kotlin/dev/huntdex/core/domain/repository/LocationRepository.kt \
        core/navigation/src/commonMain/kotlin/dev/huntdex/core/navigation/Destination.kt
git commit -m "feat(locations): domain models, LocationList destination, LocationRepository interface"
```

---

### Task 2: SQLDelight schema + desktop migration guard

**Files:**
- Modify: `core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq`
- Modify: `core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt`

- [ ] **Step 1: Add location tables to `HuntdexDatabase.sq`**

Append to the file:
```sql
-- ============================================================
-- Region Cache
-- ============================================================
CREATE TABLE region_entry (
  id         INTEGER PRIMARY KEY,
  name       TEXT    NOT NULL,
  generation TEXT    NOT NULL
);

insertRegionEntry:
INSERT OR IGNORE INTO region_entry (id, name, generation) VALUES (?, ?, ?);

selectAllRegions:
SELECT id, name, generation FROM region_entry ORDER BY id;

-- ============================================================
-- Location List Cache (per region)
-- ============================================================
CREATE TABLE location_entry (
  id        INTEGER PRIMARY KEY,
  name      TEXT    NOT NULL,
  region_id INTEGER NOT NULL
);

insertLocationEntry:
INSERT OR IGNORE INTO location_entry (id, name, region_id) VALUES (?, ?, ?);

selectLocationsByRegion:
SELECT id, name, region_id FROM location_entry WHERE region_id = ? ORDER BY id;

-- ============================================================
-- Location Detail Cache (areas + encounters as JSON)
-- ============================================================
CREATE TABLE location_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL
);

insertLocationDetail:
INSERT OR REPLACE INTO location_detail (id, data) VALUES (?, ?);

selectLocationDetail:
SELECT data FROM location_detail WHERE id = ?;
```

- [ ] **Step 2: Update migration guard in `DatabaseDriverFactory.kt`**

Add `"region_entry" !in tables` branch. Full file:
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
                driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
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
            }
            "item_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE item_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE item_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE berry_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            "region_entry" !in tables -> {
                driver.execute(null, "CREATE TABLE region_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, generation TEXT NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_entry (id INTEGER PRIMARY KEY, name TEXT NOT NULL, region_id INTEGER NOT NULL)", 0)
                driver.execute(null, "CREATE TABLE location_detail (id INTEGER PRIMARY KEY, data TEXT NOT NULL)", 0)
            }
            // else: all tables present, nothing to do
        }

        return driver
    }
}
```

- [ ] **Step 3: Verify**

```bash
./gradlew :core:data:generateSqlDelightInterface
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 4: Commit**

```bash
git add core/data/src/commonMain/sqldelight/dev/huntdex/core/data/db/HuntdexDatabase.sq \
        core/data/src/desktopMain/kotlin/dev/huntdex/core/data/db/DatabaseDriverFactory.kt
git commit -m "feat(locations): add region_entry, location_entry, location_detail SQLDelight tables"
```

---

### Task 3: Network layer — DTOs, API, mapper, repository, DI

**Files:**
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/RegionDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/LocationAreaDto.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/LocationApi.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/LocationMapper.kt`
- Create: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/LocationRepositoryImpl.kt`
- Modify: `core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt`

- [ ] **Step 1: Create DTOs**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/RegionDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegionListResponseDto(val count: Int, val results: List<NamedApiResourceDto>)

@Serializable
data class RegionDetailDto(
    val id: Int,
    val name: String,
    val locations: List<NamedApiResourceDto>,
    @SerialName("main_generation") val mainGeneration: NamedApiResourceDto?
)

@Serializable
data class LocationDetailApiDto(
    val id: Int,
    val name: String,
    val region: NamedApiResourceDto?,
    val areas: List<NamedApiResourceDto>
)
```

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/LocationAreaDto.kt`:
```kotlin
package dev.huntdex.core.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class LocationAreaApiDto(
    val id: Int,
    val name: String,
    @SerialName("pokemon_encounters") val pokemonEncounters: List<PokemonEncounterDto>
)

@Serializable
data class PokemonEncounterDto(
    val pokemon: NamedApiResourceDto,
    @SerialName("version_details") val versionDetails: List<EncounterVersionDetailDto>
)

@Serializable
data class EncounterVersionDetailDto(
    @SerialName("max_chance") val maxChance: Int,
    val version: NamedApiResourceDto,
    @SerialName("encounter_details") val encounterDetails: List<EncounterDetailDto>
)

@Serializable
data class EncounterDetailDto(
    val chance: Int,
    @SerialName("max_level") val maxLevel: Int,
    @SerialName("min_level") val minLevel: Int,
    val method: NamedApiResourceDto
)
```

- [ ] **Step 2: Create `LocationApi.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/LocationApi.kt`:
```kotlin
package dev.huntdex.core.data.network

import dev.huntdex.core.data.network.dto.LocationAreaApiDto
import dev.huntdex.core.data.network.dto.LocationDetailApiDto
import dev.huntdex.core.data.network.dto.RegionDetailDto
import dev.huntdex.core.data.network.dto.RegionListResponseDto
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get

private const val BASE_URL = "https://pokeapi.co/api/v2"

class LocationApi(private val client: HttpClient) {
    suspend fun getRegionList(): RegionListResponseDto =
        client.get("$BASE_URL/region").body()

    suspend fun getRegionDetail(id: Int): RegionDetailDto =
        client.get("$BASE_URL/region/$id").body()

    suspend fun getLocationDetail(id: Int): LocationDetailApiDto =
        client.get("$BASE_URL/location/$id").body()

    suspend fun getLocationArea(id: Int): LocationAreaApiDto =
        client.get("$BASE_URL/location-area/$id").body()
}
```

- [ ] **Step 3: Create `LocationMapper.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/LocationMapper.kt`:
```kotlin
package dev.huntdex.core.data.mapper

import dev.huntdex.core.data.network.dto.LocationAreaApiDto
import dev.huntdex.core.data.network.dto.RegionDetailDto
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.AreaEncounter
import dev.huntdex.core.domain.model.LocationArea
import dev.huntdex.core.domain.model.RegionEntry

fun toRegionEntry(dto: RegionDetailDto): RegionEntry = RegionEntry(
    id = dto.id,
    name = dto.name,
    generation = dto.mainGeneration?.name ?: "unknown"
)

fun toLocationArea(dto: LocationAreaApiDto): LocationArea {
    val encounters = dto.pokemonEncounters.flatMap { encounter ->
        val pokemonId = encounter.pokemon.url.extractPokeApiId()
        encounter.versionDetails.map { vd ->
            val detail = vd.encounterDetails.firstOrNull()
            AreaEncounter(
                pokemonId = pokemonId,
                pokemonName = encounter.pokemon.name,
                version = vd.version.name,
                method = detail?.method?.name ?: "unknown",
                chance = vd.maxChance,
                minLevel = detail?.minLevel ?: 0,
                maxLevel = detail?.maxLevel ?: 0
            )
        }
    }

    return LocationArea(
        id = dto.id,
        name = dto.name,
        encounters = encounters
    )
}
```

- [ ] **Step 4: Create `LocationRepositoryImpl.kt`**

`core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/LocationRepositoryImpl.kt`:
```kotlin
package dev.huntdex.core.data.repository

import dev.huntdex.core.data.db.HuntdexDatabase
import dev.huntdex.core.data.mapper.toLocationArea
import dev.huntdex.core.data.mapper.toRegionEntry
import dev.huntdex.core.data.network.LocationApi
import dev.huntdex.core.data.network.dto.extractPokeApiId
import dev.huntdex.core.domain.model.LocationDetail
import dev.huntdex.core.domain.model.LocationEntry
import dev.huntdex.core.domain.model.RegionEntry
import dev.huntdex.core.domain.repository.LocationRepository
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class LocationRepositoryImpl(
    private val db: HuntdexDatabase,
    private val api: LocationApi
) : LocationRepository {

    private val queries get() = db.huntdexDatabaseQueries

    override suspend fun getAllRegions(): List<RegionEntry> {
        val cached = queries.selectAllRegions().executeAsList()
        if (cached.isNotEmpty()) return cached.map { RegionEntry(it.id.toInt(), it.name, it.generation) }

        val listResponse = api.getRegionList()
        val regions = coroutineScope {
            listResponse.results.map { async { api.getRegionDetail(it.url.extractPokeApiId()) } }.awaitAll()
        }
        regions.forEach { dto ->
            val entry = toRegionEntry(dto)
            queries.insertRegionEntry(entry.id.toLong(), entry.name, entry.generation)
        }
        return queries.selectAllRegions().executeAsList().map { RegionEntry(it.id.toInt(), it.name, it.generation) }
    }

    override suspend fun getLocationsByRegion(regionId: Int): List<LocationEntry> {
        val cached = queries.selectLocationsByRegion(regionId.toLong()).executeAsList()
        if (cached.isNotEmpty()) return cached.map { LocationEntry(it.id.toInt(), it.name, it.region_id.toInt()) }

        val regionDetail = api.getRegionDetail(regionId)
        regionDetail.locations.forEach { loc ->
            val id = loc.url.extractPokeApiId()
            queries.insertLocationEntry(id.toLong(), loc.name, regionId.toLong())
        }
        return queries.selectLocationsByRegion(regionId.toLong()).executeAsList()
            .map { LocationEntry(it.id.toInt(), it.name, it.region_id.toInt()) }
    }

    override suspend fun getLocationDetail(id: Int): LocationDetail {
        val cachedJson = queries.selectLocationDetail(id.toLong()).executeAsOneOrNull()
        if (cachedJson != null) return Json.decodeFromString(cachedJson)

        val detail = coroutineScope {
            val locationDto = api.getLocationDetail(id)
            val areas = locationDto.areas
                .map { async { toLocationArea(api.getLocationArea(it.url.extractPokeApiId())) } }
                .awaitAll()

            LocationDetail(
                id = locationDto.id,
                name = locationDto.name,
                regionId = locationDto.region?.url?.extractPokeApiId() ?: 0,
                areas = areas
            )
        }

        queries.insertLocationDetail(id.toLong(), Json.encodeToString(detail))
        return detail
    }
}
```

- [ ] **Step 5: Register in `DataModule.kt`**

Add to `dataModule`:
```kotlin
import dev.huntdex.core.data.network.LocationApi
import dev.huntdex.core.data.repository.LocationRepositoryImpl
import dev.huntdex.core.domain.repository.LocationRepository

// Inside module { }:
single { LocationApi(get()) }
single<LocationRepository> { LocationRepositoryImpl(get(), get()) }
```

- [ ] **Step 6: Verify compilation**

```bash
./gradlew :core:data:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 7: Commit**

```bash
git add core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/RegionDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/dto/LocationAreaDto.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/network/LocationApi.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/LocationMapper.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/LocationRepositoryImpl.kt \
        core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt
git commit -m "feat(locations): network layer — DTOs, LocationApi, mapper, repository"
```

---

### Task 4: RegionList + LocationList ScreenModels + tests

**Files:**
- Modify: `feature/locations/build.gradle.kts`
- Delete: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/Placeholder.kt`
- Delete: `feature/locations/src/desktopMain/kotlin/dev/huntdex/feature/locations/DesktopStub.kt`
- Delete: `feature/locations/src/iosMain/kotlin/dev/huntdex/feature/locations/IosStub.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListState.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListIntent.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListScreenModel.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListState.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListIntent.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModel.kt`
- Create: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/FakeLocationRepository.kt`
- Create: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/region/RegionListScreenModelTest.kt`
- Create: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModelTest.kt`

- [ ] **Step 1: Upgrade `feature/locations/build.gradle.kts`**

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
    namespace = "dev.huntdex.feature.locations"
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
rm feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/Placeholder.kt
rm feature/locations/src/desktopMain/kotlin/dev/huntdex/feature/locations/DesktopStub.kt
rm feature/locations/src/iosMain/kotlin/dev/huntdex/feature/locations/IosStub.kt
```

- [ ] **Step 3: Create RegionList MVI files**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListState.kt`:
```kotlin
package dev.huntdex.feature.locations.region

import dev.huntdex.core.domain.model.RegionEntry

data class RegionListState(
    val regions: List<RegionEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)
```

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListIntent.kt`:
```kotlin
package dev.huntdex.feature.locations.region

sealed interface RegionListIntent {
    data class SelectRegion(val id: Int) : RegionListIntent
    data object Retry : RegionListIntent
}
```

- [ ] **Step 4: Create LocationList MVI files**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListState.kt`:
```kotlin
package dev.huntdex.feature.locations.list

import dev.huntdex.core.domain.model.LocationEntry

data class LocationListState(
    val locations: List<LocationEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val selectedGeneration: Int? = null
) {
    val displayedLocations: List<LocationEntry>
        get() = if (searchQuery.isBlank()) locations
                else locations.filter { it.name.contains(searchQuery, ignoreCase = true) }
}
```

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListIntent.kt`:
```kotlin
package dev.huntdex.feature.locations.list

sealed interface LocationListIntent {
    data class Search(val query: String) : LocationListIntent
    data class FilterByGeneration(val generationId: Int?) : LocationListIntent
    data class SelectLocation(val id: Int) : LocationListIntent
    data object Retry : LocationListIntent
    data object NavigateBack : LocationListIntent
}
```

- [ ] **Step 5: Write failing tests**

`feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/FakeLocationRepository.kt`:
```kotlin
package dev.huntdex.feature.locations

import dev.huntdex.core.domain.model.AreaEncounter
import dev.huntdex.core.domain.model.LocationArea
import dev.huntdex.core.domain.model.LocationDetail
import dev.huntdex.core.domain.model.LocationEntry
import dev.huntdex.core.domain.model.RegionEntry
import dev.huntdex.core.domain.repository.LocationRepository

class FakeLocationRepository : LocationRepository {
    val regions = listOf(
        RegionEntry(1, "kanto", "generation-i"),
        RegionEntry(2, "johto", "generation-ii"),
        RegionEntry(3, "hoenn", "generation-iii")
    )
    val locations = (1..10).map { id -> LocationEntry(id, "location-$id", 1) }

    override suspend fun getAllRegions(): List<RegionEntry> = regions

    override suspend fun getLocationsByRegion(regionId: Int): List<LocationEntry> = locations

    override suspend fun getLocationDetail(id: Int): LocationDetail = LocationDetail(
        id = id,
        name = "location-$id",
        regionId = 1,
        areas = listOf(
            LocationArea(
                id = 1, name = "area-1",
                encounters = listOf(
                    AreaEncounter(1, "bulbasaur", "red", "walk", 10, 5, 10)
                )
            )
        )
    )
}
```

`feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/region/RegionListScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.locations.region

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.locations.FakeLocationRepository
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
class RegionListScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = RegionListScreenModel(FakeLocationRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertTrue(model.state.value.regions.isEmpty())
    }

    @Test
    fun `after init regions are loaded`() = testScope.runTest {
        val model = RegionListScreenModel(FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(3, model.state.value.regions.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `selecting region navigates to location list`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = RegionListScreenModel(FakeLocationRepository(), navigator, this)
        model.onIntent(RegionListIntent.SelectRegion(1))
        assertEquals(Destination.LocationList(1), navigator.destinations.last())
    }
}
```

`feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.locations.list

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.locations.FakeLocationRepository
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
    var backCalled = false
    override fun navigateTo(destination: Destination) { destinations += destination }
    override fun navigateBack() { backCalled = true }
    override fun popTo(destination: Destination, inclusive: Boolean) {}
    override fun <T> setResult(key: String, value: T) {}
    override fun <T> getResult(key: String): Flow<T?> = throw NotImplementedError()
}

@OptIn(ExperimentalCoroutinesApi::class)
class LocationListScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = LocationListScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
    }

    @Test
    fun `after init locations for region are loaded`() = testScope.runTest {
        val model = LocationListScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertEquals(10, model.state.value.locations.size)
        assertNull(model.state.value.error)
    }

    @Test
    fun `search filters displayed locations`() = testScope.runTest {
        val model = LocationListScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(LocationListIntent.Search("location-1"))
        val names = model.state.value.displayedLocations.map { it.name }
        assertTrue(names.all { it.contains("location-1") })
    }

    @Test
    fun `selecting location navigates to detail`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = LocationListScreenModel(1, FakeLocationRepository(), navigator, this)
        model.onIntent(LocationListIntent.SelectLocation(5))
        assertEquals(Destination.LocationDetail(5), navigator.destinations.last())
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = LocationListScreenModel(1, FakeLocationRepository(), navigator, this)
        model.onIntent(LocationListIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }
}
```

- [ ] **Step 6: Run tests to verify they fail**

```bash
./gradlew :feature:locations:desktopTest
```
Expected: FAIL — ScreenModels not defined yet

- [ ] **Step 7: Create `RegionListScreenModel.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListScreenModel.kt`:
```kotlin
package dev.huntdex.feature.locations.region

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.LocationRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class RegionListScreenModel(
    private val repository: LocationRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(RegionListState())
    val state: StateFlow<RegionListState> = _state.asStateFlow()

    init { loadRegions() }

    fun onIntent(intent: RegionListIntent) {
        when (intent) {
            is RegionListIntent.SelectRegion -> navigator.navigateTo(Destination.LocationList(intent.id))
            is RegionListIntent.Retry -> loadRegions()
        }
    }

    private fun loadRegions() {
        _state.update { it.copy(isLoading = true, error = null) }
        scope.launch {
            runCatching { repository.getAllRegions() }
                .onSuccess { regions -> _state.update { it.copy(regions = regions, isLoading = false) } }
                .onFailure { e -> _state.update { it.copy(isLoading = false, error = e.message) } }
        }
    }
}
```

- [ ] **Step 8: Create `LocationListScreenModel.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListScreenModel.kt`:
```kotlin
package dev.huntdex.feature.locations.list

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.LocationRepository
import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationListScreenModel(
    private val regionId: Int,
    private val repository: LocationRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(LocationListState())
    val state: StateFlow<LocationListState> = _state.asStateFlow()

    init { loadLocations() }

    fun onIntent(intent: LocationListIntent) {
        when (intent) {
            is LocationListIntent.Search -> _state.update { it.copy(searchQuery = intent.query) }
            is LocationListIntent.FilterByGeneration -> _state.update { it.copy(selectedGeneration = intent.generationId) }
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

- [ ] **Step 9: Run all tests**

```bash
./gradlew :feature:locations:desktopTest
```
Expected: All 8 tests PASS

- [ ] **Step 10: Commit**

```bash
git add feature/locations/build.gradle.kts \
        feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/ \
        feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/ \
        feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/
git commit -m "feat(locations): RegionListScreenModel and LocationListScreenModel"
```

---

### Task 5: LocationDetailScreenModel + tests

**Files:**
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailState.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailIntent.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModel.kt`
- Create: `feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModelTest.kt`

- [ ] **Step 1: Create MVI files**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailState.kt`:
```kotlin
package dev.huntdex.feature.locations.detail

import dev.huntdex.core.domain.model.AreaEncounter
import dev.huntdex.core.domain.model.LocationDetail

data class LocationDetailState(
    val detail: LocationDetail? = null,
    val isLoading: Boolean = true,
    val error: String? = null,
    val expandedAreaIds: Set<Int> = emptySet(),
    val selectedGeneration: Int? = null
) {
    fun encountersForArea(areaId: Int): List<AreaEncounter> {
        val area = detail?.areas?.firstOrNull { it.id == areaId } ?: return emptyList()
        return if (selectedGeneration == null) area.encounters
        else area.encounters  // generation filtering happens via version name mapping in PR 4
    }
}
```

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailIntent.kt`:
```kotlin
package dev.huntdex.feature.locations.detail

sealed interface LocationDetailIntent {
    data object NavigateBack : LocationDetailIntent
    data class ToggleArea(val areaId: Int) : LocationDetailIntent
    data class FilterByGeneration(val generationId: Int?) : LocationDetailIntent
    data object Retry : LocationDetailIntent
}
```

- [ ] **Step 2: Write failing tests**

`feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModelTest.kt`:
```kotlin
package dev.huntdex.feature.locations.detail

import dev.huntdex.core.navigation.AppNavigator
import dev.huntdex.core.navigation.Destination
import dev.huntdex.feature.locations.FakeLocationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
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
class LocationDetailScreenModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val testScope = TestScope(dispatcher)

    @Test
    fun `initial state is loading`() = testScope.runTest {
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        assertTrue(model.state.value.isLoading)
        assertNull(model.state.value.detail)
    }

    @Test
    fun `after init detail is loaded`() = testScope.runTest {
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertFalse(model.state.value.isLoading)
        assertNotNull(model.state.value.detail)
    }

    @Test
    fun `area is collapsed by default`() = testScope.runTest {
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        assertTrue(model.state.value.expandedAreaIds.isEmpty())
    }

    @Test
    fun `toggling area expands it`() = testScope.runTest {
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(LocationDetailIntent.ToggleArea(1))
        assertTrue(model.state.value.expandedAreaIds.contains(1))
    }

    @Test
    fun `toggling expanded area collapses it`() = testScope.runTest {
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), FakeAppNavigator(), this)
        dispatcher.scheduler.advanceUntilIdle()
        model.onIntent(LocationDetailIntent.ToggleArea(1))
        model.onIntent(LocationDetailIntent.ToggleArea(1))
        assertFalse(model.state.value.expandedAreaIds.contains(1))
    }

    @Test
    fun `navigate back calls navigator`() = testScope.runTest {
        val navigator = FakeAppNavigator()
        val model = LocationDetailScreenModel(1, FakeLocationRepository(), navigator, this)
        model.onIntent(LocationDetailIntent.NavigateBack)
        assertTrue(navigator.backCalled)
    }
}
```

- [ ] **Step 3: Run tests to verify they fail**

```bash
./gradlew :feature:locations:desktopTest
```
Expected: FAIL — `LocationDetailScreenModel` not defined yet

- [ ] **Step 4: Create `LocationDetailScreenModel.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreenModel.kt`:
```kotlin
package dev.huntdex.feature.locations.detail

import cafe.adriel.voyager.core.model.ScreenModel
import cafe.adriel.voyager.core.model.screenModelScope
import dev.huntdex.core.domain.repository.LocationRepository
import dev.huntdex.core.navigation.AppNavigator
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class LocationDetailScreenModel(
    private val locationId: Int,
    private val repository: LocationRepository,
    private val navigator: AppNavigator,
    private val externalScope: CoroutineScope? = null
) : ScreenModel {

    private val scope: CoroutineScope get() = externalScope ?: screenModelScope
    private val _state = MutableStateFlow(LocationDetailState())
    val state: StateFlow<LocationDetailState> = _state.asStateFlow()

    init { loadDetail() }

    fun onIntent(intent: LocationDetailIntent) {
        when (intent) {
            is LocationDetailIntent.NavigateBack -> navigator.navigateBack()
            is LocationDetailIntent.ToggleArea -> {
                val current = _state.value.expandedAreaIds
                val updated = if (intent.areaId in current) current - intent.areaId else current + intent.areaId
                _state.update { it.copy(expandedAreaIds = updated) }
            }
            is LocationDetailIntent.FilterByGeneration -> _state.update { it.copy(selectedGeneration = intent.generationId) }
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

- [ ] **Step 5: Run all tests**

```bash
./gradlew :feature:locations:desktopTest
```
Expected: All 14 tests PASS

- [ ] **Step 6: Commit**

```bash
git add feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/ \
        feature/locations/src/commonTest/kotlin/dev/huntdex/feature/locations/detail/
git commit -m "feat(locations): LocationDetailScreenModel with expand/collapse areas"
```

---

### Task 6: UI screens

**Files:**
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListScreen.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListScreen.kt`
- Create: `feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreen.kt`

- [ ] **Step 1: Create `RegionListScreen.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/region/RegionListScreen.kt`:
```kotlin
package dev.huntdex.feature.locations.region

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import dev.huntdex.core.domain.model.RegionEntry

data object RegionListScreen : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<RegionListScreenModel>()
        val state by model.state.collectAsState()
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Button(onClick = { model.onIntent(RegionListIntent.Retry) }) { Text("Retry") }
                }
            }
            else -> LazyColumn(Modifier.fillMaxSize()) {
                items(state.regions) { region ->
                    RegionRow(region = region, onClick = { model.onIntent(RegionListIntent.SelectRegion(region.id)) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun RegionRow(region: RegionEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(region.name.replaceFirstChar { it.uppercase() }) },
        supportingContent = { Text(region.generation.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 2: Create `LocationListScreen.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/list/LocationListScreen.kt`:
```kotlin
package dev.huntdex.feature.locations.list

import androidx.compose.foundation.clickable
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
import dev.huntdex.core.domain.model.LocationEntry
import org.koin.core.parameter.parametersOf

data class LocationListScreen(val regionId: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<LocationListScreenModel> { parametersOf(regionId) }
        val state by model.state.collectAsState()
        LocationListContent(state = state, onIntent = model::onIntent)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationListContent(state: LocationListState, onIntent: (LocationListIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Locations") },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LocationListIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(Modifier.fillMaxSize().padding(padding)) {
            OutlinedTextField(
                value = state.searchQuery,
                onValueChange = { onIntent(LocationListIntent.Search(it)) },
                label = { Text("Search locations") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)
            )
            when {
                state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
                state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(state.error!!)
                        Button(onClick = { onIntent(LocationListIntent.Retry) }) { Text("Retry") }
                    }
                }
                else -> LazyColumn(Modifier.fillMaxSize()) {
                    items(state.displayedLocations) { location ->
                        LocationRow(location = location, onClick = { onIntent(LocationListIntent.SelectLocation(location.id)) })
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationRow(location: LocationEntry, onClick: () -> Unit) {
    ListItem(
        headlineContent = { Text(location.name.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
        modifier = Modifier.clickable(onClick = onClick)
    )
}
```

- [ ] **Step 3: Create `LocationDetailScreen.kt`**

`feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/detail/LocationDetailScreen.kt`:
```kotlin
package dev.huntdex.feature.locations.detail

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.koin.koinScreenModel
import coil3.compose.AsyncImage
import dev.huntdex.core.domain.model.AreaEncounter
import dev.huntdex.core.domain.model.LocationArea
import org.koin.core.parameter.parametersOf

data class LocationDetailScreen(val id: Int) : Screen {
    @Composable
    override fun Content() {
        val model = koinScreenModel<LocationDetailScreenModel> { parametersOf(id) }
        val state by model.state.collectAsState()
        when {
            state.isLoading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            state.error != null -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!)
                    Button(onClick = { model.onIntent(LocationDetailIntent.Retry) }) { Text("Retry") }
                }
            }
            state.detail != null -> LocationDetailLoaded(state = state, onIntent = model::onIntent)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocationDetailLoaded(state: LocationDetailState, onIntent: (LocationDetailIntent) -> Unit) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(state.detail!!.name.replace('-', ' ').replaceFirstChar { it.uppercase() }) },
                navigationIcon = {
                    IconButton(onClick = { onIntent(LocationDetailIntent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        LazyColumn(Modifier.fillMaxSize().padding(padding)) {
            items(state.detail!!.areas) { area ->
                val isExpanded = area.id in state.expandedAreaIds
                AreaSection(
                    area = area,
                    isExpanded = isExpanded,
                    encounters = state.encountersForArea(area.id),
                    onToggle = { onIntent(LocationDetailIntent.ToggleArea(area.id)) }
                )
            }
        }
    }
}

@Composable
private fun AreaSection(area: LocationArea, isExpanded: Boolean, encounters: List<AreaEncounter>, onToggle: () -> Unit) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(onClick = onToggle).padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(area.name.replace('-', ' ').replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.titleSmall)
            Icon(if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown, contentDescription = null)
        }
        if (isExpanded) {
            encounters.forEach { encounter ->
                EncounterRow(encounter = encounter)
            }
        }
        HorizontalDivider()
    }
}

@Composable
private fun EncounterRow(encounter: AreaEncounter) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = "https://raw.githubusercontent.com/PokeAPI/sprites/master/sprites/pokemon/${encounter.pokemonId}.png",
            contentDescription = encounter.pokemonName,
            modifier = Modifier.size(40.dp)
        )
        Spacer(Modifier.width(8.dp))
        Column(Modifier.weight(1f)) {
            Text(encounter.pokemonName.replaceFirstChar { it.uppercase() }, style = MaterialTheme.typography.bodyMedium)
            Text("${encounter.method} • Lv ${encounter.minLevel}–${encounter.maxLevel}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("${encounter.chance}%", style = MaterialTheme.typography.bodySmall)
    }
}
```

- [ ] **Step 4: Verify compilation**

```bash
./gradlew :feature:locations:desktopMainKlibrary
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 5: Commit**

```bash
git add feature/locations/src/commonMain/kotlin/dev/huntdex/feature/locations/
git commit -m "feat(locations): RegionListScreen, LocationListScreen, LocationDetailScreen UI"
```

---

### Task 7: Wire all 3 entry points

- [ ] **Step 1: Add `feature:locations` dependency to all 3 entry points**

In `app/build.gradle.kts` → `dependencies {}`: `implementation(projects.feature.locations)`
In `desktopApp/build.gradle.kts` → `desktopMain.dependencies {}`: `implementation(projects.feature.locations)`
In `shared/build.gradle.kts` → iosMain dependencies: `implementation(projects.feature.locations)`

- [ ] **Step 2: Register ScreenModels in all 3 DI modules**

Add to each of `AppModule.kt`, `DesktopAppModule.kt`, `IosAppModule.kt`:
```kotlin
import dev.huntdex.feature.locations.region.RegionListScreenModel
import dev.huntdex.feature.locations.list.LocationListScreenModel
import dev.huntdex.feature.locations.detail.LocationDetailScreenModel

// Inside module { }:
factory { RegionListScreenModel(get(), get()) }
factory { params -> LocationListScreenModel(params.get(), get(), get()) }
factory { params -> LocationDetailScreenModel(params.get(), get(), get()) }
```

- [ ] **Step 3: Update all 3 DestinationMappers**

Add to Android `DestinationMapper.kt`, Desktop `DesktopDestinationMapper.kt`, iOS `IosDestinationMapper.kt`:
```kotlin
import dev.huntdex.feature.locations.region.RegionListScreen
import dev.huntdex.feature.locations.list.LocationListScreen
import dev.huntdex.feature.locations.detail.LocationDetailScreen

// Add cases (before the else branch):
is Destination.RegionList -> RegionListScreen
is Destination.LocationList -> LocationListScreen(regionId)
is Destination.LocationDetail -> LocationDetailScreen(id)
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
git commit -m "feat(locations): wire feature into Android, Desktop, and iOS entry points"
git push origin phase-2/locations
```
