package com.example.uc1;

import java.util.ArrayList;
import java.util.List;

import com.example.home.HomeView;
import com.example.tour.TourPopover;
import com.example.tour.TourTestSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Nav;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;
import com.vaadin.flow.component.popover.PopoverTester;
import com.vaadin.flow.component.radiobutton.RadioButtonGroup;
import com.vaadin.flow.component.select.Select;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { AppTourView.class, HomeView.class })
class AppTourViewTest extends TourTestSupport {

    @Autowired
    private SeenVersions seenVersions;

    @BeforeEach
    void forgetAllAccounts() {
        seenVersions.set("Alex", null);
        seenVersions.set("Sam", null);
    }

    @Test
    void aNewUserGetsTheFullTour() {
        navigate(AppTourView.class);

        assertEquals("Alex has never seen the tour, so the full tour opens.",
                status());
        assertEquals("Find your way", stepTitle());
        assertEquals("Step 1 of 5", stepProgress());
        assertInstanceOf(Nav.class, stepTarget());
        assertTrue(isHighlighted(stepTarget()));
        assertFalse(stepHasButton("Back"));

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
        assertEquals("Alex has seen the tour of 2.2 now.", status());
        assertEquals(AppTourView.CURRENT_VERSION, seenVersions.get("Alex"));
        assertEquals(AppTourView.CURRENT_VERSION, seenVersion().getValue());
    }

    @Test
    void backShowsThePreviousStepAndMovesTheHighlight() {
        navigate(AppTourView.class);
        Component first = stepTarget();
        clickInStep("Next");
        assertFalse(isHighlighted(first));

        clickInStep("Back");

        assertEquals("Find your way", stepTitle());
        assertTrue(isHighlighted(first));
    }

    @Test
    void skippingWithEscapeCountsAsSeen() {
        navigate(AppTourView.class);
        PopoverTester popover = new PopoverTester(openPopover());
        // The browser opens the popover once the target is on screen.
        popover.open();

        popover.pressEscape();

        assertTrue(openPopovers().isEmpty());
        assertEquals(AppTourView.CURRENT_VERSION, seenVersions.get("Alex"));

        clickInView("Open the app again");
        assertTrue(openPopovers().isEmpty());
        assertEquals("Alex has seen the tour of 2.2, so no tour opens.",
                status());
    }

    @Test
    void aReturningUserSeesOnlyWhatIsNew() {
        seenVersions.set("Alex", "2.0");
        navigate(AppTourView.class);

        assertEquals("Alex last saw 2.0, so the tour shows only what is new "
                + "since then.", status());
        assertEquals("Step 1 of 3", stepProgress());
        assertEquals("Saved filters", stepTitle());
        assertEquals(List.of("Saved filters", "Export", "Dark mode"), badged());

        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Done");

        assertEquals(List.of(), badged());
        assertEquals(AppTourView.CURRENT_VERSION, seenVersions.get("Alex"));
    }

    @Test
    void pickingTheVersionSeenStoresItAndOpensTheAppAgain() {
        navigate(AppTourView.class);

        test(seenVersion()).selectItem("2.1");

        assertEquals("2.1", seenVersions.get("Alex"));
        assertEquals("Step 1 of 1", stepProgress());
        assertEquals("Dark mode", stepTitle());
        assertEquals(List.of("Dark mode"), badged());

        test(seenVersion()).selectItem("Never");

        assertNull(seenVersions.get("Alex"));
        assertEquals("Step 1 of 5", stepProgress());
        assertEquals(List.of(), badged());
    }

    @Test
    void anotherAccountStillGetsTheTour() {
        seenVersions.set("Alex", AppTourView.CURRENT_VERSION);
        navigate(AppTourView.class);
        assertTrue(openPopovers().isEmpty());

        test(findInView(Select.class).withLabel("Signed in as").single())
                .selectItem("Sam");

        assertEquals("Find your way", stepTitle());
        assertEquals("Sam has never seen the tour, so the full tour opens.",
                status());
    }

    @Test
    void switchingTheAccountDuringTheTourDoesNotCountAsSeen() {
        navigate(AppTourView.class);

        test(findInView(Select.class).withLabel("Signed in as").single())
                .selectItem("Sam");

        assertNull(seenVersions.get("Alex"));
        assertNull(seenVersions.get("Sam"));
        assertEquals("Find your way", stepTitle());
    }

    @Test
    void anAccountSeenInAnotherSessionGetsNoTour() {
        navigate(AppTourView.class);
        clickInStep("Skip tour");

        // A second browser of the same account.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(AppTourView.class);

        assertTrue(openPopovers().isEmpty());
        assertEquals("Alex has seen the tour of 2.2, so no tour opens.",
                status());
    }

    @Test
    void theHelpMenuReplaysTheFullTourAndResumesASkippedOne() {
        seenVersions.set("Alex", AppTourView.CURRENT_VERSION);
        navigate(AppTourView.class);
        assertThrows(IllegalStateException.class,
                () -> help().clickItem("Help", "Resume the tour"),
                "Resume is disabled before a tour was skipped");

        help().clickItem("Help", "Take the tour");
        assertEquals("Step 1 of 5", stepProgress());
        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Skip tour");

        help().clickItem("Help", "Resume the tour");

        assertEquals("Create an order", stepTitle());
        assertEquals("Step 3 of 5", stepProgress());
    }

    @Test
    @SuppressWarnings("unchecked")
    void perBrowserTheTourWaitsForTheBrowsersAnswer() {
        navigate(AppTourView.class);

        test(findInView(RadioButtonGroup.class).single())
                .selectItem("Per browser, in local storage");

        // Local storage is read in the browser, which browserless tests do
        // not have (API-GAPS.md #5): the tour must not open before it answers.
        assertTrue(openPopovers().isEmpty());
        assertEquals("Asking the browser which version it has seen…", status());
    }

    @Test
    void leavingThePageClosesTheTour() {
        navigate(AppTourView.class);
        TourPopover popover = find(TourPopover.class).single();
        Component target = stepTarget();

        navigate(HomeView.class);

        assertFalse(isShowing(popover));
        assertFalse(isHighlighted(target));
    }

    @SuppressWarnings("unchecked")
    private Select<String> seenVersion() {
        return findInView(Select.class).withLabel("Last version seen").single();
    }

    private MenuBarTester<MenuBar> help() {
        runPendingSignalsTasks();
        return test(findInView(MenuBar.class).single());
    }

    private List<String> badged() {
        return findInView(Button.class).withClassName("tour-new").all().stream()
                .map(b -> b.getText().isEmpty() ? b.getTooltip().getText()
                        : b.getText())
                .toList();
    }

    private static boolean isHighlighted(Component component) {
        return component.getElement().getClassList().contains("tour-highlight");
    }
}
