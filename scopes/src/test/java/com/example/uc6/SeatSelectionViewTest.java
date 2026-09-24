package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers rendering and the application-scoped seat inventory. Browser-tab
 * behaviour (reload, second tab, tab expiry) cannot be simulated in browserless
 * tests yet; see API-GAPS.md.
 */
@SpringBootTest
@ViewPackages(classes = SeatSelectionView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SeatSelectionViewTest extends SpringBrowserlessTest {

    @Autowired
    private SeatInventory inventory;

    @Test
    void viewRendersStepsAndSeats() {
        navigate(SeatSelectionView.class);
        runPendingSignalsTasks();

        assertEquals("UC6 — Booking wizard",
                findInView(H1.class).single().getText());
        assertTrue(findInView(Button.class).all().stream()
                .anyMatch(b -> "Reload this page".equals(b.getText())));
        assertEquals(SeatInventory.SEATS.size(),
                findInView(Button.class).all().stream()
                        .filter(b -> SeatInventory.SEATS.contains(b.getText()))
                        .count());
    }

    @Test
    void heldSeatsAreSharedAndReleasedWhenTheSessionEnds() {
        navigate(SeatSelectionView.class);
        runPendingSignalsTasks();

        test(seat("A1")).click();
        runPendingSignalsTasks();
        assertTrue(seat("A1").hasThemeName("primary"));

        // Another user holds B2 through the shared inventory.
        inventory.hold("B2", "someone-else");
        runPendingSignalsTasks();
        assertFalse(seat("B2").isEnabled());

        // Ending the session destroys its browser tabs; the browser-tab
        // scoped booking releases A1. B2 is still held by the other user.
        cleanVaadinEnvironment();
        initVaadinEnvironment();
        navigate(SeatSelectionView.class);
        runPendingSignalsTasks();
        assertTrue(seat("A1").isEnabled());
        assertFalse(seat("B2").isEnabled());
    }

    private Button seat(String name) {
        return findInView(Button.class).all().stream()
                .filter(b -> name.equals(b.getText())).findFirst()
                .orElseThrow();
    }
}
