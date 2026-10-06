# 03. Product Requirements: Hakuna Kuinama 2.0

**Status:** draft for team review. **Depends on:** `01-audit.md`, `02-council-verdict.md`.
**Re-checked against:** `dda4f4e` (2026-09-29). Requirements whose acceptance criteria are partly satisfied by P0 work are marked inline; see `01-audit.md` §3 and §7 for the per-finding status.
**Priority key (MoSCoW):** M = must (v2.0), S = should, C = could, W = won't (this cycle).
**Phase key:** P0 to P5, see `05-roadmap.md`.

## 1. Vision and scope

**Vision.** "No sleeping on food": help a Kenyan on a tight budget eat properly today and this week using the food they have and the money they have, in their own market and language.

**One-sentence promise.** Tell us your money, your kitchen and your county, and we tell you what to cook, what to buy, what it will really cost, and whether it is balanced.

**In scope for 2.0.** Real Kenyan catalogue, balanced-diet scoring, location-aware and user-set prices, pantry, weekly planner, recommendations, smart shopping list, cook mode, sharing without accounts, food-only community, AI kitchen helper, scanning.

**Out of scope for 2.0 (W).** Medical or therapeutic diets (diabetes, renal, pregnancy) beyond dietitian-reviewed general packs, online grocery ordering or delivery, payments, direct messaging between users, iOS.

## 2. Personas

| Persona | Situation | Needs |
|---|---|---|
| **Brian, 20, university student** | Fixed allowance or loan disbursement, shares a hostel room, one-burner jiko or gas, no fridge | Cheapest filling meals, survive to month-end, use what he has, cook fast |
| **Achieng, 27, young professional** | Shared flat, works long hours, wants to eat healthier without spending more | Weekly plan, balanced meals, meal prep, variety, shopping list |
| **Mama Kevin, 38, household of 5** | Buys by heaps and shillings at the market, feeds children | Cost per family plate, kids' nutrition, seasonal cheap swaps, buy-by-shillings |

## 3. Functional requirements

Each requirement has an ID, priority, phase and acceptance criteria (AC).

### 3.1 Catalogue and nutrition (FR-CAT)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-CAT-1 | Ship at least **150 ingredients and 60 recipes** covering Coast, Central, Western, Rift, Nyanza, North and Somali cuisines, each with a stable `slug` | M | P1 | Count check in CI. Each recipe has region tag, slot, difficulty, prep and cook time |
| FR-CAT-2 | Every ingredient has **nutrition per 100 g** (energy, protein, fat, carbs, fibre, iron, zinc, vitamin A, vitamin C, calcium at minimum) with a `source` and `source_ref` | M | P1 | Import from Kenya Food Composition Tables 2018 first, USDA FoodData Central to fill gaps. Missing values are explicit, never zero |
| FR-CAT-3 | Ingredients have **aliases and translations** (English, Kiswahili, optionally local names) | M | P1 | Searching "kale", "sukuma" or "collard" finds the same item |
| FR-CAT-4 | Recipe **content is translatable** (title, tagline, steps) via a translation table, not Kotlin strings | M | P1 | Switching device language changes recipe text or falls back to English per field |
| FR-CAT-5 | Each ingredient has a **cooking unit** (g, ml, piece, tbsp, tsp, cup) and **purchase packs** (1 kg, 2 kg tin/gorogoro, 500 ml, bunch, KES-10 heap) with conversion factors | M | P1 | 0.06 L oil renders as "4 tbsp"; buying it renders as "250 ml bottle" |
| FR-CAT-6 | Recipe steps use **quantity placeholders** resolved at render time | M | P1 | Scaling servings changes step text quantities. No hard-coded grams remain in steps |
| FR-CAT-7 | Structured **diet flags** (vegetarian, vegan, halal, contains pork, contains gluten, contains dairy, contains eggs, contains nuts, contains fish) derived from ingredients | M | P1 | Filter and exclusion work across the catalogue. Allergen exclusion is a hard filter |
| FR-CAT-8 | Real **food photography or consistent illustrations** for every recipe | S | P1 | No recipe shows the placeholder art. Images are lazy-loaded and optional on metered data |
| FR-CAT-9 | Catalogue **updates without an app release** through signed, versioned content packs | M | P1 | App applies a delta pack, upserts by slug, never overwrites user data. Pack signature verified |
| FR-CAT-10 | Recipes include **equipment and fuel** needs (jiko, gas, single burner, no fridge, oven) | S | P2 | Filter by available equipment. "No fridge" hides recipes needing refrigeration |

