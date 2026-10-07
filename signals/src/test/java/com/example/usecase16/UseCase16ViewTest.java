package com.example.usecase16;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.select.Select;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase16View.class)
@WithMockUser
class UseCase16ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSearchAndCategory() {
        navigate(UseCase16View.class);

        assertEquals(1, findInView(TextField.class).all().size());
        assertEquals(1, findInView(Select.class).all().size());
    }

    @Test
    void initiallyShowsAllArticles() {
        navigate(UseCase16View.class);
        runPendingSignalsTasks();

        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("10 articles")));
    }

    @Test
    void filterBySearchTerm() {
        navigate(UseCase16View.class);
        runPendingSignalsTasks();

        TextField searchField = findInView(TextField.class).single();
        test(searchField).setValue("signal");
        runPendingSignalsTasks();

        // Multiple articles mention "signal" in title or content
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("articles found")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void filterByCategory() {
        navigate(UseCase16View.class);
        runPendingSignalsTasks();

        Select<String> categorySelect = findInView(Select.class).single();
        test(categorySelect).selectItem("Tutorial");
        runPendingSignalsTasks();

        // 4 Tutorial articles
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("4 articles")));
    }

    @SuppressWarnings("unchecked")
    @Test
    void combinedFilters() {
        navigate(UseCase16View.class);
        runPendingSignalsTasks();

        TextField searchField = findInView(TextField.class).single();
        test(searchField).setValue("signal");

        Select<String> categorySelect = findInView(Select.class).single();
        test(categorySelect).selectItem("Tutorial");
        runPendingSignalsTasks();

        // Articles matching "signal" in Tutorial category
        assertTrue(findInView(H3.class).all().stream()
                .anyMatch(h -> h.getText() != null
                        && h.getText().contains("articles found")));
    }
}
