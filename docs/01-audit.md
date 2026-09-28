# 01. Audit: Hakuna Kuinama v1.0.0

**Repo:** github.com/KahlubDev/Hakuna_Kuinama, commit `87c4461` (merge of `feat/editorial-redesign`)
**Audit date:** 2026-09-28
**Method:** cloned the repo, read the README and the domain, data, DI and ViewModel layers plus the seed catalogue (about 6,500 lines of Kotlin in `main`), reviewed the 5 device screenshots, and spot-verified suspected bugs by reading the exact code paths.
**Not done:** I did not run Gradle. The sandbox has no route to the Google and Maven repositories, so build and test status below is taken from the README, not verified.

---

## 1. Verdict in one paragraph

The engineering skeleton is genuinely good: clean layering, a pure-Kotlin domain, derived (never stored) costs, a real migration policy, accessibility and Swahili plural handling, and 107 unit tests. **The product is a demo.** It contains 5 recipes, 27 ingredients, one hard-coded price per ingredient, no nutrition data, no settings, no user-entered data beyond ticking chips, and no network layer. Several visible behaviours are wrong or misleading (section 3). The gap between "well-built prototype" and "real app" is almost entirely **data, units, pricing and recommendation logic**, not UI.

## 2. Scorecard

| Area | Score (1-5) | One-line reason |
|---|---|---|
| Code architecture | 4 | Clean MVVM + use cases, dependency rule defended, Clock injected |
| Test discipline | 4 | 107 `@Test`s, ViewModel + domain covered, migration harness exists (never run) |
| Accessibility and i18n | 4 | 48dp targets, non-gesture alternatives, Swahili plurals. Content itself is English only |
| UI polish | 3 | Calm editorial look, but no food imagery, so the app has no appetite appeal |
| Product logic correctness | 2 | Four verified logic or label bugs (section 3) |
| Content and data | 1 | 5 recipes, 27 ingredients, no nutrition, one static price each |
| Feature depth | 1 | Pick chips, see matches, view recipe, favourite, shopping list. Nothing else |
| Release readiness | 1 | Targets API 34 (Play now requires 36 for new apps and updates), no CI, no licence, build never verified end to end |
| Privacy and security | 3 | Nothing leaves the device today. Every planned feature changes that (section 8) |

## 3. Verified bugs and misleading behaviour

These are the highest-value fixes because they are cheap and they affect user trust.

### L1. The "X/Y ingredients" denominator counts items the user cannot see (Medium)
- **Evidence (screenshot):** Menu Builder shows Chapati & Nyama Beans as **5/9 ingredients**. The recipe screen lists **8** ingredients.
- **Cause:** `MealMatch.of()` builds `required` with `filterNot { it.isOptional }` and never checks `mustBuy`. The hidden ninth item is `Table salt` (`mustBuy = false`, seed line 223 region), which the recipe screen hides but the matcher counts as missing. The KDoc on `MealMatch.of` says pantry staples are "treated as satisfied unless the user ticked them", so the code contradicts its own comment.
- **Effect:** "Cook now" can never trigger unless the user ticks salt. Match percentages are understated.
- **Fix:** exclude `mustBuy == false` items from `required`, or treat them as satisfied. Add a test with a pantry-only ingredient.

### L2. "cheapest first" label does not match the sort order (Medium)
- **Evidence (screenshot):** "Best matches, cheapest first" lists Ugali (KES 131), Githeri (112), Chapati (123), Pilau (241).
- **Cause:** `observeMatches` sorts by `canCookNow`, then `matchPercentage`, then cost. Cost is only the third key.
- **Fix:** either change the label ("best match first") or offer a sort toggle (Best match, Cheapest, Fastest).

### L3. "Rotating" suggestion cannot rotate (High for the "boredom" goal)
- **Evidence (code and screenshot):** seed slots are 1 breakfast, 3 lunch, 1 dinner. `suggestedMealFor` picks `(epochDay + slot.ordinal) mod meals.size`. With one meal in a slot, the index is always 0.
- **Effect:** every morning is Masala Chai & Mandazi and every evening is Chicken Pilau, forever. The home hero card is the same every day for 2 of the 3 main slots.
- **Fix:** this disappears with catalogue growth, but the algorithm should also stop being purely date-modulo (see recommendation engine in `04-architecture-and-data.md`).

### L4. Servings multiplier is applied to batches, not people (Medium, confirm in UI)
- **Cause:** `mergeIntoLines(meals, servingsMultiplier)` multiplies each recipe's base quantities by `GroceryPlan.servingsPerMeal`. Recipes are written for 2 or 3 servings (`servings = 2` for chai, ugali, chapati; `3` for githeri, pilau). A user who sets "4 people" gets 4 batches (8 to 12 portions).
- **Verify:** `GroceryListViewModel.kt` line ~100 passes `servingsPerMeal = servings`. Confirm the UI label says "batches" or convert with `people / meal.servings`.

