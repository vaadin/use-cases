package com.example.usecase31;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ClickEvent;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
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
    void firstClaimWinsTheSeatAndLaterClaimsAreRejected() {
        navigate(UseCase31View.class);
        runPendingSignalsTasks();

        UseCase31View view = (UseCase31View) getCurrentView();
        TextField name = $view(TextField.class).single();

        test(name).setValue("Attendee A");
        clickSeat(3);
        runPendingSignalsTasks();

        assertEquals("Attendee A", view.seats.get(2).peek());
        assertTrue(hasSpan("Total: 1"), "The tally counts the claim");

        test(name).setValue("Attendee B");
        clickSeat(3);
        runPendingSignalsTasks();

        assertEquals("Attendee A", view.seats.get(2).peek(),
                "The first holder keeps the seat");
        assertTrue(hasSpan("Total: 1"),
                "A rejected claim does not count as a reservation");
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

    private void clickSeat(int seatNumber) {
        Div tile = (Div) $view(Span.class).all().stream()
                .filter(s -> ("Seat " + seatNumber).equals(s.getText()))
                .findFirst().orElseThrow().getParent().orElseThrow();
        ComponentUtil.fireEvent(tile, new ClickEvent<>(tile));
    }

    private boolean hasSpan(String text) {
        return $view(Span.class).all().stream()
                .anyMatch(s -> text.equals(s.getText()));
    }
}
