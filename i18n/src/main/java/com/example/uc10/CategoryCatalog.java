package com.example.uc10;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedValueSignal;

/**
 * The product categories of UC10, shared by every session. An administrator
 * edits them; every user sees them in their own language, and an edit reaches
 * all open sessions at once.
 */
@Component
public class CategoryCatalog {

    public record Category(String id, LocalizedText name) {
    }

    private final SharedListSignal<Category> categories = new SharedListSignal<>(
            Category.class);

    public CategoryCatalog() {
        reset();
    }

    public SharedListSignal<Category> categories() {
        return categories;
    }

    public void add(LocalizedText name) {
        categories.insertLast(new Category(UUID.randomUUID().toString(), name));
    }

    public void rename(SharedValueSignal<Category> category,
            LocalizedText name) {
        category.update(current -> new Category(current.id(), name));
    }

    /** Restores the sample data. */
    public void reset() {
        categories.clear();
        List.of(LocalizedText.of("en", "Beverages", "de", "Getränke", "fi",
                "Juomat", "ar", "مشروبات", "he", "משקאות"),
                LocalizedText.of("en", "Bakery", "de", "Backwaren", "fi",
                        "Leipomo", "ar", "مخبوزات", "he", "מאפים"),
                LocalizedText.of("en", "Dairy", "de", "Milchprodukte", "fi",
                        "Maitotuotteet", "he", "מוצרי חלב"),
                LocalizedText.of("en", "Frozen food", "de", "Tiefkühlkost",
                        "ar", "أطعمة مجمدة"),
                LocalizedText.of("en", "Household", "fi", "Kodin tarvikkeet"),
                LocalizedText.of("en", "Pet supplies")).forEach(this::add);
    }
}
