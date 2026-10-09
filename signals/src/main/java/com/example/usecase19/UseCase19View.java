package com.example.usecase19;

import com.example.common.UseCaseDescription;
import com.example.service.DataLoadingService;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.card.Card;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
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
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 19: Parallel Data Loading with Individual Spinners
 *
 * Demonstrates parallel async loading where each item has its own loading state
 * and spinner. Shows how to manage multiple independent async operations with
 * Signals.
 *
 * Key Patterns: - ListSignal<ValueSignal<DataItem>> for per-item state
 * management - Spring @Async for true parallel execution - Individual loading
 * spinners (ProgressBar indeterminate) - Per-item error handling with retry -
 * CSS Grid for responsive layout - Vaadin Card components with conditional
 * rendering
 */
@Route(value = "use-case-19", layout = MainLayout.class)
@PageTitle("Use Case 19: Parallel Loading")
@Menu(order = 19, title = "UC19 — Parallel Loading")
@UseCaseDescription("Loading several items in parallel, each with its own spinner")
@StyleSheet("usecase19.css")
@AnonymousAllowed
public class UseCase19View extends VerticalLayout {

    /**
     * Represents a data item with loading state
     */

    private final DataLoadingService dataLoadingService;
    private final ListSignal<DataItem> itemsSignal = new ListSignal<>();
    private final ValueSignal<Boolean> simulateErrorsSignal = new ValueSignal<>(
            false);

    public UseCase19View(DataLoadingService dataLoadingService) {
        this.dataLoadingService = dataLoadingService;
        addClassName("usecase19-view");
        setSpacing(true);
        setPadding(true);

        H2 title = new H2(
                "Use Case 19: Parallel Data Loading with Individual Spinners");

        Paragraph description = new Paragraph(
                "This use case demonstrates parallel async loading where each item displays its own loading spinner "
                        + "until completion. Click 'Load All Items' to trigger parallel loading of 6 data items with varied "
                        + "durations (1-4 seconds). Each item completes independently and displays success/error states. "
                        + "Enable 'Simulate Random Errors' to test error handling and individual retry buttons.");

        // Initialize data items
        initializeDataItems();

        // Controls
        HorizontalLayout controls = new HorizontalLayout();
        controls.setSpacing(true);

        Button loadButton = new Button("Load All Items",
                event -> loadAllItems());
        loadButton.addThemeVariants(ButtonVariant.PRIMARY);

        Checkbox errorCheckbox = new Checkbox("Simulate Random Errors");
        errorCheckbox.bindValue(simulateErrorsSignal,
                simulateErrorsSignal::set);

        controls.add(loadButton, errorCheckbox);

        // Items container with flexbox
        Div itemsContainer = new Div();
        itemsContainer.addClassName("items-container");

        // Bind cards to items signal
        itemsContainer.bindChildren(itemsSignal, this::createDataItemCard);

        add(title, description, controls, itemsContainer);
    }

    /**
     * Initialize the 6 data items with different delays
     */
    private void initializeDataItems() {
        itemsSignal.insertLast(new DataItem("1", "Dashboard Metrics",
                LoadingState.IDLE, null, null, 1500));
        itemsSignal.insertLast(new DataItem("2", "User Statistics",
                LoadingState.IDLE, null, null, 3000));
        itemsSignal.insertLast(new DataItem("3", "Sales Report",
                LoadingState.IDLE, null, null, 2000));
        itemsSignal.insertLast(new DataItem("4", "Inventory Status",
                LoadingState.IDLE, null, null, 2500));
        itemsSignal.insertLast(new DataItem("5", "Performance Data",
                LoadingState.IDLE, null, null, 1000));
        itemsSignal.insertLast(new DataItem("6", "Analytics Summary",
                LoadingState.IDLE, null, null, 3500));
    }

    /**
     * Load all items in parallel
     */
    private void loadAllItems() {
        itemsSignal.peek().forEach(itemSignal -> {
            DataItem item = itemSignal.peek();
            // Update to LOADING state
            itemSignal.set(new DataItem(item.id(), item.name(),
                    LoadingState.LOADING, null, null, item.simulatedDelayMs()));

            // Launch async operation (truly parallel via Spring @Async)
            dataLoadingService.loadDataAsync(item.id(), item.simulatedDelayMs(),
                    simulateErrorsSignal.peek()).thenAccept(data -> {
                        // Update to SUCCESS state
                        // Signals are thread-safe - update directly from
                        // background thread
                        itemSignal.set(new DataItem(item.id(), item.name(),
                                LoadingState.SUCCESS, data, null,
                                item.simulatedDelayMs()));
                    }).exceptionally(error -> {
                        // Update to ERROR state
                        // Signals are thread-safe - update directly from
                        // background thread
                        itemSignal.set(new DataItem(item.id(), item.name(),
                                LoadingState.ERROR, null,
                                extractErrorMessage(error),
                                item.simulatedDelayMs()));
                        return null;
                    });
        });
    }

