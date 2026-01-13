package ru.realite.core.api.ui;

import java.util.List;

public record UiPage<T>(int page, int totalPages, List<T> items) {

    public boolean hasPrevious() {
        return page > 0;
    }

    public boolean hasNext() {
        return page + 1 < totalPages;
    }
}
