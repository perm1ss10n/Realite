package ru.realite.guilds.menu;

import java.util.List;
import java.util.function.Consumer;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.service.GuildUpgradeService;

public final class GuildConfirmMenu extends GuildMenu {

    private static final int SIZE = 27;

    private final Consumer<Player> onConfirm;
    private final Consumer<Player> onCancel;

    private GuildConfirmMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            String titleKey,
            Component description,
            Consumer<Player> onConfirm,
            Consumer<Player> onCancel) {
        super(manager, messages, SIZE, titleKey);
        this.onConfirm = onConfirm;
        this.onCancel = onCancel;
        build(description);
    }

    private void build(Component description) {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        setButton(13, Material.PAPER, messages.msg("ui.guild.confirm.info"), List.of(description), null);
        setButton(11, Material.LIME_CONCRETE, messages.msg("ui.common.confirm"), List.of(), player -> {
            player.closeInventory();
            onConfirm.accept(player);
        });
        setButton(15, Material.RED_CONCRETE, messages.msg("ui.common.cancel"), List.of(), player -> {
            if (onCancel != null) {
                player.closeInventory();
                onCancel.accept(player);
            } else {
                player.closeInventory();
            }
        });
    }

    public static void openLeave(GuildMenuManager manager, Player player) {
        GuildConfirmMenu menu = new GuildConfirmMenu(
                manager,
                manager.messages(),
                "ui.guild.confirm.leave.title",
                manager.messages().msg("ui.guild.confirm.leave.desc"),
                target -> manager.service().leave(target),
                manager::openRoot
        );
        menu.open(player);
    }

    public static void openDisband(GuildMenuManager manager, Player player) {
        GuildConfirmMenu menu = new GuildConfirmMenu(
                manager,
                manager.messages(),
                "ui.guild.confirm.disband.title",
                manager.messages().msg("ui.guild.confirm.disband.desc"),
                target -> manager.service().disband(target),
                manager::openRoot
        );
        menu.open(player);
    }

    public static void openUpgrade(
            GuildMenuManager manager,
            Player player,
            String upgradeId,
            String upgradeName,
            String cost,
            int returnPage) {
        Component description = manager.messages().msg("ui.guild.confirm.upgrade.desc",
                "name", upgradeName,
                "cost", cost);
        GuildConfirmMenu menu = new GuildConfirmMenu(
                manager,
                manager.messages(),
                "ui.guild.confirm.upgrade.title",
                description,
                target -> handleUpgradePurchase(manager, target, upgradeId, returnPage),
                target -> manager.openUpgrades(target, returnPage)
        );
        menu.open(player);
    }

    private static void handleUpgradePurchase(
            GuildMenuManager manager,
            Player player,
            String upgradeId,
            int returnPage) {
        GuildUpgradeService.PurchaseResult result = manager.upgradeService().purchase(player, upgradeId);
        switch (result.status()) {
            case SUCCESS -> manager.messages().send(player, "upgrade.buy.success");
            case INSUFFICIENT_FUNDS -> manager.messages().send(player, "upgrade.buy.insufficient_funds");
            case MAX_LEVEL -> manager.messages().send(player, "upgrade.buy.max_level");
            case REQUIREMENTS_NOT_MET -> manager.messages().send(player, "upgrade.buy.requirements");
            case NO_PERMISSION -> manager.messages().send(player, "error.no_permission");
            case NOT_IN_GUILD -> manager.messages().send(player, "error.guild.no_member");
            case GUILD_NOT_FOUND -> manager.messages().send(player, "guild.not_found");
            case UPGRADE_NOT_FOUND -> manager.messages().send(player, "upgrade.buy.not_found");
            case UPGRADE_DISABLED, INVALID_COST, INVALID_REQUEST ->
                    manager.messages().send(player, "upgrade.buy.unavailable");
        }
        manager.openUpgrades(player, returnPage);
    }
}
