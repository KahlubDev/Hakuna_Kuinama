# Hakuna Kuinama: from prototype to real product

Documentation set produced on 2026-09-28 from an audit of `KahlubDev/Hakuna_Kuinama` (commit `87c4461`) and its five device screenshots.

| File | What it is | Read it if you want to |
|---|---|---|
| `01-audit.md` | Full audit: verified bugs, data gaps, UX findings, architecture, release readiness, privacy | Know exactly what is wrong or missing today |
| `02-council-verdict.md` | LLM Council pressure-test of the plan (five advisors, peer review, chairman) | Understand why the order of work is what it is |
| `03-requirements.md` | Product requirements: personas, 60+ functional requirements with acceptance criteria, non-functional requirements, metrics, risks | Hand the team a spec |
| `04-architecture-and-data.md` | Target architecture, Room v2 schema, content pipeline, price engine, nutrition, recommender, sharing without accounts, community, AI helper, scanning | Start building |
| `05-roadmap.md` | Phases P0 to P5 with exit gates, cut list, first 25 tickets | Plan the next months |
| `06-feature-catalogue.md` | 52 scored features, the recommended "make it real" cut, rejected ideas | Argue about scope with numbers |

## Five-line summary

1. The code is well built. The product is a demo: 5 recipes, 27 ingredients, one static price each, no nutrition, no settings.
2. Fix four small trust bugs first (hidden salt in match counts, "cheapest first" label, servings vs batches, hard-coded step quantities).
3. Google Play now requires targeting Android 16 (API 36) for new apps and updates, and the app targets 34. Upgrade in Phase 0.
4. Build the **data spine** (content packs, nutrition, units, prices with source and age) before any flashy feature.
5. Share by link, QR and WhatsApp first (no backend). Defer the community feed and the AI helper until the spine works and moderation exists.

## Where to put these in the repo

Copy this folder to `docs/` in the repository (for example `docs/product/`) and link it from the README.

## Sources referenced (verify licences and current coverage before relying on them)

- WFP Kenya Food Prices on HDX: https://data.humdata.org/dataset/wfp-food-prices-for-kenya
- KAMIS (Kenya Agricultural Market Information System): https://kamis.kilimo.go.ke/index.php/site/about
- Kenya Food Composition Tables 2018 (FAO and Government of Kenya): https://openknowledge.fao.org/handle/20.500.14283/i8897en
- Google Play target API level requirements: https://support.google.com/googleplay/android-developer/answer/11926878
- LLM Council skill (method used in `02`): https://github.com/aiwithremy/claude-skills-llm-council
