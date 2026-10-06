# 01. Audit: Hakuna Kuinama v1.0.0

**Repo:** github.com/KahlubDev/Hakuna_Kuinama, commit `87c4461` (merge of `feat/editorial-redesign`)
**Audit date:** 2026-09-28
**Re-checked against:** `dda4f4e` (2026-09-29) — nine commits later. Findings below now carry a `Status` line; see `00-README.md`.
**Method:** cloned the repo, read the README and the domain, data, DI and ViewModel layers plus the seed catalogue (about 6,500 lines of Kotlin in `main`), reviewed 5 device screenshots, and spot-verified suspected bugs by reading the exact code paths.
**Not done at audit time:** I did not run Gradle. The sandbox has no route to the Google and Maven repositories, so build and test status below was taken from the README, not verified. *(Later corrected: the build has since been run — see R2.)*

---

## 1. Verdict in one paragraph

The engineering skeleton is genuinely good: clean layering, a pure-Kotlin domain, derived (never stored) costs, a real migration policy, accessibility and Swahili plural handling, and 109 unit tests. **The product is a demo.** It contains 5 recipes, 27 ingredients, one hard-coded price per ingredient, no nutrition data, no settings, no user-entered data beyond ticking chips, and no network layer. Several visible behaviours were wrong or misleading (section 3); two of those are since fixed. The gap between "well-built prototype" and "real app" is almost entirely **data, units, pricing and recommendation logic**, not UI.

## 2. Scorecard

Scores are as audited at `87c4461`. The right-hand column records movement since, because a scorecard that silently ages is worse than no scorecard.

| Area | Score (1-5) | One-line reason | Movement since `87c4461` |
|---|---|---|---|
| Code architecture | 4 | Clean MVVM + use cases, dependency rule defended, Clock injected | Unchanged |
| Test discipline | 4 | 109 `@Test`s, ViewModel + domain covered, migration harness now compiles and is wired to the committed schema | +2 tests. The harness had never compiled; fixed, and CI now runs it on an emulator (R2, R3) |
| Accessibility and i18n | 4 | 48dp targets, non-gesture alternatives, Swahili plurals. Content itself is English only | **Both claims were false at audit time**: a 34dp pager target (fixed, `69249ed`) and two subtitles shipping Swahili in the default locale (fixed, `bbe5fd8`) |
| UI polish | 3 | Calm editorial look, but no food imagery, so the app has no appetite appeal | Unchanged |
| Product logic correctness | 2 | Four verified logic or label bugs (section 3) | 2 of 4 fixed, 2 deferred as a pair. See section 3 |
| Content and data | 1 | 5 recipes, 27 ingredients, no nutrition, one static price each | Unchanged |
| Feature depth | 1 | Pick chips, see matches, view recipe, favourite, shopping list. Nothing else | Unchanged |
| Release readiness | 1 | Targets API 34 (Play now requires 36 for new apps and updates), no CI, no licence, build never verified end to end | **Materially better, still blocked.** Build, annotation processing and CI are all in place; `targetSdk` 34 and the missing `LICENSE` both remain, and API 34 is now the only release blocker |
| Privacy and security | 3 | Nothing leaves the device today. Every planned feature changes that (section 8) | Unchanged |

## 3. Verified bugs and misleading behaviour

These are the highest-value fixes because they are cheap and they affect user trust.

**Status vocabulary:** `Fixed` = merged, with the commit named. `Deferred` = still present in code, but paired with a `TODO` recording why it is not yet safe to fix alone. `Open` = untouched.

### L1. The "X/Y ingredients" denominator counts items the user cannot see (Medium)

**Status: Fixed** in `a291515`, pinned by two regression tests in `MealMatchTest.kt`.

- **Evidence (screenshot):** Menu Builder shows Chapati & Nyama Beans as **5/9 ingredients**. The recipe screen lists **8** ingredients.
- **Cause (as audited):** `MealMatch.of()` built `required` with `filterNot { it.isOptional }` and never checked `mustBuy`. The hidden ninth item was `Table salt` (`mustBuy = false`), which the recipe screen hides but the matcher counted as missing.
- **Fix taken:** `required` is now `meal.shoppableIngredients`, i.e. `mustBuy && !isOptional`, and the contradictory KDoc was replaced with one that documents the `mustBuy` rule. A follow-up commit (`87a20ab`) removed a second, already-drifted copy of the same rule from `MenuBuilderScreen.matchFootnote`, which was counting pantry staples the matcher no longer scores.
- **Correction to the audit as written:** the audit quoted the old KDoc as naming "(salt, oil, water)". Cooking oil is `mustBuy = true` and water is not an ingredient at all. Only salt was ever affected.

