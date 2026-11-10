package com.example.uc1;

import java.util.ArrayList;
import java.util.List;

import com.example.home.HomeView;
import com.example.tour.TourPopover;
import com.example.tour.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.popover.PopoverTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { FirstRunTourView.class, HomeView.class })
class FirstRunTourViewTest extends TourTestSupport {

    @Test
    void tourOpensWithThePageAndWalksThroughFiveSteps() {
        navigate(FirstRunTourView.class);

        assertEquals("Find your way", stepTitle());
        assertEquals("Step 1 of 5", stepProgress());
        assertInstanceOf(Nav.class, stepTarget());
        assertTrue(isHighlighted(stepTarget()));
        assertFalse(stepHasButton("Back"));
        assertEquals("At step 1 of 5", status());

        List<String> titles = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            clickInStep("Next");
            titles.add(stepTitle());
        }
        assertEquals(List.of("Search", "Create an order", "Your orders",
                "Your account"), titles);
        assertInstanceOf(Avatar.class, stepTarget());
        Component last = stepTarget();

        clickInStep("Done");

        assertTrue(openPopovers().isEmpty());
        assertFalse(isHighlighted(last));
        assertEquals("Finished", status());
    }

    @Test
    void backShowsThePreviousStepAndMovesTheHighlight() {
        navigate(FirstRunTourView.class);
        Component first = stepTarget();
        clickInStep("Next");
        assertFalse(isHighlighted(first));

        clickInStep("Back");

        assertEquals("Find your way", stepTitle());
        assertTrue(isHighlighted(first));
    }

    @Test
    void skippingRemembersTheStep() {
        navigate(FirstRunTourView.class);
        clickInStep("Next");

        clickInStep("Skip tour");

        assertTrue(openPopovers().isEmpty());
        assertEquals("Skipped at step 2 of 5", status());
    }

    @Test
    void escapeSkipsTheTour() {
        navigate(FirstRunTourView.class);

        PopoverTester popover = new PopoverTester(openPopover());
        // The browser opens the popover once the target is on screen.
        popover.open();

        popover.pressEscape();

        assertEquals("Skipped at step 1 of 5", status());
    }

    @Test
    void theTourCanBeStartedAgain() {
        navigate(FirstRunTourView.class);
        clickInStep("Skip tour");

        clickInView("Start the tour again");

        assertEquals("Find your way", stepTitle());
        assertEquals("At step 1 of 5", status());
    }

    @Test
    void leavingThePageClosesTheTour() {
        navigate(FirstRunTourView.class);
        TourPopover popover = find(TourPopover.class).single();
        Component target = stepTarget();

        navigate(HomeView.class);

        assertFalse(isShowing(popover));
        assertFalse(isHighlighted(target));
    }

    private static boolean isHighlighted(Component component) {
        return component.getElement().getClassList().contains("tour-highlight");
    }
}
