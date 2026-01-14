package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.core.CoreAccess;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.YamlProfileRepository;
import ru.realite.classes.ui.ClassLevelXpUiProvider;
import ru.realite.core.api.events.ClassSelectedEvent;
import ru.realite.core.api.ui.UiInvalidateEvent;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import javax.annotation.Nullable;

public class ClassService {

    private final YamlProfileRepository repo;
    private final EvolutionService evolutionService;

    private final Map<UUID, PlayerProfile> cache = new HashMap<>();

    public ClassService(YamlProfileRepository repo, EvolutionService evolutionService) {
        this.repo = repo;
        this.evolutionService = evolutionService;
    }

    /**
     * Получить профиль игрока (из кеша или загрузить с диска)
     */
    public PlayerProfile getProfile(Player player) {
        return cache.computeIfAbsent(player.getUniqueId(), repo::load);
    }

    /**
     * Сохранить профиль на диск
     */
    public void save(PlayerProfile profile) {
        repo.save(profile);
    }

    /**
     * Сохранить все профили из кеша (onDisable / autosave)
     */
    public void saveAll() {
        for (PlayerProfile profile : cache.values()) {
            repo.save(profile);
        }
    }

    /**
     * Удалить профиль из кеша (обычно на PlayerQuit)
     */
    public void invalidate(Player player) {
        cache.remove(player.getUniqueId());
    }

    /**
     * Назначить (или сменить) класс.
     * ВНИМАНИЕ: проверка "можно ли менять" — НЕ здесь.
     * Её делает EvolutionService (и вызывается из команд/GUI).
     */
    public void assignClass(Player player, ClassId newClass) {
        assignClass(player, newClass, null, null);
    }

    /**
     * Перегруженный метод назначения класса с возможностью указания эволюции и
     * уровня.
     */
    public void assignClass(Player player, ClassId newClass, @Nullable String evolutionId, @Nullable Integer level) {
        PlayerProfile p = getProfile(player);

        // стартовая эволюция (дефолт)
        String startEvo = evolutionService.getFirstEvolutionId(newClass);

        // если админ указал эволюцию — используем её (валидировать лучше ДО вызова)
        String evoToSet = (evolutionId != null && !evolutionId.isBlank()) ? evolutionId : startEvo;

        p.setClassId(newClass);
        p.setEvolution(evoToSet);

        // сброс прогрессии
        p.setClassLevel(0);
        p.setClassXp(0);

        // если админ указал level — применяем после сброса
        if (level != null) {
            p.setClassLevel(Math.max(0, level));
        }

        // флаги/таймеры
        p.setEvolutionRewardTaken(false);
        p.setEvolutionNotified(false);
        p.setLastClassChange(System.currentTimeMillis());

        // ✅ снимаем unlock-ready на этот класс, чтобы флаг не висел после получения
        p.removeReadyToUnlockClass(newClass);
        
        save(p);

        // важно: можно расширить событие позже, но хотя бы текущее не ломаем
        CoreAccess.core().events().publish(new ClassSelectedEvent(player.getUniqueId(), newClass.name()));
        CoreAccess.core().events().publish(new UiInvalidateEvent(player, ClassLevelXpUiProvider.ID));
    }
}
