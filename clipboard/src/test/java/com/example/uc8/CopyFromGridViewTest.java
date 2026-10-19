package com.example.uc8;

import com.example.uc8.CopyFromGridView.Customer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
@ViewPackages(classes = CopyFromGridView.class)
class CopyFromGridViewTest extends SpringBrowserlessTest {

    @Test
    void everyRowRendersItsOwnValue() {
        CopyFromGridView view = navigate(CopyFromGridView.class);

        assertEquals(CopyFromGridView.ROW_COUNT, test(view.grid).size());
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
