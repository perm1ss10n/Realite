package ru.realite.core.api.ui;

import java.util.List;

public interface UiPaginationService {
    <T> UiPage<T> paginate(List<T> items, int page, int pageSize);
}
