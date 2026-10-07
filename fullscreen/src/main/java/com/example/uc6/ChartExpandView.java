package com.example.uc6;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.PlotOptionsColumn;
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
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Chart expand-to-fullscreen.
 * <p>
 * A dashboard with several {@link Chart} cards, each with its own Expand
 * button. Clicking Expand fullscreens that card via
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(expand).enter(card)}. The fullscreen API exposes a single
 * global {@link Fullscreen#stateSignal() state signal}, so the card that was
 * last expanded is remembered in a small {@code activeOwner} signal (set on the
 * button click, cleared when the state signal leaves
 * {@link FullscreenState#FULLSCREEN}). Each card binds the {@code expanded} CSS
 * class only when it is the active one — even though the wrapper hides the rest
 * of the dashboard during fullscreen, this keeps the binding semantically
 * correct (and lets devtools/tests inspect which card is active).
 * <p>
 * Highcharts animations are disabled on the column series so the chart neither
 * morphs on initial render nor on exit-fullscreen resize — the user only sees
 * the card shrink, not a separate bar re-tween.
 */
@Route(value = "uc6", layout = MainLayout.class)
@Menu(order = 6, title = "UC6 — Chart expand")
@StyleSheet("uc6.css")
public class ChartExpandView extends VerticalLayout {

    private static final List<String> CHART_TITLES = List.of("Visitors",
            "Conversion", "Revenue");

    private final Span stateBadge = new Span();
    private final ValueSignal<Optional<Component>> activeOwner = new ValueSignal<>(
            Optional.empty());
    private final List<Div> cards = new ArrayList<>();

    public ChartExpandView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Chart expand-to-fullscreen"));
        add(new Paragraph(
                "Click Expand on any card to fill the screen with that chart "
                        + "alone. Press Escape to return to the dashboard. "
                        + "Each card hosts a Vaadin Chart; the button click "
                        + "records which card is the active one so only that "
                        + "card gets the expanded CSS class, and the active "
                        + "card is cleared when fullscreen ends."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        Div grid = new Div();
        grid.addClassName("chart-grid");
        for (String title : CHART_TITLES) {
            Div card = chartCard(title);
            cards.add(card);
            grid.add(card);
        }
        add(grid);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        Signal<FullscreenState> fs = Fullscreen.stateSignal();
        stateBadge.bindText(fs.map(ChartExpandView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));

        // Clear the active card whenever fullscreen ends — whether the user
        // pressed Escape or the request was superseded.
        Signal.effect(this, () -> {
            if (fs.get() != FullscreenState.FULLSCREEN
                    && activeOwner.peek().isPresent()) {
                activeOwner.set(Optional.empty());
            }
        });

        for (Div card : cards) {
            card.bindClassName("expanded",
                    activeOwner.map(o -> o.isPresent() && o.get() == card));
        }
    }

    private Div chartCard(String title) {
        Div card = new Div();
        card.addClassName("chart-card");

        Span heading = new Span(title);
        Button expand = new Button("Expand");
        // Reflect the active card the moment the user clicks so the expanded
        // class appears immediately; the onAttach effect clears it on exit.
        expand.addClickListener(e -> activeOwner.set(Optional.of(card)));
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Expand button's click trigger.
        Fullscreen.onClick(expand).enter(card);
        expand.addThemeVariants(ButtonVariant.SMALL);
        HorizontalLayout header = new HorizontalLayout(heading, expand);
        header.addClassName("chart-card-header");
        header.setWidthFull();
        card.add(header);

        Chart chart = new Chart(ChartType.COLUMN);
        chart.addClassName("chart-card-chart");
        Configuration conf = chart.getConfiguration();
        conf.setTitle((String) null);
        conf.getLegend().setEnabled(false);
        conf.getxAxis().setVisible(false);
        conf.getyAxis().setVisible(false);
        conf.getChart().setStyledMode(true);

        PlotOptionsColumn columnOptions = new PlotOptionsColumn();
        // No tweened bar animation on initial render or container resize —
        // the card itself snaps between sizes, no separate bar animation.
        columnOptions.setAnimation(false);
        conf.addPlotOptions(columnOptions);

        Number[] data = new Number[12];
        for (int i = 0; i < data.length; i++) {
            data[i] = 25 + ThreadLocalRandom.current().nextInt(75);
        }
        conf.addSeries(new ListSeries(title, data));

        card.add(chart);
        return card;
    }

    private static String badgeText(FullscreenState state) {
        // FULLSCREEN: only the expanded card is visible, the badge is hidden.
        // Keep the idle text rather than flipping to a message no one sees.
        return switch (state) {
        case FULLSCREEN, NOT_FULLSCREEN -> "Click Expand on any card";
        case UNSUPPORTED -> "Fullscreen unsupported in this browser";
        case UNKNOWN -> "Detecting…";
        };
    }
}
