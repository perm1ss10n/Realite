🌍 **Language:** English \| [Русский](README.ru.md)

# 🧩 Realite --- Minecraft RPG Platform

**Realite** is a modular RPG platform for Minecraft (Paper), built as a
set of interconnected plugins unified by a shared core.

The project focuses on:

-   long-term player progression
-   deep RPG systems (classes, evolutions, economy, cities, guilds,
    quests, items, magic)
-   scalability without constant refactoring
-   clean architecture and developer-friendly design
-   unified CI and monorepo workflow

The repository uses a **monorepo approach**: all plugins are developed
and versioned together.

------------------------------------------------------------------------

## 🧠 Project Architecture

    Realite/
    ├── RealiteCore/        # Platform core (API / services / integrations)
    ├── plugins/            # Gameplay modules (plugins)
    │   ├── RealiteClasses/             # RPG classes and progression
    │   ├── RealiteChat/                # Chat formatting and tags
    │   ├── RealiteGuilds/              # Player guilds
    │   ├── RealiteCityInfrastructure/  # Cities, plots, shops, market
    │   ├── RealiteQuests/              # Quests and lore
    │   ├── RealiteItems/               # Custom items & item API
    │   └── RealiteMagic/               # Magic system and spells
    ├── build.gradle
    ├── settings.gradle
    └── README.md

### 🔑 Core Idea

-   **RealiteCore** is the single source of truth (API & services)
-   gameplay modules live in `plugins/*` and **do not depend on each
    other directly**
-   interaction happens strictly through Core APIs
-   each module can evolve independently

------------------------------------------------------------------------

## 🧱 Modules

### ⚙ RealiteCore

The heart of the platform.

Responsibilities:

-   shared services and APIs
-   common utilities and contracts
-   integrations (Vault, PlaceholderAPI, etc.)
-   module lifecycle management
-   unified localization system
-   base gameplay contracts (Items, Magic, Quests, Guilds)

------------------------------------------------------------------------

### 🧬 plugins/RealiteClasses

A full-featured RPG class system.

Features:

-   class selection via GUI
-   XP and leveling
-   evolutions and progression branches
-   class passives and effects
-   HUD / hotbar integration
-   GUI settings
-   economy integration
-   events and hooks for quests, magic, items, cities, and guilds

Configuration:

-   `classes.yml`
-   `xp.yml`

------------------------------------------------------------------------

### 🎒 plugins/RealiteItems

Custom item system and shared item API.

Features:

-   YAML-based custom item definitions
-   CustomModelData, glow, unstackable items
-   localized names and lore
-   integration with quests, classes, and magic
-   API for item validation and granting
-   unified item format across all modules

Configuration:

-   `items/*.yml`
-   `items_locale.yml`

------------------------------------------------------------------------

### ✨ plugins/RealiteMagic

Spellcasting and magic system.

Features:

-   spell system (spells)
-   cooldowns and costs
-   class and evolution requirements
-   usage conditions
-   effects (particles, damage, buffs, debuffs)
-   GUI-based spell selection for players
-   admin commands for testing and management
-   deep integration with RealiteItems and RealiteClasses

Configuration:

-   `spells/*.yml`
-   `magic.yml`

------------------------------------------------------------------------

### 💬 plugins/RealiteChat

Chat formatting and social tags.

Features:

-   global chat
-   guild chat
-   **guild spy** mode for administrators
-   hover tooltips for classes and guilds
-   full message localization

------------------------------------------------------------------------

### 🛡 plugins/RealiteGuilds

Player guild and social progression system.

Features:

-   guild creation and management
-   internal ranks and permissions
-   guild chat
-   guild progression and leveling
-   guild XP rewards
-   groundwork for treasury and quest integration

------------------------------------------------------------------------

### 📜 plugins/RealiteQuests

MVP quest and lore system.

Implemented:

-   YAML-based quest definitions
-   objectives: items, crafting, mobs, blocks, NPCs
-   quest lifecycle (start → progress → completion)
-   localization
-   hooks for classes, guilds, magic, and items

Planned:

-   dialogs and NPCs
-   story-driven quest chains
-   rewards and access conditions
-   guild and city quests

------------------------------------------------------------------------

### 🏙 plugins/RealiteCityInfrastructure

City and social infrastructure.

Features:

-   cities and regions
-   plots with types
-   shops and market
-   access control
-   plot transactions and teleportation

------------------------------------------------------------------------

## 🛠 Technology Stack

-   Java 21
-   Gradle (monorepo)
-   Paper API
-   Vault
-   YAML-based configuration
-   GitHub Actions (CI)

------------------------------------------------------------------------

## 📦 Build

``` bash
./gradlew build
```

Built `.jar` files:

    /plugins/**/build/libs/

------------------------------------------------------------------------

## 🚀 Quick Start

1.  Install **Java 21** and **Paper**.
2.  Clone the repository and build the project:

``` bash
./gradlew build
```

3.  Copy the required `.jar` files into your server's `plugins/`
    directory.

------------------------------------------------------------------------

## 🧩 Project Principles

-   modularity \> monolith
-   extensibility \> hacks
-   configuration \> hardcode
-   Core as the single source of truth
-   plugins do not depend on implementations of each other
-   localization by default

------------------------------------------------------------------------

## 📌 Status

The project is under active development.\
Architecture and APIs may change until Core stabilization.

------------------------------------------------------------------------

## 👤 Team

**perm1ss10n** --- author & lead developer\
Platform architecture, RealiteCore, gameplay systems.

**satanidea** --- testing & QA

**mbutovsky** --- lore & narrative design

------------------------------------------------------------------------

## 📄 License

TBD
