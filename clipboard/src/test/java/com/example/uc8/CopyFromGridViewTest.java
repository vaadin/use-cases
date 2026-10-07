package com.example.uc8;

import com.example.uc8.CopyFromGridView.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Span;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = CopyFromGridView.class)
class CopyFromGridViewTest extends SpringBrowserlessTest {

    @Test
    void viewRendersHeadingAndGrid() {
        CopyFromGridView view = navigate(CopyFromGridView.class);

        assertTrue(findInView(H1.class).all().stream()
                .anyMatch(h -> "UC8 — A copy button in every grid row"
                        .equals(h.getText())));
        assertEquals(CopyFromGridView.ROW_COUNT, test(view.grid).size());
        assertEquals("Email", test(view.grid).getHeaderCell(2));
    }

    @Test
    void everyRowRendersItsOwnValueAndCopyButton() {
        CopyFromGridView view = navigate(CopyFromGridView.class);

        // Two rows far apart: each cell must carry its own row's value, since
        // the copy button binds that value as a literal at render time.
        assertCopyCell(view, 0);
        assertCopyCell(view, 42);
    }

    private void assertCopyCell(CopyFromGridView view, int row) {
        Customer customer = test(view.grid).getRow(row);
        Component cell = test(view.grid).getCellComponent(row, 2);

        assertTrue(
                cell.getChildren()
                        .anyMatch(child -> child instanceof Span span
                                && customer.email().equals(span.getText())),
                "row " + row + " should show its email");
        assertTrue(
                cell.getChildren()
                        .anyMatch(child -> child instanceof Button button
                                && ("Copy " + customer.email()).equals(
                                        button.getAriaLabel().orElse(null))),
                "row " + row + " should have a copy button for its own email");
    }
}
