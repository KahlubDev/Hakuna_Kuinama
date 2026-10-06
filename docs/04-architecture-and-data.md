# 04. Architecture and Data Design

Design for the requirements in `03-requirements.md`. Principle: **the phone is the source of truth for the core loop; the network only enriches it.**

**Re-checked against:** `dda4f4e` (2026-09-29). This is target architecture, not a description of current code, so little here ages — the two spots that made claims about the present tense are annotated.

## 1. Target architecture

```
                 +----------------------------- Android app (Kotlin, Compose) -----------------------------+
                 |  ui/   Home | Kitchen | Planner | List | Cook mode | Share | Community | Helper | Settings |
                 |  domain/  (no Android imports)                                                          |
                 |     engines: Matching, Pricing, Nutrition/Balance, Recommendation, Planner, Units       |
                 |  data/   Room v2 | DataStore | ContentPackUpdater | ShareCodec | ApiClient (optional)   |
                 +-----------------------------------------------------------------------------------------+
                       |  signed static packs (CDN)            |  optional REST/edge functions
                       v                                       v
         Content pipeline (GitHub Actions)              Backend (community, price reports, AI proxy)
         WFP/KAMIS/KFCT/USDA -> validate -> pack         Supabase (Postgres + RLS + Edge Functions)
```

**Repository split** (replaces the single `MealRepository`):
`CatalogueRepository`, `PriceRepository`, `PantryRepository`, `PlanRepository`, `HistoryRepository`, `ProfileRepository` (DataStore), `SharingRepository`, `CommunityRepository`, `AssistantRepository`.

**Engines are pure Kotlin, unit-tested, and take a `Clock`:** `MatchEngine`, `PriceResolver`, `NutritionCalculator`, `BalanceScorer`, `Recommender`, `PlanGenerator`, `UnitConverter`. The AI never replaces these. It calls them.

## 2. Data model v2 (Room)

Migration 1 to 2 is hand-written and covered by the existing `MigrationTestHelperTest` pattern. **Rows that came from packs carry a stable `slug`; user data is never overwritten by a pack.**

> **Harness status (updated 2026-09-29).** The harness now compiles and is correctly wired: `androidx.room.testing` is on `androidTestImplementation`, and `build.gradle.kts` adds `$projectDir/schemas` as an androidTest assets root so the schema reaches the test APK at `com.hakunakuinama.app.data.local.HakunaKuinamaDatabase/1.json`, which is the path `MigrationTestHelper` reads. It had three faults before that and had never compiled at all. CI runs it on an emulator, so it has now been executed — but there is no migration to test yet, so "green" currently only proves the plumbing. See `01-audit.md` R2.

```sql
-- Catalogue (pack-managed)
ingredient(id, slug UNIQUE, category, base_unit /*g|ml|piece*/, density_g_per_ml NULL,
           piece_weight_g NULL, emoji, is_staple, pack_version)
ingredient_i18n(ingredient_id, lang, name, aliases /* list */)
ingredient_pack(id, ingredient_id, label /* "1 kg", "2 kg tin", "KES 20 heap" */,
                base_qty, pack_kind /*STANDARD|SHILLING_HEAP|TIN*/)
nutrient(ingredient_id, energy_kcal, protein_g, fat_g, carb_g, fibre_g, iron_mg, zinc_mg,
         vit_a_ug_rae, vit_c_mg, calcium_mg, source, source_ref) -- per 100 g, NULL = unknown
food_group(ingredient_id, group_code) -- 10 groups for a diversity score
substitution(from_ingredient_id, to_ingredient_id, ratio, note)

meal(id, slug UNIQUE, slot, difficulty, prep_min, cook_min, base_servings, region, needs_fridge,
     needs_oven, fuel_kind, image_ref, reviewed_by, pack_version)
meal_i18n(meal_id, lang, title, tagline)
meal_ingredient(meal_id, ingredient_id, qty_base, is_optional, is_pantry_assumed, role /*MAIN|SIDE|SEASONING*/)
meal_step(meal_id, n, duration_min, text_template /* "Soak {q:cowpeas} ..." */)
meal_step_i18n(meal_id, n, lang, text_template)
meal_diet_flag(meal_id, flag)
meal_tag(meal_id, tag)

-- Prices (observation log)
market(id, county, name, lat_rounded NULL, lon_rounded NULL)
price_obs(id, ingredient_id, pack_id NULL, price_kes, per_base_qty, market_id NULL,
          source /*USER|CROWD|OFFICIAL|SEED*/, observed_on, confidence REAL, ref)
price_override(ingredient_id, pack_id NULL, price_kes) -- the user's own

-- User state (never touched by packs)
profile(...)                          -- in DataStore: county, market, household, budget, diet, allergens, equipment, lang
pantry_item(id, ingredient_id, qty_base, expires_on NULL, paid_kes NULL, added_on)
plan(id, week_start) ; plan_entry(plan_id, day, slot, meal_id, servings)
history(id, meal_id, cooked_on, rating NULL, skipped BOOL)   -- drives anti-repetition
preference(subject_type /*MEAL|INGREDIENT|TAG*/, subject_id, stance /*LIKE|DISLIKE|BLOCK*/)
shopping_item(...) -- existing table extended with pack_id, paid_kes
```

