package com.example.uc2;

import com.example.TourTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ShowOnceView.class)
class ShowOnceViewTest extends TourTestSupport {

    @Autowired
    private SeenTours seenTours;

    @BeforeEach
    void forgetAllAccounts() {
        seenTours.forget("Alex");
        seenTours.forget("Sam");
    }

    @Test
    void theTourOpensUntilTheAccountHasSeenIt() {
        navigate(ShowOnceView.class);
        assertEquals("Find your way", stepTitle());
        assertEquals("Alex has not seen the tour yet.", status());

        clickInStep("Skip tour");
        assertEquals("Alex has seen the tour now.", status());

        clickInView("Open the app again");
        assertTrue(openPopovers().isEmpty());
        assertEquals("Alex has seen the tour, so it stays closed.", status());
    }

    @Test
    void anotherAccountStillGetsTheTour() {
        seenTours.markSeen("Alex");
        navigate(ShowOnceView.class);
        assertTrue(openPopovers().isEmpty());

        test(findInView(Select.class).single()).selectItem("Sam");

        assertEquals("Find your way", stepTitle());
        assertEquals("Sam has not seen the tour yet.", status());
    }

    @Test
    void switchingTheAccountDuringTheTourDoesNotCountAsSeen() {
        navigate(ShowOnceView.class);

        test(findInView(Select.class).single()).selectItem("Sam");

        assertFalse(seenTours.hasSeen("Alex"));
    }

    @Test
    void anAccountSeenInAnotherSessionGetsNoTour() {
        navigate(ShowOnceView.class);
        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Done");

        // A second browser of the same account.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(ShowOnceView.class);

        assertTrue(openPopovers().isEmpty());
        assertEquals("Alex has seen the tour, so it stays closed.", status());
    }

    @Test
    void forgettingOpensTheTourAgain() {
        seenTours.markSeen("Alex");
        navigate(ShowOnceView.class);

        clickInView("Forget that it was seen");
        clickInView("Open the app again");

        assertEquals("Find your way", stepTitle());
    }

    @Test
    @SuppressWarnings("unchecked")
    void perBrowserTheTourWaitsForTheBrowsersAnswer() {
        navigate(ShowOnceView.class);

        test(findInView(RadioButtonGroup.class).single())
                .selectItem("Per browser, in local storage");

        // Local storage is read in the browser, which browserless tests do
        // not have (API-GAPS.md #5): the tour must not open before it answers.
        assertTrue(openPopovers().isEmpty());
        assertEquals("Asking the browser whether the tour was seen…", status());
    }
}
