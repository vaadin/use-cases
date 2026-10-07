package com.example.usecase14;

import com.example.service.AnalyticsService;
import com.example.service.AnalyticsService.AnalyticsReport;
import com.example.views.MainLayout;

import com.vaadin.flow.component.AttachEvent;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 14: Async Data Loading with States
 *
 * Demonstrates async operations with loading/success/error states using
 * analytics report generation as a realistic heavy operation example: - Spring
 * Boot @Async service for background processing - Signal with LoadingState
 * (Loading/Success/Error states) - Loading spinner while processing data -
 * Display analytics dashboard on success - Error message with retry button -
 * Proper separation of concerns (View → Service)
 *
 * Key Patterns: - Spring @Async service integration - CompletableFuture for
 * async operations - Async signal updates with UI thread synchronization -
 * Loading state representation - Error handling with retry - Dashboard-style
 * data visualization
 */
@Route(value = "use-case-14", layout = MainLayout.class)
@PageTitle("Use Case 14: Async Data Loading")
@Menu(order = 14, title = "UC 14: Async Data Loading")
@StyleSheet("usecase14.css")
@AnonymousAllowed
public class UseCase14View extends VerticalLayout {
    private enum LoadingState {
        IDLE, LOADING, GENERATING, SUCCESS, ERROR
    }

    private final AnalyticsService analyticsService;

    private final ValueSignal<LoadingState> stateSignal = new ValueSignal<>(
            LoadingState.IDLE);

    private final ValueSignal<AnalyticsReport> reportDataSignal = new ValueSignal<>(
            AnalyticsReport.empty());

    private final ValueSignal<Boolean> shouldFailSignal = new ValueSignal<>(
            false);

    public UseCase14View(AnalyticsService analyticsService) {
        this.analyticsService = analyticsService;
        addClassName("usecase14-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 14: Async Data Loading with States");

        Paragraph description = new Paragraph(
                "This use case demonstrates multi-step async operations with proper loading/success/error states. "
                        + "Click 'Generate Analytics Report' to start a two-step process: first fetching relevant data, then generating the report. "
                        + "Toggle 'Simulate Error' to see error handling. "
                        + "The UI reactively shows progress through each step, displays data on success, or shows error messages with retry.");

        // Controls
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);

        Button loadButton = new Button("Generate Analytics Report",
                event -> loadReport());
        loadButton.addThemeVariants(ButtonVariant.PRIMARY);

        // Disable load button while loading
        Signal<Boolean> isLoadingSignal = stateSignal
                .map(state -> state == LoadingState.LOADING
                        || state == LoadingState.GENERATING);
        loadButton.setDisableOnClick(true);
        // Can't directly bind when the button updates its own state
        Signal.effect(loadButton, () -> {
            if (!isLoadingSignal.get()) {
                loadButton.setEnabled(true);
            }
        });

        Div errorToggle = new Div();
        errorToggle.addClassName("error-toggle");

        var checkbox = new Checkbox("Simulate Error");
        checkbox.bindValue(shouldFailSignal, shouldFailSignal::set);
        errorToggle.add(checkbox);

        controls.add(loadButton, errorToggle);

        // State display box
        Div stateBox = new Div();
        stateBox.addClassName("state-box");

        // Idle state
        Div idleContent = new Div();
        Paragraph idleMessage = new Paragraph(
                "👆 Click 'Generate Analytics Report' to start the two-step process: fetch data, then generate comprehensive report with sales metrics and performance data");
        idleMessage.addClassName("idle-message");
        idleContent.add(idleMessage);
        idleContent.bindVisible(() -> stateSignal.get() == LoadingState.IDLE);

        // Loading state
        Div loadingContent = new Div();
        loadingContent.addClassName("loading-content");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("200px");

        Paragraph loadingMessage = new Paragraph(
                () -> switch (stateSignal.get()) {
                case LOADING -> "Fetching relevant data... (1/2)";
                case GENERATING -> "Generating report... (2/2)";
                default -> "";
                });
        loadingMessage.addClassName("loading-message");

        loadingContent.add(progressBar, loadingMessage);
        loadingContent.bindVisible(isLoadingSignal);

        // Success state
        Div successContent = new Div();
        H3 successTitle = new H3(
                reportDataSignal.map(report -> !report.isEmpty()
                        ? "Analytics Report - " + report.getPeriod()
                        : "Analytics Report"));
        successTitle.addClassName("success-title");

