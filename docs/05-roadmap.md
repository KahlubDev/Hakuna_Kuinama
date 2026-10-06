# 05. Roadmap

**Re-checked against:** `dda4f4e` (2026-09-29). P0 progress is marked inline below; per-finding status lives in `01-audit.md`.

Sequenced by the council verdict: **data spine, then personalization, then sharing, then assistance.** Estimates assume 1 to 2 developers working part-time and are ranges, not promises. Do not start a phase until the previous phase's exit gate is met.

## Phase overview

| Phase | Name | Rough duration | Outcome |
|---|---|---|---|
| P0 | Foundations and trust fixes | 1 to 2 weeks | Green CI, current toolchain, four bugs fixed, README honest — **bugs 2/4, hygiene 2/4, CI done, toolchain not started** |
| P1 | Real data spine | 4 to 6 weeks | 150 ingredients, 60 recipes, nutrition, units, prices with sources, county and market, user overrides, content packs |
| P2 | Never bored | 4 to 5 weeks | Pantry with quantities, recommender, weekly planner, balance score, smart list, cook mode |
| P3 | Share | 3 to 4 weeks | Link, QR and WhatsApp sharing, then a moderated anonymous community MVP |
| P4 | Assist and scan | 4 to 6 weeks | Barcode, receipt OCR, grounded AI helper, voice |
| P5 | Grow | ongoing | Crew sync, dietitian packs, group buying, USSD or WhatsApp bot, B2B |

## P0. Foundations and trust fixes (1 to 2 weeks)

Status as of `dda4f4e`, plus CI and the instrumentation-suite fixes landed the same day: **2 of 4 bugs fixed, 2 of 4 hygiene items done, CI done, toolchain not started.** The API 36 bump is now the item with a deadline attached.

- Trust bugs — **2 of 4 done.** **L1** fixed (`a291515`, +2 regression tests). **L2** fixed by changing the label rather than adding a sort toggle (`02aab77`); the sort's redundant `canCookNow`/`matchPercentage` keys remain. **L4** and **L6** are *deferred as one paired change*, not two: `2e3fc4b` adds matching `TODO(servings-scaling)` blocks in `MealRepositoryImpl` and `MealStep` stating that the batch arithmetic and the step quantities must be fixed together "or neither". Tickets 3 and 4 below are therefore one ticket.
- Repo hygiene — **2 of 4 done.** `.kotlin/` gitignored and the session file untracked (`65af64f`); the README test count corrected from 103 to 109. Still to do: `LICENSE`, and real README screenshots. *(The test count was wrong when the audit was written and drifted again with the two tests added in `a291515`. It is a hand-maintained number in four places, which is the actual problem — consider generating it or dropping it.)*
- **CI — done 2026-09-29.** `.github/workflows/ci.yml` runs `assembleDebug`, `assembleDebugAndroidTest`, `testDebugUnitTest` and `lintDebug` on every push and PR to `main`, plus an emulator job for `connectedDebugAndroidTest`, and uploads the lint and test reports as artifacts.
- **Instrumentation — fixed and wired, not yet run by hand.** The suite had never compiled. Three faults: `androidx.room.testing` was on `testImplementation` (invisible to `androidTest`, unused by `src/test`); the committed schema was never published into the androidTest APK's assets, so `MigrationTestHelper` could not find it; and one test asserted on a repository path from on-device code, which can never hold. All three are fixed and the schema is verified at the asset path Room reads. The emulator job in CI is the first real execution of these tests.
- **Toolchain — not started.** `compileSdk` and `targetSdk` are still 34. This is now the **top release blocker**, ahead of the bug fixes: Play rejects updates below API 36, and the 2026-11-01 extension deadline is not far off.
- Crash reporting (opt-in) and analytics policy — not decided.

**Exit gate:** green CI on `main`, APK runs on a physical device, Play-compliant target API, bug regression tests added. **Not met** — CI exists, but API 34 remains and nobody has run the app or its instrumentation tests on physical hardware.

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

Status column added at `dda4f4e`. **Done** means merged on `main`.

| # | Ticket | Phase | Status |
|---|---|---|---|
| 1 | Fix hidden-pantry match denominator and add test | P0 | **Done** (`a291515`) |
| 2 | Align "cheapest first" label with sort or add sort toggle | P0 | **Done** — took the label branch, not the toggle ("cheapest on a tie", `02aab77`) |
| 3 | Convert servings multiplier to people-per-recipe | P0 | Open — **must ship with #4** |
| 4 | Tokenize step quantities | P0 | Open — **must ship with #3** |
| 5 | `.gitignore` `.kotlin/`, untrack session file | P0 | **Done** (`65af64f`) |
| 6 | Add LICENSE, real README screenshots | P0 | Open |
| 7 | GitHub Actions CI | P0 | **Done** (`.github/workflows/ci.yml`) |
| 8 | Upgrade toolchain and target API 36 | P0 | Open — **highest priority remaining** |
| 9 | Run `androidTest` migration test on emulator | P0 | Wired — suite fixed and compiling, CI runs it on an emulator. Still no human run on hardware |
| 10 | Design content-pack schema v2 | P1 | Open — critical path |
| 11 | Room migration 1 to 2 with tests | P1 | Open |
| 12 | Import KFCT nutrition for existing 27 ingredients | P1 | Open |
| 13 | Price observation table and resolver with tests | P1 | Open |
| 14 | County and market picker | P1 | Open |
| 15 | User price override UI | P1 | Open |
| 16 | WFP HDX monthly price import job | P1 | Open |
| 17 | Pack updater with signature verification | P1 | Open |
| 18 | DataStore profile and onboarding | P1 | Open |
| 19 | Author **55** more recipes (60 − 5 seeded) and 123 more ingredients (150 − 27) | P1 | Open |
| 20 | Search-first pantry picker | P2 | Open |
| 21 | Recommender with cooldown and explanations | P2 | Open |
| 22 | Weekly planner and balance score | P2 | Open |
| 23 | Shopping list with pack rounding | P2 | Open |
| 24 | Cook mode | P2 | Open |
| 25 | Tier 0 sharing (link, QR, card) | P3 | Open |

Also still open from `01-audit.md` but not in the first 25: L3 (suggestion cannot rotate), L5 (plate cost vs cash outlay), L7 (no ingredient aliases), R5 (`LICENSE`), R7/R8 (crash reporting, release signing), and the missing README screenshots.
