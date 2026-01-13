package ru.realite.classes.menu;

import java.util.Map;
import org.bukkit.entity.Player;
import ru.realite.classes.model.ClassId;
import ru.realite.classes.model.HudMode;
import ru.realite.classes.service.ClassHudService;
import ru.realite.classes.service.ClassService;
import ru.realite.classes.service.EvolutionService;
import ru.realite.classes.service.HiddenClassGate;
import ru.realite.classes.service.HiddenClassGateResult;
import ru.realite.classes.storage.ClassConfigRepository;
import ru.realite.classes.storage.ClassLoreRepository;
import ru.realite.classes.util.Messages;

public final class ClassMenuManager {

    private final ClassService classService;
    private final EvolutionService evolutionService;
    private final ClassConfigRepository classConfig;
    private final ClassLoreRepository classLore;
    private final HiddenClassGate hiddenClassGate;
    private final Messages messages;
    private final ClassHudService hudService;

    public ClassMenuManager(ClassService classService,
                            EvolutionService evolutionService,
                            ClassConfigRepository classConfig,
                            ClassLoreRepository classLore,
                            HiddenClassGate hiddenClassGate,
                            Messages messages,
                            ClassHudService hudService) {
        this.classService = classService;
        this.evolutionService = evolutionService;
        this.classConfig = classConfig;
        this.classLore = classLore;
        this.hiddenClassGate = hiddenClassGate;
        this.messages = messages;
        this.hudService = hudService;
    }

    public void openMain(Player player) {
        new ClassMainMenu(this).open(player);
    }

    public void openDetails(Player player, ClassId id) {
        new ClassDetailsMenu(this, id).open(player);
    }

    public void openConfirm(Player player, ClassId id) {
        new ClassConfirmMenu(this, id).open(player);
    }

    public void openSettings(Player player) {
        new ClassSettingsMenu(this).open(player);
    }

    public void openProgress(Player player) {
        var profile = classService.getProfile(player);
        if (profile == null || !profile.hasClass()) {
            player.sendMessage(messages.get("no-class"));
            return;
        }
        openDetails(player, profile.getClassId());
    }

    public void applyHudMode(Player player, HudMode mode) {
        var profile = classService.getProfile(player);
        if (profile == null) {
            return;
        }
        profile.setHudMode(mode);
        classService.save(profile);
        if (hudService != null) {
            hudService.refreshNow(player);
        }
        player.sendMessage(messages.format("ui.classes.settings.updated",
                Map.of("mode", mode.name())));
        player.closeInventory();
    }

    public void assignClass(Player player, ClassId id) {
        var profile = classService.getProfile(player);
        if (profile == null) {
            return;
        }

        if (profile.hasClass() && !evolutionService.canChangeClass(player, profile)) {
            player.sendMessage(messages.get("cant-change"));
            player.closeInventory();
            return;
        }

        var def = classConfig.get(id);
        var loreDef = classLore != null ? classLore.get(id) : null;
        boolean hidden = (def != null && def.hidden) || (loreDef != null && loreDef.hiddenEnabled);
        if (hidden && hiddenClassGate != null) {
            HiddenClassGateResult gateResult = hiddenClassGate.check(player, id);
            if (!gateResult.available()) {
                player.closeInventory();
                sendHiddenLockedMessage(player, id, gateResult);
                return;
            }
        }

        classService.assignClass(player, id);
        profile.setStarterClass(false);
        classService.save(profile);

        String niceName = def != null ? def.name : id.name();
        player.sendMessage(messages.format("chosen", Map.of("class", niceName)));

        if (hudService != null) {
            hudService.refreshNow(player);
        }
        player.closeInventory();
    }

    private void sendHiddenLockedMessage(Player player, ClassId id, HiddenClassGateResult gateResult) {
        if (hiddenClassGate == null) {
            player.sendMessage(messages.get("class-locked"));
            return;
        }
        String reasonKey = gateResult.reasonKey();
        if ("class-locked-quest".equals(reasonKey)) {
            String questId = hiddenClassGate.requiredQuestId(id);
            player.sendMessage(messages.format("class-locked-quest",
                    Map.of("quest", questId != null ? questId : "-")));
        } else if ("class-locked-evolution".equals(reasonKey)) {
            player.sendMessage(messages.get("class-locked-evolution"));
            player.sendMessage(messages.format("class-locked-requirements",
                    Map.of("req", hiddenClassGate.describeEvolutionRequirement(id))));
        } else if ("class-locked-both".equals(reasonKey)) {
            player.sendMessage(messages.get("class-locked-both"));
            String questId = hiddenClassGate.requiredQuestId(id);
            player.sendMessage(messages.format("class-locked-quest",
                    Map.of("quest", questId != null ? questId : "-")));
            player.sendMessage(messages.format("class-locked-requirements",
                    Map.of("req", hiddenClassGate.describeEvolutionRequirement(id))));
        } else {
            player.sendMessage(messages.get("class-locked"));
        }
    }

    public ClassConfigRepository classConfig() {
        return classConfig;
    }

    public ClassLoreRepository classLore() {
        return classLore;
    }

    public HiddenClassGate hiddenClassGate() {
        return hiddenClassGate;
    }

    public Messages messages() {
        return messages;
    }

    public ClassService classService() {
        return classService;
    }
}
