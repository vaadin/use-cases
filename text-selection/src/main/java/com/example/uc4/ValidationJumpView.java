package com.example.uc4;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC4 — Jump to validation error.
 * <p>
 * On submit the server validates the username and, on failure, selects the
 * first part of the value that breaks a rule with
 * {@code setSelectionRange(start, end)}, so the user can retype it at once
 * instead of looking for the mistake.
 */
@Route(value = "uc4", layout = MainLayout.class)
@PageTitle("UC4 — Jump to validation error")
@UseCaseDescription("Pointing the user to the exact invalid part of a value")
@Menu(order = 4, title = "UC4 — Jump to validation error")
public class ValidationJumpView extends VerticalLayout {

    private static final Pattern INVALID_CHAR = Pattern.compile("[^a-z0-9_]+");

    public ValidationJumpView() {
        add(new H1("UC4 — Jump to validation error"));
        add(new Paragraph(
                "Usernames must be at least three characters and contain only "
                        + "lowercase letters, digits and underscores. Press "
                        + "Submit: the first invalid part of the value is "
                        + "selected, so you can type over just that part. "
                        + "Fix it and submit again to find the next one."));

        TextField username = new TextField("Username");
        username.setValue("My Cool User");
        username.setWidth("320px");
        username.setMaxWidth("100%");

        Span status = new Span();
        status.addClassName("uc-status");

        Button submit = new Button("Submit", e -> {
            String value = username.getValue() == null ? ""
                    : username.getValue();
            Matcher bad = INVALID_CHAR.matcher(value);
            if (bad.find()) {
                username.setSelectionRange(bad.start(), bad.end());
                status.setText("Invalid characters at " + bad.start() + "–"
                        + bad.end() + ". Retype to fix.");
                status.removeClassName("success");
                status.addClassName("error");
                return;
            }
            if (value.length() < 3) {
                username.setSelectionRange(0, value.length());
                status.setText("Username must be at least 3 characters.");
                status.removeClassName("success");
                status.addClassName("error");
                return;
            }
            status.setText("\"" + value + "\" is valid.");
            status.removeClassName("error");
            status.addClassName("success");
        });

        HorizontalLayout row = new HorizontalLayout(username, submit, status);
        row.setAlignItems(Alignment.END);
        row.addClassName("uc-toolbar");
        add(row);
    }
}
