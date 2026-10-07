package com.example.usecase04;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = UseCase04View.class)
@WithMockUser
class UseCase04ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithFilterAndGrid() {
        navigate(UseCase04View.class);

        assertEquals(1, findInView(ComboBox.class).all().size());
        assertEquals(1, findInView(TextField.class).all().size());
        assertEquals(1, findInView(Checkbox.class).all().size());
        assertEquals(1, findInView(Grid.class).all().size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void gridShowsAllProductsInitially() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        Grid<Product> grid = findInView(Grid.class).single();
        assertEquals(10, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterByCategoryElectronics() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        ComboBox<String> categoryFilter = findInView(ComboBox.class).single();
        test(categoryFilter).selectItem("Electronics");
        runPendingSignalsTasks();

        Grid<Product> grid = findInView(Grid.class).single();
        assertEquals(3, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterBySearchTerm() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        TextField searchField = findInView(TextField.class).single();
        test(searchField).setValue("laptop");
        runPendingSignalsTasks();

        Grid<Product> grid = findInView(Grid.class).single();
        assertEquals(1, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterByInStockOnly() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        Checkbox inStockCheckbox = findInView(Checkbox.class).single();
        test(inStockCheckbox).click();
        runPendingSignalsTasks();

        Grid<Product> grid = findInView(Grid.class).single();
        // P003 (stock=0) and P005 (stock=0) excluded -> 8 items
        assertEquals(8, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void combinedFilters() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        ComboBox<String> categoryFilter = findInView(ComboBox.class).single();
        test(categoryFilter).selectItem("Electronics");

        Checkbox inStockCheckbox = findInView(Checkbox.class).single();
        test(inStockCheckbox).click();
        runPendingSignalsTasks();

        Grid<Product> grid = findInView(Grid.class).single();
        // Electronics: Laptop(15), Mouse(0), Keyboard(8) -> in-stock: 2
        assertEquals(2, test(grid).size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void clearingSearchRestoresAll() {
        navigate(UseCase04View.class);
        runPendingSignalsTasks();

        TextField searchField = findInView(TextField.class).single();
        test(searchField).setValue("laptop");
        runPendingSignalsTasks();
        assertEquals(1,
                test((Grid<Product>) findInView(Grid.class).single()).size());

        test(searchField).setValue("");
        runPendingSignalsTasks();
        assertEquals(10,
                test((Grid<Product>) findInView(Grid.class).single()).size());
    }
}
