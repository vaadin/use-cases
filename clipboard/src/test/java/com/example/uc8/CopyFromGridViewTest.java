package com.example.uc8;

import com.example.uc8.CopyFromGridView.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H1;

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
    void everyRowRendersItsOwnValue() {
        CopyFromGridView view = navigate(CopyFromGridView.class);

        // Two rows far apart: the template copies the row's own email
        // property, so each row must carry its own value.
        assertEmail(view, 0);
        assertEmail(view, 42);
    }

    private void assertEmail(CopyFromGridView view, int row) {
        Customer customer = test(view.grid).getRow(row);

        assertEquals(customer.email(),
                test(view.grid).getLitRendererPropertyValue(row, 2, "email",
                        String.class),
                "row " + row + " should render its own email");
    }
}
