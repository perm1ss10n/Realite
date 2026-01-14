# GUI Migration Audit (Stage 0)

## Этап 1 — Общие правила миграции (единые для всех)

Цель: не получить 4 разных UI-фреймворка.

Требования:
- GUI создаются/живут в RealiteUI (как платформа).
- Плагины не держат свою GUI-логику, кроме “описания/провайдера”.
- Открытие GUI: через RealiteCore (единый API), без прямых вызовов между плагинами.
- Вся текстовка: Adventure components, без legacy.
- Никаких дублированных утилит (пагинация/кнопки/форматирование) — использовать то, что уже есть в RealiteUI. Если нет — добавить в RealiteUI как общий компонент, но только если реально нужно нескольким меню.

Что именно переносим:
- Старые Inventory/InventoryHolder/слушатели кликов → RealiteUI.
- В бизнес-плагинах остаются только:
  - “провайдер данных” (что показать);
  - “обработчики действий” (что делать по кнопке);
  - регистрация своих экранов в Core/UI.

Цель: инвентаризация существующих GUI/меню, точек входа и состояния переноса в RealiteUI.

## Status overview (DONE / TODO / PARTIAL)

| Plugin | Menu / GUI | Status | Notes |
| --- | --- | --- | --- |
| RealiteGuilds | Все меню гильдий (BaseMenu) | DONE | Используют `ru.realite.ui.menu.BaseMenu` + `MenuListener` из RealiteUI. |
| RealiteClasses | ClassSelectMenu, ClassSettingsMenu | TODO | Кастомные `InventoryHolder` + собственный listener. |
| RealiteCityInfrastructure | CityMainMenu, Admin GUI (MenuFactory), Player Plot GUI, ShopPoint toggle | TODO | Кастомные `InventoryHolder` + собственные listeners, нет RealiteUI роутинга. |
| RealiteQuests | GUI отсутствует | DONE | В плагине нет GUI-меню; только обработка инвентарных событий для целей. |

---

## RealiteGuilds

### GUI-related classes/packages
- `ru.realite.guilds.menu.*` (GuildCommandsMenu/ Basics/ Members/ Territory/ Claim/ Economy/ Join).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L1-L23】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsBasicsMenu.java†L1-L30】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMembersMenu.java†L1-L25】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsTerritoryMenu.java†L1-L27】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsClaimMenu.java†L1-L27】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsEconomyMenu.java†L1-L27】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsJoinMenu.java†L1-L35】
- Базовый класс `GuildMenu` расширяет `ru.realite.ui.menu.BaseMenu` (RealiteUI).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenu.java†L1-L30】【F:plugins/RealiteUI/src/main/java/ru/realite/ui/menu/BaseMenu.java†L1-L69】
- Реакция на клики идёт через `ru.realite.ui.menu.MenuListener` (RealiteUI).【F:plugins/RealiteUI/src/main/java/ru/realite/ui/menu/MenuListener.java†L1-L37】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/RealiteGuildsPlugin.java†L96-L118】

### Entry points / команды
- `/g` (alias `/guild`): при отсутствии аргументов и по `/g help` открывается корневое меню (`openRoot`).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/command/GuildCommand.java†L33-L74】【F:plugins/RealiteGuilds/src/main/resources/plugin.yml†L10-L16】
- Внутренние переходы между меню реализованы в `GuildMenuManager` (`openRoot/openBasics/...`).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L13-L66】

### Меню
- **guild.root / gui.title.root**
  - **Открывается:** `/g` без аргументов или `/g help`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/command/GuildCommand.java†L33-L74】
  - **Данные:** статические кнопки "Basics/Members/Territory/Economy/Close" (messages).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L11-L23】
  - **Клики:** открывают соответствующие подменю или закрывают GUI.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L15-L22】
  - **Сервисы:** `GuildMenuManager` (переходы).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L15-L22】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L19-L46】

