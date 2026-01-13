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
import ru.realite.quests.model.QuestType;
import ru.realite.quests.service.QuestAvailability;
import ru.realite.quests.service.QuestListEntry;
import ru.realite.quests.service.QuestServiceImpl;
import ru.realite.quests.service.QuestSort;

public final class QuestsHubMenu extends QuestMenu {

    private static final int SIZE = 54;
    private static final int LIST_SIZE = 45;
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final QuestServiceImpl questService;
    private final QuestsMessages messages;
    private final QuestMenuState state;
    private final Player viewer;

    public QuestsHubMenu(QuestServiceImpl questService, QuestsMessages messages, Player viewer, QuestMenuState state) {
        super(SIZE, LEGACY.deserialize(messages.raw("ui.quests.hub.title")));
        this.questService = questService;
        this.messages = messages;
        this.state = state == null ? QuestMenuState.defaultState() : state;
        this.viewer = viewer;
        render();
    }

    private void render() {
        fillFiller(Material.BLACK_STAINED_GLASS_PANE);
        renderTabs();
        renderSort();
        renderQuests();
        renderPaging();
        renderClose();
    }

    private void renderTabs() {
        setButton(45, tabItem(QuestAvailability.ACTIVE), player ->
                new QuestsHubMenu(questService, messages, player,
                        new QuestMenuState(QuestAvailability.ACTIVE, state.sort(), 0)).open(player));
        setButton(46, tabItem(QuestAvailability.AVAILABLE), player ->
                new QuestsHubMenu(questService, messages, player,
                        new QuestMenuState(QuestAvailability.AVAILABLE, state.sort(), 0)).open(player));
        setButton(47, tabItem(QuestAvailability.COMPLETED), player ->
                new QuestsHubMenu(questService, messages, player,
                        new QuestMenuState(QuestAvailability.COMPLETED, state.sort(), 0)).open(player));
    }

    private ItemStack tabItem(QuestAvailability availability) {
        boolean selected = state.filter() == availability;
        Material material = switch (availability) {
            case ACTIVE -> selected ? Material.LIME_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            case AVAILABLE -> selected ? Material.YELLOW_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
            case COMPLETED -> selected ? Material.LIGHT_BLUE_STAINED_GLASS_PANE : Material.GRAY_STAINED_GLASS_PANE;
        };
        String key = switch (availability) {
            case ACTIVE -> "ui.quests.hub.tab.active";
            case AVAILABLE -> "ui.quests.hub.tab.available";
            case COMPLETED -> "ui.quests.hub.tab.completed";
        };
        return item(material, LEGACY.deserialize(messages.raw(key)), List.of());
    }

    private void renderSort() {
        List<QuestSort> supported = questService.getSupportedSorts();
        QuestSort current = supported.contains(state.sort()) ? state.sort()
                : supported.isEmpty() ? QuestSort.TYPE : supported.getFirst();
        List<Component> lore = new ArrayList<>();
        lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.sort.current",
                java.util.Map.of("sort", sortLabel(current)))));
        lore.add(LEGACY.deserialize(messages.raw("ui.common.coming_soon")));
        ItemStack sortItem = item(Material.HOPPER, LEGACY.deserialize(messages.raw("ui.quests.sort.title")), lore);
        setButton(49, sortItem, player -> {
            QuestSort next = nextSort(supported, current);
            new QuestsHubMenu(questService, messages, player,
                    new QuestMenuState(state.filter(), next, 0)).open(player);
        });
    }

    private QuestSort nextSort(List<QuestSort> supported, QuestSort current) {
        if (supported == null || supported.isEmpty()) {
            return QuestSort.TYPE;
        }
        int index = supported.indexOf(current);
        if (index == -1 || index + 1 >= supported.size()) {
            return supported.getFirst();
        }
        return supported.get(index + 1);
    }

    private String sortLabel(QuestSort sort) {
        return messages.raw("ui.quests.sort." + sort.name().toLowerCase(Locale.ROOT));
    }

    private void renderQuests() {
        List<QuestListEntry> quests = questService.getQuestList(viewer, state.filter(), state.sort());
        int startIndex = state.page() * LIST_SIZE;
        int endIndex = Math.min(startIndex + LIST_SIZE, quests.size());
        if (startIndex >= quests.size()) {
            startIndex = 0;
            endIndex = Math.min(LIST_SIZE, quests.size());
        }
        if (quests.isEmpty()) {
            ItemStack empty = item(Material.BARRIER, LEGACY.deserialize(messages.raw("ui.quests.hub.empty")),
                    List.of());
            setButton(22, empty, null);
            return;
        }
        for (int i = startIndex; i < endIndex; i++) {
            QuestListEntry entry = quests.get(i);
            int slot = i - startIndex;
            ItemStack item = questItem(entry);
            setButton(slot, item, player ->
                    new QuestDetailsMenu(questService, messages, player, entry.id(), state).open(player));
        }
    }

    private ItemStack questItem(QuestListEntry entry) {
        Material material = entry.type() == QuestType.INTRO ? Material.WRITABLE_BOOK : Material.BOOK;
        List<Component> lore = new ArrayList<>();
        if (entry.description() != null && !entry.description().isBlank()) {
            lore.add(LEGACY.deserialize(entry.description()));
        }
        lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.hub.quest.type",
                java.util.Map.of("type", entry.type().name()))));
        lore.add(LEGACY.deserialize(messages.formatRaw("ui.quests.hub.quest.status",
                java.util.Map.of("status", availabilityLabel(entry.availability())))));
        lore.add(LEGACY.deserialize(messages.raw("ui.quests.hub.quest.view")));
        return item(material, LEGACY.deserialize(entry.title()), lore);
    }

    private String availabilityLabel(QuestAvailability availability) {
        return messages.raw("ui.quests.details.status." + availability.name().toLowerCase(Locale.ROOT));
    }

    private void renderPaging() {
        List<QuestListEntry> quests = questService.getQuestList(viewer, state.filter(), state.sort());
        int totalPages = (int) Math.ceil(quests.size() / (double) LIST_SIZE);
        int currentPage = Math.min(state.page(), Math.max(totalPages - 1, 0));
        if (currentPage > 0) {
            setButton(51, item(Material.ARROW, LEGACY.deserialize(messages.raw("ui.common.prev")),
                    List.of()), player -> new QuestsHubMenu(questService, messages, player,
                    new QuestMenuState(state.filter(), state.sort(), currentPage - 1)).open(player));
        }
        if (currentPage + 1 < totalPages) {
            setButton(52, item(Material.ARROW, LEGACY.deserialize(messages.raw("ui.common.next")),
                    List.of()), player -> new QuestsHubMenu(questService, messages, player,
                    new QuestMenuState(state.filter(), state.sort(), currentPage + 1)).open(player));
        }
        setButton(50, item(Material.PAPER, LEGACY.deserialize(messages.formatRaw("ui.quests.hub.page",
                java.util.Map.of("page", String.valueOf(currentPage + 1),
                        "total", String.valueOf(Math.max(totalPages, 1))))), List.of()), null);
    }

    private void renderClose() {
        setButton(53, item(Material.BARRIER, LEGACY.deserialize(messages.raw("ui.common.close")),
                List.of()), Player::closeInventory);
    }
}
