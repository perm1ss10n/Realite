package ru.realite.quests;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import ru.realite.core.api.Config;
import ru.realite.core.api.Module;
import ru.realite.core.api.ModuleContext;
import ru.realite.core.api.ModuleId;
import ru.realite.core.api.ModuleMetadata;
import ru.realite.core.api.Storage;
import ru.realite.core.api.Subscription;
import ru.realite.core.api.events.ClassLevelUpEvent;
import ru.realite.core.api.events.ClassSelectedEvent;
import ru.realite.core.api.events.EvolutionCompletedEvent;
import ru.realite.core.api.quests.ClassBackstoryService;
import ru.realite.core.api.quests.QuestService;
import ru.realite.core.api.quests.QuestUnlockService;
import ru.realite.quests.backstory.BackstoryProgressRepository;
import ru.realite.quests.backstory.ClassBackstoryConfig;
import ru.realite.quests.backstory.ClassBackstoryServiceImpl;
import ru.realite.quests.i18n.QuestsMessages;
import ru.realite.quests.service.QuestLoader;
import ru.realite.quests.service.QuestProgressRepository;
import ru.realite.quests.service.QuestRepository;
import ru.realite.quests.service.QuestServiceImpl;
import ru.realite.quests.service.QuestUnlockRepository;
import ru.realite.quests.service.QuestUnlockServiceImpl;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public final class QuestsModule implements Module {

    private final List<Subscription> subscriptions = new ArrayList<>();
    private Config config;
    private Config classesConfig;
    private QuestsMessages messages;
    private ClassBackstoryService backstoryService;
    private QuestService questService;
    private final ModuleMetadata metadata = new ModuleMetadata(
            new ModuleId("realite-quests"),
            "RealiteQuests",
            "0.1.0",
            Set.of()
    );

    @Override
    public ModuleMetadata metadata() {
        return metadata;
    }

    @Override
    public void onLoad(ModuleContext ctx) {
        config = ctx.configs().loadOrCreateDefault(
                ctx.dataFolder().resolve("config.yml"),
                "config.yml",
                getClass().getClassLoader()
        );

        classesConfig = ctx.configs().loadOrCreateDefault(
                ctx.dataFolder().resolve("classes.yml"),
                "classes.yml",
                getClass().getClassLoader()
        );
        ensureQuestDefaults(ctx);

        String lang = config.getString("lang", "ru");
        messages = new QuestsMessages(ctx, lang, getClass().getClassLoader());

        String title = config.getString("quests.title", "Realite Quests");
        int dailyLimit = config.getInt("quests.daily.limit", 3);
        boolean enabled = config.getBoolean("quests.enabled", true);
        List<String> tags = config.getStringList("quests.tags");

        ctx.logger().info("[Quests] Config loaded: title=" + title
                + ", enabled=" + enabled
                + ", dailyLimit=" + dailyLimit
                + ", tags=" + tags);
    }

    @Override
    public void onEnable(ModuleContext ctx) {
        Storage db = ctx.storage().openSqlite(ctx.dataFolder().resolve("data.sqlite"));
        try {
            Connection connection = db.connection();
            try (Statement statement = connection.createStatement()) {
                statement.execute("CREATE TABLE IF NOT EXISTS demo_kv (key TEXT PRIMARY KEY, value TEXT)");
            }

            try (PreparedStatement upsert = connection.prepareStatement(
                    "INSERT INTO demo_kv(key, value) VALUES(?, ?) "
                            + "ON CONFLICT(key) DO UPDATE SET value = excluded.value"
            )) {
                upsert.setString(1, "hello");
                upsert.setString(2, "world");
                upsert.executeUpdate();
            }

            try (PreparedStatement select = connection.prepareStatement(
                    "SELECT value FROM demo_kv WHERE key = ?"
            )) {
                select.setString(1, "hello");
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next()) {
                        ctx.logger().info("[Quests] Storage smoke: hello=" + rs.getString("value"));
                    } else {
                        ctx.logger().warn("[Quests] Storage smoke: no record for key 'hello'");
                    }
                }
            }
        } catch (SQLException e) {
            ctx.logger().error("[Quests] Storage smoke test failed", e);
        }

        QuestUnlockService questUnlockService = ctx.services().get(QuestUnlockService.class);
        if (questUnlockService == null) {
            QuestUnlockRepository unlockRepository = new QuestUnlockRepository(ctx.dataFolder());
            questUnlockService = new QuestUnlockServiceImpl(unlockRepository);
            ctx.services().register(QuestUnlockService.class, questUnlockService);
        }

        questService = ctx.services().get(QuestService.class);
        if (questService == null) {
            QuestRepository repository = new QuestLoader(ctx.dataFolder().resolve("quests"), ctx.logger()).load();
            QuestProgressRepository progressRepository = new QuestProgressRepository(ctx.dataFolder());
            questService = new QuestServiceImpl(ctx.logger(), ctx.events(), repository, progressRepository, questUnlockService);
            ctx.services().register(QuestService.class, questService);
        }

        ClassBackstoryService existingBackstory = ctx.services().get(ClassBackstoryService.class);
        if (existingBackstory != null) {
            backstoryService = existingBackstory;
        } else {
            BackstoryProgressRepository progressRepository = new BackstoryProgressRepository(ctx.dataFolder());
            ClassBackstoryConfig backstoryConfig = new ClassBackstoryConfig(classesConfig);
            backstoryService = new ClassBackstoryServiceImpl(
                    backstoryConfig,
                    progressRepository,
                    questService,
                    messages
            );
            ctx.services().register(ClassBackstoryService.class, backstoryService);
        }

        subscriptions.add(ctx.events().subscribe(ClassSelectedEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " selected class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: class selected " + event.classId());
            Player player = Bukkit.getPlayer(event.playerUuid());
            if (player != null) {
                backstoryService.show(player, event.classId(), false);
            }
        }));

        subscriptions.add(ctx.events().subscribe(ClassLevelUpEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " leveled class " + event.classId()
                    + " to " + event.newLevel());
            sendMessage(event.playerUuid(),
                    "Quest update: class " + event.classId() + " reached level " + event.newLevel());
        }));

        subscriptions.add(ctx.events().subscribe(EvolutionCompletedEvent.class, event -> {
            ctx.logger().info("[Quests] Player " + event.playerUuid()
                    + " completed evolution " + event.evolutionId()
                    + " for class " + event.classId());
            sendMessage(event.playerUuid(),
                    "Quest update: evolution completed " + event.evolutionId());
        }));
    }

    @Override
    public void onDisable(ModuleContext ctx) {
        for (Subscription subscription : subscriptions) {
            subscription.unsubscribe();
        }
        subscriptions.clear();
    }

    private void sendMessage(UUID playerUuid, String message) {
        Player player = Bukkit.getPlayer(playerUuid);
        if (player != null && player.isOnline()) {
            player.sendMessage(message);
        }
    }

    private void ensureQuestDefaults(ModuleContext ctx) {
        String[] defaultQuests = {
                "warrior_initiation.yml",
                "alchemist_initiation.yml",
                "miner_initiation.yml",
                "merchant_initiation.yml",
                "archer_initiation.yml",
                "wanderer_initiation.yml"
        };
        Path questsDir = ctx.dataFolder().resolve("quests");
        try {
            Files.createDirectories(questsDir);
        } catch (IOException e) {
            ctx.logger().error("[Quests] Failed to create quests directory", e);
            return;
        }
        ClassLoader loader = getClass().getClassLoader();
        for (String questFile : defaultQuests) {
            Path target = questsDir.resolve(questFile);
            if (Files.exists(target)) {
                continue;
            }
            try (InputStream input = loader.getResourceAsStream("quests/" + questFile)) {
                if (input == null) {
                    ctx.logger().warn("[Quests] Default quest resource missing: " + questFile);
                    continue;
                }
                Files.copy(input, target, StandardCopyOption.REPLACE_EXISTING);
                ctx.logger().info("[Quests] Saved default quest: " + questFile);
            } catch (IOException e) {
                ctx.logger().error("[Quests] Failed to save default quest " + questFile, e);
            }
        }
    }
}