- **guild.basics / gui.title.basics**
  - **Открывается:** из root (кнопка Basics).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L15-L19】
  - **Данные:** Create/Info/Join/Leave/Disband + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsBasicsMenu.java†L15-L29】
  - **Клики:** запускают команды (`g info/leave/disband`) или чатовый ввод (`create`), Join открывает список приглашений/выполняет `/g join`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsBasicsMenu.java†L17-L27】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L47-L70】
  - **Сервисы:** `GuildChatInputService`, `GuildService` (через `GuildMenuManager`).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L13-L82】

- **guild.members / gui.title.members**
  - **Открывается:** из root (Members).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L16-L19】
  - **Данные:** Invite/Ranks/Setrank + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMembersMenu.java†L15-L24】
  - **Клики:** Invite/Setrank → чатовый ввод, Ranks → `/g ranks`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMembersMenu.java†L17-L24】
  - **Сервисы:** `GuildChatInputService`, выполнение команд через `GuildMenuManager`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L47-L82】

- **guild.territory / gui.title.territory**
  - **Открывается:** из root (Territory).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L17-L19】
  - **Данные:** Claim/Home/SetHome/TP + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsTerritoryMenu.java†L15-L26】
  - **Клики:** Claim → меню claim, Home/SetHome → команды, TP → чатовый ввод (`g tp`).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsTerritoryMenu.java†L17-L26】
  - **Сервисы:** `GuildChatInputService`, `GuildMenuManager`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L13-L82】

- **guild.claim / gui.title.claim**
  - **Открывается:** из territory (Claim).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsTerritoryMenu.java†L17-L18】
  - **Данные:** Pos1/Pos2/Apply/Clear + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsClaimMenu.java†L15-L26】
  - **Клики:** выполняют `/g claim ...` команды через `GuildMenuManager`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsClaimMenu.java†L17-L26】
  - **Сервисы:** `GuildMenuManager` (команды).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L73-L82】

- **guild.economy / gui.title.economy**
  - **Открывается:** из root (Economy).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsMenu.java†L17-L20】
  - **Данные:** Salary/Upgrades/Upgrade Buy/Toggle + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsEconomyMenu.java†L15-L27】
  - **Клики:** `/g salary`, `/g upgrades`, `upgrade buy` через чатовый ввод, `/g toggle`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsEconomyMenu.java†L17-L26】
  - **Сервисы:** `GuildChatInputService`, `GuildMenuManager`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L73-L82】

- **guild.join.list / gui.title.basics**
  - **Открывается:** из Basics → Join (когда >1 приглашение).【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L47-L70】
  - **Данные:** список приглашений (`invites`) до 7 слотов + Back/Close.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsJoinMenu.java†L13-L35】
  - **Клики:** выполняют `/g join <tag>`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildCommandsJoinMenu.java†L20-L35】
  - **Сервисы:** `GuildService.getActiveInvites` через `GuildMenuManager`.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenuManager.java†L47-L70】

### RealiteUI routing
- Используется `BaseMenu` и глобальный `MenuListener` из RealiteUI → **уже перенесено**.【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/menu/GuildMenu.java†L1-L30】【F:plugins/RealiteUI/src/main/java/ru/realite/ui/menu/MenuListener.java†L1-L37】【F:plugins/RealiteGuilds/src/main/java/ru/realite/guilds/RealiteGuildsPlugin.java†L96-L118】

---

## RealiteClasses

### GUI-related classes/packages
- `ru.realite.classes.gui.ClassSelectMenu` (класс выбора).【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSelectMenu.java†L1-L152】
- `ru.realite.classes.gui.ClassSettingsMenu` (HUD/интерфейс).【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSettingsMenu.java†L1-L69】
- `ru.realite.classes.listener.MenuListener` (клики).【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L1-L154】

### Entry points / команды
- `/class` (без аргументов), `/class choose`, `/class change` → открывают ClassSelectMenu.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L97-L106】【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L672-L692】
- `/class settings` → открывает ClassSettingsMenu.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L694-L697】

