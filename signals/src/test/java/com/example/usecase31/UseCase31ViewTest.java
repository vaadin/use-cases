package com.example.usecase31;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase31View.class)
@WithMockUser
class UseCase31ViewTest extends SpringBrowserlessTest {

    @Test
    void allSeatsStartAvailable() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        long available = $view(Span.class).all().stream()
                .filter(s -> "Available".equals(s.getText())).count();
        assertEquals(8L, available, "All 8 seats should be available");
    }

    @Test
    void claimingAnEmptySeatSucceedsAndBumpsCounter() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        UseCase31View view = (UseCase31View) getCurrentView();
        int countBefore = view.reservationCount.peek();

        // Direct seat mutation models a successful first-come claim
        assertTrue(view.seats.get(2).replace(null, "Attendee A"),
                "Empty seat must accept a claim");
        view.reservationCount.update(n -> n + 1);
        runPendingSignalsTasks();

        assertEquals(countBefore + 1, view.reservationCount.peek(),
                "Counter must bump after a successful claim");
    }

    @Test
    void secondClaimOnTakenSeatFails() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        UseCase31View view = (UseCase31View) getCurrentView();
        view.seats.get(0).replace(null, "Attendee A");
        runPendingSignalsTasks();

        assertFalse(view.seats.get(0).replace(null, "Attendee B"),
                "Second claim must fail because seat is already taken");
        assertEquals("Attendee A", view.seats.get(0).peek(),
                "Original holder retained");
    }

    @Test
    void contendedRaceProducesExactlyOneWinner() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        Button race = $view(Button.class).all().stream()
                .filter(b -> b.getText() != null
                        && b.getText().startsWith("Reset seat 1"))
                .findFirst().orElseThrow();
        test(race).click();
        runPendingSignalsTasks();

        assertTrue($view(Span.class).all().stream()
                .anyMatch(s -> "Last race result: 1 device claimed seat 1"
                        .equals(s.getText())),
                "Exactly one of the 4 virtual-thread devices must win");
    }

    @Test
    void concurrentReservationBumpsAreAllCounted() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        UseCase31View view = (UseCase31View) getCurrentView();
        int before = view.reservationCount.peek();

        Button burst = $view(Button.class).all().stream()
                .filter(b -> b.getText() != null
                        && b.getText().startsWith("Simulate 6 admin tools"))
                .findFirst().orElseThrow();
        test(burst).click();
        runPendingSignalsTasks();

        assertEquals(before + 6, view.reservationCount.peek(),
                "Six concurrent update(old -> old + 1) calls must all stick");
    }
}
