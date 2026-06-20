# Huntdex

A Kotlin Multiplatform (KMP) + Compose Multiplatform application that combines a complete **Pokémon encyclopedia** powered by PokéAPI with a dedicated **shiny hunting tracker** for logging encounters, sessions, and statistics.

Primary targets: **Android**, **Desktop (JVM)**, and **iOS**. Web is planned for a later phase.

---

## Tech Stack

| Layer | Technology | Purpose |
|---|---|---|
| UI | [Compose Multiplatform](https://www.jetbrains.com/lp/compose-multiplatform/) 1.7.0 | Shared declarative UI across Android, Desktop, and iOS |
| Language | [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html) 2.0.21 | Single codebase compiled to Android (JVM), Desktop (JVM), and iOS (Kotlin/Native) |
| Architecture | Clean Architecture + MVI | Strict separation of concerns; unidirectional data flow |
| Navigation | [Voyager](https://voyager.adriel.cafe/) 1.1.0-beta02 | Screen-based navigation, isolated behind `AppNavigator` interface |
| Local Database | [SQLDelight](https://cashapp.github.io/sqldelight/) 2.0.2 | Typesafe SQL, KMP-native, offline-first cache |
| Networking | [Ktor Client](https://ktor.io/docs/client-create-new-application.html) 3.0.3 | KMP HTTP client for PokéAPI v2 |
| Serialization | [kotlinx.serialization](https://github.com/Kotlin/kotlinx.serialization) 1.7.3 | JSON parsing for API responses and SQLDelight JSON cache |
| Image Loading | [Coil 3](https://coil-kt.github.io/coil/) 3.0.4 | KMP async image loading with Ktor network backend |
| DI | [Koin](https://insert-koin.io/) 3.5.6 | Multiplatform-native dependency injection |
| Auth & Sync | [Supabase](https://supabase.com/) *(Phase 4)* | Optional user accounts and cross-device sync |
| Build | Gradle 8.9 + Kotlin DSL + Version Catalogs | Reproducible builds, typesafe dependency management |
| CI | GitHub Actions | Compile + unit test on every push/PR |

---

## Architecture

### Overview

Huntdex follows **Clean Architecture** with a strict module dependency graph and **MVI (Model-View-Intent)** as the UI pattern.

```
┌─────────────────────────────────────────────────────┐
│                        :app                          │  Android entry point
│                      :desktopApp                     │  Desktop entry point
└────────────────────┬────────────────────────────────┘
                     │ depends on
┌────────────────────▼────────────────────────────────┐
│                  :feature:*                          │  Feature modules
│  (pokedex | moves | items | locations | games |      │
│   hunting | profile)                                 │
└──────┬────────────┬────────────────┬────────────────┘
       │            │                │
       ▼            ▼                ▼
 :core:domain  :core:navigation  :core:ui
       ▲
       │ implements
 :core:data          :core:common
```

**Dependency rule (enforced by Gradle module configuration):**
- `:feature:*` modules can only import `:core:domain`, `:core:navigation`, and `:core:ui`
- `:core:data` implements the interfaces defined in `:core:domain`
- `:app` / `:desktopApp` are the **only** modules that wire everything together

### MVI Pattern

Every feature screen follows the same structure:

```
Screen (Composable)
  │  observes State via StateFlow
  │  emits Intent on user action
  ▼
ScreenModel (Voyager)
  │  reduces Intent → new State
  │  calls AppNavigator for navigation
  ▼
Repository (core:domain interface)
  ▼
DataSource (core:data implementation)
  ├── SQLDelight (local cache)
  └── Ktor (PokéAPI v2)
```

```kotlin
// Immutable snapshot of the screen
data class PokemonListState(
    val pokemon: List<PokemonEntry> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val searchQuery: String = ""
)

// All possible user actions
sealed interface PokemonListIntent {
    data class Search(val query: String) : PokemonListIntent
    data class SelectPokemon(val id: Int) : PokemonListIntent
    data object LoadNextPage : PokemonListIntent
    data object Retry : PokemonListIntent
}
```

The UI never calls navigation directly — it emits `Intent.SelectPokemon(id)` and the ScreenModel decides to navigate.

### Navigation Contract

`:core:navigation` is the single source of truth for all destinations. It has **zero Voyager imports** — it only exports a sealed class and an interface.

```kotlin
// All routes in one place
sealed class Destination {
    data object PokemonList : Destination()
    data class PokemonDetail(val id: Int) : Destination()
    data object HuntingList : Destination()
    data class HuntingSessionDetail(val sessionId: String) : Destination()
    // ...
}

// Contract that feature modules inject
interface AppNavigator {
    fun navigateTo(destination: Destination)
    fun navigateBack()
    fun popTo(destination: Destination, inclusive: Boolean = false)
    fun <T> setResult(key: String, value: T)
    fun <T> getResult(key: String): Flow<T?>
}
```

`VoyagerNavigatorAdapter` (in `:app`) and `DesktopNavigatorAdapter` (in `:desktopApp`) are the only places that know about Voyager. Swapping the navigation library requires only rewriting these two adapters.

### Data Layer

**Cache strategy:** Network → Cache → UI (offline-first; progressive for list data)

List screens (Pokémon list, Moves list) follow the cache-first path:
1. Query SQLDelight first; serve cached rows immediately if present
2. If no rows, fetch from PokéAPI, persist, then serve

Detail screens always fetch from PokéAPI:
- Detail data contains user-language-specific text (flavor text, move descriptions) via `LocaleProvider`; caching would serve the wrong language after an OS locale change
- `pokemon_detail` and `move_detail` tables still exist for a future locale-keyed cache

Complex entities (Pokémon has 20+ nested fields) are stored as JSON blobs in a `TEXT` column. Fields used for search/filter (name, type, generation) are indexed as separate columns.

---

## Project Structure

```
huntdex/
├── app/                          # Android application entry point
│   └── src/main/
│       ├── AndroidManifest.xml
│       └── kotlin/dev/huntdex/app/
│           ├── HuntdexApplication.kt    # Koin init
│           ├── MainActivity.kt
│           ├── di/AppModule.kt          # Koin wiring for :app
│           ├── navigation/
│           │   ├── VoyagerNavigatorAdapter.kt
│           │   └── DestinationMapper.kt
│           └── screens/
│               ├── MainScreen.kt        # Root: TabNavigator + floating bottom nav
│               └── DetailScreen.kt
│
├── iosApp/                       # iOS application entry point (Xcode project)
│   ├── Podfile                          # CocoaPods: pod 'shared'
│   ├── Podfile.lock
│   └── iosApp/
│       ├── iOSApp.swift                 # SwiftUI @main entry point
│       └── ContentView.swift            # Hosts MainViewController (Compose)
│
├── desktopApp/                   # Desktop (JVM) application entry point
│   └── src/desktopMain/kotlin/dev/huntdex/desktopapp/
│       ├── main.kt                      # Compose Desktop application {}
│       ├── di/DesktopAppModule.kt
│       ├── navigation/
│       │   ├── DesktopNavigatorAdapter.kt
│       │   └── DesktopDestinationMapper.kt
│       └── screens/
│           └── DesktopMainScreen.kt     # Root: collapsible NavigationRail + TabNavigator
│
├── core/
│   ├── domain/                   # Entities, use cases, repository interfaces
│   ├── data/                     # Repository implementations, Ktor, SQLDelight
│   │   └── src/
│   │       ├── commonMain/
│   │       │   ├── kotlin/dev/huntdex/core/data/
│   │       │   │   ├── network/HttpClientFactory.kt   # Ktor setup
│   │       │   │   └── di/DataModule.kt
│   │       │   └── sqldelight/dev/huntdex/core/data/db/
│   │       │       └── HuntdexDatabase.sq             # SQL schema
│   │       ├── androidMain/      # Android SQLite driver + Koin module
│   │       └── desktopMain/      # JDBC SQLite driver + Koin module
│   ├── navigation/               # Destination + AppNavigator (pure KMP, no Voyager)
│   ├── ui/                       # Design system: colors, typography, shared components, shared EN+ES string resources
│   └── common/                   # Extensions, Result wrapper, coroutine utils, LocaleProvider (OS language code, KMP expect/actual)
│
├── shared/                       # iOS shared framework (Compose Multiplatform for iOS)
│   ├── shared.podspec                   # CocoaPods spec — built by Gradle, consumed by Xcode
│   └── src/
│       ├── commonMain/           # Shared Compose UI logic
│       └── iosMain/              # MainViewController, iOS DI module, iOS navigator adapter, IosMainScreen
│
├── feature/
│   ├── pokedex/                  # Pokémon list/detail, evolutions, locations by game
│   ├── moves/                    # Moves, TMs/HMs, contest data
│   ├── items/                    # Items by pocket, berries
│   ├── locations/                # Regions, areas, reverse Pokémon lookup
│   ├── games/                    # Generations, versions — global filter layer
│   ├── hunting/                  # Shiny hunting sessions, counter, daily log
│   └── profile/                  # Hunter profile, global stats, sync
│
├── gradle/
│   └── libs.versions.toml        # Version catalog (single source of truth for deps)
├── .github/workflows/ci.yml      # Build + test on push/PR
└── local.properties.example      # Template for Android SDK path
```

---

## Data Flow

### PokéAPI Encyclopedia (read path)

```
User opens Pokémon list
        │
        ▼
PokemonListScreenModel.onIntent(LoadNextPage)
        │
        ▼
GetPokemonListUseCase (core:domain)
        │
        ▼
PokemonRepository.getList(offset, limit) (interface in core:domain)
        │
        ├─ SQLDelight: SELECT * FROM pokemon_entry LIMIT ? OFFSET ?
        │       │
        │       ├─ rows found → emit cached list immediately
        │       │
        │       └─ no rows → fetch from PokéAPI
        │               │
        │               ▼
        │         Ktor GET /api/v2/pokemon?offset=&limit=
        │               │
        │               ▼
        │         INSERT INTO pokemon_entry (id, name, sprite_url, cached_at)
        │               │
        │               ▼
        │         emit freshly fetched list
        │
        ▼
PokemonListState(pokemon = [...])  →  UI recomposes
```

### Shiny Hunting Session (write path)

```
User taps "+" on shiny hunt counter
        │
        ▼
HuntingSessionScreenModel.onIntent(IncrementCounter)
        │
        ▼
IncrementHuntCountUseCase
        │
        ▼
HuntSessionRepository.increment(sessionId) (local only, never hits PokéAPI)
        │
        ▼
SQLDelight: UPDATE hunt_session SET count = count + 1 WHERE id = ?
        │
        ▼
StateFlow emits new HuntingSessionState(count = N+1)  →  UI recomposes
```

---

## Database Schema

```sql
-- Pokémon list cache (paginated)
CREATE TABLE pokemon_entry (
  id         INTEGER PRIMARY KEY,
  name       TEXT    NOT NULL,
  sprite_url TEXT    NOT NULL
);

-- Full detail stored as JSON blob (avoids schema migrations for PokéAPI changes)
-- Includes types, stats, abilities, evolution chain, and location encounters
CREATE TABLE pokemon_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL   -- PokemonDetail serialized via kotlinx.serialization
);

-- Moves list cache (paginated)
CREATE TABLE move_entry (
  id   INTEGER PRIMARY KEY,
  name TEXT    NOT NULL
);

-- Full move detail stored as JSON blob
CREATE TABLE move_detail (
  id   INTEGER PRIMARY KEY,
  data TEXT    NOT NULL   -- MoveDetail serialized via kotlinx.serialization
);

-- Shiny hunting sessions
CREATE TABLE hunt_session (
  id           TEXT    PRIMARY KEY,   -- UUID
  pokemon_id   INTEGER NOT NULL,
  pokemon_name TEXT    NOT NULL,
  game         TEXT    NOT NULL,
  method       TEXT    NOT NULL,
  mode         TEXT    NOT NULL CHECK(mode IN ('simple', 'advanced')),
  count        INTEGER NOT NULL DEFAULT 0,
  status       TEXT    NOT NULL CHECK(status IN ('active', 'completed', 'abandoned')),
  notes        TEXT,
  charm_active INTEGER NOT NULL DEFAULT 0,  -- shiny charm boolean
  started_at   INTEGER NOT NULL,
  completed_at INTEGER
);

-- Daily encounter log (advanced mode only)
CREATE TABLE hunt_daily_log (
  id         INTEGER PRIMARY KEY AUTOINCREMENT,
  session_id TEXT    NOT NULL REFERENCES hunt_session(id) ON DELETE CASCADE,
  date       INTEGER NOT NULL,
  encounters INTEGER NOT NULL DEFAULT 0
);
```

---

## Development Phases

| Phase | Status | Goal |
|---|---|---|
| **Phase 0 — Infrastructure** | ✅ Complete | KMP project compiles, navigation contract established, two test screens run on Android + Desktop + iOS simulator |
| **Phase 1 — Pokédex MVP** | ✅ Complete | Paginated Pokémon list with search + generation filter; full detail screen (stats, types, abilities, evolutions, locations by game); progressive SQLDelight cache |
| **Phase 2 — Full Encyclopedia** | 🚧 In Progress | Moves list + detail ✅ · Bottom/rail navigation ✅ · Localized strings EN+ES ✅ · Items, Locations, Games ⬜ |
| **Phase 3 — Shiny Hunting** | ⬜ Planned | Session management, counters, daily logs, hunter profile with global stats |
| **Phase 4 — Auth & Sync** | ⬜ Planned | Supabase auth (email + Google/Apple), cross-device sync for hunting data |
| **Phase 5 — Web** | ⬜ Planned | Kotlin/WASM web target, deep links |

---

## Improvement Suggestions

### Near-term (Phase 2)

- **`Result<T>` wrapper in `core:common`** — ScreenModels currently use `runCatching` inline. Extract a shared `Result<T>` sealed class so all features handle loading/success/error uniformly.
- **Pagination abstraction** — Both `PokemonListScreenModel` and `MoveListScreenModel` duplicate offset-based pagination logic. Extract a `Pager` abstraction in `:core:common` before adding more list screens (Items, Locations).
- **Generation filter cache** — `getPokemonByGeneration` is network-only. Cache generation→Pokémon mappings in a `generation_pokemon` table so repeated filter changes don't re-hit the API.
- **Search across all Pokémon** — Current search filters only loaded pages. A proper search would query `pokemon_entry` via the `searchPokemon` SQL query (already defined in schema), which searches the full local cache.

### Architecture

- **`popTo(inclusive = true)` not yet implemented** — `VoyagerNavigatorAdapter.popTo` has a TODO. The inclusive case needs an extra `navigator.pop()` call after `popUntil`. Low priority until multi-level deep navigation is needed.
- **Desktop `DatabaseDriverFactory` creates schema on every launch** — `HuntdexDatabase.Schema.create(driver)` in the Desktop driver will throw on the second launch when the file already exists. Replace with `migrateOrCreate` or a manual version-check (`PRAGMA user_version`) before Phase 3 ships.
- **Stale `Navigator` reference on Activity recreation** — The `DesktopNavigatorAdapter` and `VoyagerNavigatorAdapter` hold a direct `Navigator` reference. On Android config change, the old adapter (still in Koin as a `single`) holds a reference to the previous `Navigator`. Safe for now (single Activity), but revisit before multi-window Desktop support.
- **`Divider` → `HorizontalDivider`** — `PokemonDetailScreen` uses the deprecated `Divider` composable. Rename to `HorizontalDivider` (available in Material3).
- **`@Destination` annotation processor** — Consider generating the `Destination.toScreen()` mapper at compile time to eliminate the `when` boilerplate as destinations grow.

### Testing

- **Repository integration tests** — `PokemonRepositoryImpl` and `MoveRepositoryImpl` have no integration tests. Highest-value additions: cache-hit/miss paths for list screens using an in-memory SQLite driver, and `flattenEvolutionChain` with branching evolutions (e.g., Eevee). Mapper language-selection logic is already covered by 8 tests in `MapperLanguageTest`.
- **Error path coverage** — ScreenModel tests cover only the happy path. Add tests for `onFailure` branches and `Retry` intent to verify error → loading → success transitions.

### CI/CD

- **Cache invalidation** — The `gradle/actions/setup-gradle` cache uses default keys. Add `hashFiles('gradle/libs.versions.toml')` to the key so the cache busts when dependencies change.
- **Lint step** — Add `./gradlew :app:lintDebug` to catch Android lint issues early.
- **Desktop packaging** — Add `./gradlew :desktopApp:packageDistributionForCurrentOS` to produce a distributable Desktop artifact from CI.
- **PR preview APK** — Consider uploading the debug APK as a CI artifact so reviewers can install it directly from the GitHub Actions run.

---

## Getting Started

### Prerequisites

- JDK 17+ (via SDKMAN, Homebrew, or official installer)
- Android Studio Hedgehog or later (for Android target)
- Android SDK (API 26+)
- Xcode 15+ with CocoaPods (`gem install cocoapods`) — for iOS target

### Setup

```bash
git clone https://github.com/llenes/huntdex.git
cd huntdex
cp local.properties.example local.properties
# Edit local.properties and set sdk.dir to your Android SDK path

./gradlew build          # Build all modules
./gradlew :app:installDebug              # Install Android app on connected device/emulator
./gradlew :desktopApp:run                # Launch Desktop app
```

### iOS Setup

```bash
# 1. Generate the Kotlin/Native framework stub
./gradlew :shared:generateDummyFramework

# 2. Install CocoaPods dependencies
cd iosApp && pod install

# 3. Open the workspace (NOT the .xcodeproj)
open iosApp.xcworkspace
```

Then select an iPhone simulator in Xcode and press **⌘R** to build and run.

> **Note for SDKMAN users:** Xcode's script phases run with a restricted PATH that does not include SDKMAN's Java. The `shared.podspec` already exports `JAVA_HOME` from `$HOME/.sdkman/candidates/java/current` automatically. If you use a different Java installation method and see "Unable to locate a Java Runtime", update the JAVA_HOME detection block in `shared/shared.podspec`.

### Running Tests

```bash
./gradlew :core:domain:desktopTest         # Domain model unit tests (3 tests)
./gradlew :core:common:desktopTest         # LocaleProvider unit tests (2 tests)
./gradlew :core:data:desktopTest           # Mapper language + fallback unit tests (8 tests)
./gradlew :feature:pokedex:desktopTest     # Pokédex ScreenModel unit tests (8 tests)
./gradlew :core:domain:testDebugUnitTest   # Android unit tests
./gradlew build                            # Full build + all tests
```

---

## Contributing

This project is in active early development. The module structure and interfaces are intentionally kept stable — new features should be added as new `:feature:*` modules following the existing MVI pattern, not by modifying `:core:*`.

Before contributing:
1. Make sure `./gradlew build` passes locally
2. Follow the module dependency rule: `:feature:*` → only `:core:domain`, `:core:navigation`, `:core:ui`
3. New screens must use Voyager `ScreenModel` (not Android `ViewModel`) and receive `AppNavigator` via Koin injection

---

## License

TBD
