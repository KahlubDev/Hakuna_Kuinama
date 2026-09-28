# 06. Feature Catalogue

Every idea raised so far plus new ones, scored so the team can argue with numbers instead of taste.

**Scoring:** Impact 1 to 5 (user value and product differentiation). Effort 1 to 5 (1 = days, 5 = months). Risk L/M/H (legal, abuse, cost, or accuracy). **Priority = Impact / Effort**, adjusted by risk. Phase refers to `05-roadmap.md`.

## A. Foundation features (enable everything else)

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 1 | Content packs and pipeline | Update catalogue and prices without an app release | 5 | 3 | L | P1 |
| 2 | Real Kenyan catalogue | 150 ingredients, 60 recipes, regional coverage, bilingual | 5 | 4 | L | P1 |
| 3 | Nutrition per ingredient | KFCT-based nutrients feed balance scoring | 5 | 3 | M | P1 |
| 4 | Units and purchase packs | Cooking units vs pack sizes, tin, KES-heap | 5 | 3 | L | P1 |
| 5 | Price observations with source badges | Every price shows source, age, confidence | 5 | 3 | M | P1 |
| 6 | County and market selection | Location-aware prices without tracking | 5 | 2 | L | P1 |
| 7 | User-set prices and pack prices | Override anything, reversible | 4 | 2 | L | P1 |
| 8 | Onboarding and profile | Household, budget, diet, allergens, equipment, language | 4 | 2 | L | P1 |

## B. Core loop features

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 9 | Search-first pantry | Quantities, expiry, aliases, recents | 5 | 3 | L | P2 |
| 10 | Substitution engine | "No tomatoes? Use paste", with price and nutrition delta | 4 | 3 | L | P2 |
| 11 | Recommender with explanations | Pantry, price, gaps, novelty, preference | 5 | 4 | L | P2 |
| 12 | Anti-boredom system | Cooldowns, main-starch variety, surprise slot, twists | 5 | 3 | L | P2 |
| 13 | Weekly planner and auto-plan | Fit to budget, diet, allergens, equipment | 5 | 4 | M | P2 |
| 14 | Balance score and gap filler | Food-group diversity, energy, protein, iron and vitamin A flags | 4 | 3 | M | P2 |
| 15 | Smart shopping list | Pack rounding, pantry subtraction, actual price paid | 5 | 3 | L | P2 |
| 16 | Plate cost vs cash needed | Two honest numbers, plus leftover value | 5 | 2 | L | P1/P2 |
| 17 | Cook mode | Big text, keep-awake, timers, step-by-step | 4 | 2 | L | P2 |
| 18 | History and preferences | Cooked, liked, skipped, blocked, on device | 4 | 2 | L | P2 |

## C. Kenya-specific differentiators (new ideas)

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 19 | **Buy-by-shillings** | Heaps sold at KES 10 or 20 convert to quantity and back. Matches how mama mboga actually sells | 5 | 3 | L | P2 |
| 20 | **Month-end survival mode** | Enter remaining KES and days to next allowance or loan disbursement. Cheapest adequate plan, with an honest warning if it cannot be done | 5 | 3 | M | P2 |
| 21 | **Cheapest protein/iron this week** | Price-per-nutrient ranking for the user's market (ndengu, eggs, omena, beans) | 5 | 3 | M | P3 |
| 22 | **Fuel and time cost** | Gas, charcoal or electricity per meal, energy-saving tips (soak beans, lid on) | 4 | 3 | L | P3 |
| 23 | **Equipment and no-fridge mode** | Jiko, one burner, no oven, no fridge filters | 4 | 2 | L | P2 |
| 24 | **Regional cuisine browsing** | Coast, Central, Western, Rift, Nyanza, North, Somali. Cultural pride plus variety | 4 | 3 | L | P1 |
| 25 | **Savings ledger** | "You saved KES 2,340 this month vs eating out", using a user-set baseline plate price | 4 | 2 | L | P3 |
| 26 | **Seasonal and price-trend hints** | "Sukuma is cheap now, tomatoes are up. Swap?" | 4 | 3 | M | P3 |
| 27 | Kiswahili and Sheng voice | TTS read-aloud, voice pantry entry ("nina sukuma, vitunguu, unga") | 4 | 4 | M | P4 |
| 28 | School lunch and family plan | Child portions, lunchbox ideas, per-plate family cost | 4 | 3 | M | P3 |

