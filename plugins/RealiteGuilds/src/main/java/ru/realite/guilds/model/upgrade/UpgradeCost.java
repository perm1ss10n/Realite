package ru.realite.guilds.model.upgrade;

import java.util.Map;

public sealed interface UpgradeCost permits UpgradeCost.Formula, UpgradeCost.Table {

    record Formula(String expression) implements UpgradeCost {}

    record Table(Map<Integer, Double> values) implements UpgradeCost {}
}
