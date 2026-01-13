package ru.realite.quests.gui;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import ru.realite.quests.i18n.QuestsMessages;
import ru.realite.quests.model.RewardType;
import ru.realite.quests.service.QuestDetails;
import ru.realite.quests.service.QuestObjectiveStatus;
import ru.realite.quests.service.QuestRewardView;
import ru.realite.quests.service.QuestServiceImpl;

public final class QuestDetailsMenu extends QuestMenu {

    private static final int SIZE = 54;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();
    private static final List<Integer> OBJECTIVE_SLOTS = List.of(
            10, 11, 12, 13, 14, 15, 16,
            19, 20, 21, 22, 23, 24, 25,
            28, 29, 30, 31, 32, 33, 34
    );
    private static final List<Integer> REWARD_SLOTS = List.of(37, 38, 39, 40, 41, 42, 43);

    private final QuestServiceImpl questService;
    private final QuestsMessages messages;
    private final Player viewer;
    private final QuestMenuState state;
    private final String questId;

    public QuestDetailsMenu(QuestServiceImpl questService, QuestsMessages messages, Player viewer,
                            String questId, QuestMenuState state) {
        super(SIZE, title(messages, viewer, questService, questId));
        this.questService = questService;
        this.messages = messages;
        this.viewer = viewer;
        this.questId = questId;
        this.state = state == null ? QuestMenuState.defaultState() : state;
        render();
    }

    private static Component title(QuestsMessages messages, Player viewer, QuestServiceImpl questService,
                                   String questId) {
        QuestDetails details = questService.getQuestDetails(viewer, questId);
        String title = details == null ? questId : details.title();
        return LEGACY.deserialize(messages.formatRaw("ui.quests.details.title",
                java.util.Map.of("title", title)));
    }

    private void render() {
        QuestDetails details = questService.getQuestDetails(viewer, questId);
        if (details == null) {
            viewer.sendMessage(LEGACY.deserialize(messages.raw("ui.quests.details.not_found")));
            viewer.closeInventory();
            return;
        }
        fillFiller(Material.GRAY_STAINED_GLASS_PANE);
        renderSummary(details);
        renderObjectives(details.objectives());
        renderRewards(details.rewards());
        renderActions();
    }

    private void renderSummary(QuestDetails details) {
        List<Component> lore = new ArrayList<>();
        if (details.description() != null && !details.description().isBlank()) {
            lore.add(LEGACY.deserialize(details.description()));
        }
        lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.hub.quest.type",
                java.util.Map.of("type", details.type().name()))));
        lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.hub.quest.status",
                java.util.Map.of("status", availabilityLabel(details)))));
        ItemStack summary = item(Material.WRITABLE_BOOK, LEGACY.deserialize(details.title()), lore);
        setButton(4, summary, null);
    }

    private String availabilityLabel(QuestDetails details) {
        return messages.raw("ui.quests.details.status." + details.availability().name().toLowerCase(Locale.ROOT));
    }

    private void renderObjectives(List<QuestObjectiveStatus> objectives) {
        int slotIndex = 0;
        for (QuestObjectiveStatus objective : objectives) {
            if (slotIndex >= OBJECTIVE_SLOTS.size()) {
                ItemStack more = item(Material.BOOK, LEGACY.deserialize(messages.raw("ui.quests.details.more")),
                        List.of());
                setButton(OBJECTIVE_SLOTS.getLast(), more, null);
                break;
            }
            List<Component> lore = new ArrayList<>();
            lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.details.progress",
                    java.util.Map.of("current", String.valueOf(objective.current()),
                            "target", String.valueOf(objective.target())))));
            lore.add(LEGACY.deserialize(objective.completed()
                    ? messages.raw("ui.quests.details.completed")
                    : messages.raw("ui.quests.details.incomplete")));
            ItemStack item = item(Material.PAPER, objectiveName(objective), lore);
            setButton(OBJECTIVE_SLOTS.get(slotIndex), item, null);
            slotIndex++;
        }
    }

    private Component objectiveName(QuestObjectiveStatus objective) {
        String prefix = objective.completed() ? "&a✔ " : "&7• ";
        return LEGACY.deserialize(prefix + objective.description());
    }

    private void renderRewards(List<QuestRewardView> rewards) {
        int slotIndex = 0;
        for (QuestRewardView reward : rewards) {
            if (slotIndex >= REWARD_SLOTS.size()) {
                break;
            }
            ItemStack item = item(rewardMaterial(reward),
                    LEGACY.deserialize(rewardName(reward)),
                    List.of(LEGACY.deserialize(messages.formatRaw("ui.quests.details.reward_amount",
                            java.util.Map.of("amount", String.valueOf(reward.amount()))))));
            setButton(REWARD_SLOTS.get(slotIndex), item, null);
            slotIndex++;
        }
    }

    private Material rewardMaterial(QuestRewardView reward) {
        if (reward.material() != null) {
            return reward.material();
        }
        return switch (reward.type()) {
            case XP -> Material.EXPERIENCE_BOTTLE;
            case CLASS_XP -> Material.ENCHANTED_BOOK;
            case ITEM -> Material.CHEST;
            case QUEST_UNLOCK -> Material.TRIPWIRE_HOOK;
        };
    }

    private String rewardName(QuestRewardView reward) {
        String key = "ui.quests.reward." + reward.type().name().toLowerCase(Locale.ROOT);
        if (reward.type() == RewardType.ITEM && reward.material() != null) {
            return messages.formatRaw(key, java.util.Map.of("item", reward.material().name()));
        }
        if (reward.type() == RewardType.QUEST_UNLOCK && reward.unlockId() != null) {
            return messages.formatRaw(key, java.util.Map.of("unlock", reward.unlockId()));
        }
        return messages.raw(key);
    }

    private void renderActions() {
        ItemStack track = item(Material.COMPASS, LEGACY.deserialize(messages.raw("ui.quests.details.track")),
                List.of(LEGACY.deserialize(messages.raw("ui.common.coming_soon"))));
        ItemStack abandon = item(Material.BARRIER, LEGACY.deserialize(messages.raw("ui.quests.details.abandon")),
                List.of(LEGACY.deserialize(messages.raw("ui.common.coming_soon"))));
        ItemStack back = item(Material.ARROW, LEGACY.deserialize(messages.raw("ui.common.back")), List.of());
        setButton(45, track, null);
        setButton(46, abandon, null);
        setButton(53, back, player ->
                new QuestsHubMenu(questService, messages, player, state).open(player));
    }
}
