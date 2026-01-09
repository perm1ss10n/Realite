🌍 **Language:** English | [Русский](README.ru.md)

# 🧩 Realite — Minecraft RPG Platform

**Realite** is a modular RPG platform for Minecraft (**Paper**), built as a set of
interconnected plugins unified by a shared core (**RealiteCore**).

The project focuses on:

- long-term player progression
- deep RPG systems (classes, evolutions, economy, cities, guilds, quests, items, magic)
- scalability without constant refactoring
- clean architecture and developer-friendly design
- monorepo workflow (all modules developed & versioned together)

---

## 🧠 Project Architecture

```
Realite/
├── RealiteCore/                        # Platform core (API / services / bridges)
└── plugins/
    ├── RealiteClasses/                 # RPG classes, evolutions, mastery (integration hooks)
    ├── RealiteChat/                    # Chat formatting + guild chat bridge
    ├── RealiteGuilds/                  # Guilds, ranks, progression, bonuses
    ├── RealiteCityInfrastructure/      # Cities, plots, shops, market, region rules
    ├── RealiteQuests/                  # Quest engine + content packs
    ├── RealiteItems/                   # Custom items system + Items API/Bridge
    └── RealiteMagic/                   # Spell system, casting engine, schools, reagents, HUD
```

### 🔑 Core Idea

- **RealiteCore** is the single source of truth (APIs, shared services, bridge contracts)
- modules should **not** depend on each other directly
- integrations happen via **bridges/interfaces** provided by the Core (or module APIs)
- modules can be added/removed and iterated independently

---

## ✨ Current Gameplay Systems

> Realite is still evolving — the goal is a coherent, extensible RPG ecosystem, not a one-off plugin dump.

### 🧬 Classes & Progression (RealiteClasses)
- class selection via GUI
- XP/levels and progression rules
- evolutions / branching paths
- mastery modifiers & balance hooks
- integrations for magic, items, quests, guilds

### 🎒 Custom Items (RealiteItems)
A centralized item provider for the whole platform.

- YAML-defined item registry (stable item IDs)
- supports:
  - `material`
  - `customModelData`
  - localized name & lore keys
  - glow
  - unstackable items
- strict identification (server-side, not “guessing by name”)
- public integration surface through an `ItemsBridge`

### 🔮 Magic & Spells (RealiteMagic)
Spell casting system designed for expansion and balance.

- spell registry (YAML-driven definitions)
- casting checks:
  - permissions
  - cooldowns (global + per-spell)
  - mana cost
  - staff/focus requirement (optional)
  - reagents requirement (optional)
  - economy cost (optional)
  - region policies (deny/allow + modifiers)
- spell targeting & delivery:
  - self / entity / block / location
  - instant, AOE, chain (depending on the spell)
- effect execution pipeline (executor registry)
  - supports pluggable effect types (e.g. particles/knockback/potions/etc.)
- HUD feedback for success/fail + diagnostics logging
- safe “smoke tests” (optional debug mode)

### 🏰 Cities, Guilds, Quests
- **Guilds**: ranks, progression and hooks for bonuses
- **City infrastructure**: plots/shops/market scaffolding + region rules
- **Quests**: quest engine + content packs and integration hooks

---

## 🧩 Localization

Realite supports multi-language messaging. Most modules ship with `messages_ru.yml` and `messages_en.yml`.

Repository-level README language switch:
- English: `README.md`
- Russian: `README.ru.md`

---

## 🛠️ Build & Development

### Requirements
- Java **21**
- Gradle Wrapper (`./gradlew`)

### Build
```bash
./gradlew clean build
```

### Run locally (Paper)
- build module(s)
- copy resulting `.jar` into your Paper `plugins/` directory
- start the server, then configure modules under `plugins/<ModuleName>/`

---

## 🧭 Repository Guidelines

- keep module boundaries clean (no direct hard dependency between gameplay modules)
- prefer **Kyori Adventure** for text/components (avoid legacy chat APIs)
- configs are YAML, with localization keys instead of hardcoded strings
- integrate through bridges (ItemsBridge, EconomyBridge, ClassesBridge, etc.)

---

## 📜 License

TBD (project is actively evolving).

---

## 📌 Status

This is an actively developed monorepo. Expect iteration, refactors, and expanding content packs.
