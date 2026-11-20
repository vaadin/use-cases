package com.example.uc1;

import java.time.LocalDate;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = BookAStayView.class)
class BookAStayViewTest extends SpringBrowserlessTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void aFreeStayIsPricedAndCanBeBooked() {
        navigate(BookAStayView.class);
        pick(today.plusDays(1), today.plusDays(3));

        assertFalse(picker().isInvalid());
        assertTrue(summary().startsWith("2 nights,"), summary());
        assertTrue(summary().endsWith("240.00 EUR"), summary());
        assertTrue(book().isEnabled());
    }

    @Test
    void aStayAcrossABookedNightIsRejected() {
        navigate(BookAStayView.class);
        // The nights 4 to 6 days from now are booked.
        pick(today.plusDays(2), today.plusDays(8));

        assertTrue(picker().isInvalid());
        assertTrue(picker().getErrorMessage().contains("is fully booked"),
                picker().getErrorMessage());
        assertFalse(book().isEnabled());
    }

    @Test
    void aStayMayEndOnTheMorningBeforeABookedNight() {
        navigate(BookAStayView.class);
        pick(today.plusDays(1), today.plusDays(4));
        assertFalse(picker().isInvalid(), picker().getErrorMessage());
    }

    @Test
    void stayLengthIsLimited() {
        navigate(BookAStayView.class);
        pick(today.plusDays(30), today.plusDays(30 + 15));
        assertTrue(picker().getErrorMessage().contains("up to 14 nights"),
                picker().getErrorMessage());

        pick(today.plusDays(30), today.plusDays(30));
        assertEquals("Check-out has to be after check-in.",
                picker().getErrorMessage());
    }

    @Test
    void bookedNightsAndTheBookingWindowAreSentToThePicker() {
        navigate(BookAStayView.class);
        assertEquals(today.toString(),
                picker().getElement().getProperty("min"));
        assertEquals(today.plusYears(1).toString(),
                picker().getElement().getProperty("max"));
    }

    private void pick(LocalDate start, LocalDate end) {
        new DateRangePickerTester(picker()).pick(start, end);
    }

    private DateRangePicker picker() {
        return findInView(DateRangePicker.class).single();
    }

    private Button book() {
        return findInView(Button.class).withText("Book").single();
    }

    private String summary() {
        return findInView(Span.class).withClassName("summary").single()
                .getText();
    }
}
