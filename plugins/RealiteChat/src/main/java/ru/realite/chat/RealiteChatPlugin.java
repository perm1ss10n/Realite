package ru.realite.chat;

import io.papermc.paper.event.player.AsyncChatEvent;
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
import org.bukkit.event.Listener;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.classes.ClassTag;
import ru.realite.core.api.classes.ClassTagProvider;
import ru.realite.core.api.guilds.GuildTagProvider;

public final class RealiteChatPlugin extends JavaPlugin implements Listener {

    private static final String DEFAULT_FORMAT = "{prefix}{class}{guild}{name}: {message}";

    private PrefixProvider prefixProvider = player -> Optional.empty();
    private ClassTagProvider classTagProvider;
    private GuildTagProvider guildTagProvider;

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

    private boolean luckPermsMissingLogged = false;

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

        reloadAll();

        getServer().getPluginManager().registerEvents(this, this);

        var cmd = getCommand("realitechat");
        if (cmd != null) {
            RealiteChatCommand handler = new RealiteChatCommand(this);
            cmd.setExecutor(handler);
            cmd.setTabCompleter(handler);
        }
    }

    ChatMessages getMessages() {
        return messages;
    }

    boolean hasReloadPermission(CommandSender sender) {
        return sender.hasPermission("realite.chat.admin") || sender.hasPermission("realite.chat.reload");
    }

    @EventHandler
    public void onAsyncChat(AsyncChatEvent event) {
        event.renderer((source, sourceDisplayName, message, viewer) -> {
            Component luckPermsPrefix = prefixEnabled
                    ? prefixProvider.getPrefix(source).orElse(Component.empty())
                    : Component.empty();

            Component classTagComponent = classEnabled ? buildClassTagComponent(source) : Component.empty();
            Component guildTag = guildEnabled ? buildGuildTagComponent(source) : Component.empty();

            return chatFormat.render(new ChatFormat.Context(
                    luckPermsPrefix,
                    classTagComponent,
                    guildTag,
                    sourceDisplayName,
                    message,
                    tagsJoiner,
                    spaceBeforeName));
        });
    }

    private PrefixProvider resolvePrefixProvider() {
        RegisteredServiceProvider<LuckPerms> provider = getServer().getServicesManager()
                .getRegistration(LuckPerms.class);

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
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) return null;

        CoreApi core = provider.getProvider();
        return core.services().get(ClassTagProvider.class);
    }

    private GuildTagProvider resolveGuildTagProvider() {
        RegisteredServiceProvider<CoreApi> provider = Bukkit.getServicesManager().getRegistration(CoreApi.class);
        if (provider == null) return null;

        CoreApi core = provider.getProvider();
        return core.services().get(GuildTagProvider.class);
    }

    private Component buildClassTagComponent(Player player) {
        ClassTagProvider provider = classTagProvider != null ? classTagProvider : resolveClassTagProvider();

        if (provider != null) {
            classTagProvider = provider;

            ClassTag tag = provider.getTag(player);
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

        String classTag = getConfig().getString("chat.class-tag", "[Бродяга-I]");
        return parseLegacy(classTag);
    }

    private Component buildGuildTagComponent(Player player) {
        GuildTagProvider provider = guildTagProvider != null ? guildTagProvider : resolveGuildTagProvider();

        if (provider != null) {
            guildTagProvider = provider;

            Optional<Component> tag = provider.getTag(player);
            if (tag.isEmpty()) return Component.empty();

            Component base = tag.get();
            if (guildHoverEnabled) {
                Optional<Component> hover = provider.getHover(player);
                if (hover.isPresent()) {
                    base = base.hoverEvent(HoverEvent.showText(hover.get()));
                }
            }
            return base;
        }

        return Component.empty();
    }

    public void reloadAll() {
        reloadConfig();

        String language = resolveLanguage();

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

    private Component parseLegacy(String text) {
        if (text == null || text.isEmpty()) {
            return Component.empty();
        }
        return LegacyComponentSerializer.legacyAmpersand().deserialize(text.replace('§', '&'));
    }
}
