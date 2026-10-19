package com.example.uc4;

import java.util.Optional;

import com.example.TextSelectionTestSupport.Requested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static com.example.TextSelectionTestSupport.clickAndReadSelection;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = ValidationJumpView.class)
class ValidationJumpViewTest extends SpringBrowserlessTest {

    @Test
    void submitSelectsTheFirstInvalidPartAndThenTheNextOne() {
        navigate(ValidationJumpView.class);
        TextField username = find(TextField.class).single();
        Button submit = find(Button.class).withText("Submit").single();

        // "My Cool User": the capital M is the first invalid part.
        assertEquals(Optional.of(new Requested(0, 1)),
                clickAndReadSelection(submit, username));
        assertEquals("Invalid characters at 0–1. Retype to fix.", status());

        // After fixing it, the space is next.
        test(username).setValue("my Cool User");
        assertEquals(Optional.of(new Requested(2, 4)),
                clickAndReadSelection(submit, username));
    }

    @Test
    void tooShortValueIsSelectedWhole() {
        navigate(ValidationJumpView.class);
        TextField username = find(TextField.class).single();
        test(username).setValue("ab");

        assertEquals(Optional.of(new Requested(0, 2)), clickAndReadSelection(
                find(Button.class).withText("Submit").single(), username));
        assertEquals("Username must be at least 3 characters.", status());
    }

    @Test
    void validValueSelectsNothing() {
        navigate(ValidationJumpView.class);
        TextField username = find(TextField.class).single();
        test(username).setValue("good_name_42");

        assertEquals(Optional.empty(), clickAndReadSelection(
                find(Button.class).withText("Submit").single(), username));
        assertEquals("\"good_name_42\" is valid.", status());
    }

    private String status() {
        return find(Span.class).withClassName("uc-status").single().getText();
    }
}
