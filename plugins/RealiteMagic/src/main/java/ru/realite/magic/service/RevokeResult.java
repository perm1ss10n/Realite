package ru.realite.magic.service;

public sealed interface RevokeResult permits RevokeResult.Ok, RevokeResult.Fail {

    record Ok(boolean removed) implements RevokeResult {}

    record Fail(SpellActionReason reason) implements RevokeResult {}
}
