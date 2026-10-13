package com.example.uc3;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;

import com.example.common.UseCaseDescription;
import com.example.daterangepicker.DateRangePicker;
import com.example.views.ApiGap;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.datepicker.DateRange;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.ListItem;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.html.UnorderedList;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationResult;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Request time off.
 * <p>
 * An employee asks for days off in a form bound with {@link Binder}, like any
 * other field: the range is required, can not start in the past, and can not
 * need more working days than the employee has left. Weekends and public
 * holidays are part of the range but are not counted, so the summary shows both
 * the calendar days and the working days. Week numbers help to plan by week.
 * <p>
 * The validation runs on the server ({@code manualValidation}). Text the picker
 * can not parse, such as "32.13.2026", reaches the server as an empty end, so
 * the form says the range is missing rather than that the date is wrong.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Request time off")
@UseCaseDescription("Asking for days off in a validated form that counts working days")
@Menu(order = 3, title = "UC3 — Request time off")
public class RequestTimeOffView extends VerticalLayout {

    static final int DAYS_LEFT = 12;
    private static final DateTimeFormatter DAY = DateTimeFormatter
            .ofPattern("d MMM yyyy", Locale.ENGLISH);

    /** The form's bean. */
    public static class LeaveRequest {
        private @Nullable DateRange period;
        private String note = "";

        public @Nullable DateRange getPeriod() {
            return period;
        }

        public void setPeriod(@Nullable DateRange period) {
            this.period = period;
        }

        public String getNote() {
            return note;
        }

        public void setNote(String note) {
            this.note = note;
        }
    }

    private final LocalDate today;
    private final Set<LocalDate> holidays;
    private final Binder<LeaveRequest> binder = new Binder<>();
    private final DateRangePicker period = new DateRangePicker("Days off");
    private final Span summary = new Span();
    private final UnorderedList sent = new UnorderedList();

    public RequestTimeOffView() {
        this(LocalDate.now());
    }

    RequestTimeOffView(LocalDate today) {
        this.today = today;
        this.holidays = holidays(today);
        add(new H1("UC3 — Request time off"));
        add(new Paragraph("Pick your first and last day off. Weekends and "
                + "public holidays are not counted. You have " + DAYS_LEFT
                + " days left this year. Try a range in the past, or one that "
                + "needs more days than you have."));

        period.setWidth("22rem");
        period.setShowWeekNumbers(true);
        period.setClearButtonVisible(true);
        period.setHelperText("Public holidays: " + String.join(", ",
                holidays.stream().sorted().map(DAY::format).toList()));
        period.addValueChangeListener(event -> summarize(event.getValue()));

        TextField note = new TextField("Note for your manager");
        note.setWidth("22rem");

        binder.forField(period).asRequired("Pick your first and last day off.")
                .withValidator(range -> !range.start().isBefore(today),
                        "Time off can not start in the past.")
                .withValidator(
                        (range, context) -> workingDays(range) <= DAYS_LEFT
                                ? ValidationResult.ok()
                                : ValidationResult.error(
                                        "This needs " + workingDays(range)
                                                + " working days; you have "
                                                + DAYS_LEFT + " left."))
                .bind(LeaveRequest::getPeriod, LeaveRequest::setPeriod);
        binder.forField(note).bind(LeaveRequest::getNote,
                LeaveRequest::setNote);
        binder.setBean(new LeaveRequest());

        Button send = new Button("Send request", event -> send());
        send.addThemeVariants(ButtonVariant.PRIMARY);
        summary.addClassName("summary");

        add(period, summary, note, send, sent);
        add(ApiGap.note(8, "text the picker can not parse reaches the server "
                + "as an empty date, so the message says a date is missing, "
                + "not that it is wrong."));
        add(ApiGap.note(12, "typing text that does not parse over a date "
                + "clears it without a message."));
        summarize(null);
    }

    /** Two public holidays in the coming weeks, relative to today. */
    static Set<LocalDate> holidays(LocalDate today) {
        return Set.of(nextWeekday(today.plusDays(10)),
                nextWeekday(today.plusDays(31)));
    }

    private static LocalDate nextWeekday(LocalDate date) {
        LocalDate day = date;
        while (day.getDayOfWeek().getValue() > DayOfWeek.FRIDAY.getValue()) {
            day = day.plusDays(1);
        }
        return day;
    }

    long workingDays(DateRange range) {
        return range.start().datesUntil(range.end().plusDays(1))
                .filter(day -> day.getDayOfWeek().getValue() <= DayOfWeek.FRIDAY
                        .getValue())
                .filter(day -> !holidays.contains(day)).count();
    }

    private void summarize(@Nullable DateRange range) {
        if (range == null) {
            summary.setText("");
            return;
        }
        long calendarDays = range.end().toEpochDay()
                - range.start().toEpochDay() + 1;
        long working = workingDays(range);
        summary.setText(
                calendarDays + " calendar days, " + working + " working days");
    }

    private void send() {
        LeaveRequest request = binder.getBean();
        if (binder.validate().isOk() && request.getPeriod() != null) {
            DateRange range = request.getPeriod();
            sent.add(new ListItem("Sent for approval: "
                    + DAY.format(range.start()) + " – "
                    + DAY.format(range.end()) + ", " + workingDays(range)
                    + " working days" + (request.getNote().isBlank() ? ""
                            : " (\"" + request.getNote() + "\")")));
            binder.setBean(new LeaveRequest());
        }
    }
}
