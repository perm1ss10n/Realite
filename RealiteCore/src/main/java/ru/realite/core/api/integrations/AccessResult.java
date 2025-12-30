package ru.realite.core.api.integrations;

public record AccessResult(AccessDecision decision, String reasonKey) {
    public static AccessResult allow() {
        return new AccessResult(AccessDecision.ALLOW, null);
    }

    public static AccessResult deny(String reasonKey) {
        return new AccessResult(AccessDecision.DENY, reasonKey);
    }

    public boolean isAllowed() {
        return decision == AccessDecision.ALLOW;
    }
}
