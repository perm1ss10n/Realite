package ru.realite.magic.api;

import java.util.Collection;
import java.util.Optional;

public interface SpellRegistryView {

    Optional<SpellView> find(String spellId);

    Collection<SpellView> all();
}
