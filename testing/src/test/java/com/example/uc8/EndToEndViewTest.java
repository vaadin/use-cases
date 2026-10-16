package com.example.uc8;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.orders.OrderStore;
import com.example.orders.Product;
import com.example.uc2.NewOrderView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.button.Button;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The view itself; the end-to-end tests are CheckoutIT and
 * CheckoutPlaywrightIT.
 */
@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = { EndToEndView.class, NewOrderView.class })
class EndToEndViewTest extends SpringBrowserlessTest {

    @Autowired
    private OrderStore store;

    @BeforeEach
    void oneOrder() {
        store.clear();
        store.place("Blue Finch", Product.GRINDER, 1, LocalDate.of(2026, 3, 5),
                new BigDecimal("189.00"));
    }

    @Test
    void resetRemovesPlacedOrders() {
        EndToEndView view = navigate(EndToEndView.class);
        assertEquals("1 orders placed so far", view.count());

        test(findInView(Button.class).single()).click();

        assertTrue(store.all().isEmpty());
        assertEquals("0 orders placed so far", view.count());
    }
}
