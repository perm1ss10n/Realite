package ru.realite.classes.model;

import java.util.UUID;

public class PlayerProfile {

    private final UUID uuid;

    // =====================
    // CLASS CORE
    // =====================
    private ClassId classId; // null = не выбран
    private String evolution; // например "soldier"

    // =====================
    // PROGRESSION
    // =====================
    private int classLevel; // уровень класса
    private long classXp; // опыт класса

    // =====================
    // STATE / FLAGS
    // =====================
    private boolean evolutionRewardTaken; // награда за текущую эволюцию
    private long lastClassChange; // timestamp смены класса
    private boolean evolutionNotified; // уведомление "доступна эволюция" уже показывали
    // стартовый класс назначен автоматически, игрок ещё может свободно выбрать
    // класс
    private boolean starterClass;

    // максимальный уровень, которого игрок достигал в каждом классе (навсегда
    // храним max)
    private java.util.Map<String, Integer> maxLevelByClass = new java.util.HashMap<>();

    // какие классы “закрыты” как пройденные до финальной эволюции (для unlock
    // скрытых классов)
    private java.util.Set<String> masteredClasses = new java.util.HashSet<>();

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
        this.evolutionNotified = false; // и уведомление тоже сбрасываем
    }

    public boolean isStarterClass() {
        return starterClass;
    }

    public void setStarterClass(boolean starterClass) {
        this.starterClass = starterClass;
    }

    public java.util.Map<String, Integer> getMaxLevelByClass() {
        return maxLevelByClass;
    }

    public void setMaxLevelByClass(java.util.Map<String, Integer> map) {
        this.maxLevelByClass = (map != null) ? map : new java.util.HashMap<>();
    }

    public java.util.Set<String> getMasteredClasses() {
        return masteredClasses;
    }

    public void setMasteredClasses(java.util.Set<String> set) {
        this.masteredClasses = (set != null) ? set : new java.util.HashSet<>();
    }

    public void addMastered(ClassId id) {
        if (id != null)
            masteredClasses.add(id.name());
    }

    public boolean hasMastered(ClassId id) {
        return id != null && masteredClasses.contains(id.name());
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
