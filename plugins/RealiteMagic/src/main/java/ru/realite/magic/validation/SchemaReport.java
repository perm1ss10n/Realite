package ru.realite.magic.validation;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class SchemaReport {

    private final List<SchemaError> errors;

    public SchemaReport() {
        this.errors = new ArrayList<>();
    }

    public SchemaReport(List<SchemaError> errors) {
        this.errors = new ArrayList<>(Objects.requireNonNull(errors, "errors"));
    }

    public List<SchemaError> errors() {
        return Collections.unmodifiableList(errors);
    }

    public boolean ok() {
        return errors.isEmpty();
    }

    public void addError(String file,
                         String spellId,
                         String path,
                         String messageKey,
                         Map<String, String> placeholders) {
        errors.add(new SchemaError(file, spellId, path, messageKey,
                placeholders == null ? Map.of() : Map.copyOf(placeholders)));
    }
}
