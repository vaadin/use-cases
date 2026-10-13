package com.example.uc4;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.daterangepicker.DateRangePicker;
import com.example.views.ApiGap;
import com.example.views.MainLayout;

import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Change the return date.
 * <p>
 * A car rental has started: the pick-up date is in the past and can not change,
 * but the customer can still move the return date. With
 * {@code setSeparateDatePicking(true)} a click on the end input picks only the
 * end date, and {@code setMin} keeps it from going before the pick-up.
 * <p>
 * The picker has no way to make one end read-only, so the start input stays
 * editable. The view puts the pick-up date back when it is changed and says
 * why.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Change the return date")
@UseCaseDescription("Moving the end of a range whose start can no longer change")
@Menu(order = 4, title = "UC4 — Change the return date")
public class ChangeReturnDateView extends VerticalLayout {

    static final BigDecimal DAILY_RATE = new BigDecimal("45.00");
    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("d MMMM", Locale.ENGLISH);

    private final DateRangePicker rental = new DateRangePicker(
            "Pick-up – return");
    private final Span summary = new Span();
    private final Paragraph note = new Paragraph();
    private final LocalDate pickUp;

    public ChangeReturnDateView() {
        this(LocalDate.now());
    }

    ChangeReturnDateView(LocalDate today) {
        pickUp = today.minusDays(2);
        add(new H1("UC4 — Change the return date"));
        add(new Paragraph("Your rental started on " + DAY.format(pickUp)
                + ". Click the return date to move it; the price follows. "
                + "The pick-up date can not change any more."));

        rental.setWidth("22rem");
        rental.setSeparateDatePicking(true);
        rental.setMin(pickUp);
        rental.setValue(new DateRange(pickUp, today.plusDays(3)));
        rental.addValueChangeListener(event -> {
            if (event.isFromClient() && !pickUp.equals(rental.getStart())) {
                // The start input can not be made read-only (API-GAPS.md #9).
                LocalDate end = rental.getEnd();
                rental.setValue(new DateRange(pickUp,
                        end == null || end.isBefore(pickUp) ? pickUp : end));
                note.setText("The pick-up date is fixed once the rental "
                        + "has started, so it was put back to "
                        + DAY.format(pickUp) + ".");
            } else {
                note.setText("");
            }
            summarize();
        });

        summary.addClassName("summary");
        add(rental, summary, note);
        add(ApiGap.note(9, "one end of the range can not be made read-only; "
                + "the view puts the pick-up date back instead."));
        summarize();
    }

    private void summarize() {
        DateRange range = rental.getValue();
        if (range == null) {
            summary.setText("");
            return;
        }
        long days = ChronoUnit.DAYS.between(range.start(), range.end());
        long charged = Math.max(1, days);
        summary.setText("Return on " + DAY.format(range.end()) + ": " + charged
                + (charged == 1 ? " day, " : " days, ")
                + DAILY_RATE.multiply(BigDecimal.valueOf(charged)) + " EUR");
    }
}
