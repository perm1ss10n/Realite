package ru.realite.magic.hud;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.Component;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Nullable;
import ru.realite.magic.cast.WarnLimiter;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class MagicHudService {

    private static final String REASON_COOLDOWN = "magic.cast.cooldown";
    private static final String REASON_NO_MANA = "magic.cast.no_mana";
    private static final String REASON_NO_TARGET = "magic.cast.no_target";

    private final JavaPlugin plugin;
    private final MagicMessages messages;
    private final SpellRegistry spellRegistry;
    private final WarnLimiter warnLimiter = new WarnLimiter();

    public MagicHudService(JavaPlugin plugin,
                           MagicMessages messages,
                           SpellRegistry spellRegistry) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.messages = Objects.requireNonNull(messages, "messages");
        this.spellRegistry = Objects.requireNonNull(spellRegistry, "spellRegistry");
    }

    public void showSelected(Player player, int slot, @Nullable String spellId) {
        if (!isEnabled() || !showOnSlotChange()) {
            return;
        }
        String key = "selected:" + slot + ":" + (spellId == null ? "empty" : spellId);
        if (spellId == null || spellId.isBlank()) {
            showActionBar(player, key, messages.msg("magic.hud.selected_empty",
                    "slot", String.valueOf(slot)));
            return;
        }
        String spellName = displaySpellName(spellId);
        showActionBar(player, key, messages.msg("magic.hud.selected",
                "slot", String.valueOf(slot),
                "spell", spellName));
    }

    public void showCastFail(Player player, String reasonKey, Map<String, String> placeholders) {
        if (!isEnabled() || !showFailReasons()) {
            return;
        }
        if (REASON_COOLDOWN.equals(reasonKey)) {
            String time = placeholders.getOrDefault("time", "?");
            showActionBar(player, "cooldown", messages.msg("magic.hud.cooldown", "time", time));
            return;
        }
        if (REASON_NO_MANA.equals(reasonKey)) {
            String needed = placeholders.getOrDefault("mana", "?");
            showActionBar(player, "no_mana", messages.msg("magic.hud.no_mana", "need", needed));
            return;
        }
        if (REASON_NO_TARGET.equals(reasonKey)) {
            showActionBar(player, "no_target", messages.msg("magic.hud.no_target"));
            return;
        }
        String reason = messages.raw(reasonKey, placeholders);
        showActionBar(player, "fail:" + reasonKey, messages.msg("magic.hud.fail", "reason", reason));
    }

    public void showCooldown(Player player, double seconds) {
        if (!isEnabled()) {
            return;
        }
        String time = formatCooldownSeconds(seconds);
        showActionBar(player, "cooldown", messages.msg("magic.hud.cooldown", "time", time));
    }

    public void showMana(Player player, int current, int max) {
        if (!isEnabled()) {
            return;
        }
        showActionBar(player, "mana", messages.msg("magic.hud.mana",
                "current", String.valueOf(current),
                "max", String.valueOf(max)));
    }

    public void showCastSuccess(Player player, SpellDefinition spell) {
        if (!isEnabled() || !showSuccess()) {
            return;
        }
        String spellName = displaySpellName(spell == null ? null : spell.id());
        showActionBar(player, "success", messages.msg("magic.hud.success", "spell", spellName));
    }

    public void showGeneric(Player player, Component message) {
        if (!isEnabled()) {
            return;
        }
        showActionBar(player, "generic", message);
    }

    public void cleanup(Player player) {
        warnLimiter.clear(player.getUniqueId());
    }

    private void showActionBar(Player player, String key, Component message) {
        if (!useActionBar()) {
            return;
        }
        long throttleMs = throttleMs();
        if (throttleMs > 0 && !warnLimiter.canWarn(player.getUniqueId(), key, throttleMs)) {
            return;
        }
        player.sendActionBar(message);
    }

    private boolean isEnabled() {
        return config().getBoolean("hud.enabled", true);
    }

    private boolean useActionBar() {
        return config().getBoolean("hud.useActionBar", true);
    }

    private boolean showOnSlotChange() {
        return config().getBoolean("hud.showOnSlotChange", true);
    }

    private boolean showFailReasons() {
        return config().getBoolean("hud.showFailReasons", true);
    }

    private boolean showSuccess() {
        return config().getBoolean("hud.showSuccess", false);
    }

    private long throttleMs() {
        return config().getLong("hud.throttleMs", 250L);
    }

    private String formatCooldownSeconds(double seconds) {
        double rounded = Math.ceil(seconds * 10.0) / 10.0;
        String pattern = config().getString("casting.cooldownFormat", "0.0");
        DecimalFormat format;
        try {
            format = new DecimalFormat(pattern, DecimalFormatSymbols.getInstance(Locale.US));
        } catch (IllegalArgumentException ex) {
            format = new DecimalFormat("0.0", DecimalFormatSymbols.getInstance(Locale.US));
        }
        return format.format(rounded);
    }

    private String displaySpellName(@Nullable String spellId) {
        if (spellId == null || spellId.isBlank()) {
            return "";
        }
        SpellDefinition spell = spellRegistry.get(spellId);
        if (spell == null) {
            return spellId;
        }
        String nameKey = spell.nameKey();
        if (nameKey == null || nameKey.isBlank()) {
            return spell.id();
        }
        String raw = messages.raw(nameKey);
        return raw == null || raw.isBlank() ? spell.id() : raw;
    }

    private FileConfiguration config() {
        return plugin.getConfig();
    }
}
