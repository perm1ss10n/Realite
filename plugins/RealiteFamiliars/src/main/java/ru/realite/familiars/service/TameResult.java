package ru.realite.familiars.service;

import ru.realite.familiars.model.FamiliarInstance;

import java.util.Objects;

public record TameResult(CheckResult result, FamiliarInstance instance) {
    public TameResult {
        Objects.requireNonNull(result, "result");
    }

    public boolean allowed() {
        return result.allowed();
    }
}
