package ru.realite.chat;

import io.papermc.paper.chat.ChatRenderer;
import io.papermc.paper.event.player.AsyncChatEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.Optional;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import net.luckperms.api.LuckPerms;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassTag;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.core.api.guilds.GuildChatBridge;
import ru.realite.core.api.guilds.GuildTagProvider;

public final class RealiteChatPlugin extends JavaPlugin implements Listener {

    private static final String DEFAULT_FORMAT = "{prefix}{class}{guild}{name}: {message}";

    /** Legacy (& / §) -> Component (используем только для строк из конфигов / LP meta) */
    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private PrefixProvider prefixProvider = player -> Optional.empty();
    private ClassTagProvider classTagProvider;
    private GuildTagProvider guildTagProvider;
    private GuildChatBridge guildChatBridge;

    private ChatMessages messages;
    private ChatFormat chatFormat;

    private String tagsJoiner = "";
    private boolean spaceBeforeName = true;

    private boolean prefixEnabled = true;
    private boolean classEnabled = true;
    private boolean guildEnabled = true;

    private boolean classHoverEnabled = true;
    private boolean classRomanEnabled = true;
    private boolean guildHoverEnabled = true;

    /** debug флаг из конфига (логируем только если включено) */
    private boolean debug = false;

    private boolean luckPermsMissingLogged = false;

    /** Один стабильный renderer, чтобы его нельзя было “потерять” и можно было проверять переопределение */
    private final ChatRenderer OUR_RENDERER = (source, sourceDisplayName, message, viewer) -> {
        Component lpPrefix = prefixEnabled
                ? prefixProvider.getPrefix(source).orElse(Component.empty())
                : Component.empty();

        Component classTag = classEnabled ? buildClassTagComponent(source) : Component.empty();
        Component guildTag = guildEnabled ? buildGuildTagComponent(source) : Component.empty();

        Component name = (sourceDisplayName == null) ? Component.text(source.getName()) : sourceDisplayName;

        // НИКАКИХ логов на каждый чат в проде
        return chatFormat.render(new ChatFormat.Context(
                lpPrefix,
                classTag,
                guildTag,
                name,
                message,
                tagsJoiner,
                spaceBeforeName));
    };

    @Override
    public void onEnable() {
        saveDefaultConfig();
        saveIfNotExists("lang/messages_ru.yml");
        saveIfNotExists("lang/messages_en.yml");

        reloadConfig();

        String language = resolveLanguage();
        messages = new ChatMessages(this, language);

        prefixProvider = resolvePrefixProvider();
        classTagProvider = resolveClassTagProvider();
        guildTagProvider = resolveGuildTagProvider();
        guildChatBridge = resolveGuildChatBridge();

        reloadAll();

        getServer().getPluginManager().registerEvents(this, this);

        var cmd = getCommand("realitechat");
        if (cmd != null) {
            RealiteChatCommand handler = new RealiteChatCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }

        var guildChat = getCommand("gc");
        if (guildChat != null) {
            guildChat.setExecutor(new GuildChatMessageCommand(this));
        }

        if (debug) {
            logDependencyDebug();
        }
    }

    ChatMessages getMessages() {
        return messages;
    }

    boolean hasReloadPermission(CommandSender sender) {
        return sender.hasPermission("realite.chat.admin") || sender.hasPermission("realite.chat.reload");
    }

    /**
     * Ставим renderer максимально поздно.
     * (MONITOR используем только для debug-проверки перетирания)
     */
    @EventHandler(priority = EventPriority.HIGHEST, ignoreCancelled = true)
    public void onAsyncChat(AsyncChatEvent event) {
        event.renderer(OUR_RENDERER);
    }

    /** Debug: если кто-то перетёр renderer после нас — узнаем кто */
    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onAsyncChatMonitor(AsyncChatEvent event) {
        if (!debug) return;

        if (event.renderer() != OUR_RENDERER) {
            getLogger().warning("[debug] ChatRenderer overridden by: " + event.renderer().getClass().getName());
        }
    }

    private PrefixProvider resolvePrefixProvider() {
        RegisteredServiceProvider<LuckPerms> provider =
                getServer().getServicesManager().getRegistration(LuckPerms.class);

        if (provider == null) {
            if (messages != null && !luckPermsMissingLogged) {
                getLogger().warning(PlainTextComponentSerializer.plainText()
                        .serialize(messages.get("chat.dependency.luckperms-missing")));
                luckPermsMissingLogged = true;
            }
            return player -> Optional.empty();
        }

        luckPermsMissingLogged = false;
        return new LuckPermsPrefixProvider(provider.getProvider());
    }

