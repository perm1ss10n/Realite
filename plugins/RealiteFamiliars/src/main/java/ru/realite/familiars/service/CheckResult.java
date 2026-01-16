package ru.realite.familiars.service;

import java.util.List;

public record CheckResult(boolean allowed, List<String> reasons, List<String> notes) {
    public static CheckResult allowed(List<String> notes) {
        return new CheckResult(true, List.of(), List.copyOf(notes));
    }

    public static CheckResult denied(List<String> reasons) {
        return new CheckResult(false, List.copyOf(reasons), List.of());
    }
}