## D. Sharing and community

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 29 | Link, QR and WhatsApp card sharing | Menus, plans, recipes, lists. No accounts, no backend | 5 | 2 | L | P3 |
| 30 | Anonymous identity | Device key, auto nickname, signed posts | 3 | 3 | M | P3 |
| 31 | Food-only community feed | Menus, "I cooked this", price reports, questions, county boards | 4 | 4 | **H** | P3 |
| 32 | Crowd price reports | Feeds the price layer after outlier checks | 5 | 3 | M | P3 |
| 33 | Confirm-price micro-task | One tap "still KES 20?" keeps prices fresh | 4 | 1 | L | P3 |
| 34 | Crew mode (housemates) | Shared plan, pantry, cost split, who-cooks rotation, via a code | 4 | 4 | M | P5 |
| 35 | Challenges | "KES 100 lunch week", "one new food a week" | 3 | 2 | L | P3 |
| 36 | Creator recipes | Users publish recipes with auto-costing and a verified badge | 3 | 4 | H | P5 |

## E. Assistance and scanning

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 37 | AI: cook with what I have | Tool-grounded options with missing items and cost | 4 | 4 | M | P4 |
| 38 | AI: rescue and technique | "Too salty", "lumpy ugali", explains cooking terms | 4 | 3 | M | P4 |
| 39 | AI: leftover remix | Turns yesterday's food into a new meal | 3 | 2 | L | P4 |
| 40 | Barcode scan | Adds packaged items and prices | 3 | 3 | M | P4 |
| 41 | Receipt OCR | Pantry and prices from a photo of a receipt | 3 | 4 | M | P4 |
| 42 | Pantry photo | Vision model proposes items, user confirms | 3 | 3 | M | P5 |
| 43 | Recipe import | From a photo, screenshot or pasted text into private recipes | 3 | 3 | M | P4 |
| 44 | Produce recognition | On-device model for local vegetables. Needs a dataset | 2 | 5 | M | later |

## F. Growth and sustainability

| # | Feature | What it does | Imp | Eff | Risk | Phase |
|---|---|---|---|---|---|---|
| 45 | Cheapest-market comparison | Total of the list per market where data exists | 4 | 3 | M | P4 |
| 46 | Group buying for chamas | Bulk staples with mobile-money payments | 4 | 5 | **H** | P5 |
| 47 | USSD or WhatsApp bot | Price check and "what can I cook" for feature phones | 4 | 4 | M | P5 |
| 48 | Home-screen widget and notifications | "Cheapest lunch today", opt-in dinner nudge | 3 | 2 | L | P3 |
| 49 | B2B dashboards | Price and nutrition data for hostels, schools, NGOs, counties | 4 | 5 | M | P5 |
| 50 | Dietitian-reviewed packs | Child weaning, general healthy eating, later condition-specific after legal review | 4 | 4 | **H** | P5 |
| 51 | Food waste tracker | Expiry alerts, use-it-up suggestions | 3 | 2 | L | P3 |
| 52 | Backup and restore | JSON export and import | 3 | 1 | L | P3 |

## Recommended "make it real" cut (the smallest set that changes the product)

**Version 2.0 = features 1 to 8, 9, 11, 12, 13, 15, 16, 17, 18, 19, 20, 29.**
That gives you a real catalogue, honest local prices, a pantry that understands quantities, a recommender that does not repeat itself, a planner that respects budget, a shopping list that reports real cash, cook mode, a Kenyan differentiator (buy-by-shillings and month-end mode), and no-backend sharing. It has no AI, no scanning and no community, and it is already a different app.

## Ideas deliberately rejected

| Idea | Why |
|---|---|
| Reading M-Pesa SMS to infer spending | Privacy invasive and against Play restrictions on SMS access |
| Direct messages between anonymous users | Abuse surface too large for a small team |
| Medical diet advice | Liability. Only dietitian-reviewed general packs, later |
| Scraping supermarket sites for prices | Terms and legal risk. Prefer agreements or user-reported prices |
| Precise GPS stored server-side | Unnecessary. County and market are enough |
