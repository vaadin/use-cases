package com.example.uc4;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.orders.Order;
import com.example.orders.OrderHistory;
import com.example.orders.OrderStore;
import com.example.orders.Product;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridSortOrder;
import com.vaadin.flow.data.provider.SortDirection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = OrdersView.class)
class OrdersViewTest extends SpringBrowserlessTest {

    @Autowired
    private OrderStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void loadsAllOrdersLazily() {
        OrdersView view = navigate(OrdersView.class);
        Grid<Order> grid = view.grid();

        assertEquals(OrderHistory.PAST_ORDERS, test(grid).size());
        assertEquals("1", test(grid).getCellText(0, 0));
        assertEquals("Order", test(grid).getHeaderCell(0));
    }

    @Test
    void sortingReachesTheBackend() {
        OrdersView view = navigate(OrdersView.class);
        Grid<Order> grid = view.grid();

        // Gap: the tester cannot click a column header to sort.
        grid.sort(GridSortOrder.desc(grid.getColumnByKey("number")).build());

        assertEquals(String.valueOf(OrderHistory.PAST_ORDERS),
                test(grid).getCellText(0, 0));
        assertEquals(SortDirection.DESCENDING,
                grid.getSortOrder().getFirst().getDirection());
    }

    @Test
    void filterNarrowsTheRows() {
        OrdersView view = navigate(OrdersView.class);

        test(view.filter()).setValue("kestrel");

        assertEquals(OrderHistory.PAST_ORDERS / 8, test(view.grid()).size());
        assertEquals("Kestrel Air", test(view.grid()).getCellText(0, 1));
    }

    @Test
    void selectingARowShowsItsDetails() {
        OrdersView view = navigate(OrdersView.class);

        test(view.grid()).select(2);

        assertEquals("Order #3: 4 × Espresso beans, 1 kg for Northwind",
                view.details());
    }

    @Test
    void placedOrdersComeFirstAndCanBeCancelledFromTheirRow() {
        Order placed = store.place("Blue Finch", Product.GRINDER, 1,
                LocalDate.of(2026, 3, 5), new BigDecimal("189.00"));
        OrdersView view = navigate(OrdersView.class);
        Grid<Order> grid = view.grid();
        assertEquals(String.valueOf(placed.number()),
                test(grid).getCellText(0, 0));

        Button cancel = (Button) test(grid).getCellComponent(0, "actions");
        test(cancel).click();

        assertTrue(store.all().isEmpty());
        assertEquals("1", test(grid).getCellText(0, 0));
    }
}