### L5. The price on every card is consumption cost, not the cash the user must spend (High for real use)
- **Evidence (screenshot):** Chapati & Nyama Beans lists cooking oil at 0.06 litre. That is KES 33 of a KES 550 litre. Nobody can buy 0.06 litre.
- **Effect:** the weekly "planned KES" total sums fractional quantities and **under-reports the cash outlay**. For a student living on a fixed allowance, the honest number is "what I must pay at the shop" (oil is KES 550 or a small bottle), and separately "what this plate really cost".
- **Fix:** model purchase packs. Show two figures: plate cost and cash needed. Track leftover value in the pantry.

### L6. Recipe steps hard-code quantities as prose (High once servings scaling exists)
- **Evidence:** steps contain literals such as "Soak 300 g cowpeas", "Whisk in 500 g maize flour", "Mix 500 g chapati flour". These do not change when servings change.
- **Fix:** template steps with ingredient placeholders (`{ing:cowpeas}`) resolved at render time (the recipe widget pattern already used elsewhere), or store steps as structured text with quantity tokens.

### L7. Ingredient identity is exact-ID only (High for the "what's in my kitchen" promise)
- No aliases (sukuma wiki / kale / collards, nyama beans / kunde / cowpeas), no Swahili names, no substitution graph, no brand or form (fresh vs tinned tomatoes).
- Matching is therefore brittle: a user with tomato paste matches nothing.

## 4. Content and data findings

| ID | Sev | Finding | Evidence |
|---|---|---|---|
| D1 | Critical | Catalogue is a toy: 5 recipes, 27 ingredients | `SeedData.kt`, README "Seeded content" |
| D2 | Critical | One static price per ingredient, labelled "Nairobi retail", no date, source, market or confidence, no way to change it | `IngredientEntity.pricePerUnitKes`, `SeedData` |
| D3 | High | Zero nutrition data. "Balanced diet" is impossible today | grep for calorie, protein, nutri, allerg finds nothing outside the category enum |
| D4 | High | Quantities are in purchase units, unreadable to a cook: "0.2 250 g piece Green capsicum", "0.02 100 g piece Garlic", "0.01 50 g pack Pili pili" | recipe screenshot |
| D5 | Medium | Content ships inside the APK as Kotlin. Count-guarded seeding means new recipes never reach existing installs | README "Known limitations" |
| D6 | Medium | Recipe and ingredient **content** is English only. The Swahili build translates chrome but not meal names, taglines or steps | `MealEntity`, no translation table |
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
| U5 | Menu Builder | Order label mismatch (see L2) | as L2 |
| U6 | Recipe | Unreadable quantities (D4) and no servings scaler, no cook mode (keep screen on, big text, step timers), no "add to plan" | Cook mode, scaler, plan action |
| U7 | Recipe | Steps show time chips, but there is no total timeline and no equipment or fuel info (jiko, gas, single burner) | Equipment and fuel model |
| U8 | Global | No onboarding, no settings, no way to say budget, household size, county, diet or allergies. The app cannot personalise because it never asks | DataStore-backed profile (stack already declares `datastore-preferences`, unused) |
| U9 | Favorites | Fine, but favourites are the only memory the app has. No "cooked it" log, no ratings, no skip or dislike | History table |

## 6. Architecture and scalability

| ID | Sev | Finding | Recommendation |
|---|---|---|---|
| A1 | High | One `MealRepository` interface owns recipes, favourites, ingredients, grocery, budget and seeding (`MealRepositoryImpl`, 314 lines) | Split into Catalogue, Pantry, Prices, Plan, History and Sharing repositories before adding features |
| A2 | High | `observeMatches` combines the **entire** meal graph (`observeAllWithDetails`, `@Transaction SELECT * FROM meals`) and re-scores in Kotlin on every emission. Favouriting one recipe re-emits and re-maps the whole catalogue | Fine at 5 meals. At 500+, add an ingredient to meal inverted index (SQL) and page results |
| A3 | High | No network layer, no WorkManager, no sync. Coil is present but nothing supplies `imageUrl` | Add a versioned content-pack updater (section 4 of the architecture doc) |
| A4 | Medium | `datastore-preferences` is declared and unused | Use it for profile, budget, county, units, onboarding flag |
| A5 | Medium | Collections stored pipe-delimited to keep JVM tests simple. It blocks querying by tag, diet or allergen | Real join tables (`meal_tags`, `meal_diet_flags`) in the v2 migration |
| A6 | Medium | Seeder is count-guarded and runs from a Room callback (first-launch empty-frame risk, acknowledged in README) | Replace with a versioned pack importer that upserts by stable `slug` keys |

