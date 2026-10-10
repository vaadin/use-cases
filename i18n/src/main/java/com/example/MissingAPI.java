package com.example;

import java.time.DayOfWeek;
import java.time.Month;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Direction;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker.DatePickerI18n;
import com.vaadin.flow.i18n.I18NProvider;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.signals.Signal;

/**
 * Shims for internationalization that Flow does not provide yet. Each method
 * names the gap it covers; see API-GAPS.md for the full story and the API we
 * would rather call.
 */
public final class MissingAPI {

    /**
     * Languages written right to left (ISO 639 codes, including the legacy
     * {@code iw} / {@code ji} that older JDKs return for Hebrew and Yiddish).
     */
    private static final Set<String> RTL_LANGUAGES = Set.of("ar", "arc", "ckb",
            "dv", "fa", "he", "iw", "ji", "ks", "ku", "ps", "sd", "ug", "ur",
            "yi");

    /** Unicode FIRST STRONG ISOLATE and POP DIRECTIONAL ISOLATE. */
    private static final char FSI = (char) 0x2068;
    private static final char PDI = (char) 0x2069;

    private MissingAPI() {
    }

    /**
     * The text direction of a locale's language.
     * <p>
     * Gap: neither the JDK (outside AWT's {@code ComponentOrientation}) nor
     * Flow can tell the direction of a locale, and {@link UI#setDirection} does
     * not follow {@link UI#setLocale} on its own. Migrate to a locale-driven
     * direction in Flow once there is one.
     */
    public static Direction directionOf(Locale locale) {
        return RTL_LANGUAGES.contains(locale.getLanguage())
                ? Direction.RIGHT_TO_LEFT
                : Direction.LEFT_TO_RIGHT;
    }

    /**
     * A translation that follows the UI's locale: bind it with {@code bindText}
     * and the text changes when the language does. A parameter that is itself a
     * {@link Signal} is read reactively too, so "{0} items" also follows the
     * count.
     * <p>
     * Gap: Flow has {@link UI#localeSignal()}, but no signal-returning
     * counterpart of {@link Component#getTranslation}. Migrate to it once there
     * is one.
     */
    public static Signal<String> translate(String key, Object... params) {
        UI ui = UI.getCurrent();
        I18NProvider provider = VaadinService.getCurrent().getInstantiator()
                .getI18NProvider();
        return Signal.computed(() -> provider.getTranslation(key,
                ui.localeSignal().get(),
                Arrays.stream(params)
                        .map(p -> p instanceof Signal<?> s ? s.get() : p)
                        .toArray()));
    }

    /**
     * Date picker texts for a locale. Month and weekday names and the first day
     * of the week come from the JDK's locale data, the buttons from the
     * application's translations.
     * <p>
     * Gap: the pickers format dates in the picker's locale but leave every
     * other text in English unless the application fills in a
     * {@link DatePickerI18n} by hand, and neither follows a later locale
     * change. Migrate to a locale-derived default once the component has one.
     */
    public static DatePickerI18n datePickerI18n(Component owner,
            Locale locale) {
        // The web component expects the weekdays and the first day of the
        // week counted from Sunday.
        List<DayOfWeek> fromSunday = List.of(DayOfWeek.SUNDAY, DayOfWeek.MONDAY,
                DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY,
                DayOfWeek.FRIDAY, DayOfWeek.SATURDAY);
        return new DatePickerI18n()
                .setMonthNames(Arrays.stream(Month.values())
                        .map(m -> m.getDisplayName(TextStyle.FULL_STANDALONE,
                                locale))
                        .toList())
                .setWeekdays(fromSunday.stream()
                        .map(d -> d.getDisplayName(TextStyle.FULL, locale))
                        .toList())
                .setWeekdaysShort(fromSunday.stream()
                        .map(d -> d.getDisplayName(TextStyle.SHORT, locale))
                        .toList())
                .setFirstDayOfWeek(fromSunday
                        .indexOf(WeekFields.of(locale).getFirstDayOfWeek()))
                .setToday(owner.getTranslation(locale, "picker.today"))
                .setCancel(owner.getTranslation(locale, "picker.cancel"));
    }

    /**
     * Wraps user-entered text in Unicode isolates, so that it is laid out on
     * its own instead of borrowing the direction of the sentence around it. Use
     * it for parameters of a translated message: "Acme Ltd." inside a Hebrew
     * sentence otherwise shows up as ".Acme Ltd".
     * <p>
     * Gap: {@link java.text.MessageFormat} and {@link I18NProvider} insert
     * parameters as they are, and Flow has no component for the HTML
     * {@code <bdi>} element. Migrate once translations isolate parameters
     * themselves.
     */
    public static String isolate(String text) {
        return FSI + text + PDI;
    }
}
