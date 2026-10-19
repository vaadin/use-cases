package com.example.uc4;

import java.time.LocalDate;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = ChangeReturnDateView.class)
class ChangeReturnDateViewTest extends SpringBrowserlessTest {

    private final LocalDate today = LocalDate.now();
    private final LocalDate pickUp = today.minusDays(2);

    @Test
    void theEndIsPickedOnItsOwn() {
        navigate(ChangeReturnDateView.class);
        DateRangePicker picker = findInView(DateRangePicker.class).single();
        assertTrue(
                picker.getElement().getProperty("separateDatePicking", false));
        assertEquals(pickUp.toString(), picker.getElement().getProperty("min"));
        assertEquals("Return on", summary().substring(0, 9));
        assertTrue(
                DateRangePickerTester.pendingJavaScript().stream()
                        .anyMatch(call -> call.getExpression()
                                .contains("dateRangePickerLockStart")),
                "The start input is locked in the browser");
    }

    @Test
    void movingTheReturnDateUpdatesThePrice() {
        navigate(ChangeReturnDateView.class);
        pick(pickUp, today.plusDays(6));
        // 8 days at 45.00
        assertTrue(summary().endsWith("8 days, 360.00 EUR"), summary());
        assertEquals("", note());
    }

    @Test
    void aChangedPickUpDateThatReachesTheServerIsPutBack() {
        navigate(ChangeReturnDateView.class);
        pick(today, today.plusDays(6));

        DateRangePicker picker = findInView(DateRangePicker.class).single();
        assertEquals(new DateRange(pickUp, today.plusDays(6)),
                picker.getValue());
        assertTrue(note().startsWith("The pick-up date is fixed"), note());
    }

    private void pick(LocalDate start, LocalDate end) {
        new DateRangePickerTester(findInView(DateRangePicker.class).single())
                .pick(start, end);
    }

    private String summary() {
        return findInView(Span.class).withClassName("summary").single()
                .getText();
    }

    private String note() {
        return findInView(Paragraph.class).all().stream()
                .map(Paragraph::getText)
                .filter(text -> text.startsWith("The pick-up date")).findFirst()
                .orElse("");
    }
}
