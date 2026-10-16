package com.example.uc4;

import java.math.BigDecimal;
import java.util.List;

import com.vaadin.flow.signals.local.ValueSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.UIScope;

/**
 * The price list being edited in one UI.
 * <p>
 * It has the same {@code @UIScope} as the {@link UndoHistory} whose entries
 * restore it. Kept in the view instead, a new view instance after navigating
 * away and back would show a fresh list while the history still held undo steps
 * for the old one.
 */
@SpringComponent
@UIScope
public class PriceList {

    public record Price(String product, BigDecimal amount) {
    }

    private final ValueSignal<List<Price>> prices = new ValueSignal<>(
            List.of(new Price("Espresso machine", new BigDecimal("499.00")),
                    new Price("Coffee grinder", new BigDecimal("129.00")),
                    new Price("Milk frother", new BigDecimal("59.00"))));

    public ValueSignal<List<Price>> prices() {
        return prices;
    }
}
