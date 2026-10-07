package com.example.uc4;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ValidationJumpView.class)
class ValidationJumpViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeading() {
        navigate(ValidationJumpView.class);

        assertTrue(findInView(H1.class).all().stream().anyMatch(
                h -> "UC4 — Jump to validation error".equals(h.getText())));
    }

    @Test
    void invalidCharactersStatusReportsTheBadRange() {
        navigate(ValidationJumpView.class);
        runPendingSignalsTasks();

        TextField username = find(TextField.class).single();
        Button submit = find(Button.class).withText("Submit").single();

        // Default value "My Cool User" — first invalid run is "M".
        test(submit).click();
        runPendingSignalsTasks();

        // Status text reports the offending range; that range, applied to the
        // current value, must contain at least one non-[a-z0-9_] character.
        String status = findInView(Span.class).all().stream().map(Span::getText)
                .filter(t -> t != null
                        && t.startsWith("Invalid characters at "))
                .findFirst().orElseThrow(() -> new AssertionError(
                        "Expected status to start with 'Invalid characters at '"));
        int dash = status.indexOf('–');
        int start = Integer.parseInt(
                status.substring("Invalid characters at ".length(), dash));
        int end = Integer
                .parseInt(status.substring(dash + 1, status.indexOf('.')));
        String run = username.getValue().substring(start, end);
        assertTrue(
                run.chars()
                        .anyMatch(c -> !Character.isDigit(c)
                                && (c < 'a' || c > 'z') && c != '_'),
                "offending run should contain a non-[a-z0-9_] char, was: \""
                        + run + "\"");
    }

    @Test
    void shortValueShowsTheLengthError() {
        navigate(ValidationJumpView.class);
        TextField username = find(TextField.class).single();
        username.setValue("ab");

        Button submit = find(Button.class).withText("Submit").single();
        test(submit).click();
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null
                        && s.getText().contains("at least 3 characters")));
    }

    @Test
    void validValueShowsSuccess() {
        navigate(ValidationJumpView.class);
        TextField username = find(TextField.class).single();
        username.setValue("good_name_42");
        runPendingSignalsTasks();

        Button submit = find(Button.class).withText("Submit").single();
        test(submit).click();
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream().anyMatch(
                s -> s.getText() != null && s.getText().contains("is valid")));
    }
}
