package com.example.uc3;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = RequestTimeOffView.class)
class RequestTimeOffViewTest extends SpringBrowserlessTest {

    private final LocalDate today = LocalDate.now();

    @Test
    void weekendsAndHolidaysAreNotCounted() {
        RequestTimeOffView view = new RequestTimeOffView(
                LocalDate.of(2026, 10, 1));
        // 5 Oct (Mon) to 18 Oct (Sun): two working weeks, and the holiday on
        // Mon 12 Oct (10 days after 1 Oct, moved to a weekday).
        assertEquals(9,
                view.workingDays(new DateRange(LocalDate.of(2026, 10, 5),
                        LocalDate.of(2026, 10, 18))));
    }

    @Test
    void aRangeInThePastIsRejected() {
        navigate(RequestTimeOffView.class);
        pick(today.minusDays(3), today.plusDays(1));
        assertEquals("Time off can not start in the past.",
                picker().getErrorMessage());
        assertTrue(picker().isInvalid());
    }

    @Test
    void aRangeThatNeedsTooManyDaysIsRejected() {
        navigate(RequestTimeOffView.class);
        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        pick(monday, monday.plusWeeks(4));
        assertTrue(picker().getErrorMessage().endsWith("you have 12 left."),
                picker().getErrorMessage());
    }

    @Test
    void sendingWithoutARangeAsksForOne() {
        navigate(RequestTimeOffView.class);
        test(findInView(Button.class).withText("Send request").single())
                .click();
        assertEquals("Pick your first and last day off.",
                picker().getErrorMessage());
        assertTrue(findInView(ListItem.class).all().isEmpty());
    }

    @Test
    void aValidRequestIsSentAndTheFormStartsOver() {
        navigate(RequestTimeOffView.class);
        LocalDate monday = today.with(TemporalAdjusters.next(DayOfWeek.MONDAY));
        pick(monday, monday.plusDays(1));
        assertFalse(picker().isInvalid(), picker().getErrorMessage());
        assertTrue(findInView(Span.class).withClassName("summary").single()
                .getText().startsWith("2 calendar days"));

        test(findInView(Button.class).withText("Send request").single())
                .click();

        assertEquals(1, findInView(ListItem.class).all().size());
        assertTrue(findInView(ListItem.class).single().getText()
                .startsWith("Sent for approval: "));
        assertNull(picker().getValue());
    }

    private void pick(LocalDate start, LocalDate end) {
        new DateRangePickerTester(picker()).pick(start, end);
    }

    private DateRangePicker picker() {
        return findInView(DateRangePicker.class).single();
    }
}
