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

### Tier-гайд

Тир указывается как `tier: T1|T2|T3|T4`. Таблица баланса находится в `config.yml`:

* T1: mana 6, cooldown 2.0s, damage 4.0
* T2: mana 10, cooldown 4.0s, damage 7.0
* T3: mana 16, cooldown 7.0s, damage 11.0
* T4: mana 24, cooldown 12.0s, damage 16.0
