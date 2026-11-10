package com.example.daterangepicker;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.vaadin.browserless.BrowserlessTest;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DateRange;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DateRangePickerTest extends BrowserlessTest {

    private static final LocalDate MARCH_2 = LocalDate.of(2026, 3, 2);
    private static final LocalDate MARCH_8 = LocalDate.of(2026, 3, 8);

    private DateRangePicker picker;

    @BeforeEach
    void createPicker() {
        picker = new DateRangePicker("Trip");
    }

    @Test
    void aPickedRangeBecomesTheValueAndIsFromTheClient() {
        UI.getCurrent().add(picker);
        List<String> events = new ArrayList<>();
        picker.addValueChangeListener(event -> events
                .add(event.getValue() + " " + event.isFromClient()));

        new DateRangePickerTester(picker).pick(MARCH_2, MARCH_8);

        assertEquals(new DateRange(MARCH_2, MARCH_8), picker.getValue());
        assertEquals(List.of(new DateRange(MARCH_2, MARCH_8) + " true"),
                events);
    }

    @Test
    void aRangeWithOneEndHasNoValueButKeepsTheEnd() {
        UI.getCurrent().add(picker);
        new DateRangePickerTester(picker).pick(MARCH_2, null);

        assertNull(picker.getValue());
        assertEquals(MARCH_2, picker.getStart());
        assertNull(picker.getEnd());
    }

    @Test
    void settingTheValueSetsBothEnds() {
        picker.setValue(new DateRange(MARCH_2, MARCH_8));
        assertEquals("2026-03-02",
                picker.getElement().getProperty("startValue"));
        assertEquals("2026-03-08", picker.getElement().getProperty("endValue"));

        picker.clear();
        assertNull(picker.getStart());
        assertNull(picker.getEnd());
    }

    @Test
    void aStartAfterTheEndIsNoRange() {
        assertNull(DateRangePicker.toRange(MARCH_8, MARCH_2));
        assertEquals(new DateRange(MARCH_2, MARCH_2),
                DateRangePicker.toRange(MARCH_2, MARCH_2));
    }

    @Test
    void theConnectorIsCreatedBeforeItIsUsed() {
        // Set before attaching: these must wait for the connector.
        picker.setDisabledDates(Set.of(MARCH_8));
        picker.setDisabledWeekdays(Set.of(DayOfWeek.SUNDAY));
        picker.setLocale(Locale.GERMANY);
        DateRangePickerTester.pendingJavaScript();

        UI.getCurrent().add(picker);

        List<String> calls = DateRangePickerTester.pendingJavaScript().stream()
                .map(call -> call.getExpression() + call.getParameters())
                .toList();
        assertTrue(calls.get(0).contains("initLazy"), calls.toString());
        String all = String.join("\n", calls);
        assertTrue(all.contains("updateI18n") && all.contains("de-DE"), all);
        // The connector takes [year, 0-based month, day] and ISO weekdays.
        assertTrue(
                all.contains("setDateMetadataConfig")
                        && all.contains("[2026,2,8]") && all.contains("[7]"),
                all);
    }

    @Test
    void validationIsLeftToTheServer() {
        assertTrue(picker.getElement().getProperty("manualValidation", false));
        picker.setErrorMessage("Too long");
        picker.setInvalid(true);
        assertEquals("Too long", picker.getErrorMessage());
        assertTrue(picker.isInvalid());
    }
}
