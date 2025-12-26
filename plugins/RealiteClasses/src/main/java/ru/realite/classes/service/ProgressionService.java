package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.util.ItemFormat;
import ru.realite.classes.util.Messages;
import ru.realite.classes.util.ChatTemplate;
import ru.realite.classes.util.ItemComponents;

import java.util.Map;

public class ProgressionService {

    private final ClassService classService;
    private final ClassConfigRepository classConfig;
    private final EvolutionService evolutionService;
    private final Messages messages;

    public ProgressionService(ClassService classService,
            ClassConfigRepository classConfig,
            EvolutionService evolutionService,
            Messages messages) {
        this.classService = classService;
        this.classConfig = classConfig;
        this.evolutionService = evolutionService;
        this.messages = messages;
    }

    /**
     * Добавить XP игроку, пересчитать level,
     * записать перманентный максимум по классу,
     * и (один раз) показать уведомление "доступна эволюция".
     */
    public void addXp(Player player, long amount) {
        if (amount <= 0)
            return;

        PlayerProfile p = classService.getProfile(player);
        if (p == null || !p.hasClass())
            return;

        var def = classConfig.get(p.getClassId());
        if (def == null)
            return;

        int xpPerLevel = Math.max(1, def.xpPerLevel);

        int oldLevel = p.getClassLevel();

        long newXp = p.getClassXp() + amount;
        p.setClassXp(newXp);

        int newLevel = (int) (newXp / xpPerLevel);

        if (newLevel != oldLevel) {
            p.setClassLevel(newLevel);

            // перманентный прогресс по уровню (на будущее)
            int prevMax = p.getMaxLevelByClass().getOrDefault(p.getClassId().name(), 0);
            if (newLevel > prevMax) {
                p.getMaxLevelByClass().put(p.getClassId().name(), newLevel);
            }
        }

        // уведомление о доступной эволюции (один раз до следующей evolve)
        var next = evolutionService.getNextEvolution(p);
        if (next != null && !p.isEvolutionNotified()) {

            if (oldLevel < next.requiredLevel && newLevel >= next.requiredLevel) {

                String moneyText = (next.costMoney > 0) ? ("$" + (long) next.costMoney) : "0$";

                ChatTemplate.sendWithComponent(
                        player,
                        messages.get("evolution-available"),
                        Map.of(
                                "class", def.name,
                                "evolution", next.title,
                                "required", String.valueOf(next.requiredLevel),
                                "money", moneyText),
                        "{items}",
                        ItemComponents.listOrDash(next.costItems));

                p.setEvolutionNotified(true);
                
                //В старом виде без локализации через ванильный перевод клиента:
                // String moneyText = (next.costMoney > 0) ? ("$" + (long) next.costMoney) :
                // "0$";
                // String itemsText = ItemFormat.formatList(next.costItems);

                // player.sendMessage(messages.format("evolution-available", Map.of(
                // "class", def.name,
                // "evolution", next.title,
                // "required", String.valueOf(next.requiredLevel),
                // "money", moneyText,
                // "items", itemsText
                // )));
                // p.setEvolutionNotified(true);
            }
        }

        classService.save(p);
    }
}
