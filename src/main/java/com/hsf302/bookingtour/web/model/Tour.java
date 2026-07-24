package com.hsf302.bookingtour.web.model;

import java.util.ArrayList;
import java.util.List;

public record Tour(String id, String name, String location, int price, double rating, String summary, String imageUrl) {

    private static final List<Tour> CATALOG = new ArrayList<>(List.of(
            new Tour("t1", "Sapa 3N2D", "Sapa, Vietnam", 429, 4.9,
                    "Mountain views, local market, and cable car.",
                    "https://images.unsplash.com/photo-1506905925346-21bda4d32df4?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t2", "Da Lat Chill", "Da Lat, Vietnam", 319, 4.8,
                    "Coffee farms, pine hills, and lakeside sunsets.",
                    "https://images.unsplash.com/photo-1540202404-a2f29016b523?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t3", "Phu Quoc Escape", "Phu Quoc, Vietnam", 559, 4.9,
                    "Beach resort, snorkelling, and night market.",
                    "https://images.unsplash.com/photo-1507525428034-b723cf961d3e?auto=format&fit=crop&w=1200&q=80"),
            new Tour("t4", "Ha Long Cruise", "Ha Long Bay, Vietnam", 489, 4.7,
                    "Luxury cruise, cave visit, and sunset deck.",
                    "https://images.unsplash.com/photo-1528127269322-539801943592?auto=format&fit=crop&w=1200&q=80")
    ));

    public static List<Tour> all() {
        return CATALOG;
    }

    public static Tour findOrNull(String id) {
        return CATALOG.stream()
                .filter(tour -> tour.id().equalsIgnoreCase(id))
                .findFirst()
                .orElse(null);
    }
}
