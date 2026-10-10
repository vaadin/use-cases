package com.example.uc5;

import jakarta.annotation.security.RolesAllowed;

import com.example.orders.OrderStore;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.security.AuthenticationContext;

/**
 * UC5 — The staff desk, only for signed-in staff.
 * <p>
 * The order desk for staff: anonymous visitors are sent to the login page,
 * clerks see the desk, and only admins see the button that clears all placed
 * orders. Access is declared with {@link RolesAllowed}, and the admin-only
 * button checks the role through {@link AuthenticationContext}.
 * <p>
 * The tests run the same view as an anonymous visitor, a clerk and an admin
 * ({@code @WithMockUser}) and check where each one ends up and what each one
 * can see.
 */
@Route(value = "uc5/desk", layout = MainLayout.class)
@PageTitle("UC5 — Staff desk")
@RolesAllowed({ "CLERK", "ADMIN" })
public class StaffDeskView extends VerticalLayout {

    private final Button clear = new Button("Clear all placed orders");

    public StaffDeskView(AuthenticationContext authentication,
            OrderStore store) {
        add(new H1("UC5 — Staff desk"));
        add(new Paragraph("Sign in as clerk / clerk or admin / admin. Only "
                + "the admin sees the button below."));

        String user = authentication.getPrincipalName().orElse("unknown");
        clear.addThemeVariants(ButtonVariant.ERROR);
        clear.setVisible(authentication.hasRole("ADMIN"));
        clear.addClickListener(e -> {
            store.clear();
            Notification.show("All placed orders cleared");
        });
        Button logout = new Button("Sign out", e -> authentication.logout());

        Div sample = new Div(new H2("Signed in as " + user), clear, logout);
        sample.addClassName("sample");
        add(sample, new TestsNote("View test: uc5/StaffDeskViewTest "
                + "(browserless, @WithMockUser per role)"));
    }

    // Package-private test seam.
    Button clearButton() {
        return clear;
    }
}
