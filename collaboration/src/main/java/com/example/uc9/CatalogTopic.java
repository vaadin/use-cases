package com.example.uc9;

import jakarta.annotation.PostConstruct;

import java.util.List;

import org.springframework.stereotype.Component;

import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.signals.shared.SharedMapSignal;

/**
 * UC9's shared catalogue: the rows, and who is editing which row.
 * <p>
 * Editing is tracked per row rather than per cell, which is not the choice this
 * module would have made — {@code GridPro}'s cell-edit event reports the item
 * and not the column, so the column a peer is in cannot be observed. See
 * API-GAPS.md #12.
 */
@Component
public class CatalogTopic {

    public record Product(String id, String name, String category,
            String price) {

        public Product withName(String value) {
            return new Product(id, value, category, price);
        }

        public Product withCategory(String value) {
            return new Product(id, name, value, price);
        }

        public Product withPrice(String value) {
            return new Product(id, name, category, value);
        }
    }

    private final SharedListSignal<Product> rows = new SharedListSignal<>(
            Product.class);

    private final SharedMapSignal<String> editing = new SharedMapSignal<>(
            String.class);

    public SharedListSignal<Product> rows() {
        return rows;
    }

    public SharedMapSignal<String> editing() {
        return editing;
    }

    @PostConstruct
    void seed() {
        if (rows.peek().isEmpty()) {
            rows.insertAllLast(List.of(
                    new Product("p1", "Hex bolt M8", "Fasteners", "0.12"),
                    new Product("p2", "Impact driver", "Power tools", "129.00"),
                    new Product("p3", "Cable clip 6 mm", "Electrical", "0.04"),
                    new Product("p4", "Spirit level 60 cm", "Hand tools",
                            "18.50")));
        }
    }
}
