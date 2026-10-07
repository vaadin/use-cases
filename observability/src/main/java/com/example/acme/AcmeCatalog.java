package com.example.acme;

import java.util.List;

/**
 * The product catalog of the fictional <em>Acme Supply Co.</em>, a wholesale
 * hardware supplier. Generated as every combination of material, product type
 * and size, so it is large enough (5&nbsp;120 items) that a lazy component has
 * to page and filter rather than load it whole, while every item still reads
 * like something a clerk would actually search for.
 */
public final class AcmeCatalog {

    private static final List<String> MATERIALS = List.of("Stainless steel",
            "Zinc-plated", "Brass", "Copper", "Galvanized", "Black oxide",
            "Aluminium", "Nylon", "Chrome-plated", "Titanium", "Bronze",
            "Hardened steel", "Phosphate-coated", "Hot-dip galvanized",
            "Nickel-plated", "Carbon steel");

    private static final List<String> TYPES = List.of("hex bolt", "wood screw",
            "machine screw", "flat washer", "lock washer", "hex nut",
            "wing nut", "carriage bolt", "lag screw", "eye bolt", "U-bolt",
            "threaded rod", "cotter pin", "dowel pin", "rivet", "anchor bolt",
            "set screw", "shoulder bolt", "flange nut", "self-tapping screw");

    private static final List<String> SIZES = List.of("M3 × 10", "M3 × 16",
            "M4 × 12", "M4 × 20", "M5 × 16", "M5 × 25", "M6 × 20", "M6 × 30",
            "M8 × 25", "M8 × 40", "M10 × 30", "M10 × 50", "M12 × 40",
            "M12 × 60", "M16 × 50", "M16 × 80");

    private static final List<String> PRODUCTS = MATERIALS.stream()
            .flatMap(material -> TYPES.stream().flatMap(type -> SIZES.stream()
                    .map(size -> "%s %s %s".formatted(material, type, size))))
            .sorted().toList();

    private AcmeCatalog() {
    }

    /** Every product Acme sells, sorted, as search-friendly display names. */
    public static List<String> products() {
        return PRODUCTS;
    }
}
