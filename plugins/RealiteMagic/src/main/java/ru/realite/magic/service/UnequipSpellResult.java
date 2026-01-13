package ru.realite.magic.service;

public sealed interface UnequipSpellResult permits UnequipSpellResult.Ok, UnequipSpellResult.Fail {

    record Ok(int removedSlots) implements UnequipSpellResult {}

    record Fail(EquipSpellFailure reason) implements UnequipSpellResult {}
}
