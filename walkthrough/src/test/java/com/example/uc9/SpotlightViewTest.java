package com.example.uc9;

import com.example.TourTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.applayout.AppLayout;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.sidenav.SideNav;
import com.vaadin.flow.component.textfield.EmailField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = SpotlightView.class)
class SpotlightViewTest extends TourTestSupport {

    @Test
    void theTourSpotlightsEachTargetAndBlocksThePage() {
        navigate(SpotlightView.class);
        clickInView("Start the tour");

        assertTrue(openPopover().isModal());
        assertInstanceOf(SideNav.class, stepTarget());
        Component menu = stepTarget();
        assertTrue(isSpotlit(menu));

        clickInStep("Next");
        assertInstanceOf(EmailField.class, stepTarget());
        assertFalse(isSpotlit(menu));
        assertTrue(isSpotlit(stepTarget()));

        clickInStep("Next");
        assertEquals("Save changes", ((Button) stepTarget()).getText());

        Component save = stepTarget();
        clickInStep("Done");
        assertFalse(isSpotlit(save));
    }

    @Test
    void theTourOpensTheOverlayDrawerForTheMenuAndClosesItAfter() {
        navigate(SpotlightView.class);
        AppLayout layout = (AppLayout) getCurrentView().getElement().getParent()
                .getComponent().orElseThrow();
        // As on a phone: the drawer is an overlay and closed.
        layout.getElement().setProperty("overlay", true);
        layout.setDrawerOpened(false);

        clickInView("Start the tour");
        assertTrue(layout.isDrawerOpened());

        clickInStep("Next");
        assertFalse(layout.isDrawerOpened());
    }

    @Test
    void anOpenDesktopDrawerIsLeftAlone() {
        navigate(SpotlightView.class);
        AppLayout layout = (AppLayout) getCurrentView().getElement().getParent()
                .getComponent().orElseThrow();
        layout.setDrawerOpened(true);

        clickInView("Start the tour");
        clickInStep("Skip tour");

        assertTrue(layout.isDrawerOpened());
    }

    private static boolean isSpotlit(Component component) {
        return component.getElement().getClassList().contains("tour-spotlight");
    }
}
