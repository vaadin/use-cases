package com.example.uc5;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.example.daterangepicker.DateRangePickerI18n;

/**
 * The picker's texts for a locale.
 * <p>
 * Month and day names and the first day of the week come from
 * {@code java.time}, which knows them for every locale. The few words of the
 * picker itself are translated here. The picker derives none of this from its
 * locale (API-GAPS.md #10).
 */
final class RangeTexts {

    private RangeTexts() {
    }

    /**
     * Creates the texts for a locale.
     *
     * @param locale
     *            the locale
     * @return the texts
     */
    static DateRangePickerI18n of(Locale locale) {
        DateRangePickerI18n i18n = new DateRangePickerI18n();
        String[] words = switch (locale.getLanguage()) {
        case "fi" -> new String[] { "Alkupäivä", "Loppupäivä", "Aikaväli",
                "aikavälin alku", "aikavälin loppu", "aikavälillä", "Tänään",
                "Peruuta" };
        case "de" -> new String[] { "Startdatum", "Enddatum", "Zeitraum",
                "Beginn des Zeitraums", "Ende des Zeitraums", "im Zeitraum",
                "Heute", "Abbrechen" };
        default -> new String[] { "Start date", "End date", "Date range",
                "range start", "range end", "in range", "Today", "Cancel" };
        };
        i18n.setStartAccessibleName(words[0]).setEndAccessibleName(words[1])
                .setRangeAccessibleName(words[2]).setRangeStart(words[3])
                .setRangeEnd(words[4]).setInRange(words[5]);
        i18n.setToday(words[6]).setCancel(words[7])
                .setMonthNames(Arrays.stream(Month.values())
                        .map(month -> month.getDisplayName(
                                TextStyle.FULL_STANDALONE, locale))
                        .toList())
                .setWeekdays(weekdays(TextStyle.FULL_STANDALONE, locale))
                .setWeekdaysShort(weekdays(TextStyle.SHORT_STANDALONE, locale))
                // The web component counts days from Sunday = 0.
                .setFirstDayOfWeek(
                        WeekFields.of(locale).getFirstDayOfWeek().getValue()
                                % 7);
        return i18n;
    }

    /** Day names starting from Sunday, the order the web component uses. */
    private static List<String> weekdays(TextStyle style, Locale locale) {
        return Arrays.stream(DayOfWeek.values())
                .map(day -> DayOfWeek.SUNDAY.plus(day.ordinal()))
                .map(day -> day.getDisplayName(style, locale)).toList();
    }
}