    private ClassTagProvider resolveClassTagProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) return null;

        CoreApi core = provider.getProvider();
        return core.services().get(ClassTagProvider.class);
    }

    private GuildTagProvider resolveGuildTagProvider() {
        RegisteredServiceProvider<CoreApi> provider =
                Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) return null;

        CoreApi core = provider.getProvider();
        return core.services().get(GuildTagProvider.class);
    }

    private GuildChatBridge resolveGuildChatBridge() {
        RegisteredServiceProvider<GuildChatBridge> provider =
                Bukkit.getServicesManager().getRegistration(GuildChatBridge.class);
        if (provider == null) return null;
        return provider.getProvider();
    }

    private Component buildClassTagComponent(Player player) {
        ClassTagProvider provider = (classTagProvider != null) ? classTagProvider : resolveClassTagProvider();

        // fallback из конфига (используем когда provider отсутствует или tag == null)
        String fallback = getConfig().getString("chat.class-tag", "");
        Component fallbackComponent = parseLegacy(fallback);

        if (provider == null) {
            if (debug) getLogger().info("[debug] ClassTagProvider missing -> fallback");
            return fallbackComponent;
        }

        classTagProvider = provider;

        ClassTag tag = provider.getTag(player);

        if (debug) {
            getLogger().info("[debug] classProvider=" + provider.getClass().getName()
                    + " player=" + player.getName()
                    + " tagNull=" + (tag == null)
                    + (tag != null ? (" displayName='" + tag.displayName() + "' stage=" + tag.evolutionStage()) : ""));
        }

        if (tag == null) {
            return fallbackComponent;
        }

        String stage = classRomanEnabled
                ? RomanNumerals.toRoman(tag.evolutionStage())
                : String.valueOf(tag.evolutionStage());

        Component base = Component.text("[")
                .append(parseLegacy(tag.displayName()))
                .append(Component.text("-" + stage + "]"));

        if (classHoverEnabled) {
            Component hover = Component.text("Класс: ")
                    .append(parseLegacy(tag.displayName()))
                    .append(Component.newline())
                    .append(Component.text("Этап: " + stage));
            return base.hoverEvent(HoverEvent.showText(hover));
        }

        return base;
    }

    private Component buildGuildTagComponent(Player player) {
        GuildTagProvider provider = (guildTagProvider != null) ? guildTagProvider : resolveGuildTagProvider();

        if (provider == null) {
            if (debug) getLogger().info("[debug] GuildTagProvider missing");
            return Component.empty();
        }

        guildTagProvider = provider;

        Optional<Component> tag = provider.getTag(player);
        if (tag.isEmpty()) {
            return Component.empty();
        }

        Component base = tag.get();
        if (guildHoverEnabled) {
            Optional<Component> hover = provider.getHover(player);
            if (hover.isPresent()) {
                base = base.hoverEvent(HoverEvent.showText(hover.get()));
            }
        }
        return base;
    }

    GuildChatBridge getGuildChatBridge() {
        return guildChatBridge;
    }

    void sendGuildChat(Player sender, Component message) {
        GuildChatBridge bridge = guildChatBridge;
        if (bridge == null) {
            return;
        }

        Component formatted = bridge.format(sender, message);
        if (formatted.equals(Component.empty())) {
            return;
        }

        List<Player> recipients = bridge.getGuildRecipients(sender);
        Set<UUID> sent = new HashSet<>();
        for (Player recipient : recipients) {
            sent.add(recipient.getUniqueId());
            recipient.sendMessage(formatted);
        }

        if (bridge.isSpyEnabled()) {
            for (Player spy : bridge.getSpyRecipients(sender)) {
                if (sent.add(spy.getUniqueId())) {
                    spy.sendMessage(formatted);
                }
            }
        }
    }

    public void reloadAll() {
        reloadConfig();

        String language = resolveLanguage();

        // debug флаг
        debug = getConfig().getBoolean("debug", false);

        tagsJoiner = getConfig().getString("chat.tags.joiner", "");
        spaceBeforeName = getConfig().getBoolean("chat.spaceBeforeName", true);

        prefixProvider = resolvePrefixProvider();

        prefixEnabled = getConfig().getBoolean("prefix.enabled", true);
        classEnabled = getConfig().getBoolean("class.enabled", true);
        guildEnabled = getConfig().getBoolean("guild.enabled", true);

        guildHoverEnabled = getConfig().getBoolean("guild.hover.enabled", true);
        classHoverEnabled = getConfig().getBoolean("class.hover.enabled", true);
        classRomanEnabled = getConfig().getBoolean("class.roman.enabled", true);

        String template = getConfig().getString("chat.format", DEFAULT_FORMAT);
        chatFormat = new ChatFormat(template);

        messages.reload(language);

        if (debug) {
            getLogger().info("[debug] chat.format = " + template);
            getLogger().info("[debug] enabled: prefix=" + prefixEnabled + " class=" + classEnabled + " guild=" + guildEnabled);
            logDependencyDebug();
        }
    }

    private void logDependencyDebug() {
        var coreReg = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        getLogger().info("[debug] CoreApi registered in Bukkit Services: " + (coreReg != null));
        if (coreReg != null) {
            CoreApi core = coreReg.getProvider();
            getLogger().info("[debug] Core services ClassTagProvider: " + (core.services().get(ClassTagProvider.class) != null));
            getLogger().info("[debug] Core services GuildTagProvider: " + (core.services().get(GuildTagProvider.class) != null));
        }

        var lpReg = Bukkit.getServicesManager().getRegistration(LuckPerms.class);
        getLogger().info("[debug] LuckPerms registered in Bukkit Services: " + (lpReg != null));
    }

    private String resolveLanguage() {
        String language = getConfig().getString("language");
        if (language == null || language.isBlank()) {
            language = getConfig().getString("lang", "ru");
        }
        return language;
    }

    private void saveIfNotExists(String resourcePath) {
        if (!getDataFolder().exists() && !getDataFolder().mkdirs()) {
            getLogger().warning("Failed to create plugin data folder: " + getDataFolder());
            return;
        }

        if (getResource(resourcePath) == null) {
            return;
        }

        if (!new java.io.File(getDataFolder(), resourcePath).exists()) {
            saveResource(resourcePath, false);
        }
    }

    private static Component parseLegacy(String text) {
        if (text == null || text.isEmpty()) return Component.empty();
        return LEGACY.deserialize(text.replace('§', '&'));
    }
}