### Меню
- **classes.select / "Классы" (ClassSelectMenu)**
  - **Открывается:** `/class` (no args) / `/class choose` / `/class change`.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L97-L106】【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L672-L692】
  - **Данные:** список всех `ClassId` с иконками/лором из `ClassConfigRepository` и `ClassLoreRepository`, учитывая скрытые классы (`HiddenClassGate`).【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSelectMenu.java†L21-L135】
  - **Клики:** выбор класса/смена (валидация эволюции, проверка скрытого доступа), сохранение профиля, обновление HUD.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L84-L154】
  - **Сервисы:** `ClassService`, `EvolutionService`, `HiddenClassGate`, `ClassConfigRepository`, `ClassHudService`.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L19-L48】【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSelectMenu.java†L23-L44】

- **classes.settings / "Class Settings" (ClassSettingsMenu)**
  - **Открывается:** `/class settings`.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/command/ClassCommand.java†L694-L697】
  - **Данные:** 4 слота для выбора режима HUD (BossBar/ActionBar/Sidebar/Off).【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSettingsMenu.java†L34-L50】
  - **Клики:** меняют `HudMode` и сохраняют профиль, обновляют HUD; закрывают меню.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L35-L82】
  - **Сервисы:** `ClassService` + `ClassHudService`.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L35-L82】

### RealiteUI routing
- Не использует BaseMenu/RealiteUI menu routing → **кандидат на перенос (TODO)**.【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/gui/ClassSelectMenu.java†L1-L152】【F:plugins/RealiteClasses/src/main/java/ru/realite/classes/listener/MenuListener.java†L1-L154】

---

## RealiteCityInfrastructure

### GUI-related classes/packages
- Главный пользовательский GUI: `ru.realite.city.gui.CityMainMenu` + `CityMenuClickListener`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMainMenu.java†L1-L259】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMenuClickListener.java†L1-L29】
- Админ/плейер GUI (plot/selection/etc): `MenuFactory`, `MenuHolder`, `GuiService`, `MenuListener`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L1-L246】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuHolder.java†L1-L19】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L1-L240】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L1-L73】
- Магазинная точка (toggle GUI): `ShopPointListener`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L1-L165】

### Entry points / команды
- `/city` или `/city gui` → открывает `CityMainMenu`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/command/CityCommand.java†L98-L127】
- `/plot` → открывает player plot GUI (`GuiService.openPlayerMain`).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/command/PlotCommand.java†L24-L44】
- Нажатие на ShopPoint блок → открывает toggle-меню (ShopPointListener).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L43-L100】

### Меню
- **city.main / "City" (CityMainMenu)**
  - **Открывается:** `/city`, `/city gui`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/command/CityCommand.java†L98-L127】
  - **Данные:** элементы "help / plot-nearby / plot-info / plot-buy / plot-members / plot-release / market-list / market-goto / shop-setup / area-list"; доступность зависит от контекста участка/прав и `ShopPointService` (PlotContext).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMainMenu.java†L66-L206】
  - **Клики:** выполняют команды `/city ...` через `player.performCommand` (например `city plot info`, `city market`).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMainMenu.java†L146-L206】
  - **Сервисы:** `PlotRepository`, `PlotMemberRepository`, `ShopPointService` (контекст/доступность).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMainMenu.java†L40-L118】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMainMenu.java†L218-L259】

- **city.admin.main / MenuType.ADMIN_MAIN**
  - **Открывается:** явная команда отсутствует; `GuiService.openMain` вызывается только из menu-навигации (back).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L55-L70】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L40-L47】
  - **Данные:** кнопки Selection/Plots/Regions/Help (статические).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L58-L68】
  - **Клики:** переходы в Selection/Plots/Back через `GuiService` (menu actions).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L40-L55】
  - **Сервисы:** `GuiService` + `MenuFactory`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L38-L91】

- **city.admin.selection / MenuType.ADMIN_SELECTION**
  - **Открывается:** из admin.main (Selection).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L40-L50】
  - **Данные:** кнопки set pos1/pos2/clear + статус выделения (coords).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L72-L89】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L256-L286】
  - **Клики:** `CityAdminService` через `GuiService` (set/clear).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L72-L136】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L44-L52】
  - **Сервисы:** `CityAdminService`, `CityAreaSelectionService`, `GuiSessionStore`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L23-L52】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L27-L55】

