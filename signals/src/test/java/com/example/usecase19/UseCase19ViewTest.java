package com.example.usecase19;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.progressbar.ProgressBar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase19View.class)
@WithMockUser
class UseCase19ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSixDataCards() {
        navigate(UseCase19View.class);
        runPendingSignalsTasks();

        assertEquals(6, findInView(Card.class).all().size());
    }

    @Test
    void loadButtonPresent() {
        navigate(UseCase19View.class);

        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Load All Items".equals(b.getText())));
    }

    @Test
    void errorCheckboxPresent() {
        navigate(UseCase19View.class);

        assertTrue(findInView(Checkbox.class).all().stream()
                .anyMatch(c -> "Simulate Random Errors".equals(c.getLabel())));
    }

    @Test
    void clickLoadAllTransitionsCardsToLoading() {
        navigate(UseCase19View.class);
        runPendingSignalsTasks();

        // Click "Load All Items"
        Button loadButton = findInView(Button.class).all().stream()
                .filter(b -> "Load All Items".equals(b.getText())).findFirst()
                .orElseThrow();
        test(loadButton).click();
        runPendingSignalsTasks();

        // All 6 cards should transition to LOADING — ProgressBars become
        // visible
        assertTrue(findInView(ProgressBar.class).all().stream()
                .anyMatch(ProgressBar::isIndeterminate));
    }

    @Test
    void cardBorderColorReflectsItemState() {
        navigate(UseCase19View.class);
        runPendingSignalsTasks();

        Card card = findInView(Card.class).first();

        // IDLE state should carry the state-idle class (rendered via CSS)
        assertTrue(card.getClassNames().contains("state-idle"),
                "IDLE card should have state-idle class but was: "
                        + card.getClassNames());
    }

    @Test
    void cardBorderColorUpdatesOnStateChange() {
        navigate(UseCase19View.class);
        runPendingSignalsTasks();

        // Click Load All to transition items to LOADING
        Button loadButton = findInView(Button.class).all().stream()
                .filter(b -> "Load All Items".equals(b.getText())).findFirst()
                .orElseThrow();
        test(loadButton).click();
        runPendingSignalsTasks();

        // Cards in LOADING state should carry the state-loading class
        Card card = findInView(Card.class).first();
        assertTrue(card.getClassNames().contains("state-loading"),
                "LOADING card should have state-loading class but was: "
                        + card.getClassNames());
    }

    @Test
    void errorCheckboxTogglesSignalValue() {
        navigate(UseCase19View.class);
        runPendingSignalsTasks();

        Checkbox errorCheckbox = findInView(Checkbox.class).all().stream()
                .filter(c -> "Simulate Random Errors".equals(c.getLabel()))
                .findFirst().orElseThrow();

        // Initially unchecked
        assertFalse(errorCheckbox.getValue());

        // Toggle on
        test(errorCheckbox).click();
        runPendingSignalsTasks();

        assertTrue(errorCheckbox.getValue());
    }
}
