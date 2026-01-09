package ru.realite.magic.service;

public sealed interface SetActiveSlotResult permits SetActiveSlotResult.Ok, SetActiveSlotResult.Fail {

    record Ok() implements SetActiveSlotResult {}

    record Fail(String reasonKey) implements SetActiveSlotResult {}
}
