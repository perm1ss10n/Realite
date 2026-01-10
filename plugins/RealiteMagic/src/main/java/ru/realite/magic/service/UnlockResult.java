package ru.realite.magic.service;

public sealed interface UnlockResult permits UnlockResult.Ok, UnlockResult.Fail {

    record Ok(boolean alreadyLearned) implements UnlockResult {}

    record Fail(SpellActionReason reason) implements UnlockResult {}
}
