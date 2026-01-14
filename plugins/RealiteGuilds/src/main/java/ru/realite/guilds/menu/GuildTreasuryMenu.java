package ru.realite.guilds.menu;

import java.util.List;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import ru.realite.guilds.i18n.GuildMessages;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.service.GuildRankPermission;
import ru.realite.guilds.service.GuildRankService;
import ru.realite.guilds.service.GuildTreasuryService;
import ru.realite.guilds.storage.GuildRepository;

public final class GuildTreasuryMenu extends GuildMenu {

    private static final int SIZE = 27;

    private final GuildRepository repository;
    private final GuildRankService rankService;
    private final GuildTreasuryService treasuryService;
    private final Player viewer;

    public GuildTreasuryMenu(
            GuildMenuManager manager,
            GuildMessages messages,
            GuildRepository repository,
            GuildRankService rankService,
            GuildTreasuryService treasuryService,
            Player viewer) {
        super(manager, messages, SIZE, "ui.guild.treasury.title");
        this.repository = repository;
        this.rankService = rankService;
        this.treasuryService = treasuryService;
        this.viewer = viewer;
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
        double balance = treasuryService.getBalance(guild.tag());
        List<Component> lore = List.of(messages.msg("ui.guild.treasury.balance", "amount", formatAmount(balance)));
        setButton(13, Material.SUNFLOWER, messages.msg("ui.guild.treasury.balance_title"), lore, null);

        boolean canSpend = rankService.hasPermission(member.role(), GuildRankPermission.TREASURY_SPEND);
        if (canSpend) {
            setButton(11, Material.EMERALD, messages.msg("ui.guild.treasury.deposit_disabled"),
                    List.of(messages.msg("ui.guild.treasury.unavailable")), null);
            setButton(15, Material.REDSTONE, messages.msg("ui.guild.treasury.withdraw_disabled"),
                    List.of(messages.msg("ui.guild.treasury.unavailable")), null);
        } else {
            setButton(11, Material.GRAY_DYE, messages.msg("ui.guild.treasury.deposit_disabled"),
                    List.of(messages.msg("ui.guild.treasury.no_permission")), null);
            setButton(15, Material.GRAY_DYE, messages.msg("ui.guild.treasury.withdraw_disabled"),
                    List.of(messages.msg("ui.guild.treasury.no_permission")), null);
        }
        setButton(16, Material.BOOK, messages.msg("ui.guild.treasury.history_disabled"),
                List.of(messages.msg("ui.guild.treasury.unavailable")), null);
        setButton(22, Material.ARROW, "ui.common.back", List.of("ui.guild.menu.back_lore"), manager::openRoot);
    }

    private String formatAmount(double amount) {
        return String.format("%.2f", amount);
    }
}
