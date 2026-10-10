package com.example.security;

import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.login.LoginForm;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.auth.AnonymousAllowed;

@Route("login")
@PageTitle("Sign in — Testing Use Cases")
@AnonymousAllowed
public class LoginView extends VerticalLayout implements BeforeEnterObserver {

    private final LoginForm form = new LoginForm();

    public LoginView() {
        setAlignItems(Alignment.CENTER);
        form.setAction("login");
        add(new H1("Order desk"),
                new Paragraph("Demo users: clerk / clerk, admin / admin"),
                form);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        form.setError(event.getLocation().getQueryParameters().getParameters()
                .containsKey("error"));
    }
}
