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
