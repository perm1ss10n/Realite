package ru.realite.chat;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

import java.util.ArrayList;
import java.util.List;

final class ChatFormat {

    private static final LegacyComponentSerializer LEGACY = LegacyComponentSerializer.legacyAmpersand();

    private final List<Token> tokens;

    ChatFormat(String template) {
        this.tokens = parse(template == null ? "" : template);
    }

    private static Component legacy(String text) {
        if (text == null || text.isEmpty())
            return Component.empty();
        // поддержка и & и § в конфиге
        return LEGACY.deserialize(text.replace('§', '&'));
    }

    Component render(Context context) {
        Component result = Component.empty();
        boolean lastTagRendered = false;
        boolean lastTokenWasTag = false;
        boolean lastAppendedSpace = false;

        for (Token token : tokens) {
            if (token instanceof TagToken tagToken) {
                Component rendered = tagToken.render(context);
                if (!isEmpty(rendered)) {
                    if (lastTagRendered && !context.joiner().isEmpty()) {
                        AppendResult appended = appendText(result, context.joiner(), lastAppendedSpace);
                        result = appended.component();
                        lastAppendedSpace = appended.lastSpace();
                    }
                    result = result.append(rendered);
                    lastTagRendered = true;
                    lastAppendedSpace = false;
                }
                lastTokenWasTag = true;
                continue;
            }

            if (token instanceof OptionalGuildToken optionalGuildToken) {
                Component rendered = optionalGuildToken.render(context);
                if (!isEmpty(rendered)) {
                    if (lastTagRendered && !context.joiner().isEmpty()) {
                        AppendResult appended = appendText(result, context.joiner(), lastAppendedSpace);
                        result = appended.component();
                        lastAppendedSpace = appended.lastSpace();
                    }
                    result = result.append(rendered);
                    lastTagRendered = true;
                    lastAppendedSpace = false;
                }
                lastTokenWasTag = true;
                continue;
            }

            if (token instanceof NameToken nameToken) {
                if (context.spaceBeforeName() && lastTagRendered) {
                    AppendResult appended = appendText(result, " ", lastAppendedSpace);
                    result = appended.component();
                    lastAppendedSpace = appended.lastSpace();
                }
                result = result.append(nameToken.render(context));
                lastTagRendered = false;
                lastTokenWasTag = false;
                lastAppendedSpace = false;
                continue;
            }

            if (token instanceof LiteralToken literalToken) {
                String value = literalToken.value();
                if (value.isBlank() && lastTokenWasTag && !lastTagRendered) {
                    // если после тега пустые пробелы, и тег не отрендерился — пропускаем
                    lastTokenWasTag = false;
                    continue;
                }
                AppendResult appended = appendText(result, value, lastAppendedSpace);
                result = appended.component();
                lastAppendedSpace = appended.lastSpace();
                lastTagRendered = false;
                lastTokenWasTag = false;
                continue;
            }

            result = result.append(token.render(context));
            lastTagRendered = false;
            lastTokenWasTag = false;
            lastAppendedSpace = false;
        }

        return result;
    }

    private List<Token> parse(String template) {
        List<Token> parsed = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;

        while (i < template.length()) {
            char ch = template.charAt(i);

            // специальный синтаксис: [{guild}] — блок, который исчезает целиком если
            // гильдии нет
            if (ch == '[') {
                int blockEnd = template.indexOf(']', i + 1);
                if (blockEnd != -1) {
                    String block = template.substring(i + 1, blockEnd).trim();
                    if (block.startsWith("{") && block.endsWith("}")) {
                        String placeholder = block.substring(1, block.length() - 1).trim();
                        if ("guild".equals(placeholder)) {
                            if (!literal.isEmpty()) {
                                parsed.add(new LiteralToken(literal.toString()));
                                literal.setLength(0);
                            }
                            parsed.add(new OptionalGuildToken());
                            i = blockEnd + 1;
                            continue;
                        }
                    }
                }
            }

            // обычные плейсхолдеры
            if (ch == '{') {
                int end = template.indexOf('}', i + 1);
                if (end == -1) {
                    literal.append(ch);
                    i++;
                    continue;
                }
                if (!literal.isEmpty()) {
                    parsed.add(new LiteralToken(literal.toString()));
                    literal.setLength(0);
                }
                String placeholder = template.substring(i + 1, end).trim();
                parsed.add(tokenFor(placeholder));
                i = end + 1;
                continue;
            }

            literal.append(ch);
            i++;
        }

        if (!literal.isEmpty()) {
            parsed.add(new LiteralToken(literal.toString()));
        }

        return parsed;
    }

