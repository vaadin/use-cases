package com.example.backend;

import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;

/** A small product catalog to search in. */
@Service
public class ProductCatalog {

    private static final List<String> PRODUCTS = List.of("Laptop 13\"",
            "Laptop 15\"", "Laptop sleeve", "Laptop stand", "Lamp, desk",
            "Lamp, floor", "Label printer", "Labels ×500", "Lanyard",
            "Laser pointer", "Latte glasses ×6", "Ladder, 3 steps",
            "Monitor 27\"", "Monitor arm", "Mouse", "Mouse pad", "Keyboard",
            "Keyboard tray", "Headset", "Webcam", "USB-C hub", "USB-C cable",
            "Docking station", "Desk, standing", "Chair, ergonomic",
            "Whiteboard", "Whiteboard markers", "Notebook A5", "Pens ×10",
            "Stapler", "Paper A4 ×500", "Shredder", "Coffee machine",
            "Water filter", "Plant, ficus", "Plant pot");

    /** The products whose name contains {@code text}, case-insensitively. */
    public List<String> search(String text) {
        String needle = text.toLowerCase(Locale.ROOT);
        return PRODUCTS.stream()
                .filter(name -> name.toLowerCase(Locale.ROOT).contains(needle))
                .toList();
    }
}
