package com.hsf302.bookingtour.catalog;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Locale;

/**
 * Read-only knowledge base for the chat assistant. Intentionally a separate
 * copy of the same seed data used in TourController, rather than reusing
 * TourController's private field, so this feature can be added without
 * modifying any existing file. If TourController's catalog is later extracted
 * into a shared service, this class can be deleted and ChatService pointed
 * at that instead.
 */
@Component
public class TourKnowledgeBase {

    private final List<Tour> tours = List.of(
            new Tour("t1", "Sapa 3N2D", "Sapa, Vietnam", 429, 4.9,
                    "Mountain views, local market, and cable car."),
            new Tour("t2", "Da Lat Chill", "Da Lat, Vietnam", 319, 4.8,
                    "Coffee farms, pine hills, and lakeside sunsets."),
            new Tour("t3", "Phu Quoc Escape", "Phu Quoc, Vietnam", 559, 4.9,
                    "Beach resort, snorkelling, and night market."),
            new Tour("t4", "Ha Long Cruise", "Ha Long Bay, Vietnam", 489, 4.7,
                    "Luxury cruise, cave visit, and sunset deck.")
    );

    public List<Tour> all() {
        return tours;
    }

    /**
     * Naive keyword retrieval (Tier A - no embeddings needed at this scale).
     * Falls back to the full catalog when nothing matches, so the assistant
     * always has grounded context rather than an empty one.
     */
    public List<Tour> search(String query) {
        if (query == null || query.isBlank()) {
            return tours;
        }
        String normalized = query.toLowerCase(Locale.ROOT);
        List<Tour> matches = tours.stream()
                .filter(t -> matches(normalized, t))
                .toList();
        return matches.isEmpty() ? tours : matches;
    }

    private boolean matches(String normalizedQuery, Tour tour) {
        String haystack = (tour.name() + " " + tour.location() + " " + tour.summary())
                .toLowerCase(Locale.ROOT);
        for (String word : normalizedQuery.split("\\s+")) {
            if (word.length() > 2 && haystack.contains(word)) {
                return true;
            }
        }
        return false;
    }
}