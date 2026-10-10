package com.example.uc11;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.home.HomeView;
import com.example.orders.OrderStore;
import com.example.orders.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.ComponentUtil;
import com.vaadin.flow.component.PollEvent;
import com.vaadin.flow.component.UI;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = { LoadTestView.class, HomeView.class })
class LoadTestViewTest extends SpringBrowserlessTest {

    @Autowired
    private OrderStore store;

    @BeforeEach
    @AfterEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void pollRefreshesTheStats() {
        LoadTestView view = navigate(LoadTestView.class);
        assertTrue(view.stats().endsWith(" 0 orders placed"), view.stats());

        store.place("Blue Finch", Product.GRINDER, 1, LocalDate.of(2026, 3, 5),
                new BigDecimal("189.00"));
        assertTrue(view.stats().endsWith(" 0 orders placed"),
                "nothing changes until the next poll");

        UI ui = UI.getCurrent();
        ComponentUtil.fireEvent(ui, new PollEvent(ui, true));

        assertTrue(view.stats().endsWith(" 1 orders placed"), view.stats());
    }

    @Test
    void pollsOnlyWhileOpen() {
        navigate(LoadTestView.class);
        assertEquals(1000, UI.getCurrent().getPollInterval());

        navigate(HomeView.class);

        assertEquals(-1, UI.getCurrent().getPollInterval());
    }
}
