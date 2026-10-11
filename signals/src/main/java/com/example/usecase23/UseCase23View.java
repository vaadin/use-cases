package com.example.usecase23;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import com.example.common.UseCaseDescription;
import com.example.usecase23.ServiceHealth.Status;
import com.example.views.MainLayout;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.board.Board;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.Configuration;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsAreaspline;
import com.vaadin.flow.component.charts.model.PointPlacement;
import com.vaadin.flow.component.charts.model.XAxis;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.grid.ColumnTextAlign;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Main;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.data.renderer.ComponentRenderer;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

@PageTitle("Use Case 23: Real-time Dashboard")
@UseCaseDescription("Updating a live service-health dashboard from background data")
@Route(value = "use-case-23", layout = MainLayout.class)
@Menu(order = 23, title = "UC 23: Real-time Dashboard")
@StyleSheet("usecase23.css")
@AnonymousAllowed
public class UseCase23View extends Main {

    private static final int TIMELINE_POINTS = 12;

    // state
    private final ValueSignal<Number> currentUsersSignal = new ValueSignal<>(0);
    private final ValueSignal<Number> viewEventsSignal = new ValueSignal<>(0);
    private final ValueSignal<Number> conversionRateSignal = new ValueSignal<>(
            0);
    private final ValueSignal<Number> customMetricSignal = new ValueSignal<>(0);

    private final ListSignal<String> timelineCategoriesSignal = new ListSignal<>();
    private final ListSignal<Number> berlinTimelineSignal = new ListSignal<>();
    private final ListSignal<Number> londonTimelineSignal = new ListSignal<>();
    private final ListSignal<Number> newYorkTimelineSignal = new ListSignal<>();
    private final ListSignal<Number> tokyoTimelineSignal = new ListSignal<>();
    private final ListSignal<ServiceHealth> serviceHealthSignal = new ListSignal<>();
    private final ListSignal<Number> responseSignal = new ListSignal<>();

    private @Nullable String taskId;

    public UseCase23View(SchedulerService schedulerService) {
        addClassName("usecase23-view");

        Board board = new Board();
        board.addRow(
                createHighlightCard("Current users", currentUsersSignal,
                        this::formatNumber),
                createHighlightCard("View events", viewEventsSignal,
                        this::formatCompactNumber),
                createHighlightCard("Conversion rate", conversionRateSignal,
                        number -> String.format("%.1f%%",
                                number.doubleValue())),
                createHighlightCard("Custom metric", customMetricSignal,
                        this::formatNumber));
        board.addRow(createViewEvents());
        board.addRow(createServiceHealth(), createResponseTimes());
        add(board);

        addAttachListener(event -> {
            UI ui = event.getUI();
            taskId = "dashboard-" + ui.getUIId();
            schedulerService.scheduleDashboardDataUpdate(taskId, ui,
                    this::onDataUpdate, 0, 2, TimeUnit.SECONDS);
        });

        addDetachListener(event -> {
            if (taskId != null) {
                schedulerService.cancelTask(taskId);
            }
        });
    }

    private HighlightCard createHighlightCard(String title,
            ValueSignal<Number> signal, Function<Number, String> format) {
        return new HighlightCard(title, signal, format);
    }

