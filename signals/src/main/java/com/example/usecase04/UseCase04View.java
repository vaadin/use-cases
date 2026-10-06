package com.example.usecase04;

import java.math.BigDecimal;
import java.util.List;

import com.example.views.MainLayout;

import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;
import com.vaadin.flow.signals.Signal;
import com.vaadin.flow.signals.local.ValueSignal;

@Route(value = "use-case-04", layout = MainLayout.class)
@PageTitle("Use Case 4: Filtered and Sorted Data Grid")
@Menu(order = 4, title = "UC 4: Filtered Data Grid")
@AnonymousAllowed
public class UseCase04View extends VerticalLayout {

    public UseCase04View() {
        setSpacing(true);
        setPadding(true);

        H2 title = new H2("Use Case 4: Filtered and Sorted Data Grid");

        Paragraph description = new Paragraph(
                "This use case demonstrates a Grid with DataView-based filtering. "
                        + "Filter by category, search by name/ID, and toggle in-stock only. "
                        + "Signals are used to bind filter controls, while DataView handles the actual filtering. "
                        + "This shows proper integration of signals with Vaadin's DataView API.");

        // Create signals for filter inputs
        ValueSignal<String> categoryFilterSignal = new ValueSignal<>("All");
        ValueSignal<String> searchTermSignal = new ValueSignal<>("");
        ValueSignal<Boolean> inStockOnlySignal = new ValueSignal<>(false);

        // Load all products and set up Grid with DataView
        List<Product> allProducts = loadProducts();

        // Data grid with ListDataView for filtering
        Grid<Product> grid = new Grid<>(Product.class);
        grid.setColumns("id", "name", "category", "price", "stock");

        // Use setItems to get a GridListDataView for filtering
        GridListDataView<Product> dataView = grid.setItems(allProducts);

        // Filter UI components with signal bindings
        ComboBox<String> categoryFilter = new ComboBox<>("Category", List.of(
                "All", "Electronics", "Clothing", "Books", "Home & Garden"));
        categoryFilter.bindValue(categoryFilterSignal,
                categoryFilterSignal::set);

        TextField searchField = new TextField("Search");
        searchField.setPlaceholder("Search by name or ID");
        searchField.bindValue(searchTermSignal, searchTermSignal::set);

        Checkbox inStockCheckbox = new Checkbox("Show in-stock items only");
        inStockCheckbox.bindValue(inStockOnlySignal, inStockOnlySignal::set);

        // Use Signal.effect to configure DataView when any filter signal
        // changes
        Signal.effect(grid, () -> {
            // Read values outside the filter callback to register dependencies
            String category = categoryFilterSignal.get();
            String searchTerm = searchTermSignal.get().toLowerCase();
            boolean inStockOnly = inStockOnlySignal.get();

            dataView.setFilter(product -> {
                boolean categoryMatch = category.equals("All")
                        || product.category().equals(category);
                boolean searchMatch = searchTerm.isEmpty()
                        || product.name().toLowerCase().contains(searchTerm)
                        || product.id().toLowerCase().contains(searchTerm);
                boolean stockMatch = !inStockOnly || product.stock() > 0;

                return categoryMatch && searchMatch && stockMatch;
            });
        });

        add(title, description, categoryFilter, searchField, inStockCheckbox,
                grid);
    }

    private List<Product> loadProducts() {
        // Stub implementation - returns mock data
        return List.of(
                new Product("P001", "Laptop", "Electronics",
                        new BigDecimal("999.99"), 15),
                new Product("P002", "T-Shirt", "Clothing",
                        new BigDecimal("19.99"), 50),
                new Product("P003", "Java Programming Book", "Books",
                        new BigDecimal("49.99"), 0),
                new Product("P004", "Garden Hose", "Home & Garden",
                        new BigDecimal("29.99"), 30),
                new Product("P005", "Wireless Mouse", "Electronics",
                        new BigDecimal("25.99"), 0),
                new Product("P006", "Jeans", "Clothing",
                        new BigDecimal("59.99"), 20),
                new Product("P007", "Fiction Novel", "Books",
                        new BigDecimal("14.99"), 100),
                new Product("P008", "Plant Pot", "Home & Garden",
                        new BigDecimal("12.99"), 45),
                new Product("P009", "Keyboard", "Electronics",
                        new BigDecimal("79.99"), 8),
                new Product("P010", "Winter Jacket", "Clothing",
                        new BigDecimal("129.99"), 5));
    }
}
