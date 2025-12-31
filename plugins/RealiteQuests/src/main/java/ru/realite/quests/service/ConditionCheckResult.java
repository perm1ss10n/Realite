package ru.realite.quests.service;

public record ConditionCheckResult(boolean allowed, String reasonKey) {

    public static ConditionCheckResult allow() {
        return new ConditionCheckResult(true, null);
    }

    public static ConditionCheckResult deny(String reasonKey) {
        return new ConditionCheckResult(false, reasonKey);
    }
}
