package ru.realite.guilds.menu;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.model.upgrade.UpgradeDefinition;
import ru.realite.guilds.service.GuildUpgradeService;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

public final class GuildUpgradesMenu extends GuildMenu {

    private static final int SIZE = 54;
    private static final int PAGE_SIZE = 45;

    private final GuildRepository repository;
    private final GuildUpgradeService upgradeService;
    private final GuildUpgradeConfigRepository upgradeConfig;
    private final Player viewer;
    private final int page;

    public GuildUpgradesMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            GuildRepository repository,
            GuildUpgradeService upgradeService,
            GuildUpgradeConfigRepository upgradeConfig,
            int page,
            Player viewer) {
        super(manager, messages, SIZE, "gui.upgrades.title");
        this.repository = repository;
        this.upgradeService = upgradeService;
        this.upgradeConfig = upgradeConfig;
        this.viewer = viewer;
        this.page = Math.max(0, page);
        build();
    }

    private void build() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        GuildMember member = repository.getMember(viewer.getUniqueId());
        if (member == null) {
            messages.send(viewer, "error.guild.no_member");
            return;
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            messages.send(viewer, "guild.not_found");
            return;
        }

        GuildUpgradeService.UpgradeListResult listResult = upgradeService.list(viewer);
        if (listResult.status() != GuildUpgradeService.UpgradeListStatus.SUCCESS) {
            messages.send(viewer, "gui.upgrades.unavailable");
            return;
        }
        java.util.Map<String, GuildUpgradeService.UpgradeEntry> entryMap = listResult.entries().stream()
                .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.id().toLowerCase(java.util.Locale.ROOT),
                        entry -> entry
                ));

        List<UpgradeDefinition> upgrades = upgradeConfig.getUpgrades().values().stream()
                .sorted(Comparator.comparing(UpgradeDefinition::id, String.CASE_INSENSITIVE_ORDER))
                .toList();

        int start = page * PAGE_SIZE;
        int end = Math.min(start + PAGE_SIZE, upgrades.size());
        if (start >= upgrades.size() && page > 0) {
            manager.openUpgrades(viewer, page - 1);
            return;
        }
        for (int i = start; i < end; i++) {
            UpgradeDefinition definition = upgrades.get(i);
            int slot = i - start;
            setUpgradeItem(viewer, guild, definition, entryMap.get(definition.id().toLowerCase(java.util.Locale.ROOT)), slot);
        }

        setButton(45, Material.ARROW, "gui.menu.back", List.of("gui.menu.back_lore"), manager::openRoot);
        if (page > 0) {
            setButton(48, Material.ARROW, "gui.menu.prev", List.of("gui.menu.page_lore"),
                    player -> manager.openUpgrades(player, page - 1));
        } else {
            setButton(48, Material.GRAY_DYE, messages.msg("gui.menu.prev_disabled"), null, null);
        }
        if (end < upgrades.size()) {
            setButton(50, Material.ARROW, "gui.menu.next", List.of("gui.menu.page_lore"),
                    player -> manager.openUpgrades(player, page + 1));
        } else {
            setButton(50, Material.GRAY_DYE, messages.msg("gui.menu.next_disabled"), null, null);
        }
    }

    private void setUpgradeItem(Player viewer, Guild guild, UpgradeDefinition definition,
                                GuildUpgradeService.UpgradeEntry entry, int slot) {
        int currentLevel = entry == null ? repository.getUpgradeLevel(guild.tag(), definition.id()) : entry.level();
        boolean maxed = entry != null && entry.maxed();
        double nextCost = entry == null ? 0.0d : entry.nextCost();
        List<Component> lore = new ArrayList<>();
        if (definition.description() != null && !definition.description().isBlank()) {
            lore.add(messages.msg("gui.upgrades.description", "description", definition.description()));
        }
        lore.add(messages.msg("gui.upgrades.level", "level", String.valueOf(currentLevel),
                "max", String.valueOf(definition.maxLevel())));
        if (maxed) {
            lore.add(messages.msg("gui.upgrades.cost_maxed"));
        } else if (nextCost <= 0.0d) {
            lore.add(messages.msg("gui.upgrades.cost_unavailable"));
        } else {
            lore.add(messages.msg("gui.upgrades.cost", "amount", formatAmount(nextCost)));
        }

        appendRequirements(lore, definition);
        if (!definition.enabled()) {
            lore.add(messages.msg("gui.upgrades.disabled"));
        } else if (!maxed && nextCost > 0.0d) {
            lore.add(messages.msg("gui.upgrades.click_to_buy"));
        }

        setButton(slot, Material.ENCHANTED_BOOK, Component.text(definition.name()), lore,
                player -> openConfirmIfAllowed(viewer, definition, maxed, nextCost));
    }

    private void openConfirmIfAllowed(Player viewer, UpgradeDefinition definition, boolean maxed, double nextCost) {
        if (!definition.enabled() || maxed || nextCost <= 0.0d) {
            return;
        }
        GuildConfirmMenu.openUpgrade(
                manager,
                viewer,
                definition.id(),
                definition.name(),
                formatAmount(nextCost),
                page
        );
    }

    private void appendRequirements(List<Component> lore, UpgradeDefinition definition) {
        Map<String, Integer> requirements = definition.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return;
        }
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            if ("guildlevelatleast".equalsIgnoreCase(entry.getKey())) {
                int value = entry.getValue() == null ? 0 : entry.getValue();
                lore.add(messages.msg("gui.upgrades.req_level", "level", String.valueOf(value)));
            }
        }
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
