# UI audit (RealiteCore)

## Что уже есть в RealiteCore

- **EventBus для модулей**: `EventBus` (subscribe/publish) доступен из `CoreApi` и `ModuleContext`. Есть простая реализация `SimpleEventBus`. Это можно использовать как шину событий/паблиш для UI-провайдеров. 
- **Service Registry / DI**: `Services` (register/require/get/replace) доступен из `CoreApi` и `ModuleContext`. Это текущий общий реестр сервисов. 
- **Scheduler**: фасад `Scheduler` с `runSync/runLater/runRepeating`, доступен через `Services` и `ModuleContext`. Есть Bukkit-реализация. 
- **Adventure/Component**: в Core API уже встречаются `Component` в контрактах `GuildChatBridge` и `GuildTagProvider`.

## Чего нет (на уровне Core)

- **Общий Messages/i18n слой**: в `RealiteCore` нет единого сервиса локализации/сообщений; реализация вынесена в плагины (Magic/Classes/Quests/Items и т.д.) и в Core нет утилит для `MiniMessage`.
- **UI-специфичный registry/adapters**: в Core пока нет отдельного API для UI-провайдеров/слотов.

## Вывод: используем X, добавляем Y

- **Используем**: существующие `EventBus`, `Services` (реестр), `Scheduler`, а также `Component` как базовый формат текста.
- **Добавляем минимально**:
  - API для UI-шины (события/провайдеры/слоты), опираясь на `EventBus` и `Services`, **без нового реестра**.
  - При необходимости — тонкие Adventure-утилиты (например, MiniMessage) в Core, но только если это нужно для UI и не дублирует существующий функционал.

