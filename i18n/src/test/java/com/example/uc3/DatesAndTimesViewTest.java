package com.example.uc3;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = DatesAndTimesView.class)
class DatesAndTimesViewTest extends SpringBrowserlessTest {

    @Test
    void rendersPickersAndSummaryInEnglish() {
        DatesAndTimesView view = navigate(DatesAndTimesView.class);
        runPendingSignalsTasks();

        assertEquals("UC3 — Dates and times",
                findInView(H1.class).single().getText());
        assertEquals("Delivery date", view.datePicker().getLabel());
        assertEquals(SupportedLocales.ENGLISH, view.datePicker().getLocale());
        assertTrue(summaryContains("Thursday, March 5, 2026"));
        assertTrue(summaryContains("The week starts on Sunday."));
    }

    @Test
    void localeChangeUpdatesPickersAndSummary() {
        DatesAndTimesView view = navigate(DatesAndTimesView.class);

        UI.getCurrent().setLocale(SupportedLocales.GERMAN);
        runPendingSignalsTasks();

        assertEquals("Liefertermin", view.datePicker().getLabel());
        assertEquals(SupportedLocales.GERMAN, view.datePicker().getLocale());
        assertEquals(SupportedLocales.GERMAN, view.timePicker().getLocale());
        assertEquals(SupportedLocales.GERMAN,
                view.dateTimePicker().getLocale());
        DatePickerI18n i18n = view.datePicker().getI18n();
        assertEquals("März", i18n.getMonthNames().get(2));
        assertEquals("Sonntag", i18n.getWeekdays().getFirst());
        assertEquals(1, i18n.getFirstDayOfWeek());
        assertEquals("Heute", i18n.getToday());
        assertEquals("Heute",
                view.dateTimePicker().getDatePickerI18n().getToday());
        assertTrue(summaryContains("Donnerstag, 5. März 2026"));
        assertTrue(summaryContains("14:30"));
        assertTrue(summaryContains("Die Woche beginnt am Montag."));
    }

    @Test
    void summaryFollowsThePickedDate() {
        DatesAndTimesView view = navigate(DatesAndTimesView.class);

        test(view.datePicker())
                .setValue(DatesAndTimesView.INITIAL_DATE.plusMonths(1));
        runPendingSignalsTasks();

        assertTrue(summaryContains("Sunday, April 5, 2026"));
    }

    private boolean summaryContains(String text) {
        return findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText().contains(text));
    }
}
