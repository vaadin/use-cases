package com.example.uc5;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.datetimepicker.DateTimePicker;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.Grid.Column;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC5 — Time zones.
 * <p>
 * Events are stored as {@link Instant}s, points in time without a zone, and
 * shown in the user's time zone. The browser reports its zone when the UI
 * starts, so it is known on the server from the first request; the user can
 * still pick another one, for example while travelling.
 * <p>
 * Scheduling works the other way round: the time picked in the
 * {@link DateTimePicker} is a wall-clock time in the chosen zone, turned into
 * an instant before it is stored. The list shows it next to the time in UTC and
 * for colleagues in other offices, and the events around the end of March show
 * the offset change when daylight saving time starts.
 * <p>
 * Time zones and languages are independent: an English-speaking user in
 * Helsinki wants English texts and Helsinki times.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Time zones")
@UseCaseDescription("Showing stored times in each user's own time zone")
@Menu(order = 5, title = "UC5 — Time zones")
public class TimeZonesView extends VerticalLayout {

    record Event(String name, Instant start) {
    }

    static final List<ZoneId> OFFICES = List.of(ZoneId.of("Europe/Helsinki"),
            ZoneId.of("America/New_York"), ZoneId.of("Asia/Tokyo"));

    private static final DateTimeFormatter OFFSET = DateTimeFormatter
            .ofPattern("'UTC'xxx");

    private final List<Event> events = new ArrayList<>(List.of(
            new Event("Release planning",
                    Instant.parse("2026-03-05T08:00:00Z")),
            new Event("Quarterly review",
                    Instant.parse("2026-03-26T14:00:00Z")),
            new Event("Customer demo", Instant.parse("2026-03-31T14:00:00Z"))));

    private final ValueSignal<ZoneId> zone;
    private final ComboBox<ZoneId> zoneBox = new ComboBox<>();
    private final Grid<Event> grid = new Grid<>();
    private final Column<Event> eventColumn;
    private final Column<Event> localColumn;
    private final Column<Event> utcColumn;
    private final List<Column<Event>> officeColumns = new ArrayList<>();
    private final DateTimePicker schedule = new DateTimePicker();
    private final Button add = new Button();

    public TimeZonesView() {
        UI ui = UI.getCurrent();
        ZoneId detected = ui.getPage().getExtendedClientDetails().getZoneId();
        zone = new ValueSignal<>(detected);

        add(new H1("UC5 — Time zones"));
        add(new Paragraph("The times below are in your browser's time zone ("
                + detected.getId() + "). Pick another zone and every time "
                + "moves, while the UTC column stays put. Schedule a call: the "
                + "time you enter is read in the chosen zone."));

        zoneBox.setItems(ZoneId.getAvailableZoneIds().stream().sorted()
                .map(ZoneId::of).toList());
        zoneBox.setItemLabelGenerator(ZoneId::getId);
        zoneBox.bindValue(zone, value -> {
            if (value != null) {
                zone.set(value);
            }
        });

        eventColumn = grid.addColumn(Event::name);
        localColumn = grid
                .addColumn(e -> format(e.start(), zone.peek(), ui.getLocale()));
        utcColumn = grid.addColumn(
                e -> DateTimeFormatter.ISO_INSTANT.format(e.start()));
        for (ZoneId office : OFFICES) {
            officeColumns.add(grid
                    .addColumn(e -> format(e.start(), office, ui.getLocale()))
                    .setHeader(office.getId()));
        }
        grid.getColumns().forEach(column -> column.setAutoWidth(true));
        grid.setItems(events);
        grid.setAllRowsVisible(true);

        schedule.setValue(LocalDateTime.of(2026, 3, 30, 9, 0));
        add.addClickListener(e -> addCall());
        HorizontalLayout scheduling = new HorizontalLayout(schedule, add);
        scheduling.setAlignItems(Alignment.BASELINE);

        Span offset = new Span();
        offset.addClassName("offset");
        offset.bindText(Signal.computed(() -> {
            ZoneId current = zone.get();
            return current.getId() + " · "
                    + OFFSET.format(Instant.now().atZone(current));
        }));

        Div sample = new Div(new HorizontalLayout(zoneBox, offset), grid,
                scheduling);
        sample.addClassName("sample");
        add(sample);

        Signal.effect(this, () -> {
            Locale locale = ui.localeSignal().get();
            ZoneId current = zone.get();
            zoneBox.setLabel(getTranslation(locale, "uc5.zone"));
            eventColumn.setHeader(getTranslation(locale, "uc5.event"));
            localColumn.setHeader(
                    getTranslation(locale, "uc5.your-time", current.getId()));
            utcColumn.setHeader("UTC");
            schedule.setLabel(
                    getTranslation(locale, "uc5.schedule", current.getId()));
            schedule.setLocale(locale);
            add.setText(getTranslation(locale, "uc5.add"));
            grid.getDataProvider().refreshAll();
        });
    }

    private void addCall() {
        LocalDateTime wallClock = schedule.getValue();
        if (wallClock == null) {
            return;
        }
        // The picker's value has no zone: it means "this time where the user
        // is", so it becomes an instant only together with the chosen zone.
        Instant start = wallClock.atZone(zone.peek()).toInstant();
        events.add(new Event(getTranslation("uc5.call"), start));
        grid.getDataProvider().refreshAll();
    }

    static String format(Instant instant, ZoneId zone, Locale locale) {
        ZonedDateTime time = instant.atZone(zone);
        return DateTimeFormatter
                .ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
                .withLocale(locale).format(time) + " (" + OFFSET.format(time)
                + ")";
    }

    // Package-private test seams.
    ComboBox<ZoneId> zoneBox() {
        return zoneBox;
    }

    Grid<Event> grid() {
        return grid;
    }

    List<Event> events() {
        return events;
    }

    DateTimePicker schedule() {
        return schedule;
    }

    Button addButton() {
        return add;
    }
}
