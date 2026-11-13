package com.example.uc6;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;

import com.vaadin.browserless.BrowserlessApplicationContext;
import com.vaadin.browserless.BrowserlessUIContext;
import com.vaadin.browserless.SpringBrowserlessApplicationContext;
import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Covers rendering, the application-scoped seat inventory shared by several
 * users, and the separate bookings of two tabs of one session. Reloads are not
 * covered yet; a duplicated tab and tab expiry cannot be simulated in these
 * tests; see API-GAPS.md #2.
 */
@SpringBootTest
@ViewPackages(classes = SeatSelectionView.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class SeatSelectionViewTest extends SpringBrowserlessTest {

    @Autowired
    private ApplicationContext applicationContext;

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
        try (var app = multiUser()) {
            var aliceTab = app.newUser().newWindow();
            var aliceOtherTab = aliceTab.getUser().newWindow();
            var bob = app.newUser().newWindow();
            aliceTab.navigate(SeatSelectionView.class);
            aliceOtherTab.navigate(SeatSelectionView.class);
            bob.navigate(SeatSelectionView.class);

            aliceTab.test(seat(aliceTab, "A1")).click();
            aliceTab.runPendingSignalsTasks();
            assertTrue(seat(aliceTab, "A1").hasThemeName("primary"));

            // A second tab of the same session has a booking of its own: the
            // first tab's seat is taken there.
            aliceOtherTab.runPendingSignalsTasks();
            assertFalse(seat(aliceOtherTab, "A1").isEnabled());
            aliceOtherTab.test(seat(aliceOtherTab, "A2")).click();
            aliceOtherTab.runPendingSignalsTasks();
            assertTrue(seat(aliceOtherTab, "A2").hasThemeName("primary"));
            assertTrue(seat(aliceTab, "A1").hasThemeName("primary"));
            assertFalse(seat(aliceTab, "A2").isEnabled());

            // Another user holds B2.
            bob.test(seat(bob, "B2")).click();
            bob.runPendingSignalsTasks();
            assertFalse(seat(bob, "A1").isEnabled());
            assertFalse(seat(aliceTab, "B2").isEnabled());

            // Ending Alice's session destroys her browser tabs; the browser-tab
            // scoped bookings release A1 and A2. B2 is still held by Bob.
            aliceTab.getUser().close();
            bob.runPendingSignalsTasks();
            assertTrue(seat(bob, "A1").isEnabled());
            assertTrue(seat(bob, "A2").isEnabled());
            assertTrue(seat(bob, "B2").hasThemeName("primary"));

            var carol = app.newUser().newWindow();
            carol.navigate(SeatSelectionView.class);
            carol.runPendingSignalsTasks();
            assertTrue(seat(carol, "A1").isEnabled());
            assertFalse(seat(carol, "B2").isEnabled());
        }
    }

    @Test
    void pickingAnotherSeatReleasesTheFirstOne() {
        try (var app = multiUser()) {
            var alice = app.newUser().newWindow();
            var bob = app.newUser().newWindow();
            alice.navigate(SeatSelectionView.class);
            bob.navigate(SeatSelectionView.class);

            alice.test(seat(alice, "A1")).click();
            alice.test(seat(alice, "A2")).click();
            alice.runPendingSignalsTasks();
            assertTrue(seat(alice, "A2").hasThemeName("primary"));

            // A1 is free again: another user can take it.
            bob.runPendingSignalsTasks();
            assertTrue(seat(bob, "A1").isEnabled());
            assertFalse(seat(bob, "A2").isEnabled());
            bob.test(seat(bob, "A1")).click();
            bob.runPendingSignalsTasks();
            assertTrue(seat(bob, "A1").hasThemeName("primary"));
        }
    }

    private BrowserlessApplicationContext multiUser() {
        return SpringBrowserlessApplicationContext.create(applicationContext,
                b -> b.withViewPackages(SeatSelectionView.class)
                        .withSessionLockIsolation(true));
    }

    private static Button seat(BrowserlessUIContext window, String name) {
        return window.find(Button.class).withText(name).single();
    }
}
