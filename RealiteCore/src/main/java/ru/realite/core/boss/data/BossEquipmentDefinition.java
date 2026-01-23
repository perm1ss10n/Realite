package ru.realite.core.boss.data;

public record BossEquipmentDefinition(
        String mainHand,
        String offHand,
        String helmet,
        String chestplate,
        String leggings,
        String boots
) {
    public BossEquipmentDefinition {
        mainHand = normalize(mainHand);
        offHand = normalize(offHand);
        helmet = normalize(helmet);
        chestplate = normalize(chestplate);
        leggings = normalize(leggings);
        boots = normalize(boots);
    }

    public static BossEquipmentDefinition empty() {
        return new BossEquipmentDefinition(null, null, null, null, null, null);
    }

    public boolean isEmpty() {
        return mainHand == null
                && offHand == null
                && helmet == null
                && chestplate == null
                && leggings == null
                && boots == null;
    }

    private static String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isBlank() ? null : trimmed;
    }
}
