🌍 **Language:** English | [Русский](README.ru.md)

# 🧩 Realite — Minecraft RPG Platform

**Realite** is a modular RPG platform for Minecraft (**Paper**), built as a set of
interconnected plugins united by a shared core (**RealiteCore**).

The project focuses on:

- long-term player progression
- deep RPG systems (classes, evolutions, economy, cities, guilds, quests, items, magic)
- scalability without endless rewrites
- clean architecture and developer convenience
- monorepo approach (all modules are developed and versioned together)

---

## 🧠 Project Architecture

```
Realite/
├── RealiteCore/                        # Platform core (API / services / bridges)
└── plugins/
    ├── RealiteClasses/                 # Classes, evolutions, mastery (integration hooks)
    ├── RealiteChat/                    # Chat formatting + guild chat bridge
    ├── RealiteGuilds/                  # Guilds, ranks, progression, bonuses
    ├── RealiteCityInfrastructure/      # Cities, plots, shops, markets, region rules
    ├── RealiteQuests/                  # Quest engine + content packs
    ├── RealiteItems/                   # Custom items + Items API/Bridge
    ├── RealiteMagic/                   # Magic: casting, schools, reagents, HUD
    ├── RealiteUI/                      # UI and HUD system
    ├── RealiteFamiliars/               # Companions and combat pets
    ├── RealiteModels/                  # Custom entity models
```

### 🔑 Core Idea

- **RealiteCore** is the single source of truth (API, shared services, bridge contracts)
- modules **must not** depend on each other directly
- integrations are done via **bridges/interfaces** provided by Core (or module APIs)
- modules can be added, removed, and evolved independently

---

## ✨ Gameplay Features Available

> Realite is developed iteratively — the goal is a cohesive, extensible RPG ecosystem, not a set of disconnected plugins.

### 🧬 Classes and Progression (RealiteClasses)
- class selection via GUI
- XP, levels, and progression rules
- evolutions and branching paths
- mastery modifiers and balance hooks
- integrations with magic, items, quests, and guilds

### 🎒 Custom Items (RealiteItems)
Centralized item provider for the entire platform.

- YAML-based item registry (stable itemId)
- support for:
  - `material`
  - `customModelData` (optional; vanilla material is the required fallback)
  - localization keys for name and lore
  - glow
  - unstackable items
- strict server-side identification (not chat-name based)
- public integration via `ItemsBridge`
- resource pack is optional; items always render as vanilla material without it

### 🔮 Magic and Spells (RealiteMagic)
A magic system designed for extensibility and balance.

- spell registry (YAML definitions)
- cast checks:
  - permissions
  - cooldowns (global + per spell)
  - mana cost
  - staff/focus (optional)
  - reagents (optional)
  - economy cost (optional)
  - regional rules (allow/deny + modifiers)
- targeting and delivery:
  - self / entity / block / location
  - instant, AOE, chain (depending on spell)
- effect pipeline (executor registry)
  - pluggable effects by type (e.g. particles / knockback / potions / etc.)
- HUD feedback for success/failure + diagnostics (cast logging)
- safe smoke tests (optional, for debugging)
- spell selection GUI and focus requirement
- integrations: Classes, Items, Economy, Regions, Guilds

### 🗺️ Quests (RealiteQuests)
- YAML-driven quest engine with strict schema and validation
- quest types:
  - global
  - class-based
  - guild-based
  - city-based
  - hidden (evolutions / conditions)
- objectives:
  - item collection
  - entity kills
  - item usage
  - magic casting
  - mastery / class progression
  - region and city conditions
- start triggers:
  - manual
  - entering a region
  - acquiring a class or evolution
  - events from other modules
- rewards:
  - items
  - currency
  - experience
  - unlocking classes, evolutions, spells
- GUI progress tracking
- integrations:
  - RealiteClasses (classes, evolutions, hidden conditions)
  - RealiteMagic (casts, mastery, magic events)
  - RealiteItems (requirements and rewards)
  - RealiteGuilds (guild quests)
  - RealiteCityInfrastructure (city and region conditions)

### 🏰 Guilds (RealiteGuilds)
- guild creation and management
- ranks, permissions, and hierarchy
- guild chat (via RealiteChat bridge)
- guild progression
- bonuses and hooks for classes, magic, and economy
- integrations:
  - RealiteChat
  - RealiteClasses
  - RealiteQuests
  - RealiteCityInfrastructure

### 🏙️ Cities and Regional Infrastructure (RealiteCityInfrastructure)
- cities as gameplay entities
- city plots:
  - purchase and rent
  - owned by players or guilds
  - plot types (residential, commercial, special)
- shops and trading zones
- city economy:
  - taxes
  - recurring fees
  - plot upkeep
