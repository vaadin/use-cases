package com.example.uc2;

import java.util.List;

import com.example.common.BuiltWith;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.ListItem;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SharedCartView.class)
class SharedCartViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersProductsAndEmptyCart() {
        navigate(SharedCartView.class);
        runPendingSignalsTasks();

        assertEquals("UC2 — Shared shopping cart",
                findInView(H1.class).single().getText());
        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "Cart (0)".equals(h.getText())));
        assertEquals(SharedCartView.PRODUCTS.size(),
                findInView(Button.class).all().stream()
                        .filter(b -> b.getText().startsWith("Add ")).count());
    }

    @Test
    void addedItemsAreKeptForTheSessionButNotSharedWithOthers() {
        navigate(SharedCartView.class);
        runPendingSignalsTasks();

        test(button("Add Travel mug")).click();
        runPendingSignalsTasks();
        assertEquals(1, cartItems().size());

        // A new view instance in the same session sees the same cart, like a
        // second tab would.
        navigate(SharedCartView.class);
        runPendingSignalsTasks();
        assertTrue(findInView(H2.class).all().stream()
                .anyMatch(h -> "Cart (1)".equals(h.getText())));

        // Another user's session has its own, empty cart.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(SharedCartView.class);
        runPendingSignalsTasks();
        assertTrue(cartItems().isEmpty());
    }

    /** The cart's items, not the points of "How it is built". */
    private List<ListItem> cartItems() {
        return findInView(ListItem.class).all().stream()
                .filter(item -> item.findAncestor(BuiltWith.class) == null)
                .toList();
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
