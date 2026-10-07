package com.example.usecase15;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.example.views.MainLayout;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.jspecify.annotations.Nullable;

import com.vaadin.flow.component.DetachEvent;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ListSignal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Use Case 15: Debounced Search
 *
 * Demonstrates search-as-you-type with debouncing: - TextField with immediate
 * updates to instant signal - Manually debounced signal (1000ms delay) - Search
 * only fires after user stops typing - Cancel in-flight requests on new input -
 * Loading indicator during search - Highlight matching text in results
 *
 * Key Patterns: - Manual debouncing with ScheduledExecutorService - Async
 * search with cancellation - Loading states for search - Real-time search
 * results - Keystroke vs search count comparison
 */
@Route(value = "use-case-15", layout = MainLayout.class)
@PageTitle("Use Case 15: Debounced Search")
@Menu(order = 15, title = "UC 15: Debounced Search")
@StyleSheet("usecase15.css")
@AnonymousAllowed
public class UseCase15View extends VerticalLayout {

    public record Product(String id, String name, String category,
            BigDecimal price) {
        public boolean matches(String query) {
            String lowerQuery = query.toLowerCase();
            return name.toLowerCase().contains(lowerQuery)
                    || category.toLowerCase().contains(lowerQuery);
        }
    }

    // Sample product database
    private static final List<Product> ALL_PRODUCTS = List.of(
            new Product("1", "Laptop Pro 15", "Electronics",
                    new BigDecimal("1299.99")),
            new Product("2", "Wireless Mouse", "Electronics",
                    new BigDecimal("29.99")),
            new Product("3", "Mechanical Keyboard", "Electronics",
                    new BigDecimal("89.99")),
            new Product("4", "Office Chair", "Furniture",
                    new BigDecimal("249.99")),
            new Product("5", "Standing Desk", "Furniture",
                    new BigDecimal("499.99")),
            new Product("6", "Coffee Mug", "Kitchen", new BigDecimal("12.99")),
            new Product("7", "Water Bottle", "Kitchen",
                    new BigDecimal("19.99")),
            new Product("8", "Notebook Set", "Stationery",
                    new BigDecimal("15.99")),
            new Product("9", "Pen Collection", "Stationery",
                    new BigDecimal("24.99")),
            new Product("10", "Desk Lamp", "Furniture",
                    new BigDecimal("39.99")),
            new Product("11", "USB-C Hub", "Electronics",
                    new BigDecimal("49.99")),
            new Product("12", "Headphones", "Electronics",
                    new BigDecimal("149.99")),
            new Product("13", "Monitor 27\"", "Electronics",
                    new BigDecimal("399.99")),
            new Product("14", "Webcam HD", "Electronics",
                    new BigDecimal("79.99")),
            new Product("15", "Bookshelf", "Furniture",
                    new BigDecimal("129.99")));

    private static final long DEBOUNCE_DELAY_MS = 1000;

    private final ValueSignal<String> instantQuerySignal = new ValueSignal<>(
            "");
    private final ValueSignal<String> searchQuerySignal = new ValueSignal<>("");
    private final ListSignal<Product> searchResultsSignal = new ListSignal<>();
    private final ValueSignal<Integer> searchCountSignal = new ValueSignal<>(0);
    private final ValueSignal<Integer> keystrokeCountSignal = new ValueSignal<>(
            0);
    private final ValueSignal<@Nullable CompletableFuture<Void>> currentSearchSignal = new ValueSignal<@Nullable CompletableFuture<Void>>(
            null);

    private final ScheduledExecutorService debounceExecutor = Executors
            .newSingleThreadScheduledExecutor();
    private volatile @Nullable ScheduledFuture<?> pendingDebounce = null;

