package ru.realite.guilds.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Supplier;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;
import ru.realite.core.api.CoreApi;
import ru.realite.core.api.events.GuildUpgradePurchasedEvent;
import ru.realite.guilds.model.Guild;
import ru.realite.guilds.model.GuildMember;
import ru.realite.guilds.model.upgrade.UpgradeCost;
import ru.realite.guilds.model.upgrade.UpgradeDefinition;
import ru.realite.guilds.storage.GuildRepository;
import ru.realite.guilds.storage.GuildUpgradeConfigRepository;

public final class GuildUpgradeService {

    private final JavaPlugin plugin;
    private final GuildRepository repository;
    private final GuildRankService rankService;
    private final GuildUpgradeConfigRepository upgradeConfig;
    private final GuildTreasuryService treasuryService;
    private final Supplier<CoreApi> coreApiSupplier;

    public GuildUpgradeService(JavaPlugin plugin,
                               GuildRepository repository,
                               GuildRankService rankService,
                               GuildUpgradeConfigRepository upgradeConfig,
                               GuildTreasuryService treasuryService,
                               Supplier<CoreApi> coreApiSupplier) {
        this.plugin = Objects.requireNonNull(plugin, "plugin");
        this.repository = Objects.requireNonNull(repository, "repository");
        this.rankService = Objects.requireNonNull(rankService, "rankService");
        this.upgradeConfig = Objects.requireNonNull(upgradeConfig, "upgradeConfig");
        this.treasuryService = Objects.requireNonNull(treasuryService, "treasuryService");
        this.coreApiSupplier = Objects.requireNonNull(coreApiSupplier, "coreApiSupplier");
    }

    public PurchaseResult purchase(Player player, String upgradeId) {
        if (player == null || upgradeId == null || upgradeId.isBlank()) {
            return new PurchaseResult(PurchaseStatus.INVALID_REQUEST, 0.0d, 0, 0.0d);
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return new PurchaseResult(PurchaseStatus.NOT_IN_GUILD, 0.0d, 0, 0.0d);
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return new PurchaseResult(PurchaseStatus.GUILD_NOT_FOUND, 0.0d, 0, 0.0d);
        }
        String normalizedId = normalizeUpgradeId(upgradeId);
        UpgradeDefinition definition = upgradeConfig.getUpgrades().get(normalizedId);
        if (definition == null) {
            return new PurchaseResult(PurchaseStatus.UPGRADE_NOT_FOUND, 0.0d, 0, treasuryService.getBalance(guild.tag()));
        }
        if (!definition.enabled()) {
            return new PurchaseResult(PurchaseStatus.UPGRADE_DISABLED, 0.0d, repository.getUpgradeLevel(guild.tag(), normalizedId),
                    treasuryService.getBalance(guild.tag()));
        }
        if (upgradeConfig.getSettings().requirePermission()) {
            boolean hasPermission = rankService.hasPermission(member.role(), GuildRankPermission.TREASURY_SPEND)
                    || rankService.hasPermission(member.role(), GuildRankPermission.UPGRADES_MANAGE);
            if (!hasPermission) {
                return new PurchaseResult(PurchaseStatus.NO_PERMISSION, 0.0d, repository.getUpgradeLevel(guild.tag(), normalizedId),
                        treasuryService.getBalance(guild.tag()));
            }
        }
        if (!meetsRequirements(guild, definition)) {
            return new PurchaseResult(PurchaseStatus.REQUIREMENTS_NOT_MET, 0.0d, repository.getUpgradeLevel(guild.tag(), normalizedId),
                    treasuryService.getBalance(guild.tag()));
        }
        int currentLevel = repository.getUpgradeLevel(guild.tag(), normalizedId);
        if (currentLevel >= definition.maxLevel()) {
            return new PurchaseResult(PurchaseStatus.MAX_LEVEL, 0.0d, currentLevel, treasuryService.getBalance(guild.tag()));
        }
        int targetLevel = currentLevel + 1;
        double cost = resolveCost(definition, targetLevel);
        if (cost <= 0.0d) {
            return new PurchaseResult(PurchaseStatus.INVALID_COST, 0.0d, currentLevel, treasuryService.getBalance(guild.tag()));
        }
        double currentBalance = treasuryService.getBalance(guild.tag());
        if (!upgradeConfig.getSettings().allowNegative() && currentBalance < cost) {
            return new PurchaseResult(PurchaseStatus.INSUFFICIENT_FUNDS, cost, currentLevel, currentBalance);
        }
        double newBalance = treasuryService.withdraw(guild.tag(), cost,
                "upgrade:" + normalizedId + ":level:" + targetLevel,
                player.getUniqueId());
        treasuryService.logTransaction(guild.tag(), -cost,
                "upgrade:" + normalizedId + ":level:" + targetLevel,
                player.getUniqueId(),
                newBalance);
        repository.setUpgradeLevel(guild.tag(), normalizedId, targetLevel);
        publishEvent(player.getUniqueId(), guild.tag(), normalizedId, targetLevel, cost, newBalance);
        return new PurchaseResult(PurchaseStatus.SUCCESS, cost, targetLevel, newBalance);
    }

