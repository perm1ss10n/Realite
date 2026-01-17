package ru.realite.core.api.models;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;

public record ModelContext(String source, Map<String, String> attributes) {

    public static final String ATTR_OWNER_UUID = "ownerUuid";
    public static final String ATTR_SEED = "seed";
    public static final String ATTR_VARIANT = "variant";
    public static final String ATTR_TAGS = "tags";

    private static final String TAG_SEPARATOR = ",";

    public ModelContext {
        Objects.requireNonNull(source, "source");
        Objects.requireNonNull(attributes, "attributes");
    }

    public static ModelContext empty() {
        return new ModelContext("unknown", Collections.emptyMap());
    }

    public static ModelContext profile(String source, UUID ownerUuid, Long seed, String variant, Set<String> tags) {
        Map<String, String> attrs = new HashMap<>();
        if (ownerUuid != null) {
            attrs.put(ATTR_OWNER_UUID, ownerUuid.toString());
        }
        if (seed != null) {
            attrs.put(ATTR_SEED, Long.toString(seed));
        }
        if (variant != null && !variant.isBlank()) {
            attrs.put(ATTR_VARIANT, variant);
        }
        if (tags != null && !tags.isEmpty()) {
            attrs.put(ATTR_TAGS, String.join(TAG_SEPARATOR, tags));
        }
        return new ModelContext(source, Collections.unmodifiableMap(attrs));
    }

    public Optional<UUID> ownerUuid() {
        String raw = attributes.get(ATTR_OWNER_UUID);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(UUID.fromString(raw));
    }

    public OptionalLong seed() {
        String raw = attributes.get(ATTR_SEED);
        if (raw == null || raw.isBlank()) {
            return OptionalLong.empty();
        }
        return OptionalLong.of(Long.parseLong(raw));
    }

    public Optional<String> variant() {
        String raw = attributes.get(ATTR_VARIANT);
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        return Optional.of(raw);
    }

    public Set<String> tags() {
        String raw = attributes.get(ATTR_TAGS);
        if (raw == null || raw.isBlank()) {
            return Collections.emptySet();
        }
        Set<String> parsed = new LinkedHashSet<>();
        for (String tag : raw.split(TAG_SEPARATOR)) {
            String trimmed = tag.trim();
            if (!trimmed.isEmpty()) {
                parsed.add(trimmed);
            }
        }
        return Collections.unmodifiableSet(parsed);
    }

    public boolean hasTag(String tag) {
        if (tag == null || tag.isBlank()) {
            return false;
        }
        return tags().contains(tag.trim());
    }
}
