package ru.realite.magic.effect;

import java.util.Map;

public final class EffectParamUtils {

    private EffectParamUtils() {
    }

    public static String stringParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        return value;
    }

    public static Double doubleParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Integer intParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Number number) {
            return number.intValue();
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    public static Boolean booleanParam(Map<String, Object> params, String key) {
        Object raw = params.get(key);
        if (raw == null) {
            return null;
        }
        if (raw instanceof Boolean bool) {
            return bool;
        }
        String value = String.valueOf(raw).trim();
        if (value.isBlank()) {
            return null;
        }
        if ("true".equalsIgnoreCase(value)) {
            return Boolean.TRUE;
        }
        if ("false".equalsIgnoreCase(value)) {
            return Boolean.FALSE;
        }
        return null;
    }
}