    private Component createViewEvents() {
        // Header
        HorizontalLayout header = createHeader("View events",
                "City / last 2 min");

        // Chart
        Chart chart = new Chart(ChartType.AREASPLINE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);

        XAxis xAxis = new XAxis();
        conf.addxAxis(xAxis);

        conf.getyAxis().setTitle("Values");

        PlotOptionsAreaspline plotOptions = new PlotOptionsAreaspline();
        plotOptions.setPointPlacement(PointPlacement.ON);
        plotOptions.setMarker(new Marker(false));
        conf.addPlotOptions(plotOptions);

        ListSeries berlinSeries = new ListSeries("Berlin", new Number[0]);
        ListSeries londonSeries = new ListSeries("London", new Number[0]);
        ListSeries newYorkSeries = new ListSeries("New York", new Number[0]);
        ListSeries tokyoSeries = new ListSeries("Tokyo", new Number[0]);

        bindData(chart, berlinSeries, berlinTimelineSignal);
        bindData(chart, londonSeries, londonTimelineSignal);
        bindData(chart, newYorkSeries, newYorkTimelineSignal);
        bindData(chart, tokyoSeries, tokyoTimelineSignal);

        Signal.effect(chart, () -> xAxis.setCategories(
                timelineCategoriesSignal.getValues().toArray(String[]::new)));

        conf.addSeries(berlinSeries);
        conf.addSeries(londonSeries);
        conf.addSeries(newYorkSeries);
        conf.addSeries(tokyoSeries);

        // Trigger chart redraw when any timeline signal changes
        Signal.effect(chart, () -> {
            berlinTimelineSignal.get();
            londonTimelineSignal.get();
            newYorkTimelineSignal.get();
            tokyoTimelineSignal.get();
            chart.drawChart();
        });

        // Add it all together
        VerticalLayout viewEvents = new VerticalLayout(header, chart);
        viewEvents.setSpacing(false);
        viewEvents.getElement().getThemeList().add("spacing-l");
        return viewEvents;
    }

    private static void bindData(Chart chart, ListSeries series,
            ListSignal<Number> signal) {
        Signal.effect(chart, () -> series
                .setData(signal.getValues().toArray(Number[]::new)));
    }

    private Component createServiceHealth() {
        // Header
        HorizontalLayout header = createHeader("Service health",
                "Input / output");

        // Grid
        Grid<ValueSignal<ServiceHealth>> grid = new Grid<>();
        grid.addThemeVariants(GridVariant.NO_BORDER);
        grid.setAllRowsVisible(true);

        grid.addColumn(new ComponentRenderer<>(signal -> {
            Span status = new Span();
            Signal<String> statusText = Signal
                    .computed(() -> getStatusDisplayName(signal.get()));
            status.getElement().bindAttribute("aria-label",
                    () -> "Status: " + statusText.get());
            status.getElement().bindAttribute("title",
                    () -> "Status: " + statusText.get());
            status.getElement().getThemeList()
                    .addAll(List.of("badge", "primary"));
            status.getElement().getThemeList().bind(() -> {
                return switch (signal.get().getStatus()) {
                case EXCELLENT -> List.of("success");
                case FAILING -> List.of("error");
                default -> List.of();
                };
            });
            return status;
        })).setHeader("").setFlexGrow(0).setAutoWidth(true);
        grid.addColumn(signal -> signal.peek().getCity()).setHeader("City")
                .setFlexGrow(1);
        grid.addColumn(new ComponentRenderer<>(signal -> new Span(
                () -> String.valueOf(signal.get().getInput()))))
                .setHeader("Input").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);
        grid.addColumn(new ComponentRenderer<>(signal -> new Span(
                () -> String.valueOf(signal.get().getOutput()))))
                .setHeader("Output").setAutoWidth(true)
                .setTextAlign(ColumnTextAlign.END);

        // Add it all together
        VerticalLayout serviceHealth = new VerticalLayout(header, grid);

        Signal.effect(grid, () -> {
            grid.setItems(serviceHealthSignal.get());
            // TODO not this, update each signal individually
        });

        // Initialize with empty service health entries (will be populated by
        // scheduler)
        List.of("Münster", "Cluj-Napoca", "Ciudad Victoria").forEach(city -> {
            serviceHealthSignal
                    .insertLast(new ServiceHealth(Status.OK, city, 0, 0));
        });

