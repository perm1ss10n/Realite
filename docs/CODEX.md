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

### Adventure compliance
- Запрещено: `ChatColor`, legacy цвет-коды, `player.sendMessage(String)` с секциями.
- Разрешено: `Component`, `MiniMessage`, централизованные message keys.
