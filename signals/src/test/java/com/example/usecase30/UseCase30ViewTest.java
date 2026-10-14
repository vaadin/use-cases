package com.example.usecase30;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.NumberField;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase30View.class)
@WithMockUser
class UseCase30ViewTest extends SpringBrowserlessTest {

    @Test
    void initialFilterShowsAllProducts() {
        navigate(UseCase30View.class);
        runPendingSignalsTasks();

        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "1000 results".equals(s.getText())),
                "All 1 000 products should match the default filter");
    }

    @Test
    void filterComputationsRunOncePerInputChangeNotPerSubscriber() {
        navigate(UseCase30View.class);
        runPendingSignalsTasks();

        UseCase30View view = (UseCase30View) getCurrentView();
        int initial = view.filterComputations.get();

        // Three subscribers: grid, badge, export. With caching the filter
        // must run only ONCE per filter-input mutation.
        view.maxPrice.set(500.0);
        runPendingSignalsTasks();
        view.search.set("pro");
        runPendingSignalsTasks();

        int extraRuns = view.filterComputations.get() - initial;
        assertTrue(extraRuns <= 2,
                "Filter should run at most twice for two input mutations,"
                        + " was: " + extraRuns);
    }

    @Test
    @SuppressWarnings("unchecked")
    void filterByCategoryReducesResultCount() {
        navigate(UseCase30View.class);
        runPendingSignalsTasks();

        Select<String> categorySelect = (Select<String>) $view(Select.class)
                .single();
        test(categorySelect).selectItem("Audio");
        runPendingSignalsTasks();

        UseCase30View view = (UseCase30View) getCurrentView();
        assertEquals(200, view.visibleProducts.peek().size(),
                "200 of 1 000 products are in the Audio category");
        assertTrue(
                $view(Span.class).all().stream()
                        .anyMatch(s -> "200 results".equals(s.getText())),
                "Badge should reflect the new count");
    }

    @Test
    void priceFilterReducesResults() {
        navigate(UseCase30View.class);
        runPendingSignalsTasks();

        NumberField maxPriceField = $view(NumberField.class).single();
        test(maxPriceField).setValue(100.0);
        runPendingSignalsTasks();

        UseCase30View view = (UseCase30View) getCurrentView();
        assertTrue(view.visibleProducts.peek().size() < 1000,
                "Max price 100 should filter out the majority");
        assertTrue(
                view.visibleProducts.peek().stream()
                        .allMatch(p -> p.price() <= 100.0),
                "Every remaining product must be within the price cap");
    }

    @Test
    void searchFiltersByName() {
        navigate(UseCase30View.class);
        runPendingSignalsTasks();

        TextField searchField = $view(TextField.class).single();
        test(searchField).setValue("Audio");
        runPendingSignalsTasks();

        UseCase30View view = (UseCase30View) getCurrentView();
        assertTrue(
                view.visibleProducts.peek().stream().allMatch(
                        p -> p.name().toLowerCase().contains("audio")),
                "All results must contain the search term");
    }
}
