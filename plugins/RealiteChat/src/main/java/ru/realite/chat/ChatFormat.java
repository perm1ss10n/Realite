package ru.realite.chat;

import java.util.ArrayList;
import java.util.List;
import net.kyori.adventure.text.Component;

final class ChatFormat {
    private final List<Token> tokens;

    ChatFormat(String template) {
        this.tokens = parse(template == null ? "" : template);
    }

    Component render(Context context) {
        Component result = Component.empty();
        boolean lastTagRendered = false;
        for (Token token : tokens) {
            if (token instanceof TagToken tagToken) {
                Component rendered = tagToken.render(context);
                if (!isEmpty(rendered)) {
                    if (lastTagRendered && !context.joiner().isEmpty()) {
                        result = result.append(Component.text(context.joiner()));
                    }
                    result = result.append(rendered);
                    lastTagRendered = true;
                }
                continue;
            }
            if (token instanceof NameToken nameToken) {
                if (context.spaceBeforeName() && lastTagRendered) {
                    result = result.append(Component.text(" "));
                }
                result = result.append(nameToken.render(context));
                lastTagRendered = false;
                continue;
            }
            result = result.append(token.render(context));
            lastTagRendered = false;
        }
        return result;
    }

    private List<Token> parse(String template) {
        List<Token> parsed = new ArrayList<>();
        StringBuilder literal = new StringBuilder();
        int i = 0;
        while (i < template.length()) {
            char ch = template.charAt(i);
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
            case "prefix" -> new TagToken(context -> context.prefix());
            case "class" -> new TagToken(context -> context.classTag());
            case "guild" -> new TagToken(context -> context.guild());
            case "name" -> new NameToken(context -> context.name());
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
            return Component.text(value);
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
        private final Component name;
        private final Component message;
        private final String joiner;
        private final boolean spaceBeforeName;

        Context(Component prefix,
                Component classTag,
                Component guild,
                Component name,
                Component message,
                String joiner,
                boolean spaceBeforeName) {
            this.prefix = prefix;
            this.classTag = classTag;
            this.guild = guild;
            this.name = name;
            this.message = message;
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
}
