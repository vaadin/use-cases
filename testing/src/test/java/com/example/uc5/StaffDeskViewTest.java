package com.example.uc5;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.example.orders.OrderStore;
import com.example.orders.Product;
import com.example.security.LoginView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithAnonymousUser;
import org.springframework.security.test.context.support.WithMockUser;

import com.vaadin.browserless.SpringBrowserlessTest;
import com.vaadin.browserless.ViewPackages;
import com.vaadin.flow.component.html.H2;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ViewPackages(classes = { StaffDeskView.class, SecurityView.class,
        LoginView.class })
class StaffDeskViewTest extends SpringBrowserlessTest {

    @Autowired
    private OrderStore store;

    @BeforeEach
    void emptyStore() {
        store.clear();
    }

    @Test
    @WithAnonymousUser
    void visitorsAreSentToTheLoginPage() {
        navigate("uc5/desk", LoginView.class);

        assertInstanceOf(LoginView.class, getCurrentView());
    }

    @Test
    @WithAnonymousUser
    void visitorsCanOpenThePublicPage() {
        assertInstanceOf(SecurityView.class, navigate(SecurityView.class));
    }

    @Test
    @WithMockUser(username = "clerk", roles = "CLERK")
    void clerksSeeTheDeskButNotTheAdminButton() {
        StaffDeskView view = navigate(StaffDeskView.class);

        assertEquals("Signed in as clerk",
                findInView(H2.class).single().getText());
        assertFalse(view.clearButton().isVisible());
    }

    @Test
    @WithMockUser(username = "admin", roles = { "CLERK", "ADMIN" })
    void adminsCanClearAllOrders() {
        store.place("Blue Finch", Product.GRINDER, 1, LocalDate.of(2026, 3, 5),
                new BigDecimal("189.00"));
        StaffDeskView view = navigate(StaffDeskView.class);
        assertTrue(view.clearButton().isVisible());

        test(view.clearButton()).click();

        assertTrue(store.all().isEmpty());
    }
}
