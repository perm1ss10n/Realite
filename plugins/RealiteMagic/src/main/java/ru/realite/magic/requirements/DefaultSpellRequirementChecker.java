package ru.realite.magic.requirements;

import java.util.HashMap;
import java.util.Map;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.bukkit.entity.Player;
import ru.realite.magic.integration.classes.ClassesBridge;
import ru.realite.magic.integration.classes.CoreClassesBridge;
import ru.realite.magic.integration.classes.NoopClassesBridge;
import ru.realite.magic.integration.items.ItemsBridge;
import ru.realite.magic.integration.items.NoopItemsBridge;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRequirements;

public final class DefaultSpellRequirementChecker implements SpellRequirementChecker {

    private static final LegacyComponentSerializer LEGACY =
            LegacyComponentSerializer.legacyAmpersand();

    private final ItemsBridge itemsBridge;
    private final ClassesBridge classesBridge;
    private final Runnable missingItemBridgeWarning;

    public DefaultSpellRequirementChecker(ItemsBridge itemsBridge,
                                          ClassesBridge classesBridge,
                                          Runnable missingItemBridgeWarning) {
        this.itemsBridge = itemsBridge;
        this.classesBridge = classesBridge;
        this.missingItemBridgeWarning = missingItemBridgeWarning;
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
        placeholders.put("spell", spell.id());

        String requiredItemId = requirements.requiredItemId();
        if (requiredItemId != null && !requiredItemId.isBlank()) {
            placeholders.put("item", resolveItemName(requiredItemId));
            if (itemsBridge instanceof NoopItemsBridge) {
                if (missingItemBridgeWarning != null) {
                    missingItemBridgeWarning.run();
                }
            } else if (!itemsBridge.hasItem(player, requiredItemId, 1)) {
                return new CheckResult.Fail(RequirementReasons.ITEM_MISSING, placeholders);
            }
        }

        String classId = requirements.classId();
        if (classId != null && !classId.isBlank()) {
            placeholders.put("class", LEGACY.serialize(classesBridge.displayClassName(classId)));
        }
        String evolutionId = requirements.evolutionId();
        if (evolutionId != null && !evolutionId.isBlank()) {
            placeholders.put("evolution", LEGACY.serialize(classesBridge.displayEvolutionName(evolutionId)));
        }

        if ((classId != null && !classId.isBlank())
                || (evolutionId != null && !evolutionId.isBlank())) {
            if (classesBridge instanceof NoopClassesBridge) {
                return new CheckResult.Fail(RequirementReasons.MISSING_CLASS_MODULE, placeholders);
            }
            if (classesBridge instanceof CoreClassesBridge coreBridge && !coreBridge.isAvailable()) {
                return new CheckResult.Fail(RequirementReasons.MISSING_CLASS_MODULE, placeholders);
            }
            String activeClassId = classesBridge.getActiveClassId(player);
            if (classId != null && !classId.isBlank()) {
                if (activeClassId == null || !activeClassId.equalsIgnoreCase(classId)) {
                    return new CheckResult.Fail(RequirementReasons.CLASS_MISMATCH, placeholders);
                }
            }
            String activeEvolutionId = classesBridge.getActiveEvolutionId(player);
            if (evolutionId != null && !evolutionId.isBlank()) {
                if (activeEvolutionId == null || !activeEvolutionId.equalsIgnoreCase(evolutionId)) {
                    return new CheckResult.Fail(RequirementReasons.EVOLUTION_MISSING, placeholders);
                }
            }
        }
        return new CheckResult.Ok();
    }

    private String resolveItemName(String requiredItemId) {
        return LEGACY.serialize(itemsBridge.displayName(requiredItemId));
    }
}
