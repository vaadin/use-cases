package com.example.uc2;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC2 — Copy the current value of another component.
 * <p>
 * The user can edit the field freely; whatever is in it at the moment of the
 * click is what gets copied. The value is read client-side from the source
 * element at click time, so there is still no server round-trip and the user
 * gesture is preserved.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Copy current value of a component")
@UseCaseDescription("Copying whatever a field holds at the moment of the click")
@Menu(order = 2, title = "UC2 — Copy component value")
public class CopyComponentValueView extends VerticalLayout {

    public CopyComponentValueView() {
        add(new H1("UC2 — Copy the current value of a component"));
        add(new Paragraph(
                "The text field's value is read client-side at click time — "
                        + "edit it and the copy reflects whatever you typed."));

        TextField linkField = new TextField("Share link");
        linkField.setValue("https://example.com/share/abc123");
        linkField.setWidthFull();

        Button copyButton = new Button("Copy");
        Clipboard.onClick(copyButton).writeText(linkField,
                written -> Notification.show("Link copied"),
                error -> Notification.show("Copy failed: " + error.message()));

        HorizontalLayout row = new HorizontalLayout(linkField, copyButton);
        row.setAlignItems(Alignment.END);
        row.setWidthFull();
        add(row);
    }
}