### 3.2 Pricing (FR-PRC)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-PRC-1 | Model prices as **observations**: ingredient, price per base unit, market or county, source (USER, CROWD, OFFICIAL, SEED), observed date, confidence | M | P1 | No price is stored without source and date |
| FR-PRC-2 | User picks **county and market** (no GPS required). Optional GPS only maps to the nearest market on device and is never stored | M | P1 | Prices shown are for the selected market or the nearest available, labelled |
| FR-PRC-3 | **Price resolution order:** user override, fresh local crowd (median of recent reports), official data, seed baseline | M | P1 | Unit tests cover each precedence and staleness cut-offs |
| FR-PRC-4 | Every price displays a **badge**: "Your price", "Nakuru market, 3 days ago", "Estimate" | M | P1 | Visible on list, recipe and shopping list. Tap explains the source |
| FR-PRC-5 | User can **override any ingredient price** and set purchase-pack prices | M | P1 | Override wins everywhere and is reversible |
| FR-PRC-6 | Show **plate cost** (consumption) and **cash needed** (rounded to purchase packs, minus pantry) | M | P1 | Audit bug L5 resolved. Both numbers labelled |
| FR-PRC-7 | **Buy-by-shillings** mode for heap-sold produce ("KES 20 of sukuma") converting to quantity | S | P2 | Purchase pack type `SHILLING_HEAP` supported in list and recipes |
| FR-PRC-8 | **Price trend** per ingredient over recent weeks and "cheaper this week" hints | S | P2 | Uses official series and crowd data. Hidden when data is thin |
| FR-PRC-9 | Confirm-price micro-task: "Is sukuma still KES 20?" one-tap yes/no/change | C | P3 | Feeds crowd layer with outlier rejection |

### 3.3 Pantry (FR-PAN)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-PAN-1 | **Search-first ingredient picker** with categories, recents and aliases. Replaces the paged chip grid | M | P2 | Find any ingredient in at most 3 taps or 1 search |
| FR-PAN-2 | Pantry stores **quantity, unit, optional expiry and purchase price** | M | P2 | "Have plenty / some / low" quick mode plus exact mode |
| FR-PAN-3 | **Auto-deduct** pantry when a meal is marked cooked. **Auto-add** when shopping items are bought | S | P2 | Reversible, with undo |
| FR-PAN-4 | Expiry reminders and "use it up" suggestions | S | P3 | Local notifications, opt-in |
| FR-PAN-5 | Matching honours **quantities** and **substitutions** (tomato paste for tomatoes) | M | P2 | **Half already shipped in P0:** the "ignores pantry staples" half of this requirement is done — `MealMatch.of` scores against `Meal.shoppableIngredients`, so a recipe is judged only on things the shopper would have to buy (`a291515`). Still open, and still P2: audit bug **L7** (no aliases, no Swahili names, no substitution graph — `Ingredient` has no such fields). Substitution must show price and nutrition delta |

### 3.4 Planner and balanced diet (FR-PLN)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-PLN-1 | **Weekly planner** (7 days by breakfast, lunch, dinner, snack) with drag or tap edit | M | P2 | Persisted, editable, shareable |
| FR-PLN-2 | **Auto-plan** given budget, household size, days, diet, allergens, equipment, pantry | M | P2 | Plan total at or under budget or explains why impossible |
| FR-PLN-3 | **Balance score** per day and week: food-group diversity (10 food groups, MDD-W style) plus energy and protein against a target from profile | M | P2 | Score explained in plain language. Not a medical claim |
| FR-PLN-4 | **Gap filler**: "No vegetables in 2 days. Add sukuma (KES 20)" | S | P2 | Suggests the cheapest fix |
| FR-PLN-5 | **Month-end / survival mode**: input remaining KES and days until next allowance, produce cheapest adequate plan | S | P2 | Warns if remaining cash cannot meet minimum needs |
| FR-PLN-6 | Household size and per-person portions (adult, child) | M | P2 | Quantities scale by people, not batches (audit L4) |
| FR-PLN-7 | Condition-specific packs (e.g. diabetes-friendly, child weaning) | W | P5 | Only after dietitian review and legal review |

### 3.5 Recommendations (FR-REC)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-REC-1 | Home "what to eat now" ranks candidates by pantry fit, price fit, nutrition gap, novelty and preference | M | P2 | Ordered by a documented, unit-tested score |
| FR-REC-2 | **Anti-repetition:** per-dish cooldown (default 4 days), per-main-starch soft penalty, and a weekly "surprise me" slot | M | P2 | No dish repeats within cooldown unless it is the only option. Users see why it was picked |
| FR-REC-3 | Learn from **cooked, liked, skipped, disliked** signals, stored on device | M | P2 | "Not for me" removes a dish or an ingredient for good, reversible in settings |
| FR-REC-4 | Explanations: "Uses 5 things you have, KES 40 to finish, adds iron" | M | P2 | Every card has a reason line |
| FR-REC-5 | **Twist** generator: same base, different sauce or side | C | P3 | Suggested variations from a curated map |
| FR-REC-6 | Seasonality and price-trend boost ("cheap now") | S | P3 | Uses price trend data |
| FR-REC-7 | Optional cloud-side collaborative signals | W | P5 | Not needed for v2 |

