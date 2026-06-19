# Phase 2 — Enciclopedia Completa: Design Spec

## Context

Phase 1 delivered the Pokédex MVP. Phase 2 completes the encyclopedia by adding moves, items, locations, and a global generation filter. It ships as 4 independent PRs reviewed separately.

---

## Architecture

All 4 PRs follow the same MVI pattern established in Phase 1 (`:feature:pokedex`). Each feature is autonomous: its own entities in `:core:domain`, its own repository in `:core:data`, its own screens in `:feature:X`. No cross-dependencies between Phase 2 features.

SQLDelight schema follows the `{entity}_entry` (list) + `{entity}_detail` (JSON blob TEXT) pattern already in place. New tables are added in `DatabaseDriverFactory` using the existing manual migration guard.

**PR dependency order:**
- PR 1, 2, 3 are fully independent — reviewable in any order
- PR 4 depends on the previous three (refactors their local filter state)

**Generation filter strategy — Option A (local per feature):**
PRs 1–3 each carry their own `selectedGeneration: StateFlow<Generation?>` in their ScreenModels, consistent with the pokedex pattern. PR 4 introduces `GlobalFilterRepository` in `:core:domain` and replaces the local state in all three features. The refactor is surgical: only the source of `selectedGeneration` changes, UI and screen logic are untouched.

---

## PR #1 — `:feature:moves`

### Screens

**MoveListScreen**
- Paginated move list (20 per page), search by name, filter chips for type (18 types) and damage class (physical / special / status)
- `selectedGeneration` in state filters relevant moves by generation

**MoveDetailScreen** — 3 sections:
1. **Stats:** type, damage class, power, accuracy, PP, priority, long effect description, flavor text
2. **Learned by:** first 10 Pokémon with sprite + name + learn method (level-up, TM, egg, tutor). "Ver más" button expands the full list
3. **Contest:** contest type (Cool/Beautiful/Cute/Clever/Tough), appeal, jam, contest effect description. Section is hidden if the move has no contest data (not all moves do)

### API Endpoints
| Endpoint | Used for |
|---|---|
| `GET /move?offset=&limit=20` | Paginated list |
| `GET /move/{id}` | Detail (includes `contest_type`, `contest_effect.url`, `learned_by_pokemon`) |
| `GET /contest-effect/{id}` | Contest effect — chained call on detail open |
| `GET /super-contest-effect/{id}` | Super contest effect — chained call on detail open |

### Cache
```sql
CREATE TABLE move_entry (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  type TEXT NOT NULL,
  damage_class TEXT NOT NULL
);

CREATE TABLE move_detail (
  id INTEGER PRIMARY KEY REFERENCES move_entry(id),
  data TEXT NOT NULL
);
```

### Tests
ScreenModel tests: load list, search, filter by type, filter by damage class, pagination, load detail, expand learned-by, contest section hidden when absent.

---

## PR #2 — `:feature:items`

### Screens

**ItemListScreen** — two tabs:
- **Objetos tab:** paginated list (20 per page) organized by pocket (Medicine, Battle, Key Items, etc.). Filter chips by pocket. Search by name applies to active tab.
- **Bayas tab:** full berry list (64 berries, no pagination needed). Search by name.

**ItemDetailScreen**
- Sprite, name, category/pocket, effect, flavor text, fling power + fling effect when applicable
- `held_by_pokemon` field is omitted (too noisy for Phase 2)

**BerryDetailScreen** (separate screen from ItemDetailScreen)
- Berry sprite, growth time, soil dryness, flavors (spicy/dry/sweet/bitter/sour with numeric values), contest stats, berry item effect

### API Endpoints
| Endpoint | Used for |
|---|---|
| `GET /item?offset=&limit=20` | Paginated item list |
| `GET /item/{id}` | Item detail |
| `GET /berry` | Full berry list (no pagination) |
| `GET /berry/{id}` | Berry detail (includes reference to its `item` for sprite and flavor text) |

