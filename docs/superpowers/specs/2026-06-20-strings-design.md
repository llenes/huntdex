# Strings PR — Design Spec

**Date:** 2026-06-20
**Branch:** `phase-2/strings`
**Scope:** Extract all hardcoded UI strings from existing screens to localized string resources, and define the mechanism for serving dynamic PokéAPI content in the user's language.

---

## Goals

1. Extract all static UI strings to `composeResources` XML files with English and Spanish translations.
2. Introduce a `LocaleProvider` that reads the system locale and exposes it to the data layer.
3. Use that locale to filter flavor text and effect text from PokéAPI instead of the current hardcoded `"en"`.

## Out of Scope

- Localized Pokémon names: requires adding `PokemonSpeciesDto.names` and changing list + detail simultaneously; deferred to a dedicated localization PR.
- Type names ("fire", "water"…) and damage class names ("physical"…): these are API slugs used as filter identifiers; will be localized in a types PR.
- `DesktopMainScreen` contentDescriptions ("Collapse/Expand navigation"): require enabling Compose in `desktopApp`; deferred to an accessibility PR.

---

## Supported Languages

- **`values/`** — English (base / fallback)
- **`values-es/`** — Spanish

The architecture is ready for any additional PokéAPI-supported language by adding a `values-xx/` directory.

---

## Architecture

### 1. Static strings — `composeResources`

Compose Multiplatform generates `Res.string.xxx` from `strings.xml` files. The resource system automatically picks the correct directory (`values/` or `values-es/`) based on the device locale at composition time — no extra code needed in the screens.

**Module distribution (Option B — modular):**

| Module | Contents |
|---|---|
| `core/ui` | Strings shared across features: Retry, Back, Loading |
| `feature/pokedex` | Pokédex-specific strings |
| `feature/moves` | Moves-specific strings |

`core/ui` requires the `compose.multiplatform` and `kotlin.compose` plugins to be added to its `build.gradle.kts` (currently only has `kotlin.multiplatform`).

### 2. Dynamic text — `LocaleProvider`

```
core/common
└── commonMain  →  expect class LocaleProvider()  { fun languageCode(): String }
└── androidMain →  actual: Locale.getDefault().language
└── desktopMain →  actual: java.util.Locale.getDefault().language
└── iosMain     →  actual: NSLocale.preferredLanguages.firstOrNull()?.substringBefore('-') ?: "en"
```

`LocaleProvider()` takes no constructor parameters on any platform.

`DataModule.kt` (commonMain) registers:
```kotlin
single { LocaleProvider() }
```

`PokemonRepositoryImpl` and `MoveRepositoryImpl` inject `LocaleProvider` via constructor and pass `localeProvider.languageCode()` to their mappers on each call. The language code is read on every fetch (not cached), so a system language change is reflected on the next data load.

### 3. Fallback pattern in mappers

```kotlin
entries.firstOrNull { it.language.name == languageCode }
    ?: entries.firstOrNull { it.language.name == "en" }
    ?: ""
```

Applied to:
- `PokemonMapper.toPokemonDetail()` — `flavorTextEntries` (1 occurrence)
- `MoveMapper.toMoveDetail()` — `effectEntries`, `flavorTextEntries`, `contestEffect.effectEntries` (3 occurrences)

---

## String Inventory

### `core/ui` — shared

| Key | EN | ES |
|---|---|---|
| `string_retry` | Retry | Reintentar |
| `string_back` | Back | Atrás |
| `string_loading` | Loading… | Cargando… |

### `feature/pokedex`

