# Hakuna Kuinama

> *Hakuna kuinama* — "no sleeping on food". A Kenyan bachelor food & menu builder: quick,
> affordable, local meals you can actually cook tonight.

An offline-first Android app that answers three questions for a student with a tight week
and a tight budget:

1. **What should I eat right now?** — a time-of-day suggestion (breakfast / lunch / dinner).
2. **What can I cook with what I already have?** — tick the ingredients in your kitchen and
   get ranked recipes, cheapest-first.
3. **What will the week cost me?** — a merged shopping list in KES from the meals you picked.

## Status

Phases 1–3 of the build are in the repository: the data layer, domain + DI, and the
presentation-layer ViewModels. **The Compose UI, navigation graph and app wiring are not
written yet** — `MainActivity` is a deliberate placeholder, and there is no screen code.
See [Roadmap](#roadmap) below for what is verified and what still needs a real build.

## Stack

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 (BOM 2024.06.00) |
| Architecture | MVVM + Repository, Clean Architecture layering |
| DI | Hilt 2.52 (KSP, not kapt) |
| Persistence | Room 2.6.1 |
| Async | Coroutines / Flow, StateFlow |
| Min / target SDK | 26 / 34 |
| Toolchain | AGP 8.5.2, Gradle 8.7, JDK 17 |

## Module layout

```
app/src/main/java/com/hakunakuinama/app/
├── data/                  # Room, mappers, repository implementation
│   ├── local/             # entities, DAOs, database, converters, seed
│   ├── mapper/            # Room graph -> domain models
│   └── repository/        # MealRepositoryImpl
├── domain/                # no Android imports anywhere in here
│   ├── model/             # Meal, Ingredient, MealMatch, GroceryItem, ...
│   ├── repository/        # the repository *contract*
│   ├── usecase/           # one screen-sized entry point each
│   └── util/              # resultOf()
├── di/                    # Hilt modules
└── ui/
    ├── navigation/        # route + nav-argument constants
    └── viewmodel/         # one ViewModel per screen, StateFlow UiState
```

The dependency rule is one-directional: `ui` → `domain` ← `data`, `di` wires them
together. `domain` has no Android imports at all, which is why the business logic is unit
testable without Robolectric.

## Build & run

```bash
# Requires a JDK 17 and an Android SDK with platform 34
echo "sdk.dir=/path/to/Android/Sdk" > local.properties

./gradlew :app:assembleDebug     # build the APK
./gradlew :app:testDebugUnitTest # unit tests
./gradlew :app:installDebug      # install on a connected device
```

Room exports its schema to `app/schemas/` on every build. Those JSON files are committed
on purpose — they are the reference for migration tests.

## Design decisions worth knowing

These are the choices that a new contributor would otherwise "fix" by accident.

**Cost is derived, never stored.** `Meal.costPerServingKes` is computed from the linked
ingredient prices. A recipe physically cannot disagree with its own shopping list. The UI
shows the derived value; there is no cost column to cache or drift.

**Favourites are a flag on the recipe row**, not a side table. The catalogue is small and
static, so `WHERE isFavourite = 1` beats a join, and the flag travels in the row the screen
is already observing — the heart and the card can never disagree. This is safe with seeding
because the seeder is `INSERT IGNORE` + count-guarded, so it can never overwrite a row the
user has already favourited.

**Ingredient usage lives on the join row.** 0.5 kg of ugali flour and 0.25 kg for githeri
are different facts about the same ingredient, so quantity/unit/optional live on
`meal_ingredients`, not on `ingredients`.

**Collections are stored pipe-delimited, not as JSON.** `org.json` is a stubbed class in
local unit tests, so a JSON converter makes every repository test that touches a `Meal`
throw "Method not mocked". The converters reject values containing the delimiter rather
than silently corrupting the row.

**Optional ingredients never reach the shopping list.** They are priced and shown on the
recipe screen, but the budget can only ever be inflated by things you actually need.

**Weeks start on Monday and Sunday belongs to the week that started six days earlier.**
`TemporalAdjusters.previousOrSame(MONDAY)` gives that for free. A Monday-anchored list that
jumped forward on Sunday would leave that day with no shopping list at all.

**The suggested meal is deterministic per (day, slot).** A dashboard left open does not
reshuffle on every recomposition; the suggestion rotates daily and re-emits when the slot
rolls over (the ticker wakes at the top of each hour, not per minute).

**Slot and meal travel together in one `MealSuggestion` object.** If the UI derived the
time of day itself, the header and the card could disagree at the moment the clock crosses
a slot boundary.

**`resultOf()` instead of `runCatching()`.** `runCatching` swallows
`CancellationException`, which silently breaks coroutine cancellation — a closed recipe
screen would keep writing to the database.

## Roadmap

- [x] Phase 1 — data layer: entities, DAOs, relation graph, repository, 5-meal seed
- [x] Phase 2 — domain: use cases + Hilt modules
- [x] Phase 3a — presentation: 4 ViewModels with `StateFlow` UiState
- [ ] Phase 3b — Compose UI: theme, navigation, 4 screens, shared components
- [ ] Phase 4 — empty/loading/error polish, unit tests for repository + ViewModels

### What is verified, and what is not

Being honest about this, because a green tick you cannot reproduce is worse than no tick:

**Verified by compilation and execution** — the domain layer and all four ViewModels were
compiled with the real `kotlinx.coroutines`, `androidx.lifecycle` and `dagger.hilt`
binaries and exercised against fakes: seed-data integrity, KES price maths, `MealMatch`
scoring, time-of-day slot boundaries, slot rollover under virtual time, `SavedStateHandle`
extraction, selection dedupe, error paths, and `WhileSubscribed` lifecycle.

**Not yet verified** — anything requiring `android.jar` or annotation processing: the
Compose UI, the Hilt module graph, and Room's KSP processing of the nested `@Relation` in
`MealWithDetails`. The first `assembleDebug` is the real test. Most likely first failures,
in order: Hilt KSP ViewModel factory generation, then the nested relation, then Compose
previews and lambdas.

## Seeded content

5 recipes (Masala Chai & Mandazi, Ugali & Sukuma Wiki, Chapati & Nyama Beans, Githeri,
Chicken Pilau) across 27 ingredients, priced in realistic Nairobi KES. Per-plate cost is
computed from those prices: KES 111 for chai and mandazi, KES 131 for ugali and sukuma,
KES 123 for chapati and beans, KES 112 for githeri, KES 241 for chicken pilau.

The catalogue is seeded from Room's `onCreate` callback, on a separate application coroutine
— writing to the database from inside that callback, while Room still holds the creation
transaction, deadlocks. **Known pre-release limitation:** the seed is count-guarded, so
recipes added in a *later* app version never reach existing installs. Before v1.0, either
ship a prepackaged database (`createFromAsset`) or add a `seed_version` row and re-seed
when it is behind.