    public UseCase15View() {
        addClassName("usecase15-view");
        setSpacing(true);
        setPadding(true);

        var isSearchingSignal = currentSearchSignal
                .map(future -> future != null);

        H2 title = new H2("Use Case 15: Debounced Search");

        Paragraph description = new Paragraph(
                "This use case demonstrates search-as-you-type with debouncing. "
                        + "As you type, the 'Instant value' updates on every keystroke, but the actual search is delayed by 1000ms. "
                        + "This prevents excessive server calls while typing and only searches after you pause. "
                        + "Compare the keystroke count vs search count to see debounce efficiency.");

        // Search field — EAGER mode sends every keystroke
        TextField searchField = new TextField("Search Products");
        searchField.setPlaceholder("Type to search...");
        searchField.setWidth("400px");
        searchField.setPrefixComponent(new Icon(VaadinIcon.SEARCH));
        searchField.setClearButtonVisible(true);
        searchField.setValueChangeMode(ValueChangeMode.EAGER);

        searchField.bindValue(instantQuerySignal, value -> {
            instantQuerySignal.set(value);
            keystrokeCountSignal.set(keystrokeCountSignal.peek() + 1);
            scheduleDebouncedSearch(value);
        });

        // Search stats
        Div statsBox = new Div();
        statsBox.addClassName("stats-box");

        Div instantQueryDiv = new Div();
        Span instantLabel = new Span("Instant value: ");
        instantLabel.addClassName("stat-label");
        Span instantValue = new Span(instantQuerySignal
                .map(q -> q.isEmpty() ? "(empty)" : "\"" + q + "\""));
        instantValue.addClassName("stat-value");
        instantQueryDiv.add(instantLabel, instantValue);

        Div debouncedQueryDiv = new Div();
        Span debouncedLabel = new Span("Debounced value (1000ms): ");
        debouncedLabel.addClassName("stat-label");
        Span debouncedValue = new Span(searchQuerySignal
                .map(q -> q.isEmpty() ? "(empty)" : "\"" + q + "\""));
        debouncedValue.addClassName("stat-value-accent");
        debouncedQueryDiv.add(debouncedLabel, debouncedValue);

        Div keystrokeCountDiv = new Div();
        Span keystrokeLabel = new Span("Keystrokes: ");
        keystrokeLabel.addClassName("stat-label");
        Span keystrokeValue = new Span(
                keystrokeCountSignal.map(String::valueOf));
        keystrokeValue.addClassName("stat-value-bold");
        keystrokeCountDiv.add(keystrokeLabel, keystrokeValue);

        Div searchCountDiv = new Div();
        Span countLabel = new Span("Searches performed: ");
        countLabel.addClassName("stat-label");
        Span countValue = new Span(searchCountSignal.map(String::valueOf));
        countValue.addClassName("stat-value-success");
        searchCountDiv.add(countLabel, countValue);

        statsBox.add(instantQueryDiv, debouncedQueryDiv, keystrokeCountDiv,
                searchCountDiv);

        // Search status
        Div statusBox = new Div();
        statusBox.addClassName("status-box");

        Icon searchingIcon = new Icon(VaadinIcon.SPINNER);
        searchingIcon.addClassName("searching-icon");
        searchingIcon.bindVisible(isSearchingSignal);

        Span statusText = new Span(isSearchingSignal
                .map(searching -> searching ? "Searching..." : ""));
        statusText.addClassName("status-text");

        statusBox.add(searchingIcon, statusText);

        // Results
        Signal<String> resultsTitleSignal = Signal.computed(() -> {
            var results = searchResultsSignal.get();
            if (results.isEmpty()) {
                if (searchQuerySignal.get().isEmpty()) {
                    return "Type to search";
                } else if (isSearchingSignal.get()) {
                    return "";
                } else {
                    return "No results found";
                }
            } else {
                return results.size() + " result"
                        + (results.size() == 1 ? "" : "s");
            }
        });
        H3 resultsTitle = new H3(resultsTitleSignal);

        Div resultsContainer = new Div();
        resultsContainer.addClassName("results-container");

        // Peek rather than binding since products are immutable
        resultsContainer.bindChildren(searchResultsSignal,
                productSignal -> createProductCard(productSignal.peek()));

        // Info box
        Div infoBox = new Div();
        infoBox.addClassName("info-box");
        infoBox.add(new Paragraph(
                "Debouncing is critical for performance in production applications. "
                        + "Without debouncing, typing 'laptop' would trigger 6 searches (one per character). "
                        + "With 1000ms debouncing, it triggers just 1 search after you stop typing. "
                        + "Compare the Keystrokes and Searches performed counters to see the savings."));

        add(title, description, searchField, statsBox, statusBox, resultsTitle,
                resultsContainer, infoBox);
    }