- **city.admin.plots / MenuType.ADMIN_PLOTS**
  - **Открывается:** из admin.main (Plots).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L44-L46】
  - **Данные:** список всех Plot (номер/id/type/owner/world/price) + пагинация.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L86-L118】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L288-L317】
  - **Клики:** prev/next/перейти к действиям plot (`open_plot_actions`).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L50-L70】
  - **Сервисы:** `PlotRepository`, `GuiService`, `GuiSessionStore`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L36-L114】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L82-L176】

- **city.admin.plot.actions / MenuType.ADMIN_PLOT_ACTIONS**
  - **Открывается:** из списка Plot (click on plot item).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L66-L70】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L120-L145】
  - **Данные:** информация о plot + действия (delete/teleport/set owner/show border/back).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L120-L145】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L319-L360】
  - **Клики:** удаление (с подтверждением), teleport, show border, set owner (player/guild), back.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L52-L70】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L148-L238】
  - **Сервисы:** `CityAdminService`, `ChatInputService`, `PlotBorderVisualizationService`, `PlotRepository`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L23-L238】

- **city.player.main / MenuType.PLAYER_MAIN**
  - **Открывается:** `/plot` (GuiService.openPlayerMain).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/command/PlotCommand.java†L24-L44】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L246-L264】
  - **Данные:** Access/Info/Show Border/Teleport (если есть право).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L172-L188】
  - **Клики:** переход в доступ, вывод инфо, показать границы, телепорт (permission).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L56-L63】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L372-L436】
  - **Сервисы:** `PlotRepository`, `PlotMemberRepository`, `PlotBorderVisualizationService`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L246-L436】

- **city.player.access / MenuType.PLAYER_ACCESS**
  - **Открывается:** из player.main (Access).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L56-L66】
  - **Данные:** список trusted-участников (UUID → name), кнопки add/remove/remove all + пагинация.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L190-L234】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuFactory.java†L210-L234】
  - **Клики:** add → чатовый ввод, remove/remove all → репозиторий, prev/next. 【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuListener.java†L60-L70】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L266-L368】
  - **Сервисы:** `PlotMemberRepository`, `ChatInputService`, `CityConfig`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/GuiService.java†L266-L368】

- **city.shop.point.menu (toggle)**
  - **Открывается:** взаимодействие с ShopPoint блоком (PlayerInteractEvent).【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L43-L100】
  - **Данные:** один слот (enabled/disabled) с текстом из messages.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L130-L157】
  - **Клики:** переключение enabled состояния ShopPoint. 【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L101-L125】
  - **Сервисы:** `ShopPointService`, `ShopRentService`, `PlotRepository`, `PlotMemberRepository`.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L23-L165】

### RealiteUI routing
- Меню реализованы через `InventoryHolder` + отдельные listeners, без BaseMenu → **кандидат на перенос (TODO)**.【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/MenuHolder.java†L1-L19】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/gui/CityMenuClickListener.java†L1-L29】【F:plugins/RealiteCityInfrastructure/src/main/java/ru/realite/city/listener/ShopPointListener.java†L1-L165】

---

## RealiteQuests

### GUI-related classes/packages
- GUI-меню не обнаружены. Плагин использует события инвентаря только для целей квестов (например `InventoryClickEvent`), но не открывает собственные GUI. 【F:plugins/RealiteQuests/src/main/java/ru/realite/quests/service/QuestObjectiveListener.java†L1-L95】

### Entry points / команды
- `/quest` — команды квестов, без GUI. 【F:plugins/RealiteQuests/src/main/resources/plugin.yml†L10-L14】

### RealiteUI routing
- Не применяется (GUI отсутствует).【F:plugins/RealiteQuests/src/main/java/ru/realite/quests/service/QuestObjectiveListener.java†L1-L95】

---

## Этап 7 — Приёмка (чеклист “готово”)

По каждому плагину:

- нет прямого Inventory GUI в плагине (или помечено deprecated и не используется);
- все точки входа открывают меню через Core → UI;
- Adventure везде;
- права (permissions) проверяются в бизнес-сервисах (UI только скрывает/показывает кнопки);
- никаких дублированных утилит;
- скрытые классы снова корректно отображаются;
- сборка проходит, нет циклических зависимостей модулей.
