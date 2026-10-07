package com.example.uc2;

import com.example.WakeLockTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RecipeView.class)
class RecipeViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndSteps() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();

        assertTrue(
                findInView(H1.class).all().stream()
                        .anyMatch(h -> h.getText() != null
                                && h.getText().contains("Leek tart")),
                "view should render the recipe heading");

        long stepCount = findInView(Div.class).all().stream().filter(
                d -> d.getElement().getClassList().contains("recipe-step"))
                .count();
        assertTrue(stepCount >= 6,
                "view should render at least 6 recipe steps, was: "
                        + stepCount);
    }

    @Test
    void firstStepIsCurrentInitially() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();

        Div firstStep = findInView(Div.class).all().stream().filter(
                d -> d.getElement().getClassList().contains("recipe-step"))
                .findFirst().orElseThrow();
        assertTrue(firstStep.getElement().getClassList().contains("current"),
                "first step should carry the 'current' class initially");
    }

    @Test
    void clickingNextAdvancesCurrentStep() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();

        Button next = find(Button.class).withText("Next step").single();
        test(next).click();
        runPendingSignalsTasks();

        var steps = findInView(Div.class).all().stream().filter(
                d -> d.getElement().getClassList().contains("recipe-step"))
                .toList();
        assertTrue(steps.get(0).getElement().getClassList().contains("done"),
                "step 1 should be marked done after advancing");
        assertTrue(steps.get(1).getElement().getClassList().contains("current"),
                "step 2 should become current after advancing");
    }

    @Test
    void badgeReflectsSimulatedLockState() {
        navigate(RecipeView.class);
        runPendingSignalsTasks();

        // onAttach has called request(); browser hasn't confirmed yet.
        assertBadgeContains("waiting for browser");

        WakeLockTestSupport.simulateAcquired();
        runPendingSignalsTasks();
        assertBadgeContains("screen will stay on");
    }

    private void assertBadgeContains(String fragment) {
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null && s.getText().toLowerCase()
                        .contains(fragment.toLowerCase())),
                "expected status badge to contain \"" + fragment + "\"");
    }
}
