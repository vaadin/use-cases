package com.example.uc6;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.concurrent.Executor;

import com.example.orders.OrderHistory;
import com.example.orders.OrderStore;
import com.example.orders.Product;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.test.context.bean.override.convention.TestBean;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.browserless.internal.MockVaadin;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;

@SpringBootTest
@WithAnonymousUser
@ViewPackages(classes = ReportView.class)
class ReportViewTest extends SpringBrowserlessTest {

    private static final ManualExecutor EXECUTOR = new ManualExecutor();

    /** Replaces the application's executor with the manual one. */
    @TestBean(name = "reportExecutor")
    private Executor reportExecutor;

    static Executor reportExecutor() {
        return EXECUTOR;
    }

    @MockitoSpyBean
    private OrderHistory history;

    @Autowired
    private OrderStore store;

    @BeforeEach
    void oneExtraOrder() {
        store.clear();
        store.place("Northwind", Product.GRINDER, 1, LocalDate.of(2026, 3, 5),
                new BigDecimal("189.00"));
    }

    @AfterEach
    void emptyStore() {
        store.clear();
    }

    @Test
    void buttonWaitsUntilThePushedResultArrives() {
        ReportView view = navigate(ReportView.class);

        test(view.buildButton()).click();

        assertFalse(view.buildButton().isEnabled());
        assertTrue(view.progress().isVisible());
        assertEquals("Building…", view.result());
        assertEquals(1, EXECUTOR.pending());

        // The background job finishes now. Its UI.access command is queued
        // until the queue runs; the public roundTrip() does not run it, so
        // the internal helper does (see API-GAPS.md).
        EXECUTOR.runAll();
        MockVaadin.runUIQueue();

        assertTrue(view.buildButton().isEnabled());
        assertFalse(view.progress().isVisible());
        // Every customer has 12,500 past orders; the tie goes to the first
        // one alphabetically, and one more order decides it.
        assertEquals("100,001 orders · top customer Northwind", view.result());
    }

    @Test
    void failedJobReEnablesTheButtonAndSaysSo() {
        doThrow(new IllegalStateException("database down")).when(history)
                .count("");
        ReportView view = navigate(ReportView.class);

        test(view.buildButton()).click();
        EXECUTOR.runAll();
        MockVaadin.runUIQueue();

        assertTrue(view.buildButton().isEnabled());
        assertFalse(view.progress().isVisible());
        assertEquals("Report failed", view.result());
    }
}
