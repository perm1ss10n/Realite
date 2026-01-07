package ru.realite.quests.backstory;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.BookMeta;
import ru.realite.core.api.quests.ClassBackstoryService;
import ru.realite.core.api.quests.QuestService;
import ru.realite.core.api.quests.QuestStartTrigger;
import ru.realite.quests.i18n.QuestsMessages;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

public final class ClassBackstoryServiceImpl implements ClassBackstoryService {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final ClassBackstoryConfig config;
    private final BackstoryProgressRepository progressRepository;
    private final QuestService questService;
    private final QuestsMessages messages;
    public ClassBackstoryServiceImpl(ClassBackstoryConfig config,
                                     BackstoryProgressRepository progressRepository,
                                     QuestService questService,
                                     QuestsMessages messages) {
        this.config = Objects.requireNonNull(config, "config");
        this.progressRepository = Objects.requireNonNull(progressRepository, "progressRepository");
        this.questService = Objects.requireNonNull(questService, "questService");
        this.messages = Objects.requireNonNull(messages, "messages");
    }

    @Override
    public void show(Player player, String classId, boolean force) {
        String normalized = normalize(classId);
        if (player == null || normalized == null) {
            return;
        }
        if (!force && progressRepository.isConfirmed(player.getUniqueId(), normalized)) {
            return;
        }

        ClassBackstoryDefinition definition = config.get(normalized);
        if (definition == null || definition.pages().isEmpty()) {
            player.sendMessage(messages.format(
                    "quests.backstory.missing",
                    Map.of("class", normalized)
            ));
            return;
        }

        List<Component> pages = new ArrayList<>();
        for (String page : definition.pages()) {
            pages.add(LEGACY.deserialize(page));
        }

        int lastIndex = pages.size() - 1;
        Component lastPage = pages.get(lastIndex);
        pages.set(lastIndex, lastPage
                .append(Component.newline())
                .append(Component.newline())
                .append(buildActions(normalized)));

        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        BookMeta meta = (BookMeta) book.getItemMeta();
        meta.setTitle(messages.format("quests.backstory.book.title", Map.of("title", definition.title())));
        meta.setAuthor(messages.raw("quests.backstory.book.author"));
        meta.pages(pages);
        book.setItemMeta(meta);
        player.openBook(book);
    }

    @Override
    public void accept(Player player, String classId) {
        String normalized = normalize(classId);
        if (player == null || normalized == null) {
            return;
        }
        progressRepository.setAccepted(player.getUniqueId(), normalized, true);
        progressRepository.setAcceptedClass(player.getUniqueId(), normalized);
        player.sendMessage(messages.format("quests.backstory.accepted", Map.of("class", normalized)));
        ClassBackstoryDefinition definition = config.get(normalized);
        if (definition != null && definition.introQuestId() != null) {
            questService.start(player, definition.introQuestId(), QuestStartTrigger.CLASS_ACCEPTED, false);
        }
    }

    @Override
    public void skip(Player player, String classId) {
        String normalized = normalize(classId);
        if (player == null || normalized == null) {
            return;
        }
        progressRepository.setAccepted(player.getUniqueId(), normalized, false);
        player.sendMessage(messages.format("quests.backstory.skipped", Map.of("class", normalized)));
    }

    @Override
    public void later(Player player, String classId) {
        String normalized = normalize(classId);
        if (player == null || normalized == null) {
            return;
        }
        player.sendMessage(messages.format("quests.backstory.deferred", Map.of("class", normalized)));
    }

    private Component buildActions(String classId) {
        Component accept = LEGACY.deserialize(messages.raw("quests.backstory.accept"))
                .clickEvent(ClickEvent.runCommand("/class lore accept " + classId));
        Component skip = LEGACY.deserialize(messages.raw("quests.backstory.skip"))
                .clickEvent(ClickEvent.runCommand("/class lore skip " + classId));
        Component later = LEGACY.deserialize(messages.raw("quests.backstory.later"))
                .clickEvent(ClickEvent.runCommand("/class lore later " + classId));
        return accept
                .append(Component.newline())
                .append(skip)
                .append(Component.newline())
                .append(later);
    }

    private String normalize(String classId) {
        if (classId == null) {
            return null;
        }
        String trimmed = classId.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toUpperCase(Locale.ROOT);
    }
}
