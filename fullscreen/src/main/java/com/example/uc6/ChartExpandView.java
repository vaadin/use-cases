package com.example.uc6;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

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
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * UC6 — Chart expand.
 * <p>
 * A dashboard with several {@link Chart} cards, each with its own Expand
 * button. Clicking Expand fullscreens that card via
 * {@link Fullscreen#onClick(com.vaadin.flow.component.Component)
 * Fullscreen.onClick(expand).enter(card)}. The expanded card needs its own
 * styles (full height, a taller chart), but the fullscreen API only exposes the
 * global {@link Fullscreen#stateSignal() state signal}; it does not say which
 * component is fullscreen (API-GAPS.md #2). As a workaround, the card that was
 * last expanded is remembered in an {@code activeCard} signal set on the button
 * click, and a card gets the expanded class while it is the active card and the
 * state is {@link FullscreenState#FULLSCREEN FULLSCREEN}.
 * <p>
 * Highcharts animations are disabled on the column series so the chart neither
 * morphs on initial render nor on exit-fullscreen resize — the user only sees
 * the card shrink, not a separate bar re-tween.
 */
@Route(value = "uc6", layout = MainLayout.class)
@PageTitle("UC6 — Chart expand")
@UseCaseDescription("Expanding a single dashboard chart card to fullscreen")
@Menu(order = 6, title = "UC6 — Chart expand")
@StyleSheet("uc6.css")
public class ChartExpandView extends VerticalLayout {

    private static final List<String> CHART_TITLES = List.of("Visitors",
            "Conversion", "Revenue");

    private final Span stateBadge = new Span();
    // The card the user last expanded: the API does not say which component
    // is fullscreen (API-GAPS.md #2).
    private final ValueSignal<Optional<Component>> activeCard = new ValueSignal<>(
            Optional.empty());
    private final Signal<FullscreenState> fs = Fullscreen.stateSignal();

    public ChartExpandView() {
        addClassName("uc6-view");
        add(new H1("UC6 — Chart expand"));
        add(new Paragraph(
                "Click Expand on any card to fill the screen with that chart "
                        + "alone. Press Escape to return to the dashboard. "
                        + "The API does not say which component is "
                        + "fullscreen, so the Expand click records the card, "
                        + "and only that card gets the expanded styles while "
                        + "the page is fullscreen."));

        stateBadge.addClassName("status-badge");
        add(stateBadge);

        Div grid = new Div();
        grid.addClassName("uc6-grid");
        for (String title : CHART_TITLES) {
            grid.add(chartCard(title));
        }
        add(grid);

        stateBadge.bindText(fs.map(ChartExpandView::badgeText));
        stateBadge.bindClassName("unsupported",
                fs.map(s -> s == FullscreenState.UNSUPPORTED));
    }

    private Div chartCard(String title) {
        Div card = new Div();
        card.addClassName("uc6-card");
        // Also check the state: a rejected request or a later exit leaves
        // activeCard set, but the card must only look expanded in fullscreen.
        card.bindClassName("uc6-card-expanded",
                Signal.computed(() -> fs.get() == FullscreenState.FULLSCREEN
                        && activeCard.get().orElse(null) == card));

        Span heading = new Span(title);
        Button expand = new Button("Expand");
        expand.addClickListener(e -> activeCard.set(Optional.of(card)));
        // Fullscreen needs the click's user gesture, so bind the request to the
        // Expand button's click trigger.
        Fullscreen.onClick(expand).enter(card);
        expand.addThemeVariants(ButtonVariant.SMALL);
        HorizontalLayout header = new HorizontalLayout(heading, expand);
        header.addClassName("uc6-card-header");
        header.setWidthFull();
        card.add(header);

        Chart chart = new Chart(ChartType.COLUMN);
        chart.addClassName("uc6-chart");
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
