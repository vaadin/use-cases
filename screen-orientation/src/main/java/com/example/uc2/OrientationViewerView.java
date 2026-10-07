package com.example.uc2;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.screenorientation.ScreenOrientation;
import com.vaadin.flow.component.screenorientation.ScreenOrientationData;
import com.vaadin.flow.component.screenorientation.ScreenOrientationType;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC2 — Live orientation viewer.
 * <p>
 * Renders everything the screen orientation signal exposes: the
 * {@link ScreenOrientationType} enum value, the rotation angle, the two derived
 * predicates {@link ScreenOrientationType#isLandscape()} /
 * {@link ScreenOrientationType#isPortrait()}, and the distinction between
 * {@link ScreenOrientationType#UNKNOWN} (no data yet) and
 * {@link ScreenOrientationType#UNSUPPORTED} (the browser does not implement the
 * Screen Orientation API). An emoji arrow rotates to match the reported angle.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Orientation viewer")
@Menu(order = 2, title = "UC2 — Orientation viewer")
@StyleSheet("uc2.css")
public class OrientationViewerView extends VerticalLayout {

    private final Div arrow = new Div();
    private final Span typeValue = new Span();
    private final Span angleValue = new Span();
    private final Span isLandscapeValue = new Span();
    private final Span isPortraitValue = new Span();
    private final Span supportBadge = new Span();

    public OrientationViewerView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Orientation viewer"));
        add(new Paragraph("Live readout of the Screen Orientation signal. "
                + "The arrow rotates to match the reported angle. The "
                + "\"support\" badge tells the API-availability story: "
                + "UNKNOWN before the bootstrap has arrived, UNSUPPORTED "
                + "on browsers without the Screen Orientation API, "
                + "otherwise the concrete orientation value."));

        arrow.addClassName("uc2-arrow");
        arrow.setText("⬆");
        add(arrow);

        supportBadge.addClassName("status-badge");
        add(supportBadge);

        Div table = new Div();
        table.addClassName("uc2-table");
        table.add(label("type"), typeValue);
        table.add(label("angle"), angleValue);
        table.add(label("isLandscape()"), isLandscapeValue);
        table.add(label("isPortrait()"), isPortraitValue);
        add(table);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal(attachEvent.getUI());

        typeValue.bindText(orientation.map(d -> d.type().name()));
        angleValue.bindText(orientation.map(d -> d.angle() + "°"));
        isLandscapeValue.bindText(
                orientation.map(d -> Boolean.toString(d.type().isLandscape())));
        isPortraitValue.bindText(
                orientation.map(d -> Boolean.toString(d.type().isPortrait())));

        supportBadge.bindText(orientation.map(OrientationViewerView::support));
        supportBadge.bindClassName("warn", orientation
                .map(d -> d.type() == ScreenOrientationType.UNKNOWN));
        supportBadge.bindClassName("error", orientation
                .map(d -> d.type() == ScreenOrientationType.UNSUPPORTED));

        // Rotate the arrow via inline transform; matches the signal angle.
        Signal.effect(this, () -> {
            int angle = orientation.get().angle();
            arrow.getStyle().set("transform", "rotate(" + angle + "deg)");
        });
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("label");
        return s;
    }

    private static String support(ScreenOrientationData data) {
        return switch (data.type()) {
        case UNKNOWN -> "Waiting for client bootstrap…";
        case UNSUPPORTED -> "Screen Orientation API not supported";
        case PORTRAIT_PRIMARY, PORTRAIT_SECONDARY, LANDSCAPE_PRIMARY,
                LANDSCAPE_SECONDARY ->
            "Supported — current type: " + data.type().getClientValue();
        };
    }
}
