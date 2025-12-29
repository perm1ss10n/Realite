package ru.realite.chat;

public final class RomanNumerals {

    private static final int[] VALUES = {
            1000, 900, 500, 400,
            100, 90, 50, 40,
            10, 9, 5, 4,
            1
    };

    private static final String[] SYMBOLS = {
            "M", "CM", "D", "CD",
            "C", "XC", "L", "XL",
            "X", "IX", "V", "IV",
            "I"
    };

    private RomanNumerals() {
    }

    public static String toRoman(int number) {
        int value = Math.max(number, 1);
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < VALUES.length; i++) {
            while (value >= VALUES[i]) {
                value -= VALUES[i];
                out.append(SYMBOLS[i]);
            }
        }
        return out.toString();
    }
}