Notes:
- Store **base units** (g, ml, piece) and convert for display with `UnitConverter` (tbsp, cup, "tin", "handful" as approximations with disclosed accuracy).
- **Cost is still derived.** Plate cost = sum of (used qty x resolved price per base unit). Cash needed = packs rounded up, minus pantry, priced by resolved pack price.
- Replace pipe-delimited tags with join tables so diet and allergen filters are SQL.

## 3. Content pipeline

```
sources/                       (CSV/JSON in git, reviewed by PR)
  ingredients.csv  nutrients.csv  meals/*.json  translations/*.json  markets.csv
        |
   validate (CI):  schema, unique slugs, all ingredient refs exist, nutrition present or explicit NULL,
                   price sanity bounds, every meal has en + sw, reviewer set for health-tagged packs
        |
   build:          pack-<version>.json.gz  + manifest.json (version, sha256, min_app_version)  + signature
        |
   publish:        static host (GitHub Pages or object storage)
        |
   app:            WorkManager (Wi-Fi or opt-in mobile) fetches manifest, downloads delta, verifies signature,
                   upserts by slug in one transaction, records pack_version
```

**Nutrition sources (verify licences before redistribution):**
- **Kenya Food Composition Tables 2018** (Government of Kenya and FAO, developed under INFOODS guidelines). PDF and an Excel version are listed on FAO's food-composition site. Also see "Kenya Food Recipes 2018" listed by INFOODS.
- **USDA FoodData Central** to fill gaps (public domain).
- Every nutrient row stores `source` and `source_ref`.

## 4. Price engine

### 4.1 Sources

| Source | What it offers | Caveats |
|---|---|---|
| **WFP Kenya Food Prices** on HDX | Monthly market prices by market and commodity, CSV, CC BY-IGO licence, updated monthly, series starting 2006 | Commodity coverage is limited to staples (maize, beans, rice, sugar and so on), and the last modified date on the page I saw was August 2024, so **verify current coverage** before depending on it |
| **KAMIS** (Kenya Agricultural Market Information System, KALRO and State Dept of Agriculture) | Per its About page: 5 markets in each of the 47 counties, 150+ products, wholesale, retail and farm-gate prices | Programmatic access and reuse terms are not stated on the page I saw. **Ask KALRO for a data-sharing agreement** |
| **KNBS** monthly CPI average retail prices | National retail averages for a food basket | Coarse geography. Verify the current publication format |
| **Crowd reports** (FR-COM-5, FR-PRC-9) | Fresh, hyper-local | Needs outlier control and enough volume |
| **User overrides** | Exact for that person | Personal only |
| **Seed baseline** | Fallback | Must always be labelled "Estimate" |

### 4.2 Resolution

```
resolve(ingredient, market, today):
  1. user override                                   -> confidence 1.0, badge "Your price"
  2. crowd: median of reports in market, last 14 d,  need >= 3 reports, reject beyond 3 MAD  -> badge "Reported, N days ago"
  3. official: latest observation for market, else nearest market in county, else national     -> badge "Market data, <date>"
  4. seed baseline                                   -> badge "Estimate"
  Staleness: official older than 60 d loses confidence linearly; crowd older than 14 d falls through.
```

