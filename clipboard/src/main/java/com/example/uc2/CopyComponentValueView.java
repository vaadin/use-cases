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
 * UC2 — Copy a field's value.
 * <p>
 * {@code Clipboard.onClick(button).writeText(linkField, onCopied, onError)}
 * copies whatever the field holds at the moment of the click. The browser reads
 * the field's {@code value} property in the click handler, so an edit the
 * server has not seen yet is copied too, and the user gesture is still valid.
 * The notification shows the text the browser reports as copied.
 */
@Route(value = "uc2", layout = MainLayout.class)
@PageTitle("UC2 — Copy a field's value")
@UseCaseDescription("Copying whatever a field holds at the moment of the click")
@Menu(order = 2, title = "UC2 — Copy a field's value")
public class CopyComponentValueView extends VerticalLayout {

    public CopyComponentValueView() {
        add(new H1("UC2 — Copy a field's value"));
        add(new Paragraph(
                "Edit the link, then click Copy. The browser reads the "
                        + "field when you click, so the copy holds exactly "
                        + "what you typed."));

        TextField linkField = new TextField("Share link");
        linkField.setValue("https://example.com/share/abc123");
        linkField.setWidthFull();

        Button copyButton = new Button("Copy");
        Clipboard.onClick(copyButton).writeText(linkField,
                copied -> Notification.show("Copied " + copied),
                error -> Notification.show("Copy failed: " + error.message()));

        HorizontalLayout row = new HorizontalLayout(linkField, copyButton);
        row.setAlignItems(Alignment.END);
        row.setWidthFull();
        add(row);
    }
}
