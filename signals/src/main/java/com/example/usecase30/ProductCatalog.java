package com.example.usecase30;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

final class ProductCatalog {

    private static final String[] CATEGORIES = { "Audio", "Wearables",
            "Cameras", "Laptops", "Phones" };
    private static final String[] ADJECTIVES = { "Pro", "Lite", "Max", "Mini",
            "Studio", "Ultra", "Plus", "Air" };

    private ProductCatalog() {
    }

    static List<Product> generate(int size) {
        Random random = new Random(42);
        List<Product> products = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            String category = CATEGORIES[i % CATEGORIES.length];
            String adjective = ADJECTIVES[random.nextInt(ADJECTIVES.length)];
            double price = 20 + random.nextInt(1900) + random.nextDouble();
            products.add(new Product(i + 1,
                    category + " " + adjective + " #" + (i + 1), category,
                    Math.round(price * 100.0) / 100.0));
        }
        return List.copyOf(products);
    }

    static List<String> categories() {
        return List.of(CATEGORIES);
    }
}
