package com.example.uc7;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.geolocation.Geolocation;
import com.vaadin.flow.component.geolocation.GeolocationOptions;
import com.vaadin.flow.component.geolocation.GeolocationPosition;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC7 — Capturing a location as part of a form.
 * <p>
 * Vertical pothole-reporting form. The description and the pinned location live
 * as signals; the submit button's enabled state and the pin label's text are
 * bound to those signals so there is no manual refresh logic. Submitted reports
 * appear in the grid below.
 */
@Route(value = "uc7", layout = MainLayout.class)
@PageTitle("UC7 — Capturing a location as part of a form")
@UseCaseDescription("Capturing a location as part of a form")
@Menu(order = 7, title = "UC7 — Form field")
public class FormFieldView extends VerticalLayout {

    private static final double MAX_ACCURACY_METRES = 50;

    private final List<PotholeReport> reports = new ArrayList<>();

    private final ValueSignal<String> descriptionSignal = new ValueSignal<>("");
    private final ValueSignal<@Nullable GeolocationPosition> pinnedSignal = new ValueSignal<@Nullable GeolocationPosition>(
            null);

    private final TextField description = new TextField("Description");
    private final Button pin = new Button("Pin my location");
    private final Span pinLabel = new Span();
    private final Button submit = new Button("Report pothole");
    private final Grid<PotholeReport> grid = new Grid<>(PotholeReport.class,
            false);

    public FormFieldView() {
        add(new H1("UC7 — Location as part of a form"));
        add(new Paragraph(
                "Fill in a description and pin your current location. "
                        + "Submit is disabled until both are ready. "
                        + "Submitted reports appear in the grid below."));

        VerticalLayout form = new VerticalLayout();
        form.setPadding(false);
        form.setSpacing(true);

        description.setRequired(true);
        description.setWidthFull();
        description.bindValue(descriptionSignal, descriptionSignal::set);

        pinLabel.bindText(
                pinnedSignal.map(p -> p == null ? "No location pinned yet"
                        : "Pinned at %.5f, %.5f (±%.0f m)".formatted(
                                p.coords().latitude(), p.coords().longitude(),
                                p.coords().accuracy())));

        pin.addClickListener(e -> capture());

        submit.bindEnabled(() -> !descriptionSignal.get().isEmpty()
                && pinnedSignal.get() != null);
        submit.addClickListener(e -> submit());

        form.add(description, pin, pinLabel, submit);
        add(form);

        add(new H2("Reported potholes"));
        configureGrid();
        add(grid);
    }

    private void configureGrid() {
        grid.addColumn(PotholeReport::description).setHeader("Description")
                .setAutoWidth(true);
        grid.addColumn(r -> "%.5f".formatted(r.latitude()))
                .setHeader("Latitude").setAutoWidth(true);
        grid.addColumn(r -> "%.5f".formatted(r.longitude()))
                .setHeader("Longitude").setAutoWidth(true);
        grid.addColumn(r -> "%.0f m".formatted(r.accuracy()))
                .setHeader("Accuracy").setAutoWidth(true);
        grid.setAllRowsVisible(true);
        grid.setItems(reports);
    }

    private void capture() {
        GeolocationOptions opts = GeolocationOptions.builder()
                .highAccuracy(true).timeout(Duration.ofSeconds(10))
                .maximumAge(Duration.ZERO).build();
        Geolocation.getPosition(pos -> {
            if (pos.coords().accuracy() > MAX_ACCURACY_METRES) {
                Notification.show(
                        "Location too imprecise (±%.0f m), please try again."
                                .formatted(pos.coords().accuracy()));
                return;
            }
            pinnedSignal.set(pos);
        }, err -> Notification.show(switch (err.errorCode()) {
        case PERMISSION_DENIED -> "Could not pin location: permission denied.";
        case POSITION_UNAVAILABLE ->
            "Could not pin location: location unavailable.";
        case TIMEOUT -> "Could not pin location: request timed out.";
        case UNKNOWN -> "Could not pin your location.";
        }), opts);
    }

    private void submit() {
        GeolocationPosition p = pinnedSignal.peek();
        if (p == null) {
            return;
        }
        reports.add(new PotholeReport(descriptionSignal.peek(),
                p.coords().latitude(), p.coords().longitude(),
                p.coords().accuracy()));
        grid.getDataProvider().refreshAll();
        Notification.show("Report filed");

        descriptionSignal.set("");
        pinnedSignal.set(null);
    }

    private record PotholeReport(String description, double latitude,
            double longitude, double accuracy) {
    }
}
