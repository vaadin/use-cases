package com.example.uc1;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.example.common.UseCaseDescription;
import com.example.daterangepicker.DateRangePicker;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC1 — Book a stay.
 * <p>
 * A guest picks check-in and check-out in one calendar. Past dates and dates
 * more than a year ahead can not be picked ({@code setMin}, {@code setMax}),
 * and fully booked nights are disabled ({@code setDisabledDates}). The price
 * follows the value.
 * <p>
 * Two rules need the server. The picker lets a range span disabled dates, so
 * the view checks that no booked night lies inside the stay; and it has no
 * limit on the length of a range, so the view checks the 14-night maximum.
 * Disabling a booked night also stops the guest from checking out on the
 * morning of that day, which a hotel allows. The disabled dates reach the
 * calendar through the connector of Flow's {@code DatePicker}, which is not
 * public API.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Book a stay")
@UseCaseDescription("Picking check-in and check-out around fully booked nights")
@Menu(order = 1, title = "UC1 — Book a stay")
public class BookAStayView extends VerticalLayout {

    static final BigDecimal NIGHTLY_RATE = new BigDecimal("120.00");
    static final int MAX_NIGHTS = 14;
    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("d MMMM", Locale.ENGLISH);

    private final DateRangePicker stay = new DateRangePicker(
            "Check-in – check-out");
    private final Span summary = new Span();
    private final Button book = new Button("Book");
    private final Paragraph result = new Paragraph();
    private final Set<LocalDate> bookedNights;

    public BookAStayView() {
        this(LocalDate.now());
    }

    BookAStayView(LocalDate today) {
        bookedNights = bookedNights(today);
        add(new H1("UC1 — Book a stay"));
        add(new Paragraph("Pick your check-in and check-out dates. Greyed-out "
                + "nights are fully booked. A stay can be up to " + MAX_NIGHTS
                + " nights; the price follows your dates."));

        stay.setWidth("22rem");
        stay.setMin(today);
        stay.setMax(today.plusYears(1));
        stay.setDisabledDates(bookedNights);
        stay.setStartPlaceholder("Check-in");
        stay.setEndPlaceholder("Check-out");
        stay.addValueChangeListener(event -> update(event.getValue()));

        summary.addClassName("summary");
        book.addThemeVariants(ButtonVariant.PRIMARY);
        book.setEnabled(false);
        book.addClickListener(event -> result.setText(
                "Booked: " + summary.getText() + " (the demo books nothing)."));

        add(stay, summary, book, result);
        add(ApiGap.note(4, "a range may span disabled dates, so the server "
                + "has to check that no booked night is inside the stay."));
        add(ApiGap.note(5, "a disabled date can be neither a start nor an "
                + "end, so you can not check out on the morning of a fully "
                + "booked night."));
        add(ApiGap.note(6, "there is no maximum length of a range; the "
                + "server checks the " + MAX_NIGHTS + "-night limit."));
        add(ApiGap.note(2, "the booked nights reach the calendar through the "
                + "connector of Flow's DatePicker, which is not public API."));
        update(null);
    }

    /** Three fully booked stretches, relative to today. */
    static Set<LocalDate> bookedNights(LocalDate today) {
        return Stream
                .of(today.plusDays(4), today.plusDays(5), today.plusDays(6),
                        today.plusDays(15), today.plusDays(23),
                        today.plusDays(24))
                .collect(Collectors.toUnmodifiableSet());
    }

    private void update(@Nullable DateRange range) {
        Optional<String> error = range == null ? Optional.empty()
                : validate(range);
        stay.setInvalid(error.isPresent());
        stay.setErrorMessage(error.orElse(null));
        book.setEnabled(range != null && error.isEmpty());
        if (range == null) {
            summary.setText("Pick your dates");
        } else if (error.isPresent()) {
            summary.setText("");
        } else {
            long nights = nights(range);
            summary.setText(nights + (nights == 1 ? " night, " : " nights, ")
                    + DAY.format(range.start()) + " to "
                    + DAY.format(range.end()) + ": "
                    + NIGHTLY_RATE.multiply(BigDecimal.valueOf(nights))
                    + " EUR");
        }
    }

    Optional<String> validate(DateRange range) {
        long nights = nights(range);
        if (nights == 0) {
            return Optional.of("Check-out has to be after check-in.");
        }
        if (nights > MAX_NIGHTS) {
            return Optional.of("A stay can be up to " + MAX_NIGHTS
                    + " nights; this one is " + nights + ".");
        }
        return range.start().datesUntil(range.end())
                .filter(bookedNights::contains).findFirst()
                .map(night -> "The night of " + DAY.format(night)
                        + " is fully booked. Pick dates around it.");
    }

    static long nights(DateRange range) {
        return ChronoUnit.DAYS.between(range.start(), range.end());
    }
}
