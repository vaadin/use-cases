package com.example.usecase06;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase06View.class)
@WithMockUser
class UseCase06ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithProductButtons() {
        navigate(UseCase06View.class);

        // 4 "Add" buttons for products + "Empty cart" button
        long addButtons = findInView(Button.class).all().stream()
                .filter(b -> "Add".equals(b.getText())).count();
        assertEquals(4, addButtons);
    }

    @Test
    void emptyCartButtonPresent() {
        navigate(UseCase06View.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Empty cart".equals(b.getText())));
    }

    @Test
    void subtotalIsZeroInitially() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        Span subtotalLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Subtotal:"))
                .findFirst().orElseThrow();
        assertEquals("Subtotal: $0.00", subtotalLabel.getText());
    }

    @Test
    void addProductToCart() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        // Click first "Add" button (Laptop - $999.99)
        Button addLaptop = findInView(Button.class).all().stream()
                .filter(b -> "Add".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addLaptop).click();
        runPendingSignalsTasks();

        Span subtotalLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Subtotal:"))
                .findFirst().orElseThrow();
        assertEquals("Subtotal: $999.99", subtotalLabel.getText());
    }

    @Test
    void shippingSelectPresent() {
        navigate(UseCase06View.class);

        assertTrue(findInView(ComboBox.class).all().stream()
                .anyMatch(c -> "Shipping Method".equals(c.getLabel())));
    }

    @Test
    void discountCodeFieldPresent() {
        navigate(UseCase06View.class);

        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Discount Code".equals(f.getLabel())));
    }

    @Test
    void shippingDefaultsToStandard() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        Span shippingLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Shipping:"))
                .findFirst().orElseThrow();
        assertEquals("Shipping: $5.99", shippingLabel.getText());
    }

    @Test
    void totalReflectsShippingWhenCartEmpty() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        Span totalLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Total:"))
                .findFirst().orElseThrow();
        // Subtotal $0 - discount $0 + shipping $5.99 + tax 8% of $0 = $5.99
        assertEquals("Total: $5.99", totalLabel.getText());
    }

    @Test
    void discountCodeSAVE10AppliesDiscount() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        // Add Laptop ($999.99)
        Button addLaptop = findInView(Button.class).all().stream()
                .filter(b -> "Add".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addLaptop).click();
        runPendingSignalsTasks();

        // Enter discount code
        TextField discountField = findInView(TextField.class).all().stream()
                .filter(f -> "Discount Code".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(discountField).setValue("SAVE10");
        runPendingSignalsTasks();

        // Discount label should be visible (> $0)
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null
                        && s.getText().startsWith("Discount:")));
    }

    @Test
    void addSameProductTwiceIncrementsQuantity() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        // Add Laptop ($999.99) once
        Button addLaptop = findInView(Button.class).all().stream()
                .filter(b -> "Add".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addLaptop).click();
        runPendingSignalsTasks();

        Span subtotalLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Subtotal:"))
                .findFirst().orElseThrow();
        assertEquals("Subtotal: $999.99", subtotalLabel.getText());

        // Add same product again — qty incremented, subtotal doubled
        test(addLaptop).click();
        runPendingSignalsTasks();

        assertEquals("Subtotal: $1999.98", subtotalLabel.getText());
    }

    @Test
    void emptyCartResetsSubtotal() {
        navigate(UseCase06View.class);
        runPendingSignalsTasks();

        // Add a product
        Button addLaptop = findInView(Button.class).all().stream()
                .filter(b -> "Add".equals(b.getText())).findFirst()
                .orElseThrow();
        test(addLaptop).click();
        runPendingSignalsTasks();

        // Click "Empty cart"
        Button emptyCartButton = findInView(Button.class).all().stream()
                .filter(b -> "Empty cart".equals(b.getText())).findFirst()
                .orElseThrow();
        test(emptyCartButton).click();
        runPendingSignalsTasks();

        Span subtotalLabel = findInView(Span.class).all().stream().filter(
                s -> s.getText() != null && s.getText().startsWith("Subtotal:"))
                .findFirst().orElseThrow();
        assertEquals("Subtotal: $0.00", subtotalLabel.getText());
    }
}
