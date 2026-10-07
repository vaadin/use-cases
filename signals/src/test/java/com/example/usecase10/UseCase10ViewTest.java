package com.example.usecase10;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.upload.Upload;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase10View.class)
@WithMockUser
class UseCase10ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithUploadComponent() {
        navigate(UseCase10View.class);

        assertEquals(1, findInView(Upload.class).all().size());
    }

    @Test
    void uploadStatusShowsIdleInitially() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText() != null && p.getText()
                        .contains("Idle — no upload in progress")));
    }

    @Test
    void shortcutLabelShowsInitialText() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "None pressed yet".equals(s.getText())));
    }

    @Test
    void darkModeCallableUpdatesDarkModeSignal() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        // Initially light mode
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Light Mode".equals(s.getText())));

        // Simulate OS dark mode change via @ClientCallable
        UseCase10View view = (UseCase10View) getCurrentView();
        view.onDarkModeChange(true);
        runPendingSignalsTasks();

        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Dark Mode".equals(s.getText())));
    }

    @Test
    void darkModeToggleBackToLight() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        UseCase10View view = (UseCase10View) getCurrentView();
        view.onDarkModeChange(true);
        runPendingSignalsTasks();
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Dark Mode".equals(s.getText())));

        view.onDarkModeChange(false);
        runPendingSignalsTasks();
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "Light Mode".equals(s.getText())));
    }

    @Test
    void composedSummaryReflectsDarkModeChange() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        // Default summary should contain "Theme: light"
        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText() != null
                        && p.getText().contains("Theme: light")));

        // Toggle dark mode
        UseCase10View view = (UseCase10View) getCurrentView();
        view.onDarkModeChange(true);
        runPendingSignalsTasks();

        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText() != null
                        && p.getText().contains("Theme: dark")));
    }

    @Test
    void composedSummaryContainsAllThreeParts() {
        navigate(UseCase10View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText() != null
                        && p.getText().contains("Upload:")
                        && p.getText().contains("Shortcut:")
                        && p.getText().contains("Theme:")));
    }
}
