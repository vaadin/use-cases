package com.example.usecase21;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase21View.class)
@WithMockUser
class UseCase21ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithFormFields() {
        navigate(UseCase21View.class);

        assertEquals(1, findInView(TextField.class).all().size());
        assertEquals(1, findInView(EmailField.class).all().size());
    }

    @Test
    void viewRendersWithButtons() {
        navigate(UseCase21View.class);
        runPendingSignalsTasks();

        // Submit and Cancel buttons (text from i18n signals)
        assertEquals(2, findInView(Button.class).all().size());
    }

    @Test
    void buttonTextsAreEnglishTranslations() {
        navigate(UseCase21View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Submit".equals(b.getText())));
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Cancel".equals(b.getText())));
    }

    @Test
    void formFieldLabelsAreEnglishTranslations() {
        navigate(UseCase21View.class);
        runPendingSignalsTasks();

        TextField nameField = findInView(TextField.class).single();
        assertEquals("Name", nameField.getLabel());

        EmailField emailField = findInView(EmailField.class).single();
        assertEquals("Email", emailField.getLabel());
    }

    @Test
    void formFieldPlaceholdersAreEnglishTranslations() {
        navigate(UseCase21View.class);
        runPendingSignalsTasks();

        TextField nameField = findInView(TextField.class).single();
        assertEquals("Enter your name", nameField.getPlaceholder());

        EmailField emailField = findInView(EmailField.class).single();
        assertEquals("Enter your email address", emailField.getPlaceholder());
    }
}
