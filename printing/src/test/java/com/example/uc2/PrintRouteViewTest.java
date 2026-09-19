package com.example.uc2;

import com.example.PrintTestSupport;
import com.example.data.Order;
import com.example.data.Orders;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = PrintRouteView.class)
class PrintRouteViewTest extends SpringBrowserlessTest {

    private static final int PRINT_COLUMN = 3;

    @Test
    void everyOrderHasAPrintButton() {
        navigate(PrintRouteView.class);

        Grid<?> grid = findInView(Grid.class).single();
        assertEquals(24, test(grid).size());
        assertEquals("ORD-1001", test(grid).getCellText(0, 0));
        assertEquals("print-ORD-1001",
                printButton(grid, 0).getId().orElseThrow());
    }

    @Test
    void printButtonOpensTheSlipInItsOwnWindow() {
        navigate(PrintRouteView.class);

        test(printButton(findInView(Grid.class).single(), 1)).click();

        assertTrue(PrintTestSupport.pendingJsContains("window.open"),
                "The slip should open in a separate window");
    }

    @Test
    void slipUrlPointsAtThePrintOnlyRoute() {
        navigate(PrintRouteView.class);

        Order order = Orders.sample(2).getLast();
        assertEquals("uc2/slip/" + order.id(), PrintRouteView.slipUrl(order));
    }

    private Button printButton(Grid<?> grid, int row) {
        return (Button) test(grid).getCellComponent(row, PRINT_COLUMN);
    }
}
