package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.PlayerProfile;
import ru.realite.classes.storage.YamlProfileRepository;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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
        PlayerProfile p = getProfile(player);

        // стартовая эволюция — первая из classes.yml
        String startEvo = evolutionService.getFirstEvolutionId(newClass);

        p.setClassId(newClass);
        p.setEvolution(startEvo);

        // сброс прогрессии
        p.setClassLevel(0);
        p.setClassXp(0);

        // сброс/инициализация флагов и таймеров
        p.setEvolutionRewardTaken(false);
        p.setEvolutionNotified(false);
        p.setLastClassChange(System.currentTimeMillis());

        save(p);
    }
}
