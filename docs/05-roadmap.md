# 05. Roadmap

Sequenced by the council verdict: **data spine, then personalization, then sharing, then assistance.** Estimates assume 1 to 2 developers working part-time and are ranges, not promises. Do not start a phase until the previous phase's exit gate is met.

## Phase overview

| Phase | Name | Rough duration | Outcome |
|---|---|---|---|
| P0 | Foundations and trust fixes | 1 to 2 weeks | Green CI, current toolchain, four bugs fixed, README honest |
| P1 | Real data spine | 4 to 6 weeks | 150 ingredients, 60 recipes, nutrition, units, prices with sources, county and market, user overrides, content packs |
| P2 | Never bored | 4 to 5 weeks | Pantry with quantities, recommender, weekly planner, balance score, smart list, cook mode |
| P3 | Share | 3 to 4 weeks | Link, QR and WhatsApp sharing, then a moderated anonymous community MVP |
| P4 | Assist and scan | 4 to 6 weeks | Barcode, receipt OCR, grounded AI helper, voice |
| P5 | Grow | ongoing | Crew sync, dietitian packs, group buying, USSD or WhatsApp bot, B2B |

## P0. Foundations and trust fixes (1 to 2 weeks)

- Fix audit bugs **L1** (hidden salt in match denominator), **L2** ("cheapest first" label or sort toggle), **L4** (servings vs batches), **L6** (hard-coded step quantities become placeholders).
- Repo hygiene: add `.kotlin/` to `.gitignore` and untrack the session file, add `LICENSE`, replace README screenshot placeholder with real captures, correct the test count.
- **CI:** GitHub Actions running `assembleDebug`, `testDebugUnitTest`, lint.
- **Toolchain:** upgrade AGP, Kotlin and Compose BOM as needed, raise `compileSdk` and `targetSdk` to 36, verify on a real device, run the never-run `androidTest` migration test on an emulator.
- Decide crash reporting (opt-in) and analytics policy.

**Exit gate:** green CI on `main`, APK runs on a physical device, Play-compliant target API, bug regression tests added.

## P1. Real data spine (4 to 6 weeks)

1. **Schema v2 and Room migration 1 to 2** (see `04`), including translations, packs, nutrients, aliases, substitutions, diet flags.
2. **Content pipeline** and pack updater (WorkManager, signed manifests, delta updates).
3. **Content growth:** 150 ingredients, 60 recipes across regions, every recipe with reviewer, region, equipment and both languages. Import Kenya Food Composition Tables nutrition, fill gaps from USDA FoodData Central.
4. **Units:** cooking units vs purchase packs, "tin/gorogoro" and KES-heap packs (verify local sizes with traders), step placeholders.
5. **Prices:** observation log, resolver, county and market picker, user overrides, source badges, monthly WFP import job, request KAMIS access.
6. **Profile and onboarding** (DataStore): language, county, market, household, budget, diet, allergies, equipment.
7. **Imagery:** photograph or illustrate the 60 recipes, with a data-saver rule.
8. Nutrition reviewer engaged (dietitian) for copy and rules.

**Exit gate:** a fresh install onboards in under 60 s, shows a real local price with source and age for at least 90% of ingredients used in seeded recipes, a pack update installs without touching favourites, CI validates content.

## P2. Never bored (4 to 5 weeks)

1. **Pantry** with quantities and optional expiry, search-first picker with aliases, substitutions, matching by quantity (fixes L7).
2. **Recommender v1** (score in `04`), cooldowns, "not for me", explanations on every card, weekly "surprise me".
3. **History:** mark cooked, rating, auto-deduct pantry.
4. **Planner:** manual and auto-plan, balance score, gap filler, month-end survival mode.
5. **Shopping list v2:** purchase-pack rounding, pantry subtraction, actual price paid, plate cost vs cash needed.
6. **Cook mode:** big text, keep-awake, timers.
7. Rename the Kitchen tab and add Planner and List as first-class destinations.

