package ru.realite.classes.service;

import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EffectService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final boolean clearManagedEffects;

    // Ищем новый метод, если он есть в твоём API: addPotionEffect(PotionEffect, Cause, boolean)
    private static final Method ADD_EFFECT_CAUSE_BOOLEAN = findAddPotionEffectCauseBoolean();
    private static final Object PLUGIN_CAUSE = findPluginCause();

    // какие эффекты мы вообще используем (чтобы уметь чистить только их)
    private final Set<PotionEffectType> managedTypes = new HashSet<>();

    public EffectService(ClassService classService,
                         ClassConfigRepository classConfig,
                         boolean clearManagedEffects) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.clearManagedEffects = clearManagedEffects;

        rebuildManagedTypes();
    }

    public void rebuildManagedTypes() {
        managedTypes.clear();
        for (ClassConfigRepository.ClassDef def : classConfig.all()) {
            for (var pe : parseEffects(def.effects)) {
                managedTypes.add(pe.getType());
            }
        }
    }

    private static Method findAddPotionEffectCauseBoolean() {
        try {
            Class<?> causeClass = Class.forName("org.bukkit.event.entity.EntityPotionEffectEvent$Cause");
            return org.bukkit.entity.LivingEntity.class.getMethod(
                    "addPotionEffect",
                    PotionEffect.class,
                    causeClass,
                    boolean.class
            );
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static Object findPluginCause() {
        try {
            Class<?> causeClass = Class.forName("org.bukkit.event.entity.EntityPotionEffectEvent$Cause");
            if (!causeClass.isEnum()) return null;

            Object[] constants = causeClass.getEnumConstants();
            if (constants == null) return null;

            for (Object c : constants) {
                if (c instanceof Enum<?> e && e.name().equals("PLUGIN")) {
                    return c;
                }
            }
            return null;
        } catch (Throwable ignored) {
            return null;
        }
    }

    private static void addPotionEffectCompat(Player player, PotionEffect effect, boolean force) {
        // 1) Новый метод (если существует) + PLUGIN cause (если нашли)
        if (ADD_EFFECT_CAUSE_BOOLEAN != null && PLUGIN_CAUSE != null) {
            try {
                ADD_EFFECT_CAUSE_BOOLEAN.invoke(player, effect, PLUGIN_CAUSE, force);
                return;
            } catch (Throwable ignored) {
                // fallback ниже
            }
        }

        // 2) Фоллбек: старый метод (deprecated), warning глушим точечно
        addPotionEffectDeprecated(player, effect, force);
    }

    @SuppressWarnings("deprecation")
    private static void addPotionEffectDeprecated(Player player, PotionEffect effect, boolean force) {
        player.addPotionEffect(effect, force);
    }

    public void applyFor(Player player) {
        PlayerProfile prof = classService.getProfile(player);
        if (prof == null) return;

        if (!prof.hasClass()) {
            if (clearManagedEffects) clear(player);
            return;
        }

        ClassConfigRepository.ClassDef def = classConfig.get(prof.getClassId());
        if (def == null) {
            if (clearManagedEffects) clear(player);
            return;
        }

        if (clearManagedEffects) clear(player);

        for (PotionEffect effect : parseEffects(def.effects)) {
            addPotionEffectCompat(player, effect, true);
        }
    }

    public void clear(Player player) {
        for (PotionEffectType type : managedTypes) {
            player.removePotionEffect(type);
        }
    }

    private List<PotionEffect> parseEffects(List<String> lines) {
        if (lines == null) return List.of();

        int durationTicks = 20 * 15; // 15 секунд (тикер у нас например 5 сек)

        return lines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    // FORMAT: NAME:AMPLIFIER
                    String[] parts = s.split(":");
                    String rawName = parts[0].trim();

                    int amp = 0;
                    if (parts.length > 1) {
                        try { amp = Integer.parseInt(parts[1].trim()); }
                        catch (NumberFormatException ignored) {}
                    }

                    PotionEffectType type = resolveEffectType(rawName);
                    if (type == null) return null;

                    return new PotionEffect(type, durationTicks, amp, true, false, true);
                })
                .filter(e -> e != null)
                .toList();
    }

    /**
     * Поддерживаем:
     * - "SPEED"
     * - "DAMAGE_RESISTANCE"
     * - "minecraft:speed"
     * - "resistance" (как minecraft key)
     */
    private PotionEffectType resolveEffectType(String raw) {
        if (raw == null || raw.isBlank()) return null;

        String s = raw.trim();

        // 1) Если явно namespaced key
        if (s.contains(":")) {
            NamespacedKey key = NamespacedKey.fromString(s.toLowerCase(Locale.ROOT));
            return key == null ? null : Registry.POTION_EFFECT_TYPE.get(key);
        }

        // 2) Пытаемся как minecraft key: "speed", "resistance"
        String keyLike = s.toLowerCase(Locale.ROOT).replace(' ', '_');

        PotionEffectType type = Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft(keyLike));
        if (type != null) return type;

        // 3) Алиас для старого "DAMAGE_RESISTANCE"
        if (keyLike.equals("damage_resistance")) {
            return Registry.POTION_EFFECT_TYPE.get(NamespacedKey.minecraft("resistance"));
        }

        return null;
    }
}