### L2. "cheapest first" label does not match the sort order (Medium)

**Status: Fixed** in `02aab77` (copy only — the roadmap's other branch, a sort toggle, was not taken).

- **Evidence (screenshot):** "Best matches, cheapest first" listed Ugali (KES 131), Githeri (112), Chapati (123), Pilau (241).
- **Cause (still true):** `observeMatches` sorts by `canCookNow`, then `matchPercentage`, then cost, then name. Cost is the third of four keys, not the first.
- **Fix taken:** the label became "cheapest on a tie" (`bei nafuu kwa usawa` in `values-sw`). **The underlying redundancy stands** — `canCookNow` and `matchPercentage` already imply cost is only breaking ties, so a reader who wants a genuinely cheapest-first list still cannot ask for one. A sort toggle remains unwritten.

### L3. "Rotating" suggestion cannot rotate (High for the "boredom" goal)

**Status: Open.**

- **Evidence (code and screenshot):** seed slots are 1 breakfast, 3 lunch, 1 dinner. `suggestedMealFor` picks `(epochDay + slot.ordinal) mod meals.size`. With one meal in a slot, the index is always 0.
- **Effect:** every morning is Masala Chai & Mandazi and every evening is Chicken Pilau, forever. The home hero card is the same every day for 2 of the 3 main slots.
- **Fix:** this disappears with catalogue growth, but the algorithm should also stop being purely date-modulo (see recommendation engine in `04-architecture-and-data.md`).

### L4. Servings multiplier is applied to batches, not people (Medium)

**Status: Deferred** — see the note below, which also covers L6. The audit's "confirm in UI" instruction is now answered: **there is no servings control.** `GroceryListViewModel.onGenerateForRecipe(mealId, servings = 1)` is only ever called with the default and the only caller is `AppNavHost`. The bug is therefore inert today and becomes a visible over-buy the moment a scaler ships.

- **Cause:** `mergeIntoLines(meals, servingsMultiplier)` multiplies each recipe's base quantities by `GroceryPlan.servingsPerMeal`. Recipes are written for 2 or 3 servings (`servings = 2` for chai, ugali, chapati; `3` for githeri, pilau). A user who sets "4 people" would get 4 batches (8 to 12 portions).
- **Fix, and why it is not taken yet:** divide by `meal.servings` rather than multiply by 1. `2e3fc4b` records this as a paired `TODO(servings-scaling)` in `MealRepositoryImpl` stating that L4 and L6 are *one feature* and "fix the batch arithmetic and the step quantities in one change, or neither". `05-roadmap.md` still lists them as two independent tickets; that pairing needs to be respected.

### L5. The price on every card is consumption cost, not the cash the user must spend (High for real use)

**Status: Open.**

- **Evidence (screenshot):** Chapati & Nyama Beans lists cooking oil at 0.06 litre. That is KES 33 of a KES 550 litre. Nobody can buy 0.06 litre.
- **Effect:** the weekly "planned KES" total sums fractional quantities and **under-reports the cash outlay**. For a student living on a fixed allowance, the honest number is "what I must pay at the shop" (oil is KES 550 or a small bottle), and separately "what this plate really cost".
- **Fix:** model purchase packs. Show two figures: plate cost and cash needed. Track leftover value in the pantry.

### L6. Recipe steps hard-code quantities as prose (High once servings scaling exists)

**Status: Deferred** — paired with L4; see above. `2e3fc4b` adds a matching `TODO(servings-scaling)` to `MealStep`.

- **Evidence:** steps contain literals such as "Soak 300 g cowpeas", "Whisk in 500 g maize flour", "Mix 500 g chapati flour". These do not change when servings change.
- **Fix:** template steps with ingredient placeholders (`{ing:cowpeas}`) resolved at render time (the recipe widget pattern already used elsewhere), or store steps as structured text with quantity tokens.

### L7. Ingredient identity is exact-ID only (High for the "what's in my kitchen" promise)

**Status: Open.** `Ingredient` still carries only `id, name, category, unit, pricePerUnitKes, emoji, isStaple` — no alias, translation, form or brand field.

- No aliases (sukuma wiki / kale / collards, nyama beans / kunde / cowpeas), no Swahili names, no substitution graph, no brand or form (fresh vs tinned tomatoes).
- Matching is therefore brittle: a user with tomato paste matches nothing.

## 4. Content and data findings

| ID | Sev | Finding | Evidence |
|---|---|---|---|
| D1 | Critical | Catalogue is a toy: 5 recipes, 27 ingredients | `SeedData.kt`, README "Seeded content". **Still true** — `SeedData.kt` is byte-identical to `87c4461` |
| D2 | Critical | One static price per ingredient, with no date, source, market or confidence, and no way to change it | `IngredientEntity.pricePerUnitKes`, `SeedData`. *(Correction: the audit originally said the price is "labelled 'Nairobi retail'". No such label exists anywhere in `main`; "Nairobi retail" appears only in `SeedData`'s KDoc and in `TestClock`'s `ZoneId`. The finding's substance is correct, the label claim is not.)* |
| D3 | High | Zero nutrition data. "Balanced diet" is impossible today | grep for calorie, protein, nutri, allerg finds nothing outside the category enum. **Still true** |
| D4 | High | Quantities are in purchase units, unreadable to a cook: "0.2 250 g piece Green capsicum", "0.02 100 g piece Garlic", "0.01 50 g pack Pili pili" | recipe screenshot |
| D5 | Medium | Content ships inside the APK as Kotlin. Count-guarded seeding means new recipes never reach existing installs | README "Known limitations" |
| D6 | Medium | Recipe and ingredient **content** is English only. The Swahili build translates chrome but not meal names, taglines or steps | `MealEntity`, no translation table. **Note the mirror-image bug found later:** two Menu Builder / Favorites subtitles shipped *Swahili in the default locale*, so English phones got English title over Swahili subtitle. Fixed in `bbe5fd8` |
| D7 | Medium | Tags are free strings (`vegan`, `budget`, `protein`), pipe-delimited. No structured diet or allergen model | `MealEntity.tags` |
| D8 | Medium | No photography. Hero and list cards are abstract geometric shapes with a fork-and-knife glyph, which reads as "unfinished" | screenshots 1 to 4 |
| D9 | Low | Catalogue misses staples in everyday Kenyan cooking: omena/dagaa, tilapia, beef, goat, liver, matoke, sweet potato, arrowroot, cassava, cabbage, spinach, managu, terere, kunde leaves, ndengu, njahi, avocado, banana, coconut, lentils, tea bread, pilau masala, royco. No regional cuisines (Coast, Western, Central, Rift, Nyanza, North, Somali) | `SeedData.ingredients` |

## 5. UX findings (from the 5 screenshots)

| ID | Screen | Finding | Fix |
|---|---|---|---|
| U1 | Home | Hero and list cards show identical placeholder art. Nothing makes you hungry | Real photos or a consistent illustration set per dish |
| U2 | Home | Greeting "Good morning, karibu chakula." reads oddly. "KES 111" has no "per plate" label, and no servings indicator | Label "per plate", show servings |
| U3 | Menu Builder | Ingredient picker is a paged chip grid ("Pantry essentials 1 of 3") showing 10 of 27 items. At 150+ ingredients this is unusable. There is no search, categories, recents, quantity or "running low" | Search-first picker, category chips, recents, quantity/expiry |
| U4 | Menu Builder | Tab is named "Menu builder" but does pantry matching. The real menu builder (weekly plan) does not exist | Rename to "Kitchen" or "Cook now", and add a real Planner tab |
| U5 | Menu Builder | Order label mismatch (see L2) | **Fixed** in `02aab77` (label changed; the sort toggle alternative was not built) |
| U6 | Recipe | Unreadable quantities (D4) and no servings scaler, no cook mode (keep screen on, big text, step timers), no "add to plan" | Cook mode, scaler, plan action |
| U7 | Recipe | Steps show time chips, but there is no total timeline and no equipment or fuel info (jiko, gas, single burner) | Equipment and fuel model |
| U8 | Global | No onboarding, no settings, no way to say budget, household size, county, diet or allergies. The app cannot personalise because it never asks | DataStore-backed profile (stack already declares `datastore-preferences`, unused) |
| U9 | Favorites | Fine, but favourites are the only memory the app has. No "cooked it" log, no ratings, no skip or dislike | History table |

## 6. Architecture and scalability

| ID | Sev | Finding | Recommendation |
|---|---|---|---|
| A1 | High | One `MealRepository` interface owns recipes, favourites, ingredients, grocery, budget and seeding (`MealRepositoryImpl`, **329 lines** as of `dda4f4e`, 314 at `87c4461`; the growth is the `TODO(servings-scaling)` block) | Split into Catalogue, Pantry, Prices, Plan, History and Sharing repositories before adding features |
| A2 | High | `observeMatches` combines the **entire** meal graph (`observeAllWithDetails`, `@Transaction SELECT * FROM meals`) and re-scores in Kotlin on every emission. Favouriting one recipe re-emits and re-maps the whole catalogue | Fine at 5 meals. At 500+, add an ingredient to meal inverted index (SQL) and page results |
| A3 | High | No network layer, no WorkManager, no sync. Coil is present but nothing supplies `imageUrl` | Add a versioned content-pack updater (section 4 of the architecture doc) |
| A4 | Medium | `datastore-preferences` is declared and unused | Use it for profile, budget, county, units, onboarding flag |
| A5 | Medium | Collections stored pipe-delimited to keep JVM tests simple. It blocks querying by tag, diet or allergen | Real join tables (`meal_tags`, `meal_diet_flags`) in the v2 migration |
| A6 | Medium | Seeder is count-guarded and runs from a Room callback (first-launch empty-frame risk, acknowledged in README) | Replace with a versioned pack importer that upserts by stable `slug` keys |

**Keep:** the `domain` purity rule, `Clock` injection, `resultOf` (not `runCatching`), `EmptyState` consolidation, no destructive migration fallback, R8 keep rule for enum converters.

## 7. Release readiness and repo hygiene

Status as of `dda4f4e`, plus the R2/R3/R6 fixes landed the same day.

| ID | Sev | Finding | Status |
|---|---|---|---|
| R1 | **High** | `targetSdk = 34`, `compileSdk = 34`. Google Play requires new apps and app updates to target Android 16 (API 36) from 2026-08-31, with an extension request available to 2026-11-01. As written, Play will reject this build. Upgrading targetSdk to 36 also needs a newer AGP and Compose BOM than the pinned 8.5.2 and 2024.06. Verify exact minimum AGP in the Android docs | **Open — now the top release blocker.** `app/build.gradle.kts` still reads 34 for both |
| R2 | High | The README stated the build had never been verified through annotation processing (Hilt and Room KSP), and that `androidTest` had never run | **Fixed 2026-09-29.** KSP is verified (`assembleDebug` builds; Room generates `HakunaKuinamaDatabase_Impl.kt`, so the nested `@Relation` is fine; all five Hilt ViewModel modules generate; 109/109 unit tests, `lintDebug` 0 errors). Separately, the instrumentation suite had **never compiled** — three faults, all fixed: `androidx.room.testing` was on `testImplementation` where `androidTest` cannot see it and nothing in `src/test` used it; the committed schema was never published into the androidTest APK's assets, so `MigrationTestHelper` could not find it; and `version1SchemaIsExportedAndCommitted` asserted on a repository path from on-device code, which can never hold. It compiles and the schema is now at the asset path Room reads. **Still unproven:** nobody has run it on a device by hand. CI now runs it on an emulator (`R3`) — trust that job's first green run before trusting the harness |
| R3 | Medium | No CI (`.github/` absent). Nothing stops a red build reaching `main` | **Fixed 2026-09-29.** `.github/workflows/ci.yml` runs `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` and `lintDebug` on every push and PR to `main`, plus an emulator job for `connectedDebugAndroidTest`. The `assembleDebugAndroidTest` step is deliberate: it is a compile gate, and its absence is why the broken instrumentation suite went unnoticed for so long |
| R4 | Medium | `.kotlin/sessions/*.salive` is tracked in git. Add `.kotlin/` to `.gitignore` and `git rm --cached` it | **Fixed** in `65af64f` (the file deletion itself landed in `bbe5fd8`) |
| R5 | Low | No `LICENSE` file, so by default nobody can legally reuse or contribute | **Open** |
| R6 | Low | README said 103 tests, code had 107 `@Test` annotations. README "Screenshots" section is a placeholder | **Count corrected 2026-09-29** — it had drifted twice (107 at `87c4461`, 109 at `dda4f4e`) and the README said 103 in four places; it now says 109. Screenshots still missing. Worth noting the count is hand-maintained in four places and had already gone stale once before the audit, so consider generating or dropping it |
| R7 | Low | No crash reporting or analytics of any kind. Fine for privacy, but you cannot see field failures. Prefer opt-in, privacy-first tooling | **Open.** No such dependency in `app/build.gradle.kts` |
| R8 | Low | Release signing, versionCode strategy and Play listing assets are not defined | **Open** |

## 8. Privacy and security (forward-looking)

Today the app is clean: no accounts, no network calls, data on device. Each planned feature changes that, so decide now:

- **Backup:** `allowBackup="true"` with the database included. Fine today. Once an anonymous identity key or price history exists, **exclude the identity key from backup**, or restoring to a second phone clones an identity.
- **Location:** never store precise GPS. Store county and market only, on device. Precise location is unnecessary for price lookup.
- **Community content:** user photos and text create moderation, takedown and child-safety obligations.
- **AI prompts:** pantry lists are low-sensitivity, but free text can contain anything. Send no device or account identifiers to the model provider, and keep the API key on a server proxy, never in the APK.
- **Kenyan law:** the Data Protection Act, 2019 applies to anyone processing personal data of people in Kenya. Check registration duties with the Office of the Data Protection Commissioner before launching community features (legal advice, not a conclusion from this audit).
- **Health claims:** "balanced diet" and any condition-specific mode (diabetes, pregnancy, hypertension) must be framed as general information, reviewed by a registered nutritionist or dietitian, and never as medical advice.

## 9. Top 12 actions, in order

Progress markers added as of `dda4f4e`. See section 3 and section 7 for the per-finding status.

1. Fix L1 (hidden salt), L2 (label), L4 (servings), L6 (quantity-in-prose). All small, all trust-related. — **L1 and L2 done; L4 and L6 deferred as one paired change (`2e3fc4b`), not as two tickets.**
2. Add `.kotlin/` to `.gitignore`, add `LICENSE`, refresh README screenshots. — **`.gitignore` and the README test count done; `LICENSE` and screenshots still missing.**
3. Add GitHub Actions: `assembleDebug`, `testDebugUnitTest`, lint. — **Done 2026-09-29** (`.github/workflows/ci.yml`), plus an `assembleDebugAndroidTest` compile gate and an emulator job for `connectedDebugAndroidTest`.
4. Bump toolchain and `targetSdk` to 36. Run the never-run `androidTest` on a device. — **Toolchain not started, and this is now the top release blocker.** The instrumentation side is done as far as it can be without a device: it compiles, and CI runs it on an emulator.
5. Design the **catalogue v2 schema** (stable slugs, cooking units vs purchase packs, nutrition per 100 g, aliases, translations). Everything else depends on it.
6. Import Kenya Food Composition Tables 2018 nutrition for the existing 27 ingredients as the end-to-end pipeline test.
7. Introduce price observations with source, date, market, confidence, plus a county and market picker and per-item user override.
8. Split the repository and move seed data to versioned JSON content packs.
9. Add DataStore profile: household size, budget, county, diet, allergens.
10. Ship pantry with quantities and a search-first picker.
11. Ship the recommendation and planner engine (see `04`).
12. Only then: sharing, community, AI helper, scanning.

## 10. Screenshot log

**As of `87c4461`.** Several rows below are no longer reproducible, because L1 and L2 have since been fixed. Kept as the historical record of what the audit saw, not as a description of current behaviour.

| # | Screen | Notes |
|---|---|---|
| 1 | Home (Mon 05:59) | Greeting, hero "Masala Chai & Mandazi KES 111", 4 picks sorted cheapest first (111 hero, then 112, 123, 131, 241), "Build a meal" FAB |
| 2 | Favorites | 2 saved (Githeri, Chai & Mandazi), heart toggles, Swahili subtitle — *the subtitle later became a real localisation bug in the default locale, see D6* |
| 3 | Recipe detail | Chapati & Nyama Beans, KES 123, Easy 50 min, 8 visible ingredients, 7 timed steps, "View shopping list" FAB. Unit display bug D4 |
| 4 | Menu Builder (10 selected) | All 10 ticked, pager "1 of 3", best matches 5/7, 6/9, 5/9, 4/10. Bugs L1, L2 — *the denominators counting above 8 were inflated by pantry staples, so those rows now read one lower in the denominator; and the pager buttons were 34dp, not 48dp* |
| 5 | Menu Builder (empty) | Numbered chips 1 to 10, text-only empty state |

Note that row 1's "cheapest first" *is* still accurate: the Home screen's weekly picks genuinely are sorted by `costPerServingKes` in `GetWeeklyPicksUseCase`. L2 was specific to the Menu Builder's match list. Do not "fix" row 1.
