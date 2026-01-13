package ru.realite.ui.pagination;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import ru.realite.core.api.ui.UiPage;
import ru.realite.core.api.ui.UiPaginationService;

public final class UiPaginationServiceImpl implements UiPaginationService {

    @Override
    public <T> UiPage<T> paginate(List<T> items, int page, int pageSize) {
        Objects.requireNonNull(items, "items");
        int safeSize = pageSize <= 0 ? items.size() : pageSize;
        if (safeSize <= 0) {
            safeSize = 1;
        }
        int totalPages = Math.max(1, (int) Math.ceil(items.size() / (double) safeSize));
        int safePage = Math.max(0, Math.min(page, totalPages - 1));
        int start = safePage * safeSize;
        int end = Math.min(start + safeSize, items.size());
        List<T> pageItems = start < end ? new ArrayList<>(items.subList(start, end)) : List.of();
        return new UiPage<>(safePage, totalPages, pageItems);
    }
}
