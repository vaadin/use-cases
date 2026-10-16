package com.example.uc5;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

import com.example.SupportedLocales;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.html.H1;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = TimeZonesView.class)
class TimeZonesViewTest extends SpringBrowserlessTest {

    @Test
    void showsStoredTimesInTheBrowserZoneAndUtc() {
        TimeZonesView view = navigate(TimeZonesView.class);
        runPendingSignalsTasks();

        assertEquals("UC5 — Time zones",
                findInView(H1.class).single().getText());
        // A browserless UI reports no time zone, which reads as UTC.
        assertEquals("Mar 5, 2026, 8:00 AM (UTC+00:00)",
                normalize(test(view.grid()).getCellText(0, 1)));
        assertEquals("2026-03-05T08:00:00Z",
                test(view.grid()).getCellText(0, 2));
        assertEquals("Mar 5, 2026, 10:00 AM (UTC+02:00)",
                normalize(test(view.grid()).getCellText(0, 3)));
    }

    @Test
    void pickingAZoneMovesTheTimesButNotUtc() {
        TimeZonesView view = navigate(TimeZonesView.class);

        test(view.zoneBox()).selectItem("Asia/Tokyo");
        runPendingSignalsTasks();

        assertEquals("Your time (Asia/Tokyo)",
                test(view.grid()).getHeaderCell(1));
        assertEquals("Mar 5, 2026, 5:00 PM (UTC+09:00)",
                normalize(test(view.grid()).getCellText(0, 1)));
        assertEquals("2026-03-05T08:00:00Z",
                test(view.grid()).getCellText(0, 2));
    }

    @Test
    void daylightSavingTimeChangesTheOffset() {
        TimeZonesView view = navigate(TimeZonesView.class);

        // Helsinki moves to summer time on 29 March 2026.
        assertEquals("Mar 26, 2026, 4:00 PM (UTC+02:00)",
                normalize(test(view.grid()).getCellText(1, 3)));
        assertEquals("Mar 31, 2026, 5:00 PM (UTC+03:00)",
                normalize(test(view.grid()).getCellText(2, 3)));
    }

    @Test
    void scheduledTimeIsReadInTheChosenZone() {
        TimeZonesView view = navigate(TimeZonesView.class);
        test(view.zoneBox()).selectItem("Europe/Helsinki");
        runPendingSignalsTasks();

        test(view.schedule()).setValue(LocalDateTime.of(2026, 3, 30, 9, 0));
        test(view.addButton()).click();

        assertEquals(Instant.parse("2026-03-30T06:00:00Z"),
                view.events().getLast().start());
        assertEquals("2026-03-30T06:00:00Z",
                test(view.grid()).getCellText(3, 2));
        assertEquals("Mar 30, 2026, 2:00 AM (UTC-04:00)",
                normalize(test(view.grid()).getCellText(3, 4)));
    }

    @Test
    void languageAndZoneAreIndependent() {
        TimeZonesView view = navigate(TimeZonesView.class);
        test(view.zoneBox()).selectItem("Europe/Helsinki");

        UI.getCurrent().setLocale(SupportedLocales.GERMAN);
        runPendingSignalsTasks();

        assertEquals("Ihre Zeit (Europe/Helsinki)",
                test(view.grid()).getHeaderCell(1));
        assertEquals("05.03.2026, 10:00 (UTC+02:00)",
                normalize(test(view.grid()).getCellText(0, 1)));
        assertEquals(ZoneId.of("Europe/Helsinki"), view.zoneBox().getValue());
    }

    // CLDR puts a narrow no-break space before AM/PM.
    private static String normalize(String text) {
        return text.replace(' ', ' ');
    }
}