### 3.6 Shopping and budget (FR-SHP)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-SHP-1 | List merges across plan, subtracts pantry, rounds to **purchase packs**, groups by market section | M | P2 | Shows cash total and leftover value |
| FR-SHP-2 | Record **actual price paid** per item when ticking | S | P2 | Feeds user price layer and budget tracker |
| FR-SHP-3 | **Budget tracker**: planned vs actual vs remaining, per week and month | M | P2 | Uses actual prices |
| FR-SHP-4 | **Cheapest market** suggestion for a list where multiple markets have data | C | P4 | Shows total per market with data age |
| FR-SHP-5 | Share list as text or image (WhatsApp) | S | P3 | Renders locally |

### 3.7 Cook mode (FR-COO)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-COO-1 | Step-by-step **cook mode**: big text, keep screen awake, step timers, next and back | M | P2 | Works offline, one-handed |
| FR-COO-2 | Voice read-aloud (Kiswahili and English text-to-speech) and voice commands | C | P4 | Uses Android TTS. Hands-free "next" |
| FR-COO-3 | **Fuel and time cost** estimate per meal (gas, charcoal, electricity) from user-set fuel price | C | P3 | Displayed as optional line |

### 3.8 Sharing without accounts (FR-SHR)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-SHR-1 | Share a **menu, plan, recipe or shopping list** as link, QR and WhatsApp card. No backend needed | M | P3 | Recipient with the app imports it. Without the app, sees a web preview |
| FR-SHR-2 | Payload is compact (target under 2 KB), versioned and validated on import | M | P3 | Malformed payloads are rejected safely |
| FR-SHR-3 | **Crew mode**: housemates share a plan and pantry through a share code, synced through the optional backend | S | P5 | No email or phone required |

### 3.9 Community (FR-COM)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-COM-1 | **Anonymous identity**: device-generated key, auto nickname and avatar, no email or phone | S | P3 | Posts signed and verified server-side |
| FR-COM-2 | Food-only feed: published menus, "I cooked this" posts, price reports, questions. County boards | S | P3 | Every post type is schema-validated |
| FR-COM-3 | **Moderation**: report, hide, rate limits, auto text filters (English, Kiswahili, Sheng), admin dashboard, ban by key | M (if FR-COM-2 ships) | P3 | Reports actioned in the admin tool. No community launch without this |
| FR-COM-4 | **No direct messages** in v2 | M | P3 | Reduces abuse surface |
| FR-COM-5 | Price reports enter the crowd layer only after outlier checks | M | P3 | Median of at least N recent reports in the same market |
| FR-COM-6 | Terms, privacy policy, takedown process, minimum-age statement | M | P3 | Linked in app and Play listing |

### 3.10 AI kitchen helper (FR-AI)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-AI-1 | "Cook with what I have": from pantry and constraints, propose up to 3 meals with missing items and cost | S | P4 | Uses tools (search recipes, prices, nutrition). Numbers come from the app, not the model |
| FR-AI-2 | Technique and rescue help ("too salty", "ugali is lumpy", "what does the oil separates mean") | S | P4 | Food-safety guardrails and Kiswahili support |
| FR-AI-3 | **Leftover remix** | C | P4 | Suggests dishes from cooked leftovers |
| FR-AI-4 | **Grounding and safety:** structured JSON output validated against a schema. No medical advice. Allergen disclaimers | M (if AI ships) | P4 | Evaluation set of at least 100 Kenyan pantry prompts run before each release |
| FR-AI-5 | **Cost controls:** server proxy, per-device daily cap, response caching, offline rule-based fallback | M (if AI ships) | P4 | App is fully usable with AI off or over quota |
| FR-AI-6 | AI is **opt-in** and states what is sent | M | P4 | Pantry list and constraints only. No identifiers |

### 3.11 Scanning (FR-SCN)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-SCN-1 | **Barcode scan** to add packaged items, on-device (ML Kit), lookup via Open Food Facts then a community product table | S | P4 | Miss path lets the user name the item and captures it for others |
| FR-SCN-2 | Capture **price paid** at scan time as a price observation | S | P4 | Optional, one tap |
| FR-SCN-3 | **Receipt OCR** on device to add pantry items and prices | C | P4 | User reviews every parsed line before saving |
| FR-SCN-4 | **Pantry photo** (fridge or shelf) via a vision model to propose ingredients | C | P5 | User confirms. Cloud, opt-in, capped |
| FR-SCN-5 | Read SMS (M-Pesa) to infer spending | W | n/a | Rejected: privacy and Play policy risk |

