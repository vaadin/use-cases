package com.example.uc2;

import java.util.List;

import com.example.common.BuiltWith;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
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

    @Autowired
    private ApplicationContext applicationContext;

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
    void cartIsSharedByTheTabsOfASessionButNotWithOtherUsers() {
        try (var app = SpringBrowserlessApplicationContext.create(
                applicationContext,
                b -> b.withViewPackages(SharedCartView.class)
                        .withSessionLockIsolation(true))) {
            var aliceTab = app.newUser().newWindow();
            var aliceOtherTab = aliceTab.getUser().newWindow();
            var bob = app.newUser().newWindow();
            aliceTab.navigate(SharedCartView.class);
            aliceOtherTab.navigate(SharedCartView.class);
            bob.navigate(SharedCartView.class);

            aliceTab.test(aliceTab.find(Button.class).withText("Add Travel mug")
                    .single()).click();
            aliceTab.runPendingSignalsTasks();
            assertEquals(List.of("Travel mug"), cartItems(aliceTab));

            // The other tab of the same session shows the same cart.
            aliceOtherTab.runPendingSignalsTasks();
            assertEquals(List.of("Travel mug"), cartItems(aliceOtherTab));

            // Another user's session has its own, empty cart.
            bob.runPendingSignalsTasks();
            assertTrue(cartItems(bob).isEmpty());
        }
    }

    /** The cart's items, not the points of "How it is built". */
    private static List<String> cartItems(BrowserlessUIContext window) {
        return window.find(ListItem.class).all().stream()
                .filter(item -> item.findAncestor(BuiltWith.class) == null)
                .map(ListItem::getText).toList();
    }
}
