package ru.realite.magic.service;

public sealed interface SetSlotResult permits SetSlotResult.Ok, SetSlotResult.Fail {

    record Ok() implements SetSlotResult {}

    record Fail(String reasonKey) implements SetSlotResult {}
}
