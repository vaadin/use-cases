package com.example.uc3;

import com.example.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.menubar.MenuBar;
import com.vaadin.flow.component.menubar.MenuBarTester;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ReplayFromHelpView.class)
class ReplayFromHelpViewTest extends TourTestSupport {

    @Test
    void theTourWaitsForTheHelpMenu() {
        navigate(ReplayFromHelpView.class);
        assertTrue(openPopovers().isEmpty());

        help().clickItem("Help", "Take the tour");

        assertEquals("Find your way", stepTitle());
    }

    @Test
    void resumeContinuesAtTheSkippedStep() {
        navigate(ReplayFromHelpView.class);
        assertThrows(IllegalStateException.class,
                () -> help().clickItem("Help", "Resume the tour"),
                "Resume is disabled before the tour was skipped");

        help().clickItem("Help", "Take the tour");
        clickInStep("Next");
        clickInStep("Next");
        clickInStep("Skip tour");
        assertEquals("Skipped at step 3 of 5", status());

        help().clickItem("Help", "Resume the tour");

        assertEquals("Create an order", stepTitle());
        assertEquals("Step 3 of 5", stepProgress());
    }

    @Test
    void takeTheTourStartsFromTheFirstStepAfterASkip() {
        navigate(ReplayFromHelpView.class);
        help().clickItem("Help", "Take the tour");
        clickInStep("Next");
        clickInStep("Skip tour");

        help().clickItem("Help", "Take the tour");

        assertEquals("Step 1 of 5", stepProgress());
    }

    private MenuBarTester<MenuBar> help() {
        runPendingSignalsTasks();
        return test(findInView(MenuBar.class).single());
    }
}