        return serviceHealth;
    }

    private Component createResponseTimes() {
        HorizontalLayout header = createHeader("Response times",
                "Average across all systems");

        // Chart
        Chart chart = new Chart(ChartType.PIE);
        Configuration conf = chart.getConfiguration();
        conf.getChart().setStyledMode(true);
        chart.setThemeName("gradient");

        DataSeries responseSeries = new DataSeries();
        responseSeries.add(new DataSeriesItem("System 1", 12.5));
        responseSeries.add(new DataSeriesItem("System 2", 12.5));
        responseSeries.add(new DataSeriesItem("System 3", 12.5));
        responseSeries.add(new DataSeriesItem("System 4", 12.5));
        responseSeries.add(new DataSeriesItem("System 5", 12.5));
        responseSeries.add(new DataSeriesItem("System 6", 12.5));
        conf.addSeries(responseSeries);

        Signal.effect(chart, () -> {
            var responseValues = responseSignal.get();
            responseSeries.get(0).setY(responseValues.get(0).get());
            responseSeries.get(1).setY(responseValues.get(1).get());
            responseSeries.get(2).setY(responseValues.get(2).get());
            responseSeries.get(3).setY(responseValues.get(3).get());
            responseSeries.get(4).setY(responseValues.get(4).get());
            responseSeries.get(5).setY(responseValues.get(5).get());
            chart.drawChart();
        });

        responseSignal.insertLast(12.5);
        responseSignal.insertLast(12.5);
        responseSignal.insertLast(12.5);
        responseSignal.insertLast(12.5);
        responseSignal.insertLast(12.5);
        responseSignal.insertLast(12.5);

        // Add it all together
        VerticalLayout serviceHealth = new VerticalLayout(header, chart);
        serviceHealth.setSpacing(false);
        return serviceHealth;
    }

    private HorizontalLayout createHeader(String title, String subtitle) {
        H2 h2 = new H2(title);
        h2.addClassName("panel-header-title");

        Span span = new Span(subtitle);
        span.addClassName("panel-header-subtitle");

        VerticalLayout column = new VerticalLayout(h2, span);
        column.setPadding(false);
        column.setSpacing(false);

        HorizontalLayout header = new HorizontalLayout(column);
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setSpacing(false);
        header.setWidthFull();
        return header;
    }

    private String getStatusDisplayName(ServiceHealth serviceHealth) {
        Status status = serviceHealth.getStatus();
        if (status == Status.OK) {
            return "Ok";
        } else if (status == Status.FAILING) {
            return "Failing";
        } else if (status == Status.EXCELLENT) {
            return "Excellent";
        } else {
            return status.toString();
        }
    }

    /**
     * Callback invoked by the scheduler service with new dashboard data. This
     * method only updates signals - no UI access or chart drawing.
     */
    void onDataUpdate(DashboardData data) {
        // Update highlight card signals
        currentUsersSignal.set(data.currentUsers());
        viewEventsSignal.set(data.viewEvents());
        conversionRateSignal.set(data.conversionRate());
        customMetricSignal.set(data.customMetric());

        // Update timeline signals
        DashboardData.TimelineData timeline = data.timelineData();

        if (berlinTimelineSignal.peek().size() >= TIMELINE_POINTS) {
            berlinTimelineSignal.remove(berlinTimelineSignal.peek().getFirst());
        }
        berlinTimelineSignal.insertLast(timeline.berlinValue());

        if (londonTimelineSignal.peek().size() >= TIMELINE_POINTS) {
            londonTimelineSignal.remove(londonTimelineSignal.peek().getFirst());
        }
        londonTimelineSignal.insertLast(timeline.londonValue());

        if (newYorkTimelineSignal.peek().size() >= TIMELINE_POINTS) {
            newYorkTimelineSignal
                    .remove(newYorkTimelineSignal.peek().getFirst());
        }
        newYorkTimelineSignal.insertLast(timeline.newYorkValue());

        if (tokyoTimelineSignal.peek().size() >= TIMELINE_POINTS) {
            tokyoTimelineSignal.remove(tokyoTimelineSignal.peek().getFirst());
        }
        tokyoTimelineSignal.insertLast(timeline.tokyoValue());

        if (timelineCategoriesSignal.peek().size() >= TIMELINE_POINTS) {
            timelineCategoriesSignal
                    .remove(timelineCategoriesSignal.peek().getFirst());
        }
        timelineCategoriesSignal.insertLast(timeline.timestamp());

        // Update response times
        List<Double> responseTimes = data.responseTimes();
        for (int i = 0; i < responseSignal.peek().size()
                && i < responseTimes.size(); i++) {
            responseSignal.peek().get(i).set(responseTimes.get(i));
        }

        // Update service health
        var healthValues = serviceHealthSignal.peek();
        data.serviceHealthList().forEach(
                newHealth -> healthValues.forEach(currentHealthSignal -> {
                    var currentHealth = currentHealthSignal.peek();
                    if (Objects.equals(currentHealth.getCity(),
                            newHealth.getCity())) {
                        currentHealthSignal.set(newHealth);
                    }
                }));
    }

    private String formatNumber(Number value) {
        return String.valueOf(value);
    }

    private String formatCompactNumber(Number value) {
        if (value.doubleValue() >= 1000) {
            double rounded = Math.round(value.doubleValue() / 100.0) / 10.0;
            return rounded + "k";
        }
        return String.valueOf(value);
    }

    private static final class HighlightCard extends VerticalLayout {
        record Change(double previous, double current) {
        }

        private HighlightCard(String title, ValueSignal<Number> signal,
                Function<Number, String> format) {

            // previous-current value holder
            ValueSignal<Change> changeSignal = new ValueSignal<>(new Change(
                    signal.peek().doubleValue(), signal.peek().doubleValue()));

            // update previous value when the main signal changes
            Signal.effect(this, () -> {
                double current = signal.get().doubleValue();
                double previous = changeSignal.peek().current();
                changeSignal.set(new Change(previous, current));
            });

            // Computed signal for percentage change
            Signal<Double> percentageSignal = changeSignal
                    .map(change -> calculatePercentageChange(change.current(),
                            change.previous()));

            // Computed signals using method references
            Signal<String> prefixSignal = percentageSignal.map(this::getPrefix);
            Signal<VaadinIcon> iconSignal = percentageSignal.map(this::getIcon);
            Signal<Boolean> successSignal = percentageSignal
                    .map(percentage -> percentage > 0);

            H2 h2 = new H2(title);
            h2.addClassName("highlight-card-title");

            Span valueSpan = new Span();
            valueSpan.addClassName("highlight-card-value");
            valueSpan.bindText(signal.map(format::apply));

            Span percentageSpan = new Span();
            percentageSpan.bindText(Signal.computed(
                    () -> prefixSignal.get() + percentageSignal.get()));

            Icon icon = new Icon(iconSignal);
            icon.setSize("10px");
            icon.addClassName("highlight-card-icon");

            Span badge = new Span();
            badge.add(icon, percentageSpan);
            badge.getElement().getThemeList()
                    .bind(() -> successSignal.get()
                            ? List.of("badge", "success")
                            : List.of("badge", "error"));

            add(h2, valueSpan, badge);
            addClassName("highlight-card");
        }

        private String getPrefix(double percentage) {
            if (percentage == 0) {
                return "±";
            } else if (percentage > 0) {
                return "+";
            } else {
                return "";
            }
        }

        private VaadinIcon getIcon(double percentage) {
            return percentage < 0 ? VaadinIcon.ARROW_DOWN : VaadinIcon.ARROW_UP;
        }

        private double calculatePercentageChange(double current,
                double previous) {
            if (previous == 0.0) {
                return 0.0;
            }
            double percent = ((current - previous) / Math.abs(previous))
                    * 100.0;
            return Math.round(percent * 10.0) / 10.0;
        }
    }

}
