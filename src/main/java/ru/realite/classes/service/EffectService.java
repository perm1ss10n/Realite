package ru.realite.classes.service;

import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class EffectService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final boolean clearManagedEffects;

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

    public void applyFor(Player player) {
        PlayerProfile prof = classService.getProfile(player);

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
            // duration: чуть больше тика, чтобы не мигало
            player.addPotionEffect(effect, true);
        }
    }

    public void clear(Player player) {
        for (PotionEffectType type : managedTypes) {
            player.removePotionEffect(type);
        }
    }

    private List<PotionEffect> parseEffects(List<String> lines) {
        if (lines == null) return List.of();

        // делаем эффекты длиннее, чем период тика
        int durationTicks = 20 * 15; // 15 секунд (тикер у нас например 5 сек)

        return lines.stream()
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(s -> {
                    // FORMAT: NAME:AMPLIFIER
                    String[] parts = s.split(":");
                    String name = parts[0].trim().toUpperCase();
                    int amp = 0;
                    if (parts.length > 1) {
                        try { amp = Integer.parseInt(parts[1].trim()); } catch (NumberFormatException ignored) {}
                    }

                    PotionEffectType type = PotionEffectType.getByName(name);
                    if (type == null) return null;

                    return new PotionEffect(type, durationTicks, amp, true, false, true);
                })
                .filter(e -> e != null)
                .toList();
    }
}