package com.example.usecase30;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

/**
 * Product catalog filter. 1 000 products are filtered by category, max price
 * and name search. Three places consume the filtered list — the grid, the "X
 * results" badge, and an "Export N products" button — so without caching, each
 * keystroke would run the filter three times.
 * <p>
 * Wrapping the computed filter signal in {@link Signal#cached(Signal)} makes
 * the filter run once per input change and serves the same list to all three
 * consumers. The "Filter computations" counter on screen proves it.
 */
@PageTitle("Use Case 30: Catalog filter")
@Route(value = "use-case-30", layout = MainLayout.class)
@Menu(order = 30, title = "UC 30: Catalog filter")
@AnonymousAllowed
public class UseCase30View extends VerticalLayout {

    private static final int PRODUCT_COUNT = 1000;
    private static final String ANY_CATEGORY = "All categories";

    private final List<Product> allProducts = ProductCatalog
            .generate(PRODUCT_COUNT);

    final ValueSignal<String> category = new ValueSignal<>(ANY_CATEGORY);
    final ValueSignal<Double> maxPrice = new ValueSignal<>(5000.0);
    final ValueSignal<String> search = new ValueSignal<>("");
    final AtomicInteger filterComputations = new AtomicInteger();
    private final ValueSignal<Integer> filterComputationsSignal = new ValueSignal<>(
            0);

    final Signal<List<Product>> visibleProducts = Signal
            .cached(Signal.computed(() -> {
                String cat = category.get();
                double max = maxPrice.get();
                String q = search.get().trim().toLowerCase();
                List<Product> result = allProducts.stream()
                        .filter(p -> ANY_CATEGORY.equals(cat)
                                || cat.equals(p.category()))
                        .filter(p -> p.price() <= max).filter(p -> q.isEmpty()
                                || p.name().toLowerCase().contains(q))
                        .toList();
                filterComputationsSignal
                        .set(filterComputations.incrementAndGet());
                return result;
            }));

    public UseCase30View() {
        setSpacing(true);
        setPadding(true);

        add(new H2("Use Case 30: Catalog filter"), new Paragraph(
                "1 000 products are filtered by category, max price and"
                        + " name search. The filter result is consumed by the"
                        + " grid, the result-count badge and the export"
                        + " button — three subscribers reading the same"
                        + " computed value. Signal.cached makes the filter"
                        + " run once per input change instead of once per"
                        + " subscriber."));

        add(buildFilterControls(), buildStatsRow(), buildGrid(),
                buildExplanation());
    }

    private HorizontalLayout buildFilterControls() {
        Select<String> categorySelect = new Select<>();
        categorySelect.setLabel("Category");
        List<String> options = new java.util.ArrayList<>();
        options.add(ANY_CATEGORY);
        options.addAll(ProductCatalog.categories());
        categorySelect.setItems(options);
        categorySelect.bindValue(category, category::set);

        NumberField maxPriceField = new NumberField("Max price");
        maxPriceField.bindValue(maxPrice, maxPrice::set);

        TextField searchField = new TextField("Search name");
        searchField.bindValue(search, search::set);

        HorizontalLayout row = new HorizontalLayout(categorySelect,
                maxPriceField, searchField);
        row.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.BASELINE);
        return row;
    }

    private HorizontalLayout buildStatsRow() {
        // Consumer #1: result-count badge
        Span badge = new Span();
        badge.bindText(visibleProducts.map(list -> list.size() + " results"));
        badge.getStyle()
                .set("padding", "var(--lumo-space-xs) var(--lumo-space-m)")
                .set("background-color", "var(--lumo-primary-color-10pct)")
                .set("color", "var(--lumo-primary-text-color)")
                .set("border-radius", "999px").set("font-weight", "bold");

        // Consumer #2: export button
        Button export = new Button();
        export.setText("Export 0 to CSV");
        Signal.effect(export, () -> {
            int size = visibleProducts.get().size();
            export.setText("Export " + size + " to CSV");
        });
        export.addClickListener(e -> Notification.show(
                "Exporting " + visibleProducts.peek().size() + " products"));

        // Computation counter — proves the cache works
        Span runs = new Span();
        runs.bindText(
                filterComputationsSignal.map(n -> "Filter computations: " + n));
        runs.getStyle().set("color", "var(--lumo-secondary-text-color)")
                .set("font-size", "var(--lumo-font-size-s)");

        HorizontalLayout row = new HorizontalLayout(badge, export, runs);
        row.setAlignItems(
                com.vaadin.flow.component.orderedlayout.FlexComponent.Alignment.CENTER);
        return row;
    }

    private Grid<Product> buildGrid() {
        // Consumer #3: the grid
        Grid<Product> grid = new Grid<>(Product.class, false);
        grid.addColumn(Product::id).setHeader("ID").setWidth("70px")
                .setFlexGrow(0);
        grid.addColumn(Product::name).setHeader("Name");
        grid.addColumn(Product::category).setHeader("Category");
        grid.addColumn(p -> String.format("%.2f", p.price())).setHeader("Price")
                .setWidth("100px").setFlexGrow(0);
        grid.setHeight("400px");

        Signal.effect(grid, () -> grid.setItems(visibleProducts.get()));
        return grid;
    }

    private Div buildExplanation() {
        Div box = new Div();
        box.getStyle().set("background-color", "var(--lumo-contrast-5pct)")
                .set("padding", "var(--lumo-space-m)")
                .set("border-radius", "8px");

        H3 title = new H3("Why Signal.cached?");
        title.getStyle().set("margin-top", "0");

        Paragraph p = new Paragraph(
                "The filter is read by three subscribers (grid, badge, export"
                        + " label). Without Signal.cached the lambda would"
                        + " re-run once per subscriber per dependency change"
                        + " — three full passes over 1 000 products on every"
                        + " keystroke. With caching, the computation counter"
                        + " advances exactly once per real input change.");

        box.add(title, p);
        return box;
    }
}
