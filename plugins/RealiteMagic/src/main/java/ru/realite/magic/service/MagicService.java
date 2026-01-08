package ru.realite.magic.service;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.plugin.RegisteredServiceProvider;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassProfile;
import ru.realite.core.api.classes.ClassProfileProvider;
import ru.realite.items.service.ItemService;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.model.MageState;
import ru.realite.magic.gui.SpellSelectMenu;
import ru.realite.magic.spell.SpellCaster;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;
import ru.realite.magic.spell.SpellRequirements;

public final class MagicService {

    private static final String GLOBAL_COOLDOWN_KEY = "global";

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final SpellRegistry spellRegistry;
    private final SpellCaster caster;
    private final Map<UUID, MageState> states = new HashMap<>();
    private final SpellSelectMenu spellSelectMenu;
    private BukkitTask regenTask;

    public MagicService(JavaPlugin plugin,
                        MagicMessages messages,
                        SpellRegistry spellRegistry,
                        PlayerSpellService playerSpellService) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
        this.caster = new SpellCaster(this, messages);
        this.spellSelectMenu = new SpellSelectMenu(plugin, spellRegistry, playerSpellService, messages);
    }

    public void start() {
        if (regenTask != null) {
            return;
        }
        regenTask = Bukkit.getScheduler().runTaskTimer(plugin, this::tickRegen, 20L, 20L);
    }

    public void stop() {
        if (regenTask != null) {
            regenTask.cancel();
            regenTask = null;
        }
    }

    public SpellRegistry spellRegistry() {
        return spellRegistry;
    }

    public double getMana(Player player) {
        return state(player).mana();
    }

    public double getMaxMana(Player player) {
        return state(player).maxMana();
    }

    public void setMana(Player player, double mana) {
        MageState state = state(player);
        state.mana(clamp(mana, 0, state.maxMana()));
    }

    public void addMana(Player player, double amount) {
        setMana(player, getMana(player) + amount);
    }

    public boolean canCast(Player player, SpellDefinition spell) {
        if (spell == null) {
            return false;
        }
        if (!meetsRequirements(player, spell)) {
            return false;
        }
        if (isOnCooldown(player, GLOBAL_COOLDOWN_KEY)) {
            return false;
        }
        if (isOnCooldown(player, spell.id())) {
            return false;
        }
        return getMana(player) >= spell.mana();
    }

    public boolean spendMana(Player player, SpellDefinition spell) {
        if (!canCast(player, spell)) {
            return false;
        }
        addMana(player, -spell.mana());
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), spell.cooldownTicks());
        return true;
    }

    public boolean isOnCooldown(Player player, String key) {
        MageState state = state(player);
        Long until = state.cooldowns().get(key);
        return until != null && until > System.currentTimeMillis();
    }

    public boolean isOnGlobalCooldown(Player player) {
        return isOnCooldown(player, GLOBAL_COOLDOWN_KEY);
    }

    public void setCooldown(Player player, String key, long ticks) {
        if (ticks <= 0) {
            return;
        }
        MageState state = state(player);
        long until = System.currentTimeMillis() + ticks * 50L;
        state.cooldowns().put(key, until);
    }

    public long remainingCooldownTicks(Player player, String key) {
        MageState state = state(player);
        Long until = state.cooldowns().get(key);
        if (until == null) {
            return 0;
        }
        long remainingMs = until - System.currentTimeMillis();
        if (remainingMs <= 0) {
            return 0;
        }
        return (long) Math.ceil(remainingMs / 50.0);
    }

    public long remainingGlobalCooldownTicks(Player player) {
        return remainingCooldownTicks(player, GLOBAL_COOLDOWN_KEY);
    }

    public void markCombat(Player player) {
        state(player).lastCombatTime(System.currentTimeMillis());
    }

    public void cleanup(Player player) {
        states.remove(player.getUniqueId());
    }

    public void cast(Player player, SpellDefinition spell) {
        if (!meetsRequirements(player, spell)) {
            sendRequirementMessage(player, spell);
            return;
        }
        caster.cast(player, spell);
    }

    public MagicMessages messages() {
        return messages;
    }

    public SpellSelectMenu spellSelectMenu() {
        return spellSelectMenu;
    }

    public void setSelectedSpell(Player player, String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return;
        }
        state(player).selectedSpellId(spellId);
    }

    public void clearSelectedSpell(Player player) {
        state(player).selectedSpellId(null);
    }

    public String getSelectedSpellId(Player player) {
        return state(player).selectedSpellId();
    }

    public SpellDefinition getSelectedSpell(Player player) {
        String spellId = getSelectedSpellId(player);
        if (spellId == null || spellId.isBlank()) {
            return null;
        }
        return spellRegistry.get(spellId);
    }

    public boolean meetsRequirements(Player player, SpellDefinition spell) {
        if (spell == null) {
            return false;
        }
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        ClassProfileProvider provider = resolveClassProfileProvider();
        if (provider == null) {
            return true;
        }
        Optional<ClassProfile> profile = provider.getProfile(player);
        if (profile.isEmpty()) {
            return false;
        }
        ClassProfile info = profile.get();
        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            if (info.classId() == null || !info.classId().equalsIgnoreCase(classId)) {
                return false;
            }
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            if (info.evolutionId() == null || !info.evolutionId().equalsIgnoreCase(evolutionId)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasRequiredFocus(Player player) {
        if (!plugin.getConfig().getBoolean("casting.requireFocus", false)) {
            return true;
        }
        ItemService itemService = resolveItemService();
        if (itemService == null) {
            return false;
        }
        var allowed = plugin.getConfig().getStringList("casting.allowedFocusItemIds");
        if (allowed == null || allowed.isEmpty()) {
            return false;
        }
        var inventory = player.getInventory();
        var mainHand = inventory.getItemInMainHand();
        var offHand = inventory.getItemInOffHand();
        for (String allowedId : allowed) {
            if (itemService.isItem(mainHand, allowedId) || itemService.isItem(offHand, allowedId)) {
                return true;
            }
        }
        return false;
    }

    private void tickRegen() {
        double regenPerSecond = plugin.getConfig().getDouble("mana.regenPerSecond", 0);
        if (regenPerSecond <= 0) {
            return;
        }
        double inCombatMultiplier = plugin.getConfig().getDouble("mana.regenInCombatMultiplier", 1.0);
        double combatTimeoutSeconds = plugin.getConfig().getDouble("mana.combatTimeoutSeconds", 10.0);
        long combatTimeoutMs = (long) (combatTimeoutSeconds * 1000L);
        long now = System.currentTimeMillis();
        for (Player player : Bukkit.getOnlinePlayers()) {
            MageState state = state(player);
            double regen = regenPerSecond;
            if (now - state.lastCombatTime() <= combatTimeoutMs) {
                regen *= inCombatMultiplier;
            }
            if (regen != 0) {
                addMana(player, regen);
            }
        }
    }

    private MageState state(Player player) {
        return states.computeIfAbsent(player.getUniqueId(), id -> {
            double max = maxManaDefault();
            return new MageState(max, max);
        });
    }

    private double maxManaDefault() {
        return plugin.getConfig().getDouble("mana.maxDefault", 100);
    }

    private long globalCastTicks() {
        return plugin.getConfig().getLong("cooldowns.globalCastTicks", 10);
    }

    public void consumeManaAndCooldowns(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        addMana(player, -spell.mana());
        setCooldown(player, GLOBAL_COOLDOWN_KEY, globalCastTicks());
        setCooldown(player, spell.id(), spell.cooldownTicks());
    }

    public String configString(String path, String fallback) {
        FileConfiguration config = plugin.getConfig();
        if (config == null) {
            return fallback;
        }
        String value = config.getString(path);
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value;
    }

    private ItemService resolveItemService() {
        RegisteredServiceProvider<ItemService> provider =
                Bukkit.getServicesManager().getRegistration(ItemService.class);
        return provider != null ? provider.getProvider() : null;
    }

    private ClassProfileProvider resolveClassProfileProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null || provider.getProvider() == null) {
            return null;
        }
        CoreApi core = provider.getProvider();
        return core.services().get(ClassProfileProvider.class);
    }

    private void sendRequirementMessage(Player player, SpellDefinition spell) {
        if (spell == null) {
            return;
        }
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        ClassProfileProvider provider = resolveClassProfileProvider();
        if (provider == null) {
            return;
        }
        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            player.sendMessage(messages.msg("magic.spell.requirements.class",
                    "class", classId));
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            player.sendMessage(messages.msg("magic.spell.requirements.evolution",
                    "evolution", evolutionId));
        }
    }

    private double clamp(double value, double min, double max) {
        return Math.max(min, Math.min(max, value));
    }
}
