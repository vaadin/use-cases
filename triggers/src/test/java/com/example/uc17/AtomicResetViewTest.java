package com.example.uc17;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = AtomicResetView.class)
class AtomicResetViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithThreeFieldsAndResetButton() {
        navigate(AtomicResetView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC17 — Atomic reset".equals(h.getText())));
        assertNotNull(findInView(TextField.class).id("name"));
        assertNotNull(findInView(TextField.class).id("email"));
        assertNotNull(findInView(Checkbox.class).id("subscribe"));
        assertNotNull(findInView(Button.class).id("reset"));
    }
}
