package com.example.uc5;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;
import com.example.views.TestsNote;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

/**
 * UC5 — Security.
 * <p>
 * The public entry to {@link StaffDeskView}, which only signed-in staff may
 * open. A protected view is left out of the menu for visitors who may not open
 * it, so this page links to it instead.
 */
@Route(value = "uc5", layout = MainLayout.class)
@PageTitle("UC5 — Security")
@UseCaseDescription("Testing login redirects, role-based access and role-dependent UI")
@Menu(order = 5, title = "UC5 — Security")
@AnonymousAllowed
public class SecurityView extends VerticalLayout {

    public SecurityView() {
        add(new H1("UC5 — Security"));
        add(new Paragraph("The staff desk is only for signed-in staff. "
                + "Open it as a visitor and you land on the login page; sign "
                + "in as clerk / clerk or admin / admin. Only the admin sees "
                + "the button that clears all orders."));
        Div sample = new Div(
                new RouterLink("Open the staff desk", StaffDeskView.class));
        sample.addClassName("sample");
        add(sample, new TestsNote("View test: uc5/StaffDeskViewTest "
                + "(browserless, @WithMockUser per role)"));
    }
}
