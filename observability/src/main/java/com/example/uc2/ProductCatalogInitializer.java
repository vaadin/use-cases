package com.example.uc2;

import java.util.HashSet;
import java.util.List;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds a handful of Acme Supply's products, each in one or two categories, so
 * the join-table fetch demo has something to load. Enough products that the
 * N+1 fan-out is unmistakable in the statement count, few enough to stay an
 * in-memory toy.
 */
@Component
class ProductCatalogInitializer implements CommandLineRunner {

    private static final String[] CATEGORY_NAMES = { "Bolts", "Screws",
            "Washers", "Nuts", "Anchors" };
    private static final String[] PRODUCT_NAMES = {
            "Stainless steel hex bolt M8 × 40", "Zinc-plated wood screw 4 × 40",
            "Brass flat washer M6", "Galvanized hex nut M10",
            "Hardened steel anchor bolt M12 × 100",
            "Nylon lock washer M8", "Copper self-tapping screw 3.5 × 25",
            "Titanium carriage bolt M6 × 50" };

    private final ProductRepository products;
    private final CategoryRepository categories;

    ProductCatalogInitializer(ProductRepository products,
            CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (products.count() > 0) {
            return;
        }

        Category[] saved = new Category[CATEGORY_NAMES.length];
        for (int i = 0; i < CATEGORY_NAMES.length; i++) {
            saved[i] = categories.save(new Category(CATEGORY_NAMES[i]));
        }

        for (int i = 0; i < PRODUCT_NAMES.length; i++) {
            Product product = new Product(PRODUCT_NAMES[i]);
            // One or two categories per product — the actual count is
            // irrelevant; what matters is that every product owns a separate
            // collection that gets fetched in its own query.
            product.setCategory(new HashSet<>(List.of(saved[i % saved.length],
                    saved[(i + 1) % saved.length])));
            products.save(product);
        }
    }
}
