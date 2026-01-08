package ru.realite.magic.service;

public sealed interface SelectResult permits SelectResult.Ok, SelectResult.Fail {

    record Ok() implements SelectResult {}

    record Fail(SpellActionReason reason) implements SelectResult {}
}
