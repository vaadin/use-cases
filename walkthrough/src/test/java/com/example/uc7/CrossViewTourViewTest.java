package com.example.uc7;

import com.example.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Nav;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CrossViewTourView.class)
class CrossViewTourViewTest extends TourTestSupport {

    @Test
    void theTourContinuesOnTheNextPage() {
        navigate(CrossViewTourView.class);
        clickInView("Start the tour");
        assertEquals("Your day at a glance", stepTitle());

        clickInStep("Next");
        assertEquals("Three pages", stepTitle());
        assertInstanceOf(Nav.class, stepTarget());

        clickInStep("Next");
        assertInstanceOf(CrossViewOrdersView.class, getCurrentView());
        assertEquals("All orders", stepTitle());
        assertEquals("Step 3 of 4", stepProgress());
        assertInstanceOf(Grid.class, stepTarget());

        clickInStep("Next");
        assertInstanceOf(CrossViewReportsView.class, getCurrentView());
        assertEquals("Monthly report", stepTitle());

        clickInStep("Done");
        assertTrue(openPopovers().isEmpty());
        assertEquals("Finished", status());
    }

    @Test
    void backGoesToThePreviousPage() {
        navigate(CrossViewTourView.class);
        clickInView("Start the tour");
        clickInStep("Next");
        clickInStep("Next");

        clickInStep("Back");

        assertInstanceOf(CrossViewTourView.class, getCurrentView());
        assertEquals("Three pages", stepTitle());
    }

    @Test
    void comingBackToTheStepsPageShowsTheStepAgain() {
        navigate(CrossViewTourView.class);
        clickInView("Start the tour");

        navigate(CrossViewReportsView.class);
        assertTrue(openPopovers().isEmpty());
        assertEquals("At step 1 of 4", status());

        navigate(CrossViewTourView.class);
        assertEquals("Your day at a glance", stepTitle());
    }
}
