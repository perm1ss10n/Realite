package ru.realite.magic.service;

import javax.annotation.Nullable;
import ru.realite.magic.requirements.CheckResult;

public sealed interface EquipSpellResult permits EquipSpellResult.Ok, EquipSpellResult.Fail {

    record Ok(int slot) implements EquipSpellResult {}

    record Fail(EquipSpellFailure reason, @Nullable CheckResult.Fail requirement) implements EquipSpellResult {}
}