**Exit gate:** in a 2-week internal test, no dish repeats within cooldown, planner produces a plan within budget for three personas, unit tests cover scoring and planning.

## P3. Share (3 to 4 weeks)

1. **Tier 0 sharing:** link, QR, WhatsApp image card, import preview, App Link plus static web preview page.
2. Backup and restore to JSON.
3. **Backend foundation:** Supabase project, RLS policies, Play Integrity, edge functions, admin dashboard.
4. **Community MVP:** anonymous key identity, feed of MENU, COOKED, PRICE and QUESTION posts, reports, rate limits, text filter, admin actions, terms and privacy policy, county boards.
5. Price reports feeding the crowd layer through outlier rejection.
6. Legal check (Data Protection Act, 2019) and takedown process **before** public launch.

**Exit gate:** moderation tools live and tested, a private beta of at most 100 users for 2 weeks with all reports actioned within 24 h, no critical abuse found.

## P4. Assist and scan (4 to 6 weeks)

1. **Barcode scanning** (ML Kit) with Open Food Facts lookup and a community product table, optional price capture.
2. **Receipt OCR** with review screen.
3. **AI kitchen helper:** edge proxy, tool calls, schema-validated output, Kiswahili, daily cap, offline fallback, 100-prompt evaluation set.
4. Voice read-aloud and hands-free "next" in cook mode.
5. Optional pantry photo via a vision model, capped.

**Exit gate:** evaluation set passes agreed thresholds, cost per active user under the agreed cap, app fully usable with AI off.

## P5. Grow (ongoing)

Crew sync for housemates, dietitian-reviewed condition packs, group buying with a chama (payments via a mobile-money API, requires a compliance review), USSD or WhatsApp bot for feature phones, B2B dashboards (hostels, schools, NGOs) built on the price and nutrition dataset, Sheng voice, regional content expansion.

## Cut list (if time runs short, cut in this order)

1. Pantry photo, voice commands, USSD or WhatsApp bot
2. Receipt OCR, cheapest-market comparison
3. Crew sync, group buying
4. Community feed (keep link/QR sharing)
5. AI helper (keep the rule-based recommender)

**Never cut:** content pipeline, price source and age badges, plate cost vs cash needed, cooldown-based variety, CI and migration tests, health-claim guardrails.

## Backlog seed (first 25 tickets)

| # | Ticket | Phase |
|---|---|---|
| 1 | Fix hidden-pantry match denominator and add test | P0 |
| 2 | Align "cheapest first" label with sort or add sort toggle | P0 |
| 3 | Convert servings multiplier to people-per-recipe | P0 |
| 4 | Tokenize step quantities | P0 |
| 5 | `.gitignore` `.kotlin/`, untrack session file | P0 |
| 6 | Add LICENSE, real README screenshots | P0 |
| 7 | GitHub Actions CI | P0 |
| 8 | Upgrade toolchain and target API 36 | P0 |
| 9 | Run `androidTest` migration test on emulator | P0 |
| 10 | Design content-pack schema v2 | P1 |
| 11 | Room migration 1 to 2 with tests | P1 |
| 12 | Import KFCT nutrition for existing 27 ingredients | P1 |
| 13 | Price observation table and resolver with tests | P1 |
| 14 | County and market picker | P1 |
| 15 | User price override UI | P1 |
| 16 | WFP HDX monthly price import job | P1 |
| 17 | Pack updater with signature verification | P1 |
| 18 | DataStore profile and onboarding | P1 |
| 19 | Author 33 more recipes and 123 more ingredients | P1 |
| 20 | Search-first pantry picker | P2 |
| 21 | Recommender with cooldown and explanations | P2 |
| 22 | Weekly planner and balance score | P2 |
| 23 | Shopping list with pack rounding | P2 |
| 24 | Cook mode | P2 |
| 25 | Tier 0 sharing (link, QR, card) | P3 |