**Keep:** the `domain` purity rule, `Clock` injection, `resultOf` (not `runCatching`), `EmptyState` consolidation, no destructive migration fallback, R8 keep rule for enum converters.

## 7. Release readiness and repo hygiene

| ID | Sev | Finding |
|---|---|---|
| R1 | **High** | `targetSdk = 34`, `compileSdk = 34`. Google Play requires new apps and app updates to target Android 16 (API 36) from 2026-08-31, with an extension request available to 2026-11-01. As written, Play will reject this build. Upgrading targetSdk to 36 also needs a newer AGP and Compose BOM than the pinned 8.5.2 and 2024.06. Verify exact minimum AGP in the Android docs |
| R2 | High | The README states the build has never been verified through annotation processing (Hilt and Room KSP), and `androidTest` has never run |
| R3 | Medium | No CI (`.github/` absent). Nothing stops a red build reaching `main` |
| R4 | Medium | `.kotlin/sessions/*.salive` is tracked in git. Add `.kotlin/` to `.gitignore` and `git rm --cached` it |
| R5 | Low | No `LICENSE` file, so by default nobody can legally reuse or contribute |
| R6 | Low | README says 103 tests, code has 107 `@Test` annotations. README "Screenshots" section is a placeholder, but real captures now exist |
| R7 | Low | No crash reporting or analytics of any kind. Fine for privacy, but you cannot see field failures. Prefer opt-in, privacy-first tooling |
| R8 | Low | Release signing, versionCode strategy and Play listing assets are not defined |

## 8. Privacy and security (forward-looking)

Today the app is clean: no accounts, no network calls, data on device. Each planned feature changes that, so decide now:

- **Backup:** `allowBackup="true"` with the database included. Fine today. Once an anonymous identity key or price history exists, **exclude the identity key from backup**, or restoring to a second phone clones an identity.
- **Location:** never store precise GPS. Store county and market only, on device. Precise location is unnecessary for price lookup.
- **Community content:** user photos and text create moderation, takedown and child-safety obligations.
- **AI prompts:** pantry lists are low-sensitivity, but free text can contain anything. Send no device or account identifiers to the model provider, and keep the API key on a server proxy, never in the APK.
- **Kenyan law:** the Data Protection Act, 2019 applies to anyone processing personal data of people in Kenya. Check registration duties with the Office of the Data Protection Commissioner before launching community features (legal advice, not a conclusion from this audit).
- **Health claims:** "balanced diet" and any condition-specific mode (diabetes, pregnancy, hypertension) must be framed as general information, reviewed by a registered nutritionist or dietitian, and never as medical advice.

## 9. Top 12 actions, in order

1. Fix L1 (hidden salt), L2 (label), L4 (servings), L6 (quantity-in-prose). All small, all trust-related.
2. Add `.kotlin/` to `.gitignore`, add `LICENSE`, refresh README screenshots.
3. Add GitHub Actions: `assembleDebug`, `testDebugUnitTest`, lint.
4. Bump toolchain and `targetSdk` to 36. Run the never-run `androidTest` on a device.
5. Design the **catalogue v2 schema** (stable slugs, cooking units vs purchase packs, nutrition per 100 g, aliases, translations). Everything else depends on it.
6. Import Kenya Food Composition Tables 2018 nutrition for the existing 27 ingredients as the end-to-end pipeline test.
7. Introduce price observations with source, date, market, confidence, plus a county and market picker and per-item user override.
8. Split the repository and move seed data to versioned JSON content packs.
9. Add DataStore profile: household size, budget, county, diet, allergens.
10. Ship pantry with quantities and a search-first picker.
11. Ship the recommendation and planner engine (see `04`).
12. Only then: sharing, community, AI helper, scanning.

## 10. Screenshot log

| # | Screen | Notes |
|---|---|---|
| 1 | Home (Mon 05:59) | Greeting, hero "Masala Chai & Mandazi KES 111", 4 picks sorted cheapest first (111 hero, then 112, 123, 131, 241), "Build a meal" FAB |
| 2 | Favorites | 2 saved (Githeri, Chai & Mandazi), heart toggles, Swahili subtitle |
| 3 | Recipe detail | Chapati & Nyama Beans, KES 123, Easy 50 min, 8 visible ingredients, 7 timed steps, "View shopping list" FAB. Unit display bug D4 |
| 4 | Menu Builder (10 selected) | All 10 ticked, pager "1 of 3", best matches 5/7, 6/9, 5/9, 4/10. Bugs L1, L2 |
| 5 | Menu Builder (empty) | Numbered chips 1 to 10, text-only empty state |
