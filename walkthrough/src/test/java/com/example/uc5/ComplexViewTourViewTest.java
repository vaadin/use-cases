package com.example.uc5;

import com.example.home.HomeView;
import com.example.tour.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.details.Details;
import com.vaadin.flow.component.tabs.TabSheet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { ComplexViewTourView.class, HomeView.class })
class ComplexViewTourViewTest extends TourTestSupport {

    @Test
    void theTourStartsOnlyOnTheFirstVisitOfTheSession() {
        navigate(ComplexViewTourView.class);
        assertEquals("Filter by status", stepTitle());
        assertEquals("Your first visit in this session, so the tour started.",
                status());

        navigate(HomeView.class);
        navigate(ComplexViewTourView.class);

        assertTrue(openPopovers().isEmpty());
        assertEquals("You were here before in this session, so the tour "
                + "did not start.", status());
    }

    @Test
    void stepsRevealTargetsInClosedPanelsAndOtherTabs() {
        navigate(ComplexViewTourView.class);
        Details moreFilters = findInView(Details.class)
                .withCondition(d -> "More filters".equals(d.getSummaryText()))
                .single();
        TabSheet tabs = findInView(TabSheet.class).single();
        assertFalse(moreFilters.isOpened());

        clickInStep("Next");
        assertInstanceOf(DatePicker.class, stepTarget());
        assertTrue(moreFilters.isOpened());

        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Next");
        assertEquals("Order history", stepTitle());
        assertEquals(1, tabs.getSelectedIndex());
        assertEquals("Export history", ((Button) stepTarget()).getText());

        clickInStep("Back");
        assertEquals("Act on many at once", stepTitle());
        assertEquals(0, tabs.getSelectedIndex());
    }

    @Test
    void theTourCanBeStartedOnALaterVisit() {
        navigate(ComplexViewTourView.class);
        navigate(HomeView.class);
        navigate(ComplexViewTourView.class);

        clickInView("Start the tour");

        assertEquals("Filter by status", stepTitle());
    }
}
