package ru.realite.core.api.familiars;

import java.util.Optional;
import org.bukkit.entity.Player;
import ru.realite.core.api.ui.UiProviderId;

public interface FamiliarUiService {

    UiProviderId HUD_PROVIDER_ID = new UiProviderId("familiars.status");

    Optional<FamiliarHudData> hudData(Player player);

    Optional<FamiliarManagerData> managerData(Player player);

    Optional<FamiliarDetailsData> detailsData(Player player, String typeId);

    boolean openInventory(Player player, String typeId);

    FamiliarActionResult summon(Player player, String typeId);

    FamiliarActionResult dismiss(Player player, String typeId);

    FamiliarActionResult setActive(Player player, String typeId);

    FamiliarActionResult rename(Player player, String typeId, String name);
}