- regional rules:
  - allow/deny magic
  - damage and effect modifiers
  - PvE / PvP restrictions
- balance hooks for other modules

Integrations:
- RealiteGuilds — guild ownership of cities and plots
- RealiteQuests — city and region-based quests
- RealiteMagic — regional casting rules and magic modifiers
- RealiteItems — trading, shops, rewards
- RealiteCore — unified access to regional data

Purpose:
- make cities part of RPG progression
- connect territory, economy, and gameplay
- replace legacy solutions (WorldGuard-like) with RPG logic

### 💬 Chat and Communication (RealiteChat)
Chat is implemented as a standalone module and used across the platform.

- message formatting via **Kyori Adventure**
- unified message pipeline (no legacy ChatColor)
- guild chat via bridge (integration with RealiteGuilds)
- prefixes:
  - class
  - guild rank
  - custom tags
- hover tooltips and clickable elements
- message interception by other modules (quests, magic, events)

Integrations:
- **RealiteGuilds** — guild chat, spy mode, ranks
- **RealiteClasses** — class display
- **RealiteCore** — centralized message delivery

Purpose:
- remove duplicated chat logic from gameplay plugins
- ensure consistent message style across the RPG ecosystem

### 🖥️ UI and HUD System (RealiteUI)

RealiteUI is a centralized UI module of the Realite platform, responsible for rendering
HUD elements, ActionBar, BossBar and screen-based interfaces.

RealiteUI fully separates presentation from gameplay logic and acts as the single
UI rendering entry point for all Realite modules.

Core principles:
- unified UI pipeline across the entire platform
- strict separation between data and presentation
- no direct UI rendering logic inside gameplay plugins

Architecture:
- gameplay modules (Classes, Magic, Quests, Guilds, etc.) do not render UI directly
- modules provide structured UI data through RealiteCore
- RealiteUI is responsible for:
  - layout
  - formatting
  - pagination
  - updating and rendering

Features:
- HUD elements (stats, mana, experience, states)
- ActionBar and BossBar with unified formatting
- GUI screens with shared navigation and pagination
- HUD fallback mechanism (polling when events are unavailable)
- unified UI localization system
- extensible UI providers for modules

Integrations:
- RealiteClasses — class, level and progression
- RealiteMagic — mana, spells, casting, diagnostics
- RealiteQuests — quest hub and progress details
- RealiteGuilds — guild information
- RealiteCityInfrastructure — city and plot menus
- RealiteCore — UI provider registration and data delivery

Purpose:
- ensure a consistent visual style across the entire RPG ecosystem
- simplify UI iteration without touching gameplay logic
- reduce duplicated HUD logic between modules
- prepare the platform for further UI scaling

### 🐾 Familiars (RealiteFamiliars)

- real entity-based companions
- taming via consumable item (kill-confirm)
- familiar limits based on class and evolution tier
- multiple familiars per player
- safe summon / dismiss / release lifecycle
- familiar progression (levels, roles)
- full HUD and GUI management via RealiteUI
- PvE-oriented logic (PvP later)
- integrations:
  - RealiteItems (taming, equipment)
  - RealiteMagic (auras, synergies)
  - RealiteClasses (limits, passives)
  - RealiteQuests (quests for unlocking and progression)
  - Cities / Guilds (limits)

### 🎭 Custom Entity Models (RealiteModels)

- standalone infrastructure plugin
- registers `ModelsBridge` in RealiteCore
- applies custom visual models to entities
- uses assets from RealiteItems
- resource pack is optional; models fall back to vanilla entities when missing
- `ModelsBridge.apply` returns `APPLIED`, `FALLBACK`, or `FAILED`

---

## 👥 Project Team

- **perm1ss10n** — architect and lead developer  
  Platform architecture, Core API, magic, items, module integrations, CI, technical direction.

- **satanidea** — testing and QA  
  Gameplay testing, mechanic validation, bug hunting, regression testing after major changes.

- **mbutovsky** — narrative and lore  
  Storylines, world lore, quest chains, class and evolution descriptions, RPG context.

---

## 🛠️ Build and Development

### Requirements

- Java **21**
- Gradle Wrapper (`./gradlew`)

### Build

```bash
./gradlew clean build
```
> Verified on Java 21 and Paper 1.21.x

### Local Run (Paper)

- build required modules
- copy `.jar` files into Paper `plugins/`
- start the server, then configure modules in `plugins/<ModuleName>/`

---

## 🧭 Repository Rules

- respect module boundaries (no hard dependencies between gameplay plugins)
- use **Kyori Adventure** for text/messages (no legacy API)
- configs are YAML; messages use localization keys instead of hardcoded text
- integrations must go through bridges (ItemsBridge, EconomyBridge, ClassesBridge, etc.)

---

## 📜 License

TBD.

---

## 📌 Status

Active development.
