package com.example.usecase14;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.progressbar.ProgressBar;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase14View.class)
@WithMockUser
class UseCase14ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithLoadButton() {
        navigate(UseCase14View.class);

        assertTrue(findInView(Button.class).all().stream().anyMatch(
                b -> "Generate Analytics Report".equals(b.getText())));
    }

    @Test
    void viewRendersWithErrorToggle() {
        navigate(UseCase14View.class);

        assertTrue(findInView(Checkbox.class).all().stream()
                .anyMatch(c -> "Simulate Error".equals(c.getLabel())));
    }

    @Test
    void loadButtonEnabledInitially() {
        navigate(UseCase14View.class);
        runPendingSignalsTasks();

        Button loadButton = findInView(Button.class).all().stream()
                .filter(b -> "Generate Analytics Report".equals(b.getText()))
                .findFirst().orElseThrow();
        assertTrue(loadButton.isEnabled());
    }

    @Test
    void idleStateShownInitially() {
        navigate(UseCase14View.class);
        runPendingSignalsTasks();

        // Idle message should be visible
        assertTrue(findInView(Paragraph.class).all().stream()
                .anyMatch(p -> p.getText() != null
                        && p.getText().contains("Generate Analytics Report")));
    }

    @Test
    void clickLoadButtonTransitionsToLoadingState() {
        navigate(UseCase14View.class);
        runPendingSignalsTasks();

        Button loadButton = findInView(Button.class).all().stream()
                .filter(b -> "Generate Analytics Report".equals(b.getText()))
                .findFirst().orElseThrow();
        test(loadButton).click();
        runPendingSignalsTasks();

        // Button should become disabled during loading
        assertFalse(loadButton.isEnabled());

        // Loading ProgressBar should be visible
        assertTrue(findInView(ProgressBar.class).all().stream()
                .anyMatch(ProgressBar::isIndeterminate));
    }

    @Test
    void errorCheckboxTogglesSignalValue() {
        navigate(UseCase14View.class);
        runPendingSignalsTasks();

        Checkbox errorCheckbox = findInView(Checkbox.class).all().stream()
                .filter(c -> "Simulate Error".equals(c.getLabel())).findFirst()
                .orElseThrow();

        // Initially unchecked
        assertFalse(errorCheckbox.getValue());

        // Toggle it on
        test(errorCheckbox).click();
        runPendingSignalsTasks();

        assertTrue(errorCheckbox.getValue());
    }
}
