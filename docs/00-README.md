# Hakuna Kuinama: from prototype to real product

Documentation set produced on 2026-09-28 from an audit of `KahlubDev/Hakuna_Kuinama` at commit `87c4461`, using five device screenshots that are **not committed** (see `screenshots/README.md`).

**Re-checked against:** `dda4f4e` (2026-09-29). Findings carry an explicit `Status` line — `Fixed`, `Deferred`, `Open` or `Still true`. Nine commits landed between `87c4461` and `dda4f4e`; without this marker the present-tense prose below reads as current when much of it is not. When you check this doc set again, move the re-check line and re-date every finding that has no `Status`.

| File | What it is | Read it if you want to |
|---|---|---|
| `01-audit.md` | Full audit: verified bugs, data gaps, UX findings, architecture, release readiness, privacy | Know what is wrong or missing, and what has since been fixed |
| `02-council-verdict.md` | LLM Council pressure-test of the plan (five advisors, peer review, chairman) | Understand why the order of work is what it is |
| `03-requirements.md` | Product requirements: personas, 66 functional requirements with acceptance criteria, 12 non-functional requirements, metrics, risks | Hand the team a spec |
| `04-architecture-and-data.md` | Target architecture, Room v2 schema, content pipeline, price engine, nutrition, recommender, sharing without accounts, community, AI helper, scanning | Start building |
| `05-roadmap.md` | Phases P0 to P5 with exit gates, cut list, first 25 tickets | Plan the next months |
| `06-feature-catalogue.md` | 52 scored features, the recommended "make it real" cut, rejected ideas | Argue about scope with numbers |

## Five-line summary

1. The code is well built. The product is a demo: 5 recipes, 27 ingredients, one static price each, no nutrition, no settings.
2. The four small trust bugs are part-done. **L1** (hidden salt in match counts) and **L2** ("cheapest first" label) are `Fixed`. **L4** (servings vs batches) and **L6** (hard-coded step quantities) are `Deferred`, cross-referenced in code as one feature that must ship as a pair.
3. Google Play requires targeting Android 16 (API 36) for new apps and updates, and the app still targets 34. This is now the top release blocker, and it is `Open`. Everything else in P0 — the CI gate, the match-count bug, the label, the repo hygiene — is done or deferred.
4. Build the **data spine** (content packs, nutrition, units, prices with source and age) before any flashy feature.
5. Share by link, QR and WhatsApp first (no backend). Defer the community feed and the AI helper until the spine works and moderation exists.

## Where these live

This set lives in `docs/` and is linked from the top-level `README.md`.

## Sources referenced (verify licences and current coverage before relying on them)

- WFP Kenya Food Prices on HDX: https://data.humdata.org/dataset/wfp-food-prices-for-kenya
- KAMIS (Kenya Agricultural Market Information System): https://kamis.kilimo.go.ke/index.php/site/about
- Kenya Food Composition Tables 2018 (FAO and Government of Kenya): https://openknowledge.fao.org/handle/20.500.14283/i8897en
- Google Play target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- LLM Council skill (method used in `02`): https://github.com/aiwithremy/claude-skills-llm-council