        // Metrics grid
        Div metricsGrid = new Div();
        metricsGrid.addClassName("metrics-grid");

        // Create metric cards with signals
        Signal<String> revenueSignal = reportDataSignal.map(report -> {
            if (!report.isEmpty()) {
                return String.format("$%,d", report.getTotalRevenue());
            }
            return "";
        });
        Card revenueCard = createMetricCardWithSignal("Total Revenue",
                revenueSignal, VaadinIcon.DOLLAR, "#4CAF50");

        Signal<String> ordersSignal = reportDataSignal.map(report -> {
            if (!report.isEmpty()) {
                return String.format("%,d", report.getTotalOrders());
            }
            return "";
        });
        Card ordersCard = createMetricCardWithSignal("Total Orders",
                ordersSignal, VaadinIcon.PACKAGE, "#2196F3");

        Signal<String> conversionSignal = reportDataSignal.map(report -> {
            if (!report.isEmpty()) {
                return String.format("%.2f%%", report.getConversionRate());
            }
            return "";
        });
        Card conversionCard = createMetricCardWithSignal("Conversion Rate",
                conversionSignal, VaadinIcon.TRENDING_UP, "#FF9800");

        Signal<String> usersSignalValue = reportDataSignal.map(report -> {
            if (!report.isEmpty()) {
                return String.format("%,d", report.getActiveUsers());
            }
            return "";
        });
        Card usersCard = createMetricCardWithSignal("Active Users",
                usersSignalValue, VaadinIcon.USERS, "#9C27B0");

        metricsGrid.add(revenueCard, ordersCard, conversionCard, usersCard);

        successContent.add(successTitle, metricsGrid);
        successContent
                .bindVisible(() -> stateSignal.get() == LoadingState.SUCCESS);

        // Error state
        Div errorContent = new Div();
        errorContent.addClassName("error-content");

        H3 errorTitle = new H3("❌ Report Generation Failed");
        errorTitle.addClassName("error-title");

        Paragraph errorMessage = new Paragraph(
                "Analytics report generation failed. Please contact the administrator.");
        errorMessage.addClassName("error-message");

        errorContent.add(errorTitle, errorMessage);
        errorContent.bindVisible(() -> stateSignal.get() == LoadingState.ERROR);

        stateBox.add(idleContent, loadingContent, successContent, errorContent);

        add(title, description, controls, stateBox);
    }

    @Override
    protected void onAttach(AttachEvent attachEvent) {
        super.onAttach(attachEvent);
        // Could auto-load data on attach if desired
    }

    private void loadReport() {
        stateSignal.set(LoadingState.LOADING);
        reportDataSignal.set(AnalyticsReport.empty());

        // Capture on UI thread — .peek() reads without creating a subscription
        boolean shouldFail = shouldFailSignal.peek();

        analyticsService.fetchReportData(shouldFail).thenCompose(rawData -> {
            stateSignal.set(LoadingState.GENERATING);
            return analyticsService.generateReportFromData(rawData, shouldFail);
        }).thenAccept(report -> {
            reportDataSignal.set(report);
            stateSignal.set(LoadingState.SUCCESS);
        }).exceptionally(error -> {
            stateSignal.set(LoadingState.ERROR);
            return null;
        });
    }

    private Card createMetricCardWithSignal(String label,
            Signal<String> valueSignal, VaadinIcon iconType, String color) {
        // Create card using Vaadin Card component with proper slots
        Card card = new Card();

        // Use headerPrefix slot for icon
        Icon icon = new Icon(iconType);
        icon.setColor(color);
        icon.setSize("24px");
        card.setHeaderPrefix(icon);

        // Use title slot for label
        Span labelSpan = new Span(label);
        labelSpan.addClassName("metric-label");
        card.setTitle(labelSpan);

        // Main content slot for value display
        Span valueSpan = new Span(valueSignal);
        valueSpan.addClassName("metric-value");
        // Per-metric color is dynamic — keep inline
        valueSpan.getStyle().set("color", color);
        card.add(valueSpan);

        // Per-metric border color is dynamic — keep inline
        card.getStyle().set("border-left", "4px solid " + color);

        return card;
    }
}
