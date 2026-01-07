package ru.realite.magic.model;

import java.util.HashMap;
import java.util.Map;

public final class MageState {

    private double mana;
    private double maxMana;
    private final Map<String, Long> cooldowns = new HashMap<>();
    private long lastCombatTime;
    private String activeSpellId;

    public MageState(double mana, double maxMana) {
        this.mana = mana;
        this.maxMana = maxMana;
    }

    public double mana() {
        return mana;
    }

    public void mana(double mana) {
        this.mana = mana;
    }

    public double maxMana() {
        return maxMana;
    }

    public void maxMana(double maxMana) {
        this.maxMana = maxMana;
    }

    public Map<String, Long> cooldowns() {
        return cooldowns;
    }

    public long lastCombatTime() {
        return lastCombatTime;
    }

    public void lastCombatTime(long lastCombatTime) {
        this.lastCombatTime = lastCombatTime;
    }

    public String activeSpellId() {
        return activeSpellId;
    }

    public void activeSpellId(String activeSpellId) {
        this.activeSpellId = activeSpellId;
    }
}