    private Token tokenFor(String placeholder) {
        return switch (placeholder) {
            case "prefix" -> new TagToken(Context::prefix);
            case "class" -> new TagToken(Context::classTag);
            case "guild" -> new TagToken(Context::guild);
            case "guildRank" -> new TagToken(Context::guildRank);

            case "name" -> new NameToken(Context::name);
            case "message" -> new ComponentToken(Context::message);
            default -> new LiteralToken("{" + placeholder + "}");
        };
    }

    interface Token {
        Component render(Context context);
    }

    private record LiteralToken(String value) implements Token {
        @Override
        public Component render(Context context) {
            return legacy(value);
        }
    }

    private record ComponentToken(ComponentResolver resolver) implements Token {
        @Override
        public Component render(Context context) {
            return resolver.resolve(context);
        }
    }

    private record TagToken(ComponentResolver resolver) implements Token {
        @Override
        public Component render(Context context) {
            return resolver.resolve(context);
        }
    }

    private record OptionalGuildToken() implements Token {
        @Override
        public Component render(Context context) {
            Component guild = context.guild();
            if (guild.equals(Component.empty())) {
                return Component.empty();
            }
            return legacy("[")
                    .append(guild)
                    .append(legacy("]"));
        }
    }

    private record NameToken(ComponentResolver resolver) implements Token {
        @Override
        public Component render(Context context) {
            return resolver.resolve(context);
        }
    }

    interface ComponentResolver {
        Component resolve(Context context);
    }

    static final class Context {
        private final Component prefix;
        private final Component classTag;
        private final Component guild;

        private final Component guildRank;

        private final Component name;
        private final Component message;
        private final String joiner;
        private final boolean spaceBeforeName;

        Context(
                Component prefix,
                Component classTag,
                Component guild,
                Component guildRank,
                Component name,
                Component message,
                String joiner,
                boolean spaceBeforeName) {
            this.prefix = prefix == null ? Component.empty() : prefix;
            this.classTag = classTag == null ? Component.empty() : classTag;
            this.guild = guild == null ? Component.empty() : guild;

            this.guildRank = guildRank == null ? Component.empty() : guildRank;

            this.name = name == null ? Component.empty() : name;
            this.message = message == null ? Component.empty() : message;
            this.joiner = joiner == null ? "" : joiner;
            this.spaceBeforeName = spaceBeforeName;
        }

        Component prefix() {
            return prefix;
        }

        Component classTag() {
            return classTag;
        }

        Component guild() {
            return guild;
        }

        Component guildRank() {
            return guildRank;
        }

        Component name() {
            return name;
        }

        Component message() {
            return message;
        }

        String joiner() {
            return joiner;
        }

        boolean spaceBeforeName() {
            return spaceBeforeName;
        }
    }

    private boolean isEmpty(Component component) {
        return component.equals(Component.empty());
    }

    private static AppendResult appendText(Component base, String text, boolean lastSpace) {
        if (text == null || text.isEmpty()) {
            return new AppendResult(base, lastSpace);
        }
        String normalized = lastSpace ? trimLeadingSpaces(text) : text;
        if (normalized.isEmpty()) {
            return new AppendResult(base, lastSpace);
        }

        Component next = base.append(legacy(normalized));

        boolean endsWithSpace = normalized.endsWith(" ");
        return new AppendResult(next, endsWithSpace);
    }

    private static String trimLeadingSpaces(String text) {
        int index = 0;
        while (index < text.length() && text.charAt(index) == ' ') {
            index++;
        }
        return text.substring(index);
    }

    private record AppendResult(Component component, boolean lastSpace) {
    }
}
