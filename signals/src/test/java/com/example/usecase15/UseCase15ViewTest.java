package com.example.usecase15;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.textfield.TextField;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = UseCase15View.class)
@WithMockUser
class UseCase15ViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersWithSearchField() {
        navigate(UseCase15View.class);

        assertTrue(findInView(TextField.class).all().stream()
                .anyMatch(f -> "Search Products".equals(f.getLabel())));
    }

    @Test
    void searchFieldPlaceholderPresent() {
        navigate(UseCase15View.class);

        TextField searchField = findInView(TextField.class).all().stream()
                .filter(f -> "Search Products".equals(f.getLabel())).findFirst()
                .orElseThrow();
        assertEquals("Type to search...", searchField.getPlaceholder());
    }

    @Test
    void instantQueryUpdatesOnTextInput() {
        navigate(UseCase15View.class);
        runPendingSignalsTasks();

        // Type into the search field
        TextField searchField = findInView(TextField.class).all().stream()
                .filter(f -> "Search Products".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(searchField).setValue("laptop");
        runPendingSignalsTasks();

        // The instant value Span should show the typed text
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> s.getText() != null
                        && s.getText().contains("\"laptop\"")));
    }

    @Test
    void keystrokeCounterIncrements() {
        navigate(UseCase15View.class);
        runPendingSignalsTasks();

        // Initially keystroke count should be 0
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "0".equals(s.getText())));

        // Type a value
        TextField searchField = findInView(TextField.class).all().stream()
                .filter(f -> "Search Products".equals(f.getLabel())).findFirst()
                .orElseThrow();
        test(searchField).setValue("a");
        runPendingSignalsTasks();

        // Keystroke count should now be 1
        assertTrue(findInView(Span.class).all().stream()
                .anyMatch(s -> "1".equals(s.getText())));
    }
}
