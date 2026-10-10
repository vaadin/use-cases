package com.example.uc3;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.format.TextStyle;
import java.time.temporal.WeekFields;
import java.util.Locale;

import com.example.MissingAPI;
import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.datepicker.DatePicker;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.timepicker.TimePicker;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.function.SignalComputation;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC3 — Dates and times.
 * <p>
 * Dates and times in the user's conventions: day-month order, separators, month
 * and weekday names, 12- or 24-hour clock and the first day of the week. The
 * pickers do that in the browser, the summary below them on the server with
 * {@link DateTimeFormatter#ofLocalizedDate}.
 * <p>
 * The pickers only pick up the UI's locale when they are attached, and fill in
 * their month names, weekdays and buttons only from a {@code DatePickerI18n}.
 * So on every locale change the view gives each picker the new locale and an
 * i18n object built from the JDK's locale data.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Dates and times")
@UseCaseDescription("Showing and entering dates and times in the user's format")
@Menu(order = 3, title = "UC3 — Dates and times")
public class DatesAndTimesView extends VerticalLayout {

    static final LocalDate INITIAL_DATE = LocalDate.of(2026, 3, 5);
    static final LocalTime INITIAL_TIME = LocalTime.of(14, 30);

    private final ValueSignal<@Nullable LocalDate> date = new ValueSignal<>(
            INITIAL_DATE);
    private final ValueSignal<@Nullable LocalTime> time = new ValueSignal<>(
            INITIAL_TIME);

    private final DatePicker datePicker = new DatePicker();
    private final TimePicker timePicker = new TimePicker();
    private final DateTimePicker dateTimePicker = new DateTimePicker();

    public DatesAndTimesView() {
        UI ui = UI.getCurrent();

        add(new H1("UC3 — Dates and times"));
        add(new Paragraph("Open a picker, then switch the language in the top "
                + "bar: the format, the month and weekday names, the first "
                + "day of the week and the clock all change. The summary "
                + "below is formatted on the server for the same date."));

        datePicker.bindValue(date, date::set);
        timePicker.bindValue(time, time::set);
        dateTimePicker.setValue(LocalDateTime.of(INITIAL_DATE, INITIAL_TIME));

        UnorderedList summary = new UnorderedList(
                line(() -> format(FormatStyle.SHORT, ui.localeSignal().get())),
                line(() -> format(FormatStyle.MEDIUM, ui.localeSignal().get())),
                line(() -> format(FormatStyle.LONG, ui.localeSignal().get())),
                line(() -> format(FormatStyle.FULL, ui.localeSignal().get())),
                line(() -> {
                    LocalTime value = time.get();
                    return value == null ? ""
                            : DateTimeFormatter
                                    .ofLocalizedTime(FormatStyle.SHORT)
                                    .withLocale(ui.localeSignal().get())
                                    .format(value);
                }), line(() -> {
                    Locale locale = ui.localeSignal().get();
                    return getTranslation(locale, "uc3.week-starts",
                            WeekFields.of(locale).getFirstDayOfWeek()
                                    .getDisplayName(TextStyle.FULL, locale));
                }));
        summary.addClassName("summary");

        HorizontalLayout pickers = new HorizontalLayout(datePicker, timePicker,
                dateTimePicker);
        pickers.setWrap(true);
        Div sample = new Div(pickers, summary);
        sample.addClassName("sample");
        add(sample);

        Signal.effect(this, () -> localize(ui.localeSignal().get()));
    }

    private void localize(Locale locale) {
        datePicker.setLabel(getTranslation(locale, "uc3.delivery-date"));
        timePicker.setLabel(getTranslation(locale, "uc3.pickup-time"));
        dateTimePicker.setLabel(getTranslation(locale, "uc3.meeting"));

        datePicker.setLocale(locale);
        timePicker.setLocale(locale);
        dateTimePicker.setLocale(locale);
        datePicker.setI18n(MissingAPI.datePickerI18n(this, locale));
        dateTimePicker
                .setDatePickerI18n(MissingAPI.datePickerI18n(this, locale));
    }

    private String format(FormatStyle style, Locale locale) {
        LocalDate value = date.get();
        return value == null ? ""
                : DateTimeFormatter.ofLocalizedDate(style).withLocale(locale)
                        .format(value);
    }

    private static ListItem line(SignalComputation<String> text) {
        Span span = new Span();
        span.bindText(Signal.computed(text));
        return new ListItem(span);
    }

    // Package-private test seams.
    DatePicker datePicker() {
        return datePicker;
    }

    TimePicker timePicker() {
        return timePicker;
    }

    DateTimePicker dateTimePicker() {
        return dateTimePicker;
    }
}
