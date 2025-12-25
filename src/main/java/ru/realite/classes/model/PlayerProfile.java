package ru.realite.classes.model;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;

    // =====================
    // CLASS CORE
    // =====================
    private ClassId classId;      // null = не выбран
    private String evolution;     // например "soldier"

    // =====================
    // PROGRESSION
    // =====================
    private int classLevel;       // уровень класса
    private long classXp;         // опыт класса

    // =====================
    // STATE / FLAGS
    // =====================
    private boolean evolutionRewardTaken; // награда за текущую эволюцию
    private long lastClassChange;          // timestamp смены класса
    private boolean evolutionNotified;     // уведомление "доступна эволюция" уже показывали

    public PlayerProfile(UUID uuid) {
        this.uuid = uuid;
    }

    // =====================
    // GETTERS / SETTERS
    // =====================

    public UUID getUuid() {
        return uuid;
    }

    public ClassId getClassId() {
        return classId;
    }

    public void setClassId(ClassId classId) {
        this.classId = classId;
    }

    public String getEvolution() {
        return evolution;
    }

    public void setEvolution(String evolution) {
        this.evolution = evolution;
        this.evolutionRewardTaken = false; // сброс при новой эволюции
        this.evolutionNotified = false;    // и уведомление тоже сбрасываем
    }

    public int getClassLevel() {
        return classLevel;
    }

    public void setClassLevel(int classLevel) {
        this.classLevel = classLevel;
    }

    public long getClassXp() {
        return classXp;
    }

    public void setClassXp(long classXp) {
        this.classXp = classXp;
    }

    public boolean isEvolutionRewardTaken() {
        return evolutionRewardTaken;
    }

    public void setEvolutionRewardTaken(boolean evolutionRewardTaken) {
        this.evolutionRewardTaken = evolutionRewardTaken;
    }

    public long getLastClassChange() {
        return lastClassChange;
    }

    public void setLastClassChange(long lastClassChange) {
        this.lastClassChange = lastClassChange;
    }

    public boolean isEvolutionNotified() {
        return evolutionNotified;
    }

    public void setEvolutionNotified(boolean evolutionNotified) {
        this.evolutionNotified = evolutionNotified;
    }

    // =====================
    // UTILS
    // =====================

    public boolean hasClass() {
        return classId != null;
    }

    public boolean hasEvolution() {
        return evolution != null && !evolution.isEmpty();
    }
}
