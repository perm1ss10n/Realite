package ru.realite.magic.api.impl;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;
import ru.realite.magic.api.SpellRegistryView;
import ru.realite.magic.api.SpellView;
import ru.realite.magic.spell.SpellDefinition;
import ru.realite.magic.spell.SpellRegistry;

public final class SpellRegistryViewImpl implements SpellRegistryView {

    private final SpellRegistry registry;

    public SpellRegistryViewImpl(SpellRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public Optional<SpellView> find(String spellId) {
        return registry.find(spellId).map(this::toView);
    }

    @Override
    public Collection<SpellView> all() {
        return registry.all().stream()
                .map(this::toView)
                .collect(Collectors.toUnmodifiableList());
    }

    private SpellView toView(SpellDefinition definition) {
        return new SpellView(
                definition.id(),
                definition.nameKey(),
                definition.descKey(),
                definition.type(),
                definition.school(),
                definition.requirements(),
                definition.target(),
                List.copyOf(definition.effects()));
    }
}
