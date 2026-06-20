# Strings PR — Design Spec

**Date:** 2026-06-20  
**Branch:** `phase-2/strings`  
**Scope:** Extraer strings hardcodeados de las pantallas existentes y definir el mecanismo para servir texto dinámico de PokéAPI en el idioma del usuario.

---

## Objetivos

1. Extraer todos los strings de UI estáticos hardcodeados a recursos de string localizados (`composeResources`).
2. Proveer un `LocaleProvider` que lea el locale del sistema y lo exponga a la capa de datos.
3. Usar ese locale para filtrar flavor text y effect text de PokéAPI en lugar del `"en"` hardcodeado actual.

## Fuera de scope

- Nombres localizados de Pokémon (requieren `PokemonSpeciesDto.names` y cambios en lista+detalle simultáneamente).
- Nombres de tipo ("fire", "water"…) y damage class ("physical"…): son slugs de API usados como identificadores de filtro; se localizarán en un PR de tipos.
- ContentDescriptions de `DesktopMainScreen` ("Collapse/Expand navigation"): requieren activar Compose en `desktopApp`; quedan para un PR de accesibilidad.

---

## Idiomas soportados

- **`values/`** — inglés (base / fallback)
- **`values-es/`** — español

La arquitectura queda lista para añadir cualquier idioma soportado por PokéAPI agregando un directorio `values-xx/`.

---

## Arquitectura

### 1. Strings estáticos — `composeResources`

Compose Multiplatform genera `Res.string.xxx` desde archivos `strings.xml`. El sistema elige el directorio (`values/` o `values-es/`) según el locale del dispositivo en tiempo de composición, sin código adicional en las pantallas.

**Distribución por módulo (Opción B — modular):**

| Módulo | Contenido |
|---|---|
| `core/ui` | Strings compartidos entre features: Retry, Back, Loading |
| `feature/pokedex` | Strings específicos de Pokédex |
| `feature/moves` | Strings específicos de Moves |

`core/ui` necesita que se le activen los plugins `compose.multiplatform` y `kotlin.compose` en su `build.gradle.kts` (actualmente solo tiene `kotlin.multiplatform`).

### 2. Texto dinámico — `LocaleProvider`

```
core/common
└── commonMain  →  expect class LocaleProvider()  { fun languageCode(): String }
└── androidMain →  actual: Locale.getDefault().language
└── desktopMain →  actual: java.util.Locale.getDefault().language
└── iosMain     →  actual: NSLocale.preferredLanguages.firstOrNull()?.substringBefore('-') ?: "en"
```

`LocaleProvider()` no recibe parámetros en ninguna plataforma.

`DataModule.kt` (commonMain) registra:
```kotlin
single { LocaleProvider() }
```

`PokemonRepositoryImpl` y `MoveRepositoryImpl` inyectan `LocaleProvider` por constructor y pasan `localeProvider.languageCode()` a sus mappers en cada llamada (sin cachear, para reflejar cambios de idioma en el próximo fetch).

### 3. Patrón de fallback en mappers

```kotlin
entries.firstOrNull { it.language.name == languageCode }
    ?: entries.firstOrNull { it.language.name == "en" }
    ?: ""
```

Aplica a:
- `PokemonMapper.toPokemonDetail()` — `flavorTextEntries` (1 ocurrencia)
- `MoveMapper.toMoveDetail()` — `effectEntries`, `flavorTextEntries`, `contestEffect.effectEntries` (3 ocurrencias)

---

## Inventario de strings

### `core/ui` — compartidos

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

## Archivos afectados

### Nuevos (6)

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

*(10 archivos nuevos — los 4 de LocaleProvider cuentan como un conjunto lógico)*

### Modificados (12)

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

**Total: 22 archivos** — dentro del target de 20–25.

---

## Notas de implementación

- `Res.string.xxx` de `core/ui` se referencia desde otros módulos con el prefijo de paquete generado: `dev.huntdex.core.ui.generated.resources.Res`. El import es verboso pero correcto; Compose genera el alias automáticamente si se usa el plugin `compose.multiplatform`.
- `generation_label` usa `stringResource(Res.string.generation_label, romanNumeral)` con argumento de formato `%s`.
- Los tabs de Voyager (`PokedexTab`, `MovesTab`) declaran `options` con `@Composable get()`, por lo que `stringResource(Res.string.xxx)` se puede usar directamente dentro del getter sin ningún workaround.
