package com.example.uc2;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Shopping cart shared by all of a user's tabs (VaadinSession scope).
 * <p>
 * A shopper opens product pages in several tabs to compare them and adds items
 * from whichever tab they are looking at. They expect one cart: an item added
 * in one tab is in the cart in every other tab, and it is still there after a
 * reload. Another shopper, in another browser, has a separate cart.
 * {@link ShoppingCart} is {@code @VaadinSessionScope}, which matches exactly
 * that lifetime.
 * <p>
 * Open this page in two tabs of the same browser and add items in one.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Shared shopping cart")
@Menu(order = 2, title = "UC2 — Shared shopping cart")
public class SharedCartView extends VerticalLayout {

    static final List<String> PRODUCTS = List.of("Espresso machine",
            "Coffee grinder", "Milk frother", "Travel mug");

    public SharedCartView(ShoppingCart cart) {
        add(new H1("UC2 — Shared shopping cart"));
        add(new Paragraph("The cart is a @VaadinSessionScope bean: one per "
                + "user and browser. Open this page in a second tab and "
                + "add products from either tab — both show the same cart. "
                + "A different browser gets its own, empty cart."));

        add(new H2("Products"));
        HorizontalLayout products = new HorizontalLayout();
        products.setWrap(true);
        PRODUCTS.forEach(product -> products
                .add(new Button("Add " + product, e -> cart.add(product))));
        add(products);

        Signal<List<String>> items = cart.items();
        H2 heading = new H2();
        heading.bindText(items.map(list -> "Cart (" + list.size() + ")"));
        add(heading);

        Span empty = new Span("The cart is empty.");
        empty.addClassName("hint");
        empty.bindVisible(items.map(List::isEmpty));

        UnorderedList list = new UnorderedList();
        Signal.effect(list, () -> {
            list.removeAll();
            items.get().forEach(item -> list.add(new ListItem(item)));
        });

        add(empty, list, new Button("Empty cart", e -> cart.clear()));
    }
}