    public UpgradeListResult list(Player player) {
        if (player == null) {
            return new UpgradeListResult(UpgradeListStatus.INVALID_REQUEST, List.of());
        }
        GuildMember member = repository.getMember(player.getUniqueId());
        if (member == null) {
            return new UpgradeListResult(UpgradeListStatus.NOT_IN_GUILD, List.of());
        }
        Guild guild = repository.getGuild(member.tag());
        if (guild == null) {
            return new UpgradeListResult(UpgradeListStatus.GUILD_NOT_FOUND, List.of());
        }
        List<UpgradeEntry> entries = upgradeConfig.getUpgrades().values().stream()
                .sorted((a, b) -> a.id().compareToIgnoreCase(b.id()))
                .map(definition -> {
                    int currentLevel = repository.getUpgradeLevel(guild.tag(), definition.id());
                    boolean maxed = currentLevel >= definition.maxLevel();
                    double nextCost = maxed ? 0.0d : resolveCost(definition, currentLevel + 1);
                    return new UpgradeEntry(
                            definition.id(),
                            definition.name(),
                            definition.description(),
                            currentLevel,
                            definition.maxLevel(),
                            nextCost,
                            maxed);
                })
                .toList();
        return new UpgradeListResult(UpgradeListStatus.SUCCESS, entries);
    }

