# RealiteMagic API

## Получение MagicApi

```java
RegisteredServiceProvider<MagicApi> provider =
        Bukkit.getServicesManager().getRegistration(MagicApi.class);
if (provider == null || provider.getProvider() == null) {
    return;
}
MagicApi magicApi = provider.getProvider();
```

## Пример использования

```java
Player player = ...;
CastAttemptResult result = magicApi.casting().tryCastSelected(player);

boolean hasSpell = magicApi.playerSpells().hasSpell(player.getUniqueId(), "warlock_firebolt");
magicApi.casting().tryCast(player, "warlock_firebolt");
magicApi.spellRegistry().find("warlock_firebolt").ifPresent(view -> {
    String nameKey = view.nameKey();
});
```

## Content authoring

### Pipeline каста (step-by-step)

1. Выбор спелла (слот/selected).
2. Проверка прав и глобального кулдауна.
3. Проверка требований (класс, эволюция, предмет, focus).
4. Региональные и школьные ограничения.
5. Проверка реагентов и экономики.
6. Списание маны, кулдаунов и реагентов.
7. Резолв цели и формирование плана каста.
8. Выполнение эффектов и наград.
9. Публикация событий и логов.

### Структура YAML

Каждый файл может содержать один спелл через `spell:` или пачку через `spells:`:

```yml
spell:
  id: "firebolt"
  type: "RAY_DAMAGE"
  nameKey: "magic.spell.firebolt.name"
  descKey: "magic.spell.firebolt.desc"
  school: "FIRE"
  tier: "T1"
  mana: 6
  cooldownTicks: 40
  range: 26
  damage: 4
  cast:
    trigger: "RIGHT_CLICK"
    delivery: "PROJECTILE"
    projectile:
      speed: 1.6
      gravity: false
      maxDistance: 26
      hitRadius: 0.6
      onHit: "STOP"
  target:
    type: "ENTITY"
    maxDistance: 26
    lineOfSight: true
    allowPlayers: true
    allowMobs: true
  effects:
    - type: "damage"
      amount: 4
      cause: "MAGIC"
  requirements: {}
```

```yml
spells:
  - id: "spell_one"
    # ...
  - id: "spell_two"
    # ...
```

### Delivery types

* `PROJECTILE`: `cast.projectile` (speed, gravity, maxDistance, hitRadius, onHit)
* `BEAM`: `cast.beam` (maxDistance, step, hitRadius, particles)
* `AOE`: `cast.aoe` (radius, maxTargets)
* `CHAIN`: `cast.chain` (jumps, jumpRange)
* `INSTANT`: без дополнительных настроек

### Effect types

Доступные типы: `damage`, `potion`, `heal`, `cleanse`, `knockback`, `particles`, `sound`, `teleport`.

### Валидация

Проверить спеллы:

```
/rmagic spells validate
```

### Где что настраивается

* `spells/` — YAML спеллы (один или пачка).
* `config.yml` — параметры кастов, баланса, PVE, мастерства, release-секция.
  * `release.strictValidation` — строгая валидация при загрузке.
  * `release.logConfigWarnings` — логирование предупреждений по конфигам.
  * `magic.debug.smokeTests` — runtime smoke-проверки при старте.

### Дебаг и админ-инструменты

* `/rmagic spells validate` — ручная проверка YAML.
* `/rmagic reload spells` — перезагрузка спеллов.
* `/rmagic admin inspect|stats|log` — инспект и диагностика.

### Безопасное добавление спеллов

1. Добавь новый YAML в `spells/`.
2. Запусти `/rmagic spells validate` — исправь предупреждения.
3. Выполни `/rmagic reload spells` — убедись, что спелл в реестре.
4. При ошибках — спелл будет пропущен, сервер не упадет (release.strictValidation).

### Tier-гайд

Тир указывается как `tier: T1|T2|T3|T4`. Таблица баланса находится в `config.yml`:

* T1: mana 6, cooldown 2.0s, damage 4.0
* T2: mana 10, cooldown 4.0s, damage 7.0
* T3: mana 16, cooldown 7.0s, damage 11.0
* T4: mana 24, cooldown 12.0s, damage 16.0