Return `Price(kes, perBaseQty, source, observedOn, confidence)`. The UI must render source and age (FR-PRC-4).

### 4.3 Location without tracking
User picks **county then market** in onboarding. If they allow coarse location, the app maps to the nearest market on device and stores only the market id. No coordinates leave the device.

## 5. Nutrition and "balanced diet"

Keep it explainable and non-medical.

- **Food-group diversity:** count distinct food groups eaten per day using a 10-group scheme in the style of FAO's Minimum Dietary Diversity for Women (MDD-W). Score 0 to 10 per day.
- **Energy and protein against a target:** target energy from a standard estimate (for example Mifflin-St Jeor with an activity factor) using optional age, sex, height, weight in the profile. If the user does not enter them, use a default range and say so.
- **Micronutrient gaps:** flag low iron, vitamin A or calcium across the week from the recipe totals.
- Output: plain-language line ("3 of 5 food groups today. Add a vegetable or fruit."), never diagnoses.
- **Governance:** a registered dietitian reviews the rules and the copy before launch. Condition-specific packs are out of scope until then.

## 6. Recommendation and planning

### 6.1 Candidate scoring

```
score(meal) = w1*pantryFit      // share of required items on hand, quantity aware, substitutes allowed
            + w2*priceFit       // cash needed vs remaining budget; cheaper-than-usual boosts
            + w3*nutritionGap   // how much it closes today's or this week's gaps
            + w4*novelty        // days since last cooked, region and main-starch variety
            + w5*preference     // learned like/skip/cooked signals, tag affinities
            + w6*seasonal       // ingredient cheaper than its 8-week average
            - penalties         // cooldown (4 d hard), same main starch 2 days running, missing equipment
Hard filters first: allergens, diet flags, equipment, time available, blocked items.
```

- **Diversification:** pick the top-k, then rerank with maximal marginal relevance so the shortlist is not five variations of ugali.
- **Exploration:** with probability epsilon (say 10%), surface a lower-ranked new dish. Later, upgrade to a contextual bandit on-device.
- **Explanation:** each score term produces a phrase, so the card can say why.
- **Weights** live in a config pack so they can be tuned without a release.

### 6.2 Weekly plan generation
Greedy construction with local search:
1. Fill each slot with the best feasible meal under hard filters and cooldown.
2. Swap to reduce total cash needed and raise balance score while keeping variety constraints (no dish twice within cooldown, at least 3 distinct main starches per week, 1 surprise slot).
3. Stop when the plan fits the budget, or report the smallest shortfall and the cheapest changes.

This is small enough to run on-device in milliseconds at 500 recipes with an ingredient inverted index.

## 7. Sharing without accounts

**Tier 0: link, QR and card (no backend).**
- Serialize the object (menu, plan, recipe or list) to a compact versioned JSON, compress, base64url. Put it in the URL fragment: `https://<your-domain>/s#<payload>` (fragments are never sent to a server). An Android App Link opens the app if installed, else a static web page previews it.
- Also render an image card locally for WhatsApp.
- Import validates schema and size and shows a preview before saving. Never execute or trust embedded content.

**Tier 1: anonymous publishing (needs backend).**
- On first publish, generate an **Ed25519 key pair** in Android Keystore. The public key is the identity. Auto nickname ("Sukuma-4821") and avatar. No email or phone.
- Each post is signed. The server verifies the signature, checks a **Play Integrity** token, and rate-limits per key and per IP range.
- Recovery is by explicit key export (QR or file). Exclude the key from Auto Backup by default.
- Supabase alternative: **anonymous sign-ins** provide an authenticated but anonymous user with a JWT, which works with row-level security. Simpler to build, weaker portability than a key pair. Either works. Pick one and document it.

**Tier 2: crew sync.** A short share code (for example `KUCHA-7QF2`) joins a room. Realtime rows for shared plan and pantry, scoped by room id with RLS. Codes expire and can be rotated.

## 8. Community (backend sketch)

