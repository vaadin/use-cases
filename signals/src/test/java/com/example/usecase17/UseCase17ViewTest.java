package com.example.usecase17;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase17View.class)
@WithMockUser
class UseCase17ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithComboBoxes() {
        navigate(UseCase17View.class);

        // CPU, Motherboard, RAM, GPU, Primary Storage, Secondary Storage,
        // PSU, Case, CPU Cooler = 9
        assertEquals(9, findInView(ComboBox.class).all().size());
    }

    @Test
    void totalPriceIsZeroInitially() {
        navigate(UseCase17View.class);
        runPendingSignalsTasks();

        // Total price formatted as "$0" (using %.0f format)
        Span totalLabel = findInView(Span.class).all().stream()
                .filter(s -> "$0".equals(s.getText())).findFirst()
                .orElseThrow();
        assertEquals("$0", totalLabel.getText());
    }

    @SuppressWarnings("unchecked")
    @Test
    void selectingCpuUpdatesPrice() {
        navigate(UseCase17View.class);
        runPendingSignalsTasks();

        ComboBox<CPU> cpuSelect = (ComboBox<CPU>) findInView(ComboBox.class)
                .all().stream().filter(c -> "CPU".equals(c.getLabel()))
                .findFirst().orElseThrow();
        test(cpuSelect).selectItem("Intel Core i5-14600K");
        runPendingSignalsTasks();

        // Total price should now be $319 (i5-14600K price)
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "$319".equals(s.getText())));
    }

    @SuppressWarnings("unchecked")
    @Test
    void socketIncompatibilityReducesCheckCount() {
        navigate(UseCase17View.class);
        runPendingSignalsTasks();

        // Select Intel CPU (LGA1700)
        ComboBox<CPU> cpuSelect = (ComboBox<CPU>) findInView(ComboBox.class)
                .all().stream().filter(c -> "CPU".equals(c.getLabel()))
                .findFirst().orElseThrow();
        test(cpuSelect).selectItem("Intel Core i5-14600K");

        // Select AMD motherboard (AM5 socket) - socket mismatch
        ComboBox<Motherboard> mbSelect = (ComboBox<Motherboard>) findInView(
                ComboBox.class).all().stream()
                .filter(c -> "Motherboard".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(mbSelect).selectItem("ASUS ROG X670E");
        runPendingSignalsTasks();

        // Compatibility count should drop below 13 - find the checks passing
        // span
        assertTrue(findInView(Span.class).all().stream().anyMatch(s -> {
            String text = s.getText();
            return text != null && text.contains("checks passing")
                    && !text.contains("13/13");
        }));
    }

    @Test
    void powerConsumptionLabelPresent() {
        navigate(UseCase17View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Power Consumption".equals(s.getText())));
    }
}