    /**
     * Retry loading a single item
     */
    private void retryItem(ValueSignal<DataItem> itemSignal) {
        DataItem item = itemSignal.peek();

        // Update to LOADING state
        itemSignal.set(new DataItem(item.id(), item.name(),
                LoadingState.LOADING, null, null, item.simulatedDelayMs()));

        // Launch async operation
        dataLoadingService.loadDataAsync(item.id(), item.simulatedDelayMs(),
                simulateErrorsSignal.peek()).thenAccept(data -> {
                    // Update to SUCCESS state
                    itemSignal.set(new DataItem(item.id(), item.name(),
                            LoadingState.SUCCESS, data, null,
                            item.simulatedDelayMs()));
                }).exceptionally(error -> {
                    // Update to ERROR state
                    itemSignal.set(new DataItem(item.id(), item.name(),
                            LoadingState.ERROR, null,
                            extractErrorMessage(error),
                            item.simulatedDelayMs()));
                    return null;
                });
    }

    /**
     * Extract clean error message from exception cause
     */
    private String extractErrorMessage(Throwable error) {
        Throwable cause = error.getCause();
        if (cause != null && cause.getMessage() != null) {
            return cause.getMessage();
        }
        return error.getMessage() != null ? error.getMessage()
                : "An error occurred";
    }

    /**
     * Create a Card component for a data item with conditional rendering based
     * on state
     */
    private Card createDataItemCard(ValueSignal<DataItem> itemSignal) {
        Card card = new Card();
        card.addClassName("data-card");

        // Header with item name
        Signal<String> nameSignal = itemSignal.map(DataItem::name);
        Span titleSpan = new Span();
        titleSpan.bindText(nameSignal);
        card.setTitle(titleSpan);

        // IDLE state content
        Div idleContent = new Div();
        idleContent.addClassName("idle-content");

        Icon idleIcon = new Icon(VaadinIcon.CIRCLE);
        idleIcon.setColor(
                "color-mix(in srgb, var(--vaadin-text-color) 30%, transparent)");
        idleIcon.setSize("2em");

        Span idleText = new Span("Ready to load");
        idleText.addClassName("idle-text");

        idleContent.add(idleIcon, idleText);
        Signal<Boolean> isIdleSignal = itemSignal
                .map(item -> item.state() == LoadingState.IDLE);
        idleContent.bindVisible(isIdleSignal);

        // LOADING state content
        Div loadingContent = new Div();
        loadingContent.addClassName("loading-content");

        ProgressBar progressBar = new ProgressBar();
        progressBar.setIndeterminate(true);
        progressBar.setWidth("80%");

        Span loadingText = new Span("Loading...");
        loadingText.addClassName("loading-text");

        loadingContent.add(progressBar, loadingText);
        Signal<Boolean> isLoadingSignal = itemSignal
                .map(item -> item.state() == LoadingState.LOADING);
        loadingContent.bindVisible(isLoadingSignal);

        // SUCCESS state content
        Div successContent = new Div();
        successContent.addClassName("success-content");

        Div successHeader = new Div();
        successHeader.addClassName("state-header");

        Icon successIcon = new Icon(VaadinIcon.CHECK_CIRCLE);
        successIcon.setColor("var(--aura-green)");

        Span successLabel = new Span("Loaded successfully");
        successLabel.addClassName("success-label");

        successHeader.add(successIcon, successLabel);

        Div dataContent = new Div();
        dataContent.addClassName("data-content");

        Span dataText = new Span();
        dataText.bindText(
                itemSignal.map(item -> item.data() != null ? item.data() : ""));
        dataContent.add(dataText);

        successContent.add(successHeader, dataContent);
        Signal<Boolean> isSuccessSignal = itemSignal
                .map(item -> item.state() == LoadingState.SUCCESS);
        successContent.bindVisible(isSuccessSignal);

        // ERROR state content
        Div errorContent = new Div();
        errorContent.addClassName("error-content");

        Div errorHeader = new Div();
        errorHeader.addClassName("state-header");

        Icon errorIcon = new Icon(VaadinIcon.CLOSE_CIRCLE);
        errorIcon.setColor("var(--aura-red)");

        Span errorLabel = new Span("Failed to load");
        errorLabel.addClassName("error-label");

        errorHeader.add(errorIcon, errorLabel);

        Div errorMessage = new Div();
        errorMessage.addClassName("error-message");

        Span errorText = new Span();
        errorText.bindText(itemSignal.map(
                item -> item.error() != null ? item.error() : "Unknown error"));
        errorMessage.add(errorText);

        Button retryButton = new Button("Retry",
                event -> retryItem(itemSignal));
        retryButton.addThemeVariants(ButtonVariant.PRIMARY);

        errorContent.add(errorHeader, errorMessage, retryButton);
        Signal<Boolean> isErrorSignal = itemSignal
                .map(item -> item.state() == LoadingState.ERROR);
        errorContent.bindVisible(isErrorSignal);

        // Add all state contents to card
        card.add(idleContent, loadingContent, successContent, errorContent);

        // Card styling based on state - swap state-* class
        card.getClassNames().bind("state-idle",
                itemSignal.map(i -> i.state() == LoadingState.IDLE));
        card.getClassNames().bind("state-loading",
                itemSignal.map(i -> i.state() == LoadingState.LOADING
                        || i.state() == LoadingState.GENERATING));
        card.getClassNames().bind("state-success",
                itemSignal.map(i -> i.state() == LoadingState.SUCCESS));
        card.getClassNames().bind("state-error",
                itemSignal.map(i -> i.state() == LoadingState.ERROR));

        return card;
    }
}
