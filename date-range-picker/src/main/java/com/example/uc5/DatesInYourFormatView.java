package com.example.uc5;

import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.daterangepicker.DateRangePicker;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC5 — Dates in your format.
 * <p>
 * A range reads differently around the world: "3/10/2026" is the 10th of March
 * in the US and the 3rd of October in the UK, and the week starts on Sunday in
 * one and on Monday in the other. The picker formats and parses the dates for
 * {@code setLocale}, and shows the month and day names, the first day of the
 * week and its screen reader texts from {@code setI18n}. The locale starts as
 * the browser's ({@code WebBrowser#getLocale}). The UI's locale would not do:
 * without an {@code I18NProvider}, Vaadin leaves it at the server's default.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Dates in your format")
@UseCaseDescription("Typing and showing a range in the user's date format and language")
@Menu(order = 5, title = "UC5 — Dates in your format")
public class DatesInYourFormatView extends VerticalLayout {

    static final List<Locale> LOCALES = List.of(Locale.US, Locale.UK,
            Locale.of("fi", "FI"), Locale.GERMANY);

    private final Select<Locale> locale = new Select<>();
    private final DateRangePicker range = new DateRangePicker("Trip dates");
    private final Span server = new Span();

    public DatesInYourFormatView() {
        add(new H1("UC5 — Dates in your format"));
        add(new Paragraph("Pick a locale, then type or pick a range. Compare "
                + "how English (United States) and English (United Kingdom) "
                + "read the same typed text, and which day each calendar "
                + "starts the week with. The line below shows what the "
                + "server received."));

        locale.setLabel("Locale");
        locale.setItems(LOCALES);
        locale.setItemLabelGenerator(item -> item.getDisplayName(item));
        locale.addValueChangeListener(event -> apply(event.getValue()));

        range.setWidth("22rem");
        range.addValueChangeListener(event -> show(event.getValue()));
        server.addClassName("summary");

        add(locale, range, server);
        add(ApiGap.note(10, "the picker takes the date format from its "
                + "locale, but the month and day names, the first day of the "
                + "week and its own texts have to be supplied for each "
                + "language."));
        add(ApiGap.note(2, "formatting by locale reuses the connector of "
                + "Flow's DatePicker, which is not public API."));

        // Start from the browser's locale, unless the reader has picked one
        // already.
        whenAttached(ui -> {
            if (locale.isEmpty()) {
                locale.setValue(
                        localeOf(ui.getSession().getBrowser().getLocale()));
            }
            return null;
        });
    }

    /**
     * The locale of the select closest to the given one, US English when none
     * is close or the locale is unknown.
     */
    static Locale localeOf(@Nullable Locale requested) {
        if (requested == null) {
            return Locale.US;
        }
        return LOCALES.stream().filter(requested::equals).findFirst()
                .or(() -> LOCALES.stream()
                        .filter(item -> item.getLanguage()
                                .equals(requested.getLanguage()))
                        .findFirst())
                .orElse(Locale.US);
    }

    private void apply(Locale selected) {
        range.setLocale(selected);
        range.setI18n(RangeTexts.of(selected));
        show(range.getValue());
    }

    private void show(@Nullable DateRange value) {
        if (value == null) {
            server.setText("The server received no range yet.");
            return;
        }
        Locale selected = locale.getValue();
        DateTimeFormatter format = DateTimeFormatter
                .ofLocalizedDate(FormatStyle.LONG)
                .withLocale(selected == null ? Locale.US : selected);
        server.setText("The server received " + format.format(value.start())
                + " – " + format.format(value.end()) + " (" + value.start()
                + " / " + value.end() + ")");
    }
}
