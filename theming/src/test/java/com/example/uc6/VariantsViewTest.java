package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = VariantsView.class)
class VariantsViewTest extends SpringBrowserlessTest {

    @Test
    void rendersVariantsAndCustomStyles() {
        navigate(VariantsView.class);
        runPendingSignalsTasks();

        assertEquals("UC6 — Variants or custom CSS",
                findInView(H1.class).single().getText());
        assertTrue(button("Primary").getThemeNames().contains("primary"));
        assertTrue(button("Upgrade plan").hasClassName("gradient"));
        assertTrue(
                button("Aura accent class").hasClassName("aura-accent-purple"));
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Current theme: Aura".equals(s.getText())));
    }

    @Test
    void notificationVariantButtonsShowNotifications() {
        navigate(VariantsView.class);

        test(button("Show \"Could not save\"")).click();

        Notification notification = $(Notification.class).single();
        assertEquals("Could not save", test(notification).getText());
        assertTrue(notification.getThemeNames().contains("error"));
    }

    private Button button(String text) {
        return findInView(Button.class).all().stream()
                .filter(b -> text.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
