package com.example.uc3;

import com.example.common.UseCaseDescription;
import com.example.views.MainLayout;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.clipboard.Clipboard;
import com.vaadin.flow.component.clipboard.ClipboardContent;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.Paragraph;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Menu;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;

/**
 * UC3 — Copy rich content (HTML with a plain-text fallback).
 * <p>
 * Writes both an HTML and a plain-text representation in a single clipboard
 * operation using {@link ClipboardContent}. Rich destinations (a doc editor, an
 * email composer) consume the HTML; terminals and code editors pick up the
 * plain-text fallback. The write happens client-side inside the click handler
 * so the user gesture is preserved.
 */
@Route(value = "uc3", layout = MainLayout.class)
@PageTitle("UC3 — Copy rich content")
@UseCaseDescription("Copying formatted HTML with a plain-text fallback")
@Menu(order = 3, title = "UC3 — Copy rich content")
public class CopyRichContentView extends VerticalLayout {

    private static final String HTML = """
            <p>Visit <a href="https://vaadin.com">Vaadin</a> for
            <strong>Java web apps</strong>.</p>""";

    private static final String PLAIN_TEXT = """
            Visit Vaadin (https://vaadin.com) for Java web apps.""";

    public CopyRichContentView() {
        add(new H1("UC3 — Copy rich content (HTML + plain-text fallback)"));
        add(new Paragraph(
                "Both representations are written in a single clipboard "
                        + "operation. Paste into a rich editor to see the HTML; "
                        + "paste into a code editor or terminal to see the "
                        + "plain text."));

        Div preview = new Div();
        preview.getElement().setProperty("innerHTML", HTML);
        add(new Paragraph("Preview:"), preview);

        Button copyButton = new Button("Copy rich content");
        ClipboardContent content = ClipboardContent.create().text(PLAIN_TEXT)
                .html(HTML);
        Clipboard.onClick(copyButton).write(content,
                written -> Notification.show("Copied"),
                error -> Notification.show("Copy failed: " + error.message()));

        add(copyButton);
    }
}
