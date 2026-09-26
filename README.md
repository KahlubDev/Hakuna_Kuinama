# Hakuna Kuinama

> *Hakuna kuinama* — "no sleeping on food". A Kenyan bachelor food & menu builder: quick,
> affordable, local meals you can actually cook tonight.

An offline-first Android app that answers three questions for a student with a tight week
and a tight budget:

1. **What should I eat right now?** — a time-of-day suggestion (breakfast / lunch / dinner).
2. **What can I cook with what I already have?** — tick the ingredients in your kitchen and
   get ranked recipes, cheapest-first.
3. **What will the week cost me?** — a merged shopping list in KES from the meals you picked.

Everything is local. There is no account, no network call, and no data leaving the device.

## Screenshots

<!-- Replace the placeholders below with real captures before publishing. -->
<!-- Suggested: capture at 1080x1920, and one pair in dark mode. -->

| Home | Menu Builder | Recipe |
|---|---|---|
| ![Home screen](docs/screenshots/home.png) | ![Menu Builder](docs/screenshots/menu-builder.png) | ![Recipe detail](docs/screenshots/recipe.png) |

| Favorites | Shopping list | Dark mode |
|---|---|---|
| ![Favorites](docs/screenshots/favorites.png) | ![Shopping list](docs/screenshots/grocery-list.png) | ![Dark mode](docs/screenshots/dark-mode.png) |

## Stack

| | |
|---|---|
| Language | Kotlin 2.0.21 |
| Languages | English + Kiswahili (`values-sw`) |
| UI | Jetpack Compose + Material 3 (BOM 2024.06.00) |
| Architecture | MVVM + Repository, Clean Architecture layering |
| DI | Hilt 2.52 (KSP, not kapt) |
| Persistence | Room 2.6.1 |
| Async | Coroutines / Flow → StateFlow |
| Images | Coil 2.6 (placeholder + error states wired; catalogue ships emoji artwork) |
| Min / target SDK | 26 / 34 |
| Toolchain | AGP 8.5.2, Gradle 8.7, JDK 17 |

## Setup

```bash
git clone https://github.com/KahlubDev/Hakuna_Kuinama.git
cd Hakuna_Kuinama
```

### In Android Studio

1. **File → Open**, select the cloned folder (the one containing `settings.gradle.kts`).
2. Let Gradle sync. The wrapper is committed, so there is no Gradle to install and the
   JDK only needs to be 17 — set it under **Settings → Build, Execution, Deployment →
   Build Tools → Gradle → Gradle JDK** if Studio picked a different one.
3. If sync fails on a missing SDK, create `local.properties` in the project root:
   ```properties
   sdk.dir=/path/to/your/Android/Sdk
   ```
   Android Studio usually writes this for you; it is git-ignored because the path is
   machine-specific.
4. Pick a device or emulator on API 26+ and press **Run**.

### From the command line

```bash
./gradlew :app:assembleDebug       # build the APK
./gradlew :app:installDebug        # install on a connected device
./gradlew :app:testDebugUnitTest   # unit tests
```

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
    ├── component/         # EmptyState, MealCard, IngredientChip, StepItem, RecipeImage
    ├── navigation/        # routes, nav-argument keys, NavHost
    ├── screen/            # Dashboard, MenuBuilder, RecipeDetail, Favorites
    ├── theme/             # colour scheme, type scale, HakunaKuinamaTheme
    ├── util/              # enum -> string resource, KES formatting
    └── viewmodel/         # one ViewModel per screen, StateFlow UiState
