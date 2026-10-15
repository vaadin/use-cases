package com.example.uc5;

import java.time.LocalDate;
import java.util.Locale;

import com.example.daterangepicker.DateRangePicker;
import com.example.daterangepicker.DateRangePickerTester;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.server.VaadinSession;
import com.vaadin.flow.server.WebBrowser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@ViewPackages(classes = DatesInYourFormatView.class)
class DatesInYourFormatViewTest extends SpringBrowserlessTest {

    @Test
    void thePickerStartsInTheBrowsersLocale() {
        browserLocale(Locale.of("de", "AT"));
        navigate(DatesInYourFormatView.class);

        String calls = String.join("\n",
                DateRangePickerTester.pendingJavaScript().stream()
                        .map(call -> call.getParameters().toString()).toList());
        assertTrue(calls.contains("de-DE"), calls);
        assertTrue(calls.contains("Januar") && calls.contains("Zeitraum"),
                calls);
    }

    @Test
    void theRangeIsShownInTheSelectedLocale() {
        navigate(DatesInYourFormatView.class);
        @SuppressWarnings("unchecked")
        Select<Locale> locale = findInView(Select.class).single();
        test(locale).selectItem("English (United Kingdom)");
        new DateRangePickerTester(findInView(DateRangePicker.class).single())
                .pick(LocalDate.of(2026, 10, 3), LocalDate.of(2026, 10, 12));

        assertEquals("The server received 3 October 2026 – 12 October 2026 "
                + "(2026-10-03 / 2026-10-12)", server());
    }

    @Test
    void theWeekStartsOnTheLocalesFirstDay() {
        // The web component counts from Sunday = 0.
        assertEquals(0, RangeTexts.of(Locale.US).getFirstDayOfWeek());
        assertEquals(1,
                RangeTexts.of(Locale.of("fi", "FI")).getFirstDayOfWeek());
        assertEquals("sunnuntai",
                RangeTexts.of(Locale.of("fi", "FI")).getWeekdays().get(0));
    }

    @Test
    void anUnknownLocaleFallsBackToTheClosestOne() {
        assertEquals(Locale.GERMANY,
                DatesInYourFormatView.localeOf(Locale.of("de", "CH")));
        assertEquals(Locale.US, DatesInYourFormatView.localeOf(Locale.JAPAN));
    }

    private String server() {
        return findInView(Span.class).withClassName("summary").single()
                .getText();
    }

    /** Makes the browser report the given locale, as its request would. */
    private static void browserLocale(Locale locale) {
        WebBrowser browser = mock(WebBrowser.class);
        when(browser.getLocale()).thenReturn(locale);
        VaadinSession.getCurrent().setBrowser(browser);
    }
}
