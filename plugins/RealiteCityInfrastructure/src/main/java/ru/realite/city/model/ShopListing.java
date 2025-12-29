package ru.realite.city.model;

import java.util.UUID;

public record ShopListing(
        String shopPointId,
        String plotId,
        UUID ownerUuid,
        String title,
        String category,
        String description,
        boolean open,
        long createdAt,
        long updatedAt
) {
}