    private boolean meetsRequirements(Guild guild, UpgradeDefinition definition) {
        Map<String, Integer> requirements = definition.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return true;
        }
        for (Map.Entry<String, Integer> entry : requirements.entrySet()) {
            String key = entry.getKey();
            if (key == null) {
                continue;
            }
            if ("guildlevelatleast".equalsIgnoreCase(key)) {
                int required = entry.getValue() == null ? 0 : entry.getValue();
                if (guild.level() < required) {
                    return false;
                }
            }
        }
        return true;
    }

    private double resolveCost(UpgradeDefinition definition, int level) {
        UpgradeCost cost = definition.cost();
        if (cost instanceof UpgradeCost.Table table) {
            Double value = table.values().get(level);
            if (value == null) {
                plugin.getLogger().warning("Upgrade cost table missing level " + level + " for " + definition.id());
                return -1.0d;
            }
            return value;
        }
        if (cost instanceof UpgradeCost.Formula formula) {
            try {
                return evaluateFormula(formula.expression(), level);
            } catch (IllegalArgumentException ex) {
                plugin.getLogger().warning("Failed to evaluate upgrade cost formula for " + definition.id() + ": " + ex.getMessage());
                return -1.0d;
            }
        }
        return -1.0d;
    }

    private double evaluateFormula(String expression, int level) {
        List<Token> tokens = tokenize(expression);
        List<Token> output = new ArrayList<>();
        List<Token> operators = new ArrayList<>();
        for (Token token : tokens) {
            switch (token.type) {
                case NUMBER, VARIABLE -> output.add(token);
                case OPERATOR -> {
                    while (!operators.isEmpty()) {
                        Token top = operators.get(operators.size() - 1);
                        if (top.type != TokenType.OPERATOR) {
                            break;
                        }
                        int precTop = precedence(top.value);
                        int precCurrent = precedence(token.value);
                        if (precTop >= precCurrent) {
                            output.add(operators.remove(operators.size() - 1));
                        } else {
                            break;
                        }
                    }
                    operators.add(token);
                }
                case LEFT_PAREN -> operators.add(token);
                case RIGHT_PAREN -> {
                    boolean matched = false;
                    while (!operators.isEmpty()) {
                        Token top = operators.remove(operators.size() - 1);
                        if (top.type == TokenType.LEFT_PAREN) {
                            matched = true;
                            break;
                        }
                        output.add(top);
                    }
                    if (!matched) {
                        throw new IllegalArgumentException("Mismatched parentheses");
                    }
                }
                default -> throw new IllegalArgumentException("Unexpected token");
            }
        }
        while (!operators.isEmpty()) {
            Token top = operators.remove(operators.size() - 1);
            if (top.type == TokenType.LEFT_PAREN) {
                throw new IllegalArgumentException("Mismatched parentheses");
            }
            output.add(top);
        }
        List<Double> stack = new ArrayList<>();
        for (Token token : output) {
            if (token.type == TokenType.NUMBER) {
                stack.add(Double.parseDouble(token.value));
            } else if (token.type == TokenType.VARIABLE) {
                stack.add((double) level);
            } else if (token.type == TokenType.OPERATOR) {
                if (stack.size() < 2) {
                    throw new IllegalArgumentException("Invalid expression");
                }
                double right = stack.remove(stack.size() - 1);
                double left = stack.remove(stack.size() - 1);
                stack.add(applyOperator(token.value, left, right));
            }
        }
        if (stack.size() != 1) {
            throw new IllegalArgumentException("Invalid expression");
        }
        return stack.get(0);
    }

    private List<Token> tokenize(String expression) {
        List<Token> tokens = new ArrayList<>();
        String trimmed = expression == null ? "" : expression.trim();
        int index = 0;
        while (index < trimmed.length()) {
            char ch = trimmed.charAt(index);
            if (Character.isWhitespace(ch)) {
                index++;
                continue;
            }
            if (Character.isDigit(ch) || ch == '.') {
                int start = index;
                index++;
                while (index < trimmed.length()) {
                    char next = trimmed.charAt(index);
                    if (Character.isDigit(next) || next == '.') {
                        index++;
                    } else {
                        break;
                    }
                }
                tokens.add(new Token(TokenType.NUMBER, trimmed.substring(start, index)));
                continue;
            }
            if (Character.isLetter(ch)) {
                int start = index;
                index++;
                while (index < trimmed.length() && Character.isLetterOrDigit(trimmed.charAt(index))) {
                    index++;
                }
                String name = trimmed.substring(start, index).toLowerCase(Locale.ROOT);
                if (!"level".equals(name)) {
                    throw new IllegalArgumentException("Unknown variable: " + name);
                }
                tokens.add(new Token(TokenType.VARIABLE, name));
                continue;
            }
            switch (ch) {
                case '+', '-', '*', '/' -> {
                    tokens.add(new Token(TokenType.OPERATOR, String.valueOf(ch)));
                    index++;
                }
                case '(' -> {
                    tokens.add(new Token(TokenType.LEFT_PAREN, "("));
                    index++;
                }
                case ')' -> {
                    tokens.add(new Token(TokenType.RIGHT_PAREN, ")"));
                    index++;
                }
                default -> throw new IllegalArgumentException("Unexpected character: " + ch);
            }
        }
        return tokens;
    }

    private int precedence(String operator) {
        return ("*".equals(operator) || "/".equals(operator)) ? 2 : 1;
    }

    private double applyOperator(String operator, double left, double right) {
        return switch (operator) {
            case "+" -> left + right;
            case "-" -> left - right;
            case "*" -> left * right;
            case "/" -> left / right;
            default -> throw new IllegalArgumentException("Unknown operator: " + operator);
        };
    }

    private void publishEvent(UUID actor, String guildTag, String upgradeId, int level, double cost, double balanceAfter) {
        CoreApi core = coreApiSupplier.get();
        if (core == null) {
            return;
        }
        core.events().publish(new GuildUpgradePurchasedEvent(actor, guildTag, upgradeId, level, cost, balanceAfter));
    }

    private String normalizeUpgradeId(String upgradeId) {
        return upgradeId.trim().toLowerCase(Locale.ROOT);
    }

    private enum TokenType {
        NUMBER,
        VARIABLE,
        OPERATOR,
        LEFT_PAREN,
        RIGHT_PAREN
    }

    private record Token(TokenType type, String value) {
    }

    public record PurchaseResult(PurchaseStatus status, double cost, int newLevel, double balanceAfter) {
        public boolean isSuccess() {
            return status == PurchaseStatus.SUCCESS;
        }
    }

    public record UpgradeListResult(UpgradeListStatus status, List<UpgradeEntry> entries) {
    }

    public record UpgradeEntry(String id, String name, String description, int level, int maxLevel,
                               double nextCost, boolean maxed) {
    }

    public enum PurchaseStatus {
        SUCCESS,
        INVALID_REQUEST,
        NOT_IN_GUILD,
        GUILD_NOT_FOUND,
        UPGRADE_NOT_FOUND,
        UPGRADE_DISABLED,
        NO_PERMISSION,
        REQUIREMENTS_NOT_MET,
        MAX_LEVEL,
        INVALID_COST,
        INSUFFICIENT_FUNDS
    }

    public enum UpgradeListStatus {
        SUCCESS,
        INVALID_REQUEST,
        NOT_IN_GUILD,
        GUILD_NOT_FOUND
    }
}
