package com.example.uc4;

import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.fullscreen.Fullscreen;
import com.vaadin.flow.component.fullscreen.FullscreenState;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;

/**
 * UC4 — Reactive layout via {@code Fullscreen.stateSignal()}.
 * <p>
 * Demonstrates that the fullscreen state is just a signal: no callback, no
 * imperative wiring. The dashboard below changes its column count and the
 * density-aside text purely through {@code bindClassName} / {@code bindText}
 * subscriptions to {@link Fullscreen#stateSignal()}, with no observers in user
 * code. The user clicks Present once; the layout reformats automatically and
 * reverts on exit.
 */
@Route(value = "uc4", layout = MainLayout.class)
@Menu(order = 4, title = "UC4 — Reactive layout")
@StyleSheet("uc4.css")
public class ReactiveLayoutView extends VerticalLayout {

    private record Metric(String label, String value) {
    }

    private static final List<Metric> METRICS = List.of(
            new Metric("Daily active users", "12 480"),
            new Metric("Orders today", "1 287"),
            new Metric("Revenue (€)", "84 312"),
            new Metric("Avg. response time", "184 ms"),
            new Metric("Errors / min", "0.42"),
            new Metric("Cache hit rate", "97.3 %"));

    private final Div dashboard = new Div();
    private final Span densityNote = new Span();
    private final Span stateBadge = new Span();

    public ReactiveLayoutView() {
        addClassName("uc4-view");
        add(new H1("UC4 — Reactive layout"));
        add(new Paragraph(
                "Click “Present” once. The 6 metric cards rearrange from "
                        + "stacked → two columns → three columns as the page "
                        + "moves through NOT_FULLSCREEN → FULLSCREEN states. "
                        + "There is no observer in this view's code — the "
                        + "layout is bound directly to fullscreenSignal()."));

        stateBadge.addClassName("status-badge");
        densityNote.addClassName("density-aside");

        dashboard.addClassName("dashboard");
        for (Metric metric : METRICS) {
            dashboard.add(metricCard(metric));
        }

        Button present = new Button("Present");
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Present button's click trigger.
        Fullscreen.onClick(present).enter();
        Button exit = new Button("Exit", e -> Fullscreen.exit());

        add(new HorizontalLayout(present, exit, stateBadge, densityNote));
        add(dashboard);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();

        stateBadge.bindText(fs.map(ReactiveLayoutView::badgeText));
        stateBadge.bindClassName("fullscreen",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));

        dashboard.bindClassName("compact",
                fs.map(s -> s == FullscreenState.NOT_FULLSCREEN));
        dashboard.bindClassName("spacious",
                fs.map(s -> s == FullscreenState.FULLSCREEN));
        densityNote.bindText(fs.map(ReactiveLayoutView::densityText));
    }

    private static Div metricCard(Metric metric) {
        Div card = new Div();
        card.addClassName("metric-card");
        Span value = new Span(metric.value());
        value.addClassName("metric-value");
        Div label = new Div();
        label.addClassName("metric-label");
        label.setText(metric.label());
        card.add(value, label);
        return card;
    }

    private static String badgeText(FullscreenState state) {
        return switch (state) {
        case FULLSCREEN -> "Fullscreen — three columns";
        case NOT_FULLSCREEN -> "Windowed — two columns";
        case UNSUPPORTED -> "Fullscreen unsupported — stacked";
        case UNKNOWN -> "Detecting…";
        };
    }

    private static String densityText(FullscreenState state) {
        return switch (state) {
        case FULLSCREEN -> "Density: spacious (3 columns)";
        case NOT_FULLSCREEN -> "Density: compact (2 columns)";
        case UNSUPPORTED, UNKNOWN -> "Density: stacked (1 column)";
        };
    }
}
