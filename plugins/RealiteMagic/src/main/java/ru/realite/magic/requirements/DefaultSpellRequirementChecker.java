package ru.realite.magic.requirements;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.entity.Player;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.i18n.MagicMessages;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRequirements;

public final class DefaultSpellRequirementChecker implements SpellRequirementChecker {

    private static final PlainTextComponentSerializer PLAIN =
            PlainTextComponentSerializer.plainText();

    private final ItemsBridge itemsBridge;
    private final ClassesBridge classesBridge;
    private final MagicMessages messages;
    private final Runnable missingItemBridgeWarning;
    private final boolean failWhenItemsUnavailable;

    public DefaultSpellRequirementChecker(ItemsBridge itemsBridge,
                                          ClassesBridge classesBridge,
                                          MagicMessages messages,
                                          Runnable missingItemBridgeWarning,
                                          boolean failWhenItemsUnavailable) {
        this.itemsBridge = itemsBridge;
        this.classesBridge = classesBridge;
        this.messages = Objects.requireNonNull(messages, "messages");
        this.missingItemBridgeWarning = missingItemBridgeWarning;
        this.failWhenItemsUnavailable = failWhenItemsUnavailable;
    }

    @Override
    public CheckResult check(Player player, SpellDefinition spell) {
        if (player == null || spell == null) {
            return new CheckResult.Ok();
        }
        SpellRequirements requirements = spell.requirements();
        if (requirements == null || requirements.isEmpty()) {
            return new CheckResult.Ok();
        }
        Map<String, String> placeholders = new HashMap<>();
        placeholders.put("spell", messages.raw(spell.nameKey()));

        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            placeholders.put("class", PLAIN.serialize(classesBridge.displayClassName(classId)));
            if (!classesBridge.isAvailable()) {
                return new CheckResult.Fail(RequirementReasons.MISSING_CLASS_MODULE, placeholders);
            }
            String activeClassId = classesBridge.getActiveClassId(player);
            if (activeClassId == null || activeClassId.isBlank()) {
                return new CheckResult.Fail(RequirementReasons.NO_CLASS_SELECTED, placeholders);
            }
            if (!activeClassId.equalsIgnoreCase(classId)) {
                return new CheckResult.Fail(RequirementReasons.CLASS_MISMATCH, placeholders);
            }
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            placeholders.put("evolution", PLAIN.serialize(classesBridge.displayEvolutionName(evolutionId)));
            if (!classesBridge.isAvailable()) {
                return new CheckResult.Fail(RequirementReasons.MISSING_CLASS_MODULE, placeholders);
            }
            String activeEvolutionId = classesBridge.getActiveEvolutionId(player);
            if (activeEvolutionId == null || activeEvolutionId.isBlank()) {
                return new CheckResult.Fail(RequirementReasons.NO_EVOLUTION, placeholders);
            }
            if (!activeEvolutionId.equalsIgnoreCase(evolutionId)) {
                return new CheckResult.Fail(RequirementReasons.EVOLUTION_MISMATCH, placeholders);
            }
        }

        String requiredItemId = requirements.requiredItemId();
        if (requiredItemId != null && !requiredItemId.isBlank()) {
            placeholders.put("item", resolveItemName(requiredItemId));
            if (itemsBridge instanceof NoopItemsBridge) {
                if (failWhenItemsUnavailable) {
                    return new CheckResult.Fail(RequirementReasons.MISSING_ITEMS_MODULE, placeholders);
                }
                if (missingItemBridgeWarning != null) {
                    missingItemBridgeWarning.run();
                }
            } else if (!itemsBridge.hasItem(player, requiredItemId, 1)) {
                return new CheckResult.Fail(RequirementReasons.ITEM_MISSING, placeholders);
            }
        }
        return new CheckResult.Ok();
    }

    private String resolveItemName(String requiredItemId) {
        return PLAIN.serialize(itemsBridge.displayName(requiredItemId));
    }
}
