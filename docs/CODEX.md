# Codex

## Pre-flight checklist (выполнять для каждой задачи)

### Перед созданием нового bridge/сервиса
- Поиск по репозиторию: `*Bridge`, `Provider`, `Service`, `Registry` (название/назначение).
- Если уже есть аналог — расширяем существующий контракт, не плодим новый.

### Перед добавлением новой команды
- Поиск по `plugin.yml` и Command классам.
- Убедиться, что нет дублей (`/models`, `/realitemodels`, `/model` и т.п.).

### Перед добавлением новой сериализации/хранилища
- Проверить, нет ли уже общего репозитория/хранилища в Core (или стандартного подхода).
- Модели-ассеты — только через RealiteItems registry (как договорились).

### Перед добавлением UI
- Проверить RealiteUI: есть ли уже паттерн “Manager screen / Details screen / Confirm dialog”.
- RealiteModels UI не рисует вообще.

### Resource pack fallback (обязательное правило)
- Все кастомные предметы и модели обязаны иметь ванильный fallback.
- Resource pack улучшает визуал, но не обязателен для геймплея.
- `ModelsBridge.apply(...)` возвращает `APPLIED`, `FALLBACK` или `FAILED`; `FALLBACK` — нормальное состояние.
- В примерах конфигов фиксируем, что `customModelData` и model-ресурсы опциональны, без RP всё выглядит нормально.
- Smoke-test: предмет с `customModelData` и предмет без него должны корректно идентифицироваться
  при принятом и отклонённом RP (без визуальных ошибок).

### Adventure compliance
- Запрещено: `ChatColor`, legacy цвет-коды, `player.sendMessage(String)` с секциями.
- Разрешено: `Component`, `MiniMessage`, централизованные message keys.

## Шаблон задачи: добавление нового босса Realite

### Заполнение шаблона (для каждой задачи босса)
- **bossId:**
- **tier:**
- **Базовая сущность:** (LivingEntity type)
- **modelId:** (RealiteModels) + фолбэк без RP
- **Экипировка:** (RealiteItems ids) + фолбэк materials
- **Фазы:** (BossPhase) и условия входа/выхода
- **Способности:** (BossAbility ids + параметры)
- **Дроп:** (гарантированный + таблица с весами)
- **Правила видимости BossBar:** (радиус/участники)

### Обязательные требования
- Спавн босса только через `BossManager.spawn(...)`.
- Босс — только реализация `AbstractRealiteBoss` (или интерфейса `RealiteBoss`).
- Все атаки/механики оформляются как `BossAbility`.
- Запрещено создавать/управлять vanilla BossBar напрямую.
- BossBar — только через `BossUIController` (RealiteUI), без дублей/конфликтов.
- Босс должен быть играбелен без ресурспака (fallback).

### Чеклист: архитектуру не обходить
- [ ] Спавн — только `BossManager.spawn(...)`.
- [ ] Босс реализует `AbstractRealiteBoss` или `RealiteBoss`.
- [ ] Все механики вынесены в `BossAbility`.
- [ ] Нет прямого использования vanilla BossBar.
- [ ] BossBar управляется только через `BossUIController`.
- [ ] Есть fallback без RP (модель/экипировка/визуалы).
