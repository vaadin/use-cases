package com.example.uc4;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import com.example.common.BuiltWith;
import com.example.common.UseCaseDescription;
import com.example.components.Sparkline;
import com.example.components.SparklineVariant;
import com.example.views.MainLayout;
import com.example.views.Sample;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Server-side DOM.
 * <p>
 * Support metrics for the last two weeks, each with a {@link Sparkline} in a
 * Grid column. The chart is plain SVG built with the Element API on the server,
 * so it needs no JavaScript and no npm package. "Next day" adds a data point;
 * only the changed attribute travels to the browser.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Server-side DOM")
@UseCaseDescription("Drawing a chart as SVG from Java with the Element API, no JavaScript")
@Menu(order = 4, title = "UC4 — Server-side DOM")
public class SparklineView extends VerticalLayout {

    /** A metric and its daily values. */
    public record Metric(String name, List<Double> values,
            SparklineVariant variant) {
    }

    private final Random random = new Random(42);
    private final List<Metric> metrics = new ArrayList<>();
    private final Grid<Metric> grid = new Grid<>();

    public SparklineView() {
        add(new H1("UC4 — Server-side DOM"));
        add(new Paragraph("Each chart is an SVG element built in Java. Add "
                + "a day of data and watch them move."));

        metrics.add(new Metric("Open tickets", series(40, 4),
                SparklineVariant.ERROR));
        metrics.add(new Metric("Solved per day", series(25, 5),
                SparklineVariant.SUCCESS));
        metrics.add(new Metric("First response, min", series(18, 3),
                SparklineVariant.FILLED));

        grid.addColumn(Metric::name).setHeader("Metric");
        grid.addColumn(
                metric -> String.format("%.0f", metric.values().getLast()))
                .setHeader("Today");
        grid.addComponentColumn(metric -> {
            Sparkline chart = new Sparkline(metric.values(), metric.name()
                    + ", last " + metric.values().size() + " days");
            chart.addThemeVariants(metric.variant());
            return chart;
        }).setHeader("Last days").setKey("chart");
        grid.setItems(metrics);
        grid.setAllRowsVisible(true);

        Button nextDay = new Button("Next day", e -> {
            for (int i = 0; i < metrics.size(); i++) {
                Metric metric = metrics.get(i);
                List<Double> values = new ArrayList<>(metric.values());
                values.removeFirst();
                values.add(values.getLast() + random.nextGaussian() * 3);
                metrics.set(i,
                        new Metric(metric.name(), values, metric.variant()));
            }
            grid.setItems(metrics);
        });

        add(new Sample("Support metrics", grid, nextDay));
        add(new BuiltWith("Sparkline",
                "Server-side DOM · Element API, no JavaScript")
                .point("@Tag(\"svg\") plus two child elements created with new Element(\"polyline\") and new Element(\"polygon\").")
                .point("setValues rewrites one attribute, and Flow sends only that change to the browser.")
                .point("The line is drawn in currentColor, so theme variants and CSS decide its color.")
                .java("components/Sparkline")
                .java("components/SparklineVariant")
                .resource("META-INF/resources/sparkline.css"));
    }

    private List<Double> series(double start, double spread) {
        List<Double> values = new ArrayList<>();
        double value = start;
        for (int day = 0; day < 14; day++) {
            values.add(value);
            value = Math.max(0, value + random.nextGaussian() * spread);
        }
        return values;
    }

    // Package-private test seams.
    Grid<Metric> grid() {
        return grid;
    }

    List<Metric> metrics() {
        return metrics;
    }
}
