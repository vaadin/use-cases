package com.example.views;

import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.router.RouterLink;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Login - Signal API Use Cases")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm loginForm = new LoginForm();

    public LoginView() {
        setSizeFull();
        setAlignItems(Alignment.CENTER);
        setJustifyContentMode(JustifyContentMode.CENTER);

        loginForm.setAction("login");

        // Add helpful information about available users
        Div infoBox = new Div();
        infoBox.getStyle().set("background-color", "#f0f8ff")
                .set("padding", "1em").set("border-radius", "4px")
                .set("margin-bottom", "1em").set("max-width", "400px");

        H3 infoTitle = new H3("Demo Users");
        infoTitle.getStyle().set("margin-top", "0");

        Paragraph infoText = new Paragraph();
        infoText.getStyle().set("font-family", "monospace")
                .set("white-space", "pre-line").set("margin", "0.5em 0");
        infoText.setText("viewer / password → VIEWER\n"
                + "editor / password → EDITOR\n" + "admin / password → ADMIN\n"
                + "superadmin / password → SUPER_ADMIN");

        infoBox.add(infoTitle, infoText);

        RouterLink continueAsGuest = new RouterLink(
                "Continue without logging in", HomeView.class);

        add(new H1("Signal API Use Cases"), infoBox, loginForm,
                continueAsGuest);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        if (event.getLocation().getQueryParameters().getParameters()
                .containsKey("error")) {
            loginForm.setError(true);
        }
    }
}
