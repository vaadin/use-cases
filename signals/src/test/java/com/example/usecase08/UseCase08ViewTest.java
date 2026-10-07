package com.example.usecase08;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.EmailField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase08View.class)
@WithMockUser
class UseCase08ViewTest extends SpringBrowserlessTest {

    // findInView() only returns VISIBLE components. Hidden step layouts
    // and hidden buttons (Previous, Submit) are not found.

    @Test
    void viewRendersAtStep1() {
        navigate(UseCase08View.class);
        runPendingSignalsTasks();

        // Step 1 fields visible
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "First Name".equals(f.getLabel())));
        assertEquals(1, findInView(EmailField.class).all().size());

        // Step 2 fields NOT visible
        assertFalse(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Company Name".equals(f.getLabel())));
    }

    @Test
    void progressIndicatorShowsStep1() {
        navigate(UseCase08View.class);
        runPendingSignalsTasks();

        Span progress = findInView(Span.class).all().stream()
                .filter(s -> s.getText().contains("Step")).findFirst()
                .orElseThrow();
        assertEquals("Step 1 of 4", progress.getText());
    }

    @Test
    void nextButtonDisabledOnEmptyStep1() {
        navigate(UseCase08View.class);
        runPendingSignalsTasks();

        // On step 1: only "Next" button is visible (Previous/Submit are hidden)
        Button nextButton = findInView(Button.class).all().stream()
                .filter(b -> "Next".equals(b.getText())).findFirst()
                .orElseThrow();
        assertFalse(nextButton.isEnabled());
    }

    @Test
    void previousButtonHiddenOnStep1() {
        navigate(UseCase08View.class);
        runPendingSignalsTasks();

        // Previous button has bindVisible(step != PERSONAL_INFO),
        // so it's hidden on step 1 and NOT found by findInView()
        assertFalse(findInView(Button.class).all().stream()
                .anyMatch(b -> "Previous".equals(b.getText())));
    }

    @Test
    void nextButtonEnabledWithValidStep1() {
        navigate(UseCase08View.class);
        fillStep1();

        Button nextButton = findInView(Button.class).all().stream()
                .filter(b -> "Next".equals(b.getText())).findFirst()
                .orElseThrow();
        assertTrue(nextButton.isEnabled());
    }

    @Test
    void navigateToStep2() {
        navigate(UseCase08View.class);
        fillStep1();
        clickNext();

        // Step 2 fields visible
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Company Name".equals(f.getLabel())));
        // Step 1 fields hidden
        assertFalse(findInView(TextField.class).all().stream()
                .anyMatch(f -> "First Name".equals(f.getLabel())));
        // Previous button now visible
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Previous".equals(b.getText())));
    }

    @Test
    void previousButtonNavigatesBack() {
        navigate(UseCase08View.class);
        fillStep1();
        clickNext();

        Button previousButton = findInView(Button.class).all().stream()
                .filter(b -> "Previous".equals(b.getText())).findFirst()
                .orElseThrow();
        test(previousButton).click();
        runPendingSignalsTasks();

        // Step 1 fields visible again
        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "First Name".equals(f.getLabel())));
    }

    @Test
    void submitButtonVisibleOnStep4() {
        navigate(UseCase08View.class);
        fillStep1();
        clickNext();
        fillStep2();
        clickNext();
        // Step 3 - plan is pre-selected as STARTER, valid
        clickNext();

        // Submit button visible on step 4
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Submit".equals(b.getText())));
        // Next button hidden on step 4
        assertFalse(findInView(Button.class).all().stream()
                .anyMatch(b -> "Next".equals(b.getText())));
    }

    private void fillStep1() {
        TextField firstName = findInView(TextField.class).all().stream()
                .filter(f -> "First Name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        TextField lastName = findInView(TextField.class).all().stream()
                .filter(f -> "Last Name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        EmailField email = findInView(EmailField.class).single();

        test(firstName).setValue("John");
        test(lastName).setValue("Doe");
        test(email).setValue("john@example.com");
        runPendingSignalsTasks();
    }

    @SuppressWarnings("unchecked")
    private void fillStep2() {
        TextField companyName = findInView(TextField.class).all().stream()
                .filter(f -> "Company Name".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(companyName).setValue("Acme Corp");

        ComboBox<String> companySize = findInView(ComboBox.class).all().stream()
                .filter(c -> "Company Size".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(companySize).selectItem("11-50");

        ComboBox<String> industry = findInView(ComboBox.class).all().stream()
                .filter(c -> "Industry".equals(c.getLabel())).findFirst()
                .orElseThrow();
        test(industry).selectItem("Technology");
        runPendingSignalsTasks();
    }

    private void clickNext() {
        Button nextButton = findInView(Button.class).all().stream()
                .filter(b -> "Next".equals(b.getText())).findFirst()
                .orElseThrow();
        test(nextButton).click();
        runPendingSignalsTasks();
    }
}
