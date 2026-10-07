package com.example.uc1;

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
 * UC1 — Adaptive layout: portrait vs landscape.
 * <p>
 * Two information panes are arranged side-by-side when the device is in
 * landscape orientation and stacked vertically when the device is in portrait.
 * The reactive switch is driven entirely by
 * {@link com.vaadin.flow.component.screenorientation.ScreenOrientation#orientationSignal()
 * ScreenOrientation.orientationSignal()}; rotating the device (or, in a
 * browser, using the devtools "responsive" rotate button) flips the arrangement
 * immediately.
 */
@Route(value = "uc1", layout = MainLayout.class)
@PageTitle("UC1 — Adaptive layout")
@Menu(order = 1, title = "UC1 — Adaptive layout")
@StyleSheet("uc1.css")
public class AdaptiveLayoutView extends VerticalLayout {

    private final Div container = new Div();
    private final Span modeBadge = new Span();

    public AdaptiveLayoutView() {
        addClassName("uc1-view");
        add(new H1("UC1 — Adaptive layout"));
        add(new Paragraph("The two panes below render side by side when the "
                + "device is in landscape orientation and stack vertically "
                + "in portrait. Resize or rotate to see the layout react."));

        modeBadge.addClassName("status-badge");
        add(modeBadge);

        container.addClassName("uc1-container");
        container.add(pane("Primary", "Main content goes here. In landscape "
                + "it occupies the left half; in portrait it sits on top."));
        container.add(pane("Secondary", "Companion content. In landscape it "
                + "sits to the right; in portrait it stacks below."));
        add(container);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<ScreenOrientationData> orientation = ScreenOrientation
                .orientationSignal(attachEvent.getUI());

        Signal<Boolean> landscape = orientation
                .map(d -> d.type().isLandscape());
        container.bindClassName("side-by-side", landscape);
        container.bindClassName("stacked", landscape.map(b -> !b));

        modeBadge.bindText(orientation.map(AdaptiveLayoutView::label));
        modeBadge.bindClassName("warn",
                orientation.map(d -> d.type() == ScreenOrientationType.UNKNOWN
                        || d.type() == ScreenOrientationType.UNSUPPORTED));
    }

    private static Div pane(String title, String body) {
        Div pane = new Div();
        pane.addClassName("uc1-pane");
        Div t = new Div(title);
        t.addClassName("uc1-pane-title");
        pane.add(t, new Span(body));
        return pane;
    }

    private static String label(ScreenOrientationData data) {
        return switch (data.type()) {
        case LANDSCAPE_PRIMARY, LANDSCAPE_SECONDARY ->
            "Landscape — side-by-side layout";
        case PORTRAIT_PRIMARY, PORTRAIT_SECONDARY ->
            "Portrait — stacked layout";
        case UNSUPPORTED ->
            "Screen Orientation API not supported — defaulting to stacked";
        case UNKNOWN -> "Orientation unknown — defaulting to stacked";
        };
    }
}