### Cache
```sql
CREATE TABLE item_entry (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  category TEXT NOT NULL,
  pocket TEXT NOT NULL
);

CREATE TABLE item_detail (
  id INTEGER PRIMARY KEY REFERENCES item_entry(id),
  data TEXT NOT NULL
);

CREATE TABLE berry_detail (
  id INTEGER PRIMARY KEY,
  data TEXT NOT NULL
);
```

### Tests
ScreenModel tests: tab switching, search per tab, filter by pocket, load item detail, load berry detail.

---

## PR #3 — `:feature:locations`

### Screens

**RegionListScreen**
- Fixed list of 9 regions (Kanto → Paldea). No pagination, no search.
- Each entry shows region name and associated generation.

**LocationListScreen**
- Shown after selecting a region. Paginated location list with search by name.
- `selectedGeneration` prefilters to show only locations from that generation.

**LocationDetailScreen**
- Location name + areas as expandable sections (collapsed by default).
- Each area section shows an encounter table: Pokémon (sprite + name), game version, encounter method (walking/surfing/fishing/etc.), encounter chance, min/max level.
- `selectedGeneration` filters which game versions are shown in encounter tables.

### API Endpoints
| Endpoint | Used for |
|---|---|
| `GET /region` | Region list |
| `GET /region/{id}` | Region detail with `locations` list |
| `GET /location/{id}` | Location detail with `areas` list |
| `GET /location-area/{id}` | Pokémon encounters by version + method |

Area calls within a single location detail are resolved in parallel (`async/await`) to reduce load time.

### Cache
```sql
CREATE TABLE region_entry (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL
);

CREATE TABLE location_entry (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  region_id INTEGER NOT NULL REFERENCES region_entry(id)
);

CREATE TABLE location_detail (
  id INTEGER PRIMARY KEY REFERENCES location_entry(id),
  data TEXT NOT NULL  -- includes areas + encounters as JSON
);
```

### Tests
ScreenModel tests: region list load, location list load + generation filter, location detail load, expand/collapse areas, encounter version filter.

---

## PR #4 — `:feature:games`

### Screens

**GenerationListScreen**
- List of generations I–IX with their associated versions (e.g. Gen I: Red, Blue, Yellow).
- Selecting a generation sets it as the active global filter.
- Visual indicator showing which generation is currently active.
- No detail screen — purpose is selection, not navigation.

### GlobalFilterRepository (new in `:core:domain`)

```kotlin
interface GlobalFilterRepository {
    val selectedGeneration: StateFlow<Generation?>
    suspend fun setGeneration(generation: Generation?)
}
```

Implemented in `:core:data` as an in-memory `MutableStateFlow`. No persistence required in Phase 2 — filter resets to "all generations" on app restart.

### Refactor in moves / items / locations (surgical)
- Remove local `selectedGeneration: MutableStateFlow` from each ScreenModel
- Inject `GlobalFilterRepository` via Koin, observe `selectedGeneration` from it
- UI unchanged — filter chips still work, now write to global state

### API Endpoints
| Endpoint | Used for |
|---|---|
| `GET /generation` | Generation list with `version_groups` |
| `GET /version-group/{id}` | Specific versions within a group |

### Cache
```sql
CREATE TABLE generation_entry (
  id INTEGER PRIMARY KEY,
  name TEXT NOT NULL,
  versions TEXT NOT NULL  -- JSON array, static data
);
```

### Tests
ScreenModel tests: generation list load, setGeneration propagates to StateFlow. Smoke test verifying filter reflects across moves/items/locations ScreenModels.

---

## Verification Criteria (per plan)

Phase 2 is complete when:
- Each encyclopedia section navigates correctly end-to-end on Android and Desktop
- Generation filter works consistently across all sections (after PR 4)
- Visited moves, items, and locations remain available offline
