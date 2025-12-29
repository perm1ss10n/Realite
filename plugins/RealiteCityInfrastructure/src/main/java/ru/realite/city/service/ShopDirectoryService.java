package ru.realite.city.service;

import ru.realite.city.model.ShopListing;
import ru.realite.city.model.ShopPoint;
import ru.realite.city.storage.ShopListingRepository;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

public final class ShopDirectoryService {

    private static final String DEFAULT_TITLE = "Shop";
    private static final String DEFAULT_CATEGORY = "general";

    private final ShopListingRepository repository;
    private final ShopMarkerService markerService;

    public ShopDirectoryService(ShopListingRepository repository, ShopMarkerService markerService) {
        this.repository = repository;
        this.markerService = markerService;
    }

    public Optional<ShopListing> getListing(String shopPointId) {
        return repository.findByShopPointId(shopPointId);
    }

    public ShopListing ensureListing(ShopPoint point) {
        return repository.findByShopPointId(point.id())
                .orElseGet(() -> createDefaultListing(point));
    }

    public ShopListing updateListing(ShopPoint point, ListingUpdate update) {
        ShopListing current = repository.findByShopPointId(point.id())
                .orElseGet(() -> createDefaultListing(point));
        long now = System.currentTimeMillis();
        ShopListing updated = new ShopListing(
                point.id(),
                point.plotId(),
                point.ownerUuid(),
                update.title() != null ? update.title() : current.title(),
                update.category() != null ? update.category() : current.category(),
                update.description() != null ? update.description() : current.description(),
                update.open() != null ? update.open() : current.open(),
                current.createdAt(),
                now
        );
        repository.upsert(updated);
        if (markerService != null) {
            markerService.onListingUpdated(point, updated);
        }
        return updated;
    }

    public void deleteListing(String shopPointId) {
        repository.delete(shopPointId);
    }

    public List<ShopListing> listAll(boolean openOnly, String category, String searchQuery) {
        List<ShopListing> listings = new ArrayList<>();
        String categoryLower = category != null ? category.toLowerCase(Locale.ROOT) : null;
        String searchLower = searchQuery != null ? searchQuery.toLowerCase(Locale.ROOT) : null;
        for (ShopListing listing : repository.findAll()) {
            if (openOnly && !listing.open()) {
                continue;
            }
            if (categoryLower != null && !categoryLower.isBlank()) {
                if (listing.category() == null
                        || !listing.category().toLowerCase(Locale.ROOT).equals(categoryLower)) {
                    continue;
                }
            }
            if (searchLower != null && !searchLower.isBlank()) {
                String title = listing.title() == null ? "" : listing.title();
                String description = listing.description() == null ? "" : listing.description();
                String haystack = (title + " " + description).toLowerCase(Locale.ROOT);
                if (!haystack.contains(searchLower)) {
                    continue;
                }
            }
            listings.add(listing);
        }
        return listings;
    }

    public List<ShopListing> listByOwner(UUID ownerUuid) {
        return repository.findByOwner(ownerUuid);
    }

    private ShopListing createDefaultListing(ShopPoint point) {
        long now = System.currentTimeMillis();
        ShopListing listing = new ShopListing(
                point.id(),
                point.plotId(),
                point.ownerUuid(),
                DEFAULT_TITLE,
                DEFAULT_CATEGORY,
                "",
                false,
                now,
                now
        );
        repository.upsert(listing);
        if (markerService != null) {
            markerService.onListingUpdated(point, listing);
        }
        return listing;
    }

    public record ListingUpdate(String title, String category, String description, Boolean open) {
    }
}