- Postgres tables: `posts(kind, author_key, county, payload, created_at, status)`, `reactions`, `reports`, `price_reports`, `bans`.
- **Row-level security:** anyone reads `status = 'visible'`. Authors insert only with a verified signature or JWT. No updates except by author within a window. Admin role for moderation.
- **Kinds:** MENU, COOKED, PRICE, QUESTION. Each with a JSON schema and length limits. Images optional, resized on device, scanned by an image-moderation service before becoming visible.
- **Moderation:** report button, auto text filter (English, Kiswahili, Sheng word lists), queue in an admin dashboard (a small Next.js app), ban by key, shadow-ban for spam, rate limits.
- **No DMs. No links from new keys** in the first 24 hours (spam control).
- Price posts flow into `price_obs` only through the outlier pipeline.

## 9. AI kitchen helper

```
App -> Edge function (auth token + Play Integrity + daily cap)
     -> LLM with TOOLS: search_recipes(pantry, filters), get_prices(items, market), get_nutrition(items),
                        scale_recipe(meal, people), suggest_substitutions(item)
     -> returns JSON {"options":[{"meal_slug":..., "missing":[...], "why":"..."}], "tips":[...]}
App validates against a schema, renders natively using OUR numbers.
```

- **Grounding rule:** the model may choose, explain and teach. It may **not** produce prices, nutrition figures or quantities that the app has not computed.
- **Modes:** cook with what I have, technique and rescue, leftover remix, Kiswahili replies.
- **Safety:** food-safety rules baked into the system prompt (reheating rice, cooking chicken through, storing cooked beans), allergen caution, no medical advice, refuse non-food topics.
- **Cost control:** server-side proxy holds the key, per-device daily cap, cache identical pantry-plus-constraint requests, small context, cheaper model for simple turns. Track cost per active user.
- **Offline fallback:** the rule-based recommender answers "what can I cook with this" when the helper is unavailable.
- **On-device option:** on supported devices Android's on-device Gemini Nano (via ML Kit GenAI and AICore) may work for short tasks. Support varies by device, so treat it as an accelerator and **verify current device coverage** before promising it.
- **Evaluation:** a fixed set of at least 100 Kenyan pantry prompts scored for correctness, safety and Kiswahili quality, run before each release.

## 10. Scanning

| Feature | Tech | Notes |
|---|---|---|
| Barcode | ML Kit Barcode Scanning (on-device, no network needed to decode) | Lookup Open Food Facts (ODbL, open database, Kenyan coverage is patchy). On a miss, let the user name the product and share it to a community product table |
| Price at scan | Optional one-tap price entry | Creates a `price_obs` with source USER or CROWD |
| Receipt OCR | ML Kit Text Recognition (on-device) | Parse lines, fuzzy-match to ingredients, always ask the user to review. Many retailers' receipts now carry tax QR codes, which may simplify parsing (investigate) |
| Pantry photo | Cloud vision model, opt-in, capped | User confirms every detected item |
| SMS reading | Rejected | Privacy and Play policy risk |

## 11. Tech choices

| Concern | Choice | Reason |
|---|---|---|
| Android | Keep Kotlin, Compose, Hilt, Room, Coroutines. Upgrade toolchain and `targetSdk` to 36 | Existing investment, Play requirement |
| Prefs | DataStore (already a dependency) | Profile and settings |
| Background | WorkManager | Pack and price updates |
| Networking | Ktor client or Retrofit + OkHttp, kotlinx.serialization | Small, testable |
| Images | Coil (already) with size and data-saver rules | Optional downloads |
| Backend | Supabase (Postgres, RLS, Edge Functions, anonymous sign-in) plus a small Next.js admin | Fast to build, RLS fits community rules |
| CI | GitHub Actions: build, unit tests, lint, content validation, pack build | Gate quality |
| Analytics | Opt-in, minimal event counts. No advertising IDs | Trust and legal |

## 12. Privacy and safety design checklist

- No precise location leaves or is stored. County and market only.
- Identity key excluded from Auto Backup. Data export and delete in settings.
- AI requests contain pantry and constraints only.
- Privacy policy and terms before any network feature ships. Legal review under the Data Protection Act, 2019.
- Signed packs, schema-validated inputs, size limits, no dynamic code from content.
- Health copy reviewed by a dietitian. Food-safety notes in cook mode.