### 3.12 Profile, onboarding, settings (FR-SET)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-SET-1 | 60-second **onboarding**: language, county and market, household, budget period and amount, diet, allergies, equipment | M | P1 | Skippable, editable later. Stored in DataStore |
| FR-SET-2 | Settings: units, currency format, notifications, data saver (no images on mobile data), AI on/off, data export and delete | M | P1 | Export and delete work fully offline |
| FR-SET-3 | Backup and restore to a JSON file | S | P3 | Round-trip test |

### 3.13 Data operations (FR-DAT)

| ID | Requirement | Pri | Ph | Acceptance criteria |
|---|---|---|---|---|
| FR-DAT-1 | Content pipeline: source files (CSV or JSON) to validated, signed pack, with CI checks (schema, duplicate slugs, missing nutrition, price sanity bounds) | M | P1 | Pull request fails on invalid data |
| FR-DAT-2 | Monthly price import job from WFP HDX (and KAMIS once access is agreed) producing a market price pack | M | P1 | Idempotent, versioned, documented |
| FR-DAT-3 | Editorial review flow for recipes and nutrition claims | M | P1 | Named reviewer field. Dietitian sign-off before health-related packs |

## 4. Non-functional requirements

| ID | Category | Requirement |
|---|---|---|
| NFR-1 | Offline | Core loop (pantry, plan, list, cook, prices from last pack) works with no network |
| NFR-2 | Performance | Cold start under 2 s and pantry match under 100 ms on a 2 to 3 GB RAM Android Go class device with 500 recipes |
| NFR-3 | Size | APK/AAB base under 25 MB. Images and packs downloaded on demand |
| NFR-4 | Data cost | Content and price updates as compressed deltas, typically under 200 KB. Images optional on mobile data |
| NFR-5 | Accessibility | Keep the existing 48 dp targets, 200% font scale, TalkBack labels, non-gesture alternatives. *(Note: "existing" was not accurate when this was written — the Menu Builder pager buttons were 34dp. Fixed in `69249ed`; the requirement now describes reality again.)* |
| NFR-6 | i18n | English and Kiswahili complete for chrome and content. Sheng optional. Native-speaker review before release |
| NFR-7 | Privacy | No account required for core features. No precise location stored. Data export and delete. Opt-in analytics |
| NFR-8 | Security | API keys only on a server proxy. Signed content packs. Input validation on every import and post |
| NFR-9 | Reliability | Crash-free sessions of at least 99.5%. No destructive migrations. Every schema bump has a migration test |
| NFR-10 | Quality gate | CI runs build, unit tests and lint on every pull request. `androidTest` migration tests on an emulator before release |
| NFR-11 | Compliance | Target API level as required by Google Play (36 as of 2026-08-31). Data Protection Act 2019 review. Content policy |
| NFR-12 | Maintainability | `domain` module free of Android imports (existing rule). Repositories split by concern |

## 5. Success metrics

| Metric | Target after 3 months of public use |
|---|---|
| Activation: user completes onboarding and ticks or scans 5 pantry items | 60% |
| Weekly retention (W4) | 25% |
| Plans created per active user per month | 2 or more |
| Repeat-dish rate (same dish within 4 days) | under 10% |
| Price freshness: share of shown prices under 14 days old | 70% |
| User-reported price accuracy ("close to what I paid") | 80% |
| Community: reported posts actioned within 24 h | 95% |
| AI helper: cost per active user per month | under an agreed cap |

## 6. Assumptions, risks and open questions

**Assumptions**
- Users are mostly on Android with 2 to 4 GB RAM and limited data bundles.
- Official price series exist for the staple basket but not for every item or market.

**Risks**

| Risk | Likelihood | Impact | Mitigation |
|---|---|---|---|
| Wrong or stale prices destroy trust | High | High | Source and age badges, confidence, user override, outlier filters |
| Community abuse and moderation load | High | High | Defer, food-only, no DMs, rate limits, admin tool before launch |
| Health-claim liability | Medium | High | General guidance only, dietitian review, disclaimers |
| AI hallucination on quantities or safety | Medium | High | Tool-grounded numbers, schema validation, eval set |
| AI and backend cost | Medium | Medium | Caps, caching, offline fallback, opt-in |
| Data licensing (KAMIS, brand data) | Medium | Medium | Formal agreements, prefer open datasets first |
| Team capacity | High | High | Strict phase gates, cut list in `05-roadmap.md` |

**Open questions**
1. Who is the named nutrition reviewer, and what is the review process?
2. Will KALRO or the State Department of Agriculture share KAMIS data, and under what terms?
3. Preferred backend for community and AI proxy (Supabase or another), and who operates it?
4. Photography: commission, license, or illustrate?
5. Does the team want a paid tier later (for example crew sync, dietitian packs), or stay free with sponsorship?
6. Which launch counties or campuses first?