| Key | EN | ES |
|---|---|---|
| `pokedex_tab_title` | Pokédex | Pokédex |
| `search_placeholder` | Search Pokémon… | Buscar Pokémon… |
| `filter_all` | All | Todos |
| `generation_label` | Gen %s | Gen %s |
| `section_types` | Types | Tipos |
| `section_stats` | Stats | Estadísticas |
| `section_abilities` | Abilities | Habilidades |
| `section_evolution` | Evolution | Evolución |
| `section_locations` | Locations | Lugares |
| `ability_hidden` | (Hidden) | (Oculta) |
| `stat_hp` | HP | PS |
| `stat_attack` | Attack | Ataque |
| `stat_defense` | Defense | Defensa |
| `stat_sp_atk` | Sp. Atk | At. Esp. |
| `stat_sp_def` | Sp. Def | Def. Esp. |
| `stat_speed` | Speed | Velocidad |

### `feature/moves`

| Key | EN | ES |
|---|---|---|
| `moves_tab_title` | Moves | Movimientos |
| `search_placeholder` | Search moves… | Buscar movimientos… |
| `section_stats` | Stats | Estadísticas |
| `stat_type` | Type | Tipo |
| `stat_category` | Category | Categoría |
| `stat_power` | Power | Potencia |
| `stat_accuracy` | Accuracy | Precisión |
| `stat_pp` | PP | PP |
| `stat_priority` | Priority | Prioridad |
| `section_learned_by` | Learned by Pokémon | Aprendido por Pokémon |
| `move_see_more` | See more | Ver más |
| `section_contest` | Contest | Concurso |
| `contest_appeal` | Appeal | Atractivo |
| `contest_jam` | Jam | Bloqueo |

---

## Files Affected

### New (10)

```
core/common/src/commonMain/kotlin/dev/huntdex/core/common/LocaleProvider.kt
core/common/src/androidMain/kotlin/dev/huntdex/core/common/LocaleProvider.kt
core/common/src/desktopMain/kotlin/dev/huntdex/core/common/LocaleProvider.kt
core/common/src/iosMain/kotlin/dev/huntdex/core/common/LocaleProvider.kt
core/ui/src/commonMain/composeResources/values/strings.xml
core/ui/src/commonMain/composeResources/values-es/strings.xml
feature/pokedex/src/commonMain/composeResources/values/strings.xml
feature/pokedex/src/commonMain/composeResources/values-es/strings.xml
feature/moves/src/commonMain/composeResources/values/strings.xml
feature/moves/src/commonMain/composeResources/values-es/strings.xml
```

### Modified (12)

```
core/ui/build.gradle.kts
core/data/src/commonMain/kotlin/dev/huntdex/core/data/di/DataModule.kt
core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/PokemonMapper.kt
core/data/src/commonMain/kotlin/dev/huntdex/core/data/mapper/MoveMapper.kt
core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/PokemonRepositoryImpl.kt
core/data/src/commonMain/kotlin/dev/huntdex/core/data/repository/MoveRepositoryImpl.kt
feature/pokedex/src/commonMain/kotlin/dev/huntdex/feature/pokedex/list/PokemonListScreen.kt
feature/pokedex/src/commonMain/kotlin/dev/huntdex/feature/pokedex/list/PokedexTab.kt
feature/pokedex/src/commonMain/kotlin/dev/huntdex/feature/pokedex/detail/PokemonDetailScreen.kt
feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MoveListScreen.kt
feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/list/MovesTab.kt
feature/moves/src/commonMain/kotlin/dev/huntdex/feature/moves/detail/MoveDetailScreen.kt
```

**Total: 22 files** — within the 20–25 file target.

---

## Implementation Notes

- `Res.string.xxx` from `core/ui` is referenced from other modules using the generated package prefix: `dev.huntdex.core.ui.generated.resources.Res`. This is verbose but correct; Compose generates the alias automatically when the `compose.multiplatform` plugin is applied to the consuming module.
- `generation_label` uses a format argument: `stringResource(Res.string.generation_label, romanNumeral)` with `%s` in the XML.
- Both `PokedexTab` and `MovesTab` declare `options` with `@Composable get()`, so `stringResource(Res.string.xxx)` can be called directly inside the getter without any workaround.
