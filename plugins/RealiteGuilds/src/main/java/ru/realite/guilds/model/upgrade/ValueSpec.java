package ru.realite.guilds.model.upgrade;

import java.util.Map;

public sealed interface ValueSpec permits ValueSpec.Linear, ValueSpec.Table {

    record Linear(double base, double perLevel) implements ValueSpec {}

    record Table(Map<Integer, Double> values) implements ValueSpec {}
}