```

The dependency rule is one-directional: `ui` → `domain` ← `data`, and `di` wires them
together. `domain` has **no** Android imports at all, which is what makes the business logic
testable without Robolectric or an emulator. If you add an `import android.*` or
`import androidx.*` to anything under `domain/`, something has leaked: a screen type, a
`@StringRes`, or a Room annotation. That is the invariant to defend in review.

## Design decisions worth knowing

These are the choices a new contributor would otherwise "fix" by accident.

**Cost is derived, never stored.** `Meal.costPerServingKes` is computed from the linked
ingredient prices. A recipe physically cannot disagree with its own shopping list, and
there is no cached figure to go stale when a price changes.

**Favourites are a flag on the recipe row**, not a side table. The catalogue is small and
static, so `WHERE isFavourite = 1` beats a join, and the flag travels in the row the screen
is already observing — the heart and the card can never disagree. Safe alongside seeding
because the seeder is `INSERT IGNORE` + count-guarded and can never overwrite a row the user
has already favourited.

**Ingredient usage lives on the join row.** 0.5 kg of ugali flour and 0.25 kg for githeri
are different facts about the same ingredient, so quantity/unit/optional live on
`meal_ingredients`, not on `ingredients`.

**Collections are stored pipe-delimited, not as JSON.** `org.json` is a stubbed class in
local unit tests, so a JSON converter makes every repository test that touches a `Meal`
throw "Method not mocked". The converters reject values containing the delimiter rather
than silently corrupting the row.

**Enum names are persisted data, and R8 must not rename them.** `TypeConverters` writes
`MealSlot.BREAKFAST` as the string `"BREAKFAST"` and reads it back with `valueOf`. The
release build is minified, so without the keep rule in `proguard-rules.pro` a release build
fails to read every slot, category and difficulty it ever wrote — while the debug build
works perfectly. This is the one non-obvious rule in the file.

**Optional ingredients never reach the shopping list.** They are priced and shown on the
recipe screen, but the budget can only ever be inflated by things you actually need.

**Weeks start on Monday and Sunday belongs to the week that started six days earlier.**
`TemporalAdjusters.previousOrSame(MONDAY)` gives that for free. A Monday-anchored list that
jumped forward on Sunday would leave that day with no shopping list at all.

**The suggested meal is deterministic per (day, slot).** A dashboard left open does not
reshuffle on every recomposition; the suggestion rotates daily and re-emits when the slot
rolls over. The ticker wakes at the top of each hour, not per minute.

**Slot and meal travel together in one `MealSuggestion`.** If the UI derived the time of day
itself, the header and the card could disagree at the moment the clock crosses a slot
boundary — and the user would be told to eat the wrong thing.

**`resultOf()` instead of `runCatching()`.** `runCatching` swallows
`CancellationException`, which silently breaks coroutine cancellation — a closed recipe
screen would keep writing to the database.

**Every screen routes its loading/empty/error state through one `EmptyState`.** Four
per-screen variants drift: three of them end up with different padding and only one gets
fixed when the text wraps at 200% font scale.

**Layouts use minimums, not fixed heights, wherever they hold text.** Same reason. The
emoji artwork is deliberately fixed-size, because it is a picture rather than text.

**Count strings are plurals, not strings.** Kiswahili marks "one" and "many" differently
from English, and "%d zilizohifadhiwa" at n=1 is simply wrong. A literal copy of the
English plurals would have been the easy mistake.

**There is no destructive migration fallback, and that is a decision not an omission.**
`fallbackToDestructiveMigration()` would mean a version bump without a migration silently
deletes a user's saved recipes and shopping list. Room's default — throw on first query —
is a nuisance on your machine and a data-loss incident in the wild. See
`HakunaKuinamaMigrations`.

**Swipe-to-remove also has a button.** A gesture is invisible to a screen reader and
unusable with a switch device, so the favourites list offers a heart that does the same
thing.

## Seeded content

5 recipes (Masala Chai & Mandazi, Ugali & Sukuma Wiki, Chapati & Nyama Beans, Githeri,
Chicken Pilau) across 27 ingredients, priced in realistic Nairobi KES. Per-plate cost is
computed from those prices: KES 111 for chai and mandazi, 131 for ugali and sukuma, 123 for
chapati and beans, 112 for githeri, 241 for chicken pilau.

## Roadmap

- [x] Data layer — entities, DAOs, relation graph, repository, 5-meal seed
- [x] Domain + DI — use cases, Hilt modules
- [x] ViewModels — four screens, `StateFlow` state, `SavedStateHandle` navigation args
- [x] Compose UI — theme, navigation, four screens, five shared components
- [x] Accessibility pass — descriptions, 48dp targets, 200% font scale, non-gesture alternatives
- [ ] Unit tests for the domain and ViewModel layers
- [ ] Room migration strategy (see below)
- [ ] Localisation (Shona/English) — copy is already externalised to `strings.xml`

### Known limitations

Being honest about these, because a limitation that surprises a user in the field is worse
than one written down here.

- **Everything compiles except the generated code.** All 310 classes across all four
  layers were compiled with the real AndroidX, Compose, Room, Hilt and Kotlin Compose
  compiler plugin. What is *not* verified is annotation processing: Hilt's KSP ViewModel
  factory generation and Room's KSP handling of the nested `@Relation` in
  `MealWithDetails`. Those need `./gradlew :app:assembleDebug`, and the nested relation is
  still the single most likely first error.
- **`app/src/androidTest` has never been run.** Migration tests need real SQLite, so they
  need a device or emulator. The file is a documented template.
- **The Kiswahili strings need a native speaker.** They are written by someone who does
  not speak Swahili natively. The grammar and the plural forms were done carefully, but
  idiom is the one thing a translation cannot be mechanically correct about.
- **No Room migration exists yet, by design.** The database is version 1, so there is
  nothing to migrate. What is in place is the policy: `addMigrations(...)` is wired, there
  is deliberately **no** `fallbackToDestructiveMigration`, and `HakunaKuinamaMigrations`
  documents exactly what to do for version 2. Until `app/schemas/1.json` is generated by
  your first build and committed, CI cannot verify a migration — so commit it.
- **Seeding is count-guarded**, so recipes added in a *later* app version never reach
  existing installs. Before v1.0, either ship a prepackaged database (`createFromAsset`) or
  add a `seed_version` row and re-seed when it is behind.
- **On first launch there may be a one-frame empty state**, because the catalogue is seeded
  from Room's `onCreate` callback and the first query can beat it. Exposing seed status from
  the data layer would close it.