    @Override
    protected void onDetach(DetachEvent detachEvent) {
        super.onDetach(detachEvent);
        // Cancel pending debounce
        ScheduledFuture<?> pending = pendingDebounce;
        if (pending != null) {
            pending.cancel(false);
        }
        debounceExecutor.shutdownNow();
        // Cancel any in-flight search
        CompletableFuture<Void> search = currentSearchSignal.peek();
        if (search != null) {
            search.cancel(true);
        }
    }

    private void scheduleDebouncedSearch(String query) {
        // Cancel any pending debounce timer
        ScheduledFuture<?> pending = pendingDebounce;
        if (pending != null) {
            pending.cancel(false);
        }

        // Schedule a new debounced search
        pendingDebounce = debounceExecutor.schedule(() -> {
            searchQuerySignal.set(query);
            performSearch(query);
        }, DEBOUNCE_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    private void performSearch(String query) {
        // Cancel previous search if still running
        CompletableFuture<Void> previousSearch = currentSearchSignal.peek();
        if (previousSearch != null && !previousSearch.isDone()) {
            previousSearch.cancel(true);
        }

        if (query.isEmpty()) {
            searchResultsSignal.clear();
            return;
        }

        // Set searching state
        searchCountSignal.set(searchCountSignal.peek() + 1);

        // Simulate async search with delay
        CompletableFuture<Void> searchFuture = CompletableFuture
                .runAsync(() -> {
                    try {
                        Thread.sleep(500); // Simulate network delay
                    } catch (InterruptedException e) {
                        // Search cancelled
                        return;
                    }

                    // Filter products
                    List<Product> results = ALL_PRODUCTS.stream()
                            .filter(p -> p.matches(query)).toList();

                    searchResultsSignal.clear();
                    results.forEach(searchResultsSignal::insertLast);
                    currentSearchSignal.set(null);
                });

        currentSearchSignal.set(searchFuture);
    }

    private Div createProductCard(Product product) {
        Div card = new Div();
        card.addClassName("product-card");

        Div leftSide = new Div();
        Div nameDiv = new Div();
        nameDiv.addClassName("product-name");

        // Highlight matching text
        String query = searchQuerySignal.peek();
        nameDiv.getElement().setProperty("innerHTML",
                highlightMatch(product.name(), query));

        Div categoryDiv = new Div(product.category());
        categoryDiv.addClassName("product-category");

        leftSide.add(nameDiv, categoryDiv);

        Div priceDiv = new Div(
                "$" + product.price().setScale(2, RoundingMode.HALF_UP));
        priceDiv.addClassName("product-price");

        card.add(leftSide, priceDiv);
        return card;
    }

    private String highlightMatch(String text, String query) {
        if (query.isEmpty()) {
            return escapeHtml(text);
        }

        String lowerText = text.toLowerCase();
        String lowerQuery = query.toLowerCase();
        int index = lowerText.indexOf(lowerQuery);

        if (index == -1) {
            return escapeHtml(text);
        }

        String before = text.substring(0, index);
        String match = text.substring(index, index + query.length());
        String after = text.substring(index + query.length());

        return escapeHtml(before)
                + "<mark style='background-color: #ffeb3b; padding: 2px 4px; border-radius: 2px;'>"
                + escapeHtml(match) + "</mark>" + escapeHtml(after);
    }

    private String escapeHtml(String text) {
        return Jsoup.clean(text, Safelist.none());
    }
}
