# 02. Council Verdict: how should Hakuna Kuinama become a real product?

> **How this was run.** This chat has no parallel sub-agent tool, so I ran the `llm-council` method as separate sequential passes: five advisors, then an anonymized peer review (responses shuffled to A to E), then a chairman synthesis. The passes share one model and one context, so independence is weaker than a true multi-agent run. Treat this as structured pressure-testing of the plan, not five separate opinions.

## Framed question

Hakuna Kuinama is an offline-first Android app (Kotlin, Compose, Room) with 5 recipes, 27 ingredients, static Nairobi prices and no accounts. The team wants to make it real: real Kenyan foods and balanced diets, prices by location plus user-set prices, a recommendation system so users do not get bored, an AI kitchen helper working from what the user already has, menu sharing and a community feed without forcing accounts, and food scanning. **What should the build order and key design decisions be, and what should be cut or deferred?** Stakes: a small team with limited time, users on low-end phones and expensive data, and a category (food, health, money) where wrong numbers destroy trust.

Context used: audit findings (`01-audit.md`), the README, and the code.

---

## The five advisors

**The Contrarian.** The app is a 5-recipe toy, and the data is the product, not the UI. Official price series are commodity-level and monthly, they cover a subset of markets, and "Sukuma at Wakulima" will not be in them. Crowdsourced prices go stale and get gamed. An anonymous community feed is a moderation and legal liability the team cannot staff. An LLM kitchen helper adds per-request cost for people paying for data bundles, and it will confidently invent nutrition and prices unless boxed in. "Balanced diet" is a health claim. Scanning Kenyan packaged goods will mostly miss because local barcode coverage is patchy. Half this list could sink a small team.

**The First Principles Thinker.** The job is: "help me eat properly tonight and this week with the money and food I have." Three variables drive everything: cash, what is on hand, and nutrition and variety. That is a **food, price and nutrition graph** plus a **planner**. Sharing, community, scanning and AI are distribution and convenience layers on top. If the graph is wrong, every layer amplifies the error. Build the spine, and make every number carry its source and date.

**The Expansionist.** The upside is much bigger than a recipe app. A live price-per-nutrient index for Kenyan markets ("cheapest protein in your county this week") is a unique dataset. It is valuable to households, hostels, schools, chamas, NGOs and county governments. Add roommate crews, buy-by-shillings (heaps sold at KES 10 or 20), month-end survival mode, fuel cost per meal, Swahili and Sheng voice, and WhatsApp as the growth channel. Early community feeds create the price-reporting loop that keeps the dataset fresh.

**The Outsider.** I do not know what "Menu builder" means. The home screen shows grey shapes, so I cannot tell what the food looks like. "KES 111": per plate, per batch, per week? "0.2 250 g piece of capsicum" makes no sense to me. I cannot tell the app how much money I have. Why does the app never ask me anything?

**The Executor.** Monday morning: fix the four small bugs, add CI, and build a green APK on a real phone. Then define one JSON content-pack schema and push the 27 existing ingredients through a real pipeline (KFCT nutrition plus WFP price series) end to end. Once one slice works, scale to 150 ingredients and 60 recipes. Sharing can ship as a link or QR with **no backend** in a week. Do not start the community until the price and catalogue loop works.

---

## Peer review (anonymized, summarized)

- **Strongest response:** the First Principles answer paired with the Executor's sequencing. Both name the data spine and both give a first step.
- **Biggest blind spot:** the Expansionist. It never prices the data licensing, moderation labor or AI cost that its upside depends on. The Contrarian correctly says "risky" but treats community as kill-or-keep rather than **defer and de-risk**.
- **What all five missed:**
  1. **Distribution.** How do the first 1,000 users find the app? WhatsApp share cards and campus channels, not the Play Store.
  2. **Data-cost sensitivity.** Content and price updates must be small deltas, compressed, and Wi-Fi or opt-in for images.
  3. **Trust UX for prices.** Every price needs a visible source, age and confidence, or one wrong number ends credibility.
  4. **Cash outlay vs plate cost** (bug L5 in the audit). This is a first-class product insight, not a detail.
  5. **Measurement.** No analytics means no learning. Use opt-in, minimal, privacy-first event counts.
  6. **Safety.** Food-safety rules, allergen handling and health-claim guardrails.
  7. **Play policy.** Targeting API 36 is required for new apps and updates from 2026-08-31 (extension to 2026-11-01).

---

## COUNCIL VERDICT

### Where the Council Agrees
- The **catalogue, units, nutrition and price model** is the foundation. UI and features sit on it.
- The app should stay **offline-first and account-free** for its core loop (pantry, plan, list, cook).
- Every price and nutrition figure must show **source, date and confidence**.
- Sharing should start with **link, QR and WhatsApp card**, which needs no backend.
- The AI helper must be **grounded**: it explains and chooses, while deterministic code computes prices and nutrition.

### Where the Council Clashes
- **Community feed now or later.** Expansionist: early, because price reports feed the data loop. Contrarian and Executor: defer, because moderation and legal load will swamp the team. *Why reasonable people differ:* network effects need early density, but abuse costs arrive on day one. **Resolution below.**
- **AI on-device or cloud.** On-device models only run on a subset of devices (verify current Android AICore and Gemini Nano support), so cloud is the only reach. Cloud costs money and data. **Resolution below.**
- **How many price sources.** Official datasets are trustworthy but coarse. Crowd data is fresh but noisy. User-set prices are exact but personal.

### Blind Spots the Council Caught
Distribution channel, data-bundle cost, price trust UX, cash outlay vs plate cost, measurement, food-safety and health-claim guardrails, Play API 36 deadline.

### The Recommendation
**Build the trustworthy data spine first, then personalization, then sharing, then assistance.** Concretely:
1. **Layered price resolution:** user override, then fresh local crowd reports, then official market data (WFP HDX Kenya food prices, KAMIS if access is agreed), then seed baseline. Always display source and age.
2. **Community: defer, then de-risk.** Ship link/QR/WhatsApp sharing first. When you build the community, launch it as a **small, food-only, moderated** feed with anonymous, device-keyed identities, no direct messages, report buttons and rate limits.
3. **AI helper: cloud, behind a thin proxy, tool-grounded, hard-capped per device per day**, with an offline rule-based fallback so the app never depends on it.
4. **Scanning: barcode and receipt OCR first** (on-device, free, and they produce price observations). Photo-of-fridge with a vision model later.
5. **Never present health advice.** "Balanced" means food-group diversity and macro targets from public guidance, with dietitian review before launch.

### The One Thing to Do First
**Define the content-pack schema (v2) and run the 27 existing ingredients through it end to end** (stable slug, cooking unit and purchase pack, nutrition per 100 g from the Kenya Food Composition Tables 2018, price observation with source, date, market, confidence). Merge the four audit bug fixes in the same week. When one slice works, everything else is scaling.
