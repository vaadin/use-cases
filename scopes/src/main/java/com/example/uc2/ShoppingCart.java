package com.example.uc2;

import java.util.List;

import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.shared.SharedListSignal;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.VaadinSessionScope;

/**
 * The signed-in user's shopping cart.
 * <p>
 * {@code @VaadinSessionScope} gives one instance per {@code VaadinSession},
 * which in practice means one per user and browser: every tab the user has open
 * shares the same cart, while other users get their own. A
 * {@link SharedListSignal} is used because several UIs (one per open tab)
 * observe the same cart and each of them must be updated when another tab adds
 * an item.
 */
@SpringComponent
@VaadinSessionScope
public class ShoppingCart {

    private final SharedListSignal<String> items = new SharedListSignal<>(
            String.class);

    public Signal<List<String>> items() {
        return Signal.computed(() -> items.getValues().toList());
    }

    public void add(String product) {
        items.insertLast(product);
    }

    public void clear() {
        items.clear();
    }
}
