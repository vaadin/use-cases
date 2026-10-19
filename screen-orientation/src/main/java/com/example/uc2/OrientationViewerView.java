package com.example.uc2;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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
 * UC2 — Orientation viewer.
 * <p>
 * Renders everything the screen orientation signal exposes: the
 * {@link ScreenOrientationType} enum value, the rotation angle, the two derived
 * predicates {@link ScreenOrientationType#isLandscape()} /
 * {@link ScreenOrientationType#isPortrait()}, and the distinction between
 * {@link ScreenOrientationType#UNKNOWN} (no data yet) and
 * {@link ScreenOrientationType#UNSUPPORTED} (the browser does not implement the
 * Screen Orientation API). An arrow rotates to match the reported angle. The
 * angle is measured from the device's natural orientation, which is portrait on
 * most phones and landscape on most desktops.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Orientation viewer")
@UseCaseDescription("Reading the device orientation, angle and support state live")
@Menu(order = 2, title = "UC2 — Orientation viewer")
@StyleSheet("uc2.css")
public class OrientationViewerView extends VerticalLayout {

    public OrientationViewerView() {
        addClassName("uc2-view");
        add(new H1("UC2 — Orientation viewer"));
        add(new Paragraph("Live readout of the screen orientation signal. "
                + "The arrow rotates to match the reported angle. The angle "
                + "is measured from the device's natural orientation: "
                + "portrait on most phones, landscape on most desktops. The "
                + "badge says whether the browser supports the Screen "
                + "Orientation API at all."));

        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal();

        Div arrow = new Div("⬆");
        arrow.addClassName("uc2-arrow");
        arrow.getStyle().bind("transform",
                orientation.map(d -> "rotate(" + d.angle() + "deg)"));
        add(arrow);

        Span supportBadge = new Span();
        supportBadge.addClassName("status-badge");
        supportBadge.bindText(orientation.map(OrientationViewerView::support));
        supportBadge.bindClassName("warn", orientation
                .map(d -> d.type() == ScreenOrientationType.UNKNOWN));
        supportBadge.bindClassName("error", orientation
                .map(d -> d.type() == ScreenOrientationType.UNSUPPORTED));
        add(supportBadge);

        Div table = new Div();
        table.addClassName("uc2-table");
        table.add(label("type"), value(orientation.map(d -> d.type().name())));
        table.add(label("angle"), value(orientation.map(d -> d.angle() + "°")));
        table.add(label("isLandscape()"), value(orientation
                .map(d -> Boolean.toString(d.type().isLandscape()))));
        table.add(label("isPortrait()"), value(
                orientation.map(d -> Boolean.toString(d.type().isPortrait()))));
        add(table);
    }

    private static Span label(String text) {
        Span s = new Span(text);
        s.addClassName("uc2-label");
        return s;
    }

    private static Span value(Signal<String> text) {
        Span s = new Span();
        s.bindText(text);
        return s;
    }

    private static String support(ScreenOrientationData data) {
        return switch (data.type()) {
        case UNKNOWN -> "Waiting for the browser to report…";
        case UNSUPPORTED -> "Screen Orientation API not supported";
        case PORTRAIT_PRIMARY, PORTRAIT_SECONDARY, LANDSCAPE_PRIMARY,
                LANDSCAPE_SECONDARY ->
            "Supported — current type: " + data.type().getClientValue();
        };
    }
}
