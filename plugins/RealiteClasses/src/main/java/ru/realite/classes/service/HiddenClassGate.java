package ru.realite.classes.service;

import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.EvolutionRequirement;
import ru.realite.classes.model.HiddenClassUnlock;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.core.api.quests.QuestUnlockService;

import java.util.StringJoiner;
import java.util.function.Supplier;

public final class HiddenClassGate {

    private final ClassConfigRepository classConfig;
    private final EvolutionRequirementAdapter evolutionAdapter;
    private final Supplier<QuestUnlockService> questUnlockServiceSupplier;

    public HiddenClassGate(ClassConfigRepository classConfig,
                           EvolutionRequirementAdapter evolutionAdapter,
                           Supplier<QuestUnlockService> questUnlockServiceSupplier) {
        this.classConfig = classConfig;
        this.evolutionAdapter = evolutionAdapter;
        this.questUnlockServiceSupplier = questUnlockServiceSupplier;
    }

    public HiddenClassGateResult check(Player player, ClassId classId) {
        if (classId == null) {
            return new HiddenClassGateResult(false, false, false, "class-locked-both");
        }
        ClassConfigRepository.ClassDef def = classConfig.get(classId);
        if (def == null || !def.hidden) {
            return new HiddenClassGateResult(true, true, true, null);
        }
        String requiredQuest = requiredQuestId(classId);
        boolean questOk = requiredQuest == null || requiredQuest.isBlank();
        if (!questOk) {
            QuestUnlockService unlockService = questUnlockServiceSupplier != null
                    ? questUnlockServiceSupplier.get()
                    : null;
            questOk = unlockService != null && unlockService.hasUnlock(player, requiredQuest);
        }
        EvolutionRequirement requirement = resolveEvolutionRequirement(classId, def);
        boolean evolutionOk = evolutionAdapter.isMet(player, requirement);

        String reasonKey = null;
        if (!questOk && !evolutionOk) {
            reasonKey = "class-locked-both";
        } else if (!questOk) {
            reasonKey = "class-locked-quest";
        } else if (!evolutionOk) {
            reasonKey = "class-locked-evolution";
        }

        return new HiddenClassGateResult(questOk && evolutionOk, questOk, evolutionOk, reasonKey);
    }

    public String requiredQuestId(ClassId classId) {
        HiddenClassUnlock unlock = classConfig.getHiddenUnlock(classId);
        if (unlock == null) {
            return null;
        }
        String questId = unlock.requiredQuestId();
        return questId == null || questId.isBlank() ? null : questId.trim();
    }

    public String describeEvolutionRequirement(ClassId classId) {
        EvolutionRequirement requirement = resolveEvolutionRequirement(classId, classConfig.get(classId));
        if (requirement == null || requirement.isEmpty()) {
            return "-";
        }
        StringJoiner joiner = new StringJoiner(", ");
        for (ClassId reqId : requirement.masteredClasses()) {
            ClassConfigRepository.ClassDef reqDef = classConfig.get(reqId);
            joiner.add(reqDef != null ? reqDef.name : reqId.name());
        }
        return joiner.toString();
    }

    private EvolutionRequirement resolveEvolutionRequirement(ClassId classId, ClassConfigRepository.ClassDef def) {
        HiddenClassUnlock unlock = classConfig.getHiddenUnlock(classId);
        if (unlock != null && unlock.evolutionRequirement() != null) {
            return unlock.evolutionRequirement();
        }
        if (def == null || def.requiresMastered == null || def.requiresMastered.isEmpty()) {
            return null;
        }
        return EvolutionRequirement.fromMastered(def.requiresMastered);
    }
}
