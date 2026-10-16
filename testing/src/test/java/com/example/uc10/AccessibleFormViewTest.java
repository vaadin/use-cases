package com.example.uc10;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * What a view test can check: accessible names and the send action. Contrast,
 * focus order and what Enter does where need a real browser:
 * AccessibilityPlaywrightIT.
 */
@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = AccessibleFormView.class)
class AccessibleFormViewTest extends SpringBrowserlessTest {

    @Test
    void iconButtonsHaveAnAccessibleName() {
        navigate(AccessibleFormView.class);

        assertTrue(findInView(Button.class).all().stream()
                .allMatch(b -> !b.getText().isBlank()
                        || b.getElement().getAttribute("aria-label") != null));
    }

    @Test
    void sendConfirmsTheMessage() {
        navigate(AccessibleFormView.class);
        test(findInView(TextField.class).single()).setValue("Ada");

        test(findInView(Button.class).all().stream()
                .filter(b -> "Send".equals(b.getText())).findFirst()
                .orElseThrow()).click();

        assertEquals("Thanks Ada, we will get back to you",
                test($(Notification.class).single()).getText());
    }

    @Test
    void brokenVariantAddsAnUnnamedButton() {
        navigate("uc10?broken", AccessibleFormView.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> b.getText().isBlank()
                        && b.getElement().getAttribute("aria-label") == null));
    }
}
