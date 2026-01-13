# День 1 — аудит существующего GUI и спецификации данных

> Цель: зафиксировать текущее состояние и не дублировать уже реализованное.

## 1.1 Найденные GUI/входы (inventory-меню и клики)

### Спелл-меню выбора (inventory GUI)
- **`SpellSelectMenu`** — inventory-меню выбора спелла. Реализует `InventoryHolder`, строит инвентарь, рисует элементы спеллов, подмешивает lore/иконки и читает persistent data (`spell_id`, `menu_action`).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/gui/SpellSelectMenu.java†L1-L225】
- **`MagicMenuListener`** — слушатель кликов `InventoryClickEvent`, отсекает не-меню клики, закрывает меню или выбирает спелл по `spell_id`.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/listener/MagicMenuListener.java†L1-L87】
- **Команда входа**: `/magic` или `/magic menu` открывает меню выбора спелла (при наличии `realite.magic.menu`).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/command/MagicCommand.java†L70-L123】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/command/MagicCommand.java†L214-L232】
- **Конфиг меню**: размеры, слоты, формат чисел, fillers, кнопка закрытия — через `menu.spellSelect.*` в `config.yml`.【F:plugins/RealiteMagic/src/main/resources/config.yml†L260-L308】

### Прочие входы, влияющие на spellbook
- **Выучивание спеллов**: `SpellUnlockListener` реагирует на right-click со свитком/гримуаром, валидирует требования и вызывает `PlayerSpellService.unlock(...)`.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/listener/SpellUnlockListener.java†L22-L128】
- **Команды для админ-управления**: `/magic spell <give|remove|list|select|clear>` и `/magic spells <reload|validate>` — действуют на learned/selection или reload/validate YAML.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/command/MagicCommand.java†L103-L123】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/command/MagicCommand.java†L322-L354】

### Интеграция с RealiteUI/Core
- В плагине **нет регистрации экранов/роутинга** для spellbook GUI через Core UI. Единственная UI-интеграция — это **`MagicManaUiProvider`**, который регистрируется в `UiRegistry` и отдаёт снапшоты маны (не экраны spellbook).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/ui/MagicManaUiProvider.java†L1-L44】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/RealiteMagicPlugin.java†L323-L338】

## 1.2 Модель данных и состояние игрока по спеллам

### Состояние игрока (learned/selected/slots/mastery)
- **`PlayerSpellData`** хранит:
  - `learned` (множество выученных спеллов),
  - `selected` (текущий выбранный),
  - `slots` (9 слотов под спеллы),
  - `activeSlot` (какой слот активен),
  - `mastery` (прогресс мастерства по спеллам).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L13-L125】
- **Порядок работы со слотами**: `select(...)` в `PlayerSpellServiceImpl` записывает спелл в активный слот и обновляет `selected`.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/service/PlayerSpellServiceImpl.java†L94-L117】
- **Активный слот**: хранится в `PlayerSpellData`, значение 1–9.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L18-L109】

### Где хранится
- **YAML storage**: `playerdata/spells/<uuid>.yml`, поля `learned`, `selected`, `slots`, `activeSlot`, `mastery`, `version`.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/storage/YamlPlayerSpellStorage.java†L17-L147】

### Доступность/условия
- **Требования спелла** описываются через `SpellRequirements` (classId, evolutionId, requiredItemId, consumeOnCast).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRequirements.java†L1-L15】
- **Проверка требований** выполняется `DefaultSpellRequirementChecker`: проверяет класс/эволюцию через ClassesBridge и предмет через ItemsBridge; выдаёт причину отказа для UI/сообщений.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/requirements/DefaultSpellRequirementChecker.java†L15-L102】
- **SpellSelectMenu** применяет `requirementChecker.check(...)` и добавляет текст причины недоступности в lore, если требования не выполнены.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/gui/SpellSelectMenu.java†L184-L221】

## 1.3 Формат спелл-дефиниций

### Где живут YAML спеллы
- Спеллы находятся в `spells/*.yml`; каждый файл может содержать `spell:` или `spells:`. Примеры структуры и полей — в README плагина.【F:plugins/RealiteMagic/README.md†L40-L89】

### Ключевые поля определения
- В коде спелл представлен `SpellDefinition`, который включает: `id`, `type`, `nameKey`, `descKey`, `school`, `mana`, `cooldownTicks`, `range`, `damage`, `requirements`, `target`, `castDelivery`, `effects`, `castTrigger`, `castItemId`, `reagents`, `moneyCost`, `iconMaterial`, `iconCustomModelData`, `guiSlot` и др.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellDefinition.java†L1-L33】
- Разбор значимых GUI-полей (`icon.material`, `icon.customModelData`, `gui.slot`) происходит в `SpellRegistry` при парсинге YAML.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRegistry.java†L144-L189】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRegistry.java†L228-L260】

## 1.4 Предложение MVP-экранов и доступные поля

### MVP-экраны (2–3 штуки)
1. **Экран списка/выбора спелла** (по сути, уже реализован как `SpellSelectMenu`). Можно переиспользовать его логику выбора/блокировок и перенести в UI слой.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/gui/SpellSelectMenu.java†L31-L225】
2. **Экран деталей спелла** — описание, мана, кулдаун, range, damage, требования (class/evolution/item) и статус learned/selected.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellDefinition.java†L1-L33】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRequirements.java†L1-L15】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L13-L125】
3. **Экран слотов/лоадаута** — управление 9 слотами + активный слот (минимум: выбор активного слота и привязка/очистка).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L18-L109】

### Какие поля доступны прямо сейчас
- **Статусы игрока**: learned, selected, activeSlot, slots(1–9), mastery (level/xp/etc).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L13-L125】
- **Список спеллов**: `SpellRegistry.all()` отдаёт все `SpellDefinition`, доступные в YAML-реестре.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRegistry.java†L108-L136】
- **Поля спелла для отображения**: id, nameKey, descKey, mana, cooldownTicks, range, damage, school, requirements, iconMaterial/customModelData, guiSlot и т.д.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellDefinition.java†L1-L33】
- **Причина блокировки**: `DefaultSpellRequirementChecker` возвращает reasonKey/placeholder-ы, которые можно показать в UI для недоступных спеллов.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/requirements/DefaultSpellRequirementChecker.java†L34-L101】

---

## Итог: что уже есть и что делать дальше
- **Уже есть inventory GUI** для выбора спелла, его можно использовать как референс поведения и доступных полей.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/gui/SpellSelectMenu.java†L31-L225】
- **Нет UI-роутинга через RealiteCore** для spellbook: единственная зарегистрированная UI-панель — `MagicManaUiProvider` (только мана).【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/ui/MagicManaUiProvider.java†L1-L44】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/RealiteMagicPlugin.java†L323-L338】
- **Данные по игроку и спеллам уже достаточны для MVP**: learned/selected/slots + детальная карточка спелла из `SpellDefinition` + требования из `SpellRequirements`.【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/model/PlayerSpellData.java†L13-L125】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellDefinition.java†L1-L33】【F:plugins/RealiteMagic/src/main/java/ru/realite/magic/spell/SpellRequirements.java†L1-L15】
